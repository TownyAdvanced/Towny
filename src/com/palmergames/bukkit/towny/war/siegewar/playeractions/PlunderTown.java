package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.siege.SiegeController;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import org.bukkit.entity.Player;

/**
 * This class is responsible for processing requests to plunder towns
 *
 * @author Goosius
 */
public class PlunderTown {

	/**
	 * Process a plunder town request
	 *
	 * This method does some final checks and if they pass, the plunder is executed.
	 *
	 * @param player the player who placed the plunder chest
	 * @param townToBePlundered the town to be plundered
	 * @throws TownyException when a plunder is not allowed.
	 */
    public static void processPlunderTownRequest(Player player,
												 Town townToBePlundered) throws TownyException {

		if(SiegeWarSettings.getWarCommonPeacefulTownsEnabled() && townToBePlundered.isNeutral())
			throw new TownyException(Translation.of("msg_war_siege_err_cannot_plunder_peaceful_town"));
		
		TownyUniverse universe = TownyUniverse.getInstance();
		Resident resident = universe.getResident(player.getUniqueId());
        if (resident == null)
        	throw new TownyException(Translation.of("msg_err_not_registered_1", player.getName()));
        
		if(!resident.hasTown())
			throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_town_member"));

		Town townOfPlunderingResident = resident.getTown();
		if(!townOfPlunderingResident.hasNation())
			throw new TownyException(Translation.of("msg_err_siege_war_action_not_a_nation_member"));


		if(townOfPlunderingResident == townToBePlundered)
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_plunder_own_town"));

		Siege siege = SiegeController.getSiege(townToBePlundered);
		if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_plunder_without_victory"));
		
		if(townOfPlunderingResident.getNation() != siege.getAttackingNation())
			throw new TownyException(Translation.of("msg_err_siege_war_cannot_plunder_without_victory"));
		
        if(siege.isTownPlundered())
            throw new TownyException(String.format(Translation.of("msg_err_siege_war_town_already_plundered"), townToBePlundered.getName()));

		if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_PLUNDER.getNode()))
            throw new TownyException(Translation.of("msg_err_command_disable"));
        
        plunderTown(siege, townToBePlundered, siege.getAttackingNation());

    }

    private static void plunderTown(Siege siege, Town town, Nation nation) {
		boolean townNewlyBankrupted = false;
		boolean townDestroyed = false;

		double plunderAmount =
				SiegeWarSettings.getWarSiegeAttackerPlunderAmountPerPlot()
				* town.getTownBlocks().size()
				* SiegeWarMoneyUtil.getMoneyMultiplier(town);
		
		try {
			//Redistribute money
			if(town.getAccount().canPayFromHoldings(plunderAmount)) {
				//Town can afford plunder			
				town.getAccount().payTo(plunderAmount, nation, "Plunder");
			} else {
				//Town cannot afford plunder
				
				if (TownySettings.isTownBankruptcyEnabled()) {
					// If able, they will go into bankrupcty.

					// Set the Town's debtcap fresh.
					town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));

					// Mark them as newly bankrupt for message later on.
					townNewlyBankrupted = true;

					// This will drop their actualPlunder amount to what the town's debt cap will allow. 
					// Enabling a town to go only so far into debt to pay the plunder cost.
					if (town.getAccount().getHoldingBalance() - plunderAmount < town.getAccount().getDebtCap() * -1)
						plunderAmount = town.getAccount().getDebtCap() - Math.abs(town.getAccount().getHoldingBalance());
						
					// Charge the town (using .withdraw() which will allow for going into bankruptcy.)
					town.getAccount().withdraw(plunderAmount, "Plunder by " + nation.getName());
					// And deposit it into the nation.
					nation.getAccount().deposit(plunderAmount, "Plunder of " + town.getName());
					
				} else {
					// Not able to go bankrupt, they are destroyed, pay what they can.
					plunderAmount = town.getAccount().getHoldingBalance();
					town.getAccount().payTo(plunderAmount, nation, "Plunder");
					townDestroyed = true;
				}
			}
		} catch (EconomyException e) {
			e.printStackTrace();
		}

		//Set siege plundered flag
		siege.setTownPlundered(true);

		//Save data
		if(townDestroyed) {
			TownyUniverse.getInstance().getDataSource().removeTown(town);
		} else {
			SiegeController.saveSiege(siege);
		}

		//Send plunder success messages
		if (town.hasNation()) {
			TownyMessaging.sendGlobalMessage(String.format(
				Translation.of("msg_siege_war_nation_town_plundered"),
				town.getFormattedName(),
				TownyEconomyHandler.getFormattedBalance(plunderAmount),
				nation.getFormattedName()
			));
		} else {
			TownyMessaging.sendGlobalMessage(String.format(
				Translation.of("msg_siege_war_neutral_town_plundered"),
				town.getFormattedName(),
				TownyEconomyHandler.getFormattedBalance(plunderAmount),
				nation.getFormattedName()
			));
		}

		//Send town bankrupted/destroyed message
		if(townNewlyBankrupted) {
			TownyMessaging.sendGlobalMessage(
				String.format(
					Translation.of("msg_siege_war_town_bankrupted_from_plunder"),
					town,
					nation.getFormattedName()));
		} else if (townDestroyed) {
			TownyMessaging.sendGlobalMessage(
				String.format(
					Translation.of("msg_siege_war_town_ruined_from_plunder"),
					town,
					nation.getFormattedName()));
		}
	}
}
