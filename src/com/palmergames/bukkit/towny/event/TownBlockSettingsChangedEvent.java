package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownBlockSettingsChangedEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;
	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	private TownyWorld w;
	private Town t;
	private TownBlock tb;
	
	public TownBlockSettingsChangedEvent (TownyWorld w) {
		this.w = w;
	}

	public TownBlockSettingsChangedEvent (Town t) {
		this.t = t;
	}

	public TownBlockSettingsChangedEvent (TownBlock tb) {
		this.tb = tb;
	}
	
	public TownyWorld getTownyWorld() {
		return w;
	}
	
	public Town getTown() {
		return t;
	}
	
	public TownBlock getTownBlock() {
		return tb;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.cancelled = b;
	}
}
