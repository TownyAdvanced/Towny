package com.palmergames.bukkit.towny;

public class TownyException extends Exception {
	private static final long serialVersionUID = -6821768221748544277L;

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
