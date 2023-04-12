package com.palmergames.bukkit.towny.event.time.dailytaxes;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NewDayTaxAndUpkeepPreCollectionEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	
	/**
	 * Cancellable event that precedes all taxes and upkeep collection.
	 */
	public NewDayTaxAndUpkeepPreCollectionEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
