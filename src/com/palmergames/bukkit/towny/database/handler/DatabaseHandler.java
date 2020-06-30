package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.dbHandlers.BaseTypeHandlers;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.NationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyPermissionsHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyWorldHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.UUIDHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.OneToMany;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The object which is responsible for converting objects from one format to another and
 * saving the mentioned format.
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseHandler {
	private final ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Type, List<Field>> fieldOneToManyCache = new ConcurrentHashMap<>();
	
	public DatabaseHandler() {
		// Register ALL default handlers.
		registerAdapter(String.class, BaseTypeHandlers.STRING_HANDLER);
		registerAdapter(UUID.class, new UUIDHandler());
		registerAdapter(Integer.class, BaseTypeHandlers.INTEGER_HANDLER);
		registerAdapter(new TypeContext<List<String>>(){}.getType(), BaseTypeHandlers.STRING_LIST_HANDLER);
		
		registerAdapter(Resident.class, new ResidentHandler());
		registerAdapter(Location.class, new LocationHandler());
		registerAdapter(new TypeContext<List<Resident>>(){}.getType(), new ResidentListHandler());
		registerAdapter(new TypeContext<List<Location>>(){}.getType(), new LocationListHandler());
		registerAdapter(new TypeContext<List<TownBlock>>(){}.getType(), new TownBlockListHandler());
		registerAdapter(TownBlock.class, new TownBlockHandler());
		registerAdapter(Nation.class, new NationHandler());
		registerAdapter(TownyWorld.class, new TownyWorldHandler());
		registerAdapter(TownyPermission.class, new TownyPermissionsHandler());
		registerAdapter(Town.class, new TownHandler());
		
		// Loads all the bukkit worlds.
		loadWorlds();
	}
	
	@NotNull
	protected final List<Field> getOneToManyFields(@NotNull Saveable obj) {
		Validate.notNull(obj);
		
		// Check cache.
		List<Field> fields = fieldOneToManyCache.get(obj.getClass());
		
		if (fields != null) {
			return fields;
		}
		
		fields = new ArrayList<>();
		for (Field field : ReflectionUtil.getNonTransientFields(obj)) {
			
			if (!field.isAnnotationPresent(OneToMany.class)) {
				continue;
			}
			
			field.setAccessible(true);
			
			// Strong condition
			try {
				Validate.isTrue(ReflectionUtil.isIterableType(field.get(obj)),
					"The OneToMany annotation for field " + field.getName() +
						" in " + obj.getClass() + " is not an iterable type.");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			OneToMany rel = field.getAnnotation(OneToMany.class);
			
			if (rel != null) {
				fields.add(field);
			}
			
			field.setAccessible(false);
		}
		
		// Cache result.
		fieldOneToManyCache.putIfAbsent(obj.getClass(), fields);
		
		return fields;
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
				
				// Save
				save(wrappedWorld);
			} catch (AlreadyRegisteredException e) {
				//e.printStackTrace();
			}
		}
	}
	
	public <T> String toStoredString(Object obj, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);
		
		if (obj == null) {
			return "null";
		}
		
		if (obj instanceof Enum<?>) {
			return ((Enum<?>) obj).name();
		}
		
		if (adapter == null) {
			return obj.toString();
		}
		
		return adapter.toStoredString((T) obj);
	}
	
	public final <T> @Nullable T fromStoredString(String str, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is no flatfile load adapter for " + type);
		}
		
		if (str.equals("") || str.equals("null")) {
			return null;
		}
		
		if (str.equals("[]")) {
			return (T) new ArrayList<>();
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
	
	protected void safeFieldIterate(Iterable<Field> itr, Consumer<Field> forEach) {
		itr.forEach((field -> {
			
			if (field == null) {
				return;
			}
			
			field.setAccessible(true);
			forEach.accept(field);
			field.setAccessible(false);
		}));
	}

	// ---------- DB operation Methods ----------

	/**
	 * Stores a newly created object in the DB
	 * 
	 * @param obj The object to save
	 */
	public abstract void saveNew(@NotNull Saveable obj);
	
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
	}
}
