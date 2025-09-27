/*
 * Towny MYSQL Source by StPinker
 *
 * Released under LGPL
 */
package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Position;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
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
import java.util.stream.Collectors;

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

		try (Connection connection = getConnection()) {
			SQLSchema.cleanup(connection);
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred when cleaning up SQL schema.", e);
		}

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
			 ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "RESIDENTS")) {
			
			while (rs.next()) {
				try {
					newResident(rs.getString("name"));
				} catch (AlreadyRegisteredException ignored) {}
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
			 ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "TOWNS")) {

			while (rs.next()) {
				try {
					universe.newTownInternal(rs.getString("name"));
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
			 ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "NATIONS")) {
			
			while (rs.next()) {
				try {
					newNation(rs.getString("name"));
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
			String search;

			try {
				if (rs.getString("uuid") != null && !rs.getString("uuid").isEmpty()) {
					
					UUID uuid = UUID.fromString(rs.getString("uuid"));
					if (universe.hasResident(uuid)) {
						Resident olderRes = universe.getResident(uuid);
						if (resident.getLastOnline() > olderRes.getLastOnline()) {
							TownyMessaging.sendDebugMsg("Deleting : " + olderRes.getName() + " which is a dupe of " + resident.getName());
							try {
								universe.unregisterResident(olderRes);
							} catch (NotRegisteredException ignored) {}
							// Check if the older resident is a part of a town
							if (olderRes.hasTown()) {
								try {
									// Resident#removeTown saves the resident, so we can't use it.
									olderRes.getTown().removeResident(olderRes);
								} catch (NotRegisteredException ignored) {}
							}
							deleteResident(olderRes);					
						} else {
							TownyMessaging.sendDebugMsg("Deleting resident : " + resident.getName() + " which is a dupe of " + olderRes.getName());
							try {
								universe.unregisterResident(resident);
							} catch (NotRegisteredException ignored) {}
							deleteResident(resident);
							return true;
						}
					}	
					resident.setUUID(uuid);
					universe.registerResidentUUID(resident);
				}
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get uuid column on the residents table", e);
			}

			try {
				resident.setLastOnline(rs.getLong("lastOnline"));
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get lastOnline column on the residents table", e);
			}
			
			try {
				resident.setRegistered(rs.getLong("registered"));
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get registered column on the residents table", e);
			}
			
			try {
				resident.setJoinedTownAt(rs.getLong("joinedTownAt"));
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get joinedTownAt column on the residents table", e);
			}
			
			try {
				resident.setNPC(rs.getBoolean("isNPC"));
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get isNPC column on the residents table", e);
			}
			
			if (rs.getString("jailUUID") != null && !rs.getString("jailUUID").isEmpty()) {
				UUID uuid = UUID.fromString(rs.getString("jailUUID"));
				if (universe.hasJail(uuid)) {
					resident.setJail(universe.getJail(uuid));
				}
			}
			
			if (resident.isJailed()) {
				try {
					if (rs.getString("jailCell") != null && !rs.getString("jailCell").isEmpty())
						resident.setJailCell(rs.getInt("jailCell"));
				} catch (SQLException e) {
					plugin.getLogger().log(Level.WARNING, "Could not get jailCell column on the residents table", e);
				}
				
				try {
					if (rs.getString("jailHours") != null && !rs.getString("jailHours").isEmpty())
						resident.setJailHours(rs.getInt("jailHours"));
				} catch (SQLException e) {
					plugin.getLogger().log(Level.WARNING, "Could not get jailHours column on the residents table", e);
				}
				
				try {
					if (rs.getString("jailBail") != null && !rs.getString("jailBail").isEmpty())
						resident.setJailBailCost(rs.getDouble("jailBail"));
				} catch (SQLException e) {
					plugin.getLogger().log(Level.WARNING, "Could not get jailBail column on the residents table", e);
				}
			}

			String line;
			try {
				line = rs.getString("about");
				if (line != null)
					resident.setAbout(line);
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get about column on the residents table", e);
			}
			
			try {
				line = rs.getString("friends");
				if (line != null) {
					search = (line.contains("#")) ? "#" : ",";
					List<Resident> friends = TownyAPI.getInstance().getResidents(line.split(search));
					for (Resident friend : friends) {
						resident.addFriend(friend);
					}
				}
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get friends column on the residents table", e);
			}
			
			try {
				resident.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "Could not get protectionStatus column on the residents table", e);
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					MetadataLoader.getInstance().deserializeMetadata(resident, line);
				}
			} catch (SQLException ignored) {
			}

			line = rs.getString("town");
			if ((line != null) && (!line.isEmpty())) {
				Town town = universe.getTown(line);
				if (town == null) {
					TownyMessaging.sendErrorMsg("Loading Error: " + resident.getName() + " tried to load the town " + line + " which is invalid, removing town from the resident.");
					resident.setTown(null, false);
				}
				else {
					resident.setTown(town, false);

					try {
						resident.setTitle(rs.getString("title"));
					} catch (SQLException e) {
						plugin.getLogger().log(Level.WARNING, "Could not get title column on the residents table", e);
					}
					try {
						resident.setSurname(rs.getString("surname"));
					} catch (SQLException e) {
						plugin.getLogger().log(Level.WARNING, "Could not get surname column on the residents table", e);
					}

					try {
						line = rs.getString("town-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setTownRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception ignored) {}

					try {
						line = rs.getString("nation-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setNationRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception ignored) {}
				}
			}
			return true;
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
		String line;
		String[] tokens;
		String search;
		String name = null;
		try {
			Town town = universe.getTown(rs.getString("name"));
			
			if (town == null) {
				TownyMessaging.sendErrorMsg("SQL: Load Town " + rs.getString("name") + ". Town was not registered properly on load!");
				return false;
			}
			
			name = town.getName();

			TownyMessaging.sendDebugMsg("Loading town " + name);

			try {
				Resident res = universe.getResident(rs.getString("mayor"));
				
				if (res == null)
					throw new TownyException();
					
				town.forceSetMayor(res);
			} catch (TownyException e1) {
				e1.getMessage();
				if (town.getResidents().size() == 0) {
					deleteTown(town);
					return true;
				} else {
					town.findNewMayor();
				}
			}

			town.setBoard(rs.getString("townBoard"));
			line = rs.getString("tag");
			if (line != null)
				town.setTag(line);
			line = rs.getString("founder");
			if (line != null)
				town.setFounder(line);
			town.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			town.setBonusBlocks(rs.getInt("bonus"));
			town.setManualTownLevel(rs.getInt("manualTownLevel"));
			town.setTaxPercentage(rs.getBoolean("taxpercent"));
			town.setTaxes(rs.getFloat("taxes"));
			town.setMaxPercentTaxAmount(rs.getFloat("maxPercentTaxAmount"));
			town.setHasUpkeep(rs.getBoolean("hasUpkeep"));
			town.setHasUnlimitedClaims(rs.getBoolean("hasUnlimitedClaims"));
			town.setVisibleOnTopLists(rs.getBoolean("visibleOnTopLists"));
			town.setPlotPrice(rs.getFloat("plotPrice"));
			town.setPlotTax(rs.getFloat("plotTax"));
			town.setEmbassyPlotPrice(rs.getFloat("embassyPlotPrice"));
			town.setEmbassyPlotTax(rs.getFloat("embassyPlotTax"));
			town.setCommercialPlotPrice(rs.getFloat("commercialPlotPrice"));
			town.setCommercialPlotTax(rs.getFloat("commercialPlotTax"));
			town.setSpawnCost(rs.getFloat("spawnCost"));
			town.setOpen(rs.getBoolean("open"));
			town.setPublic(rs.getBoolean("public"));
			town.setConquered(rs.getBoolean("conquered"), false);
			town.setAdminDisabledPVP(rs.getBoolean("admindisabledpvp"));
			town.setAdminEnabledPVP(rs.getBoolean("adminenabledpvp"));
			town.setAdminEnabledMobs(rs.getBoolean("adminEnabledMobs"));
			town.setAllowedToWar(rs.getBoolean("allowedToWar"));
			town.setJoinedNationAt(rs.getLong("joinedNationAt"));
			town.setMovedHomeBlockAt(rs.getLong("movedHomeBlockAt"));
			
			line = rs.getString("forSale");
			if (line != null)
				town.setForSale(Boolean.getBoolean(line));
			
			line = rs.getString("forSalePrice");
			if (line != null)
				town.setForSalePrice(Double.parseDouble(line));

			line = rs.getString("forSaleTime");
			if (line != null)
				town.setForSaleTime(Long.parseLong(line));

			town.setPurchasedBlocks(rs.getInt("purchased"));
			town.setNationZoneOverride(rs.getInt("nationZoneOverride"));
			town.setNationZoneEnabled(rs.getBoolean("nationZoneEnabled"));
			
			line = rs.getString("maxPercentTaxAmount");
			if (line != null)
				town.setMaxPercentTaxAmount(Double.parseDouble(line));
			else 
				town.setMaxPercentTaxAmount(TownySettings.getMaxTownTaxPercentAmount());

			line = rs.getString("homeBlock");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length == 3) {
					TownyWorld world = universe.getWorld(tokens[0]);
					if (world == null)
						TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid world.");
					else {
						try {
							int x = Integer.parseInt(tokens[1]);
							int z = Integer.parseInt(tokens[2]);
							TownBlock homeBlock = universe
									.getTownBlock(new WorldCoord(world.getName(), x, z));
							town.forceSetHomeBlock(homeBlock);
						} catch (NumberFormatException e) {
							TownyMessaging.sendErrorMsg(
									"[Warning] " + town.getName() + " homeBlock tried to load invalid location.");
						} catch (NotRegisteredException e) {
							TownyMessaging.sendErrorMsg(
									"[Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a home block.");
						}
					}
				}
			}

			line = rs.getString("spawn");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length >= 4)
					try {
						town.spawnPosition(Position.deserialize(tokens));
					} catch (IllegalArgumentException e) {
						plugin.getLogger().warning("Failed to load spawn location for town " + town.getName() + ": " + e.getMessage());
					}
			}
			// Load outpost spawns
			line = rs.getString("outpostSpawns");
			if (line != null) {
				String[] outposts = line.split(";");
				for (String spawn : outposts) {
					search = (line.contains("#")) ? "#" : ",";
					tokens = spawn.split(search);
					if (tokens.length >= 4)
						try {
							town.forceAddOutpostSpawn(Position.deserialize(tokens));
						} catch (IllegalArgumentException e) {
							plugin.getLogger().warning("Failed to load an outpost spawn location for town " + town.getName() + ": " + e.getMessage());
						}
				}
			}
			// Load legacy jail spawns into new Jail objects.
			try {
				line = rs.getString("jailSpawns");
				if (line != null) {
					String[] jails = line.split(";");
					for (String spawn : jails) {
						search = (line.contains("#")) ? "#" : ",";
						tokens = spawn.split(search);
						if (tokens.length >= 4)
							try {
								Position pos = Position.deserialize(tokens);
	
								TownBlock tb = universe.getTownBlock(pos.worldCoord());
								if (tb == null)
									continue;
								
								Jail jail = new Jail(UUID.randomUUID(), town, tb, Collections.singleton(pos));
								universe.registerJail(jail);
								town.addJail(jail);
								tb.setJail(jail);
								jail.save();
							} catch (IllegalArgumentException e) {
								plugin.getLogger().warning("Failed to load a legacy jail spawn location for town " + town.getName() + ": " + e.getMessage());
							}
					}
				}
			} catch (SQLException e) {
				// Ignore error if the column doesn't exist.
				if (!e.getMessage().equals("Column 'jailSpawns' not found."))
					throw e;
			}
			line = rs.getString("outlaws");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				for (String token : tokens) {
					if (!token.isEmpty()) {
						Resident resident = universe.getResident(token);
						if (resident != null)
							town.addOutlaw(resident);
						else {
							plugin.getLogger().warning(String.format("Loading Error: Cannot load outlaw with name '%s' for town '%s'! Skipping adding outlaw to town...", token, town.getName()));
						}
					}
				}
			}

			try {
				town.setUUID(UUID.fromString(rs.getString("uuid")));
			} catch (IllegalArgumentException | NullPointerException ee) {
				town.setUUID(UUID.randomUUID());
			}
			universe.registerTownUUID(town);

			int conqueredDays = rs.getInt("conqueredDays");
			town.setConqueredDays(conqueredDays);

			try {
				long registered = rs.getLong("registered");
				town.setRegistered(registered);
			} catch (Exception ignored) {
				town.setRegistered(0);
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					MetadataLoader.getInstance().deserializeMetadata(town, line);
				}
			} catch (SQLException ignored) {
			}

			try {
				line = rs.getString("nation");
				if (line != null && !line.isEmpty()) {
					Nation nation = universe.getNation(line);
					// Only set nation if it exists
					if (nation != null)
						town.setNation(nation, false);
				}
			} catch (SQLException ignored) {
			}

			town.setRuined(rs.getBoolean("ruined"));
			town.setRuinedTime(rs.getLong("ruinedTime"));
			town.setNeutral(rs.getBoolean("neutral"));

			town.setDebtBalance(rs.getFloat("debtBalance"));
			
			line = rs.getString("primaryJail");
			if (line != null && !line.isEmpty()) {
				UUID uuid = UUID.fromString(line);
				if (universe.hasJail(uuid))
					town.setPrimaryJail(universe.getJail(uuid));
			}
			
			line = rs.getString("trustedResidents");
			if (line != null && !line.isEmpty()) {
				search = (line.contains("#")) ? "#" : ",";
				for (Resident resident : TownyAPI.getInstance().getResidents(toUUIDArray(line.split(search))))
					town.addTrustedResident(resident);
			}
			
			line = rs.getString("trustedTowns");
			if (line != null && !line.isEmpty()) {
				search = (line.contains("#")) ? "#" : ",";
				List<UUID> uuids = Arrays.stream(line.split(search))
					.map(UUID::fromString)
					.collect(Collectors.toList());
				town.loadTrustedTowns(TownyAPI.getInstance().getTowns(uuids));
			}
			
			line = rs.getString("mapColorHexCode");
			if (line != null)
				town.setMapColorHexCode(line);
			else
				town.setMapColorHexCode(MapUtil.generateRandomTownColourAsHexCode());

			line = rs.getString("allies");
			if (line != null && !line.isEmpty()) {
				search = (line.contains("#")) ? "#" : ",";
				List<UUID> uuids = Arrays.stream(line.split(search))
						.map(uuid -> UUID.fromString(uuid))
						.collect(Collectors.toList());
				town.loadAllies(TownyAPI.getInstance().getTowns(uuids));
			}

			line = rs.getString("enemies");
			if (line != null && !line.isEmpty()) {
				search = (line.contains("#")) ? "#" : ",";
				List<UUID> uuids = Arrays.stream(line.split(search))
						.map(uuid -> UUID.fromString(uuid))
						.collect(Collectors.toList());
				town.loadEnemies(TownyAPI.getInstance().getTowns(uuids));
			}
			
			line = rs.getString("visibleOnTopLists");
			if (line != null && !line.isEmpty())
				town.setVisibleOnTopLists(rs.getBoolean("visibleOnTopLists"));

			return true;
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
		String line;
		String[] tokens;
		String search;
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

			Town town = universe.getTown(rs.getString("capital"));
			if (town != null) {
				try {
					nation.forceSetCapital(town);
				} catch (EmptyNationException e1) {
					plugin.getLogger().warning("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
					removeNation(nation, DeleteNationEvent.Cause.LOAD);
					return true;
				}
			}
			else {
				TownyMessaging.sendDebugMsg("Nation " + name + " could not set capital to " + rs.getString("capital") + ", selecting a new capital...");
				if (!nation.findNewCapital()) {
					plugin.getLogger().warning("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
					removeNation(nation, DeleteNationEvent.Cause.LOAD);
					return true;
				}
			}

			line = rs.getString("nationBoard");
			if (line != null)
				nation.setBoard(rs.getString("nationBoard"));
			else
				nation.setBoard("");

			line = rs.getString("mapColorHexCode");
			if (line != null)
				nation.setMapColorHexCode(line);
			else
				nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());

			nation.setTag(rs.getString("tag"));

			line = rs.getString("allies");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				List<Nation> allies = TownyAPI.getInstance().getNations(line.split(search));
				for (Nation ally : allies)
					nation.addAlly(ally);
			}

			line = rs.getString("enemies");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				List<Nation> enemies = TownyAPI.getInstance().getNations(line.split(search));
				for (Nation enemy : enemies) 
					nation.addEnemy(enemy);
			}

			nation.setSpawnCost(rs.getFloat("spawnCost"));
			nation.setNeutral(rs.getBoolean("neutral"));
			try {
				nation.setUUID(UUID.fromString(rs.getString("uuid")));
			} catch (IllegalArgumentException | NullPointerException ee) {
				nation.setUUID(UUID.randomUUID());
			}
			universe.registerNationUUID(nation);

			line = rs.getString("nationSpawn");
			if (line != null) {
				search = (line.contains("#")) ? "#" : ",";
				tokens = line.split(search);
				if (tokens.length >= 4)
					try {
						nation.spawnPosition(Position.deserialize(tokens));
					} catch (IllegalArgumentException e) {
						plugin.getLogger().warning("Failed to load nation spawn location for nation " + nation.getName() + ": " + e.getMessage());
					}
			}

			nation.setPublic(rs.getBoolean("isPublic"));

			nation.setOpen(rs.getBoolean("isOpen"));

			nation.setTaxPercentage(rs.getBoolean("taxpercent"));
			nation.setTaxes(rs.getDouble("taxes"));

			line = rs.getString("maxPercentTaxAmount");
			if (line != null)
				nation.setMaxPercentTaxAmount(Double.parseDouble(line));
			else 
				nation.setMaxPercentTaxAmount(TownySettings.getMaxNationTaxPercentAmount());

			try {
				line = rs.getString("registered");
				if (line != null) {
					nation.setRegistered(Long.parseLong(line));
				} else {
					nation.setRegistered(0);
				}
			} catch (SQLException ignored) {
			} catch (NumberFormatException | NullPointerException e) {
				nation.setRegistered(0);
			}

			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					MetadataLoader.getInstance().deserializeMetadata(nation, line);
				}
			} catch (SQLException ignored) {
			}

			try {
				line = rs.getString("conqueredTax");
				if (line != null && !line.isEmpty()) {
					nation.setConqueredTax(Double.parseDouble(line));
				}
			} catch (SQLException ignored) {
			}

			line = rs.getString("sanctionedTowns");
			if (line != null) {
				nation.loadSanctionedTowns(line.split("#"));
			}

			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " SQL Error - " + e.getMessage());
		} catch (TownyException ex) {
			plugin.getLogger().log(Level.WARNING, "SQL: Load Nation " + name + " unknown Error - ", ex);
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
		String line;
		boolean result;
		long resultLong;
		String search;
		String worldName = null;
		try {
			worldName = rs.getString("name");
			TownyWorld world = universe.getWorld(worldName);
			if (world == null)
				throw new Exception("World " + worldName + " not registered!");

			TownyMessaging.sendDebugMsg("Loading world " + world.getName());
			
			line = rs.getString("uuid");
			if (line != null && !line.isEmpty()) {
				try {
					world.setUUID(UUID.fromString(line));
				} catch (IllegalArgumentException ignored) {
					UUID uuid = BukkitTools.getWorldUUID(worldName);
					if (uuid != null)
						world.setUUID(uuid);
				}
			} else {
				UUID uuid = BukkitTools.getWorldUUID(worldName);
				if (uuid != null)
					world.setUUID(uuid);
			}

			result = rs.getBoolean("claimable");
			try {
				world.setClaimable(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("pvp");
			try {
				world.setPVP(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcepvp");
			try {
				world.setForcePVP(result);
			} catch (Exception ignored) {
			}
			
			result = rs.getBoolean("friendlyFire");
			try {
				world.setFriendlyFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcetownmobs");
			try {
				world.setForceTownMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("wildernessmobs");
			try {
				world.setWildernessMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("worldmobs");
			try {
				world.setWorldMobs(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("firespread");
			try {
				world.setFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forcefirespread");
			try {
				world.setForceFire(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("explosions");
			try {
				world.setExpl(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("forceexplosions");
			try {
				world.setForceExpl(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("endermanprotect");
			try {
				world.setEndermanProtect(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("disablecreaturetrample");
			try {
				world.setDisableCreatureTrample(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneBuild");
			try {
				world.setUnclaimedZoneBuild(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneDestroy");
			try {
				world.setUnclaimedZoneDestroy(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneSwitch");
			try {
				world.setUnclaimedZoneSwitch(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("unclaimedZoneItemUse");
			try {
				world.setUnclaimedZoneItemUse(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("unclaimedZoneName");
			try {
				world.setUnclaimedZoneName(line);
			} catch (Exception ignored) {
			}

			line = rs.getString("unclaimedZoneIgnoreIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setUnclaimedZoneIgnore(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("isDeletingEntitiesOnUnclaim");
			try {
				world.setDeletingEntitiesOnUnclaim(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("unclaimDeleteEntityTypes");
			if (line != null)
				try {
					List<String> entityTypes = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							entityTypes.add(split);

					world.setUnclaimDeleteEntityTypes(entityTypes);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementDelete");
			try {
				world.setUsingPlotManagementDelete(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementDeleteIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setPlotManagementDeleteIds(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementMayorDelete");
			try {
				world.setUsingPlotManagementMayorDelete(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementMayorDelete");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.toUpperCase().trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementMayorDelete(materials);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementRevert");
			try {
				world.setUsingPlotManagementRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementIgnoreIds");
			if (line != null)
				try {
					List<String> mats = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							mats.add(split);

					world.setPlotManagementIgnoreIds(mats);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingPlotManagementWildRegen");
			try {
				world.setUsingPlotManagementWildEntityRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("revertOnUnclaimWhitelistMaterials");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					for (String split : line.split("#"))
						if (!split.isEmpty())
							try {
								materials.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setRevertOnUnclaimWhitelistMaterials(materials);
				} catch (Exception ignored) {
				}

			line = rs.getString("plotManagementWildRegenEntities");
			if (line != null)
				try {
					List<String> entities = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								entities.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementWildRevertEntities(entities);
				} catch (Exception ignored) {
				}

			line = rs.getString("plotManagementWildRegenBlockWhitelist");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementWildRevertBlockWhitelist(materials);
				} catch (Exception ignored) {
				}

			line = rs.getString("wildRegenBlocksToNotOverwrite");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setWildRevertMaterialsToNotOverwrite(materials);
				} catch (Exception ignored) {
				}

			resultLong = rs.getLong("plotManagementWildRegenSpeed");
			try {
				world.setPlotManagementWildRevertDelay(resultLong);
			} catch (Exception ignored) {
			}
			
			result = rs.getBoolean("usingPlotManagementWildRegenBlocks");
			try {
				world.setUsingPlotManagementWildBlockRevert(result);
			} catch (Exception ignored) {
			}

			line = rs.getString("plotManagementWildRegenBlocks");
			if (line != null)
				try {
					List<String> materials = new ArrayList<>();
					search = (line.contains("#")) ? "#" : ",";
					for (String split : line.split(search))
						if (!split.isEmpty())
							try {
								materials.add(split.trim());
							} catch (NumberFormatException ignored) {
							}
					world.setPlotManagementWildRevertMaterials(materials);
				} catch (Exception ignored) {
				}

			result = rs.getBoolean("usingTowny");
			try {
				world.setUsingTowny(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("warAllowed");
			try {
				world.setWarAllowed(result);
			} catch (Exception ignored) {
			}

			result = rs.getBoolean("jailing");
			try {
				world.setJailingEnabled(result);
			} catch (Exception ignored) {
			}
			
			try {
				line = rs.getString("metadata");
				if (line != null && !line.isEmpty()) {
					MetadataLoader.getInstance().deserializeMetadata(world, line);
				}
			} catch (SQLException ignored) {
			}
			return true;
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
		boolean result;
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
				} catch (NotRegisteredException ex) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while fetching townblock: " + worldName + " "
							+ x + " " + z + " from memory!");
					return false;
				}

				line = rs.getString("name");
				if (line != null)
					try {
						townBlock.setName(line.trim());
					} catch (Exception ignored) {
					}

				line = rs.getString("town");
				if (line != null) {
					Town town = universe.getTown(line.trim());
					
					if (town == null) {
						TownyMessaging.sendErrorMsg("TownBlock file contains unregistered Town: " + line
							+ ", deleting " + townBlock.getWorld().getName() + "," + townBlock.getX() + ","
							+ townBlock.getZ());
						universe.removeTownBlock(townBlock);
						deleteTownBlock(townBlock);
						continue;
					}
					
					townBlock.setTown(town, false);
					try {
						town.addTownBlock(townBlock);
						TownyWorld townyWorld = townBlock.getWorld();
						if (townyWorld != null && !townyWorld.hasTown(town))
							townyWorld.addTown(town);
					} catch (AlreadyRegisteredException ignored) {
					}
				}

				line = rs.getString("resident");
				if (line != null && !line.isEmpty()) {
					Resident res = universe.getResident(line.trim());
					if (res != null)
						townBlock.setResident(res, false);
					else {
						TownyMessaging.sendErrorMsg(String.format(
							"Error fetching resident '%s' for townblock '%s'!",
							line.trim(), townBlock.toString()
						));
					}
				}

				line = rs.getString("type");
				if (line != null)
					townBlock.setType(TownBlockTypeHandler.getTypeInternal(line));

				line = rs.getString("price");
				if (line != null)
					try {
						townBlock.setPlotPrice(Float.parseFloat(line.trim()));
					} catch (Exception ignored) {
					}

				boolean taxed = rs.getBoolean("taxed");
				try {
					townBlock.setTaxed(taxed);
				} catch (Exception ignored) {
				}
				
				line = rs.getString("typeName");
				if (line != null) 
					townBlock.setType(TownBlockTypeHandler.getTypeInternal(line));

				boolean outpost = rs.getBoolean("outpost");
				try {
					townBlock.setOutpost(outpost);
				} catch (Exception ignored) {
				}

				line = rs.getString("permissions");
				if ((line != null) && !line.isEmpty())
					try {
						townBlock.setPermissions(line.trim().replaceAll("#", ","));
						// set = true;
					} catch (Exception ignored) {
					}

				result = rs.getBoolean("changed");
				try {
					townBlock.setChanged(result);
				} catch (Exception ignored) {
				}

				townBlock.setClaimedAt(rs.getLong("claimedAt"));

				
				line = rs.getString("minTownMembershipDays");
				if (line != null && !line.isEmpty())
					townBlock.setMinTownMembershipDays(Integer.valueOf(line));

				line = rs.getString("maxTownMembershipDays");
				if (line != null && !line.isEmpty())
					townBlock.setMaxTownMembershipDays(Integer.valueOf(line));

				try {
					line = rs.getString("metadata");
					if (line != null && !line.isEmpty()) {
						MetadataLoader.getInstance().deserializeMetadata(townBlock, line);
					}
				} catch (SQLException ignored) {
				}

				try {
					line = rs.getString("groupID");
					if (line != null && !line.isEmpty()) {
						try {
							UUID groupID = UUID.fromString(line.trim());
							PlotGroup group = universe.getGroup(groupID);
							if (group != null) {
								townBlock.setPlotObjectGroup(group);
								if (group.getPermissions() == null && townBlock.getPermissions() != null)
									group.setPermissions(townBlock.getPermissions());
								if (townBlock.hasResident())
									group.setResident(townBlock.getResidentOrNull());
							}
						} catch (Exception ignored) {
						}

					}
				} catch (SQLException ignored) {
				}

				try {
					line = rs.getString("districtID");
					if (line != null && !line.isEmpty()) {
						try {
							UUID districtID = UUID.fromString(line.trim());
							District district = universe.getDistrict(districtID);
							if (district != null) {
								townBlock.setDistrict(district);
							}
						} catch (Exception ignored) {
						}

					}
				} catch (SQLException ignored) {
				}

				line = rs.getString("trustedResidents");
				if (line != null && !line.isEmpty() && townBlock.getTrustedResidents().isEmpty()) {
					String search = (line.contains("#")) ? "#" : ",";
					for (Resident resident : TownyAPI.getInstance().getResidents(toUUIDArray(line.split(search))))
						townBlock.addTrustedResident(resident);

					if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getTrustedResidents().isEmpty() && townBlock.getTrustedResidents().size() > 0)
						townBlock.getPlotObjectGroup().setTrustedResidents(townBlock.getTrustedResidents());
				}
				
				line = rs.getString("customPermissionData");
				if (line != null && !line.isEmpty() && townBlock.getPermissionOverrides().isEmpty()) {
					Map<String, String> map = new Gson().fromJson(line, new TypeToken<Map<String, String>>(){}.getType());

					for (Map.Entry<String, String> entry : map.entrySet()) {
						Resident resident;
						try {
							resident = TownyAPI.getInstance().getResident(UUID.fromString(entry.getKey()));
						} catch (IllegalArgumentException e) {
							continue;
						}
						
						if (resident == null)
							continue;

						townBlock.getPermissionOverrides().put(resident, new PermissionData(entry.getValue()));
					}

					if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPermissionOverrides().isEmpty() && townBlock.getPermissionOverrides().size() > 0)
						townBlock.getPlotObjectGroup().setPermissionOverrides(townBlock.getPermissionOverrides());
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
		String line = null;
		String uuid = null;
		
		try {
			PlotGroup group = universe.getGroup(UUID.fromString(rs.getString("groupID")));
			if (group == null) {
				TownyMessaging.sendErrorMsg("SQL: A plot group was not registered properly on load!");
				return true;
			}
			uuid = group.getUUID().toString();
			
			line = rs.getString("groupName");
			if (line != null)
				try {
					group.setName(line.trim());
				} catch (Exception ignored) {
				}
			
			line = rs.getString("town");
			if (line != null) {
				Town town = universe.getTown(line.trim());
				if (town != null) {
					group.setTown(town);
				} else {
					deletePlotGroup(group);
					return true;
				}
			}
			
			line = rs.getString("groupPrice");
			if (line != null) {
				try {
					group.setPrice(Float.parseFloat(line.trim()));
				} catch (Exception ignored) {}
			}
			
			line = rs.getString("metadata");
			if (line != null) {
				MetadataLoader.getInstance().deserializeMetadata(group, line);
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Loading Error: Exception while reading plot group: " + uuid
			+ " at line: " + line + " in the sql database", e);
			return false;
		}
		return true;
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
			uuidString = district.getUUID().toString();
			
			line = rs.getString("districtName");
			if (line != null)
				try {
					district.setName(line.trim());
				} catch (Exception ignored) {
				}
			
			line = rs.getString("town");
			if (line != null) {
				UUID uuid = UUID.fromString(line.trim());
				if (uuid == null) {
					deleteDistrict(district);
					return true;
				}
				Town town = universe.getTown(uuid);
				if (town != null) {
					district.setTown(town);
				} else {
					deleteDistrict(district);
					return true;
				}
			}

			line = rs.getString("metadata");
			if (line != null) {
				MetadataLoader.getInstance().deserializeMetadata(district, line);
			}
		} catch (SQLException e) {
			plugin.getLogger().log(Level.WARNING, "Loading Error: Exception while reading district: " + uuidString
			+ " at line: " + line + " in the sql database", e);
			return false;
		}
		return true;
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
		String line;
		String[] tokens;
		String uuid = null;
		try {
			Jail jail = universe.getJail(UUID.fromString(rs.getString("uuid")));
			if (jail == null) {
				TownyMessaging.sendErrorMsg("SQL: A jail was not registered properly on load!");
				return true;
			}
			uuid = jail.getUUID().toString();
			
			line = rs.getString("townBlock");
			if (line != null) {
				tokens = line.split("#");
				WorldCoord wc = null;
				try {
					wc = new WorldCoord(tokens[0], Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim()));
					if (wc.isWilderness() || wc.getTownOrNull() == null) // Not a number format exception but it gets handled the same so why not.
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid townblock " + line + " deleting jail.");
					removeJail(jail);
					deleteJail(jail);
					return true;
				}

				TownBlock tb = wc.getTownBlockOrNull();
				Town town = tb.getTownOrNull();
				jail.setTownBlock(tb);
				jail.setTown(town);
				tb.setJail(jail);
				town.addJail(jail);
			}
			
			line = rs.getString("spawns");
			if (line != null) {
				String[] jails = line.split(";");
				for (String spawn : jails) {
					tokens = spawn.split("#");
					if (tokens.length >= 4)
						try {
							jail.addJailCell(Position.deserialize(tokens));
						} catch (IllegalArgumentException e) {
							TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid spawn " + line + " skipping: " + e.getMessage());
						}
				}
				if (jail.getJailCellLocations().size() < 1) {
					TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " loaded with zero spawns " + line + " deleting jail.");
					removeJail(jail);
					deleteJail(jail);
					return true;
				}
			}
			
				
			return true;
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
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("name", resident.getName());
			res_hm.put("uuid", resident.hasUUID() ? resident.getUUID().toString() : "");
			res_hm.put("lastOnline", resident.getLastOnline());
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("joinedTownAt", resident.getJoinedTownAt());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("jailUUID", resident.isJailed() ? resident.getJail().getUUID() : "");
			res_hm.put("jailCell", resident.getJailCell());
			res_hm.put("jailHours", resident.getJailHours());
			res_hm.put("jailBail", resident.getJailBailCost());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			
			if (!TownySettings.getDefaultResidentAbout().equals(resident.getAbout()))
				res_hm.put("about", resident.getAbout());
			res_hm.put("town", resident.hasTown() ? resident.getTown().getName() : "");
			res_hm.put("town-ranks", resident.hasTown() ? StringMgmt.join(resident.getTownRanksForSaving(), "#") : "");
			res_hm.put("nation-ranks", resident.hasTown() ? StringMgmt.join(resident.getNationRanksForSaving(), "#") : "");
			res_hm.put("friends", StringMgmt.join(resident.getFriends(), "#"));
			res_hm.put("protectionStatus", resident.getPermissions().toString().replaceAll(",", "#"));

			if (resident.hasMeta())
				res_hm.put("metadata", serializeMetadata(resident));
			else
				res_hm.put("metadata", "");

			updateDB("RESIDENTS", res_hm, Collections.singletonList("name"));
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
			HashMap<String, Object> twn_hm = new HashMap<>();
			twn_hm.put("name", town.getName());
			twn_hm.put("outlaws", StringMgmt.join(town.getOutlaws(), "#"));
			twn_hm.put("mayor", town.hasMayor() ? town.getMayor().getName() : "");
			twn_hm.put("nation", town.hasNation() ? town.getNationOrNull().getName() : "");
			twn_hm.put("townBoard", town.getBoard());
			twn_hm.put("tag", town.getTag());
			twn_hm.put("founder", town.getFounder());
			twn_hm.put("protectionStatus", town.getPermissions().toString().replaceAll(",", "#"));
			twn_hm.put("bonus", town.getBonusBlocks());
			twn_hm.put("manualTownLevel", town.getManualTownLevel());
			twn_hm.put("purchased", town.getPurchasedBlocks());
			twn_hm.put("nationZoneOverride", town.getNationZoneOverride());
			twn_hm.put("nationZoneEnabled", town.isNationZoneEnabled());
			twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
			twn_hm.put("spawnCost", town.getSpawnCost());
			twn_hm.put("plotPrice", town.getPlotPrice());
			twn_hm.put("plotTax", town.getPlotTax());
			twn_hm.put("taxes", town.getTaxes());
			twn_hm.put("hasUpkeep", town.hasUpkeep());
			twn_hm.put("hasUnlimitedClaims", town.hasUnlimitedClaims());
			twn_hm.put("visibleOnTopLists", town.isVisibleOnTopLists());
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("maxPercentTaxAmount", town.getMaxPercentTaxAmount());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("conquered", town.isConquered());
			twn_hm.put("conqueredDays", town.getConqueredDays());
			twn_hm.put("admindisabledpvp", town.isAdminDisabledPVP());
			twn_hm.put("adminenabledpvp", town.isAdminEnabledPVP());
			twn_hm.put("adminEnabledMobs", town.isAdminEnabledMobs());
			twn_hm.put("allowedToWar", town.isAllowedToWar());
			twn_hm.put("joinedNationAt", town.getJoinedNationAt());
			twn_hm.put("mapColorHexCode", town.getMapColorHexCode());
			twn_hm.put("movedHomeBlockAt", town.getMovedHomeBlockAt());
			twn_hm.put("forSale", town.isForSale());
			twn_hm.put("forSalePrice", town.getForSalePrice());
			twn_hm.put("forSaleTime", town.getForSaleTime());
			if (town.hasMeta())
				twn_hm.put("metadata", serializeMetadata(town));
			else
				twn_hm.put("metadata", "");

			twn_hm.put("homeblock",
					town.hasHomeBlock()
							? town.getHomeBlock().getWorld().getName() + "#" + town.getHomeBlock().getX() + "#"
									+ town.getHomeBlock().getZ()
							: "");
			
			final Position spawnPos = town.spawnPosition();
			twn_hm.put("spawn", spawnPos != null ? String.join("#", spawnPos.serialize()) : "");
			// Outpost Spawns
			StringBuilder outpostArray = new StringBuilder();
			if (town.hasOutpostSpawn())
				for (Position spawn : town.getOutpostSpawns()) {
					outpostArray.append(String.join("#", spawn.serialize())).append(";");
				}
			twn_hm.put("outpostSpawns", outpostArray.toString());
			if (town.hasValidUUID()) {
				twn_hm.put("uuid", town.getUUID());
			} else {
				twn_hm.put("uuid", UUID.randomUUID());
			}
			twn_hm.put("registered", town.getRegistered());

			twn_hm.put("ruined", town.isRuined());
			twn_hm.put("ruinedTime", town.getRuinedTime());
			twn_hm.put("neutral", town.isNeutral());
			
			twn_hm.put("debtBalance", town.getDebtBalance());

			if (town.getPrimaryJail() != null)
				twn_hm.put("primaryJail", town.getPrimaryJail().getUUID());
			
			twn_hm.put("trustedResidents", StringMgmt.join(toUUIDList(town.getTrustedResidents()), "#"));
			twn_hm.put("trustedTowns", StringMgmt.join(town.getTrustedTownsUUIDS(), "#"));
			
			twn_hm.put("allies", StringMgmt.join(town.getAlliesUUIDs(), "#"));
			
			twn_hm.put("enemies", StringMgmt.join(town.getEnemiesUUIDs(), "#"));
			
			updateDB("TOWNS", twn_hm, Collections.singletonList("name"));
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
			HashMap<String, Object> pltgrp_hm = new HashMap<>();
			pltgrp_hm.put("groupID", group.getUUID().toString());
			pltgrp_hm.put("groupName", group.getName());
			pltgrp_hm.put("groupPrice", group.getPrice());
			pltgrp_hm.put("town", group.getTown().getName());
			pltgrp_hm.put("metadata", serializeMetadata(group));

			updateDB("PLOTGROUPS", pltgrp_hm, Collections.singletonList("groupID"));

		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Plot groups unknown error", e);
		}
		return false;
	}

	@Override
	public boolean saveDistrict(District district) {
		TownyMessaging.sendDebugMsg("Saving district " + district.getName());
		try {
			HashMap<String, Object> pltgrp_hm = new HashMap<>();
			pltgrp_hm.put("uuid", district.getUUID().toString());
			pltgrp_hm.put("districtName", district.getName());
			pltgrp_hm.put("town", district.getTown().getUUID().toString());
			pltgrp_hm.put("metadata", serializeMetadata(district));

			updateDB("DISTRICTS", pltgrp_hm, Collections.singletonList("uuid"));

		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Districts unknown error", e);
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation) {

		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();
			nat_hm.put("name", nation.getName());
			nat_hm.put("capital", nation.hasCapital() ? nation.getCapital().getName() : "");
			nat_hm.put("nationBoard", nation.getBoard());
			nat_hm.put("mapColorHexCode", nation.getMapColorHexCode());
			nat_hm.put("tag", nation.hasTag() ? nation.getTag() : "");
			nat_hm.put("allies", StringMgmt.join(nation.getAllies(), "#"));
			nat_hm.put("enemies", StringMgmt.join(nation.getEnemies(), "#"));
			nat_hm.put("taxes", nation.getTaxes());
            nat_hm.put("taxpercent", nation.isTaxPercentage());
			nat_hm.put("maxPercentTaxAmount", nation.getMaxPercentTaxAmount());
			nat_hm.put("spawnCost", nation.getSpawnCost());
			nat_hm.put("neutral", nation.isNeutral());
			
			final Position spawnPos = nation.spawnPosition();
			nat_hm.put("nationSpawn", spawnPos != null ? String.join("#", spawnPos.serialize()) : "");
			if (nation.hasValidUUID()) {
				nat_hm.put("uuid", nation.getUUID());
			} else {
				nat_hm.put("uuid", UUID.randomUUID());
			}
			nat_hm.put("registered", nation.getRegistered());
			nat_hm.put("isPublic", nation.isPublic());
			nat_hm.put("isOpen", nation.isOpen());

			if (nation.hasMeta())
				nat_hm.put("metadata", serializeMetadata(nation));
			else
				nat_hm.put("metadata", "");

			nat_hm.put("conqueredTax", nation.getConqueredTax());
			nat_hm.put("sanctionedTowns", StringMgmt.join(nation.getSanctionedTownsForSaving(), "#"));
			updateDB("NATIONS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save Nation unknown error", e);
		}
		return false;
	}

	@Override
	public synchronized boolean saveWorld(TownyWorld world) {

		TownyMessaging.sendDebugMsg("Saving world " + world.getName());
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();

			nat_hm.put("name", world.getName());
			
			nat_hm.put("uuid", world.getUUID());

			// PvP
			nat_hm.put("pvp", world.isPVP());
			// Force PvP
			nat_hm.put("forcepvp", world.isForcePVP());
			// Friendly Fire
			nat_hm.put("friendlyFire", world.isFriendlyFireEnabled());
			// Claimable
			nat_hm.put("claimable", world.isClaimable());
			// has monster spawns
			nat_hm.put("worldmobs", world.hasWorldMobs());
			// has wilderness monster spawns
			nat_hm.put("wildernessmobs", world.hasWildernessMobs());
			// force town mob spawns
			nat_hm.put("forcetownmobs", world.isForceTownMobs());
			// has firespread enabled
			nat_hm.put("firespread", world.isFire());
			nat_hm.put("forcefirespread", world.isForceFire());
			// has explosions enabled
			nat_hm.put("explosions", world.isExpl());
			nat_hm.put("forceexplosions", world.isForceExpl());
			// Enderman block protection
			nat_hm.put("endermanprotect", world.isEndermanProtect());
			// CreatureTrample
			nat_hm.put("disablecreaturetrample", world.isDisableCreatureTrample());

			// Unclaimed Zone Build
			nat_hm.put("unclaimedZoneBuild", world.getUnclaimedZoneBuild());
			// Unclaimed Zone Destroy
			nat_hm.put("unclaimedZoneDestroy", world.getUnclaimedZoneDestroy());
			// Unclaimed Zone Switch
			nat_hm.put("unclaimedZoneSwitch", world.getUnclaimedZoneSwitch());
			// Unclaimed Zone Item Use
			nat_hm.put("unclaimedZoneItemUse", world.getUnclaimedZoneItemUse());
			// Unclaimed Zone Name
			if (world.getUnclaimedZoneName() != null)
				nat_hm.put("unclaimedZoneName", world.getUnclaimedZoneName());

			// Unclaimed Zone Ignore Ids
			if (world.getUnclaimedZoneIgnoreMaterials() != null)
				nat_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), "#"));

			// Deleting EntityTypes from Townblocks on Unclaim.
			nat_hm.put("isDeletingEntitiesOnUnclaim", world.isDeletingEntitiesOnUnclaim());
			if (world.getUnclaimDeleteEntityTypes() != null)
				nat_hm.put("unclaimDeleteEntityTypes", StringMgmt.join(BukkitTools.convertKeyedToString(world.getUnclaimDeleteEntityTypes()), "#"));

			// Using PlotManagement Delete
			nat_hm.put("usingPlotManagementDelete", world.isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				nat_hm.put("plotManagementDeleteIds", StringMgmt.join(world.getPlotManagementDeleteIds(), "#"));

			// Using PlotManagement Mayor Delete
			nat_hm.put("usingPlotManagementMayorDelete", world.isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				nat_hm.put("plotManagementMayorDelete", StringMgmt.join(world.getPlotManagementMayorDelete(), "#"));

			// Using PlotManagement Revert
			nat_hm.put("usingPlotManagementRevert", world.isUsingPlotManagementRevert());

			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				nat_hm.put("plotManagementIgnoreIds", StringMgmt.join(world.getPlotManagementIgnoreIds(), "#"));

			// Revert on Unclaim whitelisted materials
			if (world.getRevertOnUnclaimWhitelistMaterials() != null)
				nat_hm.put("revertOnUnclaimWhitelistMaterials", StringMgmt.join(world.getRevertOnUnclaimWhitelistMaterials(), "#"));

			// Using PlotManagement Wild Regen
			nat_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				nat_hm.put("PlotManagementWildRegenEntities", StringMgmt.join(BukkitTools.convertKeyedToString(world.getPlotManagementWildRevertEntities()), "#"));

			// Wilderness Explosion Protection Block Whitelist
			if (world.getPlotManagementWildRevertBlockWhitelist() != null)
				nat_hm.put("PlotManagementWildRegenBlockWhitelist",
						StringMgmt.join(world.getPlotManagementWildRevertBlockWhitelist(), "#"));

			// Wilderness Explosion Protection Materials to not overwrite.
			if (world.getWildRevertMaterialsToNotOverwrite() != null)
				nat_hm.put("wildRegenBlocksToNotOverwrite",
						StringMgmt.join(world.getWildRevertMaterialsToNotOverwrite(), "#"));

			// Using PlotManagement Wild Regen Delay
			nat_hm.put("plotManagementWildRegenSpeed", world.getPlotManagementWildRevertDelay());
			
			// Using PlotManagement Wild Block Regen
			nat_hm.put("usingPlotManagementWildRegenBlocks", world.isUsingPlotManagementWildBlockRevert());

			// Wilderness Explosion Protection blocks
			if (world.getPlotManagementWildRevertBlocks() != null)
				nat_hm.put("PlotManagementWildRegenBlocks",
						StringMgmt.join(world.getPlotManagementWildRevertBlocks(), "#"));

			// Using Towny
			nat_hm.put("usingTowny", world.isUsingTowny());

			// War allowed in this world.
			nat_hm.put("warAllowed", world.isWarAllowed());

			nat_hm.put("jailing", world.isJailingEnabled());

			if (world.hasMeta())
				nat_hm.put("metadata", serializeMetadata(world));
			else
				nat_hm.put("metadata", "");

			updateDB("WORLDS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save world unknown error (" + world.getName() + ")", e);
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock) {

		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x"
				+ townBlock.getZ());
		try {
			HashMap<String, Object> tb_hm = new HashMap<>();
			tb_hm.put("world", townBlock.getWorld().getName());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());
			tb_hm.put("name", townBlock.getName());
			tb_hm.put("price", townBlock.getPlotPrice());
			tb_hm.put("taxed", townBlock.isTaxed());
			tb_hm.put("town", townBlock.getTown().getName());
			tb_hm.put("resident", (townBlock.hasResident()) ? townBlock.getResidentOrNull().getName() : "");
			tb_hm.put("typeName", townBlock.getTypeName());
			tb_hm.put("outpost", townBlock.isOutpost());
			tb_hm.put("permissions",
					(townBlock.isChanged()) ? townBlock.getPermissions().toString().replaceAll(",", "#") : "");
			tb_hm.put("changed", townBlock.isChanged());
			tb_hm.put("claimedAt", townBlock.getClaimedAt());
			tb_hm.put("minTownMembershipDays", townBlock.getMinTownMembershipDays());
			tb_hm.put("maxTownMembershipDays", townBlock.getMaxTownMembershipDays());
			if (townBlock.hasPlotObjectGroup())
				tb_hm.put("groupID", townBlock.getPlotObjectGroup().getUUID().toString());
			else
				tb_hm.put("groupID", "");
			if (townBlock.hasDistrict())
				tb_hm.put("districtID", townBlock.getDistrict().getUUID().toString());
			else
				tb_hm.put("districtID", "");
			if (townBlock.hasMeta())
				tb_hm.put("metadata", serializeMetadata(townBlock));
			else
				tb_hm.put("metadata", "");
			
			tb_hm.put("trustedResidents", StringMgmt.join(toUUIDList(townBlock.getTrustedResidents()), "#"));

			Map<String, String> stringMap = new HashMap<>();
			for (Map.Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
				stringMap.put(entry.getKey().getUUID().toString(), entry.getValue().toString());
			}
			
			tb_hm.put("customPermissionData", new Gson().toJson(stringMap));

			updateDB("TOWNBLOCKS", tb_hm, Arrays.asList("world", "x", "z"));

		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save TownBlock unknown error", e);
		}
		return true;
	}
	
	@Override
	public synchronized boolean saveJail(Jail jail) {

		TownyMessaging.sendDebugMsg("Saving jail " + jail.getUUID());
		
		try {
			HashMap<String, Object> jail_hm = new HashMap<>();
			jail_hm.put("uuid", jail.getUUID());
			jail_hm.put("townBlock", jail.getTownBlock().getWorld().getName() + "#" + jail.getTownBlock().getX() + "#" + jail.getTownBlock().getZ());
			
			StringBuilder jailCellArray = new StringBuilder();
			if (jail.hasCells())
				for (Position cell : jail.getJailCellPositions()) {
					jailCellArray.append(String.join("#", cell.serialize())).append(";");
				}
			
			jail_hm.put("spawns", jailCellArray);
			
			updateDB("JAILS", jail_hm, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "SQL: Save jail unknown error", e);
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
