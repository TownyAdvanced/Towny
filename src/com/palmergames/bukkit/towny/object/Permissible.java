package com.palmergames.bukkit.towny.object;

/**
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface Permissible {
	
	void setPermissions(String line);
	
	TownyPermission getPermissions();
}
