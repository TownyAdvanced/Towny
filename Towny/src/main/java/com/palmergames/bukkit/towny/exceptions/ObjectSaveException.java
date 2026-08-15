package com.palmergames.bukkit.towny.exceptions;

public class ObjectSaveException extends TownyException {

	private static final long serialVersionUID = 2434653565991348834L;

	public ObjectSaveException(String message) {
		super(message);
	}
	
	public ObjectSaveException(String message, Throwable cause) {
		super(message, cause);
	}
}
