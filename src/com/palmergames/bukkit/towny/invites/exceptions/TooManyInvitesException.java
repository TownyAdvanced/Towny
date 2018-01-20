package com.palmergames.bukkit.towny.invites.exceptions;

public class TooManyInvitesException extends Exception {

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
