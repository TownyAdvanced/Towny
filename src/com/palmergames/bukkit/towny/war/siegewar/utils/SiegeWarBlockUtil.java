package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
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
			if(!(location.getBlock().getType() == Material.AIR || location.getBlock().getType() == Material.CAVE_AIR || location.getBlock().getType() == Material.VOID_AIR))
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
	 * 	First look at the material of both the target block and the block above it.
	 * 	Return false if neither is a standing banner.
	 * 	
	 * 	Then look at all siege zones within 'in progress' sieges,
	 * 	and determine if the target block or block above it is a siege banner.
	 * 	
	 * 	Note that we don't try to look at the nearby townblocks to find nearby siege zones,
	 * 	....because mayor may have unclaimed townblocks after the siege started.
	 *
	 * @param block the block to be considered
	 * @return true if the block is near an active siege banner
	 */
	public static boolean isBlockNearAnActiveSiegeBanner(Block block) {
		
		//If either the target block or block above it is a standing coloured banner, continue, else return false
		if(isStandingColouredBanner(block) || isStandingColouredBanner(block.getRelative(BlockFace.UP))) {
			
			//Look through all siege zones
			Location locationOfBlock = block.getLocation();
			Location locationOfBlockAbove = block.getRelative(BlockFace.UP).getLocation();
			Location locationOfSiegeBanner;
			TownyUniverse universe = TownyUniverse.getInstance();
			for (Siege siege : universe.getDataSource().getSieges()) {

				if (siege.getStatus() != SiegeStatus.IN_PROGRESS) {
					continue;
				}

				locationOfSiegeBanner = siege.getFlagLocation();
				if (locationOfBlock.equals(locationOfSiegeBanner) || locationOfBlockAbove.equals(locationOfSiegeBanner)) {
					return true;
				}
			}
		}
		
		//No active siege banner found near given block
		return false;
	}

	static Location getSurfaceLocation(Location topLocation) {
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
	
	private static boolean isStandingColouredBanner(Block block) {
		switch (block.getType()) {
			case BLACK_BANNER:
			case BLUE_BANNER:
			case BROWN_BANNER:
			case CYAN_BANNER:
			case GRAY_BANNER:
			case GREEN_BANNER:
			case LIGHT_BLUE_BANNER:
			case LIGHT_GRAY_BANNER:
			case LIME_BANNER:
			case MAGENTA_BANNER:
			case ORANGE_BANNER:
			case PINK_BANNER:
			case PURPLE_BANNER:
			case RED_BANNER:
			case YELLOW_BANNER:
				return true;
			case WHITE_BANNER:
				return ((Banner) block.getState()).getPatterns().size() > 0;
			default:
				return false;
		}
	}

	/**
	 * This method determines if the supporting block is unstable (e.g. sand,gravel)
	 *
	 * @param block the block
	 * @return true if support block is unstable
	 */
	public static boolean isSupportBlockUnstable(Block block) {
		Block blockBelowBanner = block.getRelative(BlockFace.DOWN);
		switch(blockBelowBanner.getType()){
			case AIR:
			case CAVE_AIR:
			case VOID_AIR:
			case GRAVEL:
			case SAND:
			case SOUL_SAND:
			case RED_SAND:
			case ACACIA_LOG:
			case BIRCH_LOG:
			case DARK_OAK_LOG:
			case JUNGLE_LOG:
			case OAK_LOG:
			case SPRUCE_LOG:
			case STRIPPED_ACACIA_LOG:
			case STRIPPED_BIRCH_LOG:
			case STRIPPED_DARK_OAK_LOG:
			case STRIPPED_JUNGLE_LOG:
			case STRIPPED_OAK_LOG:
			case STRIPPED_SPRUCE_LOG:
			case CACTUS:
				return true;
			default:
				return false;
		}
	}
}
