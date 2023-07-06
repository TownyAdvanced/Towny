package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownRuinedEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final String oldMayorName;
	
	public TownRuinedEvent(Town town, String oldMayorName) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.oldMayorName = oldMayorName;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
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
	
	/**
	 * The name of the previous mayor.
	 * 
	 * Might return "none" if no mayor was present upon ruining due to a bug.
	 */
	public String getOldMayorName() {
		return oldMayorName;
	}

	
}
