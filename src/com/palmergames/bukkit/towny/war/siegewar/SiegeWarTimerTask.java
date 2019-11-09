package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;

import java.util.ArrayList;
import java.util.List;

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
		if (!TownySettings.getWarSiegeEnabled())
			TownyMessaging.sendErrorMsg("Siege war feature disabled");

		long currentTime = System.currentTimeMillis();
		TownyObject winner;

		if(System.currentTimeMillis() > nextTimeForUpkeep) {
			timeForUpkeep = true;
		}

		if(System.currentTimeMillis() > nextTimeToSavePointsToDB) {
			timeToSaveSiegeToDB = true;
		}

		//Cycle through all sieges
		for (Siege siege : new ArrayList<>(TownyUniverse.getDataSource().getSieges())) {

			TownyMessaging.sendMsg("Now evaluating siege on " + siege.getDefendingTown().getName());

			try {
				//Apply Upkeep
				if (timeForUpkeep) {
					if (TownySettings.isUsingEconomy())
						SiegeWarUtil.applySiegeUpkeepCost(siege);
				}

				//Process active siege
				if (siege.getStatus() == SiegeStatus.IN_PROGRESS) {

					//Siege is active
					//Add siege points
					List<Town> townsWithAttackersInSiegeZone;
					townsWithAttackersInSiegeZone = SiegeWarUtil.addAttackerSiegePoints();
					SiegeWarUtil.addDefenderSiegePoints(townsWithAttackersInSiegeZone);

					//If scheduled end time has arrived, choose winner
					if(System.currentTimeMillis() > siege.getScheduledEndTime()) {
						winner = SiegeWarUtil.calculateSiegeWinner(siege);
						if (winner instanceof Town) {
							SiegeWarUtil.defenderWin(siege, (Town) winner);
						} else {
							SiegeWarUtil.attackerWin(siege, (Nation) winner);
						}

						//Save changes to db
						TownyUniverse.getDataSource().saveSiege(siege);
					} else {
						//Save changes to db
						if (timeToSaveSiegeToDB)
							TownyUniverse.getDataSource().saveSiege(siege);
					}

				} else {

					//Siege is finished.
					//Wait for siege cooldown to end then delete siege
					if (currentTime > siege.getDefendingTown().getSiegeImmunityEndTime()) {
						TownyUniverse.getDataSource().removeSiegeZone(siege);
						continue;
					}
				}

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(e.getMessage());
			}

			if (timeToSaveSiegeToDB) {
				timeToSaveSiegeToDB = false;
				nextTimeToSavePointsToDB = System.currentTimeMillis() + ONE_MINUTE_IN_MILLIS;
			}
			if (timeForUpkeep) {
				timeForUpkeep = false;
				nextTimeForUpkeep = System.currentTimeMillis() + ONE_HOUR_IN_MILLIS;
			}
		}
	}

}