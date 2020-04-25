package com.palmergames.bukkit.towny.database.handler;

import java.lang.reflect.Type;

/**
 * The context for loading objects from the Database. This is useful
 * in situations in custom handlers where you need to load an object
 * from an already-written handler. <p>
 * 
 * Note: This implementation will fail-fast whenever a field
 * of a custom type is encountered during the load phase.
 * 
 */
public class LoadContext {
	
	private final DatabaseHandler handler;

	/**
	 * Creates a new load context with the respective database handler.
	 * 
	 * @param handler The database handler to use.
	 */
	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}

	/**
	 * Loads the specified type from the given string.
	 * 
	 * @param str The <code>String</code> to load the object from.
	 * @param type The type of object to construct.
	 * @param <T> The type of constructed object.
	 * @return The constructed object.
	 */
	public <T> T fromStoredString(String str, Type type) {
		return getHandler().fromStoredString(str, type);
	}

	/**
	 * Gets the full database handler.
	 * 
	 * @return The database handler.
	 */
	public DatabaseHandler getHandler() {
		return handler;
	}
}
