package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.TownySettings;

public class NotRegisteredException extends TownyException {

	private static final long serialVersionUID = 175945283391669005L;

	public NotRegisteredException() {

		super(TownySettings.getLangString("not_registered"));
	}

	public NotRegisteredException(String message) {

		super(message);
	}
}
