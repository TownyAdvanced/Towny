package com.palmergames.bukkit.towny.object;

import org.bukkit.Location;

/**
 * A worldname, x, y, z location used to validate SpawnPoints. 
 */
public class SpawnPointLocation {
	private final String world; 
	private final int x;
	private final int y;
	private final int z;
	
	public SpawnPointLocation(Location loc) {
		this.world = loc.getWorld().getName();
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
	}

	public String getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}
	
	public static SpawnPointLocation parseSpawnPointLocaiton(Location loc) {
		return new SpawnPointLocation(loc);
	}
}
