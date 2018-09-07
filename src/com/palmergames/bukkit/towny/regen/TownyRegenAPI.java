package com.palmergames.bukkit.towny.regen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.block.BlockInventoryHolder;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.regen.block.BlockMobSpawner;
import com.palmergames.bukkit.towny.regen.block.BlockObject;
import com.palmergames.bukkit.towny.regen.block.BlockSign;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * @author ElgarL
 * 
 */
public class TownyRegenAPI {

	//private static Towny plugin = null;
	
	public static void initialize(Towny plugin) {

		//TownyRegenAPI.plugin = plugin;
	}

	// table containing snapshot data of active reversions.
	private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<String, PlotBlockData>();

	// List of all old plots still to be processed for Block removal
	private static List<WorldCoord> deleteTownBlockIdQueue = new ArrayList<WorldCoord>();

	// A list of worldCoords which are needing snapshots
	private static List<WorldCoord> worldCoords = new ArrayList<WorldCoord>();
	
	// A holder for each protection regen task
	private static  Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<BlockLocation, ProtectionRegenTask>();
	
	// List of protection blocks placed to prevent blockPhysics.
	private static  Set<Block> protectionPlaceholders = new HashSet<Block>();

	/**
	 * Add a TownBlocks WorldCoord for a snapshot to be taken.
	 * 
	 * @param worldCoord
	 */
	public static void addWorldCoord(WorldCoord worldCoord) {

		if (!worldCoords.contains(worldCoord))
			worldCoords.add(worldCoord);
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
	 * @param worldCoord
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

	/**
	 * @return the plotChunks which are being processed
	 */
	public static Hashtable<String, PlotBlockData> getPlotChunks() {

		return PlotChunks;
	}

	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasPlotChunks() {

		return !PlotChunks.isEmpty();
	}

	/**
	 * @param plotChunks the plotChunks to set
	 */
	public static void setPlotChunks(Hashtable<String, PlotBlockData> plotChunks) {

		PlotChunks = plotChunks;
	}

	/**
	 * Removes a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param plotChunk
	 */
	public static void deletePlotChunk(PlotBlockData plotChunk) {

		if (PlotChunks.containsKey(getPlotKey(plotChunk))) {
			PlotChunks.remove(getPlotKey(plotChunk));
			TownyUniverse.getDataSource().saveRegenList();
		}
	}

	/**
	 * Adds a Plot Chunk to the regeneration Hashtable
	 * 
	 * @param plotChunk
	 * @param save
	 */
	public static void addPlotChunk(PlotBlockData plotChunk, boolean save) {

		if (!PlotChunks.containsKey(getPlotKey(plotChunk))) {
			//plotChunk.initialize();
			PlotChunks.put(getPlotKey(plotChunk), plotChunk);
			if (save)
				TownyUniverse.getDataSource().saveRegenList();
		}
	}

	/**
	 * Saves a Plot Chunk snapshot to the datasource
	 * 
	 * @param plotChunk
	 */
	public static void addPlotChunkSnapshot(PlotBlockData plotChunk) {

		if (TownyUniverse.getDataSource().loadPlotData(plotChunk.getWorldName(), plotChunk.getX(), plotChunk.getZ()) == null) {
			TownyUniverse.getDataSource().savePlotData(plotChunk);
		}
	}

	/**
	 * Deletes a Plot Chunk snapshot from the datasource
	 * 
	 * @param plotChunk
	 */
	public static void deletePlotChunkSnapshot(PlotBlockData plotChunk) {

		TownyUniverse.getDataSource().deletePlotData(plotChunk);
	}

	/**
	 * Loads a Plot Chunk snapshot from the datasource
	 * 
	 * @param townBlock
	 */
	public static PlotBlockData getPlotChunkSnapshot(TownBlock townBlock) {

		return TownyUniverse.getDataSource().loadPlotData(townBlock);
	}

	/**
	 * Gets a Plot Chunk from the regeneration Hashtable
	 * 
	 * @param townBlock
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

	//TODO: restore functionality of the chunk regen command for use in 1.13+ servers.
//	/**
//	 * Regenerate the chunk the player is stood in and store the block data so it can be undone later.
//	 * 
//	 * @param player
//	 */
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
//		
//		
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
//		TownyMessaging.sendMessage(BukkitTools.getPlayerExact(resident.getName()), TownySettings.getLangString("msg_undo_complete"));
//
//	}

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
	 * @param worldCoord
	 */
	public static void doDeleteTownBlockIds(WorldCoord worldCoord) {

		//Block block = null;
		World world = null;
		int plotSize = TownySettings.getTownBlockSize();

		//TownyMessaging.sendDebugMsg("Processing deleteTownBlockIds");

		world = worldCoord.getBukkitWorld();

		if (world != null) {
			/*
			 * if
			 * (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()),
			 * MinecraftTools.calcChunk(townBlock.getZ())))
			 * return;
			 */
			int height = world.getMaxHeight() - 1;
			int worldx = worldCoord.getX() * plotSize, worldz = worldCoord.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						try {
							if (worldCoord.getTownyWorld().isPlotManagementDeleteIds(block.getType().name())) {
								block.setType(Material.AIR);
							}
						} catch (NotRegisteredException e) {
							// Not a registered world
						}
						block = null;
					}
		}

	}

	/**
	 * Deletes all of a specified block type from a TownBlock
	 * 
	 * @param townBlock
	 * @param material
	 */
	public static void deleteTownBlockMaterial(TownBlock townBlock, Material material) {

		//Block block = null;
		int plotSize = TownySettings.getTownBlockSize();

		TownyMessaging.sendDebugMsg("Processing deleteTownBlockMaterial");

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
	 * Does a task for this block already exist?
	 * 
	 * @param blockLocation
	 * @return true if a task exists
	 */
	public static boolean hasProtectionRegenTask(BlockLocation blockLocation) {

		return protectionRegenTasks.containsKey(blockLocation);

	}

	/**
	 * Fetch the relevant regen task for this block
	 * 
	 * @param blockLocation
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
	 * @param task
	 */
	public static void addProtectionRegenTask(ProtectionRegenTask task) {

		protectionRegenTasks.put(task.getBlockLocation(), task);
	}

	/**
	 * Remove this task form the protection regen queue
	 * 
	 * @param task
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
	 * @param block
	 * @return true if it is a placeholder
	 */
	public static boolean isPlaceholder(Block block) {

		return protectionPlaceholders.contains(block);
	}

	/**
	 * Add this block as a placeholder (will be replaced when it's regeneration task occurs)
	 * 
	 * @param block
	 */
	public static void addPlaceholder(Block block) {

		protectionPlaceholders.add(block);
	}

	/**
	 * Remove this block from being tracked as a placeholder.
	 * 
	 * @param block
	 */
	public static void removePlaceholder(Block block) {

		protectionPlaceholders.remove(block);
	}

}