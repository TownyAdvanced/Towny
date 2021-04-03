package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class NationListDisplayedValueCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedValue;
	private final Nation nation;

	public NationListDisplayedValueCalculationEvent(Nation nation, int displayedValue) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.displayedValue = displayedValue;
	}

	public Nation getNation() {
		return nation;
	}

	public void setDisplayedValue(int value) {
		this.displayedValue = value;
	}

	public int getDisplayedValue() {
		return displayedValue;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
