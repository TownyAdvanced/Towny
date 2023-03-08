package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
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
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WorldCoord extends Coord {

	private final String worldName;
	private UUID worldUUID;
	private Reference<World> worldRef = new WeakReference<>(null);

	public WorldCoord(String worldName, int x, int z) {
		super(x, z);
		this.worldName = worldName;
		
		World world = Bukkit.getServer().getWorld(worldName);
		if (world != null)
			this.worldUUID = world.getUID();
	}

	public WorldCoord(String worldName, Coord coord) {
		this(worldName, coord.getX(), coord.getZ());
	}

	public WorldCoord(String worldName, UUID worldUUID, int x, int z) {
		super(x, z);
		this.worldName = worldName;
		this.worldUUID = worldUUID;
	}

	public WorldCoord(String worldName, UUID worldUUID, Coord coord) {
		this(worldName, worldUUID, coord.getX(), coord.getZ());
	}

	public WorldCoord(@NotNull World world, int x, int z) {
		super(x, z);
		this.worldName = world.getName();
		this.worldUUID = world.getUID();
	}

	public WorldCoord(@NotNull World world, Coord coord) {
		this(world, coord.getX(), coord.getZ());
	}

	public WorldCoord(WorldCoord worldCoord) {
		super(worldCoord);
		this.worldName = worldCoord.getWorldName();
		this.worldUUID = worldCoord.worldUUID;
		this.worldRef = new WeakReference<>(worldCoord.worldRef.get());
	}

	public String getWorldName() {
		return this.worldName;
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
		return new WorldCoord(loc.getWorld(), toCell(loc.getBlockX()), toCell(loc.getBlockZ()));
	}

	public static WorldCoord parseWorldCoord(Block block) {
		return parseWorldCoord(block.getLocation());
	}

	public WorldCoord add(int xOffset, int zOffset) {
		return new WorldCoord(getWorldName(), worldUUID, getX() + xOffset, getZ() + zOffset);
	}

	@Override
	public int hashCode() {

		int hash = 17;
		hash = hash * 27 + this.worldName.hashCode();
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
		return this.getX() == that.getX() && this.getZ() == that.getZ() && (Objects.equals(this.getWorldName(), that.getWorldName()));
	}

	@Override
	public String toString() {
		return getWorldName() + "," + super.toString();
	}

	/**
	 * Shortcut for Bukkit.getWorld(worldName)
	 * 
	 * @return the relevant {@link World} instance
	 */
	@Nullable
	public World getBukkitWorld() {
		World world = worldRef.get();
		if (world == null) {
			world = Bukkit.getServer().getWorld(this.worldName);
			worldRef = new WeakReference<>(world);
		}
		
		return world;
	}

	/**
	 * @return the relevant TownyWorld instance or null.
	 */
	@Nullable
	public TownyWorld getTownyWorld() {
		return TownyAPI.getInstance().getTownyWorld(this.worldName);
	}

	/**
	 * @deprecated as of 0.98.4.9, please use {@link #getTownyWorld()} instead.
	 */
	@Nullable
	@Deprecated
	public TownyWorld getTownyWorldOrNull() {
		return TownyAPI.getInstance().getTownyWorld(this.worldName);
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
	
	public boolean hasTown(Town town) {
		return hasTownBlock() && getTownOrNull().equals(town);
	}

	/**
	 * Identical to !{@link WorldCoord#hasTownBlock()}, but is better readable.
	 * @return Whether this townblock is not claimed.
	 */
	public boolean isWilderness() {
		return !hasTownBlock();
	}

	/**
	 * Loads the chunks represented by a WorldCoord. Creates a PluginChunkTicket so
	 * that the WorldCoord will remain loaded, even when no players are present.
	 * <p>
	 * Uses PaperLib's getChunkAtAsync when Paper is present.
	 */
	public void loadChunks() {
		Towny plugin = Towny.getPlugin();
		if (!Bukkit.isPrimaryThread())
			Bukkit.getScheduler().runTask(plugin, () -> loadChunks(plugin));
		else
			loadChunks(plugin);
	}

	private void loadChunks(Towny plugin) {
		getChunks().forEach(future -> future.thenAccept(chunk -> chunk.addPluginChunkTicket(plugin)));
	}
	
	/**
	 * Unloads the chunks presented by a WorldCoord. Removes a PluginChunkTicket so
	 * that the WorldCoord will no longer remain loaded.
	 * <p> 
	 * Uses PaperLib's getChunkAtAsync when Paper is present.
	 */
	public void unloadChunks() {
		Towny plugin = Towny.getPlugin();
		if (!Bukkit.isPrimaryThread())
			Bukkit.getScheduler().runTask(plugin, () -> unloadChunks(plugin));
		else
			unloadChunks(plugin);
	}

	private void unloadChunks(Towny plugin) {
		getChunks().forEach(future -> future.thenAccept(chunk -> chunk.addPluginChunkTicket(plugin)));
	}

	/**
	 * Loads and returns the chunk(s) inside this WorldCoord.
	 * <p>
	 * Chunks are loaded async on servers using paper.
	 * @return An unmodifiable collection of chunk futures.
	 */
	@Unmodifiable
	public Collection<CompletableFuture<Chunk>> getChunks() {
		if (getCellSize() > 16) {
			// Dealing with a townblocksize greater than 16, we will have multiple chunks per WorldCoord.
			final Set<CompletableFuture<Chunk>> chunkFutures = new HashSet<>();
			
			int side = Math.round(getCellSize() / 16f);
			for (int x = 0; x <= side; x++) {
				for (int z = 0; z <= side; z++) {
					chunkFutures.add(PaperLib.getChunkAtAsync(getSubCorner(x, z)));
				}
			}
			
			return Collections.unmodifiableSet(chunkFutures);
		} else {
			return Collections.singleton(PaperLib.getChunkAtAsync(getCorner()));
		}
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
	 * @return Return a Bukkit bounding box containg the space of the WorldCoord.
	 */
	public BoundingBox getBoundingBox() {
		return BoundingBox.of(getLowerMostCornerLocation(), getUpperMostCornerLocation());
	}
	
	/**
	 * @return Location of the lower-most corner of a WorldCoord.
	 */
	public Location getLowerMostCornerLocation() {
		return new Location(getBukkitWorld(), getX() * getCellSize(), getBukkitWorld().getMinHeight(), getZ() * getCellSize());
	}
	
	/**
	 * @return Location of the upper-most corner of a WorldCoord.
	 */
	public Location getUpperMostCornerLocation() {
		return getCorner().add(getCellSize(), getBukkitWorld().getMaxHeight(), getCellSize());
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

	public List<WorldCoord> getCardinallyAdjacentWorldCoords(boolean... includeOrdinalFlag) {
		boolean includeOrdinal = (includeOrdinalFlag.length >= 1) ? includeOrdinalFlag[0] : false;
		List<WorldCoord> list =new ArrayList<>(includeOrdinal ? 8 : 4);
		list.add(this.add(0,-1));
		list.add(this.add(0,1));
		list.add(this.add(1,0));
		list.add(this.add(-1,0));
		if (includeOrdinal) {
			list.add(this.add(1,1));
			list.add(this.add(1,-1));
			list.add(this.add(-1,-1));
			list.add(this.add(-1,1));
		}
		return list;
	}
}
