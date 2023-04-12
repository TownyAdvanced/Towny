package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownConqueredEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	
	public TownConqueredEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * The town which has become conquered.
	 * 
	 * @return town which is being reclaimed.
	 */
	public Town getTown() {
		return town;
	}
}
