package com.palmergames.bukkit.towny.object;

/**
 * Basic interface that depicts whether an object has a specified save method.
 * Most, if not all, save methods will redirect to a specific method in {@link com.palmergames.bukkit.towny.db.TownyDataSource}.
 * 
 * Using the {@link Savable#save()} method is preferred to future proof against database class changes.
 * 
 */
public interface Savable {
	/**
	 * Schedules the object to be saved to the database.
	 */
	void save();
	
	String getSaveLocation();
}
