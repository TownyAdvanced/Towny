package com.palmergames.bukkit.towny.database.handler;

public class SaveContext {
	
	DatabaseHandler handler;
	
	public SaveContext(DatabaseHandler handler) {
		this.handler = handler;
	}

	public <T> String toFileString(T obj, Class<T> type) {
		return handler.toFileString(obj, type);
	}

	public <T> SQLData toSQL(T obj, Class<T> type) {
		return handler.toSQL(obj, type);
	}
	
}
