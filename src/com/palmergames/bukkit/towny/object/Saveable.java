package com.palmergames.bukkit.towny.object;

import java.io.File;

/**
 * Specifies *where* objects are saved in the Towny database.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface Saveable extends Nameable {
	/**
	 * Gets the parent directory of the where this file will be saved.
	 * 
	 * @return The File of the parent directory
	 */
	File getSaveDirectory();

	/**
	 * Gets the table for which this object will be saved in for SQL.
	 * 
	 * @return The name of the table.
	 */
	String getSQLTable();
}
