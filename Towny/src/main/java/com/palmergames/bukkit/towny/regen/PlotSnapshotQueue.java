package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.JavaUtil;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlotSnapshotQueue extends WorldCoordQueue {
	private static final PlotSnapshotQueue INSTANCE = new PlotSnapshotQueue();

	// https://jd.papermc.io/paper/1.20/org/bukkit/Chunk.html#getChunkSnapshot(boolean,boolean,boolean,boolean)
	private static final MethodHandle GET_CHUNK_SNAPSHOT = JavaUtil.getMethodHandle(Chunk.class, "getChunkSnapshot", boolean.class, boolean.class, boolean.class, boolean.class);
	
	public static PlotSnapshotQueue getInstance() {
		return INSTANCE;
	}
	
	@Override
	int maxActiveQueueSize() {
		return 50;
	}

	@Override
	void process(@NotNull WorldCoord coord) {
		final TownBlock townBlock = coord.getTownBlockOrNull();
		if (townBlock == null) {
			finishedProcessing(coord);
			return;
		}
		
		createPlotSnapshot(townBlock).thenAcceptAsync(data -> {
			if (data.getBlockList().isEmpty())
				return;

			TownyRegenAPI.addPlotChunkSnapshot(data);
		}).exceptionally(e -> {
			if (e.getCause() != null)
				e = e.getCause();

			Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while creating a plot snapshot for " + coord, e);
			return null;
		}).whenComplete((v, t) -> finishedProcessing(coord));
	}

	public static CompletableFuture<PlotBlockData> createPlotSnapshot(final @NotNull TownBlock townBlock) {
		final List<ChunkSnapshot> snapshots = new ArrayList<>();
		final Collection<CompletableFuture<Chunk>> futures = townBlock.getWorldCoord().getChunks();

		futures.forEach(future -> future.thenAccept(chunk -> {
			try {
				if (GET_CHUNK_SNAPSHOT != null) {
					snapshots.add((ChunkSnapshot) GET_CHUNK_SNAPSHOT.invoke(chunk, false, false, false, false));
				} else {
					snapshots.add(chunk.getChunkSnapshot(false, false, false));
				}
			} catch (Throwable throwable) {
				snapshots.add(chunk.getChunkSnapshot(false, false, false));
			}
		}));

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{})).thenApplyAsync(v -> {
			final PlotBlockData data = new PlotBlockData(townBlock);
			data.initialize(snapshots);

			return data;
		});
	}
}
