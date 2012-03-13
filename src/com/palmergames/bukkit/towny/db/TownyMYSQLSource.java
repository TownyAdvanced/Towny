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
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.KeyValueFile;
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
		
		if (getContext())				
			System.out.println("Connected to Database");		
		else		
			System.out.println("Error connecting to Database");
		
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
			ResultSet town_table = dbm.getTables(null, null, tb_prefix+"nations", null); 
		
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
				"mayor,nation,world,bonus,purchased,taxes,taxpercent,hasUpkeep," +
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
							TownBlock tb = tworld.getTownBlock(x,z);
							tb.setTown(town);
							town.setHomeBlock(tb);
							
						} catch (NotRegisteredException e) {}
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
						try
						{
							World world = plugin.getServerWorld(rs.getString("world"));						
							town.setSpawn(new Location(world,							
							rs.getFloat("spawn_x"),rs.getFloat("spawn_y"),rs.getFloat("spawn_z"),
							rs.getFloat("spawn_yaw"),rs.getFloat("spawn_pitch")
							));															
						} catch (Exception e) { System.out.println("Spawn load error "+e.getMessage()); }	
						
						// Load outpost spawns
						line = rs.getString("outpostSpawns");
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
		System.out.println("Loading world: "+world.getName());
		return true;
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
					Town t = getTown(rs.getString("town"));
					tb.setTown(t); 
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
			
			Set set = args.entrySet();			
			Iterator i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry me = (Map.Entry)i.next();
				
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
			Set set = args.entrySet();			
			Iterator i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry me = (Map.Entry)i.next();
				code += me.getKey()+" = ";
				if (me.getValue() instanceof String)
					code += "'"+me.getValue()+"'";				
				else
					code += ""+me.getValue();
				code += ""+(i.hasNext()?",":"");
			}
			code += " WHERE ";		
			i = keys.iterator();
			while (i.hasNext())
			{				
				String key = (String)i.next();
				code += key+" = ";			
				Object v = args.get(key);
				if (v instanceof String)
					code += "'"+v+"'";	
				else
					code += v;
				code += ""+(i.hasNext()?" AND ":"");
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
			Set set = args.entrySet();			
			Iterator i = set.iterator();
			while(i.hasNext()) 
			{
				Map.Entry me = (Map.Entry)i.next();
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
		System.out.println("Saving Resident");		
		String query = "";		
		try {
			Statement s = cntx.createStatement();
			
			HashMap res_hm = new HashMap();
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
		System.out.println("Saving town");
		try {			
				HashMap twn_hm = new HashMap();
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
		System.out.println("Saving nation");
		try {
			HashMap nat_hm = new HashMap();
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
		sendDebugMsg("Saving world "+world.getName());
		System.out.println("Saving world");
		return true;
	}
	
	@Override
	public boolean saveTownBlock(TownBlock townBlock) {
		sendDebugMsg("Saving town block "+townBlock.getX()+"x"+townBlock.getZ());
		try
		{
			HashMap tb_hm = new HashMap();
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
		HashMap res_hm = new HashMap();
		res_hm.put("name", resident.getName());		
		DeleteDB("residents", res_hm);
	}
	
	@Override
	public void deleteTown(Town town) {
		HashMap twn_hm = new HashMap();
		twn_hm.put("name", town.getName());		
		DeleteDB("towns", twn_hm);
	}
	
	@Override
	public void deleteNation(Nation nation) {
		HashMap nat_hm = new HashMap();
		nat_hm.put("name", nation.getName());		
		DeleteDB("nations", nat_hm);
	}
	@Override
	public void deleteTownBlock(TownBlock townBlock) {
		HashMap twn_hm = new HashMap();
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