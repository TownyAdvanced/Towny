package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object;

import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.DatabaseHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

public class SaveContext {
	
	DatabaseHandler handler;
	
	public SaveContext(DatabaseHandler handler) {
		this.handler = handler;
	}

	public <T> String toFileString(T obj, Class<T> type) {
		return handler.toFileString(obj, type);
	}

	public <T> SQLData<T> toSQL(T obj, Class<T> type) {
		return handler.toSQL(obj, type);
	}
	
}
