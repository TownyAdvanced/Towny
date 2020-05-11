package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.entity.Player;

import java.util.List;

public class SiegeWarMoneyUtil {

	/**
	 * This method gives the war chest to the attacking nation
	 *
	 * @param siege the siege
	 */
	public static void giveWarChestToAttackingNation(Siege siege) {
		Nation winnerNation = siege.getAttackingNation();
		if (TownySettings.isUsingEconomy()) {
			try {
				winnerNation.getAccount().collect(siege.getWarChestAmount(), "War Chest Captured/Returned");
				String message =
					String.format(
						TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
						winnerNation.getFormattedName(),
						TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));

				//Send message to nation(
				TownyMessaging.sendPrefixedNationMessage(winnerNation, message);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner nation");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method gives one war chest to the defending town
	 *
	 * @param siege the siege zone
	 */
	public static void giveWarChestToDefendingTown(Siege siege) {
		Town winnerTown= siege.getDefendingTown();
		if(TownySettings.isUsingEconomy()) {
			try {
				winnerTown.getAccount().collect(siege.getWarChestAmount(), "War Chest Captured");
				String message =
					String.format(
						TownySettings.getLangString("msg_siege_war_attack_recover_war_chest"),
						winnerTown.getFormattedName(),
						TownyEconomyHandler.getFormattedBalance(siege.getWarChestAmount()));

				//Send message to nation
				TownyMessaging.sendPrefixedNationMessage(siege.getAttackingNation(), message);
				//Send message to town
				TownyMessaging.sendPrefixedTownMessage(winnerTown, message);
			} catch (EconomyException e) {
				System.out.println("Problem paying war chest(s) to winner town");
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method steals money from the defending town and gives it to one or more attacking players
	 * The full steal amount is divided between all pillaging players
	 * 
	 * @param pillagingResidents the residents doing the pillaging
	 * @param attackingNation the attacking nation
	 * @param defendingTown the defending town
	 */
	public static void pillageTown(List<Resident> pillagingResidents, Nation attackingNation, Town defendingTown) {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			boolean townRuined = false;

			if (pillagingResidents.size() > 0
				&& defendingTown != null
				&& defendingTown.getTownBlocks() != null) {

				double fullPillageAmountForAllResidents =
					TownySettings.getWarSiegePillageAmountPerPlot()
						* defendingTown.getTownBlocks().size()
						* getMoneyMultiplier(defendingTown);
				double fullPillageAmountForOneResident = fullPillageAmountForAllResidents / pillagingResidents.size();

				//Pillage for each resident
				for (Resident pillagingResident : pillagingResidents) {
					if (defendingTown.getAccount().canPayFromHoldings(fullPillageAmountForOneResident)) {
						defendingTown.getAccount().payTo(fullPillageAmountForOneResident, pillagingResident, "Town pillaged by attacker");
						defendingTown.getSiege().increaseTotalPillageAmount(fullPillageAmountForOneResident);
						universe.getDataSource().saveResident(pillagingResident);
					} else {
						double actualPillageAmount = defendingTown.getAccount().getHoldingBalance();
						defendingTown.getAccount().payTo(actualPillageAmount, pillagingResident, "Towny pillaged by attacker");
						defendingTown.getSiege().increaseTotalPillageAmount(actualPillageAmount);
						universe.getDataSource().saveResident(pillagingResident);
						townRuined = true;
						break;
					}
				}

				//Save town
				universe.getDataSource().saveTown(defendingTown);

				//Remove town if necessary
				if (townRuined) {
					universe.getDataSource().removeTown(defendingTown);
					TownyMessaging.sendGlobalMessage(
						String.format(
							TownySettings.getLangString("msg_siege_war_town_ruined_from_pillage"),
							defendingTown.getFormattedName(),
							attackingNation.getFormattedName()));
				}
			}
		} catch (Exception e) {
			if(defendingTown == null) {
				System.out.println("Problem with pillaging.");
			} else {
				System.out.println("Problem with pillaging at town " + defendingTown.getName() + ".");
			}
			e.printStackTrace();
		}
	}

	/**
	 * Gets the siegewar money multiplier for the given town
	 *
	 * @param town the town to consider
	 * @return the multiplier
	 */
	public static double getMoneyMultiplier(Town town) {
		double extraMoneyPercentage = TownySettings.getWarSiegeExtraMoneyPercentagePerTownLevel();

		if(extraMoneyPercentage == 0) {
			return 1;
		} else {
			return 1 + ((extraMoneyPercentage / 100) * (TownySettings.calcTownLevelId(town) -1));
		}
	}

	/**
	 * If the player is due a nation refund, pays the refund to the player
	 *
	 * @param player
	 */
	public static void claimNationRefund(Player player) throws Exception {
		if(!(TownySettings.getWarSiegeEnabled() && TownySettings.getWarSiegeRefundInitialNationCostOnDelete())) {
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
		}

		TownyDataSource townyDataSource = TownyUniverse.getInstance().getDataSource();
		Resident formerKing = townyDataSource.getResident(player.getName());

		if(formerKing.getNationRefundAmount() != 0) {
			int refundAmount = formerKing.getNationRefundAmount();
			formerKing.getAccount().collect(refundAmount, "Nation Refund");
			formerKing.setNationRefundAmount(0);
			townyDataSource.saveResident(formerKing);
			TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_siege_war_nation_refund_claimed"), TownyEconomyHandler.getFormattedBalance(refundAmount)));
		} else {
			throw new TownyException(TownySettings.getLangString("msg_err_siege_war_nation_refund_unavailable"));
		}
	}
}
