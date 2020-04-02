package com.palmergames.bukkit.towny.database.dbHandlers.object;
import com.palmergames.bukkit.towny.database.dbHandlers.DatabaseHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

import java.lang.reflect.Type;

public class LoadContext {
	DatabaseHandler handler;

	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}
	
	public <T> String toFileString(T obj, Class<T> type) {
		return handler.toFileString(obj, type);
	}
	public <T> T fromFileString(String str, Type type) {
		return handler.fromFileString(str, type);
	}

	public <T> SQLData toSQL(T obj, Class<T> type) {
		return handler.toSQL(obj, type);
	}

	public <T> T fromSQL(Object obj, Class<T> type) {
		return handler.fromSQL(obj, type);
	}
	
	
}
