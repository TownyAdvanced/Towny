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
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.scheduler.BukkitTask;

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

	private final Queue<SQL_Task> queryQueue = new ConcurrentLinkedQueue<>();
	private BukkitTask task = null;

	private final String dsn;
	private final String db_name;
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
		db_name = TownySettings.getSQLDBName();
		tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();
		
		this.dsn = ("jdbc:mysql://" + TownySettings.getSQLHostName() + ":" + TownySettings.getSQLPort() + "/" + db_name + TownySettings.getSQLFlags());
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
		SQL_Schema.initTables(cntx, db_name);

		/*
		 * Start our Async queue for pushing data to the database.
		 */
		task = BukkitTools.getScheduler().runTaskTimerAsynchronously(plugin, () -> {

			while (!TownySQLSource.this.queryQueue.isEmpty()) {

				SQL_Task query = TownySQLSource.this.queryQueue.poll();

				if (query.update) {
					TownySQLSource.this.QueueUpdateDB(query.tb_name, query.args, query.keys);
				} else {
					TownySQLSource.this.QueueDeleteDB(query.tb_name, query.args);
				}

			}

		}, 5L, 5L);
	}

	@Override
	public void finishTasks() {
		// Cancel the repeating task as its not needed anymore.
		task.cancel();

		// Make sure that *all* tasks are saved before shutting down.
		while (!queryQueue.isEmpty()) {
			SQL_Task query = TownySQLSource.this.queryQueue.poll();

			if (query.update) {
				TownySQLSource.this.QueueUpdateDB(query.tb_name, query.args, query.keys);
			} else {
				TownySQLSource.this.QueueDeleteDB(query.tb_name, query.args);
			}
		}
		// Close the database sources on shutdown to get GC
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
			TownyMessaging.sendErrorMsg("Error could not Connect to db " + this.dsn + ": " + e.getMessage());
		}

		return false;
	}

	/**
	 * Build the SQL string and execute to INSERT/UPDATE
	 *
	 * @param tb_name - Database Table name.
	 * @param args    - Arguments.
	 * @param keys    - Table keys.
	 * @return true if the update was successful.
	 */
	public boolean UpdateDB(String tb_name, HashMap<String, Object> args, List<String> keys) {

		/*
		 * Make sure we only execute queries in async
		 */

		this.queryQueue.add(new SQL_Task(tb_name, args, keys));

		return true;

	}

	public boolean QueueUpdateDB(String tb_name, HashMap<String, Object> args, List<String> keys) {

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

			TownyMessaging.sendErrorMsg("SQL: " + e.getMessage() + " --> " + stmt.toString());

		} finally {

			try {

				if (stmt != null) {
					stmt.close();
				}

				if (rs == 0) // if entry doesn't exist then try to insert
					return UpdateDB(tb_name, args, null);

			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("SQL closing: " + e.getMessage() + " --> " + stmt.toString());
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
	public boolean DeleteDB(String tb_name, HashMap<String, Object> args) {

		// Make sure we only execute queries in async

		this.queryQueue.add(new SQL_Task(tb_name, args));

		return true;

	}

	public boolean QueueDeleteDB(String tb_name, HashMap<String, Object> args) {

		if (!getContext())
			return false;
		try {
			StringBuilder wherecode = new StringBuilder(
					"DELETE FROM " + tb_prefix + (tb_name.toUpperCase()) + " WHERE ");
			Set<Map.Entry<String, Object>> set = args.entrySet();
			Iterator<Map.Entry<String, Object>> i = set.iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> me = i.next();
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
			TownyMessaging.sendErrorMsg("SQL: Error delete : " + e.getMessage());
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

		SQL_Schema.cleanup(cntx, db_name);

		return true;
	}
	
	public enum TownyDBTableType {
		ALLIANCE("ALLIANCES", "SELECT uuid FROM ", "uuid"),
		NATION("NATIONS", "SELECT name FROM ", "name"),
		TOWN("TOWNS", "SELECT name FROM ", "name"),
		RESIDENT("RESIDENTS", "SELECT name FROM ", "name"),
		HIBERNATED_RESIDENT("HIBERNATEDRESIDENTS", "", "uuid"),
		JAIL("JAILS", "SELECT uuid FROM ", "uuid"),
		WORLD("WORLDS", "SELECT name FROM ", "name"),
		TOWNBLOCK("TOWNBLOCKS", "SELECT world,x,z FROM ", "name"),
		PLOTGROUP("PLOTGROUPS", "SELECT groupID FROM ", "groupID");
		
		private String tableName;
		private String queryString;
		private String primaryKey;

		TownyDBTableType(String tableName, String queryString, String primaryKey) {
			this.tableName = tableName;
			this.queryString = queryString;
			this.primaryKey = primaryKey;
		}
		
		private String getSingular() {
			// Hibernated Residents are never loaded so this method is never called on them.
			return tableName.substring(tableName.length()-1).toLowerCase(Locale.ROOT);
		}
		
		public String getSaveLocation(String rowKeyName) {
			return TownySettings.getSQLTablePrefix() + tableName + File.separator + rowKeyName;
		}
		
		public String getLoadErrorMsg(UUID uuid) {
			return "Loading Error: Could not read the " + getSingular() + " with UUID '" + uuid + "' from the " + tableName + " table.";
		}
	}
	
	public boolean loadResultSetListOfType(TownyDBTableType type, Consumer<UUID> consumer) {
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
	
	public boolean loadResultSetOfType(TownyDBTableType type, Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
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

	private HashMap<String, String> loadResultSetIntoHashMap(ResultSet rs) throws SQLException {
		HashMap<String, String> keys = new HashMap<>();
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		for (int i = 1; i <= columns; ++i) {
			keys.put(md.getColumnName(i), rs.getString(i));
		}
		return keys;
	}
	
	public String getNameOfObject(String type, UUID uuid) {
		if (!getContext())
			return "";
		
		TownyDBTableType tableType = TownyDBTableType.valueOf(type.toUpperCase(Locale.ROOT));
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + tableType.tableName + " WHERE uuid='" + uuid + "'");
				while (rs.next())
					return rs.getString("name");
			}
		} catch (SQLException s) {
			s.printStackTrace();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		return null;
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
		return loadResultSetListOfType(TownyDBTableType.WORLD, uuid -> universe.newWorldInternal(uuid));
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
				throw new ObjectCouldNotBeLoadedException("The Townblock: '" + townblock.toString() + "' could not be read from the database!");
		return true;
	}

	/*
	 * Methods that return objects as HashMaps for loading.
	 */

	@Override
	public HashMap<String, String> getJailMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "JAILS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find jail with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getPlotGroupMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT groupID FROM " + tb_prefix + "PLOTGROUPS WHERE groupID='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find plotgroup with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getResidentMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "RESIDENTS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find resident with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getTownMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "TOWNS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find town with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getNationMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "NATIONS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find nation with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getWorldMap(UUID uuid) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "WORLDS WHERE uuid='" + uuid + "'")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Unabled to find nation with UUID " + uuid.toString() + " in the database!");
			return null;
		}
	}

	@Override
	public HashMap<String, String> getTownBlockMap(TownBlock townBlock) {
		if (!getContext())
			return null;
		try (Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNBLOCKS")) {
			return loadResultSetIntoHashMap(rs);
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock: "
					+ (townBlock != null ? townBlock : "NULL") + " in the sql database");
			return null;
		}
	}
/*	
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
			town.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			town.setBonusBlocks(rs.getInt("bonus"));
			town.setManualTownLevel(rs.getInt("manualTownLevel"));
			town.setTaxPercentage(rs.getBoolean("taxpercent"));
			town.setTaxes(rs.getFloat("taxes"));
			town.setMaxPercentTaxAmount(rs.getFloat("maxPercentTaxAmount"));
			town.setHasUpkeep(rs.getBoolean("hasUpkeep"));
			town.setHasUnlimitedClaims(rs.getBoolean("hasUnlimitedClaims"));
			town.setPlotPrice(rs.getFloat("plotPrice"));
			town.setPlotTax(rs.getFloat("plotTax"));
			town.setEmbassyPlotPrice(rs.getFloat("embassyPlotPrice"));
			town.setEmbassyPlotTax(rs.getFloat("embassyPlotTax"));
			town.setCommercialPlotPrice(rs.getFloat("commercialPlotPrice"));
			town.setCommercialPlotTax(rs.getFloat("commercialPlotTax"));
			town.setSpawnCost(rs.getFloat("spawnCost"));
			town.setOpen(rs.getBoolean("open"));
			town.setPublic(rs.getBoolean("public"));
			town.setConquered(rs.getBoolean("conquered"));
			town.setAdminDisabledPVP(rs.getBoolean("admindisabledpvp"));
			town.setAdminEnabledPVP(rs.getBoolean("adminenabledpvp"));
			town.setJoinedNationAt(rs.getLong("joinedNationAt"));
			town.setMovedHomeBlockAt(rs.getLong("movedHomeBlockAt"));

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
						World world = plugin.getServerWorld(tokens[0]);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double z = Double.parseDouble(tokens[3]);

						Location loc = new Location(world, x, y, z);
						if (tokens.length == 6) {
							loc.setPitch(Float.parseFloat(tokens[4]));
							loc.setYaw(Float.parseFloat(tokens[5]));
						}
						town.setSpawn(loc);
					} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
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
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);

							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							town.forceAddOutpostSpawn(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
			}
			// Load legacy jail spawns into new Jail objects.
			line = rs.getString("jailSpawns");
			if (line != null) {
				String[] jails = line.split(";");
				for (String spawn : jails) {
					search = (line.contains("#")) ? "#" : ",";
					tokens = spawn.split(search);
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);

							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}

							TownBlock tb = universe.getTownBlock(WorldCoord.parseWorldCoord(loc));
							if (tb == null)
								continue;
							Jail jail = new Jail(UUID.randomUUID(), town, tb, new ArrayList<>(Collections.singleton(loc)));
							universe.registerJail(jail);
							town.addJail(jail);
							tb.setJail(jail);
							jail.save();
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
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
			
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " sql Error - " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " unknown Error - ");
			e.printStackTrace();
		}

		return false;
	}
*/
/*
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
					removeNation(nation);
					return true;
				}
			}
			else {
				TownyMessaging.sendDebugMsg("Nation " + name + " could not set capital to " + rs.getString("capital") + ", selecting a new capital...");
				if (!nation.findNewCapital()) {
					plugin.getLogger().warning("The nation " + nation.getName() + " could not load a capital city and is being disbanded.");
					removeNation(nation);
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

			nation.setTaxes(rs.getDouble("taxes"));
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
						World world = plugin.getServerWorld(tokens[0]);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double z = Double.parseDouble(tokens[3]);

						Location loc = new Location(world, x, y, z);
						if (tokens.length == 6) {
							loc.setPitch(Float.parseFloat(tokens[4]));
							loc.setYaw(Float.parseFloat(tokens[5]));
						}
						nation.setSpawn(loc);
					} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
					}
			}

			nation.setPublic(rs.getBoolean("isPublic"));

			nation.setOpen(rs.getBoolean("isOpen"));

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

			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " SQL Error - " + e.getMessage());
		} catch (TownyException ex) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " unknown Error - ");
			ex.printStackTrace();
		}

		return false;
	}
*/
/*
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
*/
/*	public boolean loadTownBlocks() {

		String line = "";
		boolean result;
		TownyMessaging.sendDebugMsg("Loading Town Blocks.");

		// Load town blocks
		if (!getContext())
			return false;

		TownBlock townBlock = null;
		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNBLOCKS")) {

			while (rs.next()) {
				String worldName = rs.getString("world");
				int x = rs.getInt("x");
				int z = rs.getInt("z");

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
							+ " , deleting " + townBlock.getWorld().getName() + "," + townBlock.getX() + ","
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

				line = rs.getString("type");
				if (line != null)
					townBlock.setType(TownBlockTypeHandler.getTypeInternal(line));

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

				line = rs.getString("price");
				if (line != null)
					try {
						townBlock.setPlotPrice(Float.parseFloat(line.trim()));
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

				result = rs.getBoolean("locked");
				try {
					townBlock.setLocked(result);
				} catch (Exception ignored) {
				}

				townBlock.setClaimedAt(rs.getLong("claimedAt"));

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

				line = rs.getString("trustedResidents");
				if (line != null && !line.isEmpty() && townBlock.getTrustedResidents().isEmpty()) {
					String search = (line.contains("#")) ? "#" : ",";
					townBlock.addTrustedResidents(TownyAPI.getInstance().getResidents(ObjectLoadUtil.toUUIDArray(line.split(search))));

					if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getTrustedResidents().isEmpty() && townBlock.getTrustedResidents().size() > 0)
						townBlock.getPlotObjectGroup().setTrustedResidents(townBlock.getTrustedResidents());
				}
				
				line = rs.getString("customPermissionData");
				if (line != null && !line.isEmpty() && townBlock.getPermissionOverrides().isEmpty()) {
					Map<String, String> map = new Gson().fromJson(line, Map.class);

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
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock: "
					+ (townBlock != null ? townBlock : "NULL") + " at line: " + line + " in the sql database");
			ex.printStackTrace();
			return false;
		}

		return true;
	}
*/
/*	private boolean loadPlotGroup(ResultSet rs) {
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
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading plot group: " + uuid
			+ " at line: " + line + " in the sql database");
			e.printStackTrace();
			return false;
		}
		return true;
	}
*/
/*	private boolean loadJail(ResultSet rs) {
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
				TownBlock tb = null;
				try {
					tb = universe.getTownBlock(new WorldCoord(tokens[0], Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
					jail.setTownBlock(tb);
					jail.setTown(tb.getTown());
					tb.setJail(jail);
					tb.getTown().addJail(jail);
				} catch (NumberFormatException | NotRegisteredException e) {
					TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid townblock " + line + " deleting jail.");
					removeJail(jail);
					deleteJail(jail);
					return true;
				}
			}
			
			line = rs.getString("spawns");
			if (line != null) {
				String[] jails = line.split(";");
				for (String spawn : jails) {
					tokens = spawn.split("#");
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);
							
							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							jail.addJailCell(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException e) {
							TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid spawn " + line + " skipping.");
							continue;
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
			TownyMessaging.sendErrorMsg("SQL: Load Jail " + uuid + " unknown Error - ");
			e.printStackTrace();
		}

		return false;
	}
*/	

	/*
	 * Save individual towny objects
	 */

	@Override
	public synchronized boolean saveJail(Jail jail, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving jail " + jail.getUUID());
		try {
			UpdateDB("JAILS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save jail unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean savePlotGroup(PlotGroup group, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving group " + group.getName());
		try {
			UpdateDB("PLOTGROUPS", data, Collections.singletonList("groupID"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Plot groups unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveResident(Resident resident, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving Resident " + resident.getName());
		try {
			UpdateDB("RESIDENTS", data, Collections.singletonList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}
	
	@Override
	public synchronized boolean saveHibernatedResident(UUID uuid, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving Hibernated Resident " + uuid);
		try {
			UpdateDB("HIBERNATEDRESIDENTS", data, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Hibernated Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public synchronized boolean saveTown(Town town, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving town " + town.getName());
		try {
			UpdateDB("TOWNS", data, Collections.singletonList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Town unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			UpdateDB("NATIONS", data, Collections.singletonList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Nation unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveWorld(TownyWorld world, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving world " + world.getName());
		try {
			UpdateDB("WORLDS", data, Collections.singletonList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save world unknown error (" + world.getName() + ")");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock, HashMap<String, Object> data) {
		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x"
				+ townBlock.getZ());
		try {
			UpdateDB("TOWNBLOCKS", data, Arrays.asList("world", "x", "z"));
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
		HashMap<String, Object> hm = new HashMap<>();
		hm.put(type.primaryKey, name);
		DeleteDB(type.tableName, hm);
	}

	@Override
	public void deleteResident(Resident resident) {
		deleteRowOfColumnAndName(TownyDBTableType.RESIDENT, resident.getName());
	}

	@Override 
	public void deleteHibernatedResident(UUID uuid) {
		deleteRowOfColumnAndUUID(TownyDBTableType.HIBERNATED_RESIDENT, uuid);
	}
	
	@Override
	public void deleteTown(Town town) {
		deleteRowOfColumnAndName(TownyDBTableType.TOWN, town.getName());
	}

	@Override
	public void deleteNation(Nation nation) {
		deleteRowOfColumnAndName(TownyDBTableType.NATION, nation.getName());
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		deleteRowOfColumnAndName(TownyDBTableType.WORLD, world.getName());
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
		deleteRowOfColumnAndUUID(TownyDBTableType.PLOTGROUP, group.getUUID());
	}
	
	@Override
	public void deleteJail(Jail jail) {
		deleteRowOfColumnAndUUID(TownyDBTableType.JAIL, jail.getUUID());
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

}
