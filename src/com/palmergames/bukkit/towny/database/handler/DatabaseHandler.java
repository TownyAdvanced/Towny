package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.dbHandlers.BaseTypeHandlers;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockHandler;
import com.palmergames.bukkit.towny.database.type.TypeAdapter;
import com.palmergames.bukkit.towny.database.type.TypeContext;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.util.FileMgmt;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class DatabaseHandler {
	private ConcurrentHashMap<Type, TypeAdapter<?>> registeredAdapters = new ConcurrentHashMap<>();
	private static final HashMap<String,String> replacementKeys = new HashMap<>();
	
	static {
		replacementKeys.put("outpostSpawns", "outpostspawns");
	}
	
	public DatabaseHandler() {
		// Register ALL default handlers.
		registerAdapter(String.class, BaseTypeHandlers.STRING_HANDLER);
		registerAdapter(UUID.class, BaseTypeHandlers.UUID_HANDLER);
		registerAdapter(Integer.class, BaseTypeHandlers.INTEGER_HANDLER);
		
		registerAdapter(Resident.class, new ResidentHandler());
		registerAdapter(Location.class, new LocationHandler());
		registerAdapter(new TypeContext<List<Resident>>(){}.getType(), new ResidentListHandler());
		registerAdapter(new TypeContext<List<Location>>(){}.getType(), new LocationListHandler());
		registerAdapter(TownBlock.class, new TownBlockHandler());
	}
	
	private boolean isPrimitive(Type type) {
		boolean primitive = type == int.class || type == Integer.class;
		primitive |= type == boolean.class || type == Boolean.class;
		primitive |= type == char.class || type == Character.class;
		primitive |= type == float.class || type == Float.class;
		primitive |= type == double.class || type == Double.class;
		primitive |= type == long.class || type == Long.class;
		primitive |= type == byte.class || type == Byte.class;
		
		return primitive;
	}
	
	public void save(Saveable obj) {
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);
		HashMap<String, String> saveMap = new HashMap<>();
		for (Field field : fields) {
			Type type = field.getGenericType();
			field.setAccessible(true);
			
			Object value = null;
			try {
				value = field.get(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			String valueStr = toFileString(value, type);
			
			String name = field.getName();
			saveMap.put(name, valueStr);
		}
		
		// Get the save getters
		for (Method method : obj.getClass().getMethods()) {
			SaveGetter saveGetter = method.getDeclaredAnnotation(SaveGetter.class);
			if (saveGetter != null) {
				String key = saveGetter.keyName();
				Type type = method.getGenericReturnType();
				Object value;
				try {
					value = method.invoke(obj);
				} catch (IllegalAccessException | InvocationTargetException e) {
					TownyMessaging.sendErrorMsg(e.getMessage());
					continue;
				}
				String valueStr = toFileString(value, type);
				
				saveMap.put(key, valueStr);
			}
		}
		
		FileMgmt.mapToFile(saveMap, new File(obj.getSaveDirectory() + "test.data"));
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
	
	public <T> void load(File file, Class<T> clazz) {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		T obj = null;
		try {
			assert objConstructor != null;
			obj = objConstructor.newInstance("");
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		assert obj != null;
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		HashMap<String, String> values = loadFileIntoHashMap(file);
		for (Field field : fields) {
			Type type = field.getGenericType();
			field.setAccessible(true);

			String fieldName = field.getName();
			if (replacementKeys.containsKey(fieldName)) {
				fieldName = replacementKeys.get(fieldName);
			}
			
			if (values.get(fieldName) == null) {
				continue;
			}
			
			Object value;
			
			if (isPrimitive(type)) {
				value = loadPrimitive(values.get(fieldName), type);
			} else {
				value = fromFileString(values.get(fieldName), type);
			}
			
			if (value == null) {
				// ignore it as another already allocated value may be there.
				continue;
			}
			
			LoadSetter loadSetter = field.getAnnotation(LoadSetter.class);
			
			try {

				if (loadSetter != null) {
					Method method = obj.getClass().getMethod(loadSetter.setterName(), field.getType());
					method.invoke(obj, value);
				} else {
					field.set(obj, value);
				}
				
				TownyMessaging.sendErrorMsg(field.getName() + "=" + field.get(obj));
			} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	public <T> String toFileString(Object obj, Type type) {
		TypeAdapter<T> adapter = (TypeAdapter<T>) getAdapter(type);
		
		if (obj == null) {
			return "null";
		}
		
		if (adapter == null) {
			return obj.toString();
		}
		
		return adapter.getFileFormat((T) obj);
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
			throw new UnsupportedOperationException("There is flatfile load adapter for " + type);
		}
		
		if (str.equals("")) {
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
	
	public Object loadPrimitive(String str, Type type) {
		
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

	private SQLData getPrimitiveSQL(Object object) {
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
}
