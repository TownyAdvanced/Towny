package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.objects.SiegeDistance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains utility functions related to calculating and validating distances
 *
 * @author Goosius
 */
public class SiegeWarDistanceUtil {

	public static List<World> worldsWithSiegeWarEnabled = null;
	public static List<World> worldsWithUndergroundBannerControlEnabled = null;

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

	/**
	 * This method returns true if the given location is in an active siegezone
	 *
	 * @param location the target location
	 * @return true is location is in an active siegezone
	 */
	public static boolean isLocationInActiveSiegeZone(Location location) {
		for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
			if(siege.getStatus().isActive()
				&& location.getWorld() == siege.getFlagLocation().getWorld()
				&& location.distance(siege.getFlagLocation()) < TownySettings.getWarSiegeZoneRadiusBlocks()) {
				return true;
			}
		}
		return false;
	}

    /**
     * This method determines if a location has an air block above it
     *
     * @param location the location
     * @return true if location has an air block above it
     */
    public static boolean doesLocationHaveANonAirBlockAboveIt(Location location) {
        location.add(0,1,0);

        while(location.getY() < 256)
        {
            if(!(location.getBlock().getType() == Material.AIR || location.getBlock().getType() == Material.CAVE_AIR || location.getBlock().getType() == Material.VOID_AIR))
            {
                return true;   //There is a non-air block above them
            }
            location.add(0,1,0);
        }
        return false;  //There is nothing but air above them
    }

	/**
	 * This method determines if a siegewar is enabled in the given world
	 *
	 * @param worldToCheck the world to check
	 * @return true if siegewar is enabled in the given world
	 */
	public static boolean isSiegeWarEnabledInWorld(World worldToCheck) {
		try {
			if (worldsWithSiegeWarEnabled == null) {
				worldsWithSiegeWarEnabled = new ArrayList<>();
				String[] worldNamesAsArray = TownySettings.getWarSiegeWorlds().split(",");
				for (String worldName : worldNamesAsArray) {
					worldsWithSiegeWarEnabled.add(Bukkit.getServer().getWorld(worldName.trim()));
				}
			}
		} catch (Exception e) {
			System.out.println("Error checking if siege war is enabled in world. Check your config file.");
			return false;
		}
		return worldsWithSiegeWarEnabled.contains(worldToCheck);
	}

	/**
	 * This method determines if underground banner control is enabled in the given world
	 *
	 * @param worldToCheck the world to check
	 * @return true if underground banner control is enabled in the given world
	 */
	public static boolean isUndergroundBannerControlEnabledInWorld(World worldToCheck) {
		try {
			if (worldsWithUndergroundBannerControlEnabled == null) {
				worldsWithUndergroundBannerControlEnabled = new ArrayList<>();
				String[] worldNamesAsArray = TownySettings.getWarWorldsWithUndergroundBannerControl().split(",");
				for (String worldName : worldNamesAsArray) {
					worldsWithUndergroundBannerControlEnabled.add(Bukkit.getServer().getWorld(worldName.trim()));
				}
			}
		} catch (Exception e) {
			System.out.println("Error checking if world has underground banner control enabled. Check your config file.");
			return false;
		}
		return worldsWithUndergroundBannerControlEnabled.contains(worldToCheck);
	}
}
