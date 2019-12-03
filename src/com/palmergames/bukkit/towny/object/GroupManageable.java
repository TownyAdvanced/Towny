package com.palmergames.bukkit.towny.object;

import java.util.Set;

/**
 * @author Suneet Tipirneni (Siris)
 */
public interface GroupManageable<T extends Group> {
	Set<T> getGroups();
	T getGroupFromID(int ID);
	
	default boolean hasGroups() {
		return getGroups() != null;
	}
	
	default boolean hasGroup(T group) {
		if (hasGroups())
			return getGroups().contains(group);
		
		return false;
	}
	
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
