package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownRuinedEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	
	public TownRuinedEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * The town which is falling into ruin.
	 * 
	 * @return town which is falling into ruin.
	 */
	public Town getTown() {
		return town;
	}

	
}
