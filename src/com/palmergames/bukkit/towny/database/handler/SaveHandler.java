package com.palmergames.bukkit.towny.database.handler;

/**
 * Provide a vector for classes to define how specific objects
 * should be loaded.
 *
 * @param <T> The type of object being loaded.
 * @author Suneet Tipirneni (Siris)
 */
public interface SaveHandler<T> {
	/**
	 * Converts from object to save-able file string.
	 * 
	 * @param context The save convert.
	 * @param obj The object to convert to a saveable string.
	 * @return The String representation of the object.
	 */
	String getFileString(SaveContext context, T obj);

	/**
	 * Converts from object to SQL save-able ata.
	 * 
	 * @param context The save context.
	 * @param obj The object to convert.
	 * @return The SQL representation of the object.
	 */
	SQLData getSQL(SaveContext context, T obj);
}
