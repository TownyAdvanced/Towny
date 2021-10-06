package com.palmergames.bukkit.towny.object.typeapi;

/**
 * A class that represents custom town block types
 * It is meant to be extended by external classes
 * @author gnosii
 */
public abstract class CustomTownBlockType {
	TownBlockTypeHandler handler;
	String internalId;
	String displayName;
	
	public CustomTownBlockType(String internalId, String displayName, TownBlockTypeHandler handler) {
		this.internalId = internalId;
		this.displayName = displayName;
		this.handler = handler;
	}
	
	public TownBlockTypeHandler getHandler() {
		return handler;
	}
	
	public String getInternalId() {
		return internalId;
	}
	
	public String getDisplayName() { return displayName; }
}
