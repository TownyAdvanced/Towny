package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownPreTransactionEvent extends Event implements Cancellable {
	private Town town;
	private static final HandlerList handlers = new HandlerList();
	private Transaction transaction;
	private String cancelMessage = "Sorry this event was cancelled.";
	private boolean isCancelled = false;

	public TownPreTransactionEvent(Town town, Transaction transaction) {
		this.town = town;
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
