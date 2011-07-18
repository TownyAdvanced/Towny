package ca.xshade.bukkit.towny;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownBlockOwner;
import ca.xshade.bukkit.towny.object.TownyObject;
import ca.xshade.bukkit.towny.object.TownyPermission.ActionType;
import ca.xshade.bukkit.towny.object.TownyPermission.PermLevel;
import ca.xshade.bukkit.towny.object.WorldCoord;
import ca.xshade.util.FileMgmt;

import org.bukkit.util.config.Configuration;


public class TownySettings {
	
	// Nation Level
	public enum NationLevel {
		NAME_PREFIX,
		NAME_POSTFIX,
		CAPITAL_PREFIX,
		CAPITAL_POSTFIX,
		KING_PREFIX,
		KING_POSTFIX,
		UPKEEP_MULTIPLIER
	};
	// Town Level
	public enum TownLevel {
		NAME_PREFIX,
		NAME_POSTFIX,
		MAYOR_PREFIX,
		MAYOR_POSTFIX,
		TOWN_BLOCK_LIMIT,
		UPKEEP_MULTIPLIER
	};
	
	private static Pattern namePattern = null;	
	private static Configuration config;
	private static Configuration language;
	
	private static final SortedMap<Integer,Map<TownySettings.TownLevel,Object>> configTownLevel = 
		Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.TownLevel,Object>>(Collections.reverseOrder()));
	private static final SortedMap<Integer,Map<TownySettings.NationLevel,Object>> configNationLevel = 
		Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.NationLevel,Object>>(Collections.reverseOrder()));
	
	/*
	static {		
		newTownLevel(0, "", " Town", "Mayor ", "", 16);
		newNationLevel(0, "", " Nation", "Capital: ", " City", "King ", "");
	}
	*/
	
	public static void newTownLevel(int numResidents,
			String namePrefix, String namePostfix,
			String mayorPrefix, String mayorPostfix, 
			int townBlockLimit, double townUpkeepMultiplier) {
		ConcurrentHashMap<TownySettings.TownLevel,Object> m = new ConcurrentHashMap<TownySettings.TownLevel,Object>();
		m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
		m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
		m.put(TownySettings.TownLevel.UPKEEP_MULTIPLIER, townUpkeepMultiplier);
		configTownLevel.put(numResidents, m);
	}
	
	public static void newNationLevel(int numResidents, 
			String namePrefix, String namePostfix, 
			String capitalPrefix, String capitalPostfix,
			String kingPrefix, String kingPostfix, double nationUpkeepMultiplier) {
		ConcurrentHashMap<TownySettings.NationLevel,Object> m = new ConcurrentHashMap<TownySettings.NationLevel,Object>();
		m.put(TownySettings.NationLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.NationLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.NationLevel.CAPITAL_PREFIX, capitalPrefix);
		m.put(TownySettings.NationLevel.CAPITAL_POSTFIX, capitalPostfix);
		m.put(TownySettings.NationLevel.KING_PREFIX, kingPrefix);
		m.put(TownySettings.NationLevel.KING_POSTFIX, kingPostfix);
		m.put(TownySettings.NationLevel.UPKEEP_MULTIPLIER, nationUpkeepMultiplier);
		configNationLevel.put(numResidents, m);
	}
	
	/**
	 * Loads town levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:townBlockLimit
	 * 
	 * townBlockLimit is a required field even if using a calculated ratio.
	 * 
	 * @param filepath
	 * @throws IOException 
	 */
	
	public static void loadTownLevelConfig() throws IOException {

		String[] tokens;		
		List<String> lines = (List<String>)config.getProperty("townLevel");
		
		//get an Iterator object for list using iterator() method.
		Iterator<String> itr = lines.iterator();
		
		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {
                tokens = itr.next().split(",", 7);
                if (tokens.length >= 7)
					try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        int townBlockLimit = Integer.parseInt(tokens[5]);
                        double townUpkeepMult = Double.valueOf(tokens[6]);
                        newTownLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], townBlockLimit, townUpkeepMult);
						if (getDebug())
							// Used to know the actual values registered
							 System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(getTownLevel(numResidents).values().toArray()));
							//System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                    	System.out.println("[Towny] Input Error: Town level ignored: " + itr.toString());
                    }
                else
                	System.out.println("[Towny] loadTownLevelConfig bad length");
            //}
			
		}
		
		/*
		//BufferedReader fin = new BufferedReader(new FileReader(filepath));
		 
        while ((line = fin.readLine()) != null)
			if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 6);
                if (tokens.length >= 6)
					try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        int townBlockLimit = Integer.parseInt(tokens[5]);
                        newTownLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], townBlockLimit);
						if (getDebug())
							// Used to know the actual values registered
							 System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(getTownLevel(numResidents).values().toArray()));
							//System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                    	System.out.println("[Towny] Input Error: Town level ignored: " + line);
                    }
            }
        fin.close();
        */
	}
	
	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:kingPrefix:kingPostfix
	 * 
	 * @param filepath
	 * @throws IOException 
	 */
	
	public static void loadNationLevelConfig() throws IOException {
		
		String[] tokens;		
		List<String> lines = (List<String>)config.getProperty("nationLevel");
		
		//get an Iterator object for list using iterator() method.
		Iterator<String> itr = lines.iterator();
		
		//use hasNext() and next() methods of Iterator to iterate through the elements
		while(itr.hasNext()) {
			tokens = itr.next().split(",", 8);
            if (tokens.length >= 8)
				try {
                    int numResidents = Integer.parseInt(tokens[0]);
                    double upkeep = Double.valueOf(tokens[7]);
                    newNationLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], upkeep);
					if (getDebug())
						// Used to know the actual values registered
						// System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(getNationLevel(numResidents).values().toArray()));
						System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(tokens));
                } catch (Exception e) {
                	System.out.println("[Towny] Input Error: Nation level ignored: " + itr.toString());
                }

			
		}
		
		/*
		String line;
		String[] tokens;
		BufferedReader fin = new BufferedReader(new FileReader(filepath));
        while ((line = fin.readLine()) != null)
			if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 7);
                if (tokens.length >= 7)
					try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        newNationLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
						if (getDebug())
							// Used to know the actual values registered
							// System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(getNationLevel(numResidents).values().toArray()));
							System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                    	System.out.println("[Towny] Input Error: Nation level ignored: " + line);
                    }
            }
        fin.close();
        */
	}
	
	public static Map<TownySettings.TownLevel,Object> getTownLevel(int numResidents) {
		return configTownLevel.get(numResidents);
	}
	
	public static Map<TownySettings.NationLevel,Object> getNationLevel(int numResidents) {
		return configNationLevel.get(numResidents);
	}
	
	public static Map<TownySettings.TownLevel,Object> getTownLevel(Town town) {
		return getTownLevel(calcTownLevel(town));
	}
	
	public static Map<TownySettings.NationLevel,Object> getNationLevel(Nation nation) {
		return getNationLevel(calcNationLevel(nation));
	}
	
	//TODO: more efficient way
	public static int calcTownLevel(Town town) {
		int n = town.getNumResidents();
		for (Integer level : configTownLevel.keySet())
			if (n >= level)
				return level;
        return 0;
    }
	
	//TODO: more efficient way
	public static int calcNationLevel(Nation nation) {
		int n = nation.getNumResidents();		
		for (Integer level : configNationLevel.keySet())
			if (n >= level)
				return level;
        return 0;
    }
	
	//////////////////////////////
	
	public static void loadConfig(String filepath, String defaultRes) throws IOException {		
		
		File file = FileMgmt.CheckYMLexists(filepath, defaultRes);
		if (file != null) {
				
			// read the config.yml into memory
			config = new Configuration(file);
			config.load();
			
			// Load Nation & Town level data into maps.
			loadTownLevelConfig();
			loadNationLevelConfig();
		}	
	}
	
	// This will read the language entry in the config.yml to attempt to load custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage (String filepath, String defaultRes) throws IOException {		
		
		String defaultName = filepath + FileMgmt.fileSeparator() + getString("language");
		
		File file = FileMgmt.CheckYMLexists(defaultName, defaultRes);
		if (file != null) {
				
			// read the (language).yml into memory
			language = new Configuration(file);
			language.load();
				
		}
	}
	
	///////////////////////////////////
	
	// Functions to pull data from the config and language files

	private static String[] parseString(String str) {
		return parseSingleLineString(str).split("@");
	}
	
	public static String parseSingleLineString(String str) {
		return str.replaceAll("&", "\u00A7");
	}
	
	public static Boolean getBoolean(String root){
        return config.getBoolean(root.toLowerCase(), true);
    }
    private static Double getDouble(String root){
        return config.getDouble(root.toLowerCase(), 0);
    }
    private static Integer getInt(String root){
        return config.getInt(root.toLowerCase(), 0);
    }
    private static Long getLong(String root){
        return Long.parseLong(getString(root).trim());
    }
    
    /*
     * Public Functions to read data from the Configuration
     * and Language data
     * 
     * 
     */
    
    public static String getString(String root){
        return config.getString(root.toLowerCase());
    }
    public static String getLangString(String root){
        return parseSingleLineString(language.getString(root.toLowerCase()));
    }
    
 // read a comma delimited string into an Integer list
	public static List<Integer> getIntArr(String root) {
		
		String[] strArray = getString(root.toLowerCase()).split(",");
		List<Integer> list = new ArrayList<Integer>();
		if (strArray != null) {
		for (int ctr=0; ctr < strArray.length; ctr++)
			if (strArray[ctr] != null)
				list.add(Integer.parseInt(strArray[ctr]));
		}	
		return list;
	}
	
	// read a comma delimited string into a trimmed list.
	public static List<String> getStrArr(String root) {
		
		String[] strArray = getString(root.toLowerCase()).split(",");
		List<String> list = new ArrayList<String>();
		if (strArray != null) {
		for (int ctr=0; ctr < strArray.length; ctr++)
			if (strArray[ctr] != null)
				list.add(strArray[ctr].trim());
		}
		return list;
	}
	
    ///////////////////////////////////
    
	public static String[] getRegistrationMsg() {		
		return parseString(getLangString("MSG_REGISTRATION"));
	}

	public static String[] getNewTownMsg(String who, String town) {
		return parseString(String.format(getLangString("MSG_NEW_TOWN"), who, town));
	}

	public static String[] getNewNationMsg(String who, String nation) {
		return parseString(String.format(getLangString("MSG_NEW_NATION"), who, nation));
	}

	public static String[] getJoinTownMsg(String who) {
		return parseString(String.format(getLangString("MSG_JOIN_TOWN"), who));
	}

	public static String[] getJoinNationMsg(String who) {
		return parseString(String.format(getLangString("MSG_JOIN_NATION"), who));
	}

	public static String[] getNewMayorMsg(String who) {
		return parseString(String.format(getLangString("MSG_NEW_MAYOR"), who));
	}

	public static String[] getNewKingMsg(String who) {
		return parseString(String.format(getLangString("MSG_NEW_KING"), who));
	}
	
	public static String[] getJoinWarMsg(TownyObject obj) {
		return parseString(String.format(getLangString("MSG_WAR_JOIN"), obj.getName()));
	}

	public static String[] getWarTimeEliminatedMsg(String who) {
		return parseString(String.format(getLangString("MSG_WAR_ELIMINATED"), who));
	}
	
	public static String[] getWarTimeForfeitMsg(String who) {
		return parseString(String.format(getLangString("MSG_WAR_FORFEITED"), who));
	}
	
	public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord, String town) {
		return parseString(String.format(getLangString("MSG_WAR_LOSE_BLOCK"), worldCoord.toString(), town));
	}
	
	public static String[] getWarTimeScoreMsg(Town town, int n) {
		return parseString(String.format(getLangString("MSG_WAR_SCORE"), town.getName(), n));
	}
	
	public static String[] getCouldntPayTaxesMsg(TownyObject obj, String type) {
		return parseString(String.format(getLangString("MSG_COULDNT_PAY_TAXES"), obj.getName(), type));
	}
	
	public static String getPayedTownTaxMsg() {
		return getLangString("MSG_PAYED_TOWN_TAX");
	}
	
	public static String getPayedResidentTaxMsg() {
		return getLangString("MSG_PAYED_RESIDENT_TAX");
	}
	
	public static String getTaxExemptMsg() {
		return getLangString("MSG_TAX_EXEMPT");
	}
	
	public static String[] getDelResidentMsg(Resident resident) {
		return parseString(String.format(getLangString("MSG_DEL_RESIDENT"), resident.getName()));
	}
	
	public static String[] getDelTownMsg(Town town) {
		return parseString(String.format(getLangString("MSG_DEL_TOWN"), town.getName()));
	}
	
	public static String[] getDelNationMsg(Nation nation) {
		return parseString(String.format(getLangString("MSG_DEL_NATION"), nation.getName()));
	}
	
	public static String[] getBuyResidentPlotMsg(String who, String owner) {
		return parseString(String.format(getLangString("MSG_BUY_RESIDENT_PLOT"), who, owner));
	}
	
	public static String[] getPlotForSaleMsg(String who, WorldCoord worldCoord) {
		return parseString(String.format(getLangString("MSG_PLOT_FS"), who, worldCoord.toString()));
	}
	
	public static String[] getMayorAbondonMsg() {
		return parseString(getLangString("MSG_MAYOR_ABANDON"));
	}
	
	public static String getNotPermToNewTownLine() {
		return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_TOWN"));
	}
	
	public static String getNotPermToNewNationLine() {
		return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_NATION"));
	}
	
	////////////////////////////////////////////
	
	public static String getKingPrefix(Resident resident) {
		try {
			return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_PREFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getKingPrefix.");
			return "";
		}
	}

	public static String getMayorPrefix(Resident resident) {
		try {
			return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_PREFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getMayorPrefix.");
			return "";
		}
	}

	public static String getCapitalPostfix(Town town) {
		try {
			return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getCapitalPostfix.");
			return "";
		}
	}

	public static String getTownPostfix(Town town) {
		try {
			return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX);
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not read getTownPostfix.");
			return "";
		}
	}
	
	public static String getNationPostfix(Nation nation) {
		try {
			return (String)getNationLevel(nation).get(TownySettings.NationLevel.NAME_POSTFIX);
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not read getNationPostfix.");
			return "";
		}
	}
	
	public static String getNationPrefix(Nation nation) {
		try {
			return (String)getNationLevel(nation).get(TownySettings.NationLevel.NAME_PREFIX);
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not read getNationPrefix.");
			return "";
		}
	}
	
	public static String getTownPrefix(Town town) {
		try {
			return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX);
		} catch (Exception e) {
			System.out.println("[Towny] Error: Could not read getTownPrefix.");
			return "";
		}
	}
	
	public static String getCapitalPrefix(Town town) {
		try {
			return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getCapitalPrefix.");
			return "";
		}
	}
	
	public static String getKingPostfix(Resident resident) {
		try {
			return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_POSTFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getKingPostfix.");
			return "";
		}
	}
	
	public static String getMayorPostfix(Resident resident) {
		try {
			return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_POSTFIX);
		} catch (NotRegisteredException e) {
			System.out.println("[Towny] Error: Could not read getMayorPostfix.");
			return "";
		}
	}
	
	public static String getNPCPrefix() {
		
		return getString("NPC_PREFIX");
	}
	
	
	// Towny commands
	
	public static List<String> getResidentCommands() {
		return getStrArr("commands.resident.aliases");
	}
	
	public static List<String> getTownCommands() {
		return getStrArr("commands.town.aliases");
	}
	
	public static List<String> getNationCommands() {
		return getStrArr("commands.nation.aliases");
	}
	
	public static List<String> getWorldCommands() {
		return getStrArr("commands.townyworld.aliases");
	}
	
	public static List<String> getPlotCommands() {
		return getStrArr("commands.plot.aliases");
	}
	
	public static List<String> getTownyCommands() {
		return getStrArr("commands.towny.aliases");
	}
	
	public static List<String> getTownyAdminCommands() {
		return getStrArr("commands.townyadmin.aliases");
	}
	
	public static List<String> getTownChatCommands() {
		return getStrArr("commands.townchat.aliases");
	}
	
	public static List<String> getNationChatCommands() {
		return getStrArr("commands.nationchat.aliases");
	}
	
	public static String getFirstCommand(List<String> commands) {
		if (commands.size() > 0)
			return commands.get(0);
		else
			return "/<unknown>";
	}

	///////////////////////////////
	
	public static long getInactiveAfter() {
		return getLong("INACTIVE_AFTER_TIME");
	}

	public static String getLoadDatabase() {
		return getString("DATABASE_LOAD");
	}

	public static String getSaveDatabase() {
		return getString("DATABASE_SAVE");
	}
	
	/*
	public static boolean isFirstRun() {
		return getBoolean("FIRST_RUN");
	}
	*/

	public static int getMaxTownBlocks(Town town) {
		int ratio = getInt("TOWN_BLOCK_RATIO");
		if (ratio == 0)
			return town.getBonusBlocks() + (Integer)getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);
		else
			return town.getBonusBlocks() + town.getNumResidents()*ratio;
	}

	public static int getTownBlockSize() {
		return getInt("TOWN_BLOCK_SIZE");
	}

	public static boolean getFriendlyFire() {
		return getBoolean("FRIENDLY_FIRE");
	}

	public static boolean isTownCreationAdminOnly() {
		return getBoolean("TOWN_CREATION_ADMIN_ONLY");
	}
	
	public static boolean isNationCreationAdminOnly() {
		return getBoolean("NATION_CREATION_ADMIN_ONLY");
	}

	public static boolean isUsingIConomy() {
		return getBoolean("USING_ICONOMY");
	}
	
	public static boolean isUsingEssentials() {
		return getBoolean("USING_ESSENTIALS");
	}

	public static double getNewTownPrice() {
		return getDouble("economy.PRICE_NEW_TOWN");
	}
	
	public static double getNewNationPrice() {
		return getDouble("economy.PRICE_NEW_NATION");
	}

	public static boolean getUnclaimedZoneBuildRights() {
		return getBoolean("unclaimed.UNCLAIMED_ZONE_BUILD");
	}
	
	public static boolean getUnclaimedZoneDestroyRights() {
		return getBoolean("unclaimed.UNCLAIMED_ZONE_DESTROY");
	}
	
	public static boolean getUnclaimedZoneItemUseRights() {
		return getBoolean("unclaimed.UNCLAIMED_ZONE_ITEM_USE");
	}

	public static boolean getDebug() {
		return getBoolean("DEBUG_MODE");
	}

	public static boolean getShowTownNotifications() {
		return getBoolean("SHOW_TOWN_NOTIFICATIONS");
	}

	public static String getUnclaimedZoneName() {
		return getLangString("UNCLAIMED_ZONE_NAME");
	}

	public static boolean isUsingChatPrefix() {
		return getBoolean("MODIFY_CHAT_NAME");
	}

	public static long getMaxInactivePeriod() {
		return getLong("DELETED_AFTER_TIME");
	}

	public static boolean isDeletingOldResidents() {
		return getBoolean("DELETE_OLD_RESIDENTS");
	}

	public static int getWarTimeWarningDelay() {
		return getInt("WARTIME_WARNING_DELAY");
	}

	public static int getWarzoneTownBlockHealth() {
		return getInt("WARTIME_TOWN_BLOCK_HP");
	}
	
	public static int getWarzoneHomeBlockHealth() {
		return getInt("WARTIME_HOME_BLOCK_HP");
	}


	public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord) {
		return getWarTimeLoseTownBlockMsg(worldCoord, "");
	}
	
	public static String getDefaultTownName() {
		return getString("DEFAULT_TOWN_NAME");
	}	
	
	public static int getWarPointsForTownBlock() {
		return getInt("WARTIME_POINTS_TOWNBLOCK");
	}
	
	public static int getWarPointsForTown() {
		return getInt("WARTIME_POINTS_TOWN");
	}
	
	public static int getWarPointsForNation() {
		return getInt("WARTIME_POINTS_NATION");
	}
	
	public static int getWarPointsForKill() {
		return getInt("WARTIME_POINTS_KILL");
	}
	
	public static int getMinWarHeight() {
		return getInt("WARTIME_MIN_HEIGHT");
	}
	
	public static List<String> getWorldMobRemovalEntities() {
		if (getDebug()) System.out.println("[Towny] Debug: Reading World Mob removal entities. ");
		return getStrArr("protection.WORLD_MOB_REMOVAL_ENTITIES");
	}
	
	public static List<String> getTownMobRemovalEntities() {
		if (getDebug()) System.out.println("[Towny] Debug: Reading Town Mob removal entities. ");
		return getStrArr("protection.TOWN_MOB_REMOVAL_ENTITIES");
	}
	
	public static int getMobRemovalSpeed() {
		return getInt("protection.MOB_REMOVAL_SPEED");
	}
	
	/*
	public static boolean isRemovingWorldMobs() {
		return getBoolean("protection.MOB_REMOVAL_WORLD");
	}
	
	public static boolean isRemovingTownMobs() {
		return getBoolean("protection.MOB_REMOVAL_TOWN");
	}
	*/
	
	public static int getHealthRegenSpeed() {
		return getInt("HEALTH_REGEN_SPEED");
	}
	
	public static boolean hasHealthRegen() {
		return getBoolean("HEALTH_REGEN");
	}
	
	public static boolean hasTownLimit() {
		return getInt("TOWN_LIMIT") == 0;
	}
	
	public static int getTownLimit() {
		return getInt("TOWN_LIMIT");
	}
	
	public static double getNationNeutralityCost() {
		return getDouble("economy.PRICE_NATION_NEUTRALITY");
	}
	
	public static boolean isAllowingOutposts() {
		return getBoolean("ALLOW_OUTPOSTS");
	}
	
	public static double getOutpostCost() {
		return getDouble("economy.PRICE_OUTPOST");
	} 
	
	public static List<Integer> getSwitchIds() {
		return getIntArr("protection.SWITCH_IDS");
	}
	
	public static List<Integer> getUnclaimedZoneIgnoreIds() {
		return getIntArr("unclaimed.UNCLAIMED_ZONE_IGNORE");
	}
	
	public static List<Integer> getItemUseIds() {
		return getIntArr("protection.ITEM_USE_IDS");
	}
	
	public static boolean isUnclaimedZoneIgnoreId(int id) {
		return getIntArr("unclaimed.UNCLAIMED_ZONE_IGNORE").contains(id);
	}
	
	public static boolean isSwitchId(int id) {
		return getIntArr("protection.SWITCH_IDS").contains(id);
	}
	
	public static boolean isItemUseId(int id) {
		return getIntArr("protection.ITEM_USE_IDS").contains(id);
	}
	
	public static void setProperty(String root, Object value) {
		config.setProperty(root.toLowerCase(), value);
		if (getDebug()) System.out.println("[Towny] Debug: Saving config.yml ");
		config.save();
	}
	
	public static Object getProperty(String root) {
		return config.getProperty(root.toLowerCase());

	}

	public static double getClaimPrice() {
		return getDouble("economy.PRICE_CLAIM_TOWNBLOCK");
	}

	public static boolean getUnclaimedZoneSwitchRights() {
		return getBoolean("unclaimed.UNCLAIMED_ZONE_SWITCH");
	}

	public static String getUnclaimedPlotName() {
		return getLangString("UNCLAIMED_PLOT_NAME");
	}

	public static long getDayInterval() {
		return getLong("DAY_INTERVAL");
	}
	
	public static boolean isAllowingTownSpawn() {
		return getBoolean("ALLOW_TOWN_SPAWN");
	}
	
	public static boolean isAllowingPublicTownSpawnTravel() {
		return getBoolean("ALLOW_TOWN_SPAWN_TRAVEL");
	}
	
	public static boolean isTaxingDaily() {
		return getBoolean("DAILY_TAXES");
	}
	
	public static boolean isBackingUpDaily() {
		return getBoolean("DAILY_BACKUPS");
	}
	
	public static double getTownSpawnTravelPrice() {
		return getDouble("economy.PRICE_TOWN_SPAWN_TRAVEL");
	}
	
	public static double getBaseSpoilsOfWar() {
		return getDouble("WARTIME_BASE_SPOILS");
	}
	
	public static double getWartimeDeathPrice() {
		return getDouble("economy.PRICE_DEATH_WARTIME");
	}
	
	public static double getDeathPrice() {
		return getDouble("economy.PRICE_DEATH");
	}
	
	public static double getWartimeTownBlockLossPrice() {
		return getDouble("WARTIME_TOWN_BLOCK_LOSS_PRICE");
	}
	
	public static boolean isDevMode() {
		return getBoolean("DEV_MODE");
	}
	
	/*
	public static boolean isPvEWithinNonPvPZones() {
		return getBoolean("PVE_IN_NON_PVP_TOWNS");
	}
	*/
	
	public static boolean isDeclaringNeutral() {
		return getBoolean("WARTIME_NATION_CAN_BE_NEUTRAL");
	}

	public static boolean isRemovingOnMonarchDeath() {
		return getBoolean("WARTIME_REMOVE_ON_MONARCH_DEATH");
	}

	public static double getTownUpkeepCost(Town town) {
		double multiplier;
		
		if (town != null)
			multiplier = Double.valueOf(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
		else
			multiplier = 1.0;
			
		return getDouble("economy.PRICE_TOWN_UPKEEP") * multiplier;
	}
	
	public static double getNationUpkeepCost(Nation nation) {
		double multiplier;
		
		if (nation != null)
			multiplier = Double.valueOf(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
		else
			multiplier = 1.0;
		
		return getDouble("economy.PRICE_NATION_UPKEEP") * multiplier;
	}
	
	public static String getFlatFileBackupType() {
		return getString("FLATFILE_BACKUP");
	}
	
	/*
	public static boolean isForcingPvP() {
	 
		return getBoolean("FORCE_PVP_ON");
	}
	
	public static boolean isForcingExplosions() {
		return getBoolean("FORCE_EXPLOSIONS_ON");
	}
	
	public static boolean isForcingMonsters() {
		return getBoolean("FORCE_MONSTERS_ON");
	}
	
	public static boolean isForcingFire() {
		return getBoolean("FORCE_FIRE_ON");
	}
	*/

	public static boolean isTownRespawning() {
		return getBoolean("TOWN_RESPAWN");
	}
	
	public static boolean isTownyUpdating(String currentVersion) {
		if (isTownyUpToDate(currentVersion))
			return false;
		else
			return true; //Assume
	}
	
	public static boolean isTownyUpToDate(String currentVersion) {
		return currentVersion.equals(getLastRunVersion());
	}

	public static String getLastRunVersion() {
		return getString("LAST_RUN_VERSION");
	}

	public static int getMinDistanceFromTownHomeblocks() {
		return getInt("MIN_DISTANCE_FROM_TOWN_HOMEBLOCK");
	}

	public static int getMaxPlotsPerResident() {
		return getInt("MAX_PLOTS_PER_RESIDENT");
	}
	
	public static boolean getDefaultResidentPermission(TownBlockOwner owner, ActionType type) {
		if (owner instanceof Resident)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_FRIEND_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_FRIEND_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_FRIEND_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_FRIEND_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_RESIDENT_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_RESIDENT_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_RESIDENT_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_RESIDENT_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}
	
	public static boolean getDefaultAllyPermission(TownBlockOwner owner, ActionType type) {
		if (owner instanceof Resident)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_ALLY_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_ALLY_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_ALLY_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_ALLY_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_ALLY_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_ALLY_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_ALLY_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_ALLY_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}
	
	public static boolean getDefaultOutsiderPermission(TownBlockOwner owner, ActionType type) {
		if (owner instanceof Resident)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_OUTSIDER_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_OUTSIDER_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_OUTSIDER_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_RESIDENT_PERM_OUTSIDER_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
				case BUILD: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_OUTSIDER_BUILD");
				case DESTROY: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_OUTSIDER_DESTROY");
				case SWITCH: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_OUTSIDER_ITEMUSE");
				case ITEM_USE: return getBoolean("default_perm_flags.DEFAULT_TOWN_PERM_OUTSIDER_SWITCH");
				default: throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}
	
	public static boolean getDefaultPermission(TownBlockOwner owner, PermLevel level, ActionType type) {
		switch (level) {
			case RESIDENT: return getDefaultResidentPermission(owner, type);
			case ALLY: return getDefaultAllyPermission(owner, type);
			case OUTSIDER: return getDefaultOutsiderPermission(owner, type);
			default: throw new UnsupportedOperationException();
		}
	}

	public static boolean isLogging() {
		return getBoolean("LOGGING");
	}

	public static boolean isUsingQuestioner() {
		return getBoolean("USING_QUESTIONER");
	}
	
	public static boolean isAppendingToLog() {
		return !getBoolean("RESET_LOG_ON_BOOT");
	}

	public static boolean isUsingPermissions() {
		return getBoolean("USING_PERMISSIONS");
	}
	
	public static String filterName(String input) {
		return input.replaceAll(getString("NAME_FILTER_REGEX"), "_").replaceAll(getString("NAME_REMOVE_REGEX"), "");
	}
	
	public static boolean isValidName(String name) {
		try {
			if (TownySettings.namePattern == null)
				namePattern = Pattern.compile(getString("NAME_CHECK_REGEX"));
			return TownySettings.namePattern.matcher(name).find();
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	public static boolean isSavingOnLoad() {
		return getBoolean("SAVE_ON_LOAD");
	}
	*/
	
	public static boolean isAllowingResidentPlots() {
		return getBoolean("ALLOW_RESIDENT_PLOTS");
	}
}
