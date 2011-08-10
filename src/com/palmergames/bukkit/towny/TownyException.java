package com.palmergames.bukkit.towny;

public class TownyException extends Exception {
	private static final long serialVersionUID = -6821768221748544277L;
	public String error;

	public TownyException() {
		super();
		error = "unknown";
	}

	public TownyException(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
