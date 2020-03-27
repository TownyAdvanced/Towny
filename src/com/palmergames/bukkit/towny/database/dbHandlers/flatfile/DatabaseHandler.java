package com.palmergames.bukkit.towny.database.dbHandlers.flatfile;

import com.palmergames.bukkit.towny.database.dbHandlers.TypeAdapter;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SaveHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLLoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLSaveHandler;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseHandler {
	private ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	
	public DatabaseHandler() {
		// Register ALL default handlers.
	}
	
	public <T> String toFileString(T obj, Class<T> type) {
		TypeAdapter<T> adapter = getAdapter(type);
		
		if (adapter == null) {
			return obj.toString();
		}
		
		return adapter.getFileFormat(obj);
	}
	
	public <T> SQLData<T> toSQL(T obj, Class<T> type) {
		TypeAdapter<T> adapter = getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is no adapter for " + type);
		}
		
		return adapter.getSQL(obj, type);
	}
	
	public <T> T fromFileString(String str, Class<T> type) {
		TypeAdapter<T> adapter = getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is no adapter for " + type);
		}
		
		return adapter.fromFileFormat(str);
	}
	
	public <T> T fromSQL(Object obj, Class<T> type) {
		TypeAdapter<T> adapter = getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is no adapter for " + type);
		}
		
		return adapter.fromSQL(null);
	}
	
	public <T> void registerAdapter(Class<T> type, Object typeAdapter) {
		
		if (!(typeAdapter instanceof SaveHandler || typeAdapter instanceof LoadHandler
			|| typeAdapter instanceof SQLLoadHandler || typeAdapter instanceof SQLSaveHandler)) {
			
			throw new UnsupportedOperationException(typeAdapter + " is not a valid adapter.");
		}
		
		SaveHandler<T> flatFileSaveHandler = typeAdapter instanceof SaveHandler ? (SaveHandler<T>) typeAdapter : null;
		LoadHandler<T> flatFileLoadHandler = typeAdapter instanceof LoadHandler ? (LoadHandler<T>) typeAdapter : null;
		SQLSaveHandler<T> sqlSaveHandler = typeAdapter instanceof SQLSaveHandler ? (SQLSaveHandler<T>) typeAdapter : null;
		SQLLoadHandler<T> sqlLoadHandler = typeAdapter instanceof SQLLoadHandler ? (SQLLoadHandler<T>) typeAdapter : null;
		
		TypeAdapter<?> adapter = new TypeAdapter<T>(this,flatFileLoadHandler, flatFileSaveHandler, sqlLoadHandler, sqlSaveHandler);
		
		// Add to hashmap.
		registeredAdapters.put(type, adapter);
	}
	
	private <T> TypeAdapter<T> getAdapter(Class<T> type) {
		return (TypeAdapter<T>) registeredAdapters.get(type);
	}
}
