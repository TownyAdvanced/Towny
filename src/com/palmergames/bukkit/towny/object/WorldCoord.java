package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Objects;

public class WorldCoord extends Coord {

	private final String worldName;

	public WorldCoord(String worldName, int x, int z) {
		super(x, z);
		this.worldName = worldName;
	}

	public WorldCoord(String worldName, Coord coord) {
		super(coord);
		this.worldName = worldName;
	}

	public WorldCoord(WorldCoord worldCoord) {
		super(worldCoord);
		this.worldName = worldCoord.getWorldName();
	}

	public String getWorldName() {
		return worldName;
	}

	public Coord getCoord() {
		return new Coord(getX(), getZ());
	}

	@Deprecated
	public TownyWorld getWorld() throws NotRegisteredException {
		return getTownyWorld();
	}

	@Deprecated
	public WorldCoord(TownyWorld world, int x, int z) {
		super(x, z);
		this.worldName = world.getName();
	}

	@Deprecated
	public WorldCoord(TownyWorld world, Coord coord) {
		super(coord);
		this.worldName = world.getName();
	}

	public static WorldCoord parseWorldCoord(Entity entity) {
		return parseWorldCoord(entity.getLocation());
	}

	public static WorldCoord parseWorldCoord(String worldName, int blockX, int blockZ) {
		return new WorldCoord(worldName, toCell(blockX), toCell(blockZ));
	}
	
	public static WorldCoord parseWorldCoord(Location loc) {
		return parseWorldCoord(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
	}

	public static WorldCoord parseWorldCoord(Block block) {
		return parseWorldCoord(block.getWorld().getName(), block.getX(), block.getZ());
	}

	public WorldCoord add(int xOffset, int zOffset) {

		return new WorldCoord(getWorldName(), getX() + xOffset, getZ() + zOffset);
	}

	@Override
	public int hashCode() {

		int hash = 17;
		hash = hash * 27 + (worldName == null ? 0 : worldName.hashCode());
		hash = hash * 27 + getX();
		hash = hash * 27 + getZ();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Coord))
			return false;

		if (!(obj instanceof WorldCoord)) {
			Coord that = (Coord) obj;
			return this.getX() == that.getZ() && this.getZ() == that.getZ();
		}

		WorldCoord that = (WorldCoord) obj;
		return this.getX() == that.getX() && this.getZ() == that.getZ() && (Objects.equals(this.worldName, that.worldName));
	}

	@Override
	public String toString() {
		return worldName + "," + super.toString();
	}

	/**
	 * Shortcut for Bukkit.getWorld(worldName)
	 * 
	 * @return the relevant org.bukkit.World instance
	 */
	public World getBukkitWorld() {
		return Bukkit.getWorld(worldName);
	}

	/**
	 * Shortcut for TownyUniverse.getDataSource().getWorld(worldName)
	 * 
	 * @return the relevant TownyWorld instance
	 * @throws NotRegisteredException if unable to return a TownyWorld instance
	 */
	public TownyWorld getTownyWorld() throws NotRegisteredException {
		return TownyUniverse.getInstance().getDataSource().getWorld(worldName);
	}

	/**
	 * Shortcut for TownyUniverse.getInstance().getTownBlock(WorldCoord).
	 * 
	 * @return the relevant TownBlock instance.
	 * @throws NotRegisteredException - If there is no TownBlock @ WorldCoord, then this exception.
	 */
	public TownBlock getTownBlock() throws NotRegisteredException {
		if (!hasTownBlock())
			throw new NotRegisteredException();
		return TownyUniverse.getInstance().getTownBlock(this);
	}
	
	public boolean hasTownBlock() {
		return TownyUniverse.getInstance().hasTownBlock(this);
	}

	/**
	 * Checks that locations are in different cells without allocating any garbage to the heap.
	 * 
	 * @param from Original location
	 * @param to Next location
	 * @return whether the locations are in different cells
	 */
	public static boolean cellChanged(Location from, Location to) {
		return toCell(from.getBlockX()) != toCell(to.getBlockX()) ||
			   toCell(from.getBlockZ()) != toCell(to.getBlockZ()) ||
			   !Objects.equals(from.getWorld(), to.getWorld());
	}
}
