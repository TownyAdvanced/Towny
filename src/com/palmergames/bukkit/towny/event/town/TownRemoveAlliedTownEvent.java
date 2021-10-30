package com.palmergames.bukkit.towny.event.town;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownRemoveAlliedTownEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	boolean cancelled;
	private final Town town;
	private final Town removedAlly;
	private String cancelMessage = "The disolution of a town alliance was cancelled by another plugin.";
	
	public TownRemoveAlliedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.removedAlly = newAlly;
	}

	/**
	 * @return town Town which is removing an ally.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return removedAlly Town which is being removed as an ally.
	 */
	public Town getRemovedAlly() {
		return removedAlly;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
	
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
	
	public String getCancelMessage() { return this.cancelMessage; }

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
