package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.database.dbHandlers.BaseTypeHandlers;
import com.palmergames.bukkit.towny.database.dbHandlers.ListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.NationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.SetHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyPermissionsHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyWorldHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.UUIDHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.WorldCoordHandler;
import com.palmergames.bukkit.towny.database.type.TypeAdapter;
import com.palmergames.bukkit.towny.database.type.TypeContext;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ObjectSerializer {
	private static final ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	
	static {
		// Register ALL default handlers.
		registerAdapter(String.class, BaseTypeHandlers.STRING_HANDLER);
		registerAdapter(UUID.class, new UUIDHandler());
		registerAdapter(Integer.class, BaseTypeHandlers.INTEGER_HANDLER);
		registerAdapter(new TypeContext<List<String>>(){}.getType(), BaseTypeHandlers.STRING_LIST_HANDLER);

		registerAdapter(Resident.class, new ResidentHandler());
		registerAdapter(Location.class, new LocationHandler());
		registerAdapter(List.class, new ListHandler());
		registerAdapter(Set.class, new SetHandler());
		registerAdapter(WorldCoord.class, new WorldCoordHandler());
		registerAdapter(TownBlock.class, new TownBlockHandler());
		registerAdapter(Nation.class, new NationHandler());
		registerAdapter(TownyWorld.class, new TownyWorldHandler());
		registerAdapter(TownyPermission.class, new TownyPermissionsHandler());
		registerAdapter(Town.class, new TownHandler());
	}

	/**
	 * Registers an adapter to use with loading fields.
	 *
	 * Note: Primitives are handled automatically.
	 *
	 * @param type The type of the object.
	 * @param typeAdapter The adapter to use with the object.
	 * @param <T> The parameterized type.
	 */
	public static <T> void registerAdapter(Type type, Object typeAdapter) {

		if (!(typeAdapter instanceof SaveHandler || typeAdapter instanceof LoadHandler)) {
			throw new UnsupportedOperationException(typeAdapter + " is not a valid adapter.");
		}

		SaveHandler<T> flatFileSaveHandler = typeAdapter instanceof SaveHandler ? (SaveHandler<T>) typeAdapter : null;
		LoadHandler<T> flatFileLoadHandler = typeAdapter instanceof LoadHandler ? (LoadHandler<T>) typeAdapter : null;

		TypeAdapter<?> adapter = new TypeAdapter<>(flatFileLoadHandler, flatFileSaveHandler);

		// Add to hashmap.
		registeredAdapters.put(type, adapter);
	}

	public static <T> String serialize(Object obj) {
		return serialize(obj, obj.getClass());
	}

	public static <T> String serialize(Object obj, Type type) {
		// If object is null, just store an empty string
		if (obj == null) {
			return "";
		}

		// Store enum name if object is an an enum
		if (obj instanceof Enum<?>) {
			return ((Enum<?>) obj).name();
		}

		// If the object has a type adapter, use that to store the string.
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);
		if (adapter != null) {
			return adapter.toStoredString((T) obj);
		}

		// If iterable store as a list.
		if (ReflectionUtil.isIterableType(obj.getClass())) {
			System.out.println("Got here"); // FIXME DEBUG
			return iterableToString(obj, type);
		}

		// Default to toString()
		return obj.toString();
	}

	private static <T> String iterableToString(Object obj, Type type) {
		if (ReflectionUtil.isArray(obj.getClass())) {
			return arrayToString(obj, type);
		}
		else {
			Type rawType = ReflectionUtil.getRawType(obj.getClass());
			TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(rawType);
			if (adapter == null) {
				// Resort to iterator
				return arrayToString(obj, type);
			}
			return adapter.toStoredString((T) obj);
		}
	}

	private static <T> String arrayToString(Object obj, Type type) {
		Iterator<?> iterator = ReflectionUtil.resolveIterator(obj);
		StringJoiner joiner = new StringJoiner(",");

		// Get the parameterized type.
		Type genericType = ReflectionUtil.getTypeOfIterable(type);

		// Iterate through it, and build the list string.
		while (iterator.hasNext()) {
			String s = serialize(iterator.next(), genericType);
			if (s != null) {
				joiner.add(s);
			}
		}

		return "[" + joiner.toString() + "]";
	}

	private static <T, P> T stringToArray(String str, Type type) {
		// Check if string is of pattern "[]"
		if (str.length() > 1
			&& 	str.charAt(0) == '['
			&& str.charAt(str.length() - 1) == ']') {
			// Remove the brackets
			String pureList = str.substring(1, str.length() - 1);
			// Get the parameterized type of the array. E.g. int[] will get the int type
			Type parameterizedType = ReflectionUtil.getTypeOfIterable(type);
			// Convert the string into a string[]
			String[] splitArray = pureList.split(",");
			// Create a new array from the parameterized type, e.g. int[]
			P[] array = (P[]) Array.newInstance((Class<?>) parameterizedType, splitArray.length);
			// Insert converted values from the string array to to the parameterized array
			for (int i = 0; i < splitArray.length; i++) {
				array[i] = deserialize(splitArray[i], parameterizedType);
			}
		}
		throw new UnsupportedOperationException("Invalid array load for type " + type + " and string " + str);
	}

	private static <T, P> T stringToCollection(String str, Type type) {

		// Get the parameterized and raw type
		Type parameterizedType = ReflectionUtil.getTypeOfIterable(type);
		Type rawType = ReflectionUtil.getRawType(type);

		// Make sure the raw type is a class
		if (rawType instanceof Class<?>) {
			Class<?> rawTypeClass = (Class<?>) rawType;
			// Make sure the raw type is a collection
			if (ReflectionUtil.isCollection(rawTypeClass)) { ;
				// Get the adapter for the raw type, make sure the type is a
				TypeAdapter<?> adapter = fitAdapterFromClass(rawTypeClass, ReflectionUtil::isCollection);
				if (adapter != null) {
					// Get a collection the adapter
					Collection<?> collection = (Collection<?>) adapter.fromStoredString(str);
					try {
						// Construct that collection from an empty parameter
						Constructor<T> rawConstructor = (Constructor<T>) collection.getClass().getConstructor();
						Collection<P> rawInstance = (Collection<P>) rawConstructor.newInstance();
						// Check if collection from the adapter has any elements
						if (!collection.isEmpty()) {
							for (Object s : collection) {
								// Make sure it's converting from a string
								if (s instanceof String) {
									P element = deserialize((String) s, parameterizedType);
									if (element != null)
										rawInstance.add(element);
								}
							}
						}
						return (T) rawInstance;
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			}
		}
		throw new UnsupportedOperationException("No adapter found for iterable " + type + " with string " + str);
	}

	private static TypeAdapter<?> fitAdapterFromClass(Class<?> clazz, Predicate<Class<?>> fitFilter) {
		// This will get an adapter for a class from its interfaces, and its super classes
		// as long as those pass the filter provided.
		TypeAdapter<?> adapter = getAdapter(clazz);
		if (adapter == null) {
			for (Class<?> clazzInterface : clazz.getInterfaces()) {
				if (fitFilter.test(clazz) &&
					(adapter = getAdapter(clazzInterface)) != null)
					return adapter;
			}

			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null && fitFilter.test(superClass)) {
				// Recursively call the method for the super class
				return fitAdapterFromClass(superClass, fitFilter);
			}
		}

		return adapter;
	}

	public static @Nullable Object deserializeField(Field field, String fieldValue) {
		Type type = field.getGenericType();
		Class<?> classType = field.getType();
		
		
		if (ReflectionUtil.isPrimitive(type)) {
			return loadPrimitive(fieldValue, type);
		} else if (classType.isEnum()) {
			return ReflectionUtil.loadEnum(fieldValue, classType);
		} else {
			return deserialize(fieldValue, type);
		}
	}

	public static final <T> @Nullable T deserialize(String str, Type type) {
		if (str.isEmpty()) {
			return null;
		}

		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);

		if (adapter == null) {
			if (ReflectionUtil.isArray(type)) {
				return stringToArray(str, type);
			}

			if (ReflectionUtil.isIterableType(type)) {
				return stringToCollection(str, type);
			}

			throw new UnsupportedOperationException("There is no flatfile load adapter for " + type.getClass() + " " + str);
		}


		return adapter.fromStoredString(str);
	}

	private static TypeAdapter<?> getAdapter(Type type) {
		return registeredAdapters.get(type);
	}

	protected static final Object loadPrimitive(String str, Type type) {

		if (!ReflectionUtil.isPrimitive(type)) {
			throw new UnsupportedOperationException(type + " is not primitive, cannot parse");
		}

		if (type == int.class) {
			return Integer.parseInt(str);
		} else if (type == boolean.class) {
			return Boolean.parseBoolean(str);
		} else if (type == char.class) {
			return str.charAt(0);
		} else if (type == float.class) {
			return  Float.parseFloat(str);
		} else if (type == double.class) {
			return Double.parseDouble(str);
		}else if (type == long.class) {
			return Long.parseLong(str);
		} else if (type == byte.class) {
			return Byte.parseByte(str);
		}

		return null;
	}

	static String getSQLColumnDefinition(Type type) {
		if (type == int.class || type == Integer.class) {
			return "INTEGER";
		} else if (type == boolean.class || type == Boolean.class) {
			return "BOOLEAN NOT NULL DEFAULT '0'";
		} else if (type == char.class || type == Character.class) {
			return "CHAR(1)";
		} else if (type == float.class || type == Float.class) {
			return "FLOAT";
		} else if (type == double.class || type == Double.class) {
			return "DOUBLE";
		} else if (type == long.class || type == Long.class) {
			return "LONG";
		} else if (type == byte.class || type == Byte.class) {
			return "BIT(8)";
		}

		TypeAdapter<?> typeAdapter = getAdapter(type);

		if (typeAdapter != null) {
			return typeAdapter.getSQLColumnDefinition();
		}

		return SQLStringType.MEDIUM_TEXT.getColumnName();
	}


}
