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

	/**
	 * Converts the given SQL object to a it's natural object form.
	 * 
	 * @param context The load context.
	 * @param result The SQL-loaded object.
	 * @return The object in it's natural state.
	 */
	T loadSQL(LoadContext context, Object result);
}
