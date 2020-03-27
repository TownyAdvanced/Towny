package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.DatabaseHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

public class LoadContext {
	DatabaseHandler handler;

	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}
	
	public <T> String toFileString(T obj, Class<T> type) {
		return handler.toFileString(obj, type);
	}
	public <T> T fromFileString(String str, Class<T> type) {
		return handler.fromFileString(str, type);
	}

	public <T> SQLData<T> toSQL(T obj, Class<T> type) {
		return handler.toSQL(obj, type);
	}

	public <T> T fromSQL(Object obj, Class<T> type) {
		return handler.fromSQL(obj, type);
	}
	
	
}
