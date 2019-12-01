package com.palmergames.bukkit.towny.object;

/**
 * A simple interface which encompassed the mechanics of objects put in groups.
 * @author Suneet Tipirneni (Siris)
 * @param <T> An object which inherits from {@link com.palmergames.bukkit.towny.object.Group}
 */
public interface Groupable<T extends Group> {
	/**
	 * Gets the unique ID for the group this objects belongs to.
	 * @return Group ID as {@link Integer}
	 */
	Integer getID();
	
	String getGroupName();

	/**
	 * Sets the unique ID for the group this object belongs to.
	 * @param ID A unique idea in the context of the group manager.
	 */
	void setID(Integer ID);
	
	void setGroupName(String name);
}
