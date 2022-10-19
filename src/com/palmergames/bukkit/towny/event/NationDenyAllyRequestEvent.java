package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;

public class NationDenyAllyRequestEvent extends CancellableTownyEvent {

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
}
