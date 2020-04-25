package com.palmergames.bukkit.towny.database.handler;

import java.lang.reflect.Type;

public class LoadContext {
	
	private DatabaseHandler handler;

	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}
	
	public <T> T fromStoredString(String str, Type type) {
		return getHandler().fromStoredString(str, type);
	}

	public DatabaseHandler getHandler() {
		return handler;
	}
}
