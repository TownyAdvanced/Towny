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
			} catch (Exception e) {
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
	 * @param nation the attacking nation
	 * @param town the defending town
	 */
	public static void pillageTown(List<Resident> pillagingResidents, Nation nation, Town town) {
		try {
			double actualPillageAmount;
			boolean townNewlyBankrupted = false;
			boolean townDestroyed = false;

			if (pillagingResidents.size() == 0
				|| town == null
				|| town.getTownBlocks() == null
				|| town.getTownBlocks().size() == 0) 
			return;

			double fullPillageAmountForAllAttackingSoldiers =
				TownySettings.getWarSiegePillageAmountPerPlot()
					* town.getTownBlocks().size()
					* getMoneyMultiplier(town);

			//Take money from town
			if(town.getAccount().pay(fullPillageAmountForAllAttackingSoldiers,"Pillage")) {
				//Town can afford payment
				actualPillageAmount = fullPillageAmountForAllAttackingSoldiers;
			} else {
				//Town cannot afford payment
				if (TownySettings.isTownBankruptcyEnabled()) {
					//Take from town
					if(town.getAccount().isBankrupt()) {
						//Town already bankrupt
						boolean pillageSuccessful = town.getAccount().withdraw(fullPillageAmountForAllAttackingSoldiers, "Pillage by soldiers of " + nation.getName());
						if(pillageSuccessful)
							actualPillageAmount = fullPillageAmountForAllAttackingSoldiers;
						else
							actualPillageAmount = 0;
					} else {
						//We will bankrupt town now
						double prePaymentTownBankBalance = town.getAccount().getHoldingBalance();
						town.getAccount().setBalance(0, "Pillage by soldiers of " + nation.getName());
						double actualDebtIncrease = town.getAccount().withdraw(fullPillageAmountForAllAttackingSoldiers - prePaymentTownBankBalance, "Pillage by soldiers of " + nation.getName());
						actualPillageAmount = prePaymentTownBankBalance + actualDebtIncrease;
						townNewlyBankrupted = true;
					}
				} else {
					//Destroy town
					actualPillageAmount = town.getAccount().getHoldingBalance();
					townDestroyed = true;
				}
			}

			//Give money steal to pillaging residents
			double pillageAmountReceivedByEachResident = actualPillageAmount / pillagingResidents.size();
			for (Resident pillagingResident : pillagingResidents) {
				pillagingResident.getAccount().collect(pillageAmountReceivedByEachResident, "Pillage of " + town.getName());
			}

			//Save data
			if(townDestroyed) {
				TownyUniverse.getInstance().getDataSource().removeTown(town);
			} 

			//Send any required messages
			if (townNewlyBankrupted) {
				TownyMessaging.sendGlobalMessage(
					String.format(
						TownySettings.getLangString("msg_siege_war_town_bankrupted_from_pillage"),
						town.getFormattedName(),
						nation.getFormattedName()));
			} else if (townDestroyed) {
				TownyMessaging.sendGlobalMessage(
					String.format(
						TownySettings.getLangString("msg_siege_war_town_ruined_from_pillage"),
						town.getFormattedName(),
						nation.getFormattedName()));
			}

		} catch (Exception e) {
			if(town == null) {
				System.out.println("Problem with pillaging.");
			} else {
				System.out.println("Problem with pillaging at town " + town.getName() + ".");
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

	public static void makeNationRefundAvailable(Resident king) {
		//Refund some of the initial setup cost to the king
		if (TownySettings.getWarSiegeEnabled()
			&& TownySettings.isUsingEconomy()
			&& TownySettings.getWarSiegeRefundInitialNationCostOnDelete()) {

			//Make the nation refund available
			//The player can later do "/n claim refund" to receive the money
			int amountToRefund = (int)(TownySettings.getNewNationPrice() * 0.01 * TownySettings.getWarSiegeNationCostRefundPercentageOnDelete());
			king.addToNationRefundAmount(amountToRefund);
			TownyUniverse.getInstance().getDataSource().saveResident(king);

			TownyMessaging.sendMsg(
				king,
				String.format(
					TownySettings.getLangString("msg_siege_war_nation_refund_available"),
					TownyEconomyHandler.getFormattedBalance(amountToRefund)));
		}
	}
}
