package com.palmergames.bukkit.towny.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import com.palmergames.bukkit.towny.Towny;

public class TownyServerListener implements Listener {

	private final Towny plugin;

	public TownyServerListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler
	public void onTownyNameUpdaterEnabled(PluginEnableEvent event) {
		if (event.getPlugin().getName().equalsIgnoreCase("TownyNameUpdater")) {
			plugin.getLogger().info("[Towny] Disabling unneeded TownyNameUpdater.jar, you may delete this .jar.");
			plugin.getPluginLoader().disablePlugin(event.getPlugin());
		}
	}
	
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if (event.getPlugin().getName().equals("Citizens"))
			plugin.checkCitizens();
	}
}
