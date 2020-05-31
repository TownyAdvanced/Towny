package com.palmergames.bukkit.towny.object;

import java.util.Collection;
import java.util.UUID;

/**
 * An interface that is used to indicate that an object can manage a 
 * set of {@link ObjectGroup}.
 * @author Suneet Tipirneni (Siris)
 */
interface ObjectGroupManageable<T extends ObjectGroup> {
	
	/**
	 * Get the set of group objects associated with the subclass.
	 * @return The {@link java.util.Set} associated with the subclass.
	 */
	Collection<T> getObjectGroups();
	T getObjectGroupFromID(UUID ID);

	/**
	 * Indicates whether the subclass has groups present.
	 * @return A boolean indicating membership.
	 */
	boolean hasObjectGroups();
	
	default boolean hasObjectGroup(T group) {
		if (hasObjectGroups())
			return getObjectGroups().contains(group);
		
		return false;
	}

	/**
	 * Indicates whether the group name is taken or not.
	 * @param name The name of the group to be tested.
	 * @return A boolean indicating if it is taken or not.
	 */
	default boolean hasObjectGroupName(String name) {
		if (hasObjectGroups()) {
			for (T group : getObjectGroups()) {
				if (group.getName().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}
}
