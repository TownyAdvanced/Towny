package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
	private Player player;
	
	public PlotAttackedEvent (int hp, Player attacker)
	{
		super();
		this.hp = hp;
		this.player = attacker;
	}
	
	public int getHP()
	{
		return hp;
	}
	
	public Player getAttacker()
	{
		return player;
	}
	

}