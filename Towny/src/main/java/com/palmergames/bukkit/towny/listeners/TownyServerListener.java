package com.palmergames.bukkit.towny.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyCommandOverrideAPI;

public class TownyServerListener implements Listener {

	private final Towny plugin;

	public TownyServerListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		TownyCommandOverrideAPI.unregisterOverrides(event.getPlugin());
	}

	@EventHandler
	public void onTownyNameUpdaterEnabled(PluginEnableEvent event) {
		if (event.getPlugin().getName().equalsIgnoreCase("TownyNameUpdater")) {
			plugin.getLogger().info("Disabling unneeded TownyNameUpdater.jar, you may delete this .jar.");
			plugin.getServer().getPluginManager().disablePlugin(event.getPlugin());
		}
	}
}
