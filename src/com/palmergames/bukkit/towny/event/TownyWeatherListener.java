package com.palmergames.bukkit.towny.event;


import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;

import com.palmergames.bukkit.towny.Towny;

public class TownyWeatherListener implements Listener {

	private final Towny plugin;

	public TownyWeatherListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onLightningStrike(LightningStrikeEvent event) {
		
	}

	/**
	 * @return the plugin
	 */
	public Towny getPlugin() {
		return plugin;
	}
	
}