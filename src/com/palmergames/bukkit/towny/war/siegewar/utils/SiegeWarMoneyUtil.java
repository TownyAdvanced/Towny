package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;

public class SiegeWarMoneyUtil {

	/**
	 * This method gives all the war chests in the siege, to the winnner nation
	 *
	 * @param siege the siege
	 * @param winnerNation the winning nation
	 */
	public static void giveWarChestsToWinnerNation(Siege siege, Nation winnerNation) {
		if (TownySettings.isUsingEconomy()) {
			try {
				for (SiegeZone siegeZone : siege.getSiegeZones().values()) {
					winnerNation.getAccount().collect(siegeZone.getWarChestAmount(), "War Chest Captured/Returned");
					String message =
						String.format(
							TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
							TownyFormatter.getFormattedNationName(winnerNation),
							TownyEconomyHandler.getFormattedBalance(siegeZone.getWarChestAmount()));

					//Send message to nation(s)
					TownyMessaging.sendPrefixedNationMessage(winnerNation, message);
					if (winnerNation != siegeZone.getAttackingNation())
						TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
					//Send message to town
					TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
				}
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner nation");
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method gives all the war chests in the siege, to the winnner town
	 *
	 * @param siege the siege
	 * @param winnerTown the winning town
	 */
	public static void giveWarChestsToWinnerTown(Siege siege, Town winnerTown) {
		for (SiegeZone siegeZone : siege.getSiegeZones().values()) {
			giveOneWarChestToWinnerTown(siegeZone, winnerTown);
		}
	}
	
	/**
	 * This method gives one war chest to the winnner town
	 *
	 * @param siegeZone the siege zone
	 * @param winnerTown the winning town
	 */
	public static void giveOneWarChestToWinnerTown(SiegeZone siegeZone, Town winnerTown) {
		if(TownySettings.isUsingEconomy()) {
			try {
				winnerTown.getAccount().collect(siegeZone.getWarChestAmount(), "War Chest Captured");
				String message =
					String.format(
						TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
						TownyFormatter.getFormattedTownName(winnerTown),
						TownyEconomyHandler.getFormattedBalance(siegeZone.getWarChestAmount()));

				//Send message to nation
				TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(winnerTown, message);
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner town");
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method steals money from the defending town and gives it to an attacking player
	 *
	 * @param player the attacking player
	 * @param attackingNation the attacking nation
	 * @param defendingTown the defending town
	 */
	public static void pillageTown(Player player, Nation attackingNation, Town defendingTown) {

		try {
			double fullPillageAmount = TownySettings.getWarSiegePillageAmountPerPlot() * defendingTown.getTownBlocks().size();

			TownyUniverse universe = TownyUniverse.getInstance();
			Resident pillagingResident = universe.getDataSource().getResident(player.getName());

			if (defendingTown.getAccount().canPayFromHoldings(fullPillageAmount)) {
				defendingTown.getAccount().payTo(fullPillageAmount, pillagingResident, "Town pillaged by attacker");
				universe.getDataSource().saveResident(pillagingResident);
				universe.getDataSource().saveTown(defendingTown);
			} else {
				double actualPillageAmount = defendingTown.getAccount().getHoldingBalance();
				defendingTown.getAccount().payTo(actualPillageAmount, pillagingResident, "Towny pillaged by attacker");

				TownyMessaging.sendGlobalMessage(
					String.format(
						TownySettings.getLangString("msg_siege_war_town_ruined_from_pillage"),
						TownyFormatter.getFormattedTownName(defendingTown),
						TownyFormatter.getFormattedNationName(attackingNation)));

				universe.getDataSource().saveResident(pillagingResident);
				universe.getDataSource().removeTown(defendingTown);
			}
		} catch (EconomyException x) {
			x.printStackTrace();
		} catch (NotRegisteredException x) {
			x.printStackTrace();
		}
	}
}
