package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author ElgarL
 * 
 */
public class TownyRegenAPI {

	// A list of worldCoords which are to be regenerated.
	private static List<WorldCoord> regenWorldCoordList = new ArrayList<>();
	
	// table containing snapshot data of active reversions.
	private static Hashtable<String, PlotBlockData> plotChunks = new Hashtable<>();
	
	// A holder for each protection regen task
	private static final Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<>();
	
	// List of protection blocks placed to prevent blockPhysics.
	private static final Set<Block> protectionPlaceholders = new HashSet<>();

	/**
	 * Removes a TownyWorld from the various Revert-on-Unclaim feature Lists/Table.
	 * @param world TownyWorld to remove.
	 */
	public static void turnOffRevertOnUnclaimForWorld(TownyWorld world) {
		removeRegenQueueListOfWorld(world); // Remove any queued regenerations.
		removePlotChunksForWorld(world); // Stop any active reverts being done.
	}
	
	/**
	 * Called when a PlotBlockData's revert-on-unclaim has been finished. 
	 * @param plotChunk PlotBlockData which finished up.
	 */
	public static void finishPlotBlockData(PlotBlockData plotChunk) {
		TownyMessaging.sendDebugMsg("Revert on unclaim complete for " + plotChunk.getWorldName() + " " + plotChunk.getX() +"," + plotChunk.getZ());
		removeFromRegenQueueList(plotChunk.getWorldCoord()); // Remove the WorldCoord from the queue.
		removeFromActiveRegeneration(plotChunk); // Remove from the active HashTable.
		deletePlotChunkSnapshot(plotChunk); // Remove from the database.
		plotChunk.getWorldCoord().unloadChunks(); // Remove the PluginChunkTickets keeping the plotChunk loaded.
	}
	
	/*
	 * Snapshots used in Revert-On-Unclaim feature
	 */

	/**
	 * Add a TownBlocks WorldCoord for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord
	 * @deprecated Towny no longer uses a snapshot queue as of 0.98.6.25.   
	 */
	@Deprecated
	public static void addWorldCoord(WorldCoord worldCoord) {
	}
	
	/**
	 * Removes a TownBlock from having a snapshot taken.
	 * 
	 * @param worldCoord - WorldCoord of TownBlock to remove from snapshot list.
	 * @deprecated Towny no longer uses a snapshot queue as of 0.98.6.25.   
	 */
	@Deprecated
	public static void removeWorldCoord(WorldCoord worldCoord) {
	}

	/**
	 * @return true if there are any TownBlocks to be processed.
	 * @deprecated Towny no longer uses a snapshot queue as of 0.98.6.25.
	 */
	@Deprecated
	public static boolean hasWorldCoords() {
		return false;
	}

	/**
	 * Check if this WorldCoord is waiting for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord to check
	 * @return true if it's in the queue.
	 * @deprecated Towny no longer uses a snapshot queue as of 0.98.6.25.
	 */
	@Deprecated
	public static boolean hasWorldCoord(WorldCoord worldCoord) {
		return false;
	}

	/**
	 * @return First WorldCoord to be processed.
	 */
	@Deprecated
	public static WorldCoord getWorldCoord() {
		return null;
	}

	/*
	 * Regeneration Queue.
	 */

	/**
	 * @return the list of WorldCoords which are waiting to be regenerated.
	 */
	public static List<WorldCoord> getRegenQueueList() {
		return regenWorldCoordList;
	}

	/**
	 * @return whether the regenQueue is empty.
	 */
	public static boolean regenQueueHasAvailable() {
		return !regenWorldCoordList.isEmpty();
	}

	/**
	 * Used when a world has the revert on unclaim feature turned off, to purge
	 * the list of queued regenerations of the world.
	 * @param world TownyWorld to remove from the queue.
	 */
	private static void removeRegenQueueListOfWorld(@NotNull TownyWorld world) {
		regenWorldCoordList = getRegenQueueList().stream()
			.filter(wc -> !world.equals(wc.getTownyWorld()))
			.collect(Collectors.toList());
		TownyUniverse.getInstance().getDataSource().saveRegenList();
	}

	/**
	 * Removes a WorldCoord from the queue of the revert on unclaim feature.
	 * @param wc WorldCoord to add to the queue.
	 */
	public static void removeFromRegenQueueList(WorldCoord wc) {
		if (regenWorldCoordList.remove(wc))
			TownyUniverse.getInstance().getDataSource().saveRegenList();
	}

	/**
	 * Adds a WorldCoord to the queue of the revert on unclaim feature.
	 * @param wc WorldCoord to remove from thequeue.
	 * @param save true to save the regenlist.
	 */
	public static void addToRegenQueueList(WorldCoord wc, boolean save) {
		if (regenWorldCoordList.contains(wc))
			return;
		regenWorldCoordList.add(wc);
		if (save)
			TownyUniverse.getInstance().getDataSource().saveRegenList();
	}

	public static  void getWorldCoordFromQueueForRegeneration() {
		for (WorldCoord wc : new ArrayList<>(TownyRegenAPI.getRegenQueueList())) {
			// We have enough plot chunks regenerating, break out of the loop.
			if (getPlotChunks().size() >= 20)
				break;
			// We have already got this worldcoord regenerating.
			if (hasActiveRegeneration(wc))
				continue;
			
			// This worldCoord isn't actively regenerating, start the regeneration.
			PlotBlockData plotData = getPlotChunkSnapshot(new TownBlock(wc.getX(), wc.getZ(), wc.getTownyWorld()));
			if (plotData != null) {
				// Load the chunks.
				plotData.getWorldCoord().loadChunks();
				addToActiveRegeneration(plotData);
				TownyMessaging.sendDebugMsg("Revert on unclaim beginning for " + plotData.getWorldName() + " " + plotData.getX() +"," + plotData.getZ());
			} else {
				removeFromRegenQueueList(wc);
			}
		}
	}
	
	/*
	 * Active Revert-On-Unclaims.
	 */

	/**
	 * @return the plotChunks which are being processed
	 */
	public static Hashtable<String, PlotBlockData> getPlotChunks() {

		return plotChunks;
	}

	public static List<PlotBlockData> getActivePlotBlockDatas() {
		return new ArrayList<>(plotChunks.values());
	}
	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasActiveRegenerations() {

		return !plotChunks.isEmpty();
	}

	/**
	 * @param wc WorldCoord to check for.
	 * @return true if this WorldCoord is actively being processed.
	 */
	public static boolean hasActiveRegeneration(WorldCoord wc) {
		return plotChunks.containsKey(getPlotKey(wc));
	}

	/**
	 * Removes all plotchunks currently in regeneration list for one world.
	 * 
	 * @param world - TownyWorld to have regeneration stop in.
	 */
	private static void removePlotChunksForWorld(TownyWorld world) {
		Hashtable<String, PlotBlockData> plotChunks = new Hashtable<>();
		// Rebuild the list of plotChunks, skipping the ones belonging to the given world.
		for (String key : getPlotChunks().keySet())
			if (!getPlotChunks().get(key).getWorldName().equals(world.getName()))
				plotChunks.put(key, getPlotChunks().get(key));

		// Set the new plotchunks.
		TownyRegenAPI.plotChunks = plotChunks;
	}

	/**
	 * Removes a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to remove (PlotBlockData)
	 */
	public static void removeFromActiveRegeneration(PlotBlockData plotChunk) {

		plotChunks.remove(getPlotKey(plotChunk));
	}
	
	/**
	 * Adds a Plot Chunk to the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to add (PlotBlockData)
	 */
	public static void addToActiveRegeneration(PlotBlockData plotChunk) {

		if (!plotChunks.containsKey(getPlotKey(plotChunk))) {
			//plotChunk.initialize();
			plotChunks.put(getPlotKey(plotChunk), plotChunk);
		}
	}

	/*
	 * PlotChunkSnapShot Methods. Creates PlotBlockDatas.
	 */

	/**
	 * Saves a Plot Chunk snapshot to the datasource
	 * 
	 * @param plotChunk - Chunk to take Snapshot (PlotBlockData)
	 */
	public static void addPlotChunkSnapshot(PlotBlockData plotChunk) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		final TownBlock townBlock = plotChunk.getWorldCoord().getTownBlockOrNull();
		if (townBlock == null || !townyUniverse.getDataSource().hasPlotData(townBlock)) {
			townyUniverse.getDataSource().savePlotData(plotChunk);
		}
	}

	/**
	 * Deletes a Plot Chunk snapshot from the datasource
	 * 
	 * @param plotChunk - Chunk to delete snapshot (PlotBlockData)
	 */
	private static void deletePlotChunkSnapshot(PlotBlockData plotChunk) {
		TownyUniverse.getInstance().getDataSource().deletePlotData(plotChunk);
	}

	/**
	 * Loads a Plot Chunk snapshot from the data source
	 * 
	 * @param townBlock - TownBlock to get
	 * @return loads the PlotData for the given townBlock or returns null.   
	 */
	public static PlotBlockData getPlotChunkSnapshot(TownBlock townBlock) {
		return TownyUniverse.getInstance().getDataSource().loadPlotData(townBlock);
	}

	/**
	 * Gets a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param townBlock - TownBlock to get
	 * @return PlotChunks or null   
	 */
	public static PlotBlockData getPlotChunk(TownBlock townBlock) {

		if (plotChunks.containsKey(getPlotKey(townBlock))) {
			return plotChunks.get(getPlotKey(townBlock));
		}
		return null;
	}

	private static String getPlotKey(PlotBlockData plotChunk) {

		return "[" + plotChunk.getWorldName() + "|" + plotChunk.getX() + "|" + plotChunk.getZ() + "]";
	}

	public static String getPlotKey(TownBlock townBlock) {

		return "[" + townBlock.getWorld().getName() + "|" + townBlock.getX() + "|" + townBlock.getZ() + "]";
	}
	
	public static String getPlotKey(WorldCoord wc) {
		return "[" + wc.getWorldName() + "|" + wc.getX() + "|" + wc.getZ() + "]";
	}

	/*
	 * Protection Regen follows
	 */
	
	/**
	 * Called from various explosion listeners.
	 * 
	 * @param block - {@link Block} which is being exploded.
	 * @param count - int for setting the delay to do one block at a time.
	 * @param world - {@link TownyWorld} for where the regen is being triggered.
	 * @param event - The Bukkit Event causing this explosion.
	 * 
	 * @return true if the protectiontask was begun successfully. 
	 */
	public static boolean beginProtectionRegenTask(Block block, int count, TownyWorld world, Event event) {
		// Don't interfere with an existing regen task
		if (!hasProtectionRegenTask(new BlockLocation(block.getLocation()))) {
			// Piston extensions which are broken by explosions ahead of the base block
			// cause baseblocks to drop as items and no base block to be regenerated.
			if (block.getType() == Material.PISTON_HEAD) {
				org.bukkit.block.data.type.PistonHead blockData = (org.bukkit.block.data.type.PistonHead) block.getBlockData(); 
				Block baseBlock = block.getRelative(blockData.getFacing().getOppositeFace());
				block = baseBlock;
			}
			ProtectionRegenTask task = new ProtectionRegenTask(Towny.getPlugin(), block);
			task.setTaskId(Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), task, (world.getPlotManagementWildRevertDelay() + count) * 20));
			addProtectionRegenTask(task);

			// If this was a TownyExplodingBlocksEvent we want to get the bukkit event from it first.
			if (event instanceof TownyExplodingBlocksEvent)
				event = ((TownyExplodingBlocksEvent) event).getBukkitExplodeEvent();
			
			// Remove the drops from the explosion.
			if (event instanceof EntityExplodeEvent) 
				((EntityExplodeEvent) event).setYield(0);
			else if (event instanceof BlockExplodeEvent)
				((BlockExplodeEvent) event).setYield(0);

			return true;
		}
		return false;
	}

	/**
	 * Does a task for this block already exist?
	 * 
	 * @param blockLocation - Location of the block
	 * @return true if a task exists
	 */
	public static boolean hasProtectionRegenTask(BlockLocation blockLocation) {

		return protectionRegenTasks.containsKey(blockLocation);

	}

	/**
	 * Fetch the relevant regen task for this block
	 * 
	 * @param blockLocation - Location of the block.
	 * @return the stored task, or null if there is none.
	 */
	public static ProtectionRegenTask GetProtectionRegenTask(BlockLocation blockLocation) {

		if (protectionRegenTasks.containsKey(blockLocation))
			return protectionRegenTasks.get(blockLocation);

		return null;
	}

	/**
	 * Add this task to the protection regen queue.
	 * 
	 * @param task - ProtectionRegenTask to add to queue
	 */
	public static void addProtectionRegenTask(ProtectionRegenTask task) {

		protectionRegenTasks.put(task.getBlockLocation(), task);
	}

	/**
	 * Remove this task form the protection regen queue
	 * 
	 * @param task - ProtectionRegenTask to remove from queue
	 */
	public static void removeProtectionRegenTask(ProtectionRegenTask task) {

		protectionRegenTasks.remove(task.getBlockLocation());
		if (protectionRegenTasks.isEmpty())
			protectionPlaceholders.clear();
	}

	/**
	 * Cancel all regenerating tasks and clear all queues.
	 */
	public static void cancelProtectionRegenTasks() {
		boolean replaceProtections = true;

		try {
			if (Class.forName("org.spigotmc.WatchdogThread").isInstance(Thread.currentThread())) {
				Towny.getPlugin().getLogger().severe("Detected a watchdog crash, ongoing protection revert tasks will not be finished.");
				replaceProtections = false;
			}
		} catch (Throwable ignored) {}

		for (ProtectionRegenTask task : protectionRegenTasks.values()) {
			BukkitTools.getServer().getScheduler().cancelTask(task.getTaskId());

			if (replaceProtections)
				task.replaceProtections();
		}
		protectionRegenTasks.clear();
		protectionPlaceholders.clear();
	}

	/**
	 * Is this a placholder block?
	 * 
	 * @param block - Block identifier
	 * @return true if it is a placeholder
	 */
	public static boolean isPlaceholder(Block block) {

		return protectionPlaceholders.contains(block);
	}

	/**
	 * Add this block as a placeholder (will be replaced when it's regeneration task occurs)
	 * 
	 * @param block - Block identifier
	 */
	public static void addPlaceholder(Block block) {

		protectionPlaceholders.add(block);
	}

	/**
	 * Remove this block from being tracked as a placeholder.
	 * 
	 * @param block - Block identifier
	 */
	public static void removePlaceholder(Block block) {

		protectionPlaceholders.remove(block);
	}

	/*
	 * Deprecated TownBlock Entity Deleting Queue.
	 */

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordEntityRemover#hasQueue()} instead.
	 * @return true if there are any chunks being processed.
	 */
	@Deprecated
	public static boolean hasDeleteTownBlockEntityQueue() {
		return WorldCoordEntityRemover.hasQueue();
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordEntityRemover#isQueued(WorldCoord)} instead.
	 * @param plot WorldCoord to check.
	 * @return true if the WorldCoord is queued to have entities removed.
	 */
	@Deprecated
	public static boolean isDeleteTownBlockEntityQueue(WorldCoord plot) {
		return WorldCoordEntityRemover.isQueued(plot);
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordEntityRemover#addToQueue(WorldCoord)} instead.
	 * @param plot WorldCoord to add to the queue.
	 */
	@Deprecated
	public static void addDeleteTownBlockEntityQueue(WorldCoord plot) {
		WorldCoordEntityRemover.addToQueue(plot);
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordEntityRemover#getWorldCoordFromQueue()} instead.
	 * @return a WorldCoord from the queue.
	 */
	@Nullable
	@Deprecated
	public static WorldCoord getDeleteTownBlockEntityQueue() {
		return WorldCoordEntityRemover.getWorldCoordFromQueue();
	}

	/**
	 * Deletes all of a specified entity type from a TownBlock
	 * @deprecated since 0.98.6.2 use {@link WorldCoordEntityRemover#doDeleteTownBlockEntities(WorldCoord)} instead.
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	@Deprecated
	public static void doDeleteTownBlockEntities(WorldCoord worldCoord) {
		WorldCoordEntityRemover.doDeleteTownBlockEntities(worldCoord);
	}

	/*
	 * Deprecated TownBlock Material Deleting Queue.
	 */

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#hasQueue()} instead.
	 * @return true if there are any chunks being processed.
	 */
	@Deprecated
	public static boolean hasDeleteTownBlockIdQueue() {
		return WorldCoordMaterialRemover.hasQueue();
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#isQueued(WorldCoord)} instead.
	 * @param plot WorldCoord
	 * @return true if this WorldCoord is needing Materials removed.
	 */
	@Deprecated
	public static boolean isDeleteTownBlockIdQueue(WorldCoord plot) {
		return WorldCoordMaterialRemover.isQueued(plot);
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#addToQueue(WorldCoord)} instead.
	 * @param plot WorldCoord to add to queue.
	 */
	@Deprecated
	public static void addDeleteTownBlockIdQueue(WorldCoord plot) {
		WorldCoordMaterialRemover.addToQueue(plot);
	}

	/**
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#getWorldCoordFromQueue()} instead.
	 * @return a WorldCoord that is queued to have materials removed.
	 */
	@Nullable
	@Deprecated
	public static WorldCoord getDeleteTownBlockIdQueue() {
		return WorldCoordMaterialRemover.getWorldCoordFromQueue();
	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#queueUnclaimMaterialsDeletion(WorldCoord)} instead.
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	@Deprecated
	public static void doDeleteTownBlockIds(WorldCoord worldCoord) {
		WorldCoordMaterialRemover.queueUnclaimMaterialsDeletion(worldCoord);
	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#queueDeleteWorldCoordMaterials(WorldCoord, Collection)} instead.
	 * @param townBlock - TownBlock to delete from
	 * @param material - Material to delete
	 */
	@Deprecated
	public static void deleteTownBlockMaterial(TownBlock townBlock, Material material) {
		WorldCoordMaterialRemover.queueDeleteWorldCoordMaterials(townBlock.getWorldCoord(), Collections.singleton(material));
	}

	/**
	 * @deprecated since 0.98.5.0 use {@link WorldCoordMaterialRemover#queueDeleteWorldCoordMaterials(WorldCoord, Collection)} instead.
	 * @param townBlock TownBlock to remove from.
	 * @param materialEnumSet Material EnumSet to remove.
	 */
	@Deprecated
	public static void deleteMaterialsFromTownBlock(TownBlock townBlock, EnumSet<Material> materialEnumSet) {
		WorldCoordMaterialRemover.queueDeleteWorldCoordMaterials(townBlock.getWorldCoord(), materialEnumSet);
	}

	/**
	 * Deletes all blocks which are found in the given EnumSet of Materials
	 * 
	 * @deprecated since 0.98.6.2 use {@link WorldCoordMaterialRemover#queueDeleteWorldCoordMaterials(WorldCoord, Collection)} instead. 
	 * @param coord WorldCoord to delete blocks from.
	 * @param collection Collection of Materials from which to remove.
	 */
	@Deprecated
	public static void deleteMaterialsFromWorldCoord(WorldCoord coord, Collection<Material> collection) {
		WorldCoordMaterialRemover.queueDeleteWorldCoordMaterials(coord, collection);
	}

	/**
	 * Creates a new snapshot and handles saving it
	 * @param townBlock The townblock to take a snapshot of
	 */
	public static void handleNewSnapshot(final @NotNull TownBlock townBlock) {
		createPlotSnapshot(townBlock).thenAcceptAsync(data -> {
			if (data.getBlockList().isEmpty())
				return;

			addPlotChunkSnapshot(data);
		}).exceptionally(e -> {
			if (e.getCause() != null)
				e = e.getCause();

			Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while creating a plot snapshot for " + townBlock.getWorldCoord().toString(), e);
			return null;
		});
	}

	public static CompletableFuture<PlotBlockData> createPlotSnapshot(final @NotNull TownBlock townBlock) {
		final List<ChunkSnapshot> snapshots = new ArrayList<>();
		final Collection<CompletableFuture<Chunk>> futures = townBlock.getWorldCoord().getChunks();
		
		futures.forEach(future -> future.thenAccept(chunk -> snapshots.add(chunk.getChunkSnapshot(false, false, false))));
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).thenApplyAsync(v -> {
			final PlotBlockData data = new PlotBlockData(townBlock);
			data.initialize(snapshots);
			
			return data;
		});
	}
}