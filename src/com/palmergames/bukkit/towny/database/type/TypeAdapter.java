package com.palmergames.bukkit.towny.database.type;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SaveHandler;
import com.palmergames.bukkit.towny.database.handler.DatabaseHandler;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;

import java.lang.reflect.Method;

/**
 * A class which is used a method of transport for type translations. This object is 
 * referenced during loading and saving in order to process how to convert objects
 * from their respective storage formats to a code-ready one.<p>
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
	public String toStoredString(T object) {
		
		// Default behavior; Just use .toString() if
		// behavior hasn't been defined.
		if (saveHandler == null) {
			return object.toString();
		}
		
		// Call handler.
		SaveContext saveContext = new SaveContext(databaseHandler);
		return saveHandler.toStoredString(saveContext, object);
	}

	/**
	 * Returns a given object from it's string flatfile format.
	 * 
	 * @param str The string to parse.
	 * @return The object from the representative string.
	 */
	public T fromStoredString(String str) {
		
		// Default behavior.
		if (loadHandler == null) {
			return null;
		}

		LoadContext loadContext = new LoadContext(databaseHandler);
		return loadHandler.loadString(loadContext, str);
	}
	
	public TypeAdapter(DatabaseHandler dataBaseHandler, LoadHandler<T> loadHandler, SaveHandler<T> saveHandler) {
		this.databaseHandler = dataBaseHandler;
		this.loadHandler = loadHandler;
		this.saveHandler = saveHandler;
	}
	
	public String getSQLColumnDefinition() {
		if (saveHandler != null) {
			try {
				Method saveMethod = saveHandler.getClass().getMethod("toStoredString", SaveContext.class, Object.class);
				SQLString sqlAnnotation = saveMethod.getAnnotation(SQLString.class);

				if (sqlAnnotation != null) {
					SQLStringType type = sqlAnnotation.stringType();
					return type.getColumnName() +
						(sqlAnnotation.length() > 0 ? "(" + sqlAnnotation.length() + ")" : "");
				}
			} catch (ReflectiveOperationException exception) {
				TownyMessaging.sendErrorMsg(exception.getMessage());
			}
		}
		
		return SQLStringType.MEDIUM_TEXT.getColumnName();
	}
	
}
