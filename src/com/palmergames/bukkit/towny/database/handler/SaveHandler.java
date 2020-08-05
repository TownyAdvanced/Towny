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
	 * @param obj The object to convert to a saveable string.
	 * @return The String representation of the object.
	 */
	String toStoredString(T obj);
	
	default String serialize(Object obj) {
		return ObjectSerializer.serialize(obj);
	}
}
