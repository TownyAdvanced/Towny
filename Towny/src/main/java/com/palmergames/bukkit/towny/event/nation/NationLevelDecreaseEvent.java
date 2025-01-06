package com.palmergames.bukkit.towny.event.nation;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;

public class NationLevelDecreaseEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Nation nation;
	
	public NationLevelDecreaseEvent(Nation nation) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * The nation which has had its Nation_Level decrease.
	 * 
	 * @return nation which has had its Nation_Level decrease.
	 */
	public Nation getNation() {
		return nation;
	}
}
