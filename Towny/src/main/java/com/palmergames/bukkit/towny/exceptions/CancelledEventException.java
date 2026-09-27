package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Translatable;

public class CancelledEventException extends TownyException {

	private static final long serialVersionUID = 3114821661008750136L;
	private final Translatable cancelMessage; 

	public CancelledEventException(CancellableTownyEvent event) {
		super(event.getCancelTranslatable());
		cancelMessage = event.getCancelTranslatable();
	}

	public String getCancelMessage() {
		return cancelMessage.translate();
	}
	
	public Translatable getCancelTranslatable() {
		return cancelMessage;
	}

}
