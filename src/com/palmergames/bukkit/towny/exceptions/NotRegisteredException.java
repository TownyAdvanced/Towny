package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translation;

public class NotRegisteredException extends TownyException {

	private static final long serialVersionUID = 175945283391669005L;

	public NotRegisteredException() {

		super(Translation.of("not_registered"));
	}

	public NotRegisteredException(String message) {

		super(message);
	}
}
