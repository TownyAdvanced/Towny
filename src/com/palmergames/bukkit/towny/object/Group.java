package com.palmergames.bukkit.towny.object;

/**
 * An abstract class which defines the mechanics of groups in towny.
 * @author Suneet Tipirneni (Siris)
 */
public abstract class Group {
	private int id;
	private String name;

	/**
	 * The constructor for the Group object.
	 * @param id A unique identifier for the group id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 */
	public Group(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Integer getID() {
		return id;
	}
	
	public void setID(Integer ID) {
		this.id = ID;
	}
	
	public String getGroupName() {
		return name;
	}
	
	public void setGroupName(String name) { this.name = name; }

	/**
	 * Determines whether a group is equivalent or not.
	 * @param obj The group to be compared
	 * @return Returns true if the id of the group matches the given group object.
	 * False otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Group) {
			return ((Group) obj).id == this.id;
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
}
