package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.objects.SiegeDistance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains utility functions related to calculating and validating distances
 *
 * @author Goosius
 */
public class SiegeWarDistanceUtil {

	public static List<String> worldsWithSiegeWarEnabled = null;
	public static List<String> worldsWithUndergroundBannerControlEnabled = null;

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

			if(!block.getLocation().getWorld().getName().equalsIgnoreCase(siege.getFlagLocation().getWorld().getName())) {
				continue;
			}

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
				&& SiegeWarDistanceUtil.isInSiegeZone(location, siege)) {
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
					worldsWithSiegeWarEnabled.add(Bukkit.getServer().getWorld(worldName.trim()).getName());
				}
			}
		} catch (Exception e) {
			System.out.println("Error checking if siege war is enabled in world. Check your config file.");
			return false;
		}
		return worldsWithSiegeWarEnabled.contains(worldToCheck.getName());
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
					worldsWithUndergroundBannerControlEnabled.add(Bukkit.getServer().getWorld(worldName.trim()).getName());
				}
			}
		} catch (Exception e) {
			System.out.println("Error checking if world has underground banner control enabled. Check your config file.");
			return false;
		}
		return worldsWithUndergroundBannerControlEnabled.contains(worldToCheck.getName());
	}

	public static boolean isInSiegeZone(Location location, Siege siege) {
		return areLocationsClose(location, siege.getFlagLocation(), TownySettings.getWarSiegeZoneRadiusBlocks());
	}

	public static boolean isInSiegeZone(Entity entity, Siege siege) {
		return areLocationsClose(entity.getLocation(), siege.getFlagLocation(), TownySettings.getWarSiegeZoneRadiusBlocks());
	}

	public static boolean isCloseToLeader(Player player1, Player player2) {
		return areLocationsClose(player1.getLocation(), player2.getLocation(), TownySettings.getWarSiegeLeadershipAuraRadiusBlocks());
	}

	public static boolean isInTimedPointZone(Entity entity, Siege siege) {
		return areLocationsClose(entity.getLocation(), siege.getFlagLocation(), TownySettings.getBannerControlHorizontalDistanceBlocks(), TownySettings.getBannerControlVerticalDistanceBlocks());
	}

	public static boolean areTownsClose(Town town1, Town town2, int radiusTownblocks) {
		try {
			if(town1.hasHomeBlock() && town2.hasHomeBlock()) {
				return areCoordsClose(
					town1.getHomeBlock().getWorld(),
					town1.getHomeBlock().getCoord(),
					town2.getHomeBlock().getWorld(),
					town2.getHomeBlock().getCoord(),
					radiusTownblocks
				);
			} else {
				return false;
			}
		} catch(TownyException te) {
			return false;
		}
	}

	private static boolean areCoordsClose(TownyWorld world1, Coord coord1, TownyWorld world2, Coord coord2, int radiusTownblocks) {
		if(!world1.getName().equalsIgnoreCase(world2.getName()))
			return false;

		double distanceTownblocks = Math.sqrt(Math.pow(coord1.getX() - coord2.getX(), 2) + Math.pow(coord1.getZ() - coord2.getZ(), 2));

		return distanceTownblocks < radiusTownblocks;
	}

	private static boolean areLocationsClose(Location location1, Location location2, int radius) {
		if(!location1.getWorld().getName().equalsIgnoreCase(location2.getWorld().getName()))
			return false;

		return location1.distance(location2) < radius;
	}

	private static boolean areLocationsClose(Location location1, Location location2, int maxHorizontalDistance, int maxVerticalDistance) {
		if(!location1.getWorld().getName().equalsIgnoreCase(location2.getWorld().getName()))
			return false;

		//Check horizontal distance
		double xzDistance = Math.sqrt(Math.pow(location1.getX() - location2.getX(), 2) + Math.pow(location1.getZ() - location2.getZ(), 2));
		if(xzDistance > maxHorizontalDistance)
			return false;

		//Check vertical distance
		double yDistance = Math.abs(Math.abs(location1.getY() - location2.getY()));
		if(yDistance > maxVerticalDistance)
			return false;

		return true;
	}
}
