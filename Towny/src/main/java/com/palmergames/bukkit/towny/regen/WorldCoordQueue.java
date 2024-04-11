package com.palmergames.bukkit.towny.regen;

import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class WorldCoordQueue {
	// World coords that are queued to be processed
	 protected final Set<WorldCoord> worldCoordQueue = new LinkedHashSet<>();
	// World coords that are currently being processed
	protected final Set<WorldCoord> activeQueue = new HashSet<>();
	
	public boolean isQueued(final @NotNull WorldCoord coord) {
		return worldCoordQueue.contains(coord);
	}
	
	public boolean enqueue(final @NotNull WorldCoord coord) {
		return worldCoordQueue.add(coord);
	}
	
	public int queueSize() {
		return worldCoordQueue.size();
	}
	
	public void pollQueue() {
		if (activeQueue.size() >= maxActiveQueueSize())
			return;
		
		final WorldCoord next = Iterables.getFirst(worldCoordQueue, null);
		if (next == null)
			return;
		
		activeQueue.add(next);
		process(next);
	}
	
	protected void finishedProcessing(final @NotNull WorldCoord coord) {
		worldCoordQueue.remove(coord);
		activeQueue.remove(coord);
	}
	
	abstract int maxActiveQueueSize();

	/**
	 * @implNote Must call {@link #finishedProcessing(WorldCoord)} when finished 
	 */
	abstract void process(final @NotNull WorldCoord coord);
}
