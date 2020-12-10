package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownyWorld;

public class WorldPVPTestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final TownyWorld world;
	private boolean pvp;
	
	public WorldPVPTestEvent(TownyWorld world, boolean pvp) {
		this.world = world;
		this.setPvp(pvp);
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public TownyWorld getWorld() {
		return world;
	}

	public boolean isPvp() {
		return pvp;
	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

}
