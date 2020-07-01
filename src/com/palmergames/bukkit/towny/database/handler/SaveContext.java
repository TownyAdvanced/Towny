package com.palmergames.bukkit.towny.database.handler;

/**
 * The context for saving objects to the Database. This is useful
 * in situations in custom handlers where you need to save an object
 * from an already-written handler.
 */
public class SaveContext {
	
	DatabaseHandler handler;

	/**
	 * Creates a new save context with the respective database handler.
	 *
	 * @param handler The database handler to use.
	 */
	public SaveContext(DatabaseHandler handler) {
		this.handler = handler;
	}

	/**
	 * Loads the specified type from the given string.
	 *
	 * @param obj object to convert.
	 * @param type The type of object
	 * @param <T> The type of object class.
	 * @return The class type.
	 */
	public <T> String toStoredString(T obj, Class<? extends T> type) {
		return handler.toStoredString(obj, type);
	}
	
}
