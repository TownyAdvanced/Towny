package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLLoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLSaveHandler;

public class TypeAdapter<T> {

	DatabaseHandler handler;
	LoadHandler<T> flatFileLoadHandler;
	SaveHandler<T> flatFileSaveHandler;
	SQLLoadHandler<T> sqlLoadHandler;
	SQLSaveHandler<T> sqlSaveHandler;
	
	// FlatFile stuff
	public String getFileFormat(T object) {
		
		// Default behavior.
		if (flatFileSaveHandler == null) {
			return object.toString();
		}

		SaveContext saveContext = new SaveContext(handler);
		return flatFileSaveHandler.getFileString(saveContext, object);
	}
	
	
	public T fromFileFormat(String str) {
		
		// Default behavior.
		if (flatFileLoadHandler == null) {
			return null;
		}

		LoadContext loadContext = new LoadContext(handler);
		return flatFileLoadHandler.loadString(loadContext, str);
	}
	
	// SQL stuff
	public SQLData<T> getSQL(T object, Class<T> type) {
		return null;
	}
	public T fromSQL(T obj) {
		return null;
	}
	
	public TypeAdapter(DatabaseHandler dataBaseHandler, LoadHandler<T> flatFileLoadHandler, SaveHandler<T> flatFileSaveHandler,
					   SQLLoadHandler<T> sqlLoadHandler, SQLSaveHandler<T> sqlSaveHandler) {
		
		this.handler = dataBaseHandler;
		this.flatFileLoadHandler = flatFileLoadHandler;
		this.flatFileSaveHandler = flatFileSaveHandler;
		this.sqlLoadHandler = sqlLoadHandler;
		this.sqlSaveHandler = sqlSaveHandler;
	}
	
}
