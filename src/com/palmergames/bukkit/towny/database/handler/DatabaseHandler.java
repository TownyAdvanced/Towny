package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.dbHandlers.BaseTypeHandlers;
import com.palmergames.bukkit.towny.database.dbHandlers.ListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.NationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.NationListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyPermissionsHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyWorldHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.UUIDHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.database.handler.annotations.SaveGetter;
import com.palmergames.bukkit.towny.database.type.TypeAdapter;
import com.palmergames.bukkit.towny.database.type.TypeContext;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The object which is responsible for converting objects from one format to another and
 * saving the mentioned format.
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseHandler {
	private final ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	
	public DatabaseHandler() {
		// Register ALL default handlers.
		registerAdapter(String.class, BaseTypeHandlers.STRING_HANDLER);
		registerAdapter(UUID.class, new UUIDHandler());
		registerAdapter(Integer.class, BaseTypeHandlers.INTEGER_HANDLER);
		registerAdapter(new TypeContext<List<String>>(){}.getType(), BaseTypeHandlers.STRING_LIST_HANDLER);
		
		registerAdapter(Resident.class, new ResidentHandler());
		registerAdapter(Location.class, new LocationHandler());
		registerAdapter(List.class, new ListHandler());
		registerAdapter(new TypeContext<List<Resident>>(){}.getType(), new ResidentListHandler());
		registerAdapter(new TypeContext<List<Location>>(){}.getType(), new LocationListHandler());
		registerAdapter(new TypeContext<List<TownBlock>>(){}.getType(), new TownBlockListHandler());
		//registerAdapter(new TypeContext<List<Nation>>(){}.getType(), new NationListHandler());
		registerAdapter(TownBlock.class, new TownBlockHandler());
		registerAdapter(Nation.class, new NationHandler());
		registerAdapter(TownyWorld.class, new TownyWorldHandler());
		registerAdapter(TownyPermission.class, new TownyPermissionsHandler());
		registerAdapter(Town.class, new TownHandler());
	}

	Map<String, ObjectContext> getSaveGetterData(Saveable obj) {

		HashMap<String, ObjectContext> saveMap = new HashMap<>();

		// Get the save getters
		for (Method method : obj.getClass().getMethods()) {

			// Get the annotation from the method.
			SaveGetter saveGetter = method.getDeclaredAnnotation(SaveGetter.class);

			// Check if its present.
			if (saveGetter != null) {

				// Get the key name from the annotation.
				String key = saveGetter.keyName();
				
				// Get type
				Type type = method.getGenericReturnType();

				// Try to fetch the return value.
				Object value;
				try {
					value = method.invoke(obj);
				} catch (IllegalAccessException | InvocationTargetException e) {
					TownyMessaging.sendErrorMsg(e.getMessage());
					continue;
				}

				// Add to map.
				saveMap.put(key, new ObjectContext(value, type));
			}
		}

		return saveMap;
	}
	
	public void loadWorlds() {
		for (World world : Bukkit.getServer().getWorlds()) {
			try {
				
				TownyWorld wrappedWorld = new TownyWorld(world.getUID(), world.getName());
				TownyUniverse.getInstance().addWorld(wrappedWorld);
				TownyMessaging.sendErrorMsg("got here");
				// Save
				save(wrappedWorld);
			} catch (AlreadyRegisteredException e) {
				//e.printStackTrace();
			}
		}
	}
	
	public <T> String toStoredString(Object obj, Type type) {
		// If object is null, just store "null" string
		if (obj == null) {
			return "null";
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
			return iterableToString(obj);
		}

		// Default to toString()
		return obj.toString();
	}
	
	private <T> String iterableToString(Object obj) {
		if (ReflectionUtil.isArray(obj.getClass())) {
			return arrayToString(obj);
		}
		else {
			Type rawType = ReflectionUtil.getRawType(obj.getClass());
			TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(rawType);
			if (adapter == null) {
				System.out.println("[Towny] No adapter found for " + rawType); // FIXME DEBUG
				// Resort to iterator
				return arrayToString(obj);
			}
			return adapter.toStoredString((T) obj);
		}
	}
	
	private <T> String arrayToString(Object obj) {
		Iterator<?> iterator = ReflectionUtil.resolveIterator(obj);
		StringJoiner joiner = new StringJoiner(",");

		// Get the parameterized type.
		Type genericType = ReflectionUtil.getTypeOfIterable(obj.getClass());

		// Iterate through it, and build the list string.
		while (iterator.hasNext()) {
			String s = toStoredString(iterator.next(), genericType);
			if (s != null) {
				joiner.add(s);
			}
		}

		return "[" + joiner.toString() + "]";
	}
	
	private <T, P> T stringToArray(String str, Type type) {
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
				array[i] = fromStoredString(splitArray[i], parameterizedType);
			}
		}
		throw new UnsupportedOperationException("Invalid array load for type " + type + " and string " + str);
	}
	
	private <T, P> T stringToCollection(String str, Type type) {
		// Get the parameterized and raw type
		Type parameterizedType = ReflectionUtil.getTypeOfIterable(type);
		Type rawType = ReflectionUtil.getRawType(type);
		// Make sure the raw type is a class
		if (rawType instanceof Class<?>) {
			Class<?> rawTypeClass = (Class<?>) rawType;
			// Make sure the raw type is a collection
			if (ReflectionUtil.isCollection(rawTypeClass)) {
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
									P element = fromStoredString((String) s, parameterizedType);
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
	
	private TypeAdapter fitAdapterFromClass(Class<?> clazz, Predicate<Class<?>> fitFilter) {
		// This will get an adapter for a class from its interfaces, and its super classes
		// as long as those pass the filter provided.
		TypeAdapter adapter = getAdapter(clazz);
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
		
		return null;
	}
	
	public final <T, P> @Nullable T fromStoredString(String str, Type type) { 
		if (str.isEmpty() || str.equals("null")) {
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

			throw new UnsupportedOperationException("There is no flatfile load adapter for " + type);
		}


		return adapter.fromStoredString(str);
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
	public <T> void registerAdapter(Type type, Object typeAdapter) {
		
		if (!(typeAdapter instanceof SaveHandler || typeAdapter instanceof LoadHandler)) {
			throw new UnsupportedOperationException(typeAdapter + " is not a valid adapter.");
		}
		
		SaveHandler<T> flatFileSaveHandler = typeAdapter instanceof SaveHandler ? (SaveHandler<T>) typeAdapter : null;
		LoadHandler<T> flatFileLoadHandler = typeAdapter instanceof LoadHandler ? (LoadHandler<T>) typeAdapter : null;
		
		TypeAdapter<?> adapter = new TypeAdapter<>(this, flatFileLoadHandler, flatFileSaveHandler);
		
		// Add to hashmap.
		registeredAdapters.put(type, adapter);
	}
	
	private TypeAdapter<?> getAdapter(Type type) {
		return registeredAdapters.get(type);
	}
	
	protected final Object loadPrimitive(String str, Type type) {
		
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
		} else if (type == byte.class) {
			return Byte.parseByte(str);
		}
		
		return null;
	}

	// This method is in this class because TypeAdapter is not exposed
	protected final String getSQLColumnDefinition(Field field) {
		Class<?> type = field.getType();
		
		if (type == String.class) {
			SQLString sqlAnnotation = field.getAnnotation(SQLString.class);

			if (sqlAnnotation != null) {
				SQLStringType sqlType = sqlAnnotation.stringType();
				return sqlType.getColumnName() +
					(sqlAnnotation.length() > 0 ? "(" + sqlAnnotation.length() + ")" : "");
			}
		}
		
		return getSQLColumnDefinition(type);
	}

	protected final String getSQLColumnDefinition(Type type) {
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
	
	@SuppressWarnings({"deprecation", "unused"})
	public void upgrade() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Collection<TownyWorld> worlds = townyUniverse.getWorldMap().values();
		Collection<Nation> nations = townyUniverse.getNationsMap().values();
		Collection<Town> towns = townyUniverse.getTownsMap().values();
		Collection<TownBlock> townBlocks = townyUniverse.getTownBlocks();
		
		// MANUALLY Save older data items.
		save(worlds);
		save(nations);
		save(towns);
		save(townBlocks);
	}

	// ---------- DB operation Methods ----------
	
	/**
	 * Saves the given object to the DB.
	 *
	 * @param obj The object to save.
	 */
	public abstract void save(@NotNull Saveable obj);

	/**
	 * Removes the given object from the DB.
	 * 
	 * @param obj The object to delete.
	 * @return A boolean indicating if successful or not.
	 */
	public abstract boolean delete(@NotNull Saveable obj);

	/**
	 * Saves all given objects to the DB.
	 * 
	 * @param objs The objects to save.
	 */
	public final void save(Saveable @NotNull ... objs) {
		for (Saveable obj : objs) {
			save(obj);
		}
	}
	
	/**
	 * Saves the objects to the database.
	 * 
	 * @param objs The objects to save.
	 */
	public final void save(@NotNull Collection<? extends Saveable> objs) {
		Validate.notNull(objs);
		
		for (Saveable obj : objs) {
			save(obj);
		}
	}
	
	// These methods will differ greatly between inheriting classes,
	// hence they are abstract.

	// ---------- Load All Methods ----------
	public abstract void loadAllResidents();
	public abstract void loadAllWorlds();
	public abstract void loadAllNations();
	public abstract void loadAllTowns();
	public abstract void loadAllTownBlocks();

	/**
	 * Loads all necessary objects for the database.
	 */
	public final void loadAll() {
		loadAllWorlds();
		loadAllNations();
		loadAllTowns();
		loadAllResidents();
		loadAllTownBlocks();
		// Loads all the bukkit worlds if they haven't been loaded.
		loadWorlds();
	}
}
