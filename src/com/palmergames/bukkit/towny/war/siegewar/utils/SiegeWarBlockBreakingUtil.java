package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class SiegeWarBlockBreakingUtil {

	//While a siege exists, nobody can destroy the siege banner or nearby blocks
	//Returns skipAdditionalPermChecks
	public static boolean evaluateSiegeWarBreakBlockRequest(Player player, Block block, BlockBreakEvent event)  {
		if (isBlockNearAnActiveSiegeBanner(block)) {
			//This block is the banner of an active siege
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, "While the siege is in progress you cannot destroy the siege banner or the block it is attached to.");
			return true;
		} else {
			return false;
		}
	}

	private static boolean isBlockNearAnActiveSiegeBanner(Block block) {
		//Look through all siege zones
		//Note that we don't just look at the town at the given location
		//....because mayor may have unclaimed the plot after the siege started

		//Location must ne nearby
		//Siege must be in progress
		//Siege zone must be active
		Location flagLocation;

		for (SiegeZone siegeZone : com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getSiegeZones()) {
			flagLocation = siegeZone.getFlagLocation();
			
			if(
				block.getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.NORTH).getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.SOUTH).getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.EAST).getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.WEST).getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.UP).getLocation().equals(flagLocation)
				|| block.getRelative(BlockFace.DOWN).getLocation().equals(flagLocation))
			{
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS && siegeZone.isActive()) {
					return true;
				} 
			}
		}

		//No active siege banner found near given block
		return false;
	}
}
