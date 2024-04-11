package com.palmergames.bukkit.towny.regen;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WorldCoordMaterialRemover extends WorldCoordQueue {
	private static final WorldCoordMaterialRemover INSTANCE = new WorldCoordMaterialRemover();

	public static WorldCoordMaterialRemover getInstance() {
		return INSTANCE;
	}

	@Override
	int maxActiveQueueSize() {
		return 10;
	}

	@Override
	void process(@NotNull WorldCoord coord) {
		final TownyWorld world = coord.getTownyWorld();
		if (world == null || !world.isUsingTowny() || world.isUsingPlotManagementDelete()) {
			finishedProcessing(coord);
			return;
		}

		Towny.getPlugin().getScheduler().run(coord.getLowerMostCornerLocation(), () -> {
			try {
				deleteMaterialsFromWorldCoord(coord, world.getPlotManagementDeleteIds());
			} finally {
				finishedProcessing(coord);
			}
		});
	}

	/**
	 * Deletes all blocks which are found in the given Collection of Materials
	 * 
	 * @param coord      WorldCoord to delete blocks from.
	 * @param collection Collection of Materials from which to remove.
	 */
	public static void deleteMaterialsFromWorldCoord(WorldCoord coord, Collection<Material> collection) {
		findBlocksIn(coord, collection).forEach(block -> block.setType(Material.AIR));
	}

	/**
	 * Scans the given WorldCoord for matching Materials in the given collection and
	 * returns the Blocks.
	 * 
	 * @param coord      WorldCoord to scan.
	 * @param collection Collection of Materials to match.
	 * @return List of Blocks that will be removed.
	 */
	private static Collection<Block> findBlocksIn(WorldCoord coord, Collection<Material> collection) {
		Set<Block> toRemove = new HashSet<>();
		World world = coord.getBukkitWorld();
		if (world == null)
			return toRemove;

		int maxHeight = world.getMaxHeight() - 1;
		int minHeight = world.getMinHeight();
		int plotSize = TownySettings.getTownBlockSize();
		int worldX = coord.getX() * plotSize, worldZ = coord.getZ() * plotSize;
		for (int z = 0; z < plotSize; z++) {
			for (int x = 0; x < plotSize; x++) {
				for (int y = maxHeight; y > minHeight; y--) { // Check from bottom up else minecraft won't remove doors
					Block block = world.getBlockAt(worldX + x, y, worldZ + z);
					if (collection.contains(block.getType()))
						toRemove.add(block);
				}
			}
		}

		return toRemove;
	}
}
