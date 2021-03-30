package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationUpkeepCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private double upkeep;
	private final Nation nation;

	public NationUpkeepCalculationEvent(Nation nation, double upkeep) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.upkeep = upkeep;
	}

	public Nation getNation() {
		return nation;
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
