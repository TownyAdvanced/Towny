package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

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
	protected Chunk chunk;
	protected World world;

	public BlockLocation(Location loc) {

		this.x = loc.getBlockX();
		this.z = loc.getBlockZ();
		this.y = loc.getBlockY();
		this.chunk = loc.getChunk();
		this.world = loc.getWorld();
	}

	public Chunk getChunk() {

		return chunk;
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

		if ((loc.getWorld() == getWorld()) && (loc.getBlockX() == getX()) && (loc.getBlockY() == getY()) && (loc.getBlockZ() == getZ()))
			return true;

		return false;
	}

	public Location getLocation() {
		return new Location(world, x, y, z);
	}

	public Block getBlock() {
		return world.getBlockAt(getLocation());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BlockLocation that = (BlockLocation) o;
		return x == that.x && z == that.z && y == that.y && Objects.equals(chunk, that.chunk) && Objects.equals(world, that.world);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + x;
		hash = 31 * hash + y;
		hash = 31 * hash + z;
		hash = hash * 31 + (chunk != null ? chunk.hashCode() : 0);
		hash = hash * 31 + (world != null ? world.hashCode() : 0);
		return hash;
	}


}
