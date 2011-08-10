package com.palmergames.bukkit.towny;

public class IConomyException extends Exception {
	private static final long serialVersionUID = 5273714478509976170L;
	public String error;

	public IConomyException() {
		super();
		error = "unknown";
	}

	public IConomyException(String error) {
		super(error);
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
