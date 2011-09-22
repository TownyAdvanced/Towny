package com.palmergames.bukkit.towny.object;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * A class to hold basic block location data
 * 
 * @author ElgarL
 */
public class BlockLocation {
	public void setY(int y) {
		this.y = y;
	}

	protected int x, z, y;
	protected World world;
	
	public BlockLocation(Location loc) {
		this.x = loc.getBlockX();
		this.z = loc.getBlockZ();
		this.y = loc.getBlockY();
		this.world = loc.getWorld();
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	public int getY() {
		return y;
	}

	public World getWorld() {
		return world;
	}
	
	public boolean isLocation(Location loc) {
		
		if ((loc.getWorld() == getWorld())
			&& (loc.getBlockX() == getX())
			&& (loc.getBlockY() == getY())
			&& (loc.getBlockZ() == getZ()))
			return true;
		
		return false;
	}
	
public boolean isLocation(BlockLocation blockLocation) {
		
		if ((blockLocation.getWorld() == getWorld())
			&& (blockLocation.getX() == getX())
			&& (blockLocation.getY() == getY())
			&& (blockLocation.getZ() == getZ()))
			return true;
		
		return false;
	}
	
	
	
}
	