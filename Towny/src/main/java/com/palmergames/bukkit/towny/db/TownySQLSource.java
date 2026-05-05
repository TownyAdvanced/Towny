/*
 * Towny MYSQL Source by StPinker
 *
 * Released under LGPL
 */
package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.ObjectCouldNotBeLoadedException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.NameAndId;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.Pair;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.zaxxer.hikari.pool.HikariPool;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class TownySQLSource extends TownyDatabaseHandler {
	private final String tb_prefix;

	private final HikariDataSource hikariDataSource;

	public TownySQLSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		if (!FileMgmt.checkOrCreateFolders(rootFolderPath, dataFolderPath,
				dataFolderPath + File.separator + "plot-block-data")
				|| !FileMgmt.checkOrCreateFiles(dataFolderPath + File.separator + "regen.txt",
						dataFolderPath + File.separator + "snapshot_queue.txt")) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");

		}
		/*
		 * Setup SQL connection
		 */
		tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();
		
		String dsn = "jdbc:mysql://" + TownySettings.getSQLHostName() + ":" + TownySettings.getSQLPort() + "/" + TownySettings.getSQLDBName() + TownySettings.getSQLFlags();
		HikariConfig config = new HikariConfig();
		
		config.setPoolName("Towny MySQL");
		config.setJdbcUrl(dsn);

		config.setUsername(TownySettings.getSQLUsername());
		config.setPassword(TownySettings.getSQLPassword());

		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("elideSetAutoCommits", "true");
		config.addDataSourceProperty("maintainTimeStats", "false");
		config.addDataSourceProperty("cacheCallableStmts", "true");

		config.setMaximumPoolSize(TownySettings.getMaxPoolSize());
		config.setMaxLifetime(TownySettings.getMaxLifetime());
		config.setConnectionTimeout(TownySettings.getConnectionTimeout());

		/*
		 * Register the driver (if possible)
		 */
		try {
			Driver driver;
			try {
				driver = (Driver) Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
			} catch (ClassNotFoundException e) {
				// The non deprecated driver was not found, fall back to the deprecated one.
				driver = (Driver) Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
			}
			
			DriverManager.registerDriver(driver);
		} catch (Exception e) {
			plugin.getLogger().severe("Driver error: " + e);
		}

		try {
			this.hikariDataSource = new HikariDataSource(config);
			
			try (Connection connection = getConnection()) {
				TownyMessaging.sendDebugMsg("Connected to the database");

				// Initialize database schema.
				SQLSchema.initTables(connection);
			}
		} catch (SQLException | HikariPool.PoolInitializationException e) {
			throw new TownyInitException("Failed to connect to the database", TownyInitException.TownyError.DATABASE, e);
		}
	}

	@Override
	public void finishTasks() {
		super.finishTasks();

		// Close the database sources on shutdown to get GC
		if (hikariDataSource != null)
			hikariDataSource.close();
	}

	/**
	 * @return Whether the datasource is initialized and running.
	 */
	public boolean isReady() {
		return hikariDataSource != null && hikariDataSource.isRunning();
	}

	/**
	 * @return A connection from the pool
	 */
	public Connection getConnection() throws SQLException {
		return this.hikariDataSource.getConnection();
	}
	
	@SuppressWarnings("unused")
	private boolean UpdateDB$$bridge$$public(String tb_name, HashMap<String, Object> args, List<String> keys) {
		return updateDB(tb_name, args, keys);
	}

	/**
	 * Build the SQL string and execute to INSERT/UPDATE
	 *
	 * @param tb_name - Database Table name.
	 * @param args    - Arguments.
	 * @param keys    - Table keys.
	 * @return true if the update was successful.
	 */
	public boolean updateDB(String tb_name, Map<String, ?> args, List<String> keys) {

		/*
		 * Make sure we only execute queries in async
		 */

		this.queryQueue.add(new SQLTask(this, tb_name, args, keys));

		return true;

	}

	@SuppressWarnings("unused")
	private boolean QueueUpdateDB$$bridge$$public(String tb_name, HashMap<String, Object> args, List<String> keys) {
		return queueUpdateDB(tb_name, args, keys);
	}

	@ApiStatus.Internal
	public boolean queueUpdateDB(String tb_name, Map<String, ?> args, List<String> keys) {

		StringBuilder code;
		PreparedStatement stmt = null;
		List<Object> parameters = new ArrayList<>();
		int rs = 0;

		try (Connection connection = getConnection()) {

			if (keys == null) {

				/*
				 * No keys so this is an INSERT not an UPDATE.
				 */

				// Push all values to a parameter list.

				parameters.addAll(args.values());

				String[] aKeys = args.keySet().toArray(new String[0]);

				// Build the prepared statement string appropriate for
				// the number of keys/values we are inserting.

				code = new StringBuilder("REPLACE INTO " + tb_prefix + (tb_name.toUpperCase()) + " ");
				StringBuilder keycode = new StringBuilder("(");
				StringBuilder valuecode = new StringBuilder(" VALUES (");

				for (int count = 0; count < args.size(); count++) {

					keycode.append("`").append(aKeys[count]).append("`");
					valuecode.append("?");

					if ((count < (args.size() - 1))) {
						keycode.append(", ");
						valuecode.append(",");
					} else {
						keycode.append(")");
						valuecode.append(")");
					}
				}

				code.append(keycode);
				code.append(valuecode);

			} else {

				/*
				 * We have keys so this is a conditional UPDATE.
				 */

				String[] aKeys = args.keySet().toArray(new String[0]);

				// Build the prepared statement string appropriate for
				// the number of keys/values we are inserting.

				code = new StringBuilder("UPDATE " + tb_prefix + (tb_name.toUpperCase()) + " SET ");

				for (int count = 0; count < args.size(); count++) {

					code.append("`").append(aKeys[count]).append("` = ?");

					// Push value for each entry.

					parameters.add(args.get(aKeys[count]));

					if ((count < (args.size() - 1))) {
						code.append(",");
					}
				}

				code.append(" WHERE ");

				for (int count = 0; count < keys.size(); count++) {

					code.append("`").append(keys.get(count)).append("` = ?");

					// Add extra values for the WHERE conditionals.

					parameters.add(args.get(keys.get(count)));

					if ((count < (keys.size() - 1))) {
						code.append(" AND ");
					}
				}

			}

			// Populate the prepared statement parameters.

			stmt = connection.prepareStatement(code.toString());

			for (int count = 0; count < parameters.size(); count++) {

				Object element = parameters.get(count);

				if (element instanceof String) {

					stmt.setString(count + 1, (String) element);

				} else if (element instanceof Boolean) {

					stmt.setString(count + 1, ((Boolean) element) ? "1" : "0");

				} else {

					stmt.setObject(count + 1, element.toString());

				}

			}

			rs = stmt.executeUpdate();

		} catch (SQLException e) {

			Towny.getPlugin().getLogger().warning("SQL: " + e.getMessage() + " --> " + stmt.toString());

		} finally {

			try {

				if (stmt != null) {
					stmt.close();
				}

				if (rs == 0) // if entry doesn't exist then try to insert
					return updateDB(tb_name, args, null);

			} catch (SQLException e) {
				Towny.getPlugin().getLogger().warning("SQL closing: " + e.getMessage() + " --> " + stmt.toString());
			}

		}

		// Failed?
		return rs != 0;

		// Success!

	}

	/**
	 * Build the SQL string and execute to DELETE
	 *
	 * @param tb_name - Database Table name
	 * @param args    - Arguments
	 * @return true if the delete was a success.
	 */
	@ApiStatus.Internal
	public boolean DeleteDB(String tb_name, HashMap<String, Object> args) {

		// Make sure we only execute queries in async

		this.queryQueue.add(new SQLTask(this, tb_name, args));

		return true;

	}

	@SuppressWarnings("unused")
	private boolean queueDeleteDB$$bridge$$public(String tb_name, HashMap<String, Object> args) {
		return queueDeleteDB(tb_name, args);
	}

	@ApiStatus.Internal
	public boolean queueDeleteDB(String tb_name, Map<String, ?> args) {

		try {
			StringBuilder wherecode = new StringBuilder(
					"DELETE FROM " + tb_prefix + (tb_name.toUpperCase()) + " WHERE ");

			Iterator<? extends Map.Entry<String, ?>> i = args.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, ?> me = i.next();
				wherecode.append("`").append(me.getKey()).append("` = ?");
				wherecode.append(i.hasNext() ? " AND " : "");
			}
			int rs;
			try (Connection connection = getConnection();
				 PreparedStatement statement = connection.prepareStatement(wherecode.toString())) {
				Object[] values = args.values().stream().toArray();
				for (int count = 0; count < values.length; count++) {
					Object object = values[count];
					if (object instanceof String str)
						statement.setString(count + 1, str);
					else if (object instanceof Boolean b)
						statement.setBoolean(count + 1, b);
					else if (object instanceof UUID uuid)
						statement.setObject(count + 1, uuid.toString());
					else
						statement.setObject(count + 1, object);
				}
				rs = statement.executeUpdate();
			}
			if (rs == 0) {
				TownyMessaging.sendDebugMsg("SQL: delete returned 0: " + wherecode);
			}
		} catch (SQLException e) {
			Towny.getPlugin().getLogger().warning("SQL: Error delete : " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean cleanup() {
		// This was previously used to clean up columns that were no longer used, but that is handled by a one-time migration instead.
		return true;
	}
	
	public enum TownyDBTableType {
		JAIL("JAILS", "SELECT uuid FROM ", "uuid"),
		PLOTGROUP("PLOTGROUPS", "SELECT groupID FROM ", "groupID"),
		DISTRICT("DISTRICTS", "SELECT uuid FROM ", "uuid"),
		RESIDENT("RESIDENTS", "SELECT uuid, name FROM ", "uuid"),
		HIBERNATED_RESIDENT("HIBERNATEDRESIDENTS", "", "uuid"),
		TOWN("TOWNS", "SELECT uuid, name FROM ", "uuid"),
		NATION("NATIONS", "SELECT uuid, name FROM ", "uuid"),
		WORLD("WORLDS", "SELECT uuid, name FROM ", "uuid"),
		TOWNBLOCK("TOWNBLOCKS", "SELECT world,x,z FROM ", "name"),
		COOLDOWN("COOLDOWNS", "SELECT * FROM ", "key");
		
		private final String tableName;
		@SuppressWarnings("unused")
		private String queryString;
		@SuppressWarnings("unused")
		private String primaryKey;

		TownyDBTableType(String tableName, String queryString, String primaryKey) {
			this.tableName = tableName;
			this.queryString = queryString;
			this.primaryKey = primaryKey;
		}
		
		public String tableName() {
			return tableName;
		}
		
		public String primaryKey() {
			return primaryKey;
		}
		
		private String getSingular() {
			// Hibernated Residents are never loaded so this method is never called on them.
			return tableName.substring(0, tableName.length()-1).toLowerCase(Locale.ROOT);
		}
		
		public String getSaveLocation(String rowKeyName) {
			return TownySettings.getSQLTablePrefix() + tableName + File.separator + rowKeyName;
		}
		
		public String getLoadErrorMsg(UUID uuid) {
			return "Loading Error: Could not read the " + getSingular() + " with UUID '" + uuid + "' from the " + tableName + " table.";
		}
	}

	private boolean loadResultSetListOfType(TownyDBTableType type, Consumer<NameAndId> consumer) {
		TownyMessaging.sendDebugMsg("Searching for " + type.tableName.toLowerCase(Locale.ROOT) + "...");
		if (!isReady())
			return false;
		
		try {
			try (Statement s = getConnection().createStatement()) {
				ResultSet rs = s.executeQuery(type.queryString + tb_prefix + type.tableName);
				if (rs.getFetchSize() != 0)
					TownyMessaging.sendDebugMsg("Loading " + rs.getFetchSize() + " entries from the " + type.tableName + " table...");

				while (rs.next()) {
					UUID uuid = UUID.fromString(rs.getString(type.primaryKey));
					String name = null;
					try {
						name = rs.getString("name");
					} catch (SQLException ignored) {} // Some data types do not store a name.

					// Residents that are NPCs can have special UUID versions applied to them.
					if (type.equals(TownyDBTableType.RESIDENT) && name != null)
						uuid = super.parsePlayerUUID(uuid.toString(), name);

					consumer.accept(new NameAndId(name, uuid));
				}
			}
			
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		return false;
	}

	private boolean loadResultSetOfType(TownyDBTableType type, Set<UUID> UUIDs) throws ObjectCouldNotBeLoadedException {
		for (UUID uuid : UUIDs)
			if (!loadResultSet(type, uuid))
				throw new ObjectCouldNotBeLoadedException(type.getLoadErrorMsg(uuid));
		return true;
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadJailList() {
		return loadResultSetListOfType(TownyDBTableType.JAIL, nameAndId -> universe.newJailInternal(nameAndId.uuid()));
	}

	@Override
	public boolean loadPlotGroupList() {
		return loadResultSetListOfType(TownyDBTableType.PLOTGROUP, nameAndId -> universe.newPlotGroupInternal(nameAndId.uuid()));
	}
	
	@Override
	public boolean loadDistrictList() {
		return loadResultSetListOfType(TownyDBTableType.DISTRICT, nameAndId -> universe.newDistrictInternal(nameAndId.uuid()));
	}
	
	@Override
	public boolean loadResidentList() {
		return loadResultSetListOfType(TownyDBTableType.RESIDENT, nameAndId -> {
			if (!universe.hasResident(nameAndId.uuid()))
				universe.newResidentInternal(nameAndId.name(), nameAndId.uuid());
			else {
				final Resident otherResident = universe.getResident(nameAndId.uuid());
				if (otherResident != null && !otherResident.getName().equals(nameAndId.name())) {
					// UUID is already registered
					super.pendingDuplicateResidents.add(Pair.pair(nameAndId.name(), otherResident.getName()));
				}
			}
		});
	}

	@Override
	public boolean loadTownList() {
		return loadResultSetListOfType(TownyDBTableType.TOWN, nameAndId -> universe.newTownInternal(nameAndId.name(), nameAndId.uuid()));
	}

	@Override
	public boolean loadNationList() {
		return loadResultSetListOfType(TownyDBTableType.NATION, nameAndId -> universe.newNationInternal(nameAndId.name(), nameAndId.uuid()));
	}
	
	@Override
	public boolean loadWorldList() {
		loadResultSetListOfType(TownyDBTableType.WORLD, nameAndId -> universe.newWorldInternal(nameAndId.name(), nameAndId.uuid()));
		for (World world : plugin.getServer().getWorlds()) {
			if (universe.getWorldIDMap().containsKey(world.getUID()))
				continue;

			// Register and create rows for any worlds which did not have files yet.
			TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
			universe.registerTownyWorld(townyWorld);
			try {
				queueUpdateDB("WORLDS", townyWorld.getObjectDataMap(), null);
			} catch (Exception e) {
				logger.warn("Could not save new world row for TownyWorld: " + townyWorld.getUUID());
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public boolean loadTownBlockList() {
		TownyMessaging.sendDebugMsg("Loading TownBlock List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT world,x,z FROM " + tb_prefix + "TOWNBLOCKS")) {
				
			int total = 0;
			while (rs.next()) {
				String worldName = rs.getString("world");
				TownyWorld world = universe.getWorld(worldName);
				if (world == null)
					throw new Exception("World " + worldName + " not registered!");

				int x = Integer.parseInt(rs.getString("x"));
				int z = Integer.parseInt(rs.getString("z"));

				TownBlock townBlock = new TownBlock(x, z, world);
				universe.addTownBlock(townBlock);
				total++;
			}

			TownyMessaging.sendDebugMsg("Loaded " + total + " townblocks.");

			return true;
		} catch (SQLException s) {
			plugin.getLogger().warning("SQL: town block list error: " + s.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: townblock list unknown error", e);
		}
		return false;

	}

	private boolean loadResultSet(TownyDBTableType type, UUID uuid) {
		return switch (type) {
		case JAIL -> loadJailData(uuid);
		case NATION -> loadNationData(uuid);
		case PLOTGROUP -> loadPlotGroupData(uuid);
		case DISTRICT -> loadDistrictData(uuid);
		case RESIDENT -> loadResidentData(uuid);
		case TOWN -> loadTownData(uuid);
		case TOWNBLOCK -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		case WORLD -> loadWorldData(uuid);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	private Map<String, String> loadResultSetIntoMap(ResultSet rs) throws SQLException {
		Map<String, String> keys = new HashMap<>();
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		for (int i = 1; i <= columns; ++i)
			keys.put(md.getColumnName(i), rs.getString(i));

		return keys;
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadJailUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.JAIL, uuids);
	}
	
	@Override
	public boolean loadPlotGroupUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.PLOTGROUP, uuids);
	}

	@Override
	public boolean loadDistrictUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.DISTRICT, uuids);
	}
	
	@Override
	public boolean loadResidentUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.RESIDENT, uuids);
	}

	@Override
	public boolean loadTownUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.TOWN, uuids);
	}
	
	@Override
	public boolean loadNationUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.NATION, uuids);
	}
	
	@Override
	public boolean loadWorldUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadResultSetOfType(TownyDBTableType.WORLD, uuids);
	}

	public boolean loadTownBlocks(Collection<TownBlock> townBlocks) throws ObjectCouldNotBeLoadedException {
		for (TownBlock townblock : townBlocks)
			if (!loadTownBlock(townblock))
				throw new ObjectCouldNotBeLoadedException("Loading Error: Could not read the townblock with details: '" + townblock.toString() + "' from the TOWNBLOCKS table.");
		return true;
	}

	/*
	 * Methods that return objects as Maps for loading.
	 */

	@Override
	public Map<String, String> getJailMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "JAILS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find jail with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getPlotGroupMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT groupID FROM " + tb_prefix + "PLOTGROUPS WHERE groupID='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find plotgroup with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getDistrictMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "DISTRICTS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find district with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getResidentMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "RESIDENTS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find resident with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getTownMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "TOWNS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find town with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getNationMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "NATIONS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find nation with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getWorldMap(UUID uuid) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "WORLDS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find world with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getTownBlockMap(TownBlock townBlock) {
		if (!isReady())
			return null;
		try (Statement s = getConnection().createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNBLOCKS")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock: "
					+ (townBlock != null ? townBlock : "NULL") + " in the sql database");
			return null;
		}
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public synchronized boolean saveJail(Jail jail, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving jail " + jail.getUUID());
		try {
			updateDB("JAILS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save jail unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean savePlotGroup(PlotGroup group, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving group " + group.getName());
		try {
			updateDB("PLOTGROUPS", data, Collections.singletonList("groupID"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Plot groups unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveDistrict(District district, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving district " + district.getName());
		try {
			updateDB("DISTRICTS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save District unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveResident(Resident resident, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving Resident " + resident.getName());
		try {
			updateDB("RESIDENTS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}
	
	@Override
	public synchronized boolean saveHibernatedResident(UUID uuid, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving Hibernated Resident " + uuid);
		try {
			updateDB("HIBERNATEDRESIDENTS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Hibernated Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public synchronized boolean saveTown(Town town, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving town " + town.getName());
		try {
			updateDB("TOWNS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Town unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			updateDB("NATIONS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Nation unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveWorld(TownyWorld world, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving world " + world.getName());
		try {
			updateDB("WORLDS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save World unknown error (" + world.getName() + ")");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock, Map<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x"
				+ townBlock.getZ());
		try {
			updateDB("TOWNBLOCKS", data, Arrays.asList("world", "x", "z"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save TownBlock unknown error");
			e.printStackTrace();
		}
		return false;
	}

	
	@Override
	public boolean loadCooldowns() {
		try (Connection connection = getConnection();
		     PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tb_prefix + TownyDBTableType.COOLDOWN.tableName()); 
		     ResultSet resultSet = statement.executeQuery()) {
			
			while (resultSet.next())
				CooldownTimerTask.getCooldowns().put(resultSet.getString("key"), resultSet.getLong("expiry"));
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred when loading cooldowns", e);
			return false;
		}
		
		return true;
	}

	@Override
	public boolean saveCooldowns() {
		for (Map.Entry<String, Long> entry : CooldownTimerTask.getCooldowns().entrySet()) {
			final Map<String, Object> data = new HashMap<>();
			data.put("key", entry.getKey());
			data.put("expiry", entry.getValue());

			queueUpdateDB(TownyDBTableType.COOLDOWN.tableName(), data, Collections.singletonList("key"));
		}
		
		return true;
	}

	/*
	 * Delete objects
	 */
	
	@Override
	public void deleteResident(Resident resident) {

		HashMap<String, Object> res_hm = new HashMap<>();
		res_hm.put("name", resident.getName());
		DeleteDB("RESIDENTS", res_hm);
	}

	@Override 
	public void deleteHibernatedResident(UUID uuid) {
		HashMap<String, Object> res_hm = new HashMap<>();
		res_hm.put("uuid", uuid);
		DeleteDB("HIBERNATEDRESIDENTS", res_hm);
	}
	
	@Override
	public void deleteTown(Town town) {

		HashMap<String, Object> twn_hm = new HashMap<>();
		twn_hm.put("name", town.getName());
		DeleteDB("TOWNS", twn_hm);
	}

	@Override
	public void deleteNation(Nation nation) {

		HashMap<String, Object> nat_hm = new HashMap<>();
		nat_hm.put("name", nation.getName());
		DeleteDB("NATIONS", nat_hm);
	}

	@Override
	public void deleteWorld(TownyWorld world) {

	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {
		HashMap<String, Object> twn_hm = new HashMap<>();
		twn_hm.put("world", townBlock.getWorld().getName());
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("TOWNBLOCKS", twn_hm);
	}

	@Override
	public void deletePlotGroup(PlotGroup group) {

		HashMap<String, Object> pltgrp_hm = new HashMap<>();
		pltgrp_hm.put("groupID", group.getUUID());
		DeleteDB("PLOTGROUPS", pltgrp_hm);
	}
	
	@Override
	public void deleteDistrict(District district) {
		HashMap<String, Object> district_hm = new HashMap<>();
		district_hm.put("uuid", district.getUUID());
		DeleteDB("DISTRICTS", district_hm);
	}

	@Override
	public void deleteJail(Jail jail) {
		
		HashMap<String, Object> jail_hm = new HashMap<>();
		jail_hm.put("uuid", jail.getUUID());
		DeleteDB("JAILS", jail_hm);
	}

	@Override
	public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection connection = getConnection(); 
				 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + tb_prefix + "HIBERNATEDRESIDENTS WHERE uuid = ? LIMIT 1")) {
				preparedStatement.setString(1, uuid.toString());
				ResultSet resultSet = preparedStatement.executeQuery();
				final String registered;
				if (resultSet.next() && (registered = resultSet.getString("registered")) != null && !registered.isEmpty()) {
					return Optional.of(Long.parseLong(registered));
				} else
					return Optional.empty();
			} catch (Exception e) {
				return Optional.empty();
			}
		});
	}

	public HikariDataSource getHikariDataSource() {
		return hikariDataSource;
	}

}
