package com.palmergames.bukkit.towny.database.handler;

import java.lang.reflect.Type;

public class LoadContext {
	
	DatabaseHandler handler;

	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}
	
	public <T> T fromFileString(String str, Type type) {
		return handler.fromFileString(str, type);
	}

	public <T> T fromSQL(Object obj, Class<T> type) {
		return handler.fromSQL(obj, type);
	}
	
}
