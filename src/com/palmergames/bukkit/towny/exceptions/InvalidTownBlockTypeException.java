package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.typeapi.CustomTownBlockType;

public class InvalidTownBlockTypeException extends TownyException {
	private CustomTownBlockType ctb;
	private ExceptionReason reason;
	
	public InvalidTownBlockTypeException(CustomTownBlockType ctb, ExceptionReason reason) {
		super("CustomTownBlockType " + ctb.getInternalId() + " was not correctly initialized.");
		this.ctb = ctb;
		this.reason = reason;
	}
	
	public enum ExceptionReason {
		MISSING_REQUIRED_ID,
		MISSING_DISPLAY_NAME
	}
}
