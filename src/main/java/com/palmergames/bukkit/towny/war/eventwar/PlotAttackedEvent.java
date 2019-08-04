package com.palmergames.bukkit.towny.war.eventwar;

import java.util.HashSet;

import org.bukkit.entity.Player;
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
	
	private TownBlock townBlock;
	private HashSet<Player> players;
	private int hp;
	
	public PlotAttackedEvent (TownBlock townBlock, HashSet<Player> players, int hp)
	{
		super();
		this.townBlock = townBlock;
		this.players = players;
		this.hp = hp;
	}
	
	public TownBlock getTownBlock() 
	{
		return townBlock;
	}
	
	public HashSet<Player> getPlayers()
	{
		return players;
	}
	
	public int getHP()
	{
		return hp;
	}
}