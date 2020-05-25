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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {
	
	private static final Map<Type, List<Field>> fieldCaches = new ConcurrentHashMap<>();

	/**
	 * Fetches all the fields from the TownyObject.
	 * 
	 * @param townyObject The TownyObject to get the fields from.
	 * @param ignoreTransient Indicates whether or not to get transient fields or not.
	 * @return A list of Fields from the TownyObject.
	 */
	public static @NotNull List<Field> getAllFields(@NotNull Object townyObject, boolean ignoreTransient) {
		Validate.notNull(townyObject);
		
		// Get the class object.
		Class<?> type = townyObject.getClass();
		
		// Check if cached.
		List<Field> fields = fieldCaches.get(type);
		
		if (fields != null) {
			return fields;
		}
		
		// Else reflect the fields
		fields = new ArrayList<>();

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
		
		// Cache the results
		fieldCaches.put(type, fields);
		
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
