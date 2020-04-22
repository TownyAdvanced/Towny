package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.objects.SiegeDistance;
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
	 * This method finds the nearest siege to the given block, 
	 *
	 * @param block the given block
	 * @return a SiegeZoneDistance object containing both the siege and distance. Null if not found.
	 */
	public static SiegeDistance findNearestSiegeDistance(Block block) {
		//Find the nearest siege zone to the given block
		Siege nearestSiege = null;
		double distanceToNearestSiegeZone = -1;
		for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {

			if (nearestSiege == null) {
				nearestSiege = siege;
				distanceToNearestSiegeZone = block.getLocation().distance(nearestSiege.getFlagLocation());
			} else {
				double distanceToNewTarget = block.getLocation().distance(siege.getFlagLocation());
				if(distanceToNewTarget < distanceToNearestSiegeZone) {
					nearestSiege = siege;
					distanceToNearestSiegeZone = distanceToNewTarget;
				}
			}
		}
	
		if(nearestSiege == null) {
			return null;
		} else {
			return new SiegeDistance(nearestSiege, distanceToNearestSiegeZone);
		}
	}
	
}
