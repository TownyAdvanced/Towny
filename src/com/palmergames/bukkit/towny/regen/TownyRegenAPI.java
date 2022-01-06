package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ElgarL
 * 
 */
public class TownyRegenAPI {

	// A list of worldCoords which are to be regenerated.
	private static List<WorldCoord> regenWorldCoordList = new ArrayList<>();
	
	// table containing snapshot data of active reversions.
	private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<>();

	// List of all old plots still to be processed for Block removal
	private static List<WorldCoord> deleteTownBlockIdQueue = new ArrayList<>();

	// A list of worldCoords which are needing snapshots
	private static List<WorldCoord> worldCoords = new ArrayList<>();
	
	// A holder for each protection regen task
	private static  Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<>();
	
	// List of protection blocks placed to prevent blockPhysics.
	private static  Set<Block> protectionPlaceholders = new HashSet<>();

	/**
	 * Removes a TownyWorld from the various Revert-on-Unclaim feature Lists/Table.
	 * @param world TownyWorld to remove.
	 */
	public static void turnOffRevertOnUnclaimForWorld(TownyWorld world) {
		removeRegenQueueListOfWorld(world); // Remove any queued regenerations.
		removeWorldCoords(world); // Stop any active snapshots being made.
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
	}
	
	/*
	 * Snapshots used in Revert-On-Unclaim feature
	 */

	/**
	 * Add a TownBlocks WorldCoord for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord
	 */
	public static void addWorldCoord(WorldCoord worldCoord) {

		if (!worldCoords.contains(worldCoord))
			worldCoords.add(worldCoord);
	}
	
	/**
	 * Removes a TownBlock from having a snapshot taken.
	 * 
	 * @param worldCoord - WorldCoord of TownBlock to remove from snapshot list.
	 */
	public static void removeWorldCoord(WorldCoord worldCoord) {

		if (worldCoords.contains(worldCoord))
			worldCoords.remove(worldCoord);
	}
	
	/**
	 * Gets a list of WorldCoords which are having snapshots taken, for one TownyWorld.
	 * 
	 * @param world - TownyWorld to gather a list of WorldCoords in.
	 * @return list - List<WorldCoord> matched to above world.
	 */
	private static List<WorldCoord> getWorldCoords(TownyWorld world) {
		List<WorldCoord> list = new ArrayList<>();
		for (WorldCoord wc : worldCoords)
			if (wc.getTownyWorldOrNull().equals(world))
				list.add(wc);

		return list;
	}
	
	/**
	 * Removes all worldcoords of given TownyWorld from having their snapshots taken.
	 * 
	 * @param world - TownyWorld to stop having snapshots made in.
	 */
	private static void removeWorldCoords(TownyWorld world) {
		for (WorldCoord wc : getWorldCoords(world))
			removeWorldCoord(wc);
	}

	/**
	 * @return true if there are any TownBlocks to be processed.
	 */
	public static boolean hasWorldCoords() {

		return worldCoords.size() != 0;
	}

	/**
	 * Check if this WorldCoord is waiting for a snapshot to be taken.
	 * 
	 * @param worldCoord - WorldCoord to check
	 * @return true if it's in the queue.
	 */
	public static boolean hasWorldCoord(WorldCoord worldCoord) {

		return worldCoords.contains(worldCoord);
	}

	/**
	 * @return First WorldCoord to be processed.
	 */
	public static WorldCoord getWorldCoord() {

		if (!worldCoords.isEmpty()) {
			WorldCoord wc = worldCoords.get(0);
			worldCoords.remove(0);
			return wc;
		}
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
	private static void removeRegenQueueListOfWorld(TownyWorld world) {
		regenWorldCoordList = getRegenQueueList().stream()
			.filter(wc -> !wc.getTownyWorldOrNull().equals(world))
			.collect(Collectors.toList());
		TownyUniverse.getInstance().getDataSource().saveRegenList();
	}

	/**
	 * Removes a WorldCoord from the queue of the revert on unclaim feature.
	 * @param wc WorldCoord to add to the queue.
	 */
	public static void removeFromRegenQueueList(WorldCoord wc) {
		if (!regenWorldCoordList.contains(wc))
			return;
		regenWorldCoordList.remove(wc);
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

	/*
	 * Active Revert-On-Unclaims.
	 */

	/**
	 * @return the plotChunks which are being processed
	 */
	public static Hashtable<String, PlotBlockData> getPlotChunks() {

		return PlotChunks;
	}

	public static List<PlotBlockData> getActivePlotBlockDatas() {
		return new ArrayList<>(PlotChunks.values());
	}
	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasActiveRegenerations() {

		return !PlotChunks.isEmpty();
	}

	/**
	 * @param wc WorldCoord to check for.
	 * @return true if this WorldCoord is actively being processed.
	 */
	public static boolean hasActiveRegeneration(WorldCoord wc) {
		return PlotChunks.containsKey(getPlotKey(wc));
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
		PlotChunks = plotChunks;
	}

	/**
	 * Removes a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to remove (PlotBlockData)
	 */
	public static void removeFromActiveRegeneration(PlotBlockData plotChunk) {

		if (PlotChunks.containsKey(getPlotKey(plotChunk)))
			PlotChunks.remove(getPlotKey(plotChunk));
	}
	
	/**
	 * Adds a Plot Chunk to the regeneration Hashtable
	 * 
	 * @param plotChunk - Chunk to add (PlotBlockData)
	 */
	public static void addToActiveRegeneration(PlotBlockData plotChunk) {

		if (!PlotChunks.containsKey(getPlotKey(plotChunk))) {
			//plotChunk.initialize();
			PlotChunks.put(getPlotKey(plotChunk), plotChunk);
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
		if (townyUniverse.getDataSource().loadPlotData(plotChunk.getWorldName(), plotChunk.getX(), plotChunk.getZ()) == null) {
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
		PlotBlockData data = TownyUniverse.getInstance().getDataSource().loadPlotData(townBlock);
		if (data != null) 
			return data;
		else
			return null;
	}

	/**
	 * Gets a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param townBlock - TownBlock to get
	 * @return PlotChunks or null   
	 */
	public static PlotBlockData getPlotChunk(TownBlock townBlock) {

		if (PlotChunks.containsKey(getPlotKey(townBlock))) {
			return PlotChunks.get(getPlotKey(townBlock));
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

	/**
	 * Regenerate the chunk the player is stood in and store the block data so it can be undone later.
	 * 
	 * @param player
	 */
//	public static void regenChunk(Player player) {
//		
//		try {
//			Coord coord = Coord.parseCoord(player);
//			World world = player.getWorld();
//			Chunk chunk = world.getChunkAt(player.getLocation());
//			int maxHeight = world.getMaxHeight();
//			
//			ChunkSnapshot snapshot = chunk.getChunkSnapshot(true,true,false);
//			
//			Object[][][] snapshot = new Object[16][maxHeight][16];
//			
//			for (int x = 0; x < 16; x++) {
//				for (int z = 0; z < 16; z++) {
//					for (int y = 0; y < maxHeight; y++) {
//						
//						//Current block to save
//						BlockState state = chunk.getBlock(x, y, z).getState();
//						
//						if (state instanceof org.bukkit.block.Sign) {
//							
//							BlockSign sign = new BlockSign(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), ((org.bukkit.block.Sign) state).getLines());
//							sign.setLocation(state.getLocation());
//							snapshot[x][y][z] = sign;
//							
//						} else if (state instanceof CreatureSpawner) {
//							
//							BlockMobSpawner spawner = new BlockMobSpawner(((CreatureSpawner) state).getSpawnedType());
//							spawner.setLocation(state.getLocation());
//							spawner.setDelay(((CreatureSpawner) state).getDelay());
//							snapshot[x][y][z] = spawner;
//							
//						} else if ((state instanceof InventoryHolder) && !(state instanceof Player)) {
//							
//							BlockInventoryHolder holder = new BlockInventoryHolder(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), ((InventoryHolder) state).getInventory().getContents());
//							holder.setLocation(state.getLocation());
//							snapshot[x][y][z] = holder;
//							
//						} else {
//						
//							snapshot[x][y][z] = new BlockObject(BukkitTools.getTypeId(state), BukkitTools.getDataData(state), state.getLocation());
//									
//						}
//						
//					}
//				}
//			}
//			
//			TownyUniverse.getDataSource().getResident(player.getName()).addUndo(snapshot);
//
//			Bukkit.getWorld(player.getWorld().getName()).regenerateChunk(coord.getX(), coord.getZ());
//
//		} catch (NotRegisteredException e) {
//			// Failed to get resident
//		}
//	}
//	
//	/**
//	 * Restore the relevant chunk using the snapshot data stored in the resident
//	 * object.
//	 * 
//	 * @param snapshot
//	 * @param resident
//	 */
//	public static void regenUndo(Object[][][] snapshot, Resident resident) {
//
//		BlockObject key = ((BlockObject) snapshot[0][0][0]);
//		World world = key.getLocation().getWorld();
//		Chunk chunk = key.getLocation().getChunk();
//		
//		int maxHeight = world.getMaxHeight();
//		
//		for (int x = 0; x < 16; x++) {
//			for (int z = 0; z < 16; z++) {
//				for (int y = 0; y < maxHeight; y++) {
//					
//					// Snapshot data we need to update the world.
//					Object state = snapshot[x][y][z];
//					
//					// The block we will be updating
//					Block block = chunk.getBlock(x, y, z);
//					
//					if (state instanceof BlockSign) {
//
//						BlockSign signData = (BlockSign)state;
//						BukkitTools.setTypeIdAndData(block, signData.getTypeId(), signData.getData(), false);
//						
//						Sign sign = (Sign) block.getState();
//						int i = 0;
//						for (String line : signData.getLines())
//							sign.setLine(i++, line);
//						
//						sign.update(true);
//						
//					} else if (state instanceof BlockMobSpawner) {
//						
//						BlockMobSpawner spawnerData = (BlockMobSpawner) state;
//						
//						BukkitTools.setTypeIdAndData(block, spawnerData.getTypeId(), spawnerData.getData(), false);
//						((CreatureSpawner) block.getState()).setSpawnedType(spawnerData.getSpawnedType());
//						((CreatureSpawner) block.getState()).setDelay(spawnerData.getDelay());
//						
//					} else if ((state instanceof BlockInventoryHolder) && !(state instanceof Player)) {
//						
//						BlockInventoryHolder containerData = (BlockInventoryHolder) state;
//						BukkitTools.setTypeIdAndData(block, containerData.getTypeId(), containerData.getData(), false);
//						
//						// Container to receive the inventory
//						InventoryHolder container = (InventoryHolder) block.getState();
//						
//						// Contents we are respawning.						
//						if (containerData.getItems().length > 0)
//							container.getInventory().setContents(containerData.getItems());
//						
//					} else {
//						
//						BlockObject blockData = (BlockObject) state;	
//						BukkitTools.setTypeIdAndData(block, blockData.getTypeId(), blockData.getData(), false);
//					}
//					
//					
//					
//
//				}
//			}
//
//		}
//
//		TownyMessaging.sendMessage(BukkitTools.getPlayerExact(resident.getName()), Translation.of("msg_undo_complete"));
//
//	}

	/*
	 * TownBlock Material Deleting Queue.
	 */
	
	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasDeleteTownBlockIdQueue() {

		return !deleteTownBlockIdQueue.isEmpty();
	}

	public static boolean isDeleteTownBlockIdQueue(WorldCoord plot) {

		return deleteTownBlockIdQueue.contains(plot);
	}

	public static void addDeleteTownBlockIdQueue(WorldCoord plot) {

		if (!deleteTownBlockIdQueue.contains(plot))
			deleteTownBlockIdQueue.add(plot);
	}

	public static WorldCoord getDeleteTownBlockIdQueue() {

		if (!deleteTownBlockIdQueue.isEmpty()) {
			WorldCoord wc = deleteTownBlockIdQueue.get(0);
			deleteTownBlockIdQueue.remove(0);
			return wc;
		}
		return null;
	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	public static void doDeleteTownBlockIds(WorldCoord worldCoord) {

		World world = worldCoord.getBukkitWorld();
		TownyWorld townyWorld = worldCoord.getTownyWorldOrNull();
		int plotSize = TownySettings.getTownBlockSize();

		if (world != null && townyWorld != null) {
			int height = world.getMaxHeight() - 1;
			int worldx = worldCoord.getX() * plotSize, worldz = worldCoord.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						if (townyWorld.isPlotManagementDeleteIds(block.getType().name()))
							block.setType(Material.AIR);

						block = null;
					}
		}
	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @param townBlock - TownBlock to delete from
	 * @param material - Material to delete
	 */
	public static void deleteTownBlockMaterial(TownBlock townBlock, Material material) {

		//Block block = null;
		int plotSize = TownySettings.getTownBlockSize();

		World world = BukkitTools.getServer().getWorld(townBlock.getWorld().getName());

		if (world != null) {
			/*
			 * if
			 * (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()),
			 * MinecraftTools.calcChunk(townBlock.getZ())))
			 * return;
			 */
			int height = world.getMaxHeight() - 1;
			int worldx = townBlock.getX() * plotSize, worldz = townBlock.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						if (block.getType() == material) {
							block.setType(Material.AIR);
						}
						block = null;
					}
		}
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
			if (block.getType().equals(Material.PISTON_HEAD)) {
				org.bukkit.block.data.type.PistonHead blockData = (org.bukkit.block.data.type.PistonHead) block.getBlockData(); 
				Block baseBlock = block.getRelative(blockData.getFacing().getOppositeFace());
				block = baseBlock;
			}
			ProtectionRegenTask task = new ProtectionRegenTask(Towny.getPlugin(), block);
			task.setTaskId(Towny.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), task, (TownySettings.getPlotManagementWildRegenDelay() + count) * 20));
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

		for (ProtectionRegenTask task : protectionRegenTasks.values()) {
			BukkitTools.getServer().getScheduler().cancelTask(task.getTaskId());
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

}