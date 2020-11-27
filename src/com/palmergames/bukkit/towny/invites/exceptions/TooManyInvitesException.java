package com.palmergames.bukkit.towny.invites.exceptions;

@SuppressWarnings("serial")
public class TooManyInvitesException extends Exception {

	/**
	 * Legacy method to get an error message.
	 * 
	 * @return Returns output of #getMessage.
	 * 
	 * @deprecated Please use {@link #getMessage()} instead.
	 */
	@Deprecated
	public String getError() {

		return getMessage();
	}

	public TooManyInvitesException() {

		super("unknown");
	}

	public TooManyInvitesException(String message) {

		super(message);
	}
}
