/*
 * Towny MYSQL Source by StPinker
 * 
 * Released under LGPL
 * 
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

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
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
import java.util.logging.Logger;

public class TownySQLSource extends TownyFlatFileSource
{
	private Logger log = Logger.getLogger("Minecraft");

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
	 * Flag if we are using h2 or standard SQL conectivity.
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
			FileMgmt.checkFolders(new String[]{
					rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds" + FileMgmt.fileSeparator() + "deleted"
			});
			FileMgmt.checkFiles(new String[]{
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"
			});		
		} catch (IOException e) {
			log.info("[Towny] Error: Could not create flatfile default files and folders.");
		}
		
		//Setup SQL connection
		hostname = TownySettings.getSQLHostName();		
		port = TownySettings.getSQLPort();
		db_name = TownySettings.getSQLDBName();
		tb_prefix = TownySettings.getSQLTablePrefix();	
		
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
		//System.out.println("Checking for tables existence");
		DatabaseMetaData dbm;
		if (getContext())				
			System.out.println("[Towny] Connected to Database");		
		else		
		{
			System.out.println("[Towny] Error: Failed when connecting to Database");
			return;
		}
		try  { dbm = cntx.getMetaData(); }
		catch (SQLException e) { System.out.println("[Towny] Error: Cannot get Table metadata"); return; }
		
		String[] types = {"TABLE"};

		try 
		{ 
			ResultSet town_table = dbm.getTables(null, null, (tb_prefix+"towns").toUpperCase(), types); 
			if (town_table.next()) {
				//System.out.println("[Towny] Table towns is ok!");
			} else {				
				String town_create = 
						  "CREATE TABLE "+tb_prefix+"towns ("+						  
						  "`name` mediumtext NOT NULL,"+
						  "`residents` mediumtext,"+						  
						  "`mayor` mediumtext,"+
						  "`nation` mediumtext NOT NULL,"+
						  "`assistants` text  DEFAULT NULL,"+
						  "`townBoard` mediumtext DEFAULT NULL,"+
						  "`tag` mediumtext DEFAULT NULL,"+
						  "`protectionStatus` mediumtext DEFAULT NULL,"+						  
						  "`bonus` int(11)  DEFAULT 0,"+
						  "`purchased` int(11)  DEFAULT 0,"+						  						 
						  "`taxpercent` tinyint(1) DEFAULT NULL,"+
						  "`taxes` float  DEFAULT 0,"+						  
						  "`hasUpkeep` tinyint(1)  DEFAULT 0,"+
						  "`plotPrice` float DEFAULT NULL,"+
						  "`plotTax` float  DEFAULT NULL,"+
						  "`commercialPlotPrice` float  DEFAULT NULL,"+
						  "`commercialPlotTax` float NOT NULL,"+
						  "`embassyPlotPrice` float NOT NULL,"+
						  "`embassyPlotTax` float NOT NULL,"+
						  "`open` tinyint(1) NOT NULL,"+
						  "`public` tinyint(1) NOT NULL,"+
						  "`homeblock` mediumtext NOT NULL,"+
						  "`townBlocks` mediumtext NOT NULL,"+
						  "`spawn` mediumtext NOT NULL,"+
						  "`outpostSpawns` mediumtext DEFAULT NULL,"+
						  "PRIMARY KEY (`name`)"+
						")";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(town_create);					
				} catch (SQLException ee) { System.out.println("[Towny] Error Creating table towns :" + ee.getMessage()); }				
			}			
		}
		catch (SQLException e)
		{ System.out.println("[Towny] Error Checking table towns :" + e.getMessage()); }
		
		
		try 
		{ 
			ResultSet res_table = dbm.getTables(null, null, (tb_prefix+"residents").toUpperCase(), types); 
			if (res_table.next()) {
				//System.out.println("[Towny] Table residents is ok!");
			} else {			
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
						  "`townBlocks` mediumtext,"+
						  "PRIMARY KEY (`name`)"+
						")";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(resident_create);
				} catch (SQLException ee) { System.out.println("[Towny] Error Creating table residents :" + ee.getMessage()); }
			}
		} 
		catch (SQLException e)
		{ System.out.println("[Towny] Error Checking table residents :" + e.getMessage()); }
		
		try 
		{ 
			ResultSet nat_table = dbm.getTables(null, null, (tb_prefix+"nations").toUpperCase(), types); 
			if (nat_table.next()) {
				//System.out.println("[Towny] Table nations is ok!");
			}
			else 
			{					
				String nation_create = 
						"CREATE TABLE "+tb_prefix+"nations ("+
						"`name` mediumtext NOT NULL,"+	
						"`towns` mediumtext NOT NULL,"+
						"`capital` mediumtext NOT NULL,"+
						"`assistants` mediumtext NOT NULL,"+
						"`tag` mediumtext NOT NULL,"+
						"`allies` mediumtext NOT NULL,"+
						"`enemies` mediumtext NOT NULL,"+
						"`taxes` float NOT NULL,"+
						"`neutral` tinyint(1) NOT NULL, "+
						"PRIMARY KEY (`name`)"+
						")";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(nation_create);
				} catch (SQLException ee) { System.out.println("[Towny] Error Creating table nations : " + ee.getMessage()); }
			}
		}
		catch (SQLException e)
		{ System.out.println("[Towny] Error Checking table nations :" + e.getMessage()); }
		
		try 
		{ 
			ResultSet tb_table = dbm.getTables(null, null, (tb_prefix+"townblocks").toUpperCase(), types); 			
			if (tb_table.next()) {
				//System.out.println("[Towny] Table townblocks is ok!");
			}
			else 
			{
				String townblock_create = 
						"CREATE TABLE "+tb_prefix+"townblocks ("+						
						"`world` mediumtext NOT NULL,"+
						"`x` bigint(20) NOT NULL,"+
						"`z` bigint(20) NOT NULL,"+												
						"`permissions` mediumtext NOT NULL,"+
						"`locked` bool NOT NULL DEFAULT '0',"+
						"`changed` tinyint(1) NOT NULL DEFAULT '0',"+						
						"PRIMARY KEY (`world`,`x`,`z`)"+
						")";
				try {				
					Statement s = cntx.createStatement();
					s.executeUpdate(townblock_create);
				} catch (SQLException ee) { System.out.println("[Towny] Error Creating table townblocks : " + ee.getMessage()); }
			}
		}
		catch (SQLException e)
		{ System.out.println("[Towny] Error Checking table townblocks :" + e.getMessage()); }
		
		//System.out.println("Checking done!");
	}
	
	/**
	 * open a connection to the SQL server.
	 * 
	 * @return
	 */
	public boolean getContext()
	{
		try
		{
			if (cntx==null || cntx.isClosed()) {
				if (ish2) {
					cntx = DriverManager.getConnection(this.dsn, this.username, this.password);
				} else
					cntx = DriverManager.getConnection(this.dsn);
			}
			
			if (cntx==null || cntx.isClosed())
				return false;
			return true;
		}
		catch (SQLException e) 
		{
			log.info("Error could not Connect to db "+this.dsn+": "+e.getMessage());
		}
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
				else if (me.getValue() instanceof Double)
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
				else if (me.getValue() instanceof Float)
					code += "'"+me.getValue()+"'";
				else if (me.getValue() instanceof Double)
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
	* Load keys
	*/
	
	@Override
	public boolean loadResidentList() {
		TownyMessaging.sendDebugMsg("Loading Resident List");
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
		TownyMessaging.sendDebugMsg("Loading Town List");
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
			{ log.info("[Towny] SQL: town list sql error : "+e.getMessage()); }				
			catch (Exception e) 
			{ log.info("[Towny] SQL: town list unknown error: ");e.printStackTrace(); }				
		return false;
	}
	
	@Override
	public boolean loadNationList() {
		TownyMessaging.sendDebugMsg("Loading Nation List");	
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
			{ log.info("[Towny] SQL: nation list sql error : "+e.getMessage()); }
			catch (Exception e)
			{ log.info("[Towny] SQL: nation list unknown error: ");e.printStackTrace();}		
		return false;	
	}
	
	/*
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
	*/
	
	
	/*
	* Load individual towny object
	*/
	
	@Override
	public boolean loadResident(Resident resident)
	{		
		TownyMessaging.sendDebugMsg("Loading resident "+resident.getName());	
		if (!getContext()) return false;							
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT " +
						" lastOnline,registered,isNPC,title,surname,town,friends,protectionStatus,townBlocks" +
						" FROM "+tb_prefix+"residents " +
						" WHERE name='"+resident.getName()+"'");								
				while (rs.next())
    			{			
					try {
					resident.setLastOnline(rs.getLong("lastOnline"));
					} catch (Exception e) { e.printStackTrace(); }
					try {
					resident.setRegistered(rs.getLong("registered"));					
					} catch (Exception e) { e.printStackTrace(); }
					try {
					resident.setNPC(rs.getBoolean("isNPC"));
					} catch (Exception e) { e.printStackTrace(); }
					try {
					resident.setTitle(rs.getString("title"));
					} catch (Exception e) { e.printStackTrace(); }
					try {
					resident.setSurname(rs.getString("surname"));
					} catch (Exception e) { e.printStackTrace(); }
					
					String line = rs.getString("town");
					if ((line != null) && (!line.isEmpty()))
					{
						resident.setTown(getTown(line));
						//System.out.println("Resident "+resident.getName()+" set to Town "+rs.getString("town"));
					}
					
					try {
					line = rs.getString("friends");
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
					} catch (Exception e) {e.printStackTrace(); }
					try {
					resident.setPermissions(rs.getString("protectionStatus"));
					} catch (Exception e) {e.printStackTrace(); }
					
					line = rs.getString("townBlocks");
					if ((line != null) && (!line.isEmpty()))
						utilLoadTownBlocks(line, null, resident);					
					return true;
    			}
				return false;				
			}
			catch (SQLException e)
			{ log.info("[Towny] SQL: Load resident sql error : "+e.getMessage()); }
			catch (Exception e)
			{ log.info("[Towny] SQL: Load resident unknown error");e.printStackTrace();}		
		return false;
	}
	
	@Override
	public boolean loadTown(Town town)
	{
		String line;
		String[] tokens;
		TownyMessaging.sendDebugMsg("Loading town "+town.getName());	
		if (!getContext()) return false;
						
			try
			{
				Statement s = cntx.createStatement();								
				 ResultSet rs = s.executeQuery("SELECT " +
				"residents,mayor,assistants,townBoard,nation,tag,protectionStatus,bonus,purchased,plotPrice,hasUpkeep,taxpercent,taxes" +				
				",plotTax,commercialPlotPrice,commercialPlotTax,embassyPlotPrice,embassyPlotTax," +
				"open,public,townBlocks, homeBlock, spawn,outpostSpawns " +
				" FROM "+tb_prefix+"towns " +
				" WHERE name='"+town.getName()+"'");
				while (rs.next())
    			{					
						
						line = rs.getString("residents");
						if (line != null) {
							tokens = line.split(",");
							for (String token : tokens) {
								if (!token.isEmpty()){
									Resident resident = getResident(token);
									if (resident != null)
										town.addResident(resident);
								}
							}
						}
						town.setMayor( getResident(rs.getString("mayor")) );	
						line = rs.getString("assistants");
						if (line != null) {
							tokens = line.split(",");
							for (String token : tokens) {
								if (!token.isEmpty()){
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
							} catch(TownyException e) {
								town.setTag("");
							}
						town.setPermissions(rs.getString("protectionStatus"));
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
										System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid location.");
									} catch (NotRegisteredException e) {
										System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
									} catch (TownyException e) {
										System.out.println("[Towny] [Warning] " + town.getName() + " does not have a home block.");
									}
									
								} catch (NotRegisteredException e) {
									System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid world.");
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
									System.out.println("[Towny] [Warning] " + town.getName() + " does not have a spawn point.");
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
			}
			catch (SQLException e)
			{  log.info("[Towny] SQL: Load Town sql Error - "+e.getMessage()); }
			catch (Exception e)
			{  log.info("[Towny] SQL: Load Town unknown Error - ");e.printStackTrace(); }		
		return false;
	}
	
	
	@Override
	public boolean loadNation(Nation nation) {
		String line = "";
		String[] tokens;
		TownyMessaging.sendDebugMsg("Loading nation "+nation.getName());	
		if (!getContext()) return false;							
			try
			{
				Statement s = cntx.createStatement();
				ResultSet rs = s.executeQuery("SELECT towns,capital,assistants,tag,allies,enemies,taxes,neutral FROM "+tb_prefix+"nations WHERE name='"+nation.getName()+"'");
				while (rs.next())
    			{
					line = rs.getString("towns");
					if (line != null) {
						tokens = line.split(",");
						for (String token : tokens) {
							if (!token.isEmpty()){
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
							if (!token.isEmpty()){
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
							if (!token.isEmpty()){
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
							if (!token.isEmpty()){
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
			}
			catch (SQLException e)
			{  log.info("[Towny] SQL: Load Nation sql error "+e.getMessage()); }
			catch (Exception e)
			{  log.info("[Towny] SQL: Load Nation unknown error - ");e.printStackTrace(); }		
		return false;
	}
	
	@Override
	public boolean loadTownBlocks() {

		String line = "";
		Boolean result = false;
		// Load town blocks
		if (!getContext()) return false;		
		ResultSet rs;
		for (TownBlock townBlock : getAllTownBlocks()) 
		{
				boolean set = false;
				try
				{
					Statement s = cntx.createStatement();
					rs = s.executeQuery("SELECT " +
							"permissions,locked,changed" +				
							" FROM "+tb_prefix+"townblocks" +
							" WHERE world='"+townBlock.getWorld().getName()+"' AND x='"+townBlock.getX()+"' AND z='"+townBlock.getZ()+"'");
					while (rs.next())
					{
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
								if (townBlock.hasResident()){
									townBlock.setPermissions(townBlock.getResident().getPermissions().toString());
								} else {
									townBlock.setPermissions(townBlock.getTown().getPermissions().toString());
								}
							} catch (NotRegisteredException e) {
								// Will never reach here
							}
						}
					}					
				}
				catch (SQLException e) 
				{ 
					System.out.println("[Towny] Loading Error: Exception while reading TownBlocks ");
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
	public boolean saveResident(Resident resident) {
		TownyMessaging.sendDebugMsg("Saving Resident");				
		try {						
			HashMap<String, Object> res_hm = new HashMap<String, Object>();
			res_hm.put("name", resident.getName());
			res_hm.put("lastOnline", resident.getLastOnline()); 
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("town", resident.hasTown()?resident.getTown().getName():"");						
			String fstr = "";
			for (Resident friend : resident.getFriends())
				fstr += friend.getName() + ",";
			res_hm.put("friends", fstr);							
			res_hm.put("townBlocks", utilSaveTownBlocks(new ArrayList<TownBlock>(resident.getTownBlocks())));
			res_hm.put("protectionStatus", resident.getPermissions().toString());
			UpdateDB("residents", res_hm, Arrays.asList("name"));																													
			return true;
		}		
		catch (Exception e)
		{ log.info("[Towny] SQL: Save Resident unknown error " + e.getMessage()); }
		return false;
	}
	
	
	@Override
	public boolean saveTown(Town town) {
		TownyMessaging.sendDebugMsg("Saving town "+town.getName());		
		try {			
				HashMap<String, Object> twn_hm = new HashMap<String, Object>();
				twn_hm.put("name", town.getName());
				twn_hm.put("residents", StringMgmt.join(town.getResidents(), ","));							
				twn_hm.put("mayor", town.hasMayor()?town.getMayor().getName():"");
				twn_hm.put("nation", town.hasNation()?town.getNation().getName():"");
				String fstr = "";
				for (Resident assist : town.getAssistants())
					fstr += assist.getName() + ",";
				twn_hm.put("assistants", fstr);
				twn_hm.put("townBoard", town.getTownBoard());
				twn_hm.put("tag", town.getTag());
				twn_hm.put("protectionStatus",town.getPermissions().toString());
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
				twn_hm.put("open", town.isOpen());
				twn_hm.put("public", town.isPublic());	
				twn_hm.put("townBlocks", utilSaveTownBlocks(new ArrayList<TownBlock>(town.getTownBlocks())));
				twn_hm.put("homeblock", town.hasHomeBlock()?town.getHomeBlock().getWorld().getName() + ","
						+ Integer.toString(town.getHomeBlock().getX()) + ","
						+ Integer.toString(town.getHomeBlock().getZ()):"");
				twn_hm.put("spawn", town.hasSpawn()?town.getSpawn().getWorld().getName() + ","
						+ Double.toString(town.getSpawn().getX()) + ","
						+ Double.toString(town.getSpawn().getY()) + ","
						+ Double.toString(town.getSpawn().getZ()) + ","
						+ Float.toString(town.getSpawn().getPitch()) + ","
						+ Float.toString(town.getSpawn().getYaw()):"");																
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
				return true;
		}		
		catch (Exception e) 
		{ log.info("[Towny] SQL: Save Town unknown error"); e.printStackTrace(); }
		return false;
	}
	
	@Override
	public boolean saveNation(Nation nation) {
		TownyMessaging.sendDebugMsg("Saving nation "+nation.getName());		
		try {
			HashMap<String, Object> nat_hm = new HashMap<String, Object>();
			nat_hm.put("name", nation.getName());
			String fstr = "";
			for (Town town : nation.getTowns())
				fstr += town.getName() + ",";
			nat_hm.put("towns", fstr);
			nat_hm.put("capital", nation.hasCapital()?nation.getCapital().getName():"");
			nat_hm.put("tag", nation.hasTag()?nation.getTag():"");
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
			UpdateDB("nations", nat_hm, Arrays.asList("name"));
		}		
		catch (Exception e) 
		{ log.info("[Towny] SQL: Save Nation unknown error"); e.printStackTrace(); }
		return false;
	}
	
	@Override
	public boolean saveTownBlock(TownBlock townBlock) {
		TownyMessaging.sendDebugMsg("Saving town block "+townBlock.getWorld().getName()+":"+townBlock.getX()+"x"+townBlock.getZ());
		try
		{
			HashMap<String, Object> tb_hm = new HashMap<String, Object>();
			tb_hm.put("world", townBlock.getWorld().getName());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());														
			tb_hm.put("permissions", townBlock.getPermissions().toString());
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());			
			UpdateDB("townblocks", tb_hm, Arrays.asList("world","x","z"));
		}
		catch (Exception e) 
		{ log.info("[Towny] SQL: Save TownBlock unknown error"); e.printStackTrace(); }		
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
		twn_hm.put("world", townBlock.getWorld().getName());
		twn_hm.put("x", townBlock.getX());
		twn_hm.put("z", townBlock.getZ());
		DeleteDB("townblocks", twn_hm);
		
	}
		
	@Override
	public void backup() throws IOException {
		System.out.println("[Towny] Performing backup");
		System.out.println("[Towny] ***** Warning *****");
		System.out.println("[Towny] ***** Only Snapshots and Regen files will be backed up");
		System.out.println("[Towny] ***** Make sure you schedule a backup in MySQL too!!!");
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