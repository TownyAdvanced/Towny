package com.palmergames.bukkit.towny.database.type;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SaveHandler;
import com.palmergames.bukkit.towny.database.handler.DatabaseHandler;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SQLData;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.JDBCType;

/**
 * A class which is used a method of transport for type translations. This object is 
 * referenced during loading and saving in order to process how to convert objects
 * from their respective storage formats to a code-ready one.
 * 
 * @param <T> The object that is being adapted.
 * @author Suneet Tipirneni (Siris)
 */
public class TypeAdapter<T> {

	DatabaseHandler databaseHandler;
	LoadHandler<T> loadHandler;
	SaveHandler<T> saveHandler;

	/**
	 * Returns a custom string representation of the given object to be
	 * saved in flatfile.
	 * 
	 * @param object The object to get the format from.
	 * @return custom String representation for flatfile.
	 */
	public String getFileFormat(T object) {
		
		// Default behavior; Just use .toString() if
		// behavior hasn't been defined.
		if (saveHandler == null) {
			return object.toString();
		}
		
		// Call handler.
		SaveContext saveContext = new SaveContext(databaseHandler);
		return saveHandler.getFileString(saveContext, object);
	}

	/**
	 * Returns a given object from it's string flatfile format.
	 * 
	 * @param str The string to parse.
	 * @return The object from the representative string.
	 */
	public T fromFileFormat(String str) {
		
		// Default behavior.
		if (loadHandler == null) {
			return null;
		}

		LoadContext loadContext = new LoadContext(databaseHandler);
		return loadHandler.loadString(loadContext, str);
	}

	/**
	 * Gets the SQL for the given object.
	 * 
	 * @param object The object to get the SQL data from.
	 * @return A {@link SQLData} object respective to the object.
	 */
	public SQLData getSQL(T object) {
		
		if (saveHandler == null) {
			return null;
		}
		
		
		
		SaveContext saveContext = new SaveContext(databaseHandler);
		return saveHandler.getSQL(saveContext, object);
	}

	/**
	 * Convert the given Object from the SQL results to its proper object form.
	 * 
	 * @param obj The object to convert.
	 * @return The object in its natural form.
	 */
	public T fromSQL(T obj) {
		return null;
	}
	
	public TypeAdapter(DatabaseHandler dataBaseHandler, LoadHandler<T> loadHandler, SaveHandler<T> saveHandler) {
		this.databaseHandler = dataBaseHandler;
		this.loadHandler = loadHandler;
		this.saveHandler = saveHandler;
	}

	private boolean isPrimitive(Object object) {
		Class<?> type = object.getClass();
		boolean primitive = type == int.class;
		primitive |= type == boolean.class;
		primitive |= type == char.class;
		primitive |= type == float.class;
		primitive |= type == double.class;
		primitive |= type == long.class;
		primitive |= type == byte.class;
		
		return primitive;
	}
	
}
