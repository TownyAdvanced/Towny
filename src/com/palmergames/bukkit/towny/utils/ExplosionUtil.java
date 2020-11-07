package com.palmergames.bukkit.towny.utils;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.eventwar.War;

public class ExplosionUtil {

	public ExplosionUtil() {
		
	}
	
	/**
	 * Test if this location has explosions enabled.
	 * 
	 * @param loc - Location to check
	 * @return true if allowed.
	 */
	public static boolean locationCanExplode(Location loc) {
		
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(loc.getWorld().getName());
		if (world == null)
			return false;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (TownyAPI.getInstance().isWilderness(loc)) {
			if (world.isForceExpl() || world.isExpl())
				return true;
			if (!world.isExpl())
				return false;				
		}

		/*
		 * Must be inside of a town.
		 */
		Coord coord = Coord.parseCoord(loc);

		/*
		 * Stops any type of exploding damage and block damage if wars are not allowing explosions.
		 */
		if (world.isWarZone(coord) && !WarZoneConfig.isAllowingExplosionsInWarZone()) {
			return false;
		}

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(loc);
		Town town = TownyAPI.getInstance().getTown(loc);

		if (TownyAPI.getInstance().isWarTime() && WarZoneConfig.explosionsBreakBlocksInWarZone() && War.isWarZone(townBlock.getWorldCoord())){
			return true;				
		}
		if ((!townBlock.getPermissions().explosion) || (TownyAPI.getInstance().isWarTime() && WarZoneConfig.isAllowingExplosionsInWarZone() && !town.hasNation() && !town.isBANG()))
			return false;

		
		return true;
	}
}
