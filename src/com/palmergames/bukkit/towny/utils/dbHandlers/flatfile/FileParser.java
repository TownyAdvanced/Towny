package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.utils.PrimitiveLoader;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileSaveContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class FileParser {

	public HashMap<Type, Handler<?>> loadHandlers = new HashMap<>();
	public HashMap<Field, Method> setters = new HashMap<>();
	
	public <T extends TownyObject> Object parseFile(File file, Class<T> clazz) {
		HashMap<String, String> keys = loadFileIntoHashMap(file);
		TownyObject obj;
		
		try {
			obj = clazz.getConstructor(String.class).newInstance("");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);
		
		for (Field field : fields) {
			field.setAccessible(true);
			String valStr = keys.get(field.getName());
			Object value = load(valStr, field);
			try {
				field.set(obj, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			field.setAccessible(false);
		}
		
		return obj;
	}
	
	public Object load(String str, Field field) {

		Type type = field.getGenericType();
		
		if (isPrimitive(type) || isWrappedPrimitive(type)) {
			return PrimitiveLoader.load(str, type);
		}
		
		Handler<?> handler = loadHandlers.get(type);

		if (!(handler instanceof LoadHandler)) {
			return null;
		}
		
		LoadContext context = new LoadContext(loadHandlers);
		
		return handler.load(context, str);
	}
	
	public <T> String save(T obj) {

		if (isPrimitive(obj.getClass()) || isWrappedPrimitive(obj.getClass())) {
			return String.valueOf(obj);
		}

		Handler<?> handler = loadHandlers.get(obj.getClass());

		if (!saveHandlers.containsKey(obj.getClass())) {
			return null;
		}
		
		FlatFileSaveContext context = new FlatFileSaveContext(loadHandlers);

		return handler.save(context, obj);
	}

	public boolean isPrimitive(Type type) {
		boolean retVal;

		retVal = type == int.class;
		retVal |= type == float.class;
		retVal |= type == double.class;
		retVal |= type == char.class;
		retVal |= type == boolean.class;
		retVal |= type == long.class;

		return retVal;
	}

	public boolean isWrappedPrimitive(Type type) {
		boolean retVal;

		retVal = type == Integer.class;
		retVal |= type == Double.class;
		retVal |= type == String.class;
		retVal |= type == Float.class;
		retVal |= type == Character.class;
		retVal |= type == Boolean.class;
		retVal |= type == Long.class;

		return retVal;
	}

	public void registerLoadHandler(Type type, LoadHandler<?> handler) {
		TownyMessaging.sendErrorMsg("added " + type);
		loadHandlers.put(type, handler);
	}

	/**
	 * Function which reads from a resident, town, nation, townyobject file, returning a hashmap. 
	 *
	 * @param file - File from which the HashMap will be made.
	 * @return HashMap<String, String> - Used for loading keys and values from object files. 
	 */
	public HashMap<String, String> loadFileIntoHashMap(File file) {
		HashMap<String, String> keys = new HashMap<>();
		try (FileInputStream fis = new FileInputStream(file);
			 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(isr);
			for (String key : properties.stringPropertyNames()) {
				String value = properties.getProperty(key);
				keys.put(key, String.valueOf(value));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return keys;
	}
}
