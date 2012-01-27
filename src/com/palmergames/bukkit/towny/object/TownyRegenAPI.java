package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.MinecraftTools;



/**
 * @author ElgarL
 *
 */
public class TownyRegenAPI extends TownyUniverse {
	
	// table containing snapshot data of active reversions.
	private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<String, PlotBlockData>();
	
	// List of all old plots still to be processed for Block removal
	private static List<WorldCoord> deleteTownBlockIdQueue = new ArrayList<WorldCoord>();
	
	// A list of worldCoords which are needing snapshots
	private static List<WorldCoord> worldCoords = new ArrayList<WorldCoord>();
	
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
		if (TownyUniverse.getDataSource().loadPlotData(plotChunk.getWorldName(),plotChunk.getX(),plotChunk.getZ()) == null) {
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
	
	/**
	 * Restore the relevant chunk using the snapshot data stored in the resident object.
	 * 
	 * @param snapshot
	 * @param resident
	 */
	public static void regenUndo (ChunkSnapshot snapshot, Resident resident) {
			
			byte data;
			int typeId;
			World world = Bukkit.getWorld(snapshot.getWorldName());
			Chunk chunk = world.getChunkAt(MinecraftTools.calcChunk(snapshot.getX()), MinecraftTools.calcChunk(snapshot.getZ()));

			for (int x = 0 ; x < 16 ; x++) {
				for (int z = 0 ; z < 16 ; z++) {
					for (int y = 0 ; y < world.getMaxHeight() ; y++) {
						data = (byte) snapshot.getBlockData(x, y, z);
						typeId = snapshot.getBlockTypeId(x, y, z);
						chunk.getBlock(x, y, z).setTypeIdAndData(typeId, data, false);
					}
				}
				
			}
			
			TownyMessaging.sendMessage(Bukkit.getPlayerExact(resident.getName()), TownySettings.getLangString("msg_undo_complete"));

	}
	
	/////////////////////////////////
	
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
	 * @param townBlock
	 * @param material
	 */
	public static void doDeleteTownBlockIds(WorldCoord worldCoord) {

		//Block block = null;
		World world = null;
		int plotSize = TownySettings.getTownBlockSize();

		TownyMessaging.sendDebugMsg("Processing deleteTownBlockIds");

		world = Bukkit.getWorld(worldCoord.getWorld().getName());
		
		if (world != null) {
			/*
			if (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()), MinecraftTools.calcChunk(townBlock.getZ())))
				return;
			*/
			int height = world.getMaxHeight() - 1;
			int worldx = worldCoord.getX() * plotSize, worldz = worldCoord.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						if (worldCoord.getWorld().isPlotManagementDeleteIds(block.getTypeId())) {
							block.setType(Material.AIR);
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
	public static void deleteTownBlockMaterial(TownBlock townBlock, int material) {

		//Block block = null;
		int plotSize = TownySettings.getTownBlockSize();

		TownyMessaging.sendDebugMsg("Processing deleteTownBlockMaterial");

		World world = Bukkit.getWorld(townBlock.getWorld().getName());
		
		if (world != null) {
			/*
			if (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()), MinecraftTools.calcChunk(townBlock.getZ())))
				return;
			*/
			int height = world.getMaxHeight() - 1;
			int worldx = townBlock.getX() * plotSize, worldz = townBlock.getZ() * plotSize;

			for (int z = 0; z < plotSize; z++)
				for (int x = 0; x < plotSize; x++)
					for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
						Block block = world.getBlockAt(worldx + x, y, worldz + z);
						if (block.getTypeId() == material) {
							block.setType(Material.AIR);
						}
						block = null;
					}
		}
	}

	
	
}