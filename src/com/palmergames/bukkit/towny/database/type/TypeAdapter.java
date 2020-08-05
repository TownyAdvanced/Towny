package com.palmergames.bukkit.towny.database.type;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SaveHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;

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
	
	LoadHandler<T> loadHandler;
	SaveHandler<T> saveHandler;

	public TypeAdapter(LoadHandler<T> loadHandler, SaveHandler<T> saveHandler) {
		this.loadHandler = loadHandler;
		this.saveHandler = saveHandler;
	}

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
		return saveHandler.toStoredString(object);
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
		
		return loadHandler.loadString(str);
	}
	
	public String getSQLColumnDefinition() {
		if (saveHandler != null) {
			try {
				Method saveMethod = saveHandler.getClass().getMethod("toStoredString", Object.class);
				SQLString sqlAnnotation = saveMethod.getAnnotation(SQLString.class);

				if (sqlAnnotation != null) {
					return sqlAnnotation.stringType().getDefinition(sqlAnnotation.length());
				}
			} catch (ReflectiveOperationException exception) {
				TownyMessaging.sendErrorMsg(exception.getMessage());
			}
		}
		
		return SQLStringType.MEDIUM_TEXT.getColumnName();
	}
	
}
