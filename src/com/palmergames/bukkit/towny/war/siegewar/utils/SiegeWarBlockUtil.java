package com.palmergames.bukkit.towny.war.siegewar.utils;

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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Goosius
 */
public class SiegeWarBlockUtil {
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

	public static boolean doesPlayerHaveANonAirBlockAboveThem(Player player) {
		return doesLocationHaveANonAirBlockAboveIt(player.getLocation());
	}

	public static boolean doesBlockHaveANonAirBlockAboveIt(Block block) {
		return doesLocationHaveANonAirBlockAboveIt(block.getLocation());
	}

	private static boolean doesLocationHaveANonAirBlockAboveIt(Location location) {
		location = location.add(0,1,0);

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

	public static boolean isBlockNearAnActiveSiegeBanner(Block block) {
		//Look through all siege zones
		//Note that we don't just look at the town at the given location
		//....because mayor may have unclaimed the plot after the siege started

		//Siege zone must be active
		//Siege must be in progress
		//This must be the banner or the block below the banner
		Block flagBlock;
		
		for (SiegeZone siegeZone : com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getSiegeZones()) {

			if (siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS) {
				return false;
			}

			flagBlock = siegeZone.getFlagLocation().getBlock();
			if(flagBlock.equals(block) || flagBlock.getRelative(BlockFace.DOWN).equals(block))
			{
				return true;	
			}
		}
		
		//No active siege banner found near given block
		return false;
	}
}
