package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationDisplayedNumTownsCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedNumTowns;
	private final Nation nation;

	public NationDisplayedNumTownsCalculationEvent(Nation nation, int displayedNumTowns) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.displayedNumTowns = displayedNumTowns;
	}

	public Nation getNation() {
		return nation;
	}

	public void setDisplayedNumTowns(int value) {
		this.displayedNumTowns = value;
	}

	public int getDisplayedNumTowns() {
		return displayedNumTowns;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
