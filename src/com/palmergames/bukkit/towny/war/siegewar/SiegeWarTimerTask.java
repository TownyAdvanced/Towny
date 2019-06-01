package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.util.ChatTools;

import java.util.ArrayList;
import java.util.List;

public class SiegeWarTimerTask extends TownyTimerTask {

	private final static long ONE_MINUTE_IN_MILLIS = 60000;
	private final static long ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;

	private static boolean timeForUpkeep;
	private static long nextTimeForUpkeep;
	private static boolean timeToSavePointsToDB;
	private static long nextTimeToSavePointsToDB;

	static
	{
		timeToSavePointsToDB = false;
		nextTimeToSavePointsToDB = System.currentTimeMillis() + ONE_MINUTE_IN_MILLIS;
		timeForUpkeep = false;
		nextTimeForUpkeep = System.currentTimeMillis() + ONE_HOUR_IN_MILLIS;
	}

	public SiegeWarTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		try {
			TownyMessaging.sendMsg("Now evaluating siege war timer task");

			if (System.currentTimeMillis() > nextTimeForUpkeep) {
				timeForUpkeep = true;
			}

			if(System.currentTimeMillis() > nextTimeToSavePointsToDB) {
				timeToSavePointsToDB = true;
			}

			//Cycle through all sieges
			for (Siege siege : new ArrayList<>(TownyUniverse.getDataSource().getSieges())) {
				if (!siege.isComplete()) {
					TownyMessaging.sendMsg("Now evaluating active siege between " +
							siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

					//Upkeep
					if (TownySettings.isUsingEconomy() && timeForUpkeep) {
						siege.applyUpkeepCost(TownySettings.getWarSiegeAttackerCostPerHour());
					}

					//Caching
					if(timeToSavePointsToDB) {
						TownyUniverse.getDataSource().saveSiege(siege);
					}

					//Adjust points
					//Here we need to cycle through all residents in the world....
					//

					//Check if scheduled end time has arrived
					if(System.currentTimeMillis() > siege.getScheduledEndTime()) {

						if(siege.getTotalSiegePointsAttacker() > siege.getTotalSiegePointsDefender()) {
							attackerWin(siege);
						} else{
							defenderWin(siege);
						}

						checkForCompletionOfAllRecentSieges(siege);
					}
				}
			}

		} finally {
			if(timeToSavePointsToDB) {
				timeToSavePointsToDB = false;
				nextTimeToSavePointsToDB = System.currentTimeMillis()+ ONE_MINUTE_IN_MILLIS;
			}
			if(timeForUpkeep) {
				timeForUpkeep = false;
				nextTimeForUpkeep = System.currentTimeMillis()+ ONE_HOUR_IN_MILLIS;
			}
		}
	}

	private void attackerWin(Siege siege) {
		captureTown(siege);
		if (TownySettings.isUsingEconomy()) {
			plunderTown(siege);
		}
	}

	private void defenderWin(Siege siege) {
		TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
				TownySettings.getLangString("msg_siege_war_defender_win"),
				TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
				TownyFormatter.getFormattedNationName(siege.getAttackingNation())
		)));

	}

	private void checkForCompletionOfAllRecentSieges(Siege siege) {

		//If all sieges on the town are now complete,
		//activate cooldowns and clear the recent sieges list.
		if(siege.getDefendingTown().areAllSiegesComplete()) {

			//Activate cooldowns
			if(siege.getSiegeType() == SiegeType.ASSAULT) {
				long totalDurationOfRecentSiegesMillis = siege.getDefendingTown().getTotalDurationOfRecentSiegesMillis();
				long cooldownDurationMillis = totalDurationOfRecentSiegesMillis * TownySettings.getWarSiegeCooldownForAssaultSiegesModifier();
				long cooldownEndTimeMillis= System.currentTimeMillis() + cooldownDurationMillis;
				siege.getDefendingTown().setAssaultSiegeCooldownEndTime(cooldownEndTimeMillis);
			} else if (siege.getSiegeType() == SiegeType.REVOLT) {
				long cooldownDurationMillis = TownySettings.getWarSiegeCooldownForRevoltsHours() * ONE_HOUR_IN_MILLIS;
				long cooldownEndTimeMillis= System.currentTimeMillis() + cooldownDurationMillis;
				siege.getDefendingTown().setRevoltSiegeCooldownEndTime(cooldownEndTimeMillis);
			} else {
				TownyMessaging.sendErrorMsg("Unknown siege type enum");
			}

			//Remove recent sieges
			for(Siege siegeToDelete: new ArrayList<>(siege.getDefendingTown().getSieges())) {
				TownyUniverse.getDataSource().removeSiege(siegeToDelete);
			}

		}
	}

	private void captureTown(Siege siege) {
		if(siege.getDefendingTown().hasNation()) {

			Nation nationOfCapturedTown = null;
			try {
				nationOfCapturedTown = siege.getDefendingTown().getNation();
			} catch (NotRegisteredException x) {
				//This won't happen because we checked for a nation just above
			}

			removeTownFromNation(siege.getDefendingTown(), nationOfCapturedTown);

			addTownToNation(siege.getDefendingTown(), siege.getAttackingNation());

			TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
					TownySettings.getLangString("msg_siege_war_nation_town_captured"),
					TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
					TownyFormatter.getFormattedNationName(nationOfCapturedTown),
					TownyFormatter.getFormattedNationName(siege.getAttackingNation())
			)));

			if(nationOfCapturedTown.getTowns().size() == 0) {
				TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
						TownySettings.getLangString("msg_siege_war_nation_defeated"),
						TownyFormatter.getFormattedNationName(nationOfCapturedTown)
				)));
			}
		} else {
			addTownToNation(siege.getDefendingTown(), siege.getAttackingNation());

			TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
					TownySettings.getLangString("msg_siege_war_neutral_town_captured"),
					TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
					TownyFormatter.getFormattedNationName(siege.getAttackingNation())
			)));
		}
	}

	private void removeTownFromNation(Town town, Nation nation) {
		boolean removeNation = false;

		try {
			nation.removeTown(town);
		} catch(NotRegisteredException x) {
			TownyMessaging.sendErrorMsg("Attempted to remove town from nation but Town was already removed.");
			return;  //Town was already removed
		} catch(EmptyNationException x) {
			removeNation = true;  //Set flag to remove nation at end of this method
		}
		/*
		 * Remove all resident titles/nationRanks before saving the town itself.
		 */
		List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

		for (Resident res : titleRemove) {
			if (res.hasTitle() || res.hasSurname()) {
				res.setTitle("");
				res.setSurname("");
			}
			res.updatePermsForNationRemoval(); // Clears the nationRanks.
			TownyUniverse.getDataSource().saveResident(res);
		}

		if(removeNation) {
			TownyUniverse.getDataSource().removeNation(nation);
			TownyUniverse.getDataSource().saveNationList();
		} else {
			TownyUniverse.getDataSource().saveNation(nation);
			TownyUniverse.getDataSource().saveNationList();
			plugin.resetCache();
		}

		TownyUniverse.getDataSource().saveTown(town);
	}

	private void addTownToNation(Town town,Nation nation) {
			try {
				nation.addTown(town);
				TownyUniverse.getDataSource().saveTown(town);
				plugin.resetCache();
				TownyUniverse.getDataSource().saveNation(nation);
			} catch (AlreadyRegisteredException x) {
				return;   //Town already in nation
			}
	}

	private void plunderTown(Siege siege) {

		if (TownySettings.isUsingEconomy()) {
			double plunder =
					TownySettings.getWarSiegeAttackerPlunderAmountPerPlot()
							* siege.getDefendingTown().getTownBlocks().size();

			try {
				if (siege.getDefendingTown().canPayFromHoldings(plunder))
					siege.getDefendingTown().pay(plunder, "Town was plundered by attacker");
				else {
					TownyMessaging.sendGlobalMessage("The town " + siege.getDefendingTown().getName() + " was destroyed by " +siege.getAttackingNation().getName());
					TownyUniverse.getDataSource().removeTown(siege.getDefendingTown());
				}
			} catch (EconomyException x) {
				TownyMessaging.sendErrorMsg(x.getMessage());
			}
		}
	}

}