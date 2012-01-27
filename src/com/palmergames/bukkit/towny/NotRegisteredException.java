package com.palmergames.bukkit.towny;


public class NotRegisteredException extends TownyException {
	private static final long serialVersionUID = 175945283391669005L;

	public NotRegisteredException() {
		super("Not registered.");
	}

	public NotRegisteredException(String message) {
		super(message);
	}
}
