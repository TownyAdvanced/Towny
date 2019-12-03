package com.palmergames.bukkit.towny.object;

import java.util.Set;

/**
 * An interface that is used to indicate that an object can manage a 
 * set of {@link com.palmergames.bukkit.towny.object.Group}.
 * @author Suneet Tipirneni (Siris)
 */
public interface GroupManageable<T extends Group> {
	/**
	 * Get the set of group objects associated with the subclass.
	 * @return The {@link java.util.Set} associated with the subclass.
	 */
	Set<T> getGroups();
	T getGroupFromID(int ID);

	/**
	 * Indicates whether the subclass has groups present.
	 * @return A boolean indicating membership.
	 */
	default boolean hasGroups() {
		return getGroups() != null;
	}
	
	default boolean hasGroup(T group) {
		if (hasGroups())
			return getGroups().contains(group);
		
		return false;
	}

	/**
	 * Indicates whether the group name is taken or not.
	 * @param name The name of the group to be tested.
	 * @return A boolean indicating if it is taken or not.
	 */
	default boolean hasGroupName(String name) {
		if (hasGroups()) {
			for (T group : getGroups()) {
				if (group.getGroupName().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}
}
