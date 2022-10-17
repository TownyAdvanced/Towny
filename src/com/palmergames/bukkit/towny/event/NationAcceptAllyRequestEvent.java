package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;

public class NationAcceptAllyRequestEvent extends CancellableTownyEvent {

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
}
