package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;

/**
 * This class contains utility functions related to completing sieges
 *
 * @author Goosius
 */
public class SiegeWarSiegeCompletionUtil {

	/**
	 * This method adjusts siege values, depending on the new status, and who has won.
	 * 
	 * @param siege siege
	 * @param siegeStatus the new status of the siege
	 * @param winnerNation the winner of the siege
	 */
	public static void updateSiegeValuesToComplete(Siege siege,
												   SiegeStatus siegeStatus,
												   Nation winnerNation) {
		siege.setStatus(siegeStatus);
		siege.setActualEndTime(System.currentTimeMillis());
		siege.setAttackerWinner(winnerNation);
		SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);

		//Save to db
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		townyUniverse.getDataSource().saveTown(siege.getDefendingTown());
		for(SiegeZone siegeZone: siege.getSiegeZones().values()) {
			townyUniverse.getDataSource().saveSiegeZone(siegeZone);
		}
	}
}
