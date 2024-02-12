package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;

public class InvalidNameException extends TownyException {
	
	private static final long serialVersionUID = 4191685532590886161L;

	public InvalidNameException() {
		super("Invalid name!");
	}

	public InvalidNameException(String message) {
		super(message);
	}

	public InvalidNameException(Translatable translatable) {
		super(translatable);
	}
}
