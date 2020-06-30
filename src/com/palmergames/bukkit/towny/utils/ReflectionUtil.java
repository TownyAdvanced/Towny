package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.database.handler.ObjectContext;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionUtil {
	
	private static final Map<Type, List<Field>> fieldCaches = new ConcurrentHashMap<>();

	/**
	 * Fetches all the fields from the TownyObject.
	 *
	 * @param townyObject The TownyObject to get the fields from.
	 * @return A list of Fields from the TownyObject.
	 */
	public static @NotNull List<Field> getNonTransientFields(@NotNull Object townyObject) {
		return getNonTransientFields(townyObject, null);
	}
	
	/**
	 * Fetches all the fields from the passed in class.
	 * 
	 * @param objType The class to get the fields from
	 * @return A list of Fields from the class passed in.
	 */
	public static @NotNull List<Field> getNonTransientFields(@NotNull Class<?> objType) {
		Validate.notNull(objType);
		return getNonTransientFields(objType, null);
	}

	public static @NotNull List<Field> getNonTransientFields(@NotNull Object townyObj, @Nullable Predicate<Field> filter) {
		Validate.notNull(townyObj);

		// Get the class object.
		Class<?> type = townyObj.getClass();
		return getNonTransientFields(type, filter);
	}

	public static @NotNull List<Field> getNonTransientFields(@NotNull Class<?> objType, @Nullable Predicate<Field> filter) {
		Validate.notNull(objType);
		
		// Check if cached.
		List<Field> fields = fieldCaches.get(objType);
		
		if (fields == null) {
			// Use a stack to evaluate classes in proper top-down hierarchy.
			ArrayDeque<Class<?>> classStack = new ArrayDeque<>();

			// Iterate through superclasses.
			for (Class<?> c = objType; c != null; c = c.getSuperclass()) {
				classStack.push(c);
			}

			fields = new ArrayList<>();

			for (Class<?> classType : classStack) {
				for (Field field : classType.getDeclaredFields()) {
					// Check transient and filter
					if (!Modifier.isTransient(field.getModifiers())) {
						fields.add(field);
					}
				}
			}

			// Cache the results
			fieldCaches.put(objType, fields);
		}
		
		if (filter == null)
			return Collections.unmodifiableList(fields);
		else {
			return fields.stream().filter(filter).collect(Collectors.toList());
		}
	}

	/**
	 * Gets the object map of the object in the format of:
	 * fieldName = fieldValue
	 *
	 * @param obj The object to get the map from.
	 * @return The map.
	 */
	public static Map<String, ObjectContext> getObjectMap(Object obj) {
		return getObjectMap(obj, null);
	}

	/**
	 * Gets the object map of the object in the format of:
	 * fieldName = fieldValue
	 *
	 * @param obj The object to get the map from.
	 * @param filter Fields to filter out   
	 * @return The map.
	 */
	public static Map<String, ObjectContext> getObjectMap(Object obj, @Nullable Predicate<Field> filter) {

		HashMap<String, ObjectContext> dataMap = new HashMap<>();
		List<Field> fields = getNonTransientFields(obj);

		for (Field field : fields) {
			if (filter != null && !filter.test(field))
				continue;

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
	
	public static boolean isIterableType(Object obj) {
		return obj instanceof Iterable || obj.getClass().isArray();
	}

	/**
	 * Takes an arbitrary object and tries to extract it's iterable type.
	 * 
	 * @param obj The object to extract for iteration
	 * @return An iterator extracted from the given object.
	 * @throws UnsupportedOperationException When object is not iterable
	 */
	public static @NotNull Iterator<?> resolveIterator(@NotNull Object obj) {
		// Check if it's a primitive array.
		if (obj.getClass().isArray()) {
			Object[] array = (Object[]) obj;
			return (Arrays.asList(array)).iterator();
		}
		else if (obj instanceof Iterable) {
			try {
				return ((Iterable<?>)obj).iterator();
			} catch (ClassCastException e) {
				throw new UnsupportedOperationException("Iterable is not a valid type!");
			}
		}

		throw new UnsupportedOperationException("The given type: " + obj.getClass() + ", is not iterable.");
	}
	
	public static Type getTypeOfIterable(Field field) {
		try {
			ParameterizedType iterableType = (ParameterizedType) field.getGenericType();
			Type[] typeArgs = iterableType.getActualTypeArguments();
			if (typeArgs.length > 0)
				return typeArgs[0];
		} catch (Exception ex) {
		}

		throw new UnsupportedOperationException("No type argument found for field " + field.getName());
	}  

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> @NotNull T loadEnum(String str, Class<?> type) {
		return Enum.valueOf((Class<T>)type, str);
	}
	
	public static void dump(Object obj) {

		System.out.println("================= " + obj + " =================");
		
		for (Field field : getNonTransientFields(obj)) {
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
