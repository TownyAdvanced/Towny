package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.DatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.sql.object.SQLData;

public class LoadContext<T> {
	DatabaseHandler handler;

	public LoadContext(DatabaseHandler handler) {
		this.handler = handler;
	}
	
	public String toFileString(T obj, Class<T> type) {
		return handler.toFileString(obj, type);
	}
	public T fromFileString(String str, Class<T> type) {
		return handler.fromFileString(str, type);
	}

	public SQLData<T> toSQL(T obj, Class<T> type) {
		return handler.toSQL(obj, type);
	}

	public T fromSQL(Object obj, Class<T> type) {
		return handler.fromSQL(obj, type);
	}
	
	
}
