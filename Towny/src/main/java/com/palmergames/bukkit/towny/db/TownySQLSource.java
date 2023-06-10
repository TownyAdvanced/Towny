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
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.util.FileMgmt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.World;

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
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class TownySQLSource extends TownyDatabaseHandler {

	private final Queue<SQLTask> queryQueue = new ConcurrentLinkedQueue<>();
	private boolean isPolling = false;
	private ScheduledTask task = null;

	private final String dsn;
		private final String username;
	private final String password;
	private final String tb_prefix;

	private Connection cntx = null;

	private final HikariConfig config;
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
		
		this.dsn = ("jdbc:mysql://" + TownySettings.getSQLHostName() + ":" + TownySettings.getSQLPort() + "/" + TownySettings.getSQLDBName() + TownySettings.getSQLFlags());
		this.config = new HikariConfig();
		
		config.setPoolName("Towny MySQL");
		config.setJdbcUrl(this.dsn);

		username = TownySettings.getSQLUsername();
		password = TownySettings.getSQLPassword();

		config.setUsername(username);
		config.setPassword(password);

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

		this.hikariDataSource = new HikariDataSource(config);

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

		/*
		 * Attempt to get a connection to the database
		 */
		if (getContext()) {

			TownyMessaging.sendDebugMsg("Connected to Database");

		} else {

			TownyMessaging.sendErrorMsg("Failed when connecting to Database");
			return;

		}

		/*
		 * Initialise database Schema.
		 */
		SQLSchema.initTables(cntx);

		/*
		 * Start our Async queue for pushing data to the database.
		 */
		task = plugin.getScheduler().runAsyncRepeating(() -> {
			if (this.isPolling)
				return;

			this.isPolling = true;
			try {
				while (!TownySQLSource.this.queryQueue.isEmpty()) {

					final SQLTask query = TownySQLSource.this.queryQueue.poll();
					if (query == null)
						break;

					if (query.update) {
						TownySQLSource.this.queueUpdateDB(query.tb_name, query.args, query.keys);
					} else {
						TownySQLSource.this.queueDeleteDB(query.tb_name, query.args);
					}

				}
			} finally {
				this.isPolling = false;
			}

		}, 5L, 5L);
	}

	@Override
	public void finishTasks() {
		// Cancel the repeating task as its not needed anymore.
		if (task != null)
			task.cancel();

		// Make sure that *all* tasks are saved before shutting down.
		while (!queryQueue.isEmpty()) {
			SQLTask query = TownySQLSource.this.queryQueue.poll();

			if (query.update) {
				TownySQLSource.this.queueUpdateDB(query.tb_name, query.args, query.keys);
			} else {
				TownySQLSource.this.queueDeleteDB(query.tb_name, query.args);
			}
		}
		// Close the database sources on shutdown to get GC
		if (hikariDataSource != null)
			hikariDataSource.close();
	}

	/**
	 * open a connection to the SQL server.
	 *
	 * @return true if we successfully connected to the db.
	 */
	public boolean getContext() {

		try {
			if (cntx == null || cntx.isClosed() || !cntx.isValid(1)) {

				if (cntx != null && !cntx.isClosed()) {

					try {

						cntx.close();

					} catch (SQLException e) {
						/*
						 * We're disposing of an old stale connection just be nice to the GC as well as
						 * mysql, so ignore the error as there's nothing we can do if it fails
						 */
					}
					cntx = null;
				}

				cntx = hikariDataSource.getConnection();

				return cntx != null && !cntx.isClosed();
			}

			return true;

		} catch (SQLException e) {
			Towny.getPlugin().getLogger().warning("Error could not Connect to db " + this.dsn + ": " + e.getMessage());
		}

		return false;
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

		this.queryQueue.add(new SQLTask(tb_name, args, keys));

		return true;

	}

	@SuppressWarnings("unused")
	private boolean QueueUpdateDB$$bridge$$public(String tb_name, HashMap<String, Object> args, List<String> keys) {
		return queueUpdateDB(tb_name, args, keys);
	}

	public boolean queueUpdateDB(String tb_name, Map<String, ?> args, List<String> keys) {

		/*
		 * Attempt to get a database connection.
		 */
		if (!getContext())
			return false;

		StringBuilder code;
		PreparedStatement stmt = null;
		List<Object> parameters = new ArrayList<>();
		int rs = 0;

		try {

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

			stmt = cntx.prepareStatement(code.toString());

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
	public boolean DeleteDB(String tb_name, Map<String, Object> args) {

		// Make sure we only execute queries in async

		this.queryQueue.add(new SQLTask(tb_name, args));

		return true;

	}

	@SuppressWarnings("unused")
	private boolean queueDeleteDB$$bridge$$public(String tb_name, HashMap<String, Object> args) {
		return queueDeleteDB(tb_name, args);
	}

	public boolean queueDeleteDB(String tb_name, Map<String, ?> args) {

		if (!getContext())
			return false;
		try {
			StringBuilder wherecode = new StringBuilder(
					"DELETE FROM " + tb_prefix + (tb_name.toUpperCase()) + " WHERE ");

			Iterator<? extends Map.Entry<String, ?>> i = args.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, ?> me = i.next();
				wherecode.append("`").append(me.getKey()).append("` = ");
				if (me.getValue() instanceof String)
					wherecode.append("'").append(((String) me.getValue()).replace("'", "''")).append("'");
				else if (me.getValue() instanceof Boolean)
					wherecode.append("'").append(((Boolean) me.getValue()) ? "1" : "0").append("'");
				else
					wherecode.append("'").append(me.getValue()).append("'");

				wherecode.append(i.hasNext() ? " AND " : "");
			}
			int rs;
			try (Statement statement = cntx.createStatement()) {
				rs = statement.executeUpdate(wherecode.toString());
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

		/*
		 * Attempt to get a database connection.
		 */
		if (!getContext())
			return false;

		SQLSchema.cleanup(cntx);

		return true;
	}
	
	public enum TownyDBTableType {
		JAIL("JAILS", "SELECT uuid FROM ", "uuid"),
		PLOTGROUP("PLOTGROUPS", "SELECT uuid FROM ", "uuid"),
		RESIDENT("RESIDENTS", "SELECT uuid FROM ", "uuid"),
		HIBERNATED_RESIDENT("HIBERNATEDRESIDENTS", "", "uuid"),
		TOWN("TOWNS", "SELECT uuid FROM ", "uuid"),
		NATION("NATIONS", "SELECT uuid FROM ", "uuid"),
		WORLD("WORLDS", "SELECT uuid FROM ", "uuid"),
		TOWNBLOCK("TOWNBLOCKS", "SELECT world,x,z FROM ", "name"),
		COOLDOWN("COOLDOWNS", "SELECT * FROM ", "key");
		
		private String tableName;
		private String queryString;
		private String primaryKey;

		TownyDBTableType(String tableName, String queryString, String primaryKey) {
			this.tableName = tableName;
			this.queryString = queryString;
			this.primaryKey = primaryKey;
		}
		
		public String tableName() {
			return tableName;
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

	private boolean loadResultSetListOfType(TownyDBTableType type, Consumer<UUID> consumer) {
		TownyMessaging.sendDebugMsg("Searching for " + type.tableName.toLowerCase(Locale.ROOT) + "...");
		if (!getContext())
			return false;
		
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery(type.queryString + tb_prefix + type.tableName);
				if (rs.getFetchSize() != 0)
					TownyMessaging.sendDebugMsg("Loading " + rs.getFetchSize() + " entries from the " + type.tableName + " table...");

				while (rs.next())
					consumer.accept(UUID.fromString(rs.getString(type.primaryKey)));
			}
			
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		return false;
	}

	private boolean loadResultSetOfType(TownyDBTableType type, Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		for (UUID uuid : uuids)
			if (!loadResultSet(type, uuid))
				throw new ObjectCouldNotBeLoadedException(type.getLoadErrorMsg(uuid));
		return true;
	}

	private boolean loadResultSet(TownyDBTableType type, UUID uuid) {
		return switch (type) {
		case JAIL -> loadJailData(uuid);
		case NATION -> loadNationData(uuid);
		case PLOTGROUP -> loadPlotGroupData(uuid);
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


	@Override
	public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			if (!getContext())
				return Optional.empty();
			
			try (Statement statement = cntx.createStatement()) {
				ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tb_prefix + "HIBERNATEDRESIDENTS WHERE uuid = '" + uuid + "' LIMIT 1");
				
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

	/*
	 * Load keys
	 */

	@Override
	public boolean loadJailList() {
		return loadResultSetListOfType(TownyDBTableType.JAIL, uuid -> universe.newJailInternal(uuid));
	}

	@Override
	public boolean loadPlotGroupList() {
		return loadResultSetListOfType(TownyDBTableType.RESIDENT, uuid -> universe.newPlotGroupInternal(uuid));
	}
	
	@Override
	public boolean loadResidentList() {
		return loadResultSetListOfType(TownyDBTableType.RESIDENT, uuid -> universe.newResidentInternal(uuid));
	}

	@Override
	public boolean loadTownList() {
		return loadResultSetListOfType(TownyDBTableType.TOWN, uuid -> universe.newTownInternal(uuid));
	}

	@Override
	public boolean loadNationList() {
		return loadResultSetListOfType(TownyDBTableType.NATION, uuid -> universe.newNationInternal(uuid));
	}
	
	@Override
	public boolean loadWorldList() {
		loadResultSetListOfType(TownyDBTableType.WORLD, uuid -> universe.newWorldInternal(uuid));
		for (World world : plugin.getServer().getWorlds()) {
			if (universe.getWorldIDMap().containsKey(world.getUID()))
				continue;

			// Register and create rows for any worlds which did not have files yet.
			TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
			universe.registerTownyWorld(townyWorld);
			try {
				queueUpdateDB("WORLDS", ObjectSaveUtil.getWorldMap(townyWorld), null);
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
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs;
				int total = 0;
				rs = s.executeQuery("SELECT world,x,z FROM " + tb_prefix + "TOWNBLOCKS");
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

			}
			
			return true;
		} catch (SQLException s) {
			s.printStackTrace();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		return false;

	}

	/*
	 * Load individual Towny object-callers
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
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "JAILS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find jail with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getPlotGroupMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT groupID FROM " + tb_prefix + "PLOTGROUPS WHERE groupID='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find plotgroup with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getResidentMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "RESIDENTS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find resident with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getTownMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "TOWNS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find town with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getNationMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "NATIONS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find nation with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getWorldMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "WORLDS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unable to find world with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public Map<String, String> getTownBlockMap(TownBlock townBlock) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
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
			updateDB("PLOTGROUPS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Plot groups unknown error");
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
			TownyMessaging.sendErrorMsg("SQL: Save world unknown error (" + world.getName() + ")");
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

	/*
	 * Delete objects
	 */

	private void deleteRowOfColumnAndUUID(TownyDBTableType type, UUID uuid) {
		deleteRowOfColumnAndName(type, String.valueOf(uuid));
	}
	
	private void deleteRowOfColumnAndName(TownyDBTableType type, String name) {
		Map<String, Object> hm = new HashMap<>();
		hm.put(type.primaryKey, name);
		DeleteDB(type.tableName, hm);
	}

	@Override
	public void deleteJail(Jail jail) {
		deleteRowOfColumnAndUUID(TownyDBTableType.JAIL, jail.getUUID());
	}

	@Override
	public void deletePlotGroup(PlotGroup group) {
		deleteRowOfColumnAndUUID(TownyDBTableType.PLOTGROUP, group.getUUID());
	}

	@Override
	public void deleteResident(Resident resident) {
		deleteRowOfColumnAndUUID(TownyDBTableType.RESIDENT, resident.getUUID());
	}

	@Override 
	public void deleteHibernatedResident(UUID uuid) {
		deleteRowOfColumnAndUUID(TownyDBTableType.HIBERNATED_RESIDENT, uuid);
	}
	
	@Override
	public void deleteTown(Town town) {
		deleteRowOfColumnAndUUID(TownyDBTableType.TOWN, town.getUUID());
	}

	@Override
	public void deleteNation(Nation nation) {
		deleteRowOfColumnAndUUID(TownyDBTableType.NATION, nation.getUUID());
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		deleteRowOfColumnAndUUID(TownyDBTableType.WORLD, world.getUUID());
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {
		Map<String, Object> twn_hm = new HashMap<>();
		twn_hm.put("world", townBlock.getWorld().getUUID());
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("TOWNBLOCKS", twn_hm);
	}

	@Override
	public boolean loadCooldowns() {
		if (!getContext())
			return false;
		
		try (PreparedStatement statement = cntx.prepareStatement("SELECT * FROM " + tb_prefix + TownyDBTableType.COOLDOWN.tableName()); 
		     ResultSet resultSet = statement.executeQuery()) {
			
			while (resultSet.next())
				CooldownTimerTask.getCooldowns().put(resultSet.getString("key"), resultSet.getLong("expiry"));
		} catch (SQLException e) {
			logger.warn("An exception occurred when loading cooldowns", e);
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
}
