package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;

/**
 * @author Goosius
 */
public class SiegeWarDbUtil {

	public static void updateAndSaveSiegeCompletionValues(Siege siege,
														  SiegeStatus siegeStatus,
														  Nation winnerNation) {
		siege.setStatus(siegeStatus);
		siege.setActualEndTime(System.currentTimeMillis());
		siege.setAttackerWinner(winnerNation);
		SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);

		//Save to db
		TownyUniverse.getDataSource().saveTown(siege.getDefendingTown());
		for(SiegeZone siegeZone: siege.getSiegeZones().values()) {
			TownyUniverse.getDataSource().saveSiegeZone(siegeZone);
		}
	}
}
