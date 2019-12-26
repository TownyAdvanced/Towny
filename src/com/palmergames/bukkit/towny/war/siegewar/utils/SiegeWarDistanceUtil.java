package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZoneDistance;
import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * This class contains utility functions related to calculating and validating distances
 *
 * @author Goosius
 */
public class SiegeWarDistanceUtil {

	/**
	 * This method determines if the difference in elevation between a (attack banner) block, 
	 * and the average height of a town block,
	 * is acceptable,
	 * 
	 * The allowed limit is configurable.
	 * 
	 * @param block the attack banner
	 * @param townBlock the town block
	 * @return true if the difference in elevation is acceptable
	 */
	public static boolean isBannerToTownElevationDifferenceOk(Block block, TownBlock townBlock) {
		int allowedDownwardElevationDifference = TownySettings.getWarSiegeMaxAllowedBannerToTownDownwardElevationDifference();
		int averageDownwardElevationDifference = getAverageBlockToTownDownwardElevationDistance(block, townBlock);
		return averageDownwardElevationDifference <= allowedDownwardElevationDifference;
	}
	
	private static int getAverageBlockToTownDownwardElevationDistance(Block block, TownBlock townBlock) {
		int blockElevation = block.getY();
		
		Location topNorthWestCornerLocation = townBlock.getCoord().getTopNorthWestCornerLocation(block.getWorld());
		int townBlockSize = TownySettings.getTownBlockSize();
		Location[] surfaceCornerLocations = new Location[4];
		surfaceCornerLocations[0] = SiegeWarBlockUtil.getSurfaceLocation(topNorthWestCornerLocation);
		surfaceCornerLocations[1] = SiegeWarBlockUtil.getSurfaceLocation(topNorthWestCornerLocation.add(townBlockSize,0,0));
		surfaceCornerLocations[2] = SiegeWarBlockUtil.getSurfaceLocation(topNorthWestCornerLocation.add(0,0,townBlockSize));
		surfaceCornerLocations[3] = SiegeWarBlockUtil.getSurfaceLocation(topNorthWestCornerLocation.add(townBlockSize,0,townBlockSize));
		
		int totalElevation = 0;
		for(Location surfaceCornerLocation: surfaceCornerLocations) {
			totalElevation += surfaceCornerLocation.getBlockY();
		}
		int averageTownElevation = totalElevation / 4;
		
		return blockElevation - averageTownElevation;
	}

	/**
	 * This method finds the nearest siegezone to the given block, 
	 *
	 * @param block the given block
	 * @return a SiegeZoneDistance object containing both the zone and distance. Null if not found.
	 */
	public static SiegeZoneDistance findNearestSiegeZoneDistance(Block block) {
		//Find the nearest siege zone to the given block
		SiegeZone nearestSiegeZone = null;
		double distanceToNearestSiegeZone = -1;
		for(SiegeZone siegeZone: TownyUniverse.getInstance().getDataSource().getSiegeZones()) {

			if (nearestSiegeZone == null) {
				nearestSiegeZone = siegeZone;
				distanceToNearestSiegeZone = block.getLocation().distance(nearestSiegeZone.getFlagLocation());
			} else {
				double distanceToNewTarget = block.getLocation().distance(siegeZone.getFlagLocation());
				if(distanceToNewTarget < distanceToNearestSiegeZone) {
					nearestSiegeZone = siegeZone;
					distanceToNearestSiegeZone = distanceToNewTarget;
				}
			}
		}
	
		if(nearestSiegeZone == null) {
			return null;
		} else {
			return new SiegeZoneDistance(nearestSiegeZone, distanceToNearestSiegeZone);
		}
	}
	
}
