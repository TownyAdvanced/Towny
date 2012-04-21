/*
 * Towny MYSQL Source by StPinker
 * 
 * Released under LGPL
 */
package com.palmergames.bukkit.towny.db;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class TownySQLSource extends TownyFlatFileSource {

	protected String dsn = "";
	protected String hostname = "";
	protected String port = "";
	protected String db_name = "";
	protected String username = "";
	protected String password = "";
	protected String tb_prefix = "";

	private Connection cntx = null;
	private boolean ish2 = false;

	/**
	 * Flag if we are using h2 or standard SQL connectivity.
	 * 
	 * @param type
	 */
	public TownySQLSource(String type) {

		if ((type.equalsIgnoreCase("sqlite")) || (type.equalsIgnoreCase("h2")))
			this.ish2 = true;
	}

	@Override
	public void initialize(Towny plugin, TownyUniverse universe) {

		this.universe = universe;
		this.plugin = plugin;
		this.rootFolder = universe.getRootFolder();

		try {
			FileMgmt.checkFolders(new String[] {
					rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" });
			FileMgmt.checkFiles(new String[] {
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt" });
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");
		}

		//Setup SQL connection
		hostname = TownySettings.getSQLHostName();
		port = TownySettings.getSQLPort();
		db_name = TownySettings.getSQLDBName();
		tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();

		if (this.ish2) {
			this.dsn = ("jdbc:h2:" + rootFolder + dataFolder + File.separator + db_name + ";AUTO_RECONNECT=TRUE");
			username = "sa";
			password = "sa";
		} else {
			this.dsn = ("jdbc:mysql://" + hostname + ":" + port + "/" + db_name);
			username = TownySettings.getSQLUsername();
			password = TownySettings.getSQLPassword();
		}

		// Checking for db tables	
		TownyMessaging.sendDebugMsg("Checking for tables existence");
		DatabaseMetaData dbm;
		if (getContext())
			TownyMessaging.sendDebugMsg("[Towny] Connected to Database");
		else {
			TownyMessaging.sendErrorMsg("Failed when connecting to Database");
			return;
		}

		try {
			dbm = cntx.getMetaData();
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Cannot get Table metadata");
			return;
		}

		String[] types = { "TABLE" };

		try {
			ResultSet town_table = dbm.getTables(null, null, (tb_prefix + "TOWNS").toUpperCase(), types);
			if (town_table.next()) {
				TownyMessaging.sendDebugMsg("Table TOWNS is ok!");
			} else {
				String town_create = "CREATE TABLE " + tb_prefix + "TOWNS (" + "`name` VARCHAR(32) NOT NULL," + "`residents` mediumtext," + "`mayor` mediumtext," + "`nation` mediumtext NOT NULL," + "`assistants` text DEFAULT NULL," + "`townBoard` mediumtext DEFAULT NULL," + "`tag` mediumtext DEFAULT NULL," + "`protectionStatus` mediumtext DEFAULT NULL," + "`bonus` int(11) DEFAULT 0," + "`purchased` int(11)  DEFAULT 0," + "`taxpercent` bool NOT NULL DEFAULT '0'," + "`taxes` float DEFAULT 0," + "`hasUpkeep` bool NOT NULL DEFAULT '0'," + "`plotPrice` float DEFAULT NULL," + "`plotTax` float DEFAULT NULL," + "`commercialPlotPrice` float DEFAULT NULL," + "`commercialPlotTax` float NOT NULL," + "`embassyPlotPrice` float NOT NULL," + "`embassyPlotTax` float NOT NULL," + "`open` bool NOT NULL DEFAULT '0'," + "`public` bool NOT NULL DEFAULT '0'," + "`homeblock` mediumtext NOT NULL," + "`townBlocks` mediumtext NOT NULL," + "`spawn` mediumtext NOT NULL," + "`outpostSpawns` mediumtext DEFAULT NULL," + "PRIMARY KEY (`name`)" + ")";
				try {
					Statement s = cntx.createStatement();
					s.executeUpdate(town_create);
				} catch (SQLException ee) {
					TownyMessaging.sendErrorMsg("Creating table TOWNS :" + ee.getMessage());
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error Checking table TOWNS :" + e.getMessage());
		}

		try {
			ResultSet res_table = dbm.getTables(null, null, (tb_prefix + "RESIDENTS").toUpperCase(), types);
			if (res_table.next()) {
				TownyMessaging.sendDebugMsg("Table RESIDENTS is ok!");
			} else {
				String resident_create = "CREATE TABLE " + tb_prefix + "RESIDENTS (" + " `name` VARCHAR(16) NOT NULL," + "`town` mediumtext," + "`lastOnline` BIGINT NOT NULL," + "`registered` BIGINT NOT NULL," + "`isNPC` bool NOT NULL DEFAULT '0'," + "`title` mediumtext," + "`surname` mediumtext," + "`protectionStatus` mediumtext," + "`friends` mediumtext," + "`townBlocks` mediumtext," + "PRIMARY KEY (`name`)" + ")";
				try {
					Statement s = cntx.createStatement();
					s.executeUpdate(resident_create);
				} catch (SQLException ee) {
					TownyMessaging.sendErrorMsg("Error Creating table RESIDENTS :" + ee.getMessage());
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error Checking table RESIDENTS :" + e.getMessage());
		}

		try {
			ResultSet nat_table = dbm.getTables(null, null, (tb_prefix + "NATIONS").toUpperCase(), types);
			if (nat_table.next()) {
				TownyMessaging.sendDebugMsg("Table NATIONS is ok!");
			} else {
				String nation_create = "CREATE TABLE " + tb_prefix + "NATIONS (" + "`name` VARCHAR(32) NOT NULL," + "`towns` mediumtext NOT NULL," + "`capital` mediumtext NOT NULL," + "`assistants` mediumtext NOT NULL," + "`tag` mediumtext NOT NULL," + "`allies` mediumtext NOT NULL," + "`enemies` mediumtext NOT NULL," + "`taxes` float NOT NULL," + "`neutral` bool NOT NULL DEFAULT '0', " + "PRIMARY KEY (`name`)" + ")";
				try {
					Statement s = cntx.createStatement();
					s.executeUpdate(nation_create);
				} catch (SQLException ee) {
					TownyMessaging.sendErrorMsg("Error Creating table NATIONS : " + ee.getMessage());
				}
			}
		} catch (SQLException e) {
			System.out.println("[Towny] Error Checking table NATIONS :" + e.getMessage());
		}

		try {
			ResultSet tb_table = dbm.getTables(null, null, (tb_prefix + "TOWNBLOCKS").toUpperCase(), types);
			if (tb_table.next()) {
				TownyMessaging.sendDebugMsg("Table TOWNBLOCKS is ok!");
			} else {
				String townblock_create = "CREATE TABLE " + tb_prefix + "TOWNBLOCKS (" + "`world` VARCHAR(32) NOT NULL," + "`x` bigint(20) NOT NULL," + "`z` bigint(20) NOT NULL," + "`permissions` mediumtext NOT NULL," + "`locked` bool NOT NULL DEFAULT '0'," + "`changed` bool NOT NULL DEFAULT '0'," + "PRIMARY KEY (`world`,`x`,`z`)" + ")";
				try {
					Statement s = cntx.createStatement();
					s.executeUpdate(townblock_create);
				} catch (SQLException ee) {
					TownyMessaging.sendErrorMsg("Error Creating table TOWNBLOCKS : " + ee.getMessage());
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error Checking table TOWNBLOCKS :" + e.getMessage());
		}

		try {
			ResultSet tb_table = dbm.getTables(null, null, (tb_prefix + "WORLDS").toUpperCase(), types);
			if (tb_table.next()) {
				TownyMessaging.sendDebugMsg("Table WORLDS is ok!");
			} else {
				String world_create = "CREATE TABLE " + tb_prefix + "WORLDS (" + "`name` VARCHAR(32) NOT NULL," + "`towns` mediumtext NOT NULL," + "`claimable` bool NOT NULL DEFAULT '0'," + "`pvp` bool NOT NULL DEFAULT '0'," + "`forcepvp` bool NOT NULL DEFAULT '0'," + "`forcetownmobs` bool NOT NULL DEFAULT '0'," + "`worldmobs` bool NOT NULL DEFAULT '0'," + "`firespread` bool NOT NULL DEFAULT '0'," + "`forcefirespread` bool NOT NULL DEFAULT '0'," + "`explosions` bool NOT NULL DEFAULT '0'," + "`forceexplosions` bool NOT NULL DEFAULT '0'," + "`endermanprotect` bool NOT NULL DEFAULT '0'," + "`disableplayertrample` bool NOT NULL DEFAULT '0'," + "`disablecreaturetrample` bool NOT NULL DEFAULT '0'," + "`unclaimedZoneBuild` bool NOT NULL DEFAULT '0'," + "`unclaimedZoneDestroy` bool NOT NULL DEFAULT '0'," + "`unclaimedZoneSwitch` bool NOT NULL DEFAULT '0'," + "`unclaimedZoneItemUse` bool NOT NULL DEFAULT '0'," + "`unclaimedZoneName` mediumtext NOT NULL," + "`unclaimedZoneIgnoreIds` mediumtext NOT NULL," + "`usingPlotManagementDelete` bool NOT NULL DEFAULT '0'," + "`plotManagementDeleteIds` mediumtext NOT NULL," + "`usingPlotManagementMayorDelete` bool NOT NULL DEFAULT '0'," + "`plotManagementMayorDelete` mediumtext NOT NULL," + "`usingPlotManagementRevert` bool NOT NULL DEFAULT '0'," + "`plotManagementRevertSpeed` long NOT NULL," + "`plotManagementIgnoreIds` mediumtext NOT NULL," + "`usingPlotManagementWildRegen` bool NOT NULL DEFAULT '0'," + "`plotManagementWildRegenEntities` mediumtext NOT NULL," + "`plotManagementWildRegenSpeed` long NOT NULL," + "`usingTowny` bool NOT NULL DEFAULT '0'," + "PRIMARY KEY (`name`)" + ")";
				try {
					Statement s = cntx.createStatement();
					s.executeUpdate(world_create);
				} catch (SQLException ee) {
					TownyMessaging.sendErrorMsg("Error Creating table WORLDS : " + ee.getMessage());
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error Checking table WORLDS :" + e.getMessage());
		}

		TownyMessaging.sendDebugMsg("Checking done!");
	}

	/**
	 * open a connection to the SQL server.
	 * 
	 * @return true if we successfully connected to the db.
	 */
	public boolean getContext() {

		try {
			if (cntx == null || cntx.isClosed()) {
				if ((this.username.equalsIgnoreCase("")) && (this.password.equalsIgnoreCase(""))) {
					cntx = DriverManager.getConnection(this.dsn);
				} else
					cntx = DriverManager.getConnection(this.dsn, this.username, this.password);

			}

			if (cntx == null || cntx.isClosed())
				return false;
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error could not Connect to db " + this.dsn + ": " + e.getMessage());
		}
		return false;
	}

	/**
	 * Build the SQL string and execute to INSERT/UPDATE
	 * 
	 * @param tb_name
	 * @param args
	 * @param keys
	 * @return true if the update was successfull.
	 */
	public boolean UpdateDB(String tb_name, HashMap<String, Object> args, List<String> keys) {

		if (!getContext())
			return false;
		String code;
		Statement s;
		if (keys == null) {
			code = "INSERT INTO " + tb_prefix + (tb_name.toUpperCase()) + " ";
			String keycode = "(";
			String valuecode = " VALUES (";

			Set<Map.Entry<String, Object>> set = args.entrySet();
			Iterator<Map.Entry<String, Object>> i = set.iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> me = (Map.Entry<String, Object>) i.next();

				keycode += me.getKey();
				keycode += "" + (i.hasNext() ? ", " : ")");
				if (me.getValue() instanceof String)
					valuecode += "'" + ((String) me.getValue()).replace("'", "\''") + "'";
				else if (me.getValue() instanceof Double)
					valuecode += "'" + me.getValue() + "'";
				else if (me.getValue() instanceof Float)
					valuecode += "'" + me.getValue() + "'";
				else
					valuecode += "" + me.getValue();
				valuecode += "" + (i.hasNext() ? "," : ")");
			}
			code += keycode;
			code += valuecode;
			try {
				s = cntx.createStatement();
				int rs = s.executeUpdate(code);
				if (rs == 0)
					return false;
				return true;
			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("SQL: Insert sql error " + e.getMessage() + " --> " + code);
			}
			return false;
		} else {
			code = "UPDATE " + tb_prefix + (tb_name.toUpperCase()) + " SET ";
			Set<Map.Entry<String, Object>> set = args.entrySet();
			Iterator<Map.Entry<String, Object>> i = set.iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> me = (Map.Entry<String, Object>) i.next();
				code += me.getKey() + " = ";
				if (me.getValue() instanceof String)
					code += "'" + ((String) me.getValue()).replace("'", "\''") + "'";
				else if (me.getValue() instanceof Float)
					code += "'" + me.getValue() + "'";
				else if (me.getValue() instanceof Double)
					code += "'" + me.getValue() + "'";
				else
					code += "" + me.getValue();
				code += "" + (i.hasNext() ? "," : "");
			}
			code += " WHERE ";

			Iterator<String> keys_i = keys.iterator();
			while (keys_i.hasNext()) {
				String key = (String) keys_i.next();
				code += key + " = ";
				Object v = args.get(key);
				if (v instanceof String)
					code += "'" + v + "'";
				else
					code += v;
				code += "" + (keys_i.hasNext() ? " AND " : "");
			}

			try {
				s = cntx.createStatement();
				int rs = s.executeUpdate(code);
				if (rs == 0) // if entry doesn't exist then try to insert 
					return UpdateDB(tb_name, args, null);
				return true;
			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("SQL: Update sql error " + e.getMessage() + " --> " + code);
			}
		}
		return false;
	}

	/**
	 * Build the SQL string and execute to DELETE
	 * 
	 * @param tb_name
	 * @param args
	 * @return true if the delete was a success.
	 */
	public boolean DeleteDB(String tb_name, HashMap<String, Object> args) {

		if (!getContext())
			return false;
		try {
			String wherecode = "DELETE FROM " + tb_prefix + (tb_name.toUpperCase()) + " WHERE ";
			Set<Map.Entry<String, Object>> set = args.entrySet();
			Iterator<Map.Entry<String, Object>> i = set.iterator();
			while (i.hasNext()) {
				Map.Entry<String, Object> me = (Map.Entry<String, Object>) i.next();
				wherecode += me.getKey() + " = ";
				if (me.getValue() instanceof String)
					wherecode += "'" + me.getValue() + "'";
				else if (me.getValue() instanceof Float)
					wherecode += "'" + me.getValue() + "'";
				else
					wherecode += "" + me.getValue();

				wherecode += "" + (i.hasNext() ? " AND " : "");
			}
			Statement s = cntx.createStatement();
			int rs = s.executeUpdate(wherecode);
			if (rs == 0) {
				TownyMessaging.sendDebugMsg("SQL: delete returned 0: " + wherecode);
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Error delete : " + e.getMessage());
		}
		return false;
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadResidentList() {

		TownyMessaging.sendDebugMsg("Loading Resident List");
		if (!getContext())
			return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "RESIDENTS");
			while (rs.next()) {
				try {
					newResident(rs.getString("name"));
				} catch (AlreadyRegisteredException e) {
				}
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
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
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "TOWNS");
			while (rs.next()) {
				try {
					newTown(rs.getString("name"));
				} catch (AlreadyRegisteredException e) {
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
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "NATIONS");
			while (rs.next()) {
				try {
					newNation(rs.getString("name"));
				} catch (AlreadyRegisteredException e) {
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

		if (!getContext())
			return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name FROM " + tb_prefix + "WORLDS");
			while (rs.next()) {
				try {
					newWorld(rs.getString("name"));
				} catch (AlreadyRegisteredException e) {
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: world list sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: world list unknown error : ");
			e.printStackTrace();
		}

		// Check for any new worlds registered with bukkit.
		if (plugin != null) {
			for (World world : plugin.getServer().getWorlds())
				try {
					newWorld(world.getName());
				} catch (AlreadyRegisteredException e) {
					//e.printStackTrace();
				} catch (NotRegisteredException e) {
					//e.printStackTrace();
				}
		}
		return true;
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResident(Resident resident) {

		TownyMessaging.sendDebugMsg("Loading resident " + resident.getName());
		if (!getContext())
			return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT " + " lastOnline,registered,isNPC,title,surname,town,friends,protectionStatus,townBlocks" + " FROM " + tb_prefix + "RESIDENTS " + " WHERE name='" + resident.getName() + "'");
			while (rs.next()) {
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
					resident.setNPC(rs.getBoolean("isNPC"));
				} catch (Exception e) {
					e.printStackTrace();
				}
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

				String line = rs.getString("town");
				if ((line != null) && (!line.isEmpty())) {
					resident.setTown(getTown(line));
					TownyMessaging.sendDebugMsg("Resident " + resident.getName() + " set to Town " + rs.getString("town"));
				}

				try {
					line = rs.getString("friends");
					if (line != null) {
						String[] tokens = line.split(",");
						for (String token : tokens) {
							if (!token.isEmpty()) {
								Resident friend = getResident(token);
								if (friend != null)
									resident.addFriend(friend);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					resident.setPermissions(rs.getString("protectionStatus"));
				} catch (Exception e) {
					e.printStackTrace();
				}

				line = rs.getString("townBlocks");
				if ((line != null) && (!line.isEmpty()))
					utilLoadTownBlocks(line, null, resident);
				return true;
			}
			return false;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident sql error : " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load resident unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadTown(Town town) {

		String line;
		String[] tokens;
		TownyMessaging.sendDebugMsg("Loading town " + town.getName());
		if (!getContext())
			return false;

		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT " + "residents,mayor,assistants,townBoard,nation,tag,protectionStatus,bonus,purchased,plotPrice,hasUpkeep,taxpercent,taxes" + ",plotTax,commercialPlotPrice,commercialPlotTax,embassyPlotPrice,embassyPlotTax" + ",open,public,townBlocks,homeBlock,spawn,outpostSpawns" + " FROM " + tb_prefix + "TOWNS " + " WHERE name='" + town.getName() + "'");
			while (rs.next()) {

				line = rs.getString("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Resident resident = getResident(token);
							if (resident != null)
								town.addResident(resident);
						}
					}
				}
				town.setMayor(getResident(rs.getString("mayor")));
				line = rs.getString("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Resident assistant = getResident(token);
							if ((assistant != null) && (town.hasResident(assistant)))
								town.addAssistant(assistant);
						}
					}
				}
				town.setTownBoard(rs.getString("townBoard"));
				line = rs.getString("tag");
				if (line != null)
					try {
						town.setTag(line);
					} catch (TownyException e) {
						town.setTag("");
					}
				town.setPermissions(rs.getString("protectionStatus"));
				town.setBonusBlocks(rs.getInt("bonus"));
				town.setTaxPercentage(rs.getBoolean("taxpercent"));
				town.setTaxes(rs.getFloat("taxes"));
				town.setHasUpkeep(rs.getBoolean("hasUpkeep"));
				town.setPlotPrice(rs.getFloat("plotPrice"));
				town.setPlotTax(rs.getFloat("plotTax"));
				town.setEmbassyPlotPrice(rs.getFloat("embassyPlotPrice"));
				town.setEmbassyPlotTax(rs.getFloat("embassyPlotTax"));
				town.setCommercialPlotPrice(rs.getFloat("commercialPlotPrice"));
				town.setCommercialPlotTax(rs.getFloat("commercialPlotTax"));
				town.setOpen(rs.getBoolean("open"));
				town.setPublic(rs.getBoolean("public"));

				town.setPurchasedBlocks(rs.getInt("purchased"));
				line = rs.getString("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, town, null);

				line = rs.getString("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = getWorld(tokens[0]);

							try {
								int x = Integer.parseInt(tokens[1]);
								int z = Integer.parseInt(tokens[2]);
								TownBlock homeBlock = world.getTownBlock(x, z);
								town.setHomeBlock(homeBlock);
							} catch (NumberFormatException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid location.");
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
							} catch (TownyException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a home block.");
							}

						} catch (NotRegisteredException e) {
							TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid world.");
						}
				}

				line = rs.getString("spawn");
				if (line != null) {
					tokens = line.split(",");
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
						} catch (NumberFormatException e) {
						} catch (NotRegisteredException e) {
						} catch (NullPointerException e) {
						} catch (TownyException e) {
							TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a spawn point.");
						}
					// Load outpost spawns
					line = rs.getString("outpostSpawns");
					if (line != null) {
						String[] outposts = line.split(";");
						for (String spawn : outposts) {
							tokens = spawn.split(",");
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
									town.addOutpostSpawn(loc);
								} catch (NumberFormatException e) {
								} catch (NotRegisteredException e) {
								} catch (NullPointerException e) {
								} catch (TownyException e) {
								}
						}
					}
				}
				return true;
			}
			return false;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town sql Error - " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load Town unknown Error - ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadNation(Nation nation) {

		String line = "";
		String[] tokens;
		TownyMessaging.sendDebugMsg("Loading nation " + nation.getName());
		if (!getContext())
			return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT towns,capital,assistants,tag,allies,enemies,taxes,neutral FROM " + tb_prefix + "NATIONS WHERE name='" + nation.getName() + "'");
			while (rs.next()) {
				line = rs.getString("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Town town = getTown(token);
							if (town != null)
								nation.addTown(town);
						}
					}
				}
				nation.setCapital(getTown(rs.getString("capital")));
				line = rs.getString("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Resident assistant = getResident(token);
							if (assistant != null)
								nation.addAssistant(assistant);
						}
					}
				}

				nation.setTag(rs.getString("tag"));

				line = rs.getString("allies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Nation friend = getNation(token);
							if (friend != null)
								nation.addAlly(friend); //("ally", friend);
						}
					}
				}

				line = rs.getString("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Nation enemy = getNation(token);
							if (enemy != null)
								nation.addEnemy(enemy); //("enemy", enemy);
						}
					}
				}
				nation.setTaxes(rs.getDouble("taxes"));
				nation.setNeutral(rs.getBoolean("neutral"));
			}
			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation sql error " + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load Nation unknown error - ");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean loadWorld(TownyWorld world) {

		String line = "";
		Boolean result = false;
		Long resultLong;
		String[] tokens;
		TownyMessaging.sendDebugMsg("Loading world " + world.getName());
		if (!getContext())
			return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM " + tb_prefix + "WORLDS WHERE name='" + world.getName() + "'");
			while (rs.next()) {
				line = rs.getString("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Town town = getTown(token);
							if (town != null) {
								town.setWorld(world);
							}
						}
					}
				}

				result = rs.getBoolean("claimable");
				if (result != null)
					try {
						world.setClaimable(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("pvp");
				if (result != null)
					try {
						world.setPVP(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("forcepvp");
				if (result != null)
					try {
						world.setForcePVP(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("forcetownmobs");
				if (result != null)
					try {
						world.setForceTownMobs(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("worldmobs");
				if (result != null)
					try {
						world.setWorldMobs(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("firespread");
				if (result != null)
					try {
						world.setFire(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("forcefirespread");
				if (result != null)
					try {
						world.setForceFire(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("explosions");
				if (result != null)
					try {
						world.setExpl(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("forceexplosions");
				if (result != null)
					try {
						world.setForceExpl(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("endermanprotect");
				if (result != null)
					try {
						world.setEndermanProtect(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("disableplayertrample");
				if (result != null)
					try {
						world.setDisablePlayerTrample(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("disablecreaturetrample");
				if (result != null)
					try {
						world.setDisableCreatureTrample(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("unclaimedZoneBuild");
				if (result != null)
					try {
						world.setUnclaimedZoneBuild(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("unclaimedZoneDestroy");
				if (result != null)
					try {
						world.setUnclaimedZoneDestroy(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("unclaimedZoneSwitch");
				if (result != null)
					try {
						world.setUnclaimedZoneSwitch(result);
					} catch (Exception e) {
					}

				result = rs.getBoolean("unclaimedZoneItemUse");
				if (result != null)
					try {
						world.setUnclaimedZoneItemUse(result);
					} catch (Exception e) {
					}

				line = rs.getString("unclaimedZoneName");
				if (result != null)
					try {
						world.setUnclaimedZoneName(line);
					} catch (Exception e) {
					}

				line = rs.getString("unclaimedZoneIgnoreIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String split : line.split(","))
							if (!split.isEmpty())
								try {
									nums.add(Integer.parseInt(split));
								} catch (NumberFormatException e) {
								}
						world.setUnclaimedZoneIgnore(nums);
					} catch (Exception e) {
					}

				result = rs.getBoolean("usingPlotManagementDelete");
				if (result != null)
					try {
						world.setUsingPlotManagementDelete(result);
					} catch (Exception e) {
					}

				line = rs.getString("plotManagementDeleteIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String split : line.split(","))
							if (!split.isEmpty())
								try {
									nums.add(Integer.parseInt(split));
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementDeleteIds(nums);
					} catch (Exception e) {
					}

				result = rs.getBoolean("usingPlotManagementMayorDelete");
				if (result != null)
					try {
						world.setUsingPlotManagementMayorDelete(result);
					} catch (Exception e) {
					}

				line = rs.getString("plotManagementMayorDelete");
				if (line != null)
					try {
						List<String> materials = new ArrayList<String>();
						for (String split : line.split(","))
							if (!split.isEmpty())
								try {
									materials.add(split.toUpperCase().trim());
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementMayorDelete(materials);
					} catch (Exception e) {
					}

				result = rs.getBoolean("usingPlotManagementRevert");
				if (result != null)
					try {
						world.setUsingPlotManagementRevert(result);
					} catch (Exception e) {
					}

				resultLong = rs.getLong("PlotManagementRevertSpeed");
				if (resultLong != null)
					try {
						world.setPlotManagementRevertSpeed(resultLong);
					} catch (Exception e) {
					}

				line = rs.getString("plotManagementIgnoreIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String split : line.split(","))
							if (!split.isEmpty())
								try {
									nums.add(Integer.parseInt(split));
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementIgnoreIds(nums);
					} catch (Exception e) {
					}

				result = rs.getBoolean("usingPlotManagementWildRegen");
				if (result != null)
					try {
						world.setUsingPlotManagementWildRevert(result);
					} catch (Exception e) {
					}

				line = rs.getString("plotManagementWildRegenEntities");
				if (line != null)
					try {
						List<String> entities = new ArrayList<String>();
						for (String split : line.split(","))
							if (!split.isEmpty())
								try {
									entities.add(split.trim());
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementWildRevertEntities(entities);
					} catch (Exception e) {
					}

				resultLong = rs.getLong("plotManagementWildRegenSpeed");
				if (resultLong != null)
					try {
						world.setPlotManagementWildRevertDelay(resultLong);
					} catch (Exception e) {
					}

				result = rs.getBoolean("usingTowny");
				if (result != null)
					try {
						world.setUsingTowny(result);
					} catch (Exception e) {
					}

			}
			return true;

		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL: Load world sql error (" + world.getName() + ")" + e.getMessage());
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Load world unknown error - ");
			e.printStackTrace();
		}
		return false;

	}

	@Override
	public boolean loadTownBlocks() {

		String line = "";
		Boolean result = false;
		// Load town blocks
		if (!getContext())
			return false;
		ResultSet rs;
		for (TownBlock townBlock : getAllTownBlocks()) {
			boolean set = false;
			try {
				Statement s = cntx.createStatement();
				rs = s.executeQuery("SELECT " + "permissions,locked,changed" + " FROM " + tb_prefix + "TOWNBLOCKS" + " WHERE world='" + townBlock.getWorld().getName() + "' AND x='" + townBlock.getX() + "' AND z='" + townBlock.getZ() + "'");
				while (rs.next()) {
					line = rs.getString("permissions");
					if (line != null)
						try {
							townBlock.setPermissions(line.trim());
							set = true;
						} catch (Exception e) {
						}

					result = rs.getBoolean("changed");
					if (result != null)
						try {
							townBlock.setChanged(result);
						} catch (Exception e) {
						}

					result = rs.getBoolean("locked");
					if (result != null)
						try {
							townBlock.setLocked(result);
						} catch (Exception e) {
						}

					if (!set) {
						// no permissions found so set in relation to it's owners perms.
						try {
							if (townBlock.hasResident()) {
								townBlock.setPermissions(townBlock.getResident().getPermissions().toString());
							} else {
								townBlock.setPermissions(townBlock.getTown().getPermissions().toString());
							}
						} catch (NotRegisteredException e) {
							// Will never reach here
						}
					}
				}
			} catch (SQLException e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlocks ");
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public synchronized boolean saveResident(Resident resident) {

		TownyMessaging.sendDebugMsg("Saving Resident");
		try {
			HashMap<String, Object> res_hm = new HashMap<String, Object>();
			res_hm.put("name", resident.getName());
			res_hm.put("lastOnline", resident.getLastOnline());
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("town", resident.hasTown() ? resident.getTown().getName() : "");
			String fstr = "";
			for (Resident friend : resident.getFriends())
				fstr += friend.getName() + ",";
			res_hm.put("friends", fstr);
			res_hm.put("townBlocks", utilSaveTownBlocks(new ArrayList<TownBlock>(resident.getTownBlocks())));
			res_hm.put("protectionStatus", resident.getPermissions().toString());
			UpdateDB("RESIDENTS", res_hm, Arrays.asList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public synchronized boolean saveTown(Town town) {

		TownyMessaging.sendDebugMsg("Saving town " + town.getName());
		try {
			HashMap<String, Object> twn_hm = new HashMap<String, Object>();
			twn_hm.put("name", town.getName());
			twn_hm.put("residents", StringMgmt.join(town.getResidents(), ","));
			twn_hm.put("mayor", town.hasMayor() ? town.getMayor().getName() : "");
			twn_hm.put("nation", town.hasNation() ? town.getNation().getName() : "");
			String fstr = "";
			for (Resident assist : town.getAssistants())
				fstr += assist.getName() + ",";
			twn_hm.put("assistants", fstr);
			twn_hm.put("townBoard", town.getTownBoard());
			twn_hm.put("tag", town.getTag());
			twn_hm.put("protectionStatus", town.getPermissions().toString());
			twn_hm.put("bonus", town.getBonusBlocks());
			twn_hm.put("purchased", town.getPurchasedBlocks());
			twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
			twn_hm.put("plotPrice", town.getPlotPrice());
			twn_hm.put("plotTax", town.getPlotTax());
			twn_hm.put("taxes", town.getTaxes());
			twn_hm.put("hasUpkeep", town.hasUpkeep());
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("townBlocks", utilSaveTownBlocks(new ArrayList<TownBlock>(town.getTownBlocks())));
			twn_hm.put("homeblock", town.hasHomeBlock() ? town.getHomeBlock().getWorld().getName() + "," + Integer.toString(town.getHomeBlock().getX()) + "," + Integer.toString(town.getHomeBlock().getZ()) : "");
			twn_hm.put("spawn", town.hasSpawn() ? town.getSpawn().getWorld().getName() + "," + Double.toString(town.getSpawn().getX()) + "," + Double.toString(town.getSpawn().getY()) + "," + Double.toString(town.getSpawn().getZ()) + "," + Float.toString(town.getSpawn().getPitch()) + "," + Float.toString(town.getSpawn().getYaw()) : "");
			// Outpost Spawns
			if (town.hasOutpostSpawn()) {
				String outpostArray = "";
				for (Location spawn : new ArrayList<Location>(town.getAllOutpostSpawns())) {
					outpostArray += (spawn.getWorld().getName() + "," + Double.toString(spawn.getX()) + "," + Double.toString(spawn.getY()) + "," + Double.toString(spawn.getZ()) + "," + Float.toString(spawn.getPitch()) + "," + Float.toString(spawn.getYaw()) + ";");
				}
				twn_hm.put("outpostSpawns", outpostArray);
			}

			UpdateDB("TOWNS", twn_hm, Arrays.asList("name"));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save Town unknown error");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized boolean saveNation(Nation nation) {

		TownyMessaging.sendDebugMsg("Saving nation " + nation.getName());
		try {
			HashMap<String, Object> nat_hm = new HashMap<String, Object>();
			nat_hm.put("name", nation.getName());
			String fstr = "";
			for (Town town : nation.getTowns())
				fstr += town.getName() + ",";
			nat_hm.put("towns", fstr);
			nat_hm.put("capital", nation.hasCapital() ? nation.getCapital().getName() : "");
			nat_hm.put("tag", nation.hasTag() ? nation.getTag() : "");
			fstr = "";
			for (Resident assistant : nation.getAssistants())
				fstr += assistant.getName() + ",";
			nat_hm.put("assistants", fstr);
			fstr = "";
			for (Nation allyNation : nation.getAllies())
				fstr += allyNation.getName() + ",";
			nat_hm.put("allies", fstr);
			fstr = "";
			for (Nation enemyNation : nation.getEnemies())
				fstr += enemyNation.getName() + ",";
			nat_hm.put("enemies", fstr);
			nat_hm.put("taxes", nation.getTaxes());
			nat_hm.put("neutral", nation.isNeutral());
			UpdateDB("NATIONS", nat_hm, Arrays.asList("name"));
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
			HashMap<String, Object> nat_hm = new HashMap<String, Object>();

			nat_hm.put("name", world.getName());

			String fstr = "";
			for (Town town : world.getTowns())
				fstr += town.getName() + ",";

			// Towns
			nat_hm.put("towns", fstr);
			// PvP
			nat_hm.put("pvp", world.isPVP());
			// Force PvP
			nat_hm.put("forcepvp", world.isForcePVP());
			// Claimable
			nat_hm.put("claimable", world.isClaimable());
			// has monster spawns			
			nat_hm.put("worldmobs", world.hasWorldMobs());
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
			// PlayerTrample
			nat_hm.put("disableplayertrample", world.isDisablePlayerTrample());
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
			if (world.getUnclaimedZoneIgnoreIds() != null)
				nat_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(world.getUnclaimedZoneIgnoreIds(), ","));

			// Using PlotManagement Delete
			nat_hm.put("usingPlotManagementDelete", world.isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				nat_hm.put("plotManagementDeleteIds", StringMgmt.join(world.getPlotManagementDeleteIds(), ","));

			// Using PlotManagement Mayor Delete
			nat_hm.put("usingPlotManagementMayorDelete", world.isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				nat_hm.put("plotManagementMayorDelete", StringMgmt.join(world.getPlotManagementMayorDelete(), ","));

			// Using PlotManagement Revert
			nat_hm.put("usingPlotManagementRevert", world.isUsingPlotManagementRevert());
			// Using PlotManagement Revert Speed
			nat_hm.put("plotManagementRevertSpeed", world.getPlotManagementRevertSpeed());

			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				nat_hm.put("plotManagementIgnoreIds", StringMgmt.join(world.getPlotManagementIgnoreIds(), ","));

			// Using PlotManagement Wild Regen
			nat_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				nat_hm.put("PlotManagementWildRegenEntities", StringMgmt.join(world.getPlotManagementWildRevertEntities(), ","));

			// Using PlotManagement Wild Regen Delay
			nat_hm.put("plotManagementWildRegenSpeed", world.getPlotManagementWildRevertDelay());

			// Using Towny
			nat_hm.put("usingTowny", world.isUsingTowny());

			UpdateDB("WORLDS", nat_hm, Arrays.asList("name"));

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save world unknown error (" + world.getName() + ")");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean saveTownBlock(TownBlock townBlock) {

		TownyMessaging.sendDebugMsg("Saving town block " + townBlock.getWorld().getName() + ":" + townBlock.getX() + "x" + townBlock.getZ());
		try {
			HashMap<String, Object> tb_hm = new HashMap<String, Object>();
			tb_hm.put("world", townBlock.getWorld().getName());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());
			tb_hm.put("permissions", townBlock.getPermissions().toString());
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());
			UpdateDB("TOWNBLOCKS", tb_hm, Arrays.asList("world", "x", "z"));
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("SQL: Save TownBlock unknown error");
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void deleteResident(Resident resident) {

		HashMap<String, Object> res_hm = new HashMap<String, Object>();
		res_hm.put("name", resident.getName());
		DeleteDB("RESIDENTS", res_hm);
	}

	@Override
	public void deleteTown(Town town) {

		HashMap<String, Object> twn_hm = new HashMap<String, Object>();
		twn_hm.put("name", town.getName());
		DeleteDB("TOWNS", twn_hm);
	}

	@Override
	public void deleteNation(Nation nation) {

		HashMap<String, Object> nat_hm = new HashMap<String, Object>();
		nat_hm.put("name", nation.getName());
		DeleteDB("NATIONS", nat_hm);
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {

		HashMap<String, Object> twn_hm = new HashMap<String, Object>();
		twn_hm.put("world", townBlock.getWorld().getName());
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("TOWNBLOCKS", twn_hm);

	}

	@Override
	public synchronized void backup() throws IOException {

		TownyMessaging.sendMsg("Performing backup");
		TownyMessaging.sendMsg("***** Warning *****");
		TownyMessaging.sendMsg("***** Only Snapshots and Regen files will be backed up");
		TownyMessaging.sendMsg("***** Make sure you schedule a backup in MySQL too!!!");
		String backupType = TownySettings.getFlatFileBackupType();
		if (!backupType.equalsIgnoreCase("none")) {

			TownyLogger.shutDown();

			long t = System.currentTimeMillis();
			String newBackupFolder = rootFolder + FileMgmt.fileSeparator() + "backup" + FileMgmt.fileSeparator() + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + Long.toString(t);
			FileMgmt.checkFolders(new String[] {
					rootFolder,
					rootFolder + FileMgmt.fileSeparator() + "backup" });
			if (backupType.equalsIgnoreCase("folder")) {
				FileMgmt.checkFolders(new String[] { newBackupFolder });
				FileMgmt.copyDirectory(new File(rootFolder + dataFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + logFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + settingsFolder), new File(newBackupFolder));
			} else if (backupType.equalsIgnoreCase("zip"))
				FileMgmt.zipDirectories(new File[] {
						new File(rootFolder + dataFolder),
						new File(rootFolder + logFolder),
						new File(rootFolder + settingsFolder) }, new File(newBackupFolder + ".zip"));
			else {
				plugin.setupLogger();
				throw new IOException("[Towny] Unsupported flatfile backup type (" + backupType + ")");
			}
			plugin.setupLogger();
		}
	}

	@Override
	public void deleteUnusedResidentFiles() {

	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveResidentList() {

		return true;
	}

	@Override
	public boolean saveTownList() {

		return true;
	}

	@Override
	public boolean saveNationList() {

		return true;
	}

	@Override
	public boolean saveWorldList() {

		return true;
	}

}