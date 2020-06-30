package com.palmergames.bukkit.towny.exceptions;

/**
 * A base class for exceptions that occur in towny that cannot be recovered from.
 */
public class TownyRuntimeException extends RuntimeException {
	public TownyRuntimeException(String message) {
		super(message);
	}
}
