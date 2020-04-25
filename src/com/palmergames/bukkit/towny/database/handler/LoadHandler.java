package com.palmergames.bukkit.towny.database.handler;

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
	 * @param context The context object to convert.
	 * @param str The str to convert from.   
	 * @return The object in its natural form.
	 */
	T loadString(LoadContext context, String str);
}
