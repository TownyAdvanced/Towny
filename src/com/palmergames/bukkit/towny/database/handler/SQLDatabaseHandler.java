package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.database.handler.annotations.ForeignKey;
import com.palmergames.bukkit.towny.database.handler.annotations.LoadSetter;
import com.palmergames.bukkit.towny.database.handler.annotations.OneToMany;
import com.palmergames.bukkit.towny.database.handler.annotations.PrimaryKey;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.database.handler.annotations.SavedEntity;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyRuntimeException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SQLDatabaseHandler extends DatabaseHandler {
	private final SQLHandler sqlHandler;
	private final SQLAdapter sqlAdapter;
	
	// This map allows us to cache a stub object for quicker loading.
	private final Map<Class<?>, String> tableNameCache = new HashMap<>();

	// Store the fields for OneToMany relationships
	private final ConcurrentHashMap<Type, List<Field>> fieldOneToManyCache = new ConcurrentHashMap<>();
	
	// TODO Queue creation/saves/deletes and process them async
	
	public SQLDatabaseHandler(String databaseType) {
		sqlHandler = new SQLHandler(databaseType);
		sqlAdapter = SQLAdapter.from(databaseType);
		
		if (!sqlHandler.testConnection()) {
			TownyMessaging.sendErrorMsg("Cannot establish connection for SQL db type " + databaseType + "!");
			return;
		}
		
		// Enable foreign keys explicitly if necessary
		if (sqlAdapter.explicitForeignKeyEnable()) {
			sqlHandler.executeUpdate(sqlAdapter.explicitForeignKeyStatement(),
				"Error enabling foreign keys for " + databaseType +  " DB!");
		}
		

		// Create tables
		createTownyObjectTable(Town.class);
		createTownyObjectTable(Nation.class);
		createTownyObjectTable(Resident.class);
		createTownyObjectTable(TownyWorld.class);
		createTownyObjectTable(TownBlock.class);
		
		// Update/alter tables. Order of this matters!
		alterTownyObjectTable(TownyWorld.class);
		alterTownyObjectTable(Nation.class);
		alterTownyObjectTable(Town.class);
		alterTownyObjectTable(Resident.class);
		alterTownyObjectTable(TownBlock.class);
	}

	private <T extends TownyObject> void createTownyObjectTable(Class<T> objectClazz) {
		final String tableName = getTableName(objectClazz);
		Validate.notNull(tableName);

		// Fetch primary field, and gather appropriate SQL.
		Field primaryField = fetchPrimaryKeyField(objectClazz);
		String pkStmt = "";
		if (primaryField != null) {
			pkStmt = ", PRIMARY KEY" + "(`" + primaryField.getName() + "`)";
		}

		String createTableStmt = "CREATE TABLE IF NOT EXISTS " + tableName +" ("
			+ "uniqueIdentifier VARCHAR(36) NOT NULL"
			+ pkStmt
			+ ");";

		sqlHandler.executeUpdate(createTableStmt, "Error creating table " + tableName + "!");
		createRelationship(objectClazz);
	}
	
	private <T extends TownyObject> void createRelationship(Class<T> objectClazz) {
		String objTableName = getTableName(objectClazz);
		
		List<String> updateStatements = new ArrayList<>();
		
		final String tableTemplate = "CREATE TABLE IF NOT EXISTS " + TownySettings.getSQLTablePrefix() + "%s" + 
			"(containerUUID VARCHAR(36), referenceValue %s," +
			"FOREIGN KEY (containerUUID) REFERENCES " + objTableName + "(uniqueIdentifier) ON DELETE CASCADE" +
			"%s);";
		
		List<Field> fields = ReflectionUtil.getNonTransientFields(objectClazz, f -> f.isAnnotationPresent(OneToMany.class));

		for (Field field : fields) {
			OneToMany annotation = field.getAnnotation(OneToMany.class);
			
			Type parameterizedType = ReflectionUtil.getTypeOfIterable(field);
			
			String columnDef = ObjectSerializer.getSQLColumnDefinition(parameterizedType);
			
			String foreignKey = "";
			
			// Instance of Savable
			if(Saveable.class.isAssignableFrom((Class<?>) parameterizedType)) {
				String typeTableName = tableNameCache.get(parameterizedType);

				if (typeTableName != null) {
					foreignKey = ", FOREIGN KEY (referenceValue) REFERENCES " +
						typeTableName + "(uniqueIdentifier) ON DELETE CASCADE";
				}
				else {
					TownyMessaging.sendErrorMsg("Cannot get type table name of class " + ((Class<?>) parameterizedType).getName());
				}
			}
			
			updateStatements.add(String.format(tableTemplate, annotation.tableName(), columnDef, foreignKey));
		}
		
		sqlHandler.executeUpdatesError("Cannot create relationships for " + objectClazz.getName(), updateStatements);
	}
	
	@Override
	public void save(@NotNull Saveable obj) {
		Map<String, String> insertionMap = generateInsertionMap(obj);
		
		String upsertStmt = sqlAdapter.upsertStatement(TownySettings.getSQLTablePrefix() + getTableName(obj.getClass()),
														insertionMap);
		
		sqlHandler.executeUpdate(upsertStmt, "Error updating object " + obj.getName());
		saveOneToManyRelationships(obj);
	}
	
	private Map<String, String> generateInsertionMap(@NotNull Saveable obj) {
		// Get map for fields not OneToMany
		Map<String, ObjectContext> contextMap = ReflectionUtil.getObjectMap(obj, field ->
												!field.isAnnotationPresent(OneToMany.class));

		// Invoke all the specified save methods and merge the results into the context map
		for (Map.Entry<String, ObjectContext> entry : getSaveGetterData(obj).entrySet()) {
			contextMap.put(entry.getKey(), entry.getValue());
		}

		return convertToInsertionMap(contextMap);
	}

	@Override
	public boolean delete(@NotNull Saveable obj) {
		return sqlHandler.executeUpdate("DELETE FROM " + getTableName(obj.getClass()) + " WHERE uniqueIdentifier = '" + obj.getUniqueIdentifier() + "'");
	}
	
	
	protected void saveOneToManyRelationships(final Saveable obj) {
		// For each relationship
		// We want to delete all rows in that relationship table with the parent id.
		// Then we want to insert the collection back into the table.
		
		for (Field field : getOneToManyFields(obj.getClass())) {
			if (field == null)
				continue;

			// Get the table name of the relationship
			String tableName = field.getAnnotation(OneToMany.class).tableName();
			tableName = TownySettings.getSQLTablePrefix() + tableName;

			List<String> updateStatements = new ArrayList<>();

			// Delete all rows that match with the parent object
			updateStatements.add("DELETE FROM " + tableName + " WHERE containerUUID = '" + obj.getUniqueIdentifier() + "'");

			// Perform re-insertions
			try {
				field.setAccessible(true);
				Iterator<?> itr = ReflectionUtil.resolveIterator(field.get(obj));

				itr.forEachRemaining(refVal ->
					updateStatements.add("INSERT INTO (containerUUID, referenceValue) VALUES (" + obj.getUniqueIdentifier() + ", "
						+ getConvertedValue(refVal.getClass(), refVal) + ");"));
				field.setAccessible(false);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			if (!updateStatements.isEmpty())
				// Execute the update
				sqlHandler.executeUpdatesError("Error storing OneToMany relationship for field " + field.getName(),
					updateStatements);
		}
	}

	@NotNull
	private final List<Field> getOneToManyFields(@NotNull Class<? extends Saveable> clazz) {
		Validate.notNull(clazz);

		// Check cache.
		List<Field> fields = fieldOneToManyCache.get(clazz);

		if (fields != null) {
			return fields;
		}

		fields = new ArrayList<>();
		for (Field field : ReflectionUtil.getNonTransientFields(clazz, f -> f.isAnnotationPresent(OneToMany.class))) {
			Validate.isTrue(ReflectionUtil.isIterableType(field.getClass()),
				"The OneToMany annotation for field " + field.getName() +
					" in " + clazz + " is not a List or primitive array type.");

			OneToMany rel = field.getAnnotation(OneToMany.class);

			if (rel != null) {
				fields.add(field);
			}
		}

		// Cache result.
		fieldOneToManyCache.putIfAbsent(clazz, fields);

		return fields;
	}

	private <T extends TownyObject> void alterTownyObjectTable(Class<T> objectClazz) {
		final String tableName = getTableName(objectClazz);
		Validate.notNull(tableName);

		Collection<String> existingColumns = getColumnNames(tableName);
		
		// Get fields that are not already in the DB
		Collection<Field> fields = ReflectionUtil.getNonTransientFields(objectClazz, 
			f -> !f.isAnnotationPresent(OneToMany.class) && !existingColumns.contains(f.getName()));
		
		// List of update statements
		Collection<String> updateStatements = new ArrayList<>(fields.size());
		
		// Get the alter table statement from the SQLAdapter
		for (Field field : fields) {
			String alterTableStmt = sqlAdapter.getAlterTableStatement(tableName, field.getName(),
												getSQLColumnDefinition(field), getForeignKeyDefinition(field));

			updateStatements.add(alterTableStmt);
		}

		sqlHandler.executeUpdatesError("Error altering table " + tableName, updateStatements);
	}
	
	private Collection<String> getColumnNames(String tableName) {
		final String[] columnStatement = sqlAdapter.getColumnNameStmt(tableName);
		
		String queryStatement = columnStatement[0];
		String columName = columnStatement[1];
		
		final Set<String> columnNames = new HashSet<>();
		sqlHandler.executeQuery(queryStatement, "Error fetching column names for " + tableName, rs -> {
			while (rs.next())
				columnNames.add(rs.getString(columName));
		});
		return columnNames;
	}
	
	private <T extends Saveable> T load(ResultSet rs, @NotNull Class<T> clazz) throws SQLException {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(UUID.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		T obj = null;
		try {
			Validate.isTrue(objConstructor != null);
			obj = objConstructor.newInstance((Object) null);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		Validate.isTrue(obj != null);
		List<Field> fields = ReflectionUtil.getNonTransientFields(obj, f -> !f.isAnnotationPresent(OneToMany.class));

		Map<String, Object> values = rowToMap(rs);
		for (Field field : fields) {
			field.setAccessible(true);

			String fieldName = field.getName();

			Object value = getAdaptedObject(values.get(fieldName), field);

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
				e.printStackTrace();
				return null;
			}
		}

		return obj;
	}
	
	private Object getAdaptedObject(Object value, Field field) {
		if (value instanceof String && field.getType() != String.class) {
			String stringValue = (String) value;
			return ObjectSerializer.deserializeField(field, stringValue);
		}
		return value;
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
	
	private <T extends TownyObject> void loadNormalTownyObject(final Class<T> objectClazz, final Consumer<T> consumer) {
		final String tableName = getTableName(objectClazz);
		Validate.notNull(tableName);
		
		Map<UUID, T> townyObjects = new HashMap<>();
		
		sqlHandler.executeQuery("SELECT * from " + tableName,
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
					
					// For loading the OneToMany relations after it has been added to the universe map
					townyObjects.put(townyObj.getUniqueIdentifier(), townyObj);
				}
			});
		
		loadRelationships(objectClazz, townyObjects);
	}
	
	private <T extends TownyObject> void loadRelationships(final Class<T> objectClass, Map<UUID, T> uuidObjMap) {
		for (Field oneToManyField : getOneToManyFields(objectClass)) {

			OneToMany annotation = oneToManyField.getAnnotation(OneToMany.class);

			String tableName = TownySettings.getSQLTablePrefix() + annotation.tableName();

			Type parameterizedType = ReflectionUtil.getTypeOfIterable(oneToManyField);

			final Map<UUID, Collection<Object>> iterableMap = new HashMap<>();
			
			// Perform query
			sqlHandler.executeQuery("SELECT * FROM " + tableName + " GROUP BY containerUUID",
				"Error loading " + oneToManyField.getName() + " for " + objectClass.getName(),
				rs -> {
					UUID lastUUID = null;
					Collection<Object> lastCollection = null;
					
					while (rs.next()) {
						UUID containerUUID = UUID.fromString(rs.getString("containerUUID"));
						
						if (!containerUUID.equals(lastUUID)) {
							if (!uuidObjMap.containsKey(containerUUID))
								continue;
							
							lastUUID = containerUUID;
							lastCollection = iterableMap.computeIfAbsent(lastUUID, k -> new ArrayList<>());
						}
						
						Object rsObj = rs.getObject("referenceValue");
						
						if (rsObj instanceof String && parameterizedType != String.class) {
							rsObj = ObjectSerializer.deserialize((String) rsObj, parameterizedType);
						}
						
						if (rsObj != null)
							lastCollection.add(rsObj);
					}
				});
			
			boolean isArray = oneToManyField.getType().isArray();
			boolean isCollection = !isArray && Collection.class.isAssignableFrom(oneToManyField.getType());
			oneToManyField.setAccessible(true); // Allow field to be accessed if it is private
			for (Map.Entry<UUID, Collection<Object>> entry : iterableMap.entrySet()) {
				T loadedObj = uuidObjMap.get(entry.getKey());
				
				if (loadedObj == null)
					continue;
				
				try {
					// Check if field is an array. if so just set the field to collection.toArray
					if (isArray) {
						oneToManyField.set(loadedObj, entry.getValue().toArray());
					}
					else if (isCollection && !entry.getValue().isEmpty()) {
						Collection<Object> collection = (Collection<Object>) oneToManyField.get(loadedObj);
						if (collection != null)
							collection.addAll(entry.getValue());
					}
				} catch (IllegalAccessException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void loadAllResidents() {
		loadNormalTownyObject(Resident.class, (res -> {
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
		loadNormalTownyObject(TownyWorld.class, (tw -> {
			try {
				TownyUniverse.getInstance().addWorld(tw);
			} catch (AlreadyRegisteredException ex) {
				TownyMessaging.sendErrorMsg("World " + tw.getName() + " is already registered!");
			}
		}));
	}

	@Override
	public void loadAllNations() {
		loadNormalTownyObject(Nation.class, nation -> {
			try {
				TownyUniverse.getInstance().addNation(nation);	
			} catch (AlreadyRegisteredException e) {
				TownyMessaging.sendErrorMsg("Nation " + nation.getName() + " is already registered!");
			}
		});
	}

	@Override
	public void loadAllTowns() {
		loadNormalTownyObject(Town.class, (town -> {
			try {
				TownyUniverse.getInstance().addTown(town);
			} catch (AlreadyRegisteredException ex) {
				TownyMessaging.sendErrorMsg("Town " + town.getName() + " is already registered!");
			}
		}));
	}

	@Override
	public void loadAllTownBlocks() {
		final String tableName = getTableName(TownBlock.class);
		sqlHandler.executeQuery("SELECT * from " + tableName,
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

	@Override
	protected void completeLoad() {
		throw new UnsupportedOperationException("Not implemented yet!");
	}

	// Returns the SQL table name from a savable object.
	@Nullable
	private <T extends Saveable> String getTableName(@NotNull Class<T> type) {
		// Check the cache
		String cached = tableNameCache.get(type);
		if (tableNameCache.get(type) != null) {
			return cached;
		}

		SavedEntity annotation = type.getAnnotation(SavedEntity.class);
		if (annotation == null) {
			throw new TownyRuntimeException("Saveable class is not annotated with @SavedEntity.");
		}

		// Cache result.
		tableNameCache.putIfAbsent(type, annotation.tableName());
		
		return annotation.tableName();
	}
	
	@Nullable
	private Field fetchPrimaryKeyField(@NotNull Object obj) {
		Validate.notNull(obj);
		
		List<Field> fields = ReflectionUtil.getNonTransientFields(obj, f -> f.isAnnotationPresent(PrimaryKey.class));
		
		if (!fields.isEmpty())
			return fields.get(0);
		
		return null;
	}
	
	private String getForeignKeyDefinition(Field field) {
		ForeignKey fkAnnotation = field.getAnnotation(ForeignKey.class);
		
		if (fkAnnotation != null) {
			final String tableName = getTableName(fkAnnotation.reference());
			Validate.notNull(tableName);
			
			String keyConstraint = tableName + "(uniqueIdentifier)";
			
			if (fkAnnotation.cascadeOnDelete())
				keyConstraint += " ON DELETE CASCADE";
			
			return keyConstraint;
		}
		
		return "";
	}
	
	private Map<String, String> convertToInsertionMap(Map<String, ObjectContext> contextMap) {
		Map<String, String> insertionMap = new HashMap<>((contextMap.size() * 4) / 3);
		for (Map.Entry<String, ObjectContext> entry : contextMap.entrySet()) {
			String field = entry.getKey();
			String insertionValue = getConvertedValue(entry.getValue().getType(),
													entry.getValue().getValue());
			
			insertionMap.put(field, insertionValue);
		}
		return insertionMap;
	}
	
	private String getConvertedValue(Type type, Object value) {
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
			insertionValue = ObjectSerializer.serialize(value, type);
			// Sanitize the input.
			// Replace " with \"
			insertionValue = insertionValue.replace("\"", "\\\"");
			// Wrap with double quotes
			insertionValue = "\"" + insertionValue + "\"";
		}
		
		return insertionValue;
	}

	@Override
	public void upgrade() {
		throw new UnsupportedOperationException("Not implemented yet");
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

		return ObjectSerializer.getSQLColumnDefinition(type);
	}
}
