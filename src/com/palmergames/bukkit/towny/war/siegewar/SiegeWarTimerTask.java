package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;

import java.util.ArrayList;

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
					if (TownySettings.isUsingEconomy() && timeForUpkeep)
						SiegeWarUtil.applyUpkeepCost(siege, TownySettings.getWarSiegeAttackerCostPerHour());

					//Caching
					if(timeToSavePointsToDB)
						TownyUniverse.getDataSource().saveSiege(siege);

					//Adjust points
					//Here we need to cycle through all residents in the world....
					//TODO
					//Todo -dont forget to save to db sometimes etc.

					//Check if scheduled end time has arrived
					if(System.currentTimeMillis() > siege.getScheduledEndTime()) {
						siege.setComplete(true);
						if(siege.getTotalSiegePointsAttacker() > siege.getTotalSiegePointsDefender()) {
							SiegeWarUtil.attackerWin(plugin, siege);
						} else{
							SiegeWarUtil.defenderWin(siege);
						}
					}

					//If siege is now complete, check if all recent sieges of the town are complete
					if(siege.isComplete())
						SiegeWarUtil.checkForCompletionOfAllRecentTownSieges(siege);
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


}