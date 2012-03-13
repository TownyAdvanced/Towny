/*
 * Towny MYSQL Source by StPinker
 * 
 * Released under LGPL
 * 
*/
package com.palmergames.bukkit.towny.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.KeyValueFile;
import com.palmergames.util.StringMgmt;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.logging.Logger;
public class TownyMYSQLSource extends TownyDatabaseHandler
{
	Logger log = Logger.getLogger("Minecraft");
	protected String rootFolder = "";
	protected String dataFolder = FileMgmt.fileSeparator() + "data";
	protected String settingsFolder = FileMgmt.fileSeparator() + "settings";
	protected final String newLine = System.getProperty("line.separator");
	protected String hostname = "";
	protected String port = "";
	protected String db_name = "";
	protected String username = "";
	protected String password = "";
	protected String tb_prefix = "";
	protected String connect_str = "";
	protected String logFolder = FileMgmt.fileSeparator() + "logs";
	Connection cntx = null;
		
	private enum elements {
	VER, novalue;
	public static elements fromString(String Str) {
		try {
				return valueOf(Str);
			} catch (Exception ex){
				return novalue;
			}
		}
	};
		
	public TownyMYSQLSource(String arg) {		
		String[] dbinfos = arg.split(";");
		if (dbinfos.length<6)
			log.info("Error wrong number of args "+dbinfos.length+" < 6");
		hostname = dbinfos[0];		
		port = dbinfos[1];
		db_name = dbinfos[2];
		username = dbinfos[3];
		password = dbinfos[4];
		tb_prefix = db_name+"."+dbinfos[5];	
		
		connect_str = "jdbc:mysql://"+hostname+":"+port+"/"+db_name+"?user="+username+"&password="+password;		
	}
	
	public boolean getContext()
	{
		try
		{
			if (cntx==null || cntx.isClosed())
				cntx = DriverManager.getConnection(connect_str);
			if (cntx==null || cntx.isClosed())
				return false;
			return true;
		}
		catch (SQLException e) 
		{
			log.info("Error could not Connect to db"+connect_str+": "+e.getMessage());
		}
		return false;
	}
	@Override
	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;	
		this.rootFolder = universe.getRootFolder();
						
		try {
			FileMgmt.checkFiles(new String[]{
					rootFolder,				
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data"
			});
			FileMgmt.checkFiles(new String[]{
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt"
			});		
		} catch (IOException e) {
			log.info("[Towny] Error: Could not create flatfile default files and folders.");
		}
		
		
		// Checking for db tables	
		System.out.println("Checking for tables existence");
		DatabaseMetaData dbm;
		if (getContext())				
			System.out.println("Connected to Database");		
		else		
		{
			System.out.println("Error connecting to Database");
			return;
		}
		try  { dbm = cntx.getMetaData(); }
		catch (SQLException e) { System.out.println("Cannot get Table metadata"); return; }
		
		try 
		{ 
			ResultSet town_table = dbm.getTables(null, null, tb_prefix+"towns", null); 
			if (town_table.next()) { System.out.println("[Towny] Table towns is ok!"); }
			else {				
				String town_create = 
						  "CREATE TABLE "+tb_prefix+"towns ("+
						  "`name` mediumtext NOT NULL,"+
						  "`world` mediumtext,"+
						  "`mayor` mediumtext NOT NULL,"+
						  "`assistants` text,"+
						  "`bonus` int(11) NOT NULL,"+
						  "`purchased` int(11) NOT NULL,"+
						  "`taxes` float NOT NULL,"+
						  "`taxpercent` tinyint(1) DEFAULT NULL,"+
						  "`hasUpkeep` tinyint(1) NOT NULL,"+
						  "`plotPrice` float NOT NULL,"+
						  "`plotTax` float NOT NULL,"+
						  "`commercialPlotPrice` float NOT NULL,"+
						  "`commercialPlotTax` float NOT NULL,"+
						  "`embassyPlotPrice` float NOT NULL,"+
						  "`embassyPlotTax` float NOT NULL,"+
						  "`open` tinyint(1) NOT NULL,"+
						  "`public` tinyint(1) NOT NULL,"+
						  "`board` mediumtext NOT NULL,"+
						  "`nation` mediumtext NOT NULL,"+
						  "`protectionStatus` mediumtext NOT NULL,"+
						  "`spawn_x` float DEFAULT NULL,"+
						  "`spawn_y` float DEFAULT NULL,"+
						  "`spawn_z` float DEFAULT NULL,"+
						  "`spawn_pitch` float DEFAULT NULL,"+
						  "`spawn_yaw` float DEFAULT NULL,"+
						  "`homeblock_x` int(11) DEFAULT NULL,"+
						  "`homeblock_z` int(11) DEFAULT NULL,"+
						  "`outpostSpawns` mediumtext DEFAULT NULL,"+
						  "PRIMARY KEY (`name`(20))"+
						") ENGINE=INNODB DEFAULT CHARSET=latin1";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(town_create);					
				} catch (SQLException ec) { System.out.println("Error Creating table towns " + ec.getMessage()); }				
			}			
		}
		catch (SQLException e)
		{ System.out.println("Error Checking table towns" + e.getMessage()); }
		
		
		try 
		{ 
			ResultSet res_table = dbm.getTables(null, null, tb_prefix+"residents", null); 
			if (res_table.next()) { System.out.println("[Towny] Table residents is ok!"); }
			else 
			{			
				String resident_create = 
						"CREATE TABLE "+tb_prefix+"residents ("+
						 " `name` text NOT NULL,"+
						  "`town` mediumtext,"+
						  "`lastOnline` BIGINT NOT NULL,"+
						  "`registered` BIGINT NOT NULL,"+
						  "`isNPC` tinyint(1) NOT NULL,"+
						  "`title` mediumtext,"+
						  "`surname` mediumtext,"+
						  "`protectionStatus` mediumtext,"+
						  "`friends` mediumtext,"+
						  "KEY `new_index` (`name`(20))"+
						") ENGINE=INNODB DEFAULT CHARSET=latin1";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(resident_create);
				} catch (SQLException ee) { System.out.println("Error Creating table residents"); }
			}
		} 
		catch (SQLException e)
		{ System.out.println("Error Checking table residents" + e.getMessage()); }
		
		try 
		{ 
			ResultSet nat_table = dbm.getTables(null, null, tb_prefix+"nations", null); 
			if (nat_table.next()) { System.out.println("[Towny] Table nations is ok!"); }
			else 
			{					
				String nation_create = 
						"CREATE TABLE "+tb_prefix+"nations ("+
						"`name` mediumtext NOT NULL,"+						
						"`capital` mediumtext NOT NULL,"+						
						"`taxes` float NOT NULL"+
						") ENGINE=INNODB DEFAULT CHARSET=latin1";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(nation_create);
				} catch (SQLException ee) { System.out.println("Error Creating table nations"); }
			}
		}
		catch (SQLException e)
		{ System.out.println("Error Checking table nations" + e.getMessage()); }
		
		try 
		{ 
			ResultSet tb_table = dbm.getTables(null, null, tb_prefix+"townblocks", null); 			
			if (tb_table.next()) { System.out.println("[Towny] Table townblocks is ok!"); }
			else 
			{
				String townblock_create = 
						"CREATE TABLE "+tb_prefix+"townblocks ("+						
						"`x` bigint(20) NOT NULL,"+
						"`z` bigint(20) NOT NULL,"+						
						"`world` mediumtext NOT NULL,"+
						"`owner` mediumtext NOT NULL,"+
						"`town` mediumtext NOT NULL,"+
						"`protectionStatus` mediumtext NOT NULL,"+
						"`locked` tinyint(1) NOT NULL DEFAULT '0',"+
						"`changed` tinyint(1) NOT NULL DEFAULT '0',"+
						"`isOutpost` tinyint(1) NOT NULL DEFAULT 0,"+
						"`plotType` int(7) NOT NULL DEFAULT 0,"+
						"`plotPrice` float NOT NULL DEFAULT '-1.0',"+
						"PRIMARY KEY (`x`,`z`)"+
						") ENGINE=INNODB DEFAULT CHARSET=latin1";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(townblock_create);
				} catch (SQLException ee) { System.out.println("Error Creating table townblocks"); }
			}
		}
		catch (SQLException e)
		{ System.out.println("Error Checking table townblocks" + e.getMessage()); }
		
		System.out.println("Checking done!");
	}
	
	/*
	* Load keys
	*/
	
	@Override
	public boolean loadResidentList() {
		sendDebugMsg("Loading Resident List");
		if (!getContext()) return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name FROM "+tb_prefix+"residents");						
			while (rs.next())
			{
				try {
					newResident(rs.getString("name"));	
				} catch (AlreadyRegisteredException e) {}
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
		sendDebugMsg("Loading Town List");
		if (!getContext()) return false;
			try {
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT name FROM "+tb_prefix+"towns");
				while (rs.next())
    			{					
					try {											
						newTown(rs.getString("name"));
					} catch (AlreadyRegisteredException e) {}
    			} 
				return true;
			} 
			catch (SQLException e) 
			{ log.info("Towny SQL: town list sql error : "+e.getMessage()); }				
			catch (Exception e) 
			{ log.info("Towny SQL: town list unknown error: ");e.printStackTrace(); }		
		return false;
	}
	
	@Override
	public boolean loadNationList() {
		sendDebugMsg("Loading Nation List");	
		if (!getContext()) return false;								
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT name FROM "+tb_prefix+"nations");
				while (rs.next())
    			{
					try {
					newNation(rs.getString("name"));
					} catch (AlreadyRegisteredException e) {} 
    			}
				return true;
			}
			catch (SQLException e)
			{ log.info("Towny SQL: nation list sql error : "+e.getMessage()); }
			catch (Exception e)
			{ log.info("Towny SQL: nation list unknown error: ");e.printStackTrace();}		
		return false;	
	}
	
	@Override
	public boolean loadWorldList() {
	
		sendDebugMsg("Loading World List");
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
	public boolean loadResident(Resident resident)
	{		
		sendDebugMsg("Loading resident "+resident.getName());	
		if (!getContext()) return false;							
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT town,lastOnline,registered,isNPC,title,surname,protectionStatus,friends FROM "+tb_prefix+"residents WHERE name='"+resident.getName()+"'");				
				while (rs.next())
    			{	
					try
					{
					Town t = getTown(rs.getString("town"));
					resident.setTown(t);
					} catch (NotRegisteredException e) {}
					resident.setLastOnline(rs.getLong("lastOnline"));
					resident.setRegistered(rs.getLong("registered"));
					resident.setNPC(rs.getBoolean("isNPC"));
					resident.setTitle(rs.getString("title"));
					resident.setSurname(rs.getString("surname"));
					resident.setPermissions(rs.getString("protectionStatus"));
					String line = rs.getString("friends");
					if (line != null) {
						String[] tokens = line.split(",");
						for (String token : tokens) {
							if (!token.isEmpty()){
								Resident friend = getResident(token);
								if (friend != null)
									resident.addFriend(friend);
							}
						}
					}
					return true;
    			}
				return false;				
			}
			catch (SQLException e)
			{ log.info("Towny SQL: Load resident sql error : "+e.getMessage()); }
			catch (Exception e)
			{ log.info("Towny SQL: Load resident unknown error");e.printStackTrace();}		
		return false;
	}
	
	@Override
	public boolean loadTown(Town town)
	{
		sendDebugMsg("Loading town "+town.getName());	
		if (!getContext()) return false;
						
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT name FROM "+tb_prefix+"residents WHERE town='"+town.getName()+"'");				
				try 
				{
				while (rs.next())					
					town.addResident(getResident(rs.getString("name")));
				} catch (AlreadyRegisteredException e) {}
				
												
				rs = s.executeQuery("SELECT " +
				"mayor,assistants, nation,world,bonus,purchased,taxes,taxpercent,hasUpkeep,outpostSpawns," +
				"plotPrice,plotTax,commercialPlotPrice,commercialPlotTax,embassyPlotPrice,embassyPlotTax," +
				"open,public,board,protectionStatus,spawn_x,spawn_y,spawn_z,spawn_pitch,spawn_yaw,homeblock_x,homeblock_z" +
				" FROM "+tb_prefix+"towns WHERE name='"+town.getName()+"'");
				while (rs.next())
    			{					
						
						
						town.setMayor( getResident(rs.getString("mayor")) );	
						try 
						{ 
							Nation n = getNation(rs.getString("nation"));
							n.addTown(town);
						} catch (NotRegisteredException e) {}
						try 
						{
							TownyWorld tworld = getWorld(rs.getString("world")); 					
							town.setWorld(tworld);
							int x = rs.getInt("homeblock_x");
							int z = rs.getInt("homeblock_z");
							try {
								tworld.newTownBlock(x,z);								
							} catch (AlreadyRegisteredException e) {}
							try {
								TownBlock tb = tworld.getTownBlock(x,z);
								tb.setTown(town);
								town.setHomeBlock(tb);
								if (tb.isHomeBlock())
								{
									System.out.println("Homeblock: "+tb.getX()+"x"+tb.getZ());
								}
								System.out.println("Homeblock set! "+x+"x"+z );
							} catch (NumberFormatException e) {
								System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid location.");
							} catch (NotRegisteredException e) {
								System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
							} catch (TownyException e) {
								System.out.println("[Towny] [Warning] " + town.getName() + " does not have a home block.");
							}
							
						} catch (NotRegisteredException e) { System.out.println("Could not load homeblock"); }
						town.setBonusBlocks(rs.getInt("bonus"));
						town.setTaxes(rs.getFloat("taxes"));
						town.setTaxPercentage(rs.getBoolean("taxpercent"));						
						town.setHasUpkeep(rs.getBoolean("hasUpkeep"));
						town.setPlotPrice(rs.getFloat("plotPrice"));
						town.setPlotTax(rs.getFloat("plotTax"));
						town.setEmbassyPlotPrice(rs.getFloat("embassyPlotPrice"));
						town.setEmbassyPlotTax(rs.getFloat("embassyPlotTax"));
						town.setCommercialPlotPrice(rs.getFloat("commercialPlotPrice"));
						town.setCommercialPlotTax(rs.getFloat("commercialPlotTax"));
						town.setOpen(rs.getBoolean("open"));
						town.setPublic(rs.getBoolean("public"));			
						town.setTownBoard(rs.getString("board"));
						town.setPurchasedBlocks(rs.getInt("purchased"));						
						town.setPermissions(rs.getString("protectionStatus"));
						if (rs.getString("assistants")!=null)
						{
							String line = rs.getString("assistants");
							if (line != null) {
								String[] tokens = line.split(",");
								for (String token : tokens) {
									if (!token.isEmpty()){
										Resident assistant = getResident(token);
										if ((assistant != null) && (town.hasResident(assistant)))
											town.addAssistant(assistant);
									}
								}
							}
						}
						try
						{
							World world = plugin.getServerWorld(rs.getString("world"));						
							town.setSpawn(new Location(world,							
							rs.getFloat("spawn_x"),rs.getFloat("spawn_y"),rs.getFloat("spawn_z"),
							rs.getFloat("spawn_yaw"),rs.getFloat("spawn_pitch")
							));															
						} catch (Exception e) { System.out.println("Spawn load error "+e.getMessage()); }	
						
						// Load outpost spawns
						String line = rs.getString("outpostSpawns");
						if (line != null) {
							String[] outposts = line.split(";");
							for (String spawn : outposts) {
								String[] tokens = spawn.split(",");
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
						
						return true;																			
    			}
				return false;
			}
			catch (SQLException e)
			{  log.info("Towny MySQL: Load Town sql Error - "+e.getMessage()); }
			catch (Exception e)
			{  log.info("Towny MySQL: Load Town unknown Error - ");e.printStackTrace(); }		
		return false;
	}
	
	
	@Override
	public boolean loadNation(Nation nation) {
		sendDebugMsg("Loading nation "+nation.getName());	
		if (!getContext()) return false;							
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT capital,taxes FROM "+tb_prefix+"nations WHERE name='"+nation.getName()+"'");
				while (rs.next())
    			{
					try
					{
						Town capital = getTown(rs.getString("capital"));
						nation.setCapital(capital);
					} catch (NotRegisteredException e) {}
					nation.setTaxes(rs.getFloat("taxes"));
    			}	
				return true;
			}
			catch (SQLException e)
			{  log.info("Towny MySQL: Load Nation sql error "+e.getMessage()); }
			catch (Exception e)
			{  log.info("Towny MySQL: Load Nation unknown error - ");e.printStackTrace(); }		
		return false;
	}
	
	@Override
	public boolean loadWorld(TownyWorld world) {
		String line = "";		
		String path = getWorldFilename(world);
		
		// create the world file if it doesn't exist
		try {
			FileMgmt.checkFiles(new String[]{path});
		} catch (IOException e1) {
			System.out.println("[Towny] Loading Error: Exception while reading file " + path);
			e1.printStackTrace();
		}
		
		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
												
				line = kvFile.get("claimable");
				if (line != null)
					try {
						world.setClaimable(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
					
				line = kvFile.get("pvp");
				if (line != null)
					try {
						world.setPVP(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("forcepvp");
				if (line != null)
					try {
						world.setForcePVP(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("forcetownmobs");
				if (line != null)
					try {
						world.setForceTownMobs(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("worldmobs");
				if (line != null)
					try {
						world.setWorldMobs(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
					
				line = kvFile.get("firespread");
				if (line != null)
					try {
						world.setFire(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("forcefirespread");
				if (line != null)
					try {
						world.setForceFire(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("explosions");
				if (line != null)
					try {
						world.setExpl(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("forceexplosions");
				if (line != null)
					try {
						world.setForceExpl(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("endermanprotect");
				if (line != null)
					try {
						world.setEndermanProtect(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("disableplayertrample");
				if (line != null)
					try {
						world.setDisablePlayerTrample(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("disablecreaturetrample");
				if (line != null)
					try {
						world.setDisableCreatureTrample(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("unclaimedZoneBuild");
				if (line != null)
					try {
						world.setUnclaimedZoneBuild(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneDestroy");
				if (line != null)
					try {
						world.setUnclaimedZoneDestroy(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneSwitch");
				if (line != null)
					try {
						world.setUnclaimedZoneSwitch(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneItemUse");
				if (line != null)
					try {
						world.setUnclaimedZoneItemUse(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneName");
				if (line != null)
					try {
						world.setUnclaimedZoneName(line);
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneIgnoreIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: line.split(","))
							if (!s.isEmpty())
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						world.setUnclaimedZoneIgnore(nums);
					} catch (Exception e) {
					}
				
				line = kvFile.get("usingPlotManagementDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementDelete(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("plotManagementDeleteIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: line.split(","))
							if (!s.isEmpty())
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						world.setPlotManagementDeleteIds(nums);
					} catch (Exception e) {
					}
				
				line = kvFile.get("usingPlotManagementMayorDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementMayorDelete(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("plotManagementMayorDelete");
				if (line != null)
					try {
						List<String> materials = new ArrayList<String>();
						for (String s: line.split(","))
							if (!s.isEmpty())
							try {
								materials.add(s.toUpperCase().trim());
							} catch (NumberFormatException e) {
							}
						world.setPlotManagementMayorDelete(materials);
					} catch (Exception e) {
					}
				
				line = kvFile.get("usingPlotManagementRevert");
				if (line != null)
					try {
						world.setUsingPlotManagementRevert(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("usingPlotManagementRevertSpeed");
				if (line != null)
					try {
						world.setPlotManagementRevertSpeed(Long.parseLong(line));
					} catch (Exception e) {
					}
				line = kvFile.get("plotManagementIgnoreIds");
				if (line != null)
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: line.split(","))
							if (!s.isEmpty())
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						world.setPlotManagementIgnoreIds(nums);
					} catch (Exception e) {
					}
				
				line = kvFile.get("usingPlotManagementWildRegen");
				if (line != null)
					try {
						world.setUsingPlotManagementWildRevert(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("PlotManagementWildRegenEntities");
				if (line != null)
					try {
						List<String> entities = new ArrayList<String>();
						for (String s: line.split(","))
							if (!s.isEmpty())
							try {
								entities.add(s.trim());
							} catch (NumberFormatException e) {
							}
						world.setPlotManagementWildRevertEntities(entities);
					} catch (Exception e) {
					}
				
				line = kvFile.get("usingPlotManagementWildRegenDelay");
				if (line != null)
					try {
						world.setPlotManagementWildRevertDelay(Long.parseLong(line));
					} catch (Exception e) {
					}				
				
				line = kvFile.get("usingTowny");
				if (line != null)
					try {
						world.setUsingTowny(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				// loadTownBlocks(world);

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading world file " + path);
				e.printStackTrace();
				return false;
			}

			return true;
		} else {
			System.out.println("[Towny] Loading Error: File error while reading " + world.getName());
			return false;
		}		
	}
	
	@Override
	public boolean loadTownBlocks() {
		System.out.println("Loading Town blocks");
		// Load town blocks
		if (!getContext()) return false;
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT " +
				"world,town,x,z,owner,protectionStatus,locked, " +
				"isOutpost, plotPrice, plotType"+
				" FROM "+tb_prefix+"townblocks");
			while (rs.next())
			{
				Integer x = rs.getInt("x");
				Integer z = rs.getInt("z");																
				
				TownyWorld world = getWorld(rs.getString("world"));
				try  { world.newTownBlock(x, z); } 
				catch (AlreadyRegisteredException e) {}				
							
				TownBlock tb = world.getTownBlock(x, z);	
				try 
				{ 
					if (!tb.hasTown())
					{
						Town t = getTown(rs.getString("town"));
						tb.setTown(t);
					}
				}
				catch (NotRegisteredException e) {}
				
				tb.setPermissions(rs.getString("protectionStatus"));
				tb.setLocked(rs.getBoolean("locked"));
				tb.setPlotPrice(rs.getFloat("plotPrice"));
				tb.setType(rs.getInt("plotType"));
				tb.setOutpost(rs.getBoolean("isOutpost"));
				try
				{
					Resident r = getResident(rs.getString("owner")); 
					tb.setResident(r);					
				} catch (NotRegisteredException e) {}
				
				try {
					if (tb.hasResident()){
						tb.setPermissions(tb.getResident().getPermissions().toString());
					} else {
						tb.setPermissions(tb.getTown().getPermissions().toString());
					}
				} catch (NotRegisteredException e) {}
				
			}			
			return true;
		}
		catch (Exception e) {System.out.println("Loading Town blocks Exception "+e.getMessage());}
		return false;
	}
	
	
	
	
	public boolean UpdateDB(String tb_name, HashMap<String,Object> args, List<String> keys)
	{
		if (!getContext()) return false;
		String code;
		Statement s;		
		if (keys==null)
		{
			code = "INSERT INTO "+tb_prefix+tb_name+" ";
			String keycode = "(";
			String valuecode = " VALUES (";
						
			Set<Map.Entry<String,Object>> set = args.entrySet();			
			Iterator<Map.Entry<String,Object>> i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry<String,Object> me = (Map.Entry<String,Object>)i.next();
				
				keycode += me.getKey();
				keycode += ""+(i.hasNext()?", ":")");			
				if (me.getValue() instanceof String)
					valuecode += "'"+me.getValue()+"'";
				else if (me.getValue() instanceof Float)
					valuecode += "'"+me.getValue()+"'";
				else
					valuecode += ""+me.getValue();			
				valuecode += ""+(i.hasNext()?",":")");								
			}
			code += keycode;		
			code += valuecode;
			try
			{
				s = cntx.createStatement();
				int rs = s.executeUpdate(code);
				if (rs == 0)
					return false;
				return true;
			}
			catch (SQLException e)
			{ log.info("Towny SQL: Insert sql error " + e.getMessage()+ " --> "+code); }
			return false;
		}
		else
		{
			code = "UPDATE "+tb_prefix+tb_name+" SET ";
			Set<Map.Entry<String,Object>> set = args.entrySet();			
			Iterator<Map.Entry<String,Object>> i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry<String,Object> me = (Map.Entry<String,Object>)i.next();
				code += me.getKey()+" = ";
				if (me.getValue() instanceof String)
					code += "'"+me.getValue()+"'";				
				else
					code += ""+me.getValue();
				code += ""+(i.hasNext()?",":"");
			}
			code += " WHERE ";
			
			Iterator<String> keys_i = keys.iterator();
			while (keys_i.hasNext())
			{				
				String key = (String)keys_i.next();
				code += key+" = ";			
				Object v = args.get(key);
				if (v instanceof String)
					code += "'"+v+"'";	
				else
					code += v;
				code += ""+(keys_i.hasNext()?" AND ":"");
			}
			
			try
			{
				s = cntx.createStatement();
				int rs = s.executeUpdate(code);
				if (rs == 0) // if entry dont exist then try to insert 
					return UpdateDB(tb_name, args, null);	
				return true;
			}
			catch (SQLException e)
			{ log.info("Towny SQL: Update sql error " + e.getMessage()+ " --> "+code); }
		}			
		return false;
	}
	public boolean DeleteDB(String tb_name, HashMap<String,Object> args)
	{
		if (!getContext()) return false;
		try
		{
			String wherecode = "DELETE FROM "+tb_prefix+tb_name+" WHERE ";
			Set<Map.Entry<String,Object>> set = args.entrySet();			
			Iterator<Map.Entry<String,Object>> i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry<String,Object> me = (Map.Entry<String,Object>)i.next();
				wherecode += me.getKey() + " = ";
				if (me.getValue() instanceof String)
					wherecode += "'"+me.getValue()+"'";
				else if (me.getValue() instanceof Float)
					wherecode += "'"+me.getValue()+"'";
				else
					wherecode += ""+me.getValue();
				
				wherecode += ""+(i.hasNext()?" AND ":"");	
			}
			Statement s = cntx.createStatement();
			int rs = s.executeUpdate(wherecode);
			if (rs == 0)
			{
				log.info("Towny SQL: delete returned 0: "+wherecode);
			}
		} catch (SQLException e) { System.out.println("Towny SQL: Error delete : "+e.getMessage()); }
		return false;
	}
	/*
	* Save individual towny objects
	*/
	
	@Override
	public boolean saveResident(Resident resident) {
		sendDebugMsg("Saving Resident");				
		try {						
			HashMap<String, Object> res_hm = new HashMap<String, Object>();
			res_hm.put("name", resident.getName());
			res_hm.put("town", resident.hasTown()?resident.getTown().getName():"");
			res_hm.put("name", resident.getName());						
			res_hm.put("lastOnline", resident.getLastOnline()); 
			res_hm.put("registered", resident.getRegistered());				
			res_hm.put("isNPC", (resident.isNPC()?1:0));
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("protectionStatus", resident.getPermissions().toString());
			UpdateDB("residents", res_hm, Arrays.asList("name"));								
							
			String fstr = "";
			for (Resident friend : resident.getFriends())
				fstr += friend.getName() + ",";
			res_hm.put("friends", fstr);
					
			for (TownBlock tb : resident.getTownBlocks())
				saveTownBlock(tb);			
			return true;
		}		
		catch (Exception e)
		{ log.info("Towny SQL: Save Resident unknown error " + e.getMessage()); }
		return false;
	}
	
	
	@Override
	public boolean saveTown(Town town) {
		sendDebugMsg("Saving town "+town.getName());		
		try {			
				HashMap<String, Object> twn_hm = new HashMap<String, Object>();
				twn_hm.put("name", town.getName());
				twn_hm.put("world", town.getWorld().getName());
				twn_hm.put("mayor", town.hasMayor()?town.getMayor().getName():"");
				twn_hm.put("nation", town.hasNation()?town.getNation().getName():"");
				twn_hm.put("bonus", town.getBonusBlocks());
				twn_hm.put("purchased", town.getPurchasedBlocks());
				twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
				twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
				twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
				twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
				twn_hm.put("plotPrice", town.getPlotPrice());
				twn_hm.put("plotTax", town.getPlotTax());
				twn_hm.put("taxes", town.getTaxes());
				twn_hm.put("hasUpkeep", town.hasUpkeep()?1:0);
				twn_hm.put("open", town.isOpen()?1:0);
				twn_hm.put("public", town.isPublic()?1:0);
				twn_hm.put("board", town.getTownBoard());	
				twn_hm.put("protectionStatus", town.getPermissions().toString());	
				if (town.hasSpawn())
				{
					twn_hm.put("spawn_x", town.getSpawn().getX());
					twn_hm.put("spawn_y", town.getSpawn().getY());
					twn_hm.put("spawn_z", town.getSpawn().getZ());
					twn_hm.put("spawn_pitch", town.getSpawn().getPitch());
					twn_hm.put("spawn_yaw", town.getSpawn().getYaw());
				}				
				if (town.hasHomeBlock())
				{
					twn_hm.put("homeblock_x", town.getHomeBlock().getX());
					twn_hm.put("homeblock_z", town.getHomeBlock().getZ());
				}
				String fstr = "";
				for (Resident assist : town.getAssistants())
					fstr += assist.getName() + ",";
				twn_hm.put("assistants", fstr);
				
				// Outpost Spawns
				if (town.hasOutpostSpawn()) {
					String outpostArray = "";
					for (Location spawn : new ArrayList<Location>(town.getAllOutpostSpawns())) {
						outpostArray += (spawn.getWorld().getName() + ","
							+ Double.toString(spawn.getX()) + ","
							+ Double.toString(spawn.getY()) + ","
							+ Double.toString(spawn.getZ()) + ","
							+ Float.toString(spawn.getPitch()) + ","
							+ Float.toString(spawn.getYaw()) + ";");
						}
						twn_hm.put("outpostSpawns", outpostArray);
					}
						
						
				UpdateDB("towns", twn_hm, Arrays.asList("name"));
				for (TownBlock tb : town.getTownBlocks())				
					saveTownBlock(tb);									
				return true;
		}		
		catch (Exception e) 
		{ log.info("Towny SQL: Save Town unknown error"); e.printStackTrace(); }
		return false;
	}
	
	@Override
	public boolean saveNation(Nation nation) {
		sendDebugMsg("Saving nation "+nation.getName());		
		try {
			HashMap<String, Object> nat_hm = new HashMap<String, Object>();
			nat_hm.put("name", nation.getName());
			nat_hm.put("capital", nation.hasCapital()?nation.getCapital().getName():"");			
			nat_hm.put("taxes", nation.getTaxes());
			UpdateDB("nations", nat_hm, Arrays.asList("name"));
		}		
		catch (Exception e) 
		{ log.info("Towny SQL: Save Nation unknown error"); e.printStackTrace(); }
		return false;
	}
	
	@Override
	public boolean saveWorld(TownyWorld world) {
		try {
			sendDebugMsg("Saving world - " + getWorldFilename(world));
			
			String path = getWorldFilename(world);
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
									
			fout.write(newLine);
			fout.write(newLine);
			
			// PvP
			fout.write("pvp=" + Boolean.toString(world.isPVP()) + newLine);
			// Force PvP
			fout.write("forcepvp=" + Boolean.toString(world.isForcePVP()) + newLine);
			// Claimable
			fout.write("# Can players found towns and claim plots in this world?" + newLine);
			fout.write("claimable=" + Boolean.toString(world.isClaimable()) + newLine);
			// has monster spawns			
			fout.write("worldmobs=" + Boolean.toString(world.hasWorldMobs()) + newLine);
			// force town mob spawns			
			fout.write("forcetownmobs=" + Boolean.toString(world.isForceTownMobs()) + newLine);
			// has firespread enabled
			fout.write("firespread=" + Boolean.toString(world.isFire()) + newLine);
			fout.write("forcefirespread=" + Boolean.toString(world.isForceFire()) + newLine);
			// has explosions enabled
			fout.write("explosions=" + Boolean.toString(world.isExpl()) + newLine);
			fout.write("forceexplosions=" + Boolean.toString(world.isForceExpl()) + newLine);
			// Enderman block protection
			fout.write("endermanprotect=" + Boolean.toString(world.isEndermanProtect()) + newLine);
			// PlayerTrample
			fout.write("disableplayertrample=" + Boolean.toString(world.isDisablePlayerTrample()) + newLine);
			// CreatureTrample
			fout.write("disablecreaturetrample=" + Boolean.toString(world.isDisableCreatureTrample()) + newLine);

			// Unclaimed
			fout.write(newLine);
			fout.write("# Unclaimed Zone settings." + newLine);
			
			// Unclaimed Zone Build
			if (world.getUnclaimedZoneBuild() != null)
				fout.write("unclaimedZoneBuild=" + Boolean.toString(world.getUnclaimedZoneBuild()) + newLine);
			// Unclaimed Zone Destroy
			if (world.getUnclaimedZoneDestroy() != null)
				fout.write("unclaimedZoneDestroy=" + Boolean.toString(world.getUnclaimedZoneDestroy()) + newLine);
			// Unclaimed Zone Switch
			if (world.getUnclaimedZoneSwitch() != null)
				fout.write("unclaimedZoneSwitch=" + Boolean.toString(world.getUnclaimedZoneSwitch()) + newLine);
			// Unclaimed Zone Item Use
			if (world.getUnclaimedZoneItemUse() != null)
				fout.write("unclaimedZoneItemUse=" + Boolean.toString(world.getUnclaimedZoneItemUse()) + newLine);
			// Unclaimed Zone Name
			if (world.getUnclaimedZoneName() != null)
				fout.write("unclaimedZoneName=" + world.getUnclaimedZoneName() + newLine);
			
			fout.write(newLine);
			fout.write("# The following settings are only used if you are not using any permissions provider plugin" + newLine);
			
			// Unclaimed Zone Ignore Ids
			if (world.getUnclaimedZoneIgnoreIds() != null)
				fout.write("unclaimedZoneIgnoreIds=" + StringMgmt.join(world.getUnclaimedZoneIgnoreIds(), ",") + newLine);
			
			// PlotManagement Delete
			fout.write(newLine);
			fout.write("# The following settings control what blocks are deleted upon a townblock being unclaimed" + newLine);
						
			// Using PlotManagement Delete
			fout.write("usingPlotManagementDelete=" + Boolean.toString(world.isUsingPlotManagementDelete()) + newLine);
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				fout.write("plotManagementDeleteIds=" + StringMgmt.join(world.getPlotManagementDeleteIds(), ",") + newLine);
			
			// PlotManagement
			fout.write(newLine);
			fout.write("# The following settings control what blocks are deleted upon a mayor issuing a '/plot clear' command" + newLine);
						
			// Using PlotManagement Mayor Delete
			fout.write("usingPlotManagementMayorDelete=" + Boolean.toString(world.isUsingPlotManagementMayorDelete()) + newLine);
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				fout.write("plotManagementMayorDelete=" + StringMgmt.join(world.getPlotManagementMayorDelete(), ",") + newLine);
			
			// PlotManagement Revert
			fout.write(newLine + "# If enabled when a town claims a townblock a snapshot will be taken at the time it is claimed." + newLine);
			fout.write("# When the townblock is unclaimded its blocks will begin to revert to the original snapshot." + newLine);
						
			// Using PlotManagement Revert
			fout.write("usingPlotManagementRevert=" + Boolean.toString(world.isUsingPlotManagementRevert()) + newLine);
			// Using PlotManagement Revert Speed
			fout.write("usingPlotManagementRevertSpeed=" + Long.toString(world.getPlotManagementRevertSpeed()) + newLine);
			
			fout.write("# Any block Id's listed here will not be respawned. Instead it will revert to air." + newLine);
			
			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				fout.write("plotManagementIgnoreIds=" + StringMgmt.join(world.getPlotManagementIgnoreIds(), ",") + newLine);
			
			// PlotManagement Wild Regen
			fout.write(newLine);
			fout.write("# If enabled any damage caused by explosions will repair itself." + newLine);
			
			// Using PlotManagement Wild Regen
			fout.write("usingPlotManagementWildRegen=" + Boolean.toString(world.isUsingPlotManagementWildRevert()) + newLine);
			
			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				fout.write("PlotManagementWildRegenEntities=" + StringMgmt.join(world.getPlotManagementWildRevertEntities(), ",") + newLine);
			
			
			
			
			// Using PlotManagement Wild Regen Delay
			fout.write("usingPlotManagementWildRegenDelay=" + Long.toString(world.getPlotManagementWildRevertDelay()) + newLine);
			
			// Using Towny
			fout.write(newLine);
			fout.write("# This setting is used to enable or disable Towny in this world." + newLine);
						
			// Using Towny
			fout.write("usingTowny=" + Boolean.toString(world.isUsingTowny()) + newLine);
			
			fout.close();

			// saveTownBlocks(world);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public boolean saveTownBlock(TownBlock townBlock) {
		sendDebugMsg("Saving town block "+townBlock.getX()+"x"+townBlock.getZ());
		try
		{
			HashMap<String, Object> tb_hm = new HashMap<String, Object>();
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());														
			tb_hm.put("world", townBlock.hasTown()?townBlock.getTown().getWorld().getName():"");
			tb_hm.put("town", townBlock.hasTown()?townBlock.getTown().getName():"");
			tb_hm.put("owner", townBlock.hasResident()?townBlock.getResident().getName():"");
			tb_hm.put("protectionStatus", townBlock.getPermissions().toString());
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("isOutpost", townBlock.isOutpost()?1:0);
			tb_hm.put("plotPrice", townBlock.getPlotPrice());
			tb_hm.put("plotType", townBlock.getType().getId());
			UpdateDB("townblocks", tb_hm, Arrays.asList("x","z"));
		}
		catch (Exception e) 
		{ log.info("Towny SQL: Save TownBlock unknown error"); e.printStackTrace(); }		
		return true;	
	}
	@Override
	public void deleteResident(Resident resident) {
		HashMap<String, Object> res_hm = new HashMap<String, Object>();
		res_hm.put("name", resident.getName());		
		DeleteDB("residents", res_hm);
	}
	
	@Override
	public void deleteTown(Town town) {
		HashMap<String, Object> twn_hm = new HashMap<String, Object>();
		twn_hm.put("name", town.getName());		
		DeleteDB("towns", twn_hm);
	}
	
	@Override
	public void deleteNation(Nation nation) {
		HashMap<String, Object> nat_hm = new HashMap<String, Object>();
		nat_hm.put("name", nation.getName());		
		DeleteDB("nations", nat_hm);
	}
	@Override
	public void deleteTownBlock(TownBlock townBlock) {
		HashMap<String, Object> twn_hm = new HashMap<String, Object>();
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("townblocks", twn_hm);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/* Original FlatFile DB Stuff
	 * 
	 * PlotData and regenlist should stay on hd for speed purpose ... maybe ramfs ? 
	 */
	
	
	@Override
	public boolean saveRegenList() {
		try {
			
			//System.out.print("[Towny] save active regen list");
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt"));
			for (PlotBlockData plot : new ArrayList<PlotBlockData>(TownyRegenAPI.getPlotChunks().values()))
				fout.write(plot.getWorldName() + "," + plot.getX() + "," + plot.getZ() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Saving Error: Exception while saving regen file");
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean saveSnapshotList() {
		try {
			
			//System.out.print("[Towny] save active snapshot queue");
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt"));
			while (TownyRegenAPI.hasWorldCoords()) {
				WorldCoord worldCoord = TownyRegenAPI.getWorldCoord();
				fout.write(worldCoord.getWorld().getName() + "," + worldCoord.getX() + "," + worldCoord.getZ() + newLine);
			}
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Saving Error: Exception while saving snapshot_queue file");
			e.printStackTrace();
			return false;
		}
	}
	@Override
	public boolean loadRegenList() {
		sendDebugMsg("Loading Regen List");

		String line;
		BufferedReader fin;
		String[] split;
		PlotBlockData plotData;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {
					split = line.split(",");
					plotData = loadPlotData(split[0],Integer.parseInt(split[1]),Integer.parseInt(split[2]));
                	if (plotData != null) {
                		TownyRegenAPI.addPlotChunk(plotData, false);
                	}
				}
			

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				fin.close();
			} catch (IOException e) {
				// Failed to close file.
			}
		}
		return true;

	}
	
	@Override
	public boolean loadSnapshotList() {
		sendDebugMsg("Loading Snapshot Queue");

		String line;
		BufferedReader fin;
		String[] split;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {
					split = line.split(",");
					TownyWorld world = getWorld(split[0]);
					WorldCoord worldCoord = new WorldCoord(world, Integer.parseInt(split[1]),Integer.parseInt(split[2]));
					TownyRegenAPI.addWorldCoord(worldCoord);
				}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				fin.close();
			} catch (IOException e) {
				// Failed to close file.
			}
		}
		
		return true;

	}

	/**
	 * Save PlotBlockData
	 * 
	 * @param plotChunk
	 * @return true if saved
	 */
	@Override
	public boolean savePlotData(PlotBlockData plotChunk) {
		
		FileMgmt.checkFolders(new String[]{
				rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() + plotChunk.getWorldName()});
		
		BufferedWriter fout;
		String path = getPlotFilename(plotChunk);
		try {
			fout = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			
			switch (plotChunk.getVersion()) {
			
			case 1:
				/*
				 * New system requires pushing
				 * version data first
				 */
				fout.write("VER");
				fout.write(plotChunk.getVersion());
				
				break;
				
			default:
				
			}
			
			// Push the plot height, then the plot block data types.
			fout.write(plotChunk.getHeight());
			for (int block: new ArrayList<Integer>(plotChunk.getBlockList())) {
				fout.write(block);
			}
						
			fout.close();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;		
		
	}
	
	/**
	 * Load PlotBlockData
	 * 
	 * @param worldName
	 * @param x
	 * @param z
	 * @return PlotBlockData or null
	 */
	@Override
	public PlotBlockData loadPlotData(String worldName, int x, int z) {
		
		try {
			TownyWorld world = getWorld(worldName);
			TownBlock townBlock = new TownBlock(x,z,world);
			
			return loadPlotData(townBlock);
		} catch (NotRegisteredException e) {
			// Failed to get world
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Load PlotBlockData for regen at unclaim
	 * 
	 * @param townBlock
	 * @return PlotBlockData or null
	 */
	@Override
	public PlotBlockData loadPlotData(TownBlock townBlock) {
		String fileName = getPlotFilename(townBlock);

		int value;
		
		if (isFile(fileName)) {
			PlotBlockData plotBlockData = new PlotBlockData(townBlock);
			List<Integer>IntArr = new ArrayList<Integer>();
			
			try {
				BufferedReader fin = new BufferedReader(new FileReader(fileName));
				try {
					//read the first 3 characters to test for version info
					char[] key = new char[3];
					fin.read(key,0,3);
					String test = new String(key);
					
					switch (elements.fromString(test)) {
					case VER:
						// Read the file version
						int version = fin.read();
						plotBlockData.setVersion(version);
						
						// next entry is the plot height
						plotBlockData.setHeight(fin.read());
						break;
						
					default:
						/*
						 * no version field so set height
						 * and push rest to queue
						 * 
						 */
						plotBlockData.setVersion(0);
						// First entry is the plot height
						plotBlockData.setHeight(key[0]);
						IntArr.add((int) key[1]);
						IntArr.add((int) key[2]);
					}
					
					// load remainder of file
					while ((value = fin.read()) >= 0) {
						IntArr.add(value);	
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			plotBlockData.setBlockList(IntArr);
			plotBlockData.resetBlockListRestored();
			return plotBlockData;
		}
		return null;
	}
	
	@Override
	public void deletePlotData(PlotBlockData plotChunk) {
		File file = new File(getPlotFilename(plotChunk));
		if (file.exists())
			file.delete();
	}
	private boolean isFile(String fileName) {
		File file = new File(fileName);
		if (file.exists() && file.isFile())
			return true;
		
		return false;
	}	
	public String getPlotFilename(PlotBlockData plotChunk) {
		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() +  plotChunk.getWorldName()
				+ FileMgmt.fileSeparator() + plotChunk.getX() + "_" + plotChunk.getZ()  + "_" + plotChunk.getSize() + ".data";
	}

	public String getPlotFilename(TownBlock townBlock) {
		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() +  townBlock.getWorld().getName()
				+ FileMgmt.fileSeparator() + townBlock.getX() + "_" + townBlock.getZ()  + "_" + TownySettings.getTownBlockSize() + ".data";
	}
	public String getWorldFilename(TownyWorld world) {
		return rootFolder + dataFolder + FileMgmt.fileSeparator() +  "worlds" + FileMgmt.fileSeparator() + world.getName() + ".txt";
	}
	
	
	
	/// Unused ...
	@Override
	public void deleteFile(String fileName) {
		File file = new File(fileName);
		if (file.exists())
			file.delete();
	}
		
	@Override
	public void backup() throws IOException {
		System.out.println("Doing MySQL backup");
		System.out.println("***** Warning *****");
		System.out.println("***** Only Snapshots and Regens files will be backuped");
		System.out.println("***** Make sure you schedule a backup in MySQL too!!!");
		String backupType = TownySettings.getFlatFileBackupType();
		if (!backupType.equalsIgnoreCase("none")) {
			
			TownyLogger.shutDown();
			
			long t = System.currentTimeMillis();
			String newBackupFolder = rootFolder + FileMgmt.fileSeparator() + "backup" + FileMgmt.fileSeparator() + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + Long.toString(t);
			FileMgmt.checkFolders(new String[]{ rootFolder, rootFolder + FileMgmt.fileSeparator() + "backup" });
			if (backupType.equalsIgnoreCase("folder")) {
				FileMgmt.checkFolders(new String[]{newBackupFolder});
				FileMgmt.copyDirectory(new File(rootFolder + dataFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + logFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + settingsFolder), new File(newBackupFolder));
			} else if (backupType.equalsIgnoreCase("zip"))
				FileMgmt.zipDirectories(new File[]{
						new File(rootFolder + dataFolder),
						new File(rootFolder + logFolder),
						new File(rootFolder + settingsFolder)
						}, new File(newBackupFolder + ".zip"));
			else {
				plugin.setupLogger();
				throw new IOException("Unsupported flatfile backup type (" + backupType + ")");
			}
			plugin.setupLogger();
		}
	}
		
	@Override
	public void cleanupBackups() {
		long deleteAfter = TownySettings.getBackupLifeLength();
		if (deleteAfter >= 0)
			FileMgmt.deleteOldBackups(new File(rootFolder + FileMgmt.fileSeparator() + "backup"), deleteAfter);
	}
	
	@Override
	public void deleteUnusedResidentFiles() {
	}
	@Override
	public void deleteWorld(TownyWorld world) {
		File file = new File(getWorldFilename(world));
		if (file.exists()){
			try {
				FileMgmt.moveFile(file, ("deleted"));
			} catch (IOException e) {
				System.out.println("[Towny] Error moving World txt file.");
			}
		}
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