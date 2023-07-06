package com.palmergames.bukkit.towny.object;

/**
 * A class used to show permission capability.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface Permissible {

	/**
	 * The the permissions of this object.
	 * 
	 * @param line The String line representation of the permissions.
	 */
	void setPermissions(String line);

	/**
	 * Gets the permissions of the object.
	 * 
	 * @return {@link TownyPermission} the permissions of the object.
	 */
	TownyPermission getPermissions();
}
