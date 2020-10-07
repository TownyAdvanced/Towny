package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationPreDenyAllyRequestEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Nation receiverNation;
	private final Nation senderNation;
	boolean cancelled;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationPreDenyAllyRequestEvent(Nation receiverNation, Nation senderNation) {
		this.receiverNation = receiverNation;
		this.senderNation = senderNation;

	}

	public Nation getReceiverNation() {
		return receiverNation;
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

	public Nation getSenderNation() {
		return senderNation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	
	
}
