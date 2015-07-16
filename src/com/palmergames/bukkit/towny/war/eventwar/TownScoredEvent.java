package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownScoredEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	private Town town;
	
	public TownScoredEvent (Town town)
	{
		this.town = town;
	}
	
	public Town getTown()
	{
		return town;
	}

}
