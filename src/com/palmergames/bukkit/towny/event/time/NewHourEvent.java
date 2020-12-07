package com.palmergames.bukkit.towny.event.time;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NewHourEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	private long time;
	public NewHourEvent(long time) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.time = time;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	/**
	 * To turn this into something you could display use the following:
	 * 
	 * timeFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm")
	 * timeFormat.format(event.getTime())
	 * 
	 * @return time 
	 */
	public long getTime() {
		return time;
	}
}
