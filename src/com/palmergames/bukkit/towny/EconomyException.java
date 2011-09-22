package com.palmergames.bukkit.towny;

public class EconomyException extends Exception {
	private static final long serialVersionUID = 5273714478509976170L;
	public String error;

	public EconomyException() {
		super();
		error = "unknown";
	}

	public EconomyException(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
