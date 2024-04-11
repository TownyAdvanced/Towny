package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WorldCoordEntityRemover extends WorldCoordQueue {
	private static final WorldCoordEntityRemover INSTANCE = new WorldCoordEntityRemover();
	
	public static WorldCoordEntityRemover getInstance() {
		return INSTANCE;
	}

	@Override
	int maxActiveQueueSize() {
		return 0;
	}

	@Override
	void process(@NotNull WorldCoord coord) {
		final TownyWorld world = coord.getTownyWorld();
		if (world == null || !world.isUsingTowny() || !world.isDeletingEntitiesOnUnclaim()) {
			finishedProcessing(coord);
			return;
		}

		Towny.getPlugin().getScheduler().run(coord.getLowerMostCornerLocation(), () -> {
			try {
				final World bukkitWorld = world.getBukkitWorld();
				if (bukkitWorld == null)
					return;

				for (final Entity entity : bukkitWorld.getNearbyEntities(coord.getBoundingBox())) {
					if (world.getUnclaimDeleteEntityTypes().contains(entity.getType()))
						entity.remove();
				}
			} finally {
				finishedProcessing(coord);
			}
		});
	}
}
