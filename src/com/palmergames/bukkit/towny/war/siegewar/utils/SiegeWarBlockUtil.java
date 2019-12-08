package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains utility functions related to blocks 
 * (e.g. placing/breaking/analysing nearby blocks)
 *
 * @author Goosius
 */
public class SiegeWarBlockUtil {

	/**
	 * This method gets a list of adjacent townblocks, either N, S, E or W.
	 * 
	 * @param player the player
	 * @param block the block to start from
	 * @return list of nearby townblocks
	 */
	public static List<TownBlock> getAdjacentTownBlocks(Player player, Block block) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld;
		List<TownBlock> nearbyTownBlocks = new ArrayList<>();

		try {
			townyWorld = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e) {
			return nearbyTownBlocks;
		}

		List<Coord> nearbyCoOrdinates =new ArrayList<>();
		Coord blockCoordinate = Coord.parseCoord(block);
		nearbyCoOrdinates.add(blockCoordinate.add(0,-1));
		nearbyCoOrdinates.add(blockCoordinate.add(0,1));
		nearbyCoOrdinates.add(blockCoordinate.add(1,0));
		nearbyCoOrdinates.add(blockCoordinate.add(-1,0));

		TownBlock nearbyTownBlock = null;
		for(Coord nearbyCoord: nearbyCoOrdinates){
			if(townyWorld.hasTownBlock(nearbyCoord)) {

				try {nearbyTownBlock = townyWorld.getTownBlock(nearbyCoord);
				} catch (NotRegisteredException e) {}

				if (nearbyTownBlock.hasTown()) {
					nearbyTownBlocks.add(nearbyTownBlock);
				}
			}
		}

		return nearbyTownBlocks;
	}

	/**
	 * This method determines if the player has an air block above them
	 *
	 * @param player the player
	 * @return true if player has an air block above them
	 */
	public static boolean doesPlayerHaveANonAirBlockAboveThem(Player player) {
		return doesLocationHaveANonAirBlockAboveIt(player.getLocation());
	}

	/**
	 * This method determines if a block has an air block above it
	 *
	 * @param block the block
	 * @return true if block has an air block above it
	 */
	public static boolean doesBlockHaveANonAirBlockAboveIt(Block block) {
		return doesLocationHaveANonAirBlockAboveIt(block.getLocation());
	}
	
	private static boolean doesLocationHaveANonAirBlockAboveIt(Location location) {
		location.add(0,1,0);

		while(location.getY() < 256)
		{
			if(location.getBlock().getType() != Material.AIR)
			{
				return true;   //There is a non-air block above them
			}
			location.add(0,1,0);
		}
		return false;  //There is nothing but air above them
	}

	/**
	 * 	Determine if the block is an active siege banner, or the support block.
	 * 	
	 * 	First look at the material of both the target block & the block above it.
	 * 	Return false if neither is a standing banner.
	 * 	
	 * 	Then look at all siege zones within 'in progress' sieges,
	 * 	and determine if the target block or block above it is a siege banner.
	 * 	
	 * 	Note that we don't try to look at the nearby townblocks to find the nearby siege zone,
	 * 	....because mayor may have unclaimed townblocks after the siege started.
	 *
	 * @param block the block to be considered
	 * @return true if the block is near an active siege banner
	 */
	public static boolean isBlockNearAnActiveSiegeBanner(Block block) {
		Block blockAbove = block.getRelative(BlockFace.UP);
		
		//If neither the target block nor block above it is a banner, return false
		if(!block.getType().name().endsWith("BANNER") && !blockAbove.getType().name().endsWith("BANNER"))
			return false;

		//If either the target block or block above it is a wall banner, return false
		if(block.getType().name().contains("WALL") || blockAbove.getType().name().contains("WALL"))
			return false;
		
		//Look through all siege zones
		Location locationOfBlock = block.getLocation();
		Location locationOfBlockAbove = blockAbove.getLocation();
		Location locationOfSiegeBanner;
		TownyUniverse universe = TownyUniverse.getInstance();
		for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {

			if (siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS) {
				continue;
			}

			locationOfSiegeBanner = siegeZone.getFlagLocation();
			if(locationOfBlock.equals(locationOfSiegeBanner) || locationOfBlockAbove.equals(locationOfSiegeBanner))
			{
				return true;	
			}
		}
		
		//No active siege banner found near given block
		return false;
	}

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
	
	private static Location getSurfaceLocation(Location topLocation) {
		topLocation.add(0,-1,0);

		while(topLocation.getY() < 256)
		{
			if(topLocation.getBlock().getType() != Material.AIR)
			{
				return topLocation;
			}
			topLocation.add(0,-1,0);
		}
		
		topLocation.setY(255); //This would only occur if it was air all the way down.....unlikely but ok, just in case
		return topLocation;
	}
}
