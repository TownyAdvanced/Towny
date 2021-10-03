package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.TownyUniverse;

public abstract class CustomTownBlockType {
	TownBlockTypeHandler handler;
	String internalId;
	String displayName;

	/**
	 * Create a new town block type.
	 * @param internalId Used for internally saving the town block type. You should ideally name it after your plugin.
	 * @param displayName Name displayed on the front-end (seen by players)
	 */
	public CustomTownBlockType(String internalId, String displayName) {
		this.internalId = internalId;
		this.displayName = displayName;
		TownyUniverse.getInstance().registerCustomTownBlockType(this);
	}

	/**
	 * Set the town block type handler.
	 * @param tbHandler Handler for Towny events
	 */
	public void setHandler(TownBlockTypeHandler tbHandler) {
		handler = tbHandler;
	}
	
	public TownBlockTypeHandler getHandler() {
		return handler;
	}
	
	public String getInternalId() {
		return internalId;
	}
	
	public String getDisplayName() { return displayName; }
}
