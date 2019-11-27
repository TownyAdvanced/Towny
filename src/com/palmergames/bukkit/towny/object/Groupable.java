package com.palmergames.bukkit.towny.object;

import java.util.UUID;

/**
 * @author Suneet Tipirneni (Siris) 
 */
public interface Groupable {
	/**
	 * Gets the unique ID for the group this objects belongs to.
	 * @return UUID of ID
	 */
	UUID getGroupID();
	
	String getGroupName();
	
	default boolean hasGroup() {
		return (getGroupID() != null);
	}
	
	default boolean hasGroupName() {
		return (getGroupName() != null);
	}
}
