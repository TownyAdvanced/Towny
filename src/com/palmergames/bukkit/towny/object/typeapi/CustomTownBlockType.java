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
	String asciiKey;
	double price;
	
	public CustomTownBlockType(String internalId, String displayName, TownBlockTypeHandler handler, String asciiKey, double price) {
		this.internalId = internalId;
		this.displayName = displayName;
		this.handler = handler;
		this.asciiKey = asciiKey;
		this.price = price;
	}

	public CustomTownBlockType(String internalId, String displayName, TownBlockTypeHandler handler) {
		this.internalId = internalId;
		this.displayName = displayName;
		this.handler = handler;
		this.asciiKey = "C";
		this.price = 0.0;
	}
	
	public TownBlockTypeHandler getHandler() {
		return handler;
	}
	
	public String getInternalId() {
		return internalId;
	}
	
	public String getDisplayName() { return displayName; }

	public String getAsciiMapKey() {
		return getAsciiMapKey();
	}
	
	public double getPrice() {
		return price;
	}
}
