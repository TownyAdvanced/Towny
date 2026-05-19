package com.palmergames.bukkit.towny.object;

import java.util.Map;

import com.palmergames.bukkit.towny.exceptions.ObjectSaveException;

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
	
	/**
	 * 
	 * @return a Map which stores keys and values, meant to be written to a database.
	 * @throws ObjectSaveException when something cannot be saved.
	 */
	Map<String, Object> getObjectDataMap() throws ObjectSaveException;
}
