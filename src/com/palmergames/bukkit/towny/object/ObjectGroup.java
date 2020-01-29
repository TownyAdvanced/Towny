package com.palmergames.bukkit.towny.object;

import java.util.UUID;

/**
 * An abstract class which defines the mechanics of groups in towny.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public abstract class ObjectGroup implements Nameable {
	private UUID id;
	private String name;

	/**
	 * The constructor for the Group object.
	 * @param id A unique identifier for the group id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 */
	public ObjectGroup(UUID id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public UUID getID() {
		return id;
	}
	
	public void setID(UUID ID) {
		this.id = ID;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) { this.name = name; }

	/**
	 * Determines whether a group is equivalent or not.
	 * @param obj The group to be compared
	 * @return Returns true if the id (UUID) of the group matches the given group object.
	 * False otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectGroup) {
			return ((ObjectGroup) obj).id.equals(this.id);
		}
		return false;
	}

	/**
	 * Converts this into the qualified string representation.
	 * @return A string in the format "name, id".
	 */
	@Override
	public String toString() {
		return name + "," + id;
	}

	/**
	 * Get hashcode for object group name.
	 * The reason we hash the name and not the UUID is for quicker common
	 * access from commands.
	 * @return name hashcode
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
