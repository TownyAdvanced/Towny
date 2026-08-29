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
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.Pair;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.zaxxer.hikari.pool.HikariPool;
import org.bukkit.Bukkit;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class TownySQLSource extends TownyDatabaseHandler {
	private final String tb_prefix;
	private static final SerializationContext context = new SerializationContext(false);
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
		RESIDENT("RESIDENTS", "SELECT name FROM ", "name"),
		HIBERNATED_RESIDENT("HIBERNATEDRESIDENTS", "", "uuid"),
		TOWN("TOWNS", "SELECT name FROM ", "name"),
		NATION("NATIONS", "SELECT name FROM ", "name"),
		WORLD("WORLDS", "SELECT name FROM ", "name"),
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

	private Map<String, String> loadResultSetIntoMap(ResultSet rs) throws SQLException {
		Map<String, String> keys = new HashMap<>();
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		for (int i = 1; i <= columns; ++i)
			keys.put(md.getColumnName(i), rs.getString(i));

		return keys;
	}

	/*
	 * Load keys
	 */
	
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

	@Override
	public boolean loadResidentList() {
		TownyMessaging.sendDebugMsg("Loading Resident List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT name, uuid FROM " + tb_prefix + "RESIDENTS")) {
			
			while (rs.next()) {
				final String name = rs.getString("name");
				final UUID uuid = super.parsePlayerUUID(rs.getString("uuid"), name);
				
				if (uuid == null) {
					plugin.getLogger().warning("Resident '" + name + "' does not have a valid uuid and cannot be loaded.");
					continue;
				}

				try {
					newResident(name, uuid);
				} catch (AlreadyRegisteredException e) {
					final Resident otherResident = universe.getResident(uuid);
					if (otherResident != null && !otherResident.getName().equals(name)) {
						// UUID is already registered
						super.pendingDuplicateResidents.add(Pair.pair(name, otherResident.getName()));
					}
				}
			}
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: resident list unknown error", e);
		}
		return false;
	}

	@Override
	public boolean loadTownList() {
		TownyMessaging.sendDebugMsg("Loading Town List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT name, uuid FROM " + tb_prefix + "TOWNS")) {

			while (rs.next()) {
				final String name = rs.getString("name");

				try {
					universe.newTownInternal(name, super.parseUUIDOrNew(rs.getString("uuid"), "town '" + name + "'"));
				} catch (AlreadyRegisteredException ignored) {}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: town list sql error : " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: town list unknown error", e);
		}
		return false;
	}

	@Override
	public boolean loadNationList() {
		TownyMessaging.sendDebugMsg("Loading Nation List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT name, uuid FROM " + tb_prefix + "NATIONS")) {
			
			while (rs.next()) {
				final String name = rs.getString("name");

				try {
					newNation(name, super.parseUUIDOrNew(rs.getString("uuid"), "nation '" + name + "'"));
				} catch (AlreadyRegisteredException ignored) {}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: nation list sql error : " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: nation list unknown error", e);
		}
		return false;
	}

	@Override
	public boolean loadWorldList() {
		TownyMessaging.sendDebugMsg("Loading World List");

		// Check for any new worlds registered with bukkit.
		for (World world : Bukkit.getServer().getWorlds())
			universe.registerTownyWorld(new TownyWorld(world.getName(), world.getUID()));

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT name, uuid FROM " + tb_prefix + "WORLDS")) {
			
			while (rs.next()) {
				final String name = rs.getString("name");
					
				// World is loaded in bukkit and got registered by the newWorld above.
				if (universe.getWorld(name) != null)
					continue;
					
				UUID uuid = null;
				try {
					uuid = UUID.fromString(rs.getString("uuid"));
				} catch (IllegalArgumentException | NullPointerException | SQLException ignored) {}
					
				if (uuid != null) {
					universe.registerTownyWorld(new TownyWorld(rs.getString("name"), uuid));
				} else {
					try {
						newWorld(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {}
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: world list sql error : " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: world list unknown error", e);
		}

		return true;
	}
	
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg("Loading PlotGroup List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT groupID FROM " + tb_prefix + "PLOTGROUPS")) {

			while (rs.next()) {
				try {
					universe.newPlotGroupInternal(UUID.fromString(rs.getString("groupID")));
				} catch (IllegalArgumentException e) {
					plugin.getLogger().log(Level.WARNING, "ID for plot group is not a valid uuid, skipped loading plot group {}", rs.getString("groupID"));
				}
			}
			
			return true;

		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "An exception occurred while loading plot group list", e);
		}
		
		return false;
	}
	
	@Override
	public boolean loadDistrictList() {
		TownyMessaging.sendDebugMsg("Loading District List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "DISTRICTS")) {

			while (rs.next()) {
				try {
					universe.newDistrictInternal(UUID.fromString(rs.getString("uuid")));
				} catch (IllegalArgumentException e) {
					plugin.getLogger().log(Level.WARNING, "ID for district is not a valid uuid, skipped loading district {}", rs.getString("uuid"));
				}
			}
			
			return true;

		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "An exception occurred while loading district list", e);
		}
		
		return false;
	}

	public boolean loadJailList() {
		TownyMessaging.sendDebugMsg("Loading Jail List");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "JAILS")) {
				
			while (rs.next()) {
				universe.newJailInternal(rs.getString("uuid"));
			}

			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.SEVERE, "An exception occurred while loading jail list", e);
		}
		
		return false;
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResidents() {
		TownyMessaging.sendDebugMsg("Loading Residents");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "RESIDENTS")) {
			
			while (rs.next()) {
				String residentName;
				try {
					residentName = rs.getString("name");
				} catch (SQLException ex) {
					plugin.getLogger().log(Level.SEVERE, "Loading Error: Error fetching a resident name from SQL Database. Skipping loading resident..", ex);
					continue;
				}
				
				Resident resident = universe.getResident(residentName);
				
				if (resident == null) {
					plugin.getLogger().severe(String.format("Loading Error: Could not fetch resident '%s' from Towny universe while loading from SQL DB.", residentName));
					continue;
				}

				if (!loadResident(resident, rs)) {
					plugin.getLogger().severe("Loading Error: Could not read resident data '" + resident.getName() + "'.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		}

		return true;
	}

	@Override
	public boolean loadResident(Resident resident) {

		/*
		 * Never called in SQL setups.
		 */
		return true;

	}

	private boolean loadResident(Resident resident, ResultSet rs) {
		try {
			return resident.load(loadResultSetIntoMap(rs));
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Load resident unknown error", e);
		}
		return false;
	}

	@Override
	public boolean loadTowns() {
		TownyMessaging.sendDebugMsg("Loading Towns");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNS ")) {
			while (rs.next()) {
				String townName;
				try {
					townName = rs.getString("name");
				} catch (SQLException ex) {
					plugin.getLogger().log(Level.SEVERE, "Loading Error: Error fetching a town name from SQL Database. Skipping loading town..", ex);
					continue;
				}
				Town town = universe.getTown(townName);
				if (town == null) {
					plugin.getLogger().severe(String.format("Loading Error: Could not fetch town '%s' from Towny universe while loading from SQL DB.", townName));
					continue;
				}

				if (!loadTown(rs)) {
					plugin.getLogger().warning("Loading Error: Could not read town data properly.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town sql Error - " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean loadTown(Town town) {

		/*
		 * Never called in SQL setups.
		 */
		return true;

	}

	private boolean loadTown(ResultSet rs) {
		String name = null;
		try {
			Town town = universe.getTown(rs.getString("name"));

			if (town == null) {
				TownyMessaging.sendErrorMsg("SQL: Load Town " + rs.getString("name") + ". Town was not registered properly on load!");
				return false;
			}

			name = town.getName();

			TownyMessaging.sendDebugMsg("Loading town " + name);
			return town.load(loadResultSetIntoMap(rs));

		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " sql Error - " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Load Town " + name + " unknown Error - ", e);
		}

		return false;
	}

	@Override
	public boolean loadNations() {
		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "NATIONS")) {
			
			while (rs.next()) {
				String nationName;
				try {
					nationName = rs.getString("name");
				} catch (SQLException ex) {
					plugin.getLogger().log(Level.SEVERE, "Loading Error: Error fetching a nation name from SQL Database. Skipping loading nation..", ex);
					continue;
				}
				Nation nation = universe.getNation(nationName);
				if (nation == null) {
					plugin.getLogger().severe(String.format("Loading Error: Could not fetch nation '%s' from Towny universe while loading from SQL DB.", nationName));
					continue;
				}

				if (!loadNation(rs)) {
					plugin.getLogger().warning("Loading Error: Could not properly read nation data.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation sql error " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public boolean loadNation(Nation nation) {

		/*
		 * Never called in SQL setups.
		 */
		return true;

	}

	private boolean loadNation(ResultSet rs) {
		String name = null;
		try {
			Nation nation = universe.getNation(rs.getString("name"));
			
			// Could not find nation in universe maps
			if (nation == null) {
				plugin.getLogger().warning(String.format("Error: The nation with the name '%s' was not registered and cannot be loaded!", rs.getString("name")));
				return false;
			}
			
			name = nation.getName();

			TownyMessaging.sendDebugMsg("Loading nation " + nation.getName());

			return nation.load(loadResultSetIntoMap(rs));
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " SQL Error - " + e.getMessage());
		}

		return false;
	}

	@Override
	public boolean loadWorlds() {
		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "WORLDS")) {

			while (rs.next()) {
				String worldName;
				try {
					worldName = rs.getString("name");
				} catch (SQLException ex) {
					plugin.getLogger().log(Level.SEVERE, "Loading Error: Error fetching a world name from SQL Database. Skipping loading world..", ex);
					continue;
				}
				TownyWorld world = universe.getWorld(worldName);
				if (world == null) {
					plugin.getLogger().severe(String.format("Loading Error: Could not fetch world '%s' from Towny universe while loading from SQL DB.", worldName));
					continue;
				}
				if (!loadWorld(rs)) {
					plugin.getLogger().warning("Loading Error: Could not read properly world data.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Error reading worlds from SQL database!");
			return false;
		}
		return true;
	}

	@Override
	public boolean loadWorld(TownyWorld world) {
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + tb_prefix + "WORLDS WHERE name=?")) {
			ps.setString(1, world.getName());

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return loadWorld(rs);
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load world sql error (" + world.getName() + ")" + e.getMessage());
		}
		return false;
	}

	private boolean loadWorld(ResultSet rs) {
		String worldName = null;
		try {
			worldName = rs.getString("name");
			TownyWorld world = universe.getWorld(worldName);
			if (world == null)
				throw new Exception("World " + worldName + " not registered!");

			TownyMessaging.sendDebugMsg("Loading world " + world.getName());

			return world.load(loadResultSetIntoMap(rs));
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg(
					"SQL: Load world sql error (" + (worldName != null ? worldName : "NULL") + ")" + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		return false;
	}

	@Override
	public boolean loadTownBlocks() {

		String line = "";
		TownyMessaging.sendDebugMsg("Loading Town Blocks.");

		TownBlock townBlock = null;
		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNBLOCKS")) {

			while (rs.next()) {
				String worldName = rs.getString("world");
				int x = rs.getInt("x");
				int z = rs.getInt("z");

				if (!universe.hasTownyWorld(worldName))
					continue;

				try {
					townBlock = universe.getTownBlock(new WorldCoord(worldName, x, z));
					townBlock.load(loadResultSetIntoMap(rs));
				} catch (NotRegisteredException ex) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while fetching townblock: " + worldName + " "
							+ x + " " + z + " from memory!");
					return false;
				}
			}

		} catch (SQLException ex) {
			plugin.getLogger().log(Level.WARNING, "Loading Error: Exception while reading TownBlock: "
					+ (townBlock != null ? townBlock : "NULL") + " at line: " + line + " in the sql database", ex);
			return false;
		}

		return true;
	}
	
	@Override
	public boolean loadPlotGroups() {
		TownyMessaging.sendDebugMsg("Loading plot groups.");
		
		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "PLOTGROUPS ")) {
			while (rs.next()) {
				if (!loadPlotGroup(rs)) {
					plugin.getLogger().warning("Loading Error: Could not read plotgroup data properly.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load PlotGroup sql Error - " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean loadDistricts() {
		TownyMessaging.sendDebugMsg("Loading districts.");
		
		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "DISTRICTS ")) {
			while (rs.next()) {
				if (!loadDistrict(rs)) {
					plugin.getLogger().warning("Loading Error: Could not read district data properly.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load District sql Error - " + e.getMessage());
			return false;
		}

		return true;
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

	private boolean loadPlotGroup(ResultSet rs) {
		String uuid = null;
		
		try {
			uuid = rs.getString("groupID");
			PlotGroup group = universe.getGroup(UUID.fromString(uuid));
			if (group == null) {
				TownyMessaging.sendErrorMsg("SQL: A plot group was not registered properly on load!");
				return true;
			}
			return group.load(loadResultSetIntoMap(rs));
		} catch (SQLException | IllegalArgumentException e) {
			plugin.getLogger().log(Level.WARNING, "Loading Error: Exception while reading plot group: " + uuid, e);
		}
		return false;
	}

	@Override
	public boolean loadPlotGroup(PlotGroup group) {
		// Unused in SQL.
		return true;
	}

	private boolean loadDistrict(ResultSet rs) {
		String line = null;
		String uuidString = null;
		
		try {
			District district = universe.getDistrict(UUID.fromString(rs.getString("uuid")));
			if (district == null) {
				TownyMessaging.sendErrorMsg("SQL: A district was not registered properly on load!");
				return true;
			}
			return district.load(loadResultSetIntoMap(rs));
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Loading Error: Exception while reading district: " + uuidString
			+ " at line: " + line + " in the sql database", e);
		}
		return false;
	}
	
	@Override
	public boolean loadDistrict(District district) {
		// Unused in SQL.
		return true;
	}
	
	@Override
	public boolean loadJails() {
		TownyMessaging.sendDebugMsg("Loading Jails");

		try (Connection connection = getConnection();
			 Statement s = connection.createStatement();
			 ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "JAILS ")) {
			while (rs.next()) {
				if (!loadJail(rs)) {
					plugin.getLogger().warning("Loading Error: Could not read jail data properly.");
					return false;
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Jail sql Error - " + e.getMessage());
			return false;
		}

		return true;
	}
	
	@Override
	public boolean loadJail(Jail jail) {
		// Unused in SQL.
		return true;
	}
	
	private boolean loadJail(ResultSet rs) {
		String uuid = null;
		try {
			Jail jail = universe.getJail(UUID.fromString(rs.getString("uuid")));
			if (jail == null) {
				TownyMessaging.sendErrorMsg("SQL: A jail was not registered properly on load!");
				return true;
			}
			return jail.load(loadResultSetIntoMap(rs));
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Jail " + uuid + " sql Error - " + e.getMessage());
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Load Jail " + uuid + " unknown Error - ", e);
		}

		return false;
	}
	
	/*
	 * Save individual towny objects
	 */

	@Override
	public synchronized boolean saveResident(Resident resident) {

		TownyMessaging.sendDebugMsg("Saving Resident " + resident.getName());
		try {
			updateDB("RESIDENTS", resident.getObjectDataMap(context), Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}
	
	@Override
	public synchronized boolean saveHibernatedResident(UUID uuid, long registered) {
		TownyMessaging.sendDebugMsg("Saving Hibernated Resident " + uuid);
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("uuid", uuid);
			res_hm.put("registered", registered);

			updateDB("HIBERNATEDRESIDENTS", res_hm, Collections.singletonList("uuid"));
			return true;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Hibernated Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public synchronized boolean saveTown(Town town) {

		TownyMessaging.sendDebugMsg("Saving town " + town.getName());
		try {
			updateDB("TOWNS", town.getObjectDataMap(context), Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Town unknown error", e);
		}
		return false;
	}

	@Override
	public synchronized boolean savePlotGroup(PlotGroup group) {
		TownyMessaging.sendDebugMsg("Saving group " + group.getName());
		try {
			updateDB("PLOTGROUPS", group.getObjectDataMap(context), Collections.singletonList("groupID"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Plot groups unknown error", e);
		}
		return false;
	}

	@Override
	public boolean saveDistrict(District district) {
		TownyMessaging.sendDebugMsg("Saving district " + district.getName());
		try {
			updateDB("DISTRICTS", district.getObjectDataMap(context), Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Districts unknown error", e);
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation) {

		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			updateDB("NATIONS", nation.getObjectDataMap(context), Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Nation unknown error", e);
		}
		return false;
	}

	@Override
	public synchronized boolean saveWorld(TownyWorld world) {

		TownyMessaging.sendDebugMsg("Saving world " + world.getName());
		try {
			updateDB("WORLDS", world.getObjectDataMap(context), Collections.singletonList("name"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save world unknown error (" + world.getName() + ")", e);
		}
		return false;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock) {

		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x"
				+ townBlock.getZ());
		try {
			updateDB("TOWNBLOCKS", townBlock.getObjectDataMap(context), Arrays.asList("world", "x", "z"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save TownBlock unknown error", e);
		}
		return false;
	}
	
	@Override
	public synchronized boolean saveJail(Jail jail) {

		TownyMessaging.sendDebugMsg("Saving jail " + jail.getUUID());
		try {
			updateDB("JAILS", jail.getObjectDataMap(context), Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save jail unknown error", e);
		}
		return false;
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
