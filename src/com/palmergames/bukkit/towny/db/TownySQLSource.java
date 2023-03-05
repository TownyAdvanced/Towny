/*
 * Towny MYSQL Source by StPinker
 *
 * Released under LGPL
 */
package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

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
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class TownySQLSource extends TownyDatabaseHandler {

	private final Queue<SQL_Task> queryQueue = new ConcurrentLinkedQueue<>();
	private boolean isPolling = false;
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
			if (this.isPolling)
				return;

			this.isPolling = true;
			try {
				while (!TownySQLSource.this.queryQueue.isEmpty()) {

					final SQL_Task query = TownySQLSource.this.queryQueue.poll();
					if (query == null)
						break;

					if (query.update) {
						TownySQLSource.this.QueueUpdateDB(query.tb_name, query.args, query.keys);
					} else {
						TownySQLSource.this.QueueDeleteDB(query.tb_name, query.args);
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
	
	/*
	 * Load keys
	 */
	
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

	@Override
	public boolean loadResidentList() {

		TownyMessaging.sendDebugMsg("Loading Resident List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "RESIDENTS");

				while (rs.next()) {
					try {
						newResident(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTownList() {

		TownyMessaging.sendDebugMsg("Loading Town List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "TOWNS");

				while (rs.next()) {
					try {
						universe.newTownInternal(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: town list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: town list unknown error: ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadNationList() {

		TownyMessaging.sendDebugMsg("Loading Nation List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "NATIONS");
				while (rs.next()) {
					try {
						newNation(rs.getString("name"));
					} catch (AlreadyRegisteredException ignored) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: nation list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: nation list unknown error : ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadWorldList() {

		TownyMessaging.sendDebugMsg("Loading World List");

		// Check for any new worlds registered with bukkit.
		for (World world : Bukkit.getServer().getWorlds())
			universe.newWorld(world);

		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "WORLDS");
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
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: world list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: world list unknown error : ");
			e.printStackTrace();
		}

		return true;
	}
	
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg("Loading PlotGroup List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT groupID FROM " + tb_prefix + "PLOTGROUPS");

				while (rs.next()) {
					universe.newPlotGroupInternal(UUID.fromString(rs.getString("groupID")));
				}
			}
			
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean loadJailList() {
		TownyMessaging.sendDebugMsg("Loading Jail List");
		if (!getContext())
			return false;
		try {
			try (Statement s = cntx.createStatement()) {
				ResultSet rs = s.executeQuery("SELECT uuid FROM " + tb_prefix + "JAILS");
				while (rs.next()) {
					universe.newJailInternal(rs.getString("uuid"));
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: jail list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: jail list unknown error : ");
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		TownySettings.setUUIDCount(0);

		if (!getContext())
			return false;
		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "RESIDENTS")) {

			while (rs.next()) {
				String residentName;
				try {
					residentName = rs.getString("name");
				} catch (SQLException ex) {
					plugin.getLogger().severe("Loading Error: Error fetching a resident name from SQL Database. Skipping loading resident..");
					ex.printStackTrace();
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
				
				if (resident.hasUUID())
					TownySettings.incrementUUIDCount();

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
								} catch (NotRegisteredException nre) {}
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
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				resident.setLastOnline(rs.getLong("lastOnline"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setRegistered(rs.getLong("registered"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setJoinedTownAt(rs.getLong("joinedTownAt"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setNPC(rs.getBoolean("isNPC"));
			} catch (Exception e) {
				e.printStackTrace();
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
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					if (rs.getString("jailHours") != null && !rs.getString("jailHours").isEmpty())
						resident.setJailHours(rs.getInt("jailHours"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					if (rs.getString("jailBail") != null && !rs.getString("jailBail").isEmpty())
						resident.setJailBailCost(rs.getDouble("jailBail"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			String line;
			try {
				line = rs.getString("friends");
				if (line != null) {
					search = (line.contains("#")) ? "#" : ",";
					List<Resident> friends = TownyAPI.getInstance().getResidents(line.split(search));
					for (Resident friend : friends) {
						try {
							resident.addFriend(friend);
						} catch (AlreadyRegisteredException ignored) {}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				resident.setPermissions(rs.getString("protectionStatus").replaceAll("#", ","));
			} catch (Exception e) {
				e.printStackTrace();
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
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						resident.setSurname(rs.getString("surname"));
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						line = rs.getString("town-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setTownRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception e) {
					}

					try {
						line = rs.getString("nation-ranks");
						if ((line != null) && (!line.isEmpty())) {
							search = (line.contains("#")) ? "#" : ",";
							resident.setNationRanks(Arrays.asList((line.split(search))));
						}
					} catch (Exception e) {
					}
				}
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTowns() {
		TownyMessaging.sendDebugMsg("Loading Towns");
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "TOWNS ")) {
			while (rs.next()) {
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
			town.setAllowedToWar(rs.getBoolean("allowedToWar"));
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
			
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " sql Error - " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town " + name + " unknown Error - ");
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean loadNations() {
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "NATIONS")) {
			while (rs.next()) {
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

			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " SQL Error - " + e.getMessage());
		} catch (TownyException ex) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation " + name + " unknown Error - ");
			ex.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean loadWorlds() {
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "WORLDS")) {

			while (rs.next()) {
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
		if (!getContext())
			return false;

		try (PreparedStatement ps = cntx.prepareStatement("SELECT * FROM " + tb_prefix + "WORLDS WHERE name=?")) {
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
					// Invalid uuid
				}
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

	@Override
	public boolean loadTownBlocks() {

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
					for (Resident resident : TownyAPI.getInstance().getResidents(toUUIDArray(line.split(search))))
						townBlock.addTrustedResident(resident);

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
	
	@Override
	public boolean loadPlotGroups() {
		TownyMessaging.sendDebugMsg("Loading plot groups.");
		if (!getContext())
			return false;
		try (Statement s = cntx.createStatement();
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
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading plot group: " + uuid
			+ " at line: " + line + " in the sql database");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean loadPlotGroup(PlotGroup group) {
		// Unused in SQL.
		return true;
	}

	@Override
	public boolean loadJails() {
		TownyMessaging.sendDebugMsg("Loading Jails");
		if (!getContext())
			return false;

		try (Statement s = cntx.createStatement();
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
			res_hm.put("town", resident.hasTown() ? resident.getTown().getName() : "");
			res_hm.put("town-ranks", resident.hasTown() ? StringMgmt.join(resident.getTownRanks(), "#") : "");
			res_hm.put("nation-ranks", resident.hasTown() ? StringMgmt.join(resident.getNationRanks(), "#") : "");
			res_hm.put("friends", StringMgmt.join(resident.getFriends(), "#"));
			res_hm.put("protectionStatus", resident.getPermissions().toString().replaceAll(",", "#"));

			if (resident.hasMeta())
				res_hm.put("metadata", serializeMetadata(resident));
			else
				res_hm.put("metadata", "");

			UpdateDB("RESIDENTS", res_hm, Collections.singletonList("name"));
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

			UpdateDB("HIBERNATEDRESIDENTS", res_hm, Collections.singletonList("uuid"));
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
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("maxPercentTaxAmount", town.getMaxPercentTaxAmount());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("conquered", town.isConquered());
			twn_hm.put("conqueredDays", town.getConqueredDays());
			twn_hm.put("admindisabledpvp", town.isAdminDisabledPVP());
			twn_hm.put("adminenabledpvp", town.isAdminEnabledPVP());
			twn_hm.put("allowedToWar", town.isAllowedToWar());
			twn_hm.put("joinedNationAt", town.getJoinedNationAt());
			twn_hm.put("mapColorHexCode", town.getMapColorHexCode());
			twn_hm.put("movedHomeBlockAt", town.getMovedHomeBlockAt());
			if (town.hasMeta())
				twn_hm.put("metadata", serializeMetadata(town));
			else
				twn_hm.put("metadata", "");

			twn_hm.put("homeblock",
					town.hasHomeBlock()
							? town.getHomeBlock().getWorld().getName() + "#" + town.getHomeBlock().getX() + "#"
									+ town.getHomeBlock().getZ()
							: "");
			twn_hm.put("spawn",
					town.hasSpawn()
							? town.getSpawn().getWorld().getName() + "#" + town.getSpawn().getX() + "#"
									+ town.getSpawn().getY() + "#" + town.getSpawn().getZ() + "#"
									+ town.getSpawn().getPitch() + "#" + town.getSpawn().getYaw()
							: "");
			// Outpost Spawns
			StringBuilder outpostArray = new StringBuilder();
			if (town.hasOutpostSpawn())
				for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns())) {
					outpostArray.append(spawn.getWorld().getName()).append("#").append(spawn.getX()).append("#")
							.append(spawn.getY()).append("#").append(spawn.getZ()).append("#").append(spawn.getPitch())
							.append("#").append(spawn.getYaw()).append(";");
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
			
			UpdateDB("TOWNS", twn_hm, Collections.singletonList("name"));
			return true;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Town unknown error");
			e.printStackTrace();
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
			pltgrp_hm.put("town", group.getTown().toString());

			UpdateDB("PLOTGROUPS", pltgrp_hm, Collections.singletonList("groupID"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Plot groups unknown error");
			e.printStackTrace();
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
			nat_hm.put("nationSpawn",
					nation.hasSpawn()
							? nation.getSpawn().getWorld().getName() + "#" + nation.getSpawn().getX() + "#"
									+ nation.getSpawn().getY() + "#" + nation.getSpawn().getZ() + "#"
									+ nation.getSpawn().getPitch() + "#" + nation.getSpawn().getYaw()
							: "");
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

			UpdateDB("NATIONS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Nation unknown error");
			e.printStackTrace();
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
				nat_hm.put("unclaimDeleteEntityTypes", StringMgmt.join(world.getUnclaimDeleteEntityTypes(), "#"));

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

			// Using PlotManagement Wild Regen
			nat_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				nat_hm.put("PlotManagementWildRegenEntities",
						StringMgmt.join(world.getPlotManagementWildRevertEntities(), "#"));

			// Wilderness Explosion Protection Block Whitelist
			if (world.getPlotManagementWildRevertBlockWhitelist() != null)
				nat_hm.put("PlotManagementWildRegenBlockWhitelist",
						StringMgmt.join(world.getPlotManagementWildRevertBlockWhitelist(), "#"));

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

			if (world.hasMeta())
				nat_hm.put("metadata", serializeMetadata(world));
			else
				nat_hm.put("metadata", "");

			UpdateDB("WORLDS", nat_hm, Collections.singletonList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save world unknown error (" + world.getName() + ")");
			e.printStackTrace();
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
			tb_hm.put("town", townBlock.getTown().getName());
			tb_hm.put("resident", (townBlock.hasResident()) ? townBlock.getResidentOrNull().getName() : "");
			tb_hm.put("typeName", townBlock.getTypeName());
			tb_hm.put("outpost", townBlock.isOutpost());
			tb_hm.put("permissions",
					(townBlock.isChanged()) ? townBlock.getPermissions().toString().replaceAll(",", "#") : "");
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());
			tb_hm.put("claimedAt", townBlock.getClaimedAt());
			if (townBlock.hasPlotObjectGroup())
				tb_hm.put("groupID", townBlock.getPlotObjectGroup().getUUID().toString());
			else
				tb_hm.put("groupID", "");
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

			UpdateDB("TOWNBLOCKS", tb_hm, Arrays.asList("world", "x", "z"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save TownBlock unknown error");
			e.printStackTrace();
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
				for (Location cell : new ArrayList<>(jail.getJailCellLocations())) {
					jailCellArray.append(cell.getWorld().getName()).append("#").append(cell.getX()).append("#")
							.append(cell.getY()).append("#").append(cell.getZ()).append("#").append(cell.getPitch())
							.append("#").append(cell.getYaw()).append(";");
				}
			
			jail_hm.put("spawns", jailCellArray);
			
			UpdateDB("JAILS", jail_hm, Collections.singletonList("uuid"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save jail unknown error");
			e.printStackTrace();
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
	public void deleteJail(Jail jail) {
		
		HashMap<String, Object> jail_hm = new HashMap<>();
		jail_hm.put("uuid", jail.getUUID());
		DeleteDB("JAILS", jail_hm);
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
