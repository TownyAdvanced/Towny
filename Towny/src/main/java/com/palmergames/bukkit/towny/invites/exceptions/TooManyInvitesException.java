package com.palmergames.bukkit.towny.invites.exceptions;

@SuppressWarnings("serial")
public class TooManyInvitesException extends Exception {

	public TooManyInvitesException() {

		super("unknown");
	}

	public TooManyInvitesException(String message) {

		super(message);
	}
}
