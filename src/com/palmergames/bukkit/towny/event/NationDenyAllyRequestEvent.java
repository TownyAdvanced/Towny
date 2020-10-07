package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationDenyAllyRequestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Nation receiverNation;
	private final Nation senderNation;
	
	public NationDenyAllyRequestEvent(Nation receiverNation, Nation senderNation) {
		this.receiverNation = receiverNation;
		this.senderNation = senderNation;
		
	}

	public Nation getReceiverNation() {
		return receiverNation;
	}

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
