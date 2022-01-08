package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

import io.papermc.lib.PaperLib;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

	@Nullable
	public TownyWorld getTownyWorldOrNull() {
		return TownyAPI.getInstance().getTownyWorld(worldName);
	}
	
	/**
	 * Shortcut for TownyUniverse.getInstance().getTownBlock(WorldCoord).
	 * 
	 * @return the relevant TownBlock instance.
	 * @throws NotRegisteredException If there is no TownBlock at this WorldCoord.
	 */
	public TownBlock getTownBlock() throws NotRegisteredException {
		if (!hasTownBlock())
			throw new NotRegisteredException();
		return TownyUniverse.getInstance().getTownBlock(this);
	}
	
	/**
	 * Relatively safe to use if {@link #hasTownBlock()} has already been
	 * checked and returned true.
	 * 
	 * @return TownBlock at this WorldCoord or null;
	 */
	@Nullable
	public TownBlock getTownBlockOrNull() {
		return TownyUniverse.getInstance().getTownBlockOrNull(this);
	}
	
	public boolean hasTownBlock() {
		return TownyUniverse.getInstance().hasTownBlock(this);
	}

	/**
	 * Identical to !{@link WorldCoord#hasTownBlock()}, but is better readable.
	 * @return Whether this townblock is not claimed.
	 */
	public boolean isWilderness() {
		return !hasTownBlock();
	}

	/**
	 * Returns a list of Chunks, in which the WorldCoord exists.
	 * When the server town_block_size is less than or equal to 16 this list will be a single Chunk.
	 * When the server town_block_size is greater than 16 the list will contain more than one Chunk.
	 * 
	 * @return List of Chunks.
	 */
	public List<Chunk> getChunks() {
		List<Chunk> chunks = new ArrayList<>();
		if (getCellSize() > 16) {
			// Dealing with a townblocksize greater than 16, we will have multiple chunks per WorldCoord.
			int side = Math.round(getCellSize() / 16);
			for (int x = 0; x <= side; x++) {
				for (int z = 0; z <= side; z++) {
					PaperLib.getChunkAtAsync(getSubCorner(x, z)).thenAccept(chunk -> chunks.add(chunk));
				}
			}
		} else {
			PaperLib.getChunkAtAsync(getCorner()).thenAccept(chunk -> chunks.add(chunk));
		}
		return chunks;
	}
	
	// Used to get a location at the corner of a WorldCoord.
	private Location getCorner() {
		Location loc = new Location(getBukkitWorld(), getX() * getCellSize(), 0, getZ() * getCellSize());
		return loc;
	}
	
	// Used to get a location representing sub coordinates of a WorldCoord, to ease the lookup of a corresponding Chunk. 
	private Location getSubCorner(int x, int z) {
		return getCorner().add(x * 16, 0, z * 16);
	}

	/**
	 * Relatively safe to use if {@link #hasTownBlock()} has already been used.
	 * @return Town at this WorldCoord or null;
	 */
	@Nullable
	public Town getTownOrNull() {
		if (hasTownBlock())
			return getTownBlockOrNull().getTownOrNull();
		return null;
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
