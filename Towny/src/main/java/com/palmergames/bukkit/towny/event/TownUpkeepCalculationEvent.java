package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownUpkeepCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private double upkeep;
	private final Town town;

	public TownUpkeepCalculationEvent(Town town, double upkeep) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.upkeep = upkeep;
	}

	public Town getTown() {
		return town;
	}

	public void setUpkeep(double value) {
		this.upkeep = value;
	}

	public double getUpkeep() {
		return upkeep;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}