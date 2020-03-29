package com.palmergames.bukkit.towny.exceptions;

/**
 * Exception that is thrown in Towny's startup.
 */
public class TownyStartException extends RuntimeException {
	public TownyStartException() {
		super();
	}

	public TownyStartException(String message) {
		super(message);
	}
}
