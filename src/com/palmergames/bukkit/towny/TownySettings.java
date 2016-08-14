package com.palmergames.bukkit.towny;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

public class TownySettings {

	// Town Level
	public enum TownLevel {
		NAME_PREFIX, NAME_POSTFIX, MAYOR_PREFIX, MAYOR_POSTFIX, TOWN_BLOCK_LIMIT, UPKEEP_MULTIPLIER
	};

	// Nation Level
	public enum NationLevel {
		NAME_PREFIX, NAME_POSTFIX, CAPITAL_PREFIX, CAPITAL_POSTFIX, KING_PREFIX, KING_POSTFIX, TOWN_BLOCK_LIMIT_BONUS, UPKEEP_MULTIPLIER, NATION_TOWN_UPKEEP_MULTIPLIER
	};

	// private static Pattern namePattern = null;
	private static CommentedConfiguration config, newConfig, language, newLanguage;

	private static final SortedMap<Integer, Map<TownySettings.TownLevel, Object>> configTownLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.TownLevel, Object>>(Collections.reverseOrder()));
	private static final SortedMap<Integer, Map<TownySettings.NationLevel, Object>> configNationLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.NationLevel, Object>>(Collections.reverseOrder()));

	public static void newTownLevel(int numResidents, String namePrefix, String namePostfix, String mayorPrefix, String mayorPostfix, int townBlockLimit, double townUpkeepMultiplier) {

		ConcurrentHashMap<TownySettings.TownLevel, Object> m = new ConcurrentHashMap<TownySettings.TownLevel, Object>();
		m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
		m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
		m.put(TownySettings.TownLevel.UPKEEP_MULTIPLIER, townUpkeepMultiplier);				
		configTownLevel.put(numResidents, m);
	}

	public static void newNationLevel(int numResidents, String namePrefix, String namePostfix, String capitalPrefix, String capitalPostfix, String kingPrefix, String kingPostfix, int townBlockLimitBonus, double nationUpkeepMultiplier, double nationTownUpkeepMultiplier) {

		ConcurrentHashMap<TownySettings.NationLevel, Object> m = new ConcurrentHashMap<TownySettings.NationLevel, Object>();
		m.put(TownySettings.NationLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.NationLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.NationLevel.CAPITAL_PREFIX, capitalPrefix);
		m.put(TownySettings.NationLevel.CAPITAL_POSTFIX, capitalPostfix);
		m.put(TownySettings.NationLevel.KING_PREFIX, kingPrefix);
		m.put(TownySettings.NationLevel.KING_POSTFIX, kingPostfix);
		m.put(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS, townBlockLimitBonus);
		m.put(TownySettings.NationLevel.UPKEEP_MULTIPLIER, nationUpkeepMultiplier);
		m.put(TownySettings.NationLevel.NATION_TOWN_UPKEEP_MULTIPLIER, nationTownUpkeepMultiplier);
		configNationLevel.put(numResidents, m);
	}

	/**
	 * Loads town levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:
	 * townBlockLimit
	 * 
	 * townBlockLimit is a required field even if using a calculated ratio.
	 * 
	 * @throws IOException
	 */
	public static void loadTownLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.town_level");
		for (Map<?, ?> level : levels) {

			newTownLevel(
					(Integer) level.get("numResidents"),
					(String) level.get("namePrefix"),
					(String) level.get("namePostfix"),
					(String) level.get("mayorPrefix"),
					(String) level.get("mayorPostfix"),
					(Integer) level.get("townBlockLimit"),
					(Double) level.get("upkeepModifier")
					);

		}
	}

	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * 
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:
	 * kingPrefix:kingPostfix
	 * 
	 * @throws IOException
	 */

	public static void loadNationLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.nation_level");
		for (Map<?, ?> level : levels) {

			newNationLevel(
					(Integer) level.get("numResidents"),
					(String) level.get("namePrefix"),
					(String) level.get("namePostfix"),
					(String) level.get("capitalPrefix"),
					(String) level.get("capitalPostfix"),
					(String) level.get("kingPrefix"),
					(String) level.get("kingPostfix"),
					(level.containsKey("townBlockLimitBonus") ? (Integer) level.get("townBlockLimitBonus") : 0),
					(Double) level.get("upkeepModifier"),
					(level.containsKey("nationTownUpkeepModifier") ? (Double) level.get("nationTownUpkeepModifier") : 1.0)
					);

		}
	}

	public static Map<TownySettings.TownLevel, Object> getTownLevel(int numResidents) {

		return configTownLevel.get(numResidents);
	}

	public static Map<TownySettings.NationLevel, Object> getNationLevel(int numResidents) {

		return configNationLevel.get(numResidents);
	}

	public static Map<TownySettings.TownLevel, Object> getTownLevel(Town town) {

		return getTownLevel(calcTownLevel(town));
	}

	public static Map<TownySettings.NationLevel, Object> getNationLevel(Nation nation) {

		return getNationLevel(calcNationLevel(nation));
	}

	// TODO: more efficient way
	public static int calcTownLevel(Town town) {

		int n = town.getNumResidents();
		for (Integer level : configTownLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	// TODO: more efficient way
	public static int calcNationLevel(Nation nation) {

		int n = nation.getNumResidents();
		for (Integer level : configNationLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	public static void loadConfig(String filepath, String version) throws IOException {

		File file = FileMgmt.CheckYMLExists(new File(filepath));
		if (file != null) {

			// read the config.yml into memory
			config = new CommentedConfiguration(file);
			if (!config.load())
				System.out.print("Failed to load Config!");

			setDefaults(version, file);

			config.save();

			loadCachedObjects();
		}
	}

	public static void loadCachedObjects() throws IOException {

		// Cell War material types.
		TownyWarConfig.setFlagBaseMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_BASE_BLOCK)));
		TownyWarConfig.setFlagLightMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_LIGHT_BLOCK)));
		TownyWarConfig.setBeaconWireFrameMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_BEACON_WIREFRAME_BLOCK)));

		// Load Nation & Town level data into maps.
		loadTownLevelConfig();
		loadNationLevelConfig();

		// Load allowed blocks in warzone.
		TownyWarConfig.setEditableMaterialsInWarZone(getAllowedMaterials(ConfigNodes.WAR_WARZONE_EDITABLE_MATERIALS));

		ChunkNotification.loadFormatStrings();
	}

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage(String filepath, String defaultRes) throws IOException {

		String res = getString(ConfigNodes.LANGUAGE.getRoot(), defaultRes);
		String fullPath = filepath + FileMgmt.fileSeparator() + res;
		File file = FileMgmt.unpackResourceFile(fullPath, res, defaultRes);
		
		if (file != null) {

			// read the (language).yml into memory
			language = new CommentedConfiguration(file);
			language.load();
			newLanguage = new CommentedConfiguration(file);
			try {
				newLanguage.loadFromString(FileMgmt.convertStreamToString("/" + res));
			} catch (IOException e) {
				TownyMessaging.sendMsg("Custom language file detected, not updating.");
				return;
			} catch (InvalidConfigurationException e) {
				TownyMessaging.sendMsg("Invalid Configuration in language file detected.");
			}
			String resVersion = newLanguage.getString("version");			
			String langVersion = TownySettings.getLangString("version");
			
			if (!langVersion.equalsIgnoreCase(resVersion)) {
				language = newLanguage;
				newLanguage = null;
				TownyMessaging.sendMsg("Newer language file available, language file updated.");
				FileMgmt.stringToFile(FileMgmt.convertStreamToString("/" + res), file);
			}			
		}
	}

	private static void sendError(String msg) {

		System.out.println("[Towny] Error could not read " + msg);
	}

	private static String[] parseString(String str) {

		return parseSingleLineString(str).split("@");
	}

	public static String parseSingleLineString(String str) {

		return str.replaceAll("&", "\u00A7");
	}

	public static boolean getBoolean(ConfigNodes node) {

		return Boolean.parseBoolean(config.getString(node.getRoot().toLowerCase(), node.getDefault()));
	}

	public static double getDouble(ConfigNodes node) {

		try {
			return Double.parseDouble(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 0.0;
		}
	}

	public static int getInt(ConfigNodes node) {

		try {
			return Integer.parseInt(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 0;
		}
	}

	public static String getString(ConfigNodes node) {

		return config.getString(node.getRoot().toLowerCase(), node.getDefault());
	}

	public static String getString(String root, String def) {

		String data = config.getString(root.toLowerCase(), def);
		if (data == null) {
			sendError(root.toLowerCase() + " from config.yml");
			return "";
		}
		return data;
	}

	public static String getLangString(String root) {

		String data = language.getString(root.toLowerCase());

		if (data == null) {
			sendError(root.toLowerCase() + " from " + config.getString("language"));
			return "";
		}
		return parseSingleLineString(data);
	}

	public static String getConfigLang(ConfigNodes node) {

		return parseSingleLineString(getString(node));
	}

	public static List<Integer> getIntArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot(), node.getDefault()).split(",");
		List<Integer> list = new ArrayList<Integer>();
		if (strArray != null) {
			for (int ctr = 0; ctr < strArray.length; ctr++)
				if (strArray[ctr] != null) {
					try {
						list.add(Integer.parseInt(strArray[ctr].trim()));
					} catch (NumberFormatException e) {
						sendError(node.getRoot().toLowerCase() + " from config.yml");
					}
				}
		}
		return list;
	}

	private static List<String> getStrArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot().toLowerCase(), node.getDefault()).split(",");
		List<String> list = new ArrayList<String>();
		if (strArray != null) {
			for (int ctr = 0; ctr < strArray.length; ctr++)
				if (strArray[ctr] != null)
					list.add(strArray[ctr].trim());
		}
		return list;
	}

	public static long getSeconds(ConfigNodes node) {

		try {
			return TimeTools.getSeconds(getString(node));
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase() + " from config.yml");
			return 1;
		}
	}

	public static Set<Material> getAllowedMaterials(ConfigNodes node) {

		Set<Material> allowedMaterials = new HashSet<Material>();
		for (String material : getStrArr(node)) {
			if (material.equals("*")) {
				allowedMaterials.addAll(Arrays.asList(Material.values()));
			} else if (material.startsWith("-")) {
				allowedMaterials.remove(Material.matchMaterial(material));
			} else {
				allowedMaterials.add(Material.matchMaterial(material));
			}
		}
		return allowedMaterials;
	}

	public static void addComment(String root, String... comments) {

		newConfig.addComment(root.toLowerCase(), comments);
	}

	/**
	 * Builds a new config reading old config data.
	 */
	private static void setDefaults(String version, File file) {

		newConfig = new CommentedConfiguration(file);
		newConfig.load();

		for (ConfigNodes root : ConfigNodes.values()) {
			if (root.getComments().length > 0)
				addComment(root.getRoot(), root.getComments());

			if (root.getRoot() == ConfigNodes.LEVELS.getRoot()) {
				
				setDefaultLevels();
				
			} else if ((root.getRoot() == ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()) || (root.getRoot() == ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
				
				// Do nothing here as setDefaultLevels configured town and
				// nation levels.
				
			} else if (root.getRoot() == ConfigNodes.VERSION.getRoot()) {
				setNewProperty(root.getRoot(), version);
			} else if (root.getRoot() == ConfigNodes.LAST_RUN_VERSION.getRoot()) {
				setNewProperty(root.getRoot(), getLastRunVersion(version));
			} else if (root.getRoot() == ConfigNodes.VERSION_BUKKIT.getRoot()) {
				setNewProperty(root.getRoot(), ConfigNodes.VERSION_BUKKIT.getDefault());
			} else if (root.getRoot() == ConfigNodes.PROT_ITEM_USE_MAT.getRoot()) {
				
				/*
				 * Update any Id's to Material names (where required).
				 */
				setNewProperty(root.getRoot(), convertIds(getStrArr(ConfigNodes.PROT_ITEM_USE_MAT)));
				
			} else if (root.getRoot() == ConfigNodes.PROT_SWITCH_MAT.getRoot()) {
				
				/*
				 * Update any Id's to Material names (where required).
				 */
				setNewProperty(root.getRoot(), convertIds(getStrArr(ConfigNodes.PROT_SWITCH_MAT)));
				
			} else if (root.getRoot() == ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE.getRoot()) {
				
				/*
				 * Update any Id's to Material names (where required).
				 */
				setNewProperty(root.getRoot(), convertIds(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE)));
				
			} else if (root.getRoot() == ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_IGNORE.getRoot()) {
				
				/*
				 * Update any Id's to Material names (where required).
				 */
				setNewProperty(root.getRoot(), convertIds(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_IGNORE)));
				
			} else if (root.getRoot() == ConfigNodes.UNCLAIMED_ZONE_IGNORE.getRoot()) {
				
				/*
				 * Update any Id's to Material names (where required).
				 */
				setNewProperty(root.getRoot(), convertIds(getStrArr(ConfigNodes.UNCLAIMED_ZONE_IGNORE)));
				
			} else
				setNewProperty(root.getRoot(), (config.get(root.getRoot().toLowerCase()) != null) ? config.get(root.getRoot().toLowerCase()) : root.getDefault());

		}

		config = newConfig;
		newConfig = null;
	}
	
	private static String convertIds(List<String> list) {
		
		int value;
		List<String> newValues = new ArrayList<String>();
		
		for (String id : list) {
				
			try {
				
				// Try to read a value
				value = Integer.parseInt(id);
				newValues.add(BukkitTools.getMaterial(value).name());
				
			} catch (NumberFormatException e) {
				
				// Is already a string.
				newValues.add(id);
				
			} catch (NullPointerException e) {
				
				// Assume modded item.
				if (!id.startsWith("X")) {
					
					// Prepend an X
					newValues.add("X" + id);
				} else {
					
					// Already has an X
					newValues.add(id);
				}
			}
			
		}
		
		return StringMgmt.join(newValues, ",");
	}

	private static void setDefaultLevels() {

		addComment(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), "", "# default Town levels.");
		if (!config.contains(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot())) {
			// List<Map<String, Object>> townLevels =
			// config.getMapList(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot());

			// if (townLevels == null || townLevels.isEmpty() ||
			// townLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<Map<String, Object>>();
			Map<String, Object> level = new HashMap<String, Object>();
			level.put("numResidents", 0);
			level.put("namePrefix", "");
			level.put("namePostfix", " Ruins");
			level.put("mayorPrefix", "Spirit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 1);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 1);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Settlement)");
			level.put("mayorPrefix", "Hermit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 16);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 2);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Hamlet)");
			level.put("mayorPrefix", "Chief ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 32);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 6);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Village)");
			level.put("mayorPrefix", "Baron Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 96);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Town)");
			level.put("mayorPrefix", "Viscount ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 160);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 14);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large Town)");
			level.put("mayorPrefix", "Count Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 224);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "");
			level.put("namePostfix", " (City)");
			level.put("mayorPrefix", "Earl ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 320);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 24);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large City)");
			level.put("mayorPrefix", "Duke ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 384);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 28);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Metropolis)");
			level.put("mayorPrefix", "Lord ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 448);
			level.put("upkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), levels);
		} else {
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()));
		}

		addComment(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), "", "# default Nation levels.");

		if (!config.contains(ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
			// List<Map<String, Object>> nationLevels =
			// config.getMapList(ConfigNodes.LEVELS_NATION_LEVEL.getRoot());

			// if (nationLevels == null || nationLevels.isEmpty() ||
			// nationLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<Map<String, Object>>();
			Map<String, Object> level = new HashMap<String, Object>();
			level.put("numResidents", 0);
			level.put("namePrefix", "Land of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Leader ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 10);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "Federation of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Count ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 20);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "Dominion of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Duke ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 40);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 30);
			level.put("namePrefix", "Kingdom of ");
			level.put("namePostfix", " (Nation)");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "King ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 60);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 40);
			level.put("namePrefix", "The ");
			level.put("namePostfix", " Empire");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "Emperor ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 100);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			level.put("numResidents", 60);
			level.put("namePrefix", "The ");
			level.put("namePostfix", " Realm");
			level.put("capitalPrefix", "");
			level.put("capitalPostfix", "");
			level.put("kingPrefix", "God Emperor ");
			level.put("kingPostfix", "");
			level.put("townBlockLimitBonus", 140);
			level.put("upkeepModifier", 1.0);
			level.put("nationTownUpkeepModifier", 1.0);
			levels.add(new HashMap<String, Object>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), levels);
		} else
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_NATION_LEVEL.getRoot()));
	}

	public static String[] getRegistrationMsg(String name) {

		return parseString(String.format(getLangString("MSG_REGISTRATION"), name));
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

	public static String[] getNewKingMsg(String who, String nation) {

		return parseString(String.format(getLangString("MSG_NEW_KING"), who, nation));
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
	
	//Need other languages Methods
	public static String[] getWarTimeScoreNationEliminatedMsg(Town town, int n, Nation fallenNation) {

		return parseString(String.format(getLangString("MSG_WAR_SCORE_NATION_ELIM"), town.getName(), n, fallenNation.getName()));
	}
	
	public static String[] getWarTimeScoreTownEliminatedMsg(Town town, int n, Town fallenTown, int fallenTownBlocks) {

		return parseString(String.format(getLangString("MSG_WAR_SCORE_TOWN_ELIM"), town.getName(), n, fallenTown.getName(), fallenTownBlocks));
	}
	
	public static String[] getWarTimeScoreTownBlockEliminatedMsg(Town town, int n, TownBlock fallenTownBlock) {

		String townBlockName = "";
		try {
			Town fallenTown = ((TownBlock)fallenTownBlock).getTown();
			townBlockName = "[" + fallenTown.getName() + "](" + ((TownBlock)fallenTownBlock).getCoord().toString() + ")";
		} catch (NotRegisteredException e) {
			townBlockName = "(" + ((TownBlock)fallenTownBlock).getCoord().toString() + ")";
		}
		return parseString(String.format(getLangString("MSG_WAR_SCORE_TOWNBLOCK_ELIM"), town.getName(), n, townBlockName));
	}
	        
	public static String[] getWarTimeScorePlayerKillMsg(Player attacker, Player dead, int n, Town attackingTown) {

		return parseString(String.format(getLangString("MSG_WAR_SCORE_PLAYER_KILL"), attacker.getName(), dead.getName(), n, attackingTown.getName()));
	}
	
	public static String[] getWarTimeScorePlayerKillMsg(Player attacker, Player dead, Player defender, int n, Town attackingTown) {

		return parseString(String.format(getLangString("MSG_WAR_SCORE_PLAYER_KILL_DEFENDING"), attacker.getName(), dead.getName(), defender.getName(), n, attackingTown.getName()));
	}
	
	public static String[] getWarTimeKingKilled(Nation kingsNation) {

		return parseString(String.format(getLangString("MSG_WAR_KING_KILLED"), kingsNation.getName()));
	}
	
	public static String[] getWarTimeMayorKilled(Town mayorsTown) {

		return parseString(String.format(getLangString("MSG_WAR_MAYOR_KILLED"), mayorsTown.getName()));
	}
	
	public static String[] getWarTimeWinningNationSpoilsMsg(Nation winningNation, String money)
	{
		return parseString(String.format(getLangString("MSG_WAR_WINNING_NATION_SPOILS"), winningNation.getName(), money));
	}
	
	public static String[] getWarTimeWinningTownSpoilsMsg(Town winningTown, String money, int score)
	{
		return parseString(String.format(getLangString("MSG_WAR_WINNING_TOWN_SPOILS"), winningTown.getName(), money, score));
	}
	//Score Methods

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

	public static String[] getBuyResidentPlotMsg(String who, String owner, Double price) {

		return parseString(String.format(getLangString("MSG_BUY_RESIDENT_PLOT"), who, owner, price));
	}

	public static String[] getPlotForSaleMsg(String who, WorldCoord worldCoord) {

		return parseString(String.format(getLangString("MSG_PLOT_FOR_SALE"), who, worldCoord.toString()));
	}

	public static String getMayorAbondonMsg() {

		return parseSingleLineString(getLangString("MSG_MAYOR_ABANDON"));
	}

	public static String getNotPermToNewTownLine() {

		return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_TOWN"));
	}

	public static String getNotPermToNewNationLine() {

		return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_NATION"));
	}

	public static String getKingPrefix(Resident resident) {

		try {
			return (String) getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_PREFIX);
		} catch (NotRegisteredException e) {
			sendError("getKingPrefix.");
			return "";
		}
	}

	public static String getMayorPrefix(Resident resident) {

		try {
			return (String) getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_PREFIX);
		} catch (NotRegisteredException e) {
			sendError("getMayorPrefix.");
			return "";
		}
	}

	public static String getCapitalPostfix(Town town) {

		try {
			return (String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX);
		} catch (NotRegisteredException e) {
			sendError("getCapitalPostfix.");
			return "";
		}
	}

	public static String getTownPostfix(Town town) {

		try {
			return (String) getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX);
		} catch (Exception e) {
			sendError("getTownPostfix.");
			return "";
		}
	}

	public static String getNationPostfix(Nation nation) {

		try {
			return (String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_POSTFIX);
		} catch (Exception e) {
			sendError("getNationPostfix.");
			return "";
		}
	}

	public static String getNationPrefix(Nation nation) {

		try {
			return (String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_PREFIX);
		} catch (Exception e) {
			sendError("getNationPrefix.");
			return "";
		}
	}

	public static String getTownPrefix(Town town) {

		try {
			return (String) getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX);
		} catch (Exception e) {
			sendError("getTownPrefix.");
			return "";
		}
	}

	public static String getCapitalPrefix(Town town) {

		try {
			return (String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX);
		} catch (NotRegisteredException e) {
			sendError("getCapitalPrefix.");
			return "";
		}
	}

	public static String getKingPostfix(Resident resident) {

		try {
			return (String) getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_POSTFIX);
		} catch (NotRegisteredException e) {
			sendError("getKingPostfix.");
			return "";
		}
	}

	public static String getMayorPostfix(Resident resident) {

		try {
			return (String) getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_POSTFIX);
		} catch (NotRegisteredException e) {
			sendError("getMayorPostfix.");
			return "";
		}
	}

	public static String getNPCPrefix() {

		return getString(ConfigNodes.FILTERS_NPC_PREFIX.getRoot(), ConfigNodes.FILTERS_NPC_PREFIX.getDefault());
	}

	public static long getInactiveAfter() {

		return getSeconds(ConfigNodes.RES_SETTING_INACTIVE_AFTER_TIME);
	}

	public static boolean getBedUse() {

		return getBoolean(ConfigNodes.RES_SETTING_DENY_BED_USE);
	}

	public static String getLoadDatabase() {

		return getString(ConfigNodes.PLUGIN_DATABASE_LOAD);
	}

	public static String getSaveDatabase() {

		return getString(ConfigNodes.PLUGIN_DATABASE_SAVE);
	}

	// SQL
	public static String getSQLHostName() {

		return getString(ConfigNodes.PLUGIN_DATABASE_HOSTNAME);
	}

	public static String getSQLPort() {

		return getString(ConfigNodes.PLUGIN_DATABASE_PORT);
	}

	public static String getSQLDBName() {

		return getString(ConfigNodes.PLUGIN_DATABASE_DBNAME);
	}

	public static String getSQLTablePrefix() {

		return getString(ConfigNodes.PLUGIN_DATABASE_TABLEPREFIX);
	}

	public static String getSQLUsername() {

		return getString(ConfigNodes.PLUGIN_DATABASE_USERNAME);
	}

	public static String getSQLPassword() {

		return getString(ConfigNodes.PLUGIN_DATABASE_PASSWORD);
	}

	public static int getMaxTownBlocks(Town town) {

		int ratio = getTownBlockRatio();
		int n = town.getBonusBlocks() + town.getPurchasedBlocks();

		if (ratio == 0) {
			n += (Integer) getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);

		} else
			n += town.getNumResidents() * ratio;

		if (town.hasNation())
			try {
				n += (Integer) getNationLevel(town.getNation()).get(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS);
			} catch (NotRegisteredException e) {
			}

		return n;
	}

	public static int getNationBonusBlocks(Nation nation) {

		return (Integer) getNationLevel(nation).get(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS);
	}

	public static int getNationBonusBlocks(Town town) {

		if (town.hasNation())
			try {
				return getNationBonusBlocks(town.getNation());
			} catch (NotRegisteredException e) {
			}

		return 0;
	}

	public static int getTownBlockRatio() {

		return getInt(ConfigNodes.TOWN_TOWN_BLOCK_RATIO);
	}

	public static int getTownBlockSize() {

		return getInt(ConfigNodes.TOWN_TOWN_BLOCK_SIZE);
	}

	public static boolean getFriendlyFire() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_FRIENDLY_FIRE);
	}

//	public static boolean isTownCreationAdminOnly() {
//		return getBoolean(ConfigNodes.PERMS_TOWN_CREATION_ADMIN_ONLY);
//	}
//	public static boolean isNationCreationAdminOnly() {
//		return getBoolean(ConfigNodes.PERMS_NATION_CREATION_ADMIN_ONLY);
//	}

	/*
	 * public static boolean isUsingRegister() {
	 * return getBoolean(ConfigNodes.PLUGIN_USING_REGISTER);
	 * }
	 * 
	 * public static void setUsingRegister(boolean newSetting) {
	 * setProperty(ConfigNodes.PLUGIN_USING_REGISTER.getRoot(), newSetting);
	 * }
	 * 
	 * public static boolean isUsingIConomy() {
	 * return getBoolean(ConfigNodes.PLUGIN_USING_ICONOMY);
	 * }
	 * 
	 * public static void setUsingIConomy(boolean newSetting) {
	 * setProperty(ConfigNodes.PLUGIN_USING_ICONOMY.getRoot(), newSetting);
	 * }
	 */
	public static boolean isUsingEconomy() {

		return getBoolean(ConfigNodes.PLUGIN_USING_ECONOMY);
		// return (isUsingIConomy() || isUsingRegister());
	}

	public static boolean isFakeResident(String name) {

		return getString(ConfigNodes.PLUGIN_MODS_FAKE_RESIDENTS).toLowerCase().contains(name.toLowerCase());
	}

	public static boolean isUsingEssentials() {

		return getBoolean(ConfigNodes.PLUGIN_USING_ESSENTIALS);
	}

	public static void setUsingEssentials(boolean newSetting) {

		setProperty(ConfigNodes.PLUGIN_USING_ESSENTIALS.getRoot(), newSetting);
	}

	public static double getNewTownPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_NEW_TOWN);
	}

	public static double getNewNationPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_NEW_NATION);
	}

	public static boolean getUnclaimedZoneBuildRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_BUILD);
	}

	public static boolean getUnclaimedZoneDestroyRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_DESTROY);
	}

	public static boolean getUnclaimedZoneItemUseRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_ITEM_USE);
	}

	public static boolean getDebug() {

		return getBoolean(ConfigNodes.PLUGIN_DEBUG_MODE);
	}
	
	public static String getTool() {

		return getString(ConfigNodes.PLUGIN_INFO_TOOL);
	}

	public static void setDebug(boolean b) {

		setProperty(ConfigNodes.PLUGIN_DEBUG_MODE.getRoot(), b);
	}

	public static boolean getShowTownNotifications() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_SHOW_TOWN_NOTIFICATIONS);
	}

	public static String getUnclaimedZoneName() {

		return getLangString("UNCLAIMED_ZONE_NAME");
	}

	public static int getMaxTitleLength() {

		return getInt(ConfigNodes.FILTERS_MODIFY_CHAT_MAX_LGTH);
	}

	public static int getMaxNameLength() {

		return getInt(ConfigNodes.FILTERS_MAX_NAME_LGTH);
	}

	public static long getDeleteTime() {

		return getSeconds(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TIME);
	}

	public static boolean isDeleteEcoAccount() {

		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_ECO);
	}

	public static boolean isDeletingOldResidents() {

		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_ENABLE);
	}

	public static int getWarTimeWarningDelay() {

		return getInt(ConfigNodes.WAR_EVENT_WARNING_DELAY);
	}

	public static boolean isWarTimeTownsNeutral() {

		return getBoolean(ConfigNodes.WAR_EVENT_TOWNS_NEUTRAL);
	}

	public static boolean isAllowWarBlockGriefing() {

		return getBoolean(ConfigNodes.WAR_EVENT_BLOCK_GRIEFING);
	}

	public static int getWarzoneTownBlockHealth() {

		return getInt(ConfigNodes.WAR_EVENT_TOWN_BLOCK_HP);
	}

	public static int getWarzoneHomeBlockHealth() {

		return getInt(ConfigNodes.WAR_EVENT_HOME_BLOCK_HP);
	}

	public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord) {

		return getWarTimeLoseTownBlockMsg(worldCoord, "");
	}

	public static String getDefaultTownName() {

		return getString(ConfigNodes.RES_SETTING_DEFAULT_TOWN_NAME);
	}

	public static int getWarPointsForTownBlock() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_TOWNBLOCK);
	}

	public static int getWarPointsForTown() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_TOWN);
	}

	public static int getWarPointsForNation() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_NATION);
	}

	public static int getWarPointsForKill() {

		return getInt(ConfigNodes.WAR_EVENT_POINTS_KILL);
	}

	public static int getMinWarHeight() {

		return getInt(ConfigNodes.WAR_EVENT_MIN_HEIGHT);
	}

	public static List<String> getWorldMobRemovalEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading World Mob removal entities. ");
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WORLD);
	}

	public static List<String> getTownMobRemovalEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading Town Mob removal entities. ");
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_TOWN);
	}

	public static boolean isEconomyAsync() {

		return getBoolean(ConfigNodes.ECO_USE_ASYNC);
	}

	public static boolean isRemovingVillagerBabiesWorld() {

		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_VILLAGER_BABIES_WORLD);
	}

	public static boolean isCreatureTriggeringPressurePlateDisabled() {

		return getBoolean(ConfigNodes.PROT_MOB_DISABLE_TRIGGER_PRESSURE_PLATE_STONE);
	}

	public static boolean isRemovingVillagerBabiesTown() {

		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_VILLAGER_BABIES_TOWN);
	}

	public static List<String> getWildExplosionProtectionEntities() {

		if (getDebug())
			System.out.println("[Towny] Debug: Wilderness explosion protection entities. ");
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_ENTITY_REVERT_LIST);
	}

	public static long getMobRemovalSpeed() {

		return getSeconds(ConfigNodes.PROT_MOB_REMOVE_SPEED);
	}

	public static long getHealthRegenSpeed() {

		return getSeconds(ConfigNodes.GTOWN_SETTINGS_REGEN_SPEED);
	}

	public static boolean hasHealthRegen() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_REGEN_ENABLE);
	}

	public static boolean getTownDefaultPublic() {

		return getBoolean(ConfigNodes.TOWN_DEF_PUBLIC);
	}

	public static boolean getTownDefaultOpen() {

		return getBoolean(ConfigNodes.TOWN_DEF_OPEN);
	}

	public static double getTownDefaultTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_TAX);
	}

	public static double getTownDefaultShopTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_SHOP_TAX);
	}
	
	public static double getTownDefaultEmbassyTax() {

		return getDouble(ConfigNodes.TOWN_DEF_TAXES_EMBASSY_TAX);
	}
	
	public static double getTownDefaultPlotTax() {
		
		return getDouble(ConfigNodes.TOWN_DEF_TAXES_PLOT_TAX);		
	}

	public static boolean getTownDefaultTaxPercentage() {

		return getBoolean(ConfigNodes.TOWN_DEF_TAXES_TAXPERCENTAGE);
	}
	
	public static boolean hasTownLimit() {

		return getTownLimit() != 0;
	}

	public static int getTownLimit() {

		return getInt(ConfigNodes.TOWN_LIMIT);
	}

	public static int getMaxPurchedBlocks() {

		return getInt(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS);
	}

	public static boolean isSellingBonusBlocks() {

		return getMaxPurchedBlocks() != 0;
	}

	public static double getPurchasedBonusBlocksCost() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK);
	}

	public static double getPurchasedBonusBlocksIncreaseValue() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE);
	}

	public static double getNationNeutralityCost() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY);
	}

	public static boolean isAllowingOutposts() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTPOSTS);
	}

	public static double getOutpostCost() {

		return getDouble(ConfigNodes.ECO_PRICE_OUTPOST);
	}

	private static List<String> getSwitchMaterials() {

		return getStrArr(ConfigNodes.PROT_SWITCH_MAT);
	}
	
	private static List<String> getItemUseMaterials() {

		return getStrArr(ConfigNodes.PROT_ITEM_USE_MAT);
	}
	
	public static boolean isSwitchMaterial(String mat) {

		return getSwitchMaterials().contains(mat);
	}

	public static boolean isItemUseMaterial(String mat) {

		return getItemUseMaterials().contains(mat);
	}
	
	public static List<String> getUnclaimedZoneIgnoreMaterials() {

		return getStrArr(ConfigNodes.UNCLAIMED_ZONE_IGNORE);
	}

	public static List<String> getEntityTypes() {

		return getStrArr(ConfigNodes.PROT_MOB_TYPES);
	}
	
	public static List<String> getPotionTypes() {

		return getStrArr(ConfigNodes.PROT_POTION_TYPES);
	}

	private static void setProperty(String root, Object value) {

		config.set(root.toLowerCase(), value.toString());
	}

	private static void setNewProperty(String root, Object value) {

		if (value == null) {
			// System.out.print("value is null for " + root.toLowerCase());
			value = "";
		}
		newConfig.set(root.toLowerCase(), value.toString());
	}

	public static Object getProperty(String root) {

		return config.get(root.toLowerCase());
	}

	public static double getClaimPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK);
	}

	public static boolean getUnclaimedZoneSwitchRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_SWITCH);
	}

	public static boolean getEndermanProtect() {

		return getBoolean(ConfigNodes.NWS_WORLD_ENDERMAN);
	}

	public static String getUnclaimedPlotName() {

		return getLangString("UNCLAIMED_PLOT_NAME");
	}

	public static long getDayInterval() {

		// return TimeTools.secondsFromDhms("24h");
		return getSeconds(ConfigNodes.PLUGIN_DAY_INTERVAL);
	}

	public static long getNewDayTime() {

		long time = getSeconds(ConfigNodes.PLUGIN_NEWDAY_TIME);
		long day = getDayInterval();
		if (time > day) {
			setProperty(ConfigNodes.PLUGIN_NEWDAY_TIME.getRoot(), day);
			return day;
		}
		return time;
	}

	public static boolean isAllowingTownSpawn() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN);
	}

	public static boolean isAllowingPublicTownSpawnTravel() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL);
	}

	public static List<String> getDisallowedTownSpawnZones() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading disallowed town spawn zones. ");
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_PREVENT_TOWN_SPAWN_IN);
	}

	public static boolean isTaxingDaily() {

		return getBoolean(ConfigNodes.ECO_DAILY_TAXES_ENABLED);
	}

	public static double getMaxTax() {

		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TAX);
	}

	public static double getMaxTaxPercent() {

		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TAX_PERCENT);
	}

	public static boolean isBackingUpDaily() {

		return getBoolean(ConfigNodes.PLUGIN_DAILY_BACKUPS);
	}

	public static double getBaseSpoilsOfWar() {

		return getDouble(ConfigNodes.WAR_EVENT_BASE_SPOILS);
	}
	
	public static boolean getOnlyAttackEdgesInWar() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_ENEMY_ONLY_ATTACK_BORDER);
	}
	
	public static boolean getPlotsHealableInWar() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_PLOTS_HEALABLE);
	}
	
	public static boolean getPlotsFireworkOnAttacked() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_PLOTS_FIREWORK_ON_ATTACKED);
	}

	public static double getWartimeDeathPrice() {

		return getDouble(ConfigNodes.WAR_EVENT_PRICE_DEATH);
	}

	public static boolean isChargingDeath() {
		
		return (getDeathPrice()>0 || getDeathPriceTown()>0 || getDeathPriceNation()>0 );
	}
	
	public static boolean isDeathPriceType() {

		return getString(ConfigNodes.ECO_PRICE_DEATH_TYPE).equalsIgnoreCase("fixed");
	}

	public static boolean isDeathPricePVPOnly() {

		return getBoolean(ConfigNodes.ECO_PRICE_DEATH_PVP_ONLY);
		
	}	
	
	public static double getDeathPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH);
	}

	public static double getDeathPriceTown() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH_TOWN);
	}

	public static double getDeathPriceNation() {

		return getDouble(ConfigNodes.ECO_PRICE_DEATH_NATION);
	}
	
	public static boolean isJailingAttackingEnemies() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_ENEMIES);	
	}

	public static boolean JailAllowsEnderPearls() {
		
		return getBoolean(ConfigNodes.JAIL_JAIL_ALLOWS_ENDER_PEARLS);	
	}
	
	public static boolean JailDeniesTownLeave() {
		
		return getBoolean(ConfigNodes.JAIL_JAIL_DENIES_TOWN_LEAVE);	
	}	

	public static boolean isAllowingBail() {
		
		return getBoolean(ConfigNodes.JAIL_BAIL_IS_ALLOWING_BAIL);	
	}
	
	public static double getBailAmount() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT);
	}

	public static double getWartimeTownBlockLossPrice() {

		return getDouble(ConfigNodes.WAR_EVENT_TOWN_BLOCK_LOSS_PRICE);
	}

	public static boolean isDevMode() {

		return getBoolean(ConfigNodes.PLUGIN_DEV_MODE_ENABLE);
	}

	public static void setDevMode(boolean choice) {

		setProperty(ConfigNodes.PLUGIN_DEV_MODE_ENABLE.getRoot(), choice);
	}

	public static String getDevName() {

		return getString(ConfigNodes.PLUGIN_DEV_MODE_DEV_NAME);
	}

	public static boolean isDeclaringNeutral() {

		return getBoolean(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL);
	}

	public static void setDeclaringNeutral(boolean choice) {

		setProperty(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL.getRoot(), choice);
	}

	public static boolean isRemovingOnMonarchDeath() {

		return getBoolean(ConfigNodes.WAR_EVENT_REMOVE_ON_MONARCH_DEATH);
	}

	public static double getTownUpkeepCost(Town town) {

		double multiplier;

		if (town != null) {
			if (isUpkeepByPlot()) {
				multiplier = town.getTownBlocks().size(); // town.getTotalBlocks();
			} else {
				multiplier = Double.valueOf(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
			}
		} else
			multiplier = 1.0;

		if (town.hasNation()) {
			double nationMultiplier = 1.0;
			try {
				nationMultiplier = Double.valueOf(getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_TOWN_UPKEEP_MULTIPLIER).toString());
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotRegisteredException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return (getTownUpkeep() * multiplier) * nationMultiplier ;
		} else 		
			return getTownUpkeep() * multiplier ;
	}

	public static double getTownUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP);
	}

	public static boolean isUpkeepByPlot() {

		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED);
	}

	public static boolean isUpkeepPayingPlots() {

		return getBoolean(ConfigNodes.ECO_UPKEEP_PLOTPAYMENTS);
	}

	public static double getNationUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_UPKEEP);
	}

	public static double getNationUpkeepCost(Nation nation) {

		double multiplier;

		if (nation != null)
			multiplier = Double.valueOf(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
		else
			multiplier = 1.0;

		return getNationUpkeep() * multiplier;
	}

	public static String getFlatFileBackupType() {

		return getString(ConfigNodes.PLUGIN_FLATFILE_BACKUP);
	}

	public static long getBackupLifeLength() {

		long t = TimeTools.getMillis(TownySettings.getString(ConfigNodes.PLUGIN_BACKUPS_ARE_DELETED_AFTER));
		long minT = TimeTools.getMillis("1d");
		if (t >= 0 && t < minT)
			t = minT;
		return t;
	}

	public static boolean isUsingTowny() {

		return getBoolean(ConfigNodes.NWS_WORLD_USING_TOWNY);
	}

	public static boolean isPvP() {

		return getBoolean(ConfigNodes.NWS_WORLD_PVP);
	}

	public static boolean isForcingPvP() {

		return getBoolean(ConfigNodes.NWS_FORCE_PVP_ON);
	}

	public static boolean isPlayerTramplingCropsDisabled() {

		return getBoolean(ConfigNodes.NWS_DISABLE_PLAYER_CROP_TRAMPLING);
	}

	public static boolean isCreatureTramplingCropsDisabled() {

		return getBoolean(ConfigNodes.NWS_DISABLE_CREATURE_CROP_TRAMPLING);
	}

	public static boolean isWorldMonstersOn() {

		return getBoolean(ConfigNodes.NWS_WORLD_MONSTERS_ON);
	}

	public static boolean isExplosions() {

		return getBoolean(ConfigNodes.NWS_WORLD_EXPLOSION);
	}

	public static boolean isForcingExplosions() {

		return getBoolean(ConfigNodes.NWS_FORCE_EXPLOSIONS_ON);
	}

	public static boolean isForcingMonsters() {

		return getBoolean(ConfigNodes.NWS_FORCE_TOWN_MONSTERS_ON);
	}

	public static boolean isFire() {

		return getBoolean(ConfigNodes.NWS_WORLD_FIRE);
	}

	public static boolean isForcingFire() {

		return getBoolean(ConfigNodes.NWS_FORCE_FIRE_ON);
	}

	public static boolean isUsingPlotManagementDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE_ENABLE);
	}

	public static List<String> getPlotManagementDeleteIds() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE);
	}

	public static boolean isUsingPlotManagementMayorDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE_ENABLE);
	}

	public static List<String> getPlotManagementMayorDelete() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE);
	}

	public static boolean isUsingPlotManagementRevert() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_ENABLE);
	}

	public static long getPlotManagementSpeed() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_TIME);
	}

	public static boolean isUsingPlotManagementWildRegen() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_ENABLE);
	}

	public static long getPlotManagementWildRegenDelay() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_TIME);
	}

	public static List<String> getPlotManagementIgnoreIds() {

		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_IGNORE);
	}

	public static boolean isTownRespawning() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_TOWN_RESPAWN);
	}

	public static boolean isTownRespawningInOtherWorlds() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_TOWN_RESPAWN_SAME_WORLD_ONLY);
	}

	public static boolean isTownyUpdating(String currentVersion) {

		if (isTownyUpToDate(currentVersion))
			return false;
		else
			return true; // Assume
	}

	public static int getMinBukkitVersion() {

		return getInt(ConfigNodes.VERSION_BUKKIT);
	}

	public static boolean isTownyUpToDate(String currentVersion) {

		return currentVersion.equals(getLastRunVersion(currentVersion));
	}

	public static String getLastRunVersion(String currentVersion) {

		return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
	}

	public static void setLastRunVersion(String currentVersion) {

		setProperty(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
		config.save();
	}

	public static int getMinDistanceFromTownHomeblocks() {

		return getInt(ConfigNodes.TOWN_MIN_DISTANCE_FROM_TOWN_HOMEBLOCK);
	}

	public static int getMinDistanceFromTownPlotblocks() {

		return getInt(ConfigNodes.TOWN_MIN_PLOT_DISTANCE_FROM_TOWN_PLOT);

	}

	public static int getMaxDistanceBetweenHomeblocks() {

		return getInt(ConfigNodes.TOWN_MAX_DISTANCE_BETWEEN_HOMEBLOCKS);
	}

	public static int getMaxResidentPlots(Resident resident) {

		int maxPlots = TownyUniverse.getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_PLOTS.getNode());
		if (maxPlots == -1)
			maxPlots = getInt(ConfigNodes.TOWN_MAX_PLOTS_PER_RESIDENT);
		return maxPlots;
	}

	public static int getMaxResidentOutposts(Resident resident) {

		int maxOutposts = TownyUniverse.getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_OUTPOSTS.getNode());
		return maxOutposts;
	}

	public static boolean getPermFlag_Resident_Friend_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_BUILD);
	}

	public static boolean getPermFlag_Resident_Friend_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_DESTROY);
	}

	public static boolean getPermFlag_Resident_Friend_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Friend_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_FR_SWITCH);
	}

	public static boolean getPermFlag_Resident_Ally_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_BUILD);
	}

	public static boolean getPermFlag_Resident_Ally_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_DESTROY);
	}

	public static boolean getPermFlag_Resident_Ally_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Ally_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_ALLY_SWITCH);
	}

	public static boolean getPermFlag_Resident_Outsider_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_BUILD);
	}

	public static boolean getPermFlag_Resident_Outsider_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_DESTROY);
	}

	public static boolean getPermFlag_Resident_Outsider_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Outsider_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_SWITCH);
	}

	public static boolean getPermFlag_Town_Default_PVP() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_PVP);
	}

	public static boolean getPermFlag_Town_Default_FIRE() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_FIRE);
	}

	public static boolean getPermFlag_Town_Default_Explosion() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_EXPLOSION);
	}

	public static boolean getPermFlag_Town_Default_Mobs() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_DEF_MOBS);
	}

	public static boolean getPermFlag_Town_Resident_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_BUILD);
	}

	public static boolean getPermFlag_Town_Resident_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_DESTROY);
	}

	public static boolean getPermFlag_Town_Resident_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Resident_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_RES_SWITCH);
	}

	public static boolean getPermFlag_Town_Ally_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_BUILD);
	}

	public static boolean getPermFlag_Town_Ally_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_DESTROY);
	}

	public static boolean getPermFlag_Town_Ally_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Ally_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_SWITCH);
	}

	public static boolean getPermFlag_Town_Outsider_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_BUILD);
	}

	public static boolean getPermFlag_Town_Outsider_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_DESTROY);
	}

	public static boolean getPermFlag_Town_Outsider_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Outsider_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_SWITCH);
	}

	public static boolean getDefaultResidentPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Friend_Build();
			case DESTROY:
				return getPermFlag_Resident_Friend_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Friend_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Friend_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Resident_Build();
			case DESTROY:
				return getPermFlag_Town_Resident_Destroy();
			case SWITCH:
				return getPermFlag_Town_Resident_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Resident_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultAllyPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Ally_Build();
			case DESTROY:
				return getPermFlag_Resident_Ally_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Ally_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Ally_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Ally_Build();
			case DESTROY:
				return getPermFlag_Town_Ally_Destroy();
			case SWITCH:
				return getPermFlag_Town_Ally_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Ally_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultOutsiderPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Outsider_Build();
			case DESTROY:
				return getPermFlag_Resident_Outsider_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Outsider_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Outsider_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Outsider_Build();
			case DESTROY:
				return getPermFlag_Town_Outsider_Destroy();
			case SWITCH:
				return getPermFlag_Town_Outsider_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Outsider_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultPermission(TownBlockOwner owner, PermLevel level, ActionType type) {

		switch (level) {
		case RESIDENT:
			return getDefaultResidentPermission(owner, type);
		case ALLY:
			return getDefaultAllyPermission(owner, type);
		case OUTSIDER:
			return getDefaultOutsiderPermission(owner, type);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static boolean isLogging() {

		return getBoolean(ConfigNodes.PLUGIN_LOGGING);
	}

	public static boolean isUsingQuestioner() {

		return getBoolean(ConfigNodes.PLUGIN_USING_QUESTIONER_ENABLE);
	}

	public static void setUsingQuestioner(boolean newSetting) {

		setProperty(ConfigNodes.PLUGIN_USING_QUESTIONER_ENABLE.getRoot(), newSetting);
	}

	public static String questionerAccept() {

		return getString(ConfigNodes.PLUGIN_QUESTIONER_ACCEPT);
	}

	public static String questionerDeny() {

		return getString(ConfigNodes.PLUGIN_QUESTIONER_DENY);
	}

	public static boolean isAppendingToLog() {

		return !getBoolean(ConfigNodes.PLUGIN_RESET_LOG_ON_BOOT);
	}

	public static boolean isUsingPermissions() {

		return getBoolean(ConfigNodes.PLUGIN_USING_PERMISSIONS);
	}

	public static void setUsingPermissions(boolean newSetting) {

		setProperty(ConfigNodes.PLUGIN_USING_PERMISSIONS.getRoot(), newSetting);
	}

	public static String getNameFilterRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_FILTER_REGEX);
	}

	public static String getNameCheckRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_CHECK_REGEX);
	}

	public static String getStringCheckRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_STRING_CHECK_REGEX);
	}

	public static String getNameRemoveRegex() {

		return getString(ConfigNodes.FILTERS_REGEX_NAME_REMOVE_REGEX);
	}

	public static boolean isUsingCheatProtection() {

		return getBoolean(ConfigNodes.PROT_CHEAT);
	}

	public static long getRegenDelay() {

		return getSeconds(ConfigNodes.PROT_REGEN_DELAY);
	}

	public static int getTeleportWarmupTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_TIMER);
	}
	
	public static String getTownAccountPrefix() {

		return getString(ConfigNodes.ECO_TOWN_PREFIX);
	}

	public static String getNationAccountPrefix() {

		return getString(ConfigNodes.ECO_NATION_PREFIX);
	}

	public static double getTownBankCap() {

		return getDouble(ConfigNodes.ECO_BANK_CAP_TOWN);
	}

	public static double getNationBankCap() {

		return getDouble(ConfigNodes.ECO_BANK_CAP_NATION);
	}

	public static boolean getTownBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWLS);
	}

	public static void SetTownBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWLS.getRoot(), newSetting);
	}

	public static boolean geNationBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWLS);
	}

	public static void SetNationBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWLS.getRoot(), newSetting);
	}

	@Deprecated
	public static boolean isValidRegionName(String name) {

		return !NameValidation.isBlacklistName(name);
	}

	@Deprecated
	public static boolean isValidName(String name) {

		return NameValidation.isValidName(name);
	}

	@Deprecated
	public static String filterName(String input) {

		return NameValidation.filterName(input);
	}

	public static boolean isDisallowOneWayAlliance() {
		
		return getBoolean(ConfigNodes.WAR_DISALLOW_ONE_WAY_ALLIANCE);
	}
	
	public static int getNumResidentsJoinNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION);
	}
	
	public static int getNumResidentsCreateNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION);
	}
	
	public static boolean isRefundNationDisbandLowResidents() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_REFUND_DISBAND_LOW_RESIDENTS);
	}
	
	public static List<String> getFarmPlotBlocks() {
		return getStrArr(ConfigNodes.GTOWN_FARM_PLOT_ALLOW_BLOCKS);
	}
	
	public static List<String> getFarmAnimals() {
		return getStrArr(ConfigNodes.GTOWN_FARM_ANIMALS);
	}
}