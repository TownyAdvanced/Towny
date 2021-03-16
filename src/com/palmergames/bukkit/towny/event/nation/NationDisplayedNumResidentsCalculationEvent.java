package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationDisplayedNumResidentsCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int displayedNumResidents;
	private final Nation nation;

	public NationDisplayedNumResidentsCalculationEvent(Nation nation, int displayedNumResidents) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.displayedNumResidents = displayedNumResidents;
	}

	public Nation getNation() {
		return nation;
	}

	public void setDisplayedNumResidents(int value) {
		this.displayedNumResidents = value;
	}

	public int getDisplayedNumResidents() {
		return displayedNumResidents;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
