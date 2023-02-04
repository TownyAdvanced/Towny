package com.palmergames.bukkit.towny.regen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WorldCoordMaterialRemover {

	/** List of all WorldCoords still to be processed for Block removal */
	private static final List<WorldCoord> worldCoordQueue = new ArrayList<>();
	/** List of all WorldCoords which are being processed for Block removal */
	private static final List<WorldCoord> activeQueue = new ArrayList<>();

	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasQueue() {

		return !worldCoordQueue.isEmpty();
	}

	/**
	 * @param worldCoord WorldCoord
	 * @return true if this WorldCoord is needing Materials removed.
	 */
	public static boolean isQueued(WorldCoord worldCoord) {

		return worldCoordQueue.contains(worldCoord);
	}

	/**
	 * @return size of the waiting queue.
	 */
	public static int getQueueSize() {

		return worldCoordQueue.size();
	}

	/**
	 * @param worldCoord WorldCoord to add to queue.
	 */
	public static void addToQueue(WorldCoord worldCoord) {

		if (!worldCoordQueue.contains(worldCoord))
			worldCoordQueue.add(worldCoord);
	}

	/**
	 * @return a WorldCoord that is queued to have materials removed or null.
	 */
	@Nullable
	public static WorldCoord getWorldCoordFromQueue() {

		if (!worldCoordQueue.isEmpty()) {
			for (WorldCoord wc : worldCoordQueue)
				if (!isActiveQueue(wc))
					return wc;
		}

		return null;
	}

	/**
	 * @param worldCoord WorldCoord to check for.
	 * @return true if this WorldCoord is being actively processed.
	 */
	public static boolean isActiveQueue(WorldCoord worldCoord) {

		return activeQueue.contains(worldCoord);
	}

	/**
	 * @return size of the activeQueue.
	 */
	public static int getActiveQueueSize() {

		return activeQueue.size();
	}

	/**
	 * @param worldCoord WorldCoord to move to the activeQueue.
	 */
	public static void addToActiveQueue(WorldCoord worldCoord) {

		if (!activeQueue.contains(worldCoord))
			activeQueue.add(worldCoord);
	}

	/**
	 * Used to delete the world's PlotManagementDeleteIds when a townblock is
	 * unclaimed.
	 * 
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	public static void queueUnclaimMaterialsDeletion(WorldCoord worldCoord) {
		TownyWorld world = worldCoord.getTownyWorld();
		if (world == null || isActiveQueue(worldCoord))
			return;
		addToActiveQueue(worldCoord);
		deleteMaterialsFromWorldCoord(worldCoord, world.getPlotManagementDeleteIds());
	}

	/**
	 * Used to delete a collection of Materials from a worldCoord.
	 * 
	 * Towny uses this for the /plot clear command.
	 * 
	 * @param coord      WorldCoord to remove materials from.
	 * @param collection Material collection that will be used to delete.
	 */
	public static void queueDeleteWorldCoordMaterials(WorldCoord coord, Collection<Material> collection) {
		if (isActiveQueue(coord))
			return;
		addToActiveQueue(coord);
		deleteMaterialsFromWorldCoord(coord, collection);
	}

	/**
	 * Deletes all blocks which are found in the given Collection of Materials
	 * 
	 * @param coord      WorldCoord to delete blocks from.
	 * @param collection Collection of Materials from which to remove.
	 */
	public static void deleteMaterialsFromWorldCoord(WorldCoord coord, Collection<Material> collection) {

		List<Block> toRemove = findBlocksIn(coord, collection);
		if (toRemove.isEmpty()) {
			worldCoordQueue.remove(coord);
			activeQueue.remove(coord);
			return;
		}

		if (Bukkit.isPrimaryThread())
			deleteBlocks(coord, toRemove);
		else
			Bukkit.getScheduler().runTask(Towny.getPlugin(), () -> deleteBlocks(coord, toRemove));
	}

	/**
	 * Scans the given WorldCoord for matching Materials in the given collection and
	 * returns the Blocks.
	 * 
	 * @param coord      WorldCoord to scan.
	 * @param collection Collection of Materials to match.
	 * @return List of Blocks that will be removed.
	 */
	private static List<Block> findBlocksIn(WorldCoord coord, Collection<Material> collection) {
		List<Block> toRemove = new ArrayList<>();
		World world = coord.getBukkitWorld();
		if (world == null)
			return toRemove;

		int maxHeight = world.getMaxHeight() - 1;
		int minHeight = world.getMinHeight();
		int plotSize = TownySettings.getTownBlockSize();
		int worldX = coord.getX() * plotSize, worldZ = coord.getZ() * plotSize;
		for (int z = 0; z < plotSize; z++)
			for (int x = 0; x < plotSize; x++)
				for (int y = maxHeight; y > minHeight; y--) { // Check from bottom up else minecraft won't remove doors
					Block block = world.getBlockAt(worldX + x, y, worldZ + z);
					if (collection.contains(block.getType()))
						toRemove.add(block);
				}

		return toRemove;
	}

	private static void deleteBlocks(WorldCoord coord, List<Block> toRemove) {
		toRemove.forEach(block -> block.setType(Material.AIR));
		worldCoordQueue.remove(coord);
		activeQueue.remove(coord);
	}

}
