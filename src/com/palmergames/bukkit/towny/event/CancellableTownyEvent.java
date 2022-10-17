package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class CancellableTownyEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	public CancellableTownyEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Whether the event has been cancelled.
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Set the event to cancelled.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	/**
	 * @return the String message which will display to the player/sender.
	 */
	public String getCancelMessage() {
		return cancelMessage;
	}

	/**
	 * Sets the cancellation message which will display to the player. Set to "" to display nothing.
	 * @param msg Message to display.
	 */
	public void setCancelMessage(String msg) {
		this.cancelMessage = msg;
	}

}
