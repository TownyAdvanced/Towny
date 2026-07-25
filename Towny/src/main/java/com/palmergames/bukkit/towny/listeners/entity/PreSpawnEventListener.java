package com.palmergames.bukkit.towny.listeners.entity;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.palmergames.bukkit.towny.Towny;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PreSpawnEventListener implements Listener {
	private final Towny plugin;
	private final TownyEntityListener entityListener;

	public PreSpawnEventListener(final Towny plugin, final TownyEntityListener entityListener) {
		this.plugin = plugin;
		this.entityListener = entityListener;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPreCreatureSpawn(final PreCreatureSpawnEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		final Location location = event.getSpawnLocation();
		if (entityListener.preventEntitySpawn(event.getType(), event.getReason(), location, location.getWorld(), null)) {
			event.setCancelled(true);
		}
	}
}
