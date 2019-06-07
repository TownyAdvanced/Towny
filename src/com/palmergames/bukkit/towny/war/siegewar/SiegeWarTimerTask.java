package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;

import java.util.ArrayList;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_HOUR_IN_MILLIS;
import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_MINUTE_IN_MILLIS;

public class SiegeWarTimerTask extends TownyTimerTask {


	private static boolean timeForUpkeep;
	private static long nextTimeForUpkeep;
	private static boolean timeToSaveSiegeToDB;
	private static long nextTimeToSavePointsToDB;

	static
	{
		timeToSaveSiegeToDB = false;
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
				timeToSaveSiegeToDB = true;
			}

			//Cycle through all sieges
			for (Siege siege : new ArrayList<>(TownyUniverse.getDataSource().getSieges())) {
				if (siege.getStatus() == SiegeStatus.IN_PROGRESS) {
					TownyMessaging.sendMsg("Now evaluating siege on " + siege.getDefendingTown().getName());

					//Upkeep
					if (TownySettings.isUsingEconomy() && timeForUpkeep)
						SiegeWarUtil.applyUpkeepCost(siege);

					//Adjust points
					//Here we need to cycle through all residents in the world....
					//TODO
					//Todo -dont forget to save to db sometimes etc.

					//Check if scheduled end time has arrived
					if(System.currentTimeMillis() > siege.getScheduledEndTime()) {

						//Find out who won
						TownyObject winner = SiegeWarUtil.calculateSiegeWinner(siege);

						if(winner instanceof Town) {
							SiegeWarUtil.defenderWin(siege, (Town)winner);
						} else{
							SiegeWarUtil.attackerWin(plugin, siege, (Nation)winner);
						}
					}

					//Caching
					if(timeToSaveSiegeToDB)
						TownyUniverse.getDataSource().saveSiege(siege);

				} else {
					//The siege is inactive/completed

					//This sieges stay in all lists for for 24 hours or until cooldown has expired,
					//whichever is sooner

					//TODO
				}
			}

		} finally {
			if(timeToSaveSiegeToDB) {
				timeToSaveSiegeToDB = false;
				nextTimeToSavePointsToDB = System.currentTimeMillis()+ ONE_MINUTE_IN_MILLIS;
			}
			if(timeForUpkeep) {
				timeForUpkeep = false;
				nextTimeForUpkeep = System.currentTimeMillis()+ ONE_HOUR_IN_MILLIS;
			}
		}
	}


}