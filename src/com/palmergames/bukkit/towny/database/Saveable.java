package com.palmergames.bukkit.towny.database;

import com.palmergames.bukkit.towny.object.Identifiable;
import com.palmergames.bukkit.towny.object.Nameable;

import java.io.File;

/**
 * Specifies *where* objects are saved in the Towny database.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface Saveable extends Nameable, Identifiable, Changed {
	/**
	 * Gets the path of the where this file will be saved.
	 * 
	 * @return The path of the file.
	 */
	File getSavePath();

	/**
	 * Gets the table for which this object will be saved in for SQL.
	 * 
	 * @return The name of the table.
	 */
	String getSQLTable();
}
