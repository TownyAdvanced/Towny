package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class TownReclaimedEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final Resident resident;
	
	public TownReclaimedEvent(Town town, Resident resident) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.resident = resident;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * The ruined town which has been reclaimed.
	 * 
	 * @return town which is being reclaimed.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * The resident who is reclaiming the town, and will become the new mayor.
	 * 
	 * @return resident who will be mayor.
	 */
	public Resident getResident() {
		return resident;
	}

	
}
