package com.palmergames.bukkit.towny.event.town;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownAddAlliedTownEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	boolean cancelled;
	private final Town town;
	private final Town newAlly;
	private String cancelMessage = "A town alliance was cancelled by another plugin.";
	
	public TownAddAlliedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.newAlly = newAlly;
	}

	/**
	 * @return town Town which is receiving a new ally.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return newAlly Town which is being added as a new ally.
	 */
	public Town getNewAlly() {
		return newAlly;
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
