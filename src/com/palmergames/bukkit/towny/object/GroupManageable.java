package com.palmergames.bukkit.towny.object;

import java.util.Set;

/**
 * @author Suneet Tipirneni (Siris)
 */
public interface GroupManageable<T extends Groupable> {
	Set<T> getGroups();
	Groupable getGroupFromID(int ID);
	
	default boolean hasGroups() {
		return getGroups() != null;
	}
	
	default boolean hasGroup(Groupable group) {
		if (hasGroups())
			return getGroups().contains(group);
		
		return false;
	}
	
	default boolean hasGroupName(String name) {
		if (hasGroups()) {
			for (Groupable group : getGroups()) {
				if (group.getGroupName().equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}
}
