package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SQLDatabaseHandler extends DatabaseHandler {
	SQLHandler sqlHandler;
	
	public SQLDatabaseHandler(String databaseType) {
		sqlHandler = new SQLHandler(databaseType);
		sqlHandler.testConnection();

		createTownyObjectTable("TOWNS", Town.class);
		createTownyObjectTable("NATIONS", Nation.class);
		createTownyObjectTable("RESIDENTS", Resident.class);
	}
	
	@Override
	public void save(Saveable obj) {
		// TODO Fill-in method with proper save algorithm
		throw new UnsupportedOperationException("save() Stub");
	}
	
	private String tblPrefix() {
		return TownySettings.getSQLTablePrefix();
	}
	
	private <T> String updateColumnsFromFields(Class<T> clazz) {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(UUID.class);
		} catch (NoSuchMethodException e) {
			TownyMessaging.sendErrorMsg("flag 1");
			e.printStackTrace();
		}

		T obj = null;
		try {
			Validate.isTrue(objConstructor != null);
			obj = objConstructor.newInstance((Object) null);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			TownyMessaging.sendErrorMsg("flag 2");
			e.printStackTrace();
		}

		Validate.isTrue(obj != null);
		
		return ReflectionUtil.getAllFields(obj, true).stream()
				.map(f -> f.getName() + " " + getSQLColumnDefinition(f))
				.collect(Collectors.joining(", "));
	}
	
	private <T extends TownyObject> void createTownyObjectTable(String tableName, Class<T> objectClazz) {
		tableName = tblPrefix() + tableName;
		// TODO Make PK an annotation
		String createTableStmt = "CREATE TABLE IF NOT EXISTS " + tableName +" ("
			+ "`uniqueIdentifier` VARCHAR(32) NOT NULL,"
			+ "PRIMARY KEY (`uniqueIdentifier`)"
			+ ")";

		String alterTableStmt = "ALTER TABLE " + tableName + " ADD COLUMN " + updateColumnsFromFields(objectClazz);

		sqlHandler.executeUpdatesError("Error creating table " + tableName + "!" , createTableStmt, alterTableStmt);
	}

	@Override
	public boolean delete(Saveable obj) {
		return false;
	}
	
	private <T> T load(ResultSet rs, @NotNull Class<T> clazz) throws SQLException {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(UUID.class);
		} catch (NoSuchMethodException e) {
			TownyMessaging.sendErrorMsg("flag 1");
			e.printStackTrace();
		}

		T obj = null;
		try {
			Validate.isTrue(objConstructor != null);
			obj = objConstructor.newInstance((Object) null);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			TownyMessaging.sendErrorMsg("flag 2");
			e.printStackTrace();
		}

		Validate.isTrue(obj != null);
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		Map<String, Object> values = rowToMap(rs);
		for (Field field : fields) {
			Type type = field.getGenericType();
			Class<?> classType = field.getType();
			field.setAccessible(true);

			String fieldName = field.getName();

			if (values.get(fieldName) == null) {
				continue;
			}

			Object value = values.get(fieldName);

			if (value instanceof String) {
				String stringValue = (String) value;
				if (!ReflectionUtil.isPrimitive(type)) {
					value = fromStoredString(stringValue, classType);
				} else if (field.getType().isEnum()) {
					// Assume value is a string
					value = ReflectionUtil.loadEnum(stringValue, classType);
				}
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

			} catch (ReflectiveOperationException e) {
				TownyMessaging.sendErrorMsg("flag 3");
				e.printStackTrace();
				return null;
			}
		}

		return obj;
	}
	
	private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
		ResultSetMetaData rsMD = rs.getMetaData();
		int columns = rsMD.getColumnCount();
		
		Map<String, Object> columnMap = new HashMap<>(columns);
		
		for (int i = 1; i <= columns; ++i) {
			columnMap.put(rsMD.getColumnName(i), rs.getObject(i));
		}
		return columnMap;
	}
	
	// None of the methods below call their respective load functions
	// because that would require an useless extra query
	
	private <T extends TownyObject> void loadNormalTownyObject(String tableName, final Class<T> objectClazz, final Consumer<T> consumer) {
		sqlHandler.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + tableName,
			"Error loading" + tableName + "from SQL",
			(rs) -> {
				while (rs.next()) {
					T townyObj = load(rs, objectClazz);

					if (townyObj == null) {
						String objName = rs.getString("name");
						TownyMessaging.sendErrorMsg("Could not load " + objectClazz.getName() + (objName != null ? objName : "null"));
						continue;
					}
					
					consumer.accept(townyObj);
				}
			});
	}
	
	@Override
	public void loadAllResidents() {
		loadNormalTownyObject("RESIDENTS", Resident.class, (res -> {
			try {
				TownyUniverse.getInstance().addResident(res);
			} catch (AlreadyRegisteredException ex) {
				TownyMessaging.sendErrorMsg("Resident " + res.getName() + " is already registered!");
			}
		}
		));
	}

	@Override
	public void loadAllWorlds() {
		loadNormalTownyObject("WORLDS", TownyWorld.class, (tw -> {
			try {
				TownyUniverse.getInstance().addWorld(tw);
			} catch (AlreadyRegisteredException ex) {
				TownyMessaging.sendErrorMsg("World " + tw.getName() + " is already registered!");
			}
		}));
	}

	@Override
	public void loadAllTowns() {
		loadNormalTownyObject("TOWNS", Town.class, (town -> {
			try {
				TownyUniverse.getInstance().addTown(town);
			} catch (AlreadyRegisteredException ex) {
				TownyMessaging.sendErrorMsg("Town " + town.getName() + " is already registered!");
			}
		}));
	}

	@Override
	public void loadAllTownBlocks() {
		sqlHandler.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + "TOWNBLOCKS",
			"Error loading townblocks from SQL",
			(rs) -> {
				while (rs.next()) {
					TownBlock townBlock = load(rs, TownBlock.class);

					if (townBlock == null) {
						String worldName = rs.getString("world");
						int x = rs.getInt("x");
						int z = rs.getInt("z");
						TownyMessaging.sendErrorMsg("Could not load townblock (" + worldName + ", " + x + ", " + z);
						continue;
					}

					try {
						TownyUniverse.getInstance().addTownBlock(townBlock);
					} catch (AlreadyRegisteredException ex) {
						TownyMessaging.sendErrorMsg("Townblock  " + townBlock.toString() + " is already registered!");
					}
				}
			});
	}
}
