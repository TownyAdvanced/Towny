package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownScoredEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
	
	private Town town;
	private int score;
	
	public TownScoredEvent (Town town, int score)
	{
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.score = score;
	}
	
	public Town getTown()
	{
		return town;
	}
	
	public int getScore()
	{
		return score;
	}

}
