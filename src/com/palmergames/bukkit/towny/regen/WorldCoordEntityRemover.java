package com.palmergames.bukkit.towny.regen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WorldCoordEntityRemover {

	/** List of all WorldCoords still to be processed for Entity removal */
	private static final List<WorldCoord> worldCoordQueue = new ArrayList<>();
	/** List of all WorldCoords which are being processed for Entity removal */
	private static final List<WorldCoord> activeQueue = new ArrayList<>();

	/**
	 * @return true if there are any chunks being processed.
	 */
	public static boolean hasQueue() {

		return !worldCoordQueue.isEmpty();
	}

	/**
	 * @param worldCoord WorldCoord
	 * @return true if this WorldCoord is needing entities removed.
	 */
	public static boolean isQueued(WorldCoord worldCoord) {

		return worldCoordQueue.contains(worldCoord);
	}

	/**
	 * @return return the current queue size.
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
	 * @return a WorldCoord that is queued to have entities removed or null.
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
	 * @return true if this WorldCoord is actively being processed.
	 */
	public static boolean isActiveQueue(WorldCoord worldCoord) {

		return activeQueue.contains(worldCoord);
	}

	/**
	 * @return size of activeQueue list.
	 */
	public static int getActiveQueueSize() {

		return activeQueue.size();
	}

	/**
	 * @param worldCoord WorldCoord to add to the actively queued list.
	 */
	public static void addToActiveQueue(WorldCoord worldCoord) {

		if (!activeQueue.contains(worldCoord))
			activeQueue.add(worldCoord);
	}

	/**
	 * Deletes all of the world's deleted-entities-on-unclaim from the given WorldCoord.
	 * 
	 * @param worldCoord - WorldCoord for the Town Block
	 */
	public static void doDeleteTownBlockEntities(WorldCoord worldCoord) {
		TownyWorld world = worldCoord.getTownyWorld();
		if (world == null || !world.isUsingTowny() || !world.isDeletingEntitiesOnUnclaim())
			return;
		
		addToActiveQueue(worldCoord);
		List<Entity> toRemove = new ArrayList<>();
		Collection<Entity> entities = worldCoord.getBukkitWorld().getNearbyEntities(worldCoord.getBoundingBox());
		for (Entity entity : entities) {
			if (world.getUnclaimDeleteEntityTypes().contains(entity.getType()))
				toRemove.add(entity);
		}
		
		for (Entity entity : toRemove)
			entity.remove();
		
		worldCoordQueue.remove(worldCoord);
		activeQueue.remove(worldCoord);
	}

}
