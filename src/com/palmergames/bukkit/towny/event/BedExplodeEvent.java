package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BedExplodeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player player;
	private final Location location1;
	private final Location location2;
	
	public BedExplodeEvent(Player player, Location loc1, Location loc2) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.location1 = loc1;
		this.location2 = loc2;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
    public static HandlerList getHandlerList() {

		return handlers;
	}
	
	public Location getLocation() {
		return location1;
	}
	
	public Location getLocation2() {
		return location2;
	}
	
	public Player getPlayer() {
		return player;
	}

}
