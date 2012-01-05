package com.palmergames.bukkit.towny.event;


import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.WeatherListener;

import com.palmergames.bukkit.towny.Towny;

public class TownyWeatherListener extends WeatherListener {

	private final Towny plugin;

	public TownyWeatherListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onLightningStrike(LightningStrikeEvent event) {
		
	}

	/**
	 * @return the plugin
	 */
	public Towny getPlugin() {
		return plugin;
	}
	
}