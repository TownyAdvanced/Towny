package com.palmergames.bukkit.towny.database.handler;

import java.lang.reflect.Type;

/**
 * Provide a vector for classes to define how specific objects
 * should be loaded.
 * 
 * @param <T> The type of object being loaded.
 * @author Suneet Tipirneni (Siris)
 */
public interface LoadHandler<T>  {
	/**
	 * Converts the given Object from the SQL results to its proper object form.
	 *
	 * @param str The str to convert from.   
	 * @return The object in its natural form.
	 */
	T loadString(String str);
	
	default <O> O deserialize(String string, Type type) {
		return ObjectSerializer.deserialize(string, type);
	}
}
