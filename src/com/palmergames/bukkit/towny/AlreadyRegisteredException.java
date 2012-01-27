package com.palmergames.bukkit.towny;

public class AlreadyRegisteredException extends TownyException {
	private static final long serialVersionUID = 4191685552690886161L;

	public AlreadyRegisteredException() {
		super("Already registered.");
	}

	public AlreadyRegisteredException(String message) {
		super(message);
	}
}
