package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.annotations.ForeignKey;
import com.palmergames.bukkit.towny.database.handler.annotations.LoadSetter;
import com.palmergames.bukkit.towny.database.handler.annotations.PrimaryKey;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SQLDatabaseHandler extends DatabaseHandler {
	SQLHandler sqlHandler;
	
	// TODO Queue creation/saves/deletes and process them async
	
	public SQLDatabaseHandler(String databaseType) {
		sqlHandler = new SQLHandler(databaseType);
		sqlHandler.testConnection();

		// Create tables
		createTownyObjectTable("TOWNS", Town.class);
		createTownyObjectTable("NATIONS", Nation.class);
		createTownyObjectTable("RESIDENTS", Resident.class);
		
		// Update/alter tables. Order of this matters!
		alterTownyObjectTable("NATIONS", Nation.class);
		alterTownyObjectTable("TOWNS", Town.class);
		alterTownyObjectTable("RESIDENTS", Resident.class);
	}
	
	// TODO Figure out how to handle insertions vs updates
	@Override
	public void save(@NotNull Saveable obj) {
		Map<String, ObjectContext> contextMap = ReflectionUtil.getObjectMap(obj);

		// Invoke all the specified save methods and merge the results into the context map
		for (Map.Entry<String, ObjectContext> entry : getSaveGetterData(obj).entrySet()) {
			contextMap.put(entry.getKey(), entry.getValue());
		}
		
		Map<String, String> insertionMap = convertToInsertionMap(contextMap);
		
		StringBuilder stmtBuilder = new StringBuilder("UPDATE ").append(obj.getSQLTable().toUpperCase()).append(" SET ");
		
		// Convert the map into the statement
		// Depending on performance, we may want to remove the stream
		String valueBuilder = insertionMap.entrySet().stream()
							.map(e -> e.getKey() + " = " + e.getValue())
							.collect(Collectors.joining(", "));
		
		stmtBuilder.append(valueBuilder);
		stmtBuilder.append(" WHERE uniqueIdentifier = '")
					.append(obj.getUniqueIdentifier().toString())
					.append("'");
		
		sqlHandler.executeUpdate(stmtBuilder.toString(), "Error updating object " + obj.getName());
	}
	
	private String tblPrefix() {
		return TownySettings.getSQLTablePrefix();
	}
	
	private <T> String[] alterColumnStatements(String tableName, Class<T> clazz, Collection<String> filter) {
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
				.filter(f -> !filter.contains(f.getName()))
				.map(f -> "ALTER TABLE " + tableName + " ADD  (" +
					f.getName() + " " + getSQLColumnDefinition(f) + getForeignKeyDefinition(f) + ")")
				.toArray(String[]::new);
	}

	private <T extends TownyObject> void createTownyObjectTable(String tableName, Class<T> objectClazz) {
		tableName = tblPrefix() + tableName;

		// Fetch primary field, and gather appropriate SQL.
		Field primaryField = fetchPrimaryKeyField(objectClazz);
		String pkStmt = "";
		if (primaryField != null) {
			pkStmt = ", PRIMARY KEY" + "(`" + primaryField.getName() + "`)";
		}

		String createTableStmt = "CREATE TABLE IF NOT EXISTS " + tableName +" ("
			+ "`uniqueIdentifier` VARCHAR(32) NOT NULL"
			+ pkStmt
			+ ")";

		sqlHandler.executeUpdate(createTableStmt, "Error creating table " + tableName + "!");
	}
	
	private <T extends TownyObject> void alterTownyObjectTable(String tableName, Class<T> objectClazz) {
		// Set of column names that already exist in the table
		Set<String> columnNames = new HashSet<>();
		
		// Fetch all the column names
		sqlHandler.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'" + tableName + "'", 
			"Could not get column names from " + tableName, 
			rs -> {
				while (rs.next()) {
					columnNames.add(rs.getString("COLUMN_NAME"));
				}
			});
		
		// Returns a list of statements to create columns which are not already in the table.
		// This should prevent an SQL error from being thrown
		String[] columnStatements = alterColumnStatements(tableName, objectClazz, columnNames);

		sqlHandler.executeUpdatesError("Error creating table " + tableName + "!" , columnStatements);
	}

	@Override
	public boolean delete(@NotNull Saveable obj) {
		return sqlHandler.executeUpdate("DELETE FROM " + obj.getSQLTable() + " WHERE uniqueIdentifier = '" + obj.getUniqueIdentifier() + "'");
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
	
	@Nullable
	private Field fetchPrimaryKeyField(@NotNull Object obj) {
		Validate.notNull(obj);
		
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);
		
		for (Field field : fields) {
			if (field.getAnnotation(PrimaryKey.class) != null) {
				return field;
			}
		}
		
		return null;
	}
	
	private String getForeignKeyDefinition(Field field) {
		ForeignKey fkAnnotation = field.getAnnotation(ForeignKey.class);
		
		if (fkAnnotation != null) {
			// We have to create an instance of the reference class
			// in order to get the sql table from the class.
			Constructor<? extends Saveable> objConstructor = null;
			try {
				objConstructor = fkAnnotation.reference().getConstructor(UUID.class);
			} catch (NoSuchMethodException e) {
				TownyMessaging.sendErrorMsg("Unable to get constructor of " + fkAnnotation.reference().getName() + " for ForeignKey constraint!");
				e.printStackTrace();
				return "";
			}

			Saveable obj = null;
			try {
				Validate.isTrue(objConstructor != null);
				obj = objConstructor.newInstance(null);
			} catch (ReflectiveOperationException e) {
				TownyMessaging.sendErrorMsg("Unable to construct instance of " + fkAnnotation.reference().getName() + " for ForeignKey constraint!");
				e.printStackTrace();;
				return "";
			}

			String keyConstraint = ", FOREIGN KEY (%s) REFERENCES %s(uniqueIdentifier)";
			
			if (fkAnnotation.cascadeOnDelete())
				keyConstraint += " ON DELETE CASCADE";
			
			return String.format(keyConstraint, field.getName(), tblPrefix() + obj.getSQLTable());
		}
		
		return "";
	}
	
	private Map<String, String> convertToInsertionMap(Map<String, ObjectContext> contextMap) {
		Map<String, String> insertionMap = new HashMap<>((contextMap.size() * 4) / 3);
		for (Map.Entry<String, ObjectContext> entry : contextMap.entrySet()) {
			String field = entry.getKey();
			Type type = entry.getValue().getType();
			Object value = entry.getValue().getValue();
			String insertionValue;

			if (ReflectionUtil.isPrimitive(type)) {
				if (type == boolean.class || type == Boolean.class) { // Is this the right comparison??
					// Booleans are 1 and 0 in SQL not true or false
					insertionValue = ((boolean) value) ? "1" : "0";
				}
				// It's a primitive so to string should(tm) be fine.
				else {
					insertionValue = value.toString();
				}
			} else {
				// toStoredString will call toString() on the object which on a String obj will wrap the "" inside the string which we don't want
				if (value instanceof String) {
					insertionValue = (String) value;
				}
				else {
					insertionValue = toStoredString(value, type);
				}
				// Replace " with \"
				insertionValue = insertionValue.replace("\"", "\\\"");
				// Wrap with double quotes
				insertionValue = "\"" + insertionValue + "\"";
			}
			
			insertionMap.put(field, insertionValue);
		}
		return insertionMap;
	}
}
