package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.Location;

/**
 * @author Goosius
 */
public class SiegeWarSpawnUtil {
	
	public static void throwErrorIfSpawnPointIsTooNearSiegeZone(Location spawnLoc) throws TownyException {
		if(TownySettings.getWarSiegeSpawningDisabledNearSieges()) {
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			int minimumAllowedDistance = TownySettings.getWarSiegeMinAllowedSpawnDistanceFromSiegeZone();

			for (SiegeZone siegeZone : townyUniverse.getSiegeZonesMap().values()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& spawnLoc.distance(siegeZone.getFlagLocation()) < minimumAllowedDistance) {

					throw new TownyException(TownySettings.getLangString("msg_err_siege_cannot_spawn_near_siege"));
				}
			}
		}
	}
}
