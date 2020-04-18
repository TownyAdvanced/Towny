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
import com.palmergames.bukkit.towny.database.type.TypeAdapter;
import com.palmergames.bukkit.towny.database.type.TypeContext;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.Trie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The object which is responsible for converting objects from one format to another and
 * saving the mentioned format.
 * 
 * @author Suneet Tipirneni (Siris)
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseHandler {
	private final ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	protected static final HashMap<String,String> replacementKeys = new HashMap<>();
	
	private final Trie townsTrie = new Trie();
	
	static {
		replacementKeys.put("outpostSpawns", "outpostspawns");
		replacementKeys.put("adminEnabledPVP", "adminEnabledPvP");
		replacementKeys.put("adminDisabledPVP", "adminEnabledPvP");
		replacementKeys.put("isTaxPercentage", "taxpercent");
		replacementKeys.put("jailSpawns", "jailspawns");
		replacementKeys.put("permissions", "protectionStatus");
		replacementKeys.put("isPublic", "public");
		replacementKeys.put("isOpen", "open");
		replacementKeys.put("isConquered", "conquered");
	}
	
	public DatabaseHandler() {
		// Register ALL default handlers.
		registerAdapter(String.class, BaseTypeHandlers.STRING_HANDLER);
		registerAdapter(UUID.class, BaseTypeHandlers.UUID_HANDLER);
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
		
	}
	
	protected boolean isPrimitive(Type type) {
		boolean primitive = type == int.class || type == Integer.class;
		primitive |= type == boolean.class || type == Boolean.class;
		primitive |= type == char.class || type == Character.class;
		primitive |= type == float.class || type == Float.class;
		primitive |= type == double.class || type == Double.class;
		primitive |= type == long.class || type == Long.class;
		primitive |= type == byte.class || type == Byte.class;
		
		return primitive;
	}
	
	public Map<String, ObjectContext> getObjectMap(Saveable obj) {
		
		HashMap<String, ObjectContext> dataMap = new HashMap<>();
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);
		
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
			
			if (replacementKeys.containsKey(fieldName)) {
				fieldName = replacementKeys.get(fieldName);
			}
			
			// Place value into map.
			dataMap.put(fieldName, new ObjectContext(value, type));
			
			// Close field up.
			field.setAccessible(false);
		}
		
		return dataMap;
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
	
	public abstract void save(Saveable obj);
	
	public void save(Saveable @NotNull ... objs) {
		for (Saveable obj : objs) {
			save(obj);
		}
	}
	
	public void saveSQL(Object obj) {
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		for (Field field : fields) {
			Type type = field.getGenericType();
			field.setAccessible(true);

			Object value = null;
			try {
				value = field.get(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			SQLData storedValue = toSQL(value, type);
			TownyMessaging.sendErrorMsg(field.getName() + "=" + storedValue);
		}
	}
	
	public void loadWorlds() {
		for (World world : Bukkit.getServer().getWorlds()) {
			try {
				TownyUniverse.getInstance().newWorld(world.getUID(), world.getName());
			} catch (AlreadyRegisteredException | NotRegisteredException e) {
				e.printStackTrace();
			}
		}
	}
	
	public abstract <T> T load(File file, Class<T> clazz);
	
	public <T> String toFileString(Object obj, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);
		
		if (obj == null) {
			return "null";
		}
		
		if (obj.getClass().isEnum()) {
			return ((Enum<?>) obj).name();
		}
		
		if (adapter == null) {
			return obj.toString();
		}
		
		return adapter.getFileFormat((T) obj);
	}
	
	public <T extends Enum<T>> String getEnumString(Enum<T> enumVal) {
		return "" + enumVal.ordinal();
	}
	
	public <T> SQLData toSQL(Object obj, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);
		
		if (obj == null) {
			return null;
		}
		
		if (isPrimitive(obj.getClass())) {
			return getPrimitiveSQL(obj);
		}

		if (adapter == null) {
			return new SQLData(obj.toString(), JDBCType.VARCHAR);
		}
		
		return adapter.getSQL((T) obj);
	}
	
	public <T> T fromFileString(String str, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is no flatfile load adapter for " + type);
		}
		
		if (str.equals("") || str.equals("null")) {
			return null;
		}
		
		return adapter.fromFileFormat(str);
	}
	
	public <T> T fromSQL(Object obj, Class<T> type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);

		if (adapter == null) {
			throw new UnsupportedOperationException("There is SQL load adapter for " + type);
		}
		
		return adapter.fromSQL(null);
	}
	
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
	
	public final Object loadPrimitive(String str, Type type) {
		
		if (!isPrimitive(type)) {
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

	private final SQLData getPrimitiveSQL(Object object) {
		Class<?> type = object.getClass();
		if (type == int.class || type == Integer.class) {
			return new SQLData(object, JDBCType.INTEGER);
		} else if (type == boolean.class || type == Boolean.class) {
			return new SQLData(object, JDBCType.BOOLEAN);
		} else if (type == char.class || type == Character.class) {
			return new SQLData(object, JDBCType.CHAR);
		} else if (type == float.class || type == Float.class) {
			return new SQLData(object, JDBCType.FLOAT);
		} else if (type == double.class || type == Double.class) {
			return new SQLData(object, JDBCType.DOUBLE);
		} else if (type == long.class || type == Long.class) {
			return new SQLData(object, JDBCType.BIGINT);
		} else if (type == byte.class || type == Byte.class) {
			return new SQLData(object, JDBCType.VARBINARY);
		}

		return null;
	}
	
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
	
	public void save(Collection<? extends Saveable> objs) {
		for (Saveable obj : objs) {
			save(obj);
		}
	}
	
	// These methods will differ greatly between inheriting classes,
	// hence they are abstract.
	public abstract Town loadTown(UUID id);
	public abstract Resident loadResident(UUID id);
	public abstract Nation loadNation(UUID id);
	public abstract TownyWorld loadWorld(UUID id);
	public abstract TownBlock loadTownBlock(UUID id);
	public abstract void loadAllResidents();
	public abstract void loadAllWorlds();
	public abstract void loadAllTowns();
	public abstract void loadAllTownBlocks();
	
	public void loadAll() {

		// 1.) Load Worlds
		loadAllWorlds();

		// 2.) Load Towns
		loadAllTowns();
		
		// 3. Load TownBlocks
		loadAllTownBlocks();

		// 4.) Load Residents
		loadAllResidents();
	}
	
	public abstract void load();
	
}
