package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;

public class CancelledEventException extends TownyException {

	private static final long serialVersionUID = 3114821661008750136L;
	private final String cancelMessage; 

	public CancelledEventException(CancellableTownyEvent event) {
		super(event.getCancelMessage());
		cancelMessage = event.getCancelMessage();
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

}
