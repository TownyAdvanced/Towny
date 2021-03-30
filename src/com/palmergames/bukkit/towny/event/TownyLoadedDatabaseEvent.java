package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyLoadedDatabaseEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	public TownyLoadedDatabaseEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
