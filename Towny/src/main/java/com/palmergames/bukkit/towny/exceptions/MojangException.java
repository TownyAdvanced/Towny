package com.palmergames.bukkit.towny.exceptions;

public class MojangException extends TownyException {

	private static final long serialVersionUID = -7863854182660266492L;

	public MojangException() {
		super("Mojang returned 204 on a resident.");
	}
	
	public MojangException(String message) {
		super(message);
	}
}
