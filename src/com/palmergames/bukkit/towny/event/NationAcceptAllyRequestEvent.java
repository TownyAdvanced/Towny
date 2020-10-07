package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationAcceptAllyRequestEvent extends Event {
	
	private static final HandlerList handlers = new HandlerList();
	private final Nation senderNation;
	private final Nation receiverNation;
	
	public NationAcceptAllyRequestEvent(Nation senderNation, Nation receiverNation) {
		this.senderNation = senderNation;
		this.receiverNation = receiverNation;

	}
	
	public Nation getSenderNation() {
		return senderNation;
	}
	
	public Nation getInvitedNation() {
		return receiverNation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
