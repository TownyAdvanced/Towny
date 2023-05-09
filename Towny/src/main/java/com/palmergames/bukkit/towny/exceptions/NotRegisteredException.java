package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;

public class NotRegisteredException extends TownyException {

	private static final long serialVersionUID = 175945283391669005L;

	public NotRegisteredException() {

		super(Translatable.of("not_registered"));
	}

	public NotRegisteredException(String message) {

		super(message);
	}
	
	public NotRegisteredException(Translatable message) {
		super(message);
	}
}
