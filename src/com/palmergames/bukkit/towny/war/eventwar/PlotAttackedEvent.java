package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;

public class PlotAttackedEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
	
	private int hp;
	private TownBlock townBlock;
	
	public PlotAttackedEvent (int hp, TownBlock townBlock)
	{
		super();
		this.hp = hp;
		this.townBlock = townBlock;
	}
	
	public int getHP()
	{
		return hp;
	}
	
	public TownBlock getTownBlock()
	{
		return townBlock;
	}
	

}