package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownLevelDecreaseEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	
	public TownLevelDecreaseEvent(Town town) {
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
	 * The town which has had its Town_Level decrease.
	 * 
	 * @return town which has had its Town_Level decrease.
	 */
	public Town getTown() {
		return town;
	}
}
