package com.palmergames.bukkit.towny.database.dbHandlers.flatfile;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.dbHandlers.TypeAdapter;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SaveHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLLoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLSaveHandler;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.bukkit.Location;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseHandler {
	private ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	
	public DatabaseHandler() {
		// Register ALL default handlers.
		registerAdapter(Resident.class, new ResidentHandler());
		registerAdapter(Location.class, new LocationHandler());
	}
	
	public void save(Object obj) {
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);
		
		for (Field field : fields) {
			Class<?> type = field.getType();
			field.setAccessible(true);
			
			Object value = null;
			try {
				value = field.get(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String storedValue = toFileString(value, type);
			TownyMessaging.sendErrorMsg(field.getName() + "=" + storedValue);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> String toFileString(Object obj, Class<T> type) {
		TypeAdapter<T> adapter = getAdapter(type);
		
		if (obj == null) {
			return "null";
		}
		
		if (adapter == null) {
			return obj.toString();
		}
		
		return adapter.getFileFormat((T) obj);
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
	
	@SuppressWarnings("unchecked")
	public <T> void registerAdapter(Class<T> type, Object typeAdapter) {
		
		if (!(typeAdapter instanceof SaveHandler || typeAdapter instanceof LoadHandler
			|| typeAdapter instanceof SQLLoadHandler || typeAdapter instanceof SQLSaveHandler)) {
			
			throw new UnsupportedOperationException(typeAdapter + " is not a valid adapter.");
		}
		
		SaveHandler<T> flatFileSaveHandler = typeAdapter instanceof SaveHandler ? (SaveHandler<T>) typeAdapter : null;
		LoadHandler<T> flatFileLoadHandler = typeAdapter instanceof LoadHandler ? (LoadHandler<T>) typeAdapter : null;
		SQLSaveHandler<T> sqlSaveHandler = typeAdapter instanceof SQLSaveHandler ? (SQLSaveHandler<T>) typeAdapter : null;
		SQLLoadHandler<T> sqlLoadHandler = typeAdapter instanceof SQLLoadHandler ? (SQLLoadHandler<T>) typeAdapter : null;
		
		
		
		TypeAdapter<?> adapter = new TypeAdapter<>(this, flatFileLoadHandler, flatFileSaveHandler, sqlLoadHandler, sqlSaveHandler);
		
		// Add to hashmap.
		registeredAdapters.put(type, adapter);
	}
	
	@SuppressWarnings("unchecked")
	private <T> TypeAdapter<T> getAdapter(Class<T> type) {
		return (TypeAdapter<T>) registeredAdapters.get(type);
	}
}
