package com.palmergames.bukkit.towny.object;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

/**
 * A class to hold and calculate coordinates in a grid according to the size
 * defined in
 * the static field size.
 * 
 * @author Shade
 */
public class Coord {

	protected static int cellSize = 16;
	protected int x, z;

	public Coord(int x, int z) {

		this.x = x;
		this.z = z;
	}

	public Coord(Coord coord) {

		this.x = coord.getX();
		this.z = coord.getZ();
	}

	public int getX() {

		return x;
	}

	public void setX(int x) {

		this.x = x;
	}

	public int getZ() {

		return z;
	}

	public void setZ(int z) {

		this.z = z;
	}

	public Coord add(int xOffset, int zOffset) {

		return new Coord(getX() + xOffset, getZ() + zOffset);
	}

	@Override
	public int hashCode() {

		int result = 17;
		result = 27 * result + x;
		result = 27 * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this)
			return true;
		if (!(obj instanceof Coord))
			return false;

		Coord o = (Coord) obj;
		return this.x == o.x && this.z == o.z;
	}

	/*
	 * OLD METHOD
	 * 
	 * public static Coord parseCoord(int x, int z) {
	 * return new Coord(x / getCellSize() - (x < 0 ? 1 : 0), z / getCellSize() -
	 * (z < 0 ? 1 : 0));
	 * }
	 */

	/**
	 * Convert a value to the grid cell counterpart
	 * @param value x/z integer
	 * @return cell position
	 */
	protected static int toCell(int value) {
		// Floor divides means that for negative values will round to the next negative value
		// and positive value to the previous positive value.
		return Math.floorDiv(value, getCellSize());
	}

	/**
	 * Convert regular grid coordinates to their grid cell's counterparts.
	 * 
	 * @param x - X int (Coordinates)
	 * @param z - Z int (Coordinates)
	 * @return a new instance of Coord.
	 * 
	 */
	public static Coord parseCoord(int x, int z) {
		return new Coord(toCell(x), toCell(z));
	}

	public static Coord parseCoord(Entity entity) {

		return parseCoord(entity.getLocation());
	}

	public static Coord parseCoord(Location loc) {

		return parseCoord(loc.getBlockX(), loc.getBlockZ());
	}

	public static Coord parseCoord(Block block) {

		return parseCoord(block.getX(), block.getZ());
	}

	@Override
	public String toString() {

		return getX() + "," + getZ();
	}

	public static void setCellSize(int cellSize) {

		Coord.cellSize = cellSize;
	}

	public static int getCellSize() {

		return cellSize;
	}

	/**
	 * Get the Location object representing the "top north west corner" of this co-ord
	 * 
	 * @param world the world
	 * @return the corner location
	 */
	public Location getTopNorthWestCornerLocation(World world) {
		int locX = this.x * getCellSize();
		int locZ = this.z * getCellSize();
		return new Location(world, locX, 255, locZ);
	}
}
