package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class SiegeWarBlockBreakingUtil {
	//While a siege exists, nobody can destroy the siege banner
	//Returns skipAdditionalPermChecks
	public static boolean evaluateSiegeWarBreakBlockRequest(Player player, Block block, BlockBreakEvent event)  {
		String blockTypeName = block.getType().getKey().getKey();

		if (blockTypeName.contains("banner")) {
			if (isBlockNearAnActiveSiegeBanner(block)) {
				//This is not a siege banner
				return false;
			} else {
				//This block is the banner of an active siege
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, "\"This is a siege banner. While the siege is in progress, you cannot destroy blocks near it.");
				return true;
			}
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
		Location blockLocation = block.getLocation();
		Location flagLocation;

		for (SiegeZone siegeZone : com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getSiegeZones()) {
			flagLocation = siegeZone.getFlagLocation();
			if (
				blockLocation.getWorld() == flagLocation.getWorld()
					&& (Math.abs(blockLocation.getBlockX() - flagLocation.getBlockX()) < 3)
					&& (Math.abs(blockLocation.getBlockY() - flagLocation.getBlockY()) < 3)
					&& (Math.abs(blockLocation.getBlockZ() - flagLocation.getBlockZ()) < 3)
			) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS && siegeZone.isActive()) {
					return true;
				} 
			}
		}

		//No active siege banner found near given block
		return false;
	}
}
