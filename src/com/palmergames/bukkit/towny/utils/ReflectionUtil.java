package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.database.handler.ObjectContext;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {
	
	private static final Map<Type, Field[]> fieldCaches = new ConcurrentHashMap<>();

	/**
	 * Fetches all the fields from the TownyObject.
	 *
	 * @param townyObject The TownyObject to get the fields from.
	 * @param ignoreTransient Indicates whether or not to get transient fields or not.
	 * @return A list of Fields from the TownyObject.
	 */
	public static @NotNull Field[] getAllFields(@NotNull Object townyObject, boolean ignoreTransient) {
		Validate.notNull(townyObject);

		// Get the class object.
		Class<?> type = townyObject.getClass();
		return getAllFields(type, ignoreTransient);
	}
	
	/**
	 * Fetches all the fields from the passed in class.
	 * 
	 * @param objType The class to get the fields from
	 * @param ignoreTransient Indicates whether or not to get transient fields or not.
	 * @return A list of Fields from the class passed in.
	 */
	public static @NotNull Field[] getAllFields(@NotNull Class<?> objType, boolean ignoreTransient) {
		// Check if cached.
		Field[] fields = fieldCaches.get(objType);
		
		if (fields != null) {
			return fields;
		}

		// Use a stack to evaluate classes in proper top-down hierarchy.
		ArrayDeque<Class<?>> classStack = new ArrayDeque<>();
		
		// Iterate through superclasses.
		int fieldCount = 0;
		for (Class<?> c = objType; c != null; c = c.getSuperclass()) {
			classStack.push(c);
			fieldCount += c.getDeclaredFields().length;
		}
		
		fields = new Field[fieldCount];

		int curIndex = 0;
		for (Class<?> classType : classStack) {
			for (Field field : classType.getDeclaredFields()) {
				// Ignore transient fields.
				if (ignoreTransient && Modifier.isTransient(field.getModifiers())) {
					continue;
				}
				
				fields[curIndex] = field;
				curIndex++;
			}
		}
		
		// Cache the results
		fieldCaches.put(objType, fields);
		
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
		Field[] fields = getAllFields(obj, true);

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
		return type == int.class || type == Integer.class
		|| type == boolean.class || type == Boolean.class
		|| type == char.class || type == Character.class
		|| type == float.class || type == Float.class
		|| type == double.class || type == Double.class
		|| type == long.class || type == Long.class
		|| type == byte.class || type == Byte.class;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> @NotNull T loadEnum(String str, Class<?> type) {
		return Enum.valueOf((Class<T>)type, str);
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

	/**
	 * A method to get the minimum viable object of the given type.
	 * 
	 * This should ONLY be used to fetch instance-wise data, and discarded
	 * afterwards as it is unsafe.
	 * 
	 * @param clazz The class to instantiate.
	 * @param <T> The class type.
	 * @return The unsafely allocated object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T unsafeNewInstance(Class<T> clazz) {
		try {
			return (T)Unsafe.getUnsafe().allocateInstance(clazz);
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
