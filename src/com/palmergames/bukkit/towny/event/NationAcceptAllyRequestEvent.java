package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationAcceptAllyRequestEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	boolean cancelled;
	private final Nation senderNation;
	private final Nation receiverNation;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationAcceptAllyRequestEvent(Nation senderNation, Nation receiverNation) {
		this.senderNation = senderNation;
		this.receiverNation = receiverNation;

	}

	public Nation getSenderNation() {
		return senderNation;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	public Nation getInvitedNation() {
		return receiverNation;
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
