package com.palmergames.bukkit.towny.exceptions;

public class TownyException extends Exception {

	private static final long serialVersionUID = -6821768221748544277L;

	/**
	 * Legacy method to get an error message.
	 * 
	 * @return Returns output of {@link #getMessage()}.
	 * 
	 * @deprecated Use {@link #getMessage()} instead.
	 */
	@Deprecated
	public String getError() {

		return getMessage();
	}

	public TownyException() {

		super("unknown");
	}

	public TownyException(String message) {

		super(message);
	}
}
