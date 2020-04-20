package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.database.handler.ObjectContext;
import com.palmergames.bukkit.towny.object.TownyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class ReflectionUtil {

	/**
	 * Fetches all the fields from the TownyObject.
	 * 
	 * @param townyObject The TownyObject to get the fields from.
	 * @param ignoreTransient Indicates whether or not to get transient fields or not.
	 * @return A list of Fields from the TownyObject.
	 */
	public static List<Field> getAllFields(Object townyObject, boolean ignoreTransient) {
		
		// Get the class object.
		Class<?> type = townyObject.getClass();
		List<Field> fields = new ArrayList<>();

		// Use a stack to evaluate classes in proper top-down hierarchy.
		ArrayDeque<Class<?>> classStack = new ArrayDeque<>();
		
		// Iterate through superclasses.
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			classStack.push(c);
		}
		
		for (Class<?> classType : classStack) {
			for (Field field : classType.getDeclaredFields()) {
				// Ignore transient fields.
				if (ignoreTransient && Modifier.isTransient(field.getModifiers())) {
					continue;
				}
				
				fields.add(field);
			}
		}
		
		return fields;
	}

	/**
	 * Gets the object map of the object in the format of:
	 * fieldName = fieldValue
	 *
	 * @param obj The object to get the map from.
	 * @return The map.
	 */
	public static Map<String, ObjectContext> getObjectMap(Object obj) {

		HashMap<String, ObjectContext> dataMap = new HashMap<>();
		List<Field> fields = getAllFields(obj, true);

		for (Field field : fields) {
			// Open field.
			field.setAccessible(true);

			// Get field type
			Type type = field.getGenericType();

			// Fetch field value.
			Object value = null;
			try {
				value = field.get(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}

			String fieldName = field.getName();

			// Place value into map.
			dataMap.put(fieldName, new ObjectContext(value, type));

			// Close field up.
			field.setAccessible(false);
		}

		return dataMap;
	}
	
	public static boolean isPrimitive(Type type) {
		
		boolean primitive = type == int.class || type == Integer.class;
		primitive |= type == boolean.class || type == Boolean.class;
		primitive |= type == char.class || type == Character.class;
		primitive |= type == float.class || type == Float.class;
		primitive |= type == double.class || type == Double.class;
		primitive |= type == long.class || type == Long.class;
		primitive |= type == byte.class || type == Byte.class;
		
		return primitive;
		
	}
	
	public static void dump(Object obj) {

		System.out.println("================= " + obj + " =================");
		
		for (Field field : getAllFields(obj, true)) {
			field.setAccessible(true);
			Object value;
			try {
				value = field.get(obj);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
			
			if (value instanceof String) {
				value = "\"" + value + "\"";
			}

			field.setAccessible(false);

			System.out.println(field.getName() + " = " + value);
		}

		System.out.println("================= " + obj + " =================");
	}
}
