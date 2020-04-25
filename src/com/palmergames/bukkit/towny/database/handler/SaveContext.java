package com.palmergames.bukkit.towny.database.handler;

public class SaveContext {
	
	DatabaseHandler handler;
	
	public SaveContext(DatabaseHandler handler) {
		this.handler = handler;
	}

	public <T> String toStoredString(T obj, Class<T> type) {
		return handler.toStoredString(obj, type);
	}
	
}
