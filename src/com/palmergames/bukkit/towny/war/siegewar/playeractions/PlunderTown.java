package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

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
	 * @param event the place block event
	 */
    public static void processPlunderTownRequest(Player player,
												 Town townToBePlundered,
												 BlockPlaceEvent event) {
        try {
			if(!TownySettings.isUsingEconomy())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_economy"));

			if(TownySettings.getWarCommonPeacefulTownsEnabled() && townToBePlundered.isPeaceful())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_peaceful_town_cannot_plunder"));
			
			TownyUniverse universe = TownyUniverse.getInstance();
			Resident resident = universe.getDataSource().getResident(player.getName());
			if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_town_member"));

			Town townOfPlunderingResident = resident.getTown();
			if(!townOfPlunderingResident.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_nation_member"));
			
			if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_PLUNDER.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(townOfPlunderingResident == townToBePlundered)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_own_town"));

			Siege siege = townToBePlundered.getSiege();
			if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));
			
			if(townOfPlunderingResident.getNation() != siege.getAttackingNation())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));
			
            if(siege.isTownPlundered())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_plundered"), townToBePlundered.getName()));

            plunderTown(siege, townToBePlundered, siege.getAttackingNation(), event);
            
        } catch (TownyException e) {
            event.setBuild(false);
        	event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, e.getMessage());
        } catch (Exception e) {
			event.setBuild(false);
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg("Problem plundering town. Contact server support team.");
			System.out.println("Unexpected problem plundering town");
			e.printStackTrace();
		}
    }

    private static void plunderTown(Siege siege, Town defendingTown, Nation winnerNation, BlockPlaceEvent event) throws Exception {
		double actualPlunderAmount;
		boolean townRuined;

		double fullPlunderAmount =
			TownySettings.getWarSiegeAttackerPlunderAmountPerPlot()
				* defendingTown.getTownBlocks().size()
				* SiegeWarMoneyUtil.getMoneyMultiplier(defendingTown);

		if (defendingTown.getAccount().canPayFromHoldings(fullPlunderAmount)) {
			actualPlunderAmount = fullPlunderAmount;
			townRuined = false;
		} else {
			actualPlunderAmount = defendingTown.getAccount().getHoldingBalance();
			townRuined = true;
		}

		//Pay plunder & ruin town if applicable
		defendingTown.getAccount().payTo(actualPlunderAmount, winnerNation, "Town was plundered by attacker");
		siege.setTownPlundered(true);
		if (townRuined) {
			TownyUniverse.getInstance().getDataSource().removeTown(defendingTown);
		}
		
		//Save to db
		if(!townRuined) {
			TownyUniverse.getInstance().getDataSource().saveSiege(siege);
			TownyUniverse.getInstance().getDataSource().saveTown(defendingTown);
		} 
		
		//Send messages
		sendPlunderSuccessMessages(defendingTown, winnerNation, actualPlunderAmount, townRuined);
	}

    private static void sendPlunderSuccessMessages(Town defendingTown, Nation winnerNation, double plunderAmount, boolean townRuined) {
		//Same messages for now but may diverge in future (if we decide to track the original nation of the town)
		if (defendingTown.hasNation()) {
			TownyMessaging.sendGlobalMessage(String.format(
				TownySettings.getLangString("msg_siege_war_nation_town_plundered"),
				defendingTown.getFormattedName(),
				TownyEconomyHandler.getFormattedBalance(plunderAmount),
				winnerNation.getFormattedName()
			));
		} else {
			TownyMessaging.sendGlobalMessage(String.format(
				TownySettings.getLangString("msg_siege_war_neutral_town_plundered"),
				defendingTown.getFormattedName(),
				TownyEconomyHandler.getFormattedBalance(plunderAmount),
				winnerNation.getFormattedName()
			));
		}
		if (townRuined) {
			TownyMessaging.sendGlobalMessage(
				String.format(
					TownySettings.getLangString("msg_siege_war_town_ruined_from_plunder"),
					defendingTown,
					winnerNation.getFormattedName()));
		}
	}
}
