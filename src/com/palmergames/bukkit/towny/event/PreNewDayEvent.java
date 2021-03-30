package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called before the new day operations are executed in DailyTimerTask.
 */
public class PreNewDayEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	public PreNewDayEvent() {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
