package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Event called whenever the town's *local* map colour is being retrieved.
 */
public class TownMapColourLocalCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Town town;
	private String mapColorHexCode;

	public TownMapColourLocalCalculationEvent(Town town, String mapColorHexCode) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.mapColorHexCode = mapColorHexCode;
	}

	public Town getTown() {
		return town;
	}

	@Nullable
	public String getMapColorHexCode() {
		return mapColorHexCode;
	}

	public void setMapColorHexCode(String mapColorHexCode) {
		this.mapColorHexCode = mapColorHexCode;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
