package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;

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
	 */
	public static void updateSiegeValuesToComplete(Siege siege,
												   SiegeStatus siegeStatus) {
		//Update values
		siege.setStatus(siegeStatus);
		siege.setActualEndTime(System.currentTimeMillis());
		SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);
		if(siegeStatus == SiegeStatus.DEFENDER_SURRENDER || siegeStatus == SiegeStatus.ATTACKER_WIN) {
			SiegeWarTimeUtil.activateRevoltImmunityTimer(siege.getDefendingTown()); //Prevent immediate revolt
		}

		//Save to db
		TownyUniverse.getInstance().getDataSource().saveSiege(siege);
		TownyUniverse.getInstance().getDataSource().saveTown(siege.getDefendingTown());
	}
}
