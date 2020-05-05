package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.event.NationBonusCalculationEvent;
import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepPenalityCalculationEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.NationSpawnLevel.NSpawnLevel;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownSpawnLevel.SpawnLevel;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;
import com.palmergames.bukkit.towny.war.siegewar.objects.HeldItemsCombination;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

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

public class TownySettings {

	// Town Level
	public enum TownLevel {
		NAME_PREFIX, NAME_POSTFIX, MAYOR_PREFIX, MAYOR_POSTFIX, TOWN_BLOCK_LIMIT, UPKEEP_MULTIPLIER, OUTPOST_LIMIT, TOWN_BLOCK_BUY_BONUS_LIMIT
	}

	// Nation Level
	public enum NationLevel {
		NAME_PREFIX, NAME_POSTFIX, CAPITAL_PREFIX, CAPITAL_POSTFIX, KING_PREFIX, KING_POSTFIX, TOWN_BLOCK_LIMIT_BONUS, UPKEEP_MULTIPLIER, NATION_TOWN_UPKEEP_MULTIPLIER, NATIONZONES_SIZE, NATION_BONUS_OUTPOST_LIMIT
	}

	// private static Pattern namePattern = null;
	private static CommentedConfiguration config, newConfig, language, newLanguage, playermap;

	private static final SortedMap<Integer, Map<TownySettings.TownLevel, Object>> configTownLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.TownLevel, Object>>(Collections.reverseOrder()));
	private static final SortedMap<Integer, Map<TownySettings.NationLevel, Object>> configNationLevel = Collections.synchronizedSortedMap(new TreeMap<Integer, Map<TownySettings.NationLevel, Object>>(Collections.reverseOrder()));
	private static final List<HeldItemsCombination> tacticalVisibilityItems = new ArrayList<>();


	public static void newTownLevel(int numResidents, String namePrefix, String namePostfix, String mayorPrefix, String mayorPostfix, int townBlockLimit, double townUpkeepMultiplier, int townOutpostLimit, int townBlockBuyBonusLimit) {

		ConcurrentHashMap<TownySettings.TownLevel, Object> m = new ConcurrentHashMap<TownySettings.TownLevel, Object>();
		m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
		m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
		m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
		m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
		m.put(TownySettings.TownLevel.UPKEEP_MULTIPLIER, townUpkeepMultiplier);
		m.put(TownySettings.TownLevel.OUTPOST_LIMIT, townOutpostLimit);
		m.put(TownySettings.TownLevel.TOWN_BLOCK_BUY_BONUS_LIMIT, townBlockBuyBonusLimit);
		configTownLevel.put(numResidents, m);
	}

	public static void newNationLevel(int numResidents, String namePrefix, String namePostfix, String capitalPrefix, String capitalPostfix, String kingPrefix, String kingPostfix, int townBlockLimitBonus, double nationUpkeepMultiplier, double nationTownUpkeepMultiplier, int nationZonesSize, int nationBonusOutpostLimit) {

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
		m.put(TownySettings.NationLevel.NATIONZONES_SIZE, nationZonesSize);
		m.put(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT, nationBonusOutpostLimit);
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
	 * @throws IOException if unable to load the Town Levels
	 */
	public static void loadTownLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.town_level");
		for (Map<?, ?> level : levels) {

			try {
				newTownLevel(
						(Integer) level.get("numResidents"),
						(String) level.get("namePrefix"),
						(String) level.get("namePostfix"),
						(String) level.get("mayorPrefix"),
						(String) level.get("mayorPostfix"),
						(Integer) level.get("townBlockLimit"),
						(Double) level.get("upkeepModifier"),
						(Integer) level.get("townOutpostLimit"),
						(Integer) level.get("townBlockBuyBonusLimit")
						);
			} catch (NullPointerException e) {
				System.out.println("Your Towny config.yml's town_level section is out of date.");
				System.out.println("This can be fixed automatically by deleting the town_level section and letting Towny remake it on the next startup.");
				throw new IOException("Config.yml town_levels incomplete.");
			}

		}
	}

	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 *
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:
	 * kingPrefix:kingPostfix
	 *
	 * @throws IOException if Nation Levels cannot be loaded from config
	 */

	public static void loadNationLevelConfig() throws IOException {

		List<Map<?, ?>> levels = config.getMapList("levels.nation_level");
		for (Map<?, ?> level : levels) {

			try {
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
						(level.containsKey("nationTownUpkeepModifier") ? (Double) level.get("nationTownUpkeepModifier") : 1.0),
						(Integer) level.get("nationZonesSize"),
						(Integer) level.get("nationBonusOutpostLimit")
						);
			} catch (Exception e) {
				System.out.println("Your Towny config.yml's nation_level section is out of date.");
				System.out.println("This can be fixed automatically by deleting the nation_level section and letting Towny remake it on the next startup.");
				throw new IOException("Config.yml nation_levels incomplete.");
			}

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

	public static int calcTownLevel(Town town) {
//Creatorfromhell's PR for replacing SortedMap town and nation levels.
//		Integer level = configTownLevel.floorKey(town.getNumResidents());
//
//		if (level != null) return level;
//		return 0;
		if(town.isRuined())
			return 0;

		int n = town.getNumResidents();
		for (Integer level : configTownLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	/**
	 * This method returns the id of the town level
	 *
	 * e.g.
	 * ruins = 0
	 * hamlet = 1
	 * village = 2
	 *
	 * @param town
	 * @return id
	 */
	public static int calcTownLevelId(Town town) {
		if(town.isRuined())
			return 0;

		int townLevelId = -1;
		int numResidents = town.getNumResidents();
		for (Integer level : configTownLevel.keySet()) {
			if (level <= numResidents)
				townLevelId ++;
		}
		return townLevelId;
	}

	public static int calcNationLevel(Nation nation) {
//Creatorfromhell's PR for replacing SortedMap town and nation levels.
//		Integer level = configNationLevel.floorKey(nation.getNumResidents());
//
//		if (level != null) return level;
//		return 0;
		int n = nation.getNumResidents();
		for (Integer level : configNationLevel.keySet())
			if (n >= level)
				return level;
		return 0;
	}

	public static void loadConfig(String filepath, String version) throws IOException {
		if (FileMgmt.checkOrCreateFile(filepath)) {
			File file = new File(filepath);

			// read the config.yml into memory
			config = new CommentedConfiguration(file);
			if (!config.load()) {
				System.out.print("Failed to load Config!");
			}

			setDefaults(version, file);

			config.save();

			loadCachedObjects();
		}
	}
	
	public static void loadPlayerMap(String filepath) {
		if (FileMgmt.checkOrCreateFile(filepath)) {
			File file = new File(filepath);
			
			playermap = new CommentedConfiguration(file);
			if (!playermap.load()) {
				System.out.println("Failed to load playermap!");
				
				
			}
		}
	}

	public static void loadCachedObjects() throws IOException {

		// Cell War material types.
		FlagWarConfig.setFlagBaseMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_BASE_BLOCK)));
		FlagWarConfig.setFlagLightMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_FLAG_LIGHT_BLOCK)));
		FlagWarConfig.setBeaconWireFrameMaterial(Material.matchMaterial(getString(ConfigNodes.WAR_ENEMY_BEACON_WIREFRAME_BLOCK)));

		// Load Nation & Town level data into maps.
		loadTownLevelConfig();
		loadNationLevelConfig();

		// Load allowed blocks in warzone.
		WarZoneConfig.setEditableMaterialsInWarZone(getAllowedMaterials(ConfigNodes.WAR_WARZONE_EDITABLE_MATERIALS));

		ChunkNotification.loadFormatStrings();
	}

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage(String filepath, String defaultRes) throws IOException {

		String res = getString(ConfigNodes.LANGUAGE.getRoot(), defaultRes);
		String fullPath = filepath + File.separator + res;
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
	
	public static SpawnLevel getSpawnLevel(ConfigNodes node)
	{
		SpawnLevel level = SpawnLevel.valueOf(config.getString(node.getRoot()).toUpperCase());
		if(level == null) {
			level = SpawnLevel.valueOf(node.getDefault().toUpperCase());
		}
		return level;
	}
	
	public static NSpawnLevel getNSpawnLevel(ConfigNodes node)
	{
		NSpawnLevel level = NSpawnLevel.valueOf(config.getString(node.getRoot()).toUpperCase());
		if(level == null) {
			level = NSpawnLevel.valueOf(node.getDefault().toUpperCase());
		}
		return level;
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
		List<Integer> list = new ArrayList<>();
		if (strArray != null) {
			for (String aStrArray : strArray)
				if (aStrArray != null) {
					try {
						list.add(Integer.parseInt(aStrArray.trim()));
					} catch (NumberFormatException e) {
						sendError(node.getRoot().toLowerCase() + " from config.yml");
					}
				}
		}
		return list;
	}

	public static List<String> getStrArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot().toLowerCase(), node.getDefault()).split(",");
		List<String> list = new ArrayList<>();
		if (strArray != null) {
			for (String aStrArray : strArray)
				if (aStrArray != null)
					list.add(aStrArray.trim());
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
	
	@SuppressWarnings("deprecation")
	private static String convertIds(List<String> list) {
		
		int value;
		List<String> newValues = new ArrayList<>();
		
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

		addComment(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), "# default Town levels.");
		if (!config.contains(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot())) {
			// List<Map<String, Object>> townLevels =
			// config.getMapList(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot());

			// if (townLevels == null || townLevels.isEmpty() ||
			// townLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<>();
			Map<String, Object> level = new HashMap<>();
			level.put("numResidents", 0);
			level.put("namePrefix", "");
			level.put("namePostfix", " Ruins");
			level.put("mayorPrefix", "Spirit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 1);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 1);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Settlement)");
			level.put("mayorPrefix", "Hermit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 16);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 2);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Hamlet)");
			level.put("mayorPrefix", "Chief ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 32);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 6);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Village)");
			level.put("mayorPrefix", "Baron Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 96);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Town)");
			level.put("mayorPrefix", "Viscount ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 160);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 14);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large Town)");
			level.put("mayorPrefix", "Count Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 224);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "");
			level.put("namePostfix", " (City)");
			level.put("mayorPrefix", "Earl ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 320);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 24);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large City)");
			level.put("mayorPrefix", "Duke ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 384);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 28);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Metropolis)");
			level.put("mayorPrefix", "Lord ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 448);
			level.put("upkeepModifier", 1.0);
			level.put("townOutpostLimit", 4);
			level.put("townBlockBuyBonusLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), levels);
		} else {
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()));
		}

		addComment(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), "# default Nation levels.");

		if (!config.contains(ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
			// List<Map<String, Object>> nationLevels =
			// config.getMapList(ConfigNodes.LEVELS_NATION_LEVEL.getRoot());

			// if (nationLevels == null || nationLevels.isEmpty() ||
			// nationLevels.size() == 0) {
			List<Map<String, Object>> levels = new ArrayList<>();
			Map<String, Object> level = new HashMap<>();
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
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 0);
			levels.add(new HashMap<>(level));
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
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 1);
			levels.add(new HashMap<>(level));
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
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 2);
			levels.add(new HashMap<>(level));
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
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 3);
			levels.add(new HashMap<>(level));
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
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 4);
			levels.add(new HashMap<>(level));
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
			level.put("nationZonesSize", 3);
			level.put("nationBonusOutpostLimit", 5);
			levels.add(new HashMap<>(level));
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
			Town fallenTown = fallenTownBlock.getTown();
			townBlockName = "[" + fallenTown.getName() + "](" + fallenTownBlock.getCoord().toString() + ")";
		} catch (NotRegisteredException e) {
			townBlockName = "(" + fallenTownBlock.getCoord().toString() + ")";
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
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX));
		} catch (NotRegisteredException e) {
			sendError("getCapitalPostfix.");
			return "";
		}
	}

	public static String getTownPostfix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX));
		} catch (Exception e) {
			sendError("getTownPostfix.");
			return "";
		}
	}

	public static String getNationPostfix(Nation nation) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_POSTFIX));
		} catch (Exception e) {
			sendError("getNationPostfix.");
			return "";
		}
	}

	public static String getNationPrefix(Nation nation) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(nation).get(TownySettings.NationLevel.NAME_PREFIX));
		} catch (Exception e) {
			sendError("getNationPrefix.");
			return "";
		}
	}

	public static String getTownPrefix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX));
		} catch (Exception e) {
			sendError("getTownPrefix.");
			return "";
		}
	}

	public static String getCapitalPrefix(Town town) {

		try {
			return ChatColor.translateAlternateColorCodes('&',(String) getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX));
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
	
	public static boolean getSQLUsingSSL() {

		return getBoolean(ConfigNodes.PLUGIN_DATABASE_SSL);
	}

	public static int getMaxTownBlocks(Town town) {

		int ratio = getTownBlockRatio();
		int n = town.getBonusBlocks() + town.getPurchasedBlocks();

		if (ratio == 0) {
			n += (Integer) getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);

		} else
			n += town.getNumResidents() * ratio;

		n += getNationBonusBlocks(town);

		return n;
	}
	
	public static int getMaxOutposts(Town town) {
		
		int townOutposts = (Integer) getTownLevel(town).get(TownySettings.TownLevel.OUTPOST_LIMIT);
		int nationOutposts = 0;
		if (town.hasNation())
			try {
				nationOutposts = (Integer) getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
			} catch (NotRegisteredException e) {
			}
		int n = townOutposts + nationOutposts;
		return n;
	}
	
	public static int getMaxBonusBlocks(Town town) {
		
		return (Integer) getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_BUY_BONUS_LIMIT);
	}

	public static int getNationBonusBlocks(Nation nation) {
		int bonusBlocks = (Integer) getNationLevel(nation).get(TownySettings.NationLevel.TOWN_BLOCK_LIMIT_BONUS);
		NationBonusCalculationEvent calculationEvent = new NationBonusCalculationEvent(nation, bonusBlocks);
		Bukkit.getPluginManager().callEvent(calculationEvent);
		return calculationEvent.getBonusBlocks();
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

	public static boolean isUsingEconomy() {

		return getBoolean(ConfigNodes.PLUGIN_USING_ECONOMY);
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
	
	public static boolean isNotificationOwnerShowingNationTitles() {
		
		return getBoolean(ConfigNodes.NOTIFICATION_OWNER_SHOWS_NATION_TITLE);
	}
	
	public static boolean isNotificationsAppearingInActionBar() {
		return getBoolean(ConfigNodes.NOTIFICATION_NOTIFICATIONS_APPEAR_IN_ACTION_BAR);
	}

	public static boolean getShowTownBoardOnLogin() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_TOWNBOARD_ONLOGIN);
	}
	
	public static boolean getShowNationBoardOnLogin() {
		
		return getBoolean(ConfigNodes.GNATION_SETTINGS_DISPLAY_NATIONBOARD_ONLOGIN);
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

	public static boolean isDeleteTownlessOnly() {
		
		return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TOWNLESS_ONLY);
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
	
	public static boolean getNationDefaultOpen() {

		return getBoolean(ConfigNodes.GNATION_DEF_OPEN);
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
	
	public static double getTownDefaultTaxMinimumTax() {
		
		return getDouble(ConfigNodes.TOWN_DEF_TAXES_MINIMUMTAX);
	}
	
	public static boolean hasTownLimit() {

		return getTownLimit() != 0;
	}

	public static int getTownLimit() {

		return getInt(ConfigNodes.TOWN_LIMIT);
	}

	public static int getMaxPurchedBlocks(Town town) {

		if (isBonusBlocksPerTownLevel())
			return getMaxBonusBlocks(town);
		else			
			return getInt(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS);
	}
	
	public static int getMaxClaimRadiusValue() {
		
		return getInt(ConfigNodes.TOWN_MAX_CLAIM_RADIUS_VALUE);
	}

	public static boolean isSellingBonusBlocks(Town town) {

		return getMaxPurchedBlocks(town) != 0;
	}
	
	public static boolean isBonusBlocksPerTownLevel() { 
		
		return getBoolean(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS_USES_TOWN_LEVELS);
	}

	public static double getPurchasedBonusBlocksCost() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK);
	}

	public static double getPurchasedBonusBlocksIncreaseValue() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE);
	}
	
	public static boolean isTownSpawnPaidToTown() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_SPAWN_PAID_TO_TOWN);
	}

	public static double getNationNeutralityCost() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY);
	}

	public static boolean isAllowingOutposts() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTPOSTS);
	}
	
	public static boolean isOutpostsLimitedByLevels() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_LIMIT_OUTPOST_USING_LEVELS);
	}
	
	public static boolean isOutpostLimitStoppingTeleports() {
		
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OVER_OUTPOST_LIMIT_STOP_TELEPORT);
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
	
	public static double getClaimPriceIncreaseValue() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_INCREASE);
	}
	
	public static double getClaimRefundPrice() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_REFUND);
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
	
	public static boolean isNewDayDeleting0PlotTowns() {
		return getBoolean(ConfigNodes.PLUGIN_NEWDAY_DELETE_0_PLOT_TOWNS);
	}

	public static long getHourInterval() {
		return getSeconds(ConfigNodes.PLUGIN_HOUR_INTERVAL);
	}

	public static long getShortInterval() {
		return getSeconds(ConfigNodes.PLUGIN_SHORT_INTERVAL);
	}

	public static long getNewHourTime() {
		long time = getSeconds(ConfigNodes.PLUGIN_NEWHOUR_TIME);
		long hour = getHourInterval();
		if (time > hour) {
			setProperty(ConfigNodes.PLUGIN_NEWHOUR_TIME.getRoot(), hour);
			return hour;
		}
		return time;
	}

	public static SpawnLevel isAllowingTownSpawn() {

		return getSpawnLevel(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN);
	}

	public static SpawnLevel isAllowingPublicTownSpawnTravel() {

		return getSpawnLevel(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL);
	}
	
	public static NSpawnLevel isAllowingNationSpawn() {

		return getNSpawnLevel(ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN);
	}

	public static NSpawnLevel isAllowingPublicNationSpawnTravel() {

		return getNSpawnLevel(ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN_TRAVEL);
	}

	public static List<String> getDisallowedTownSpawnZones() {

		if (getDebug())
			System.out.println("[Towny] Debug: Reading disallowed town spawn zones. ");
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_PREVENT_TOWN_SPAWN_IN);
	}
	
	public static boolean getSpawnWarnConfirmations() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_SPAWN_WARNINGS);
	}

	public static boolean isTaxingDaily() {

		return getBoolean(ConfigNodes.ECO_DAILY_TAXES_ENABLED);
	}

	public static double getMaxPlotTax() {
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_PLOT_TAX);
	}

	public static double getMaxTownTax() {
		return getDouble(ConfigNodes.ECO_DAILY_TOWN_TAXES_MAX);
	}

	public static double getMaxNationTax() {
		return getDouble(ConfigNodes.ECO_DAILY_NATION_TAXES_MAX);
	}
	
	public static double getMaxPlotPrice() {
		
		return getDouble(ConfigNodes.GTOWN_MAX_PLOT_PRICE_COST);
	}
	
	public static double getMaxTownTaxPercent() {
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TOWN_TAX_PERCENT);
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
	
	public static boolean getWarEventCostsTownblocks() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_COSTS_TOWNBLOCKS);
	}

	public static boolean getWarEventWinnerTakesOwnershipOfTownblocks() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNBLOCKS);
	}
	
	public static boolean getWarEventWinnerTakesOwnershipOfTown() {
		
		return getBoolean(ConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWN);
	}
	
	public static int getWarEventConquerTime() {
		
		return getInt(ConfigNodes.WAR_EVENT_CONQUER_TIME);
	}
	
	public static boolean isChargingDeath() {
		
		return (getDeathPrice()>0 || getDeathPriceTown()>0 || getDeathPriceNation()>0 );
	}
	
	public static boolean isDeathPriceType() {

		return getString(ConfigNodes.ECO_PRICE_DEATH_TYPE).equalsIgnoreCase("fixed");
	}
	
	public static double getDeathPricePercentageCap() {
		
		return getDouble(ConfigNodes.ECO_PRICE_DEATH_PERCENTAGE_CAP);
	}
	
	public static boolean isDeathPricePercentageCapped() {
		
		return (getDeathPricePercentageCap()>0);
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
	
	public static boolean isEcoClosedEconomyEnabled() {
		
		return getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED);
	}
	
	public static boolean isJailingAttackingEnemies() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_ENEMIES);
	}
	
	public static boolean isJailingAttackingOutlaws() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_OUTLAWS);
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
	
	public static double getBailAmountMayor() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_MAYOR);
	}
	
	public static double getBailAmountKing() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_KING);
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
		TownUpkeepCalculationEvent event = new TownUpkeepCalculationEvent(town,getTownUpkeepCostRaw(town));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getTownUpkeepCostRaw(Town town) {
		double multiplier = 1.0;

		if (town != null) {
			if (isUpkeepByPlot()) {
				multiplier = town.getTownBlocks().size();
			} else {
				multiplier = Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
			}
		}
		
		if (town.hasNation()) {
			double nationMultiplier = 1.0;
			try {
				nationMultiplier = Double.parseDouble(getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_TOWN_UPKEEP_MULTIPLIER).toString());
			} catch (NumberFormatException|NotRegisteredException e) {
				e.printStackTrace();
			}
			if (isUpkeepByPlot()) {
				double amount;
				if (isTownLevelModifiersAffectingPlotBasedUpkeep())
					amount = (((getTownUpkeep() * multiplier) * Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString())) * nationMultiplier);
				else
					amount = (getTownUpkeep() * multiplier) * nationMultiplier;
				if (TownySettings.getPlotBasedUpkeepMinimumAmount() > 0.0 && amount < TownySettings.getPlotBasedUpkeepMinimumAmount())
					amount = TownySettings.getPlotBasedUpkeepMinimumAmount();
				return amount;
			}
			return (getTownUpkeep() * multiplier) * nationMultiplier;
		} else {
			if (isUpkeepByPlot()) {
				double amount;
				if (isTownLevelModifiersAffectingPlotBasedUpkeep())
					amount = (getTownUpkeep() * multiplier) * Double.parseDouble(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
				else
					amount = getTownUpkeep() * multiplier;
				if (TownySettings.getPlotBasedUpkeepMinimumAmount() > 0.0 && amount < TownySettings.getPlotBasedUpkeepMinimumAmount())
					amount = TownySettings.getPlotBasedUpkeepMinimumAmount();
				return amount;
			}
			return getTownUpkeep() * multiplier;
		}
	}

	public static double getTownUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP);
	}

	public static boolean isUpkeepByPlot() {

		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED);
	}
	
	public static double getPlotBasedUpkeepMinimumAmount () {
		
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_MINIMUM_AMOUNT);
		
	}
	public static boolean isTownLevelModifiersAffectingPlotBasedUpkeep() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_TOWNLEVEL_MODIFIER);
	
	}

	public static boolean isUpkeepPayingPlots() {

		return getBoolean(ConfigNodes.ECO_UPKEEP_PLOTPAYMENTS);
	}

	public static double getTownPenaltyUpkeepCost(Town town) {
		TownUpkeepPenalityCalculationEvent event = new TownUpkeepPenalityCalculationEvent(town, getTownPenaltyUpkeepCostRaw(town));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getTownPenaltyUpkeepCostRaw(Town town) {

		if (getUpkeepPenalty() > 0) {
			
			int overClaimed = town.getTownBlocks().size() - getMaxTownBlocks(town);
			
			if (!town.isOverClaimed())
				return 0;
			if (isUpkeepPenaltyByPlot())
				return getUpkeepPenalty() * overClaimed;
			return getUpkeepPenalty();
		}
		return 0;
	}

    public static double getUpkeepPenalty() {
    	
    	return getDouble(ConfigNodes.ECO_PRICE_TOWN_OVERCLAIMED_UPKEEP_PENALTY);
    }
    public static boolean isUpkeepPenaltyByPlot() {
    	
    	return getBoolean(ConfigNodes.ECO_PRICE_TOWN_OVERCLAIMED_UPKEEP_PENALTY_PLOTBASED);
    }

	public static double getNationUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_UPKEEP);
	}

	public static double getNationUpkeepCost(Nation nation) {
		NationUpkeepCalculationEvent event = new NationUpkeepCalculationEvent(nation, getNationUpkeepCostRaw(nation));
		Bukkit.getPluginManager().callEvent(event);
		return event.getUpkeep();
	}

	private static double getNationUpkeepCostRaw(Nation nation) {

		double multiplier = 1.0;

		if (nation != null) {
			if (isNationUpkeepPerTown()) {
				if (isNationLevelModifierAffectingNationUpkeepPerTown())
					return (getNationUpkeep() * nation.getTowns().size()) * Double.parseDouble(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
				else
					return (getNationUpkeep() * nation.getTowns().size());
			} else {
				multiplier = Double.parseDouble(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
			}
		}
		return getNationUpkeep() * multiplier;
	}

	private static boolean isNationLevelModifierAffectingNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN_NATIONLEVEL_MODIFIER);
	}

	private static boolean isNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN);
	}

	public static boolean getNationDefaultPublic(){

		return getBoolean(ConfigNodes.GNATION_DEF_PUBLIC);
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
	
	public static int getMaxResidentsPerTown() {
		
		return getInt(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN);
	}

	public static boolean isTownyUpdating(String currentVersion) {

		if (isTownyUpToDate(currentVersion))
			return false;
		else
			return true; // Assume
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
	
	public static int getMinDistanceForOutpostsFromPlot() {
		
		return getInt(ConfigNodes.TOWN_MIN_DISTANCE_FOR_OUTPOST_FROM_PLOT);
	}

	public static int getMinDistanceFromTownPlotblocks() {

		return getInt(ConfigNodes.TOWN_MIN_PLOT_DISTANCE_FROM_TOWN_PLOT);
	}

	public static int getMaxDistanceBetweenHomeblocks() {

		return getInt(ConfigNodes.TOWN_MAX_DISTANCE_BETWEEN_HOMEBLOCKS);
	}

	public static int getMaxResidentPlots(Resident resident) {

		int maxPlots = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_PLOTS.getNode());
		if (maxPlots == -1)
			maxPlots = getInt(ConfigNodes.TOWN_MAX_PLOTS_PER_RESIDENT);
		return maxPlots;
	}
	
	public static int getMaxResidentExtraPlots(Resident resident) {

		int extraPlots = TownyUniverse.getInstance().getPermissionSource().getPlayerPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_EXTRA_PLOTS.getNode());
		if (extraPlots == -1)
			extraPlots = 0;
		return extraPlots;
	}

	public static int getMaxResidentOutposts(Resident resident) {

		int maxOutposts = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_OUTPOSTS.getNode());
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

	public static boolean getPermFlag_Resident_Town_Build() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_BUILD);
	}

	public static boolean getPermFlag_Resident_Town_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_DESTROY);
	}

	public static boolean getPermFlag_Resident_Town_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_ITEM_USE);
	}

	public static boolean getPermFlag_Resident_Town_Switch() {

		return getBoolean(ConfigNodes.FLAGS_RES_TOWN_SWITCH);
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

	public static boolean getPermFlag_Town_Nation_Build() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_BUILD);
	}

	public static boolean getPermFlag_Town_Nation_Destroy() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_DESTROY);
	}

	public static boolean getPermFlag_Town_Nation_ItemUse() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_ITEM_USE);
	}

	public static boolean getPermFlag_Town_Nation_Switch() {

		return getBoolean(ConfigNodes.FLAGS_TOWN_NATION_SWITCH);
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

	public static boolean getDefaultNationPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			switch (type) {
			case BUILD:
				return getPermFlag_Resident_Town_Build();
			case DESTROY:
				return getPermFlag_Resident_Town_Destroy();
			case SWITCH:
				return getPermFlag_Resident_Town_Switch();
			case ITEM_USE:
				return getPermFlag_Resident_Town_ItemUse();
			default:
				throw new UnsupportedOperationException();
			}
		else if (owner instanceof Town)
			switch (type) {
			case BUILD:
				return getPermFlag_Town_Nation_Build();
			case DESTROY:
				return getPermFlag_Town_Nation_Destroy();
			case SWITCH:
				return getPermFlag_Town_Nation_Switch();
			case ITEM_USE:
				return getPermFlag_Town_Nation_ItemUse();
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
		case NATION:
			return getDefaultNationPermission(owner, type);
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

	//public static boolean isUsingQuestioner() {
	//
	//	return getBoolean(ConfigNodes.PLUGIN_USING_QUESTIONER_ENABLE);
	//}

	public static String getAcceptCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_ACCEPT_COMMAND);
	}

	public static String getDenyCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_DENY_COMMAND);
	}

	public static String getConfirmCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_CONFIRM_COMMAND);
	}

	public static String getCancelCommand(){
		return getString(ConfigNodes.INVITE_SYSTEM_CANCEL_COMMAND);
	}
	//public static void setUsingQuestioner(boolean newSetting) {
	//
	//	setProperty(ConfigNodes.PLUGIN_USING_QUESTIONER_ENABLE.getRoot(), newSetting);
	//}

	public static boolean getOutsidersPreventPVPToggle() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OUTSIDERS_PREVENT_PVP_TOGGLE);
	}
	
	public static boolean isForcePvpNotAffectingHomeblocks() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_HOMEBLOCKS_PREVENT_FORCEPVP);
	}

	//public static String questionerAccept() {
	//
	//	return getString(ConfigNodes.PLUGIN_QUESTIONER_ACCEPT);
	//}

	//public static String questionerDeny() {
	//
	//	return getString(ConfigNodes.PLUGIN_QUESTIONER_DENY);
	//}
	
	public static long getTownInviteCooldown() {

		return getSeconds(ConfigNodes.INVITE_SYSTEM_COOLDOWN_TIME);
	}

	public static boolean isAppendingToLog() {

		return !getBoolean(ConfigNodes.PLUGIN_RESET_LOG_ON_BOOT);
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

	public static int getTeleportWarmupTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_TIMER);
	}
	
	public static boolean isMovementCancellingSpawnWarmup() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_MOVEMENT_CANCELS_SPAWN_WARMUP);
	}
	
	public static boolean isDamageCancellingSpawnWarmup() {
		
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DAMAGE_CANCELS_SPAWN_WARMUP);
	}
	
	public static int getSpawnCooldownTime() {
		
		return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_COOLDOWN_TIMER);
	}
	
	public static int getPVPCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_PVP_COOLDOWN_TIMER);
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

		return getBoolean(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWALS);
	}

	public static void SetTownBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_TOWN_ALLOW_WITHDRAWALS.getRoot(), newSetting);
	}

	public static boolean geNationBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS);
	}
	
	public static boolean isBankActionDisallowedOutsideTown() {
		
		return getBoolean(ConfigNodes.ECO_BANK_DISALLOW_BANK_ACTIONS_OUTSIDE_TOWN);
	}
	
	public static boolean isBankActionLimitedToBankPlots() {
		
		return getBoolean(ConfigNodes.BANK_IS_LIMTED_TO_BANK_PLOTS);
	}

	public static void SetNationBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS.getRoot(), newSetting);
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
	
	public static int getMaxNumResidentsWithoutNation() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION);
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
	
	public static double getNationRequiresProximity() {
		return getDouble(ConfigNodes.GTOWN_SETTINGS_NATION_REQUIRES_PROXIMITY);
	}
	
	public static List<String> getFarmPlotBlocks() {
		return getStrArr(ConfigNodes.GTOWN_FARM_PLOT_ALLOW_BLOCKS);
	}
	
	public static List<String> getFarmAnimals() {
		return getStrArr(ConfigNodes.GTOWN_FARM_ANIMALS);
	}

	public static boolean getKeepInventoryInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_TOWN);
	}

	public static boolean getKeepExperienceInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_EXPERIENCE_ON_DEATH_IN_TOWN);
	}
	
	public static String getListPageMsg(int page, int total) {
		 
	    return parseString(String.format(getLangString("LIST_PAGE"), page, total))[0];
	}
	
	public static String getListNotEnoughPagesMsg(int max) {
	 
	    return parseString(String.format(getLangString("LIST_ERR_NOT_ENOUGH_PAGES"), max))[0];
	}
	
	public static String[] getWarAPlayerHasNoTownMsg() {
		return parseString(String.format(getLangString("msg_war_a_player_has_no_town")));
	}
	
	public static String[] getWarAPlayerHasNoNationMsg() {
		return parseString(String.format(getLangString("msg_war_a_player_has_no_nation")));
	}
	
	public static String[] getWarAPlayerHasANeutralNationMsg() {
		return parseString(String.format(getLangString("msg_war_a_player_has_a_neutral_nation")));
	}
	
	public static String[] getWarAPlayerHasBeenRemovedFromWarMsg() {
		return parseString(String.format(getLangString("msg_war_a_player_has_been_removed_from_war")));
	}
	
	public static String[] getWarPlayerCannotBeJailedPlotFallenMsg() {
		return parseString(String.format(getLangString("msg_war_player_cant_be_jailed_plot_fallen")));
	}
	
	public static String[] getWarAPlayerIsAnAllyMsg() {
		return parseString(String.format(getLangString("msg_war_a_player_is_an_ally")));
	}
	
	public static boolean isNotificationUsingTitles() {
		return getBoolean(ConfigNodes.NOTIFICATION_USING_TITLES);
	}

	public static int getAmountOfResidentsForOutpost() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_MINIMUM_AMOUNT_RESIDENTS_FOR_OUTPOSTS);
	}

	public static int getMaximumInvitesSentTown() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_SENT_TOWN);
	}
	public static int getMaximumInvitesSentNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_SENT_NATION);
	}
	public static int getMaximumRequestsSentNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_REQUESTS_SENT_NATION);
	}

	public static int getMaximumInvitesReceivedResident() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_RECEIVED_PLAYER);
	}
	public static int getMaximumInvitesReceivedTown() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_INVITES_RECEIVED_TOWN);
	}
	public static int getMaximumRequestsReceivedNation() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAXIMUM_REQUESTS_RECEIVED_NATION);
	}
	
	public static boolean getNationZonesEnabled() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_ENABLE);
	}
	
	public static boolean getNationZonesCapitalsOnly() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_ONLY_CAPITALS);
	}
	
	public static boolean getNationZonesWarDisables() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_WAR_DISABLES);
	}
	
	public static boolean getNationZonesShowNotifications() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_SHOW_NOTIFICATIONS);
	}
	
	public static int getNationZonesCapitalBonusSize() {
		return getInt(ConfigNodes.GNATION_SETTINGS_NATIONZONE_CAPITAL_BONUS_SIZE);
	}

	public static boolean isShowingRegistrationMessage() {
		return getBoolean(ConfigNodes.RES_SETTING_IS_SHOWING_WELCOME_MESSAGE);
	}
	
	public static int getMaxTownsPerNation() {
		return getInt(ConfigNodes.GNATION_SETTINGS_MAX_TOWNS_PER_NATION);
	}
	
	public static double getSpawnTravelCost() {
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC);
	}
	
	public static boolean isAllySpawningRequiringPublicStatus() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_IS_ALLY_SPAWNING_REQUIRING_PUBLIC_STATUS);
	}
	
	public static String getNotificationTitlesTownTitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_TOWN_TITLE);
	}
	
	public static String getNotificationTitlesTownSubtitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_TOWN_SUBTITLE);
	}
	
	public static String getNotificationTitlesWildTitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_WILDERNESS_TITLE);
	}
	
	public static String getNotificationTitlesWildSubtitle() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_WILDERNESS_SUBTITLE);
	}

	public static double getTownRenameCost() {
		return getDouble(ConfigNodes.ECO_TOWN_RENAME_COST);
	}

	public static double getNationRenameCost() {
		return getDouble(ConfigNodes.ECO_NATION_RENAME_COST);
	}

	public static boolean isRemovingKillerBunny() {		
		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_TOWN_KILLER_BUNNY);
	}
	
	public static boolean isSkippingRemovalOfNamedMobs() {
		return getBoolean(ConfigNodes.PROT_MOB_REMOVE_SKIP_NAMED_MOBS);
	}
	public static List<String> getJailBlacklistedCommands() {
		return getStrArr(ConfigNodes.JAIL_BLACKLISTED_COMMANDS);
	}
	
	public static String getPAPIFormattingBoth() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_BOTH);
	}

	public static String getPAPIFormattingTown() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_TOWN);
	}

	public static String getPAPIFormattingNation() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_NATION);
	}
	
	public static String getPAPIFormattingNomad() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_NOMAD);
	}

	public static String getPAPIFormattingResident() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_RESIDENT);
	}

	public static String getPAPIFormattingMayor() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_MAYOR);
	}

	public static String getPAPIFormattingKing() {
		return getString(ConfigNodes.FILTERS_PAPI_CHAT_FORMATTING_RANKS_KING);
	}

	public static double getPlotSetCommercialCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_COMMERCIAL);
	}
	
	public static double getPlotSetArenaCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_ARENA);
	}
	
	public static double getPlotSetEmbassyCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_EMBASSY);
	}
	
	public static double getPlotSetWildsCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_WILDS);
	}
	
	public static double getPlotSetInnCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_INN);
	}
	
	public static double getPlotSetJailCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_JAIL);
	}
	
	public static double getPlotSetFarmCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_FARM);
	}
	
	public static double getPlotSetBankCost() {
		return getDouble(ConfigNodes.ECO_PLOT_TYPE_COSTS_BANK);
	}
	
	public static int getMaxDistanceFromTownSpawnForInvite() {
		return getInt(ConfigNodes.INVITE_SYSTEM_MAX_DISTANCE_FROM_TOWN_SPAWN);
	}
	
	public static boolean getTownDisplaysXYZ() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_XYZ_INSTEAD_OF_TOWNY_COORDS);
	}
	
	public static boolean isTownListRandom() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_TOWN_LIST_RANDOMLY);
	}

	public static boolean isWarAllowed() {
		return getBoolean(ConfigNodes.NWS_WAR_ALLOWED);
	}
	
	public static int timeToWaitAfterFlag() {
		return getInt(ConfigNodes.WAR_ENEMY_TIME_TO_WAIT_AFTER_FLAGGED);
	}
	
	public static boolean isFlaggedInteractionTown() {
		return getBoolean(ConfigNodes.WAR_ENEMY_PREVENT_INTERACTION_WHILE_FLAGGED);
	}
	
	public static boolean isFlaggedInteractionNation() {
		return getBoolean(ConfigNodes.WAR_ENEMY_PREVENT_NATION_INTERACTION_WHILE_FLAGGED);
	}

	public static boolean isNotificationsTownNamesVerbose() {
		return getBoolean(ConfigNodes.NOTIFICATION_TOWN_NAMES_ARE_VERBOSE);
	}

	public static Map<String,String> getNationColorsMap() {
		List<String> nationColorsList = getStrArr(ConfigNodes.GNATION_SETTINGS_ALLOWED_NATION_COLORS);
		Map<String,String> nationColorsMap = new HashMap<>();
		String[] keyValuePair;
		for(String nationColor: nationColorsList) {
			keyValuePair = nationColor.trim().split(":");
			nationColorsMap.put(keyValuePair[0], keyValuePair[1]);
		}
		return nationColorsMap;
	}

	public static boolean getWarSiegeEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_ENABLED);
	}

	public static boolean getWarSiegeAttackEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_ATTACK_ENABLED);
	}

	public static boolean getWarSiegeAbandonEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_ABANDON_ENABLED);
	}

	public static boolean getWarSiegeSurrenderEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_TOWN_SURRENDER_ENABLED);
	}

	public static boolean getWarSiegeInvadeEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_INVADE_ENABLED);
	}

	public static boolean getWarSiegePlunderEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_PLUNDER_ENABLED);
	}

	public static boolean getWarSiegeRevoltEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_REVOLT_ENABLED);
	}

	public static boolean getWarSiegeTownLeaveDisabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_TOWN_LEAVE_DISABLED);
	}

	public static boolean getWarSiegePvpAlwaysOnInBesiegedTowns() {
		return getBoolean(ConfigNodes.WAR_SIEGE_PVP_ALWAYS_ON_IN_BESIEGED_TOWNS);
	}

	public static boolean getWarSiegeExplosionsAlwaysOnInBesiegedTowns() {
		return getBoolean(ConfigNodes.WAR_SIEGE_EXPLOSIONS_ALWAYS_ON_IN_BESIEGED_TOWNS);
	}

	public static boolean getWarSiegeClaimingDisabledNearSiegeZones() {
		return getBoolean(ConfigNodes.WAR_SIEGE_CLAIMING_DISABLED_NEAR_SIEGE_ZONES);
	}

	public static int getWarSiegeClaimDisableDistanceBlocks() {
		return getInt(ConfigNodes.WAR_SIEGE_CLAIM_DISABLE_DISTANCE_BLOCKS);
	}

	public static int getWarSiegeMaxAllowedBannerToTownDownwardElevationDifference() {
		return getInt(ConfigNodes.WAR_SIEGE_MAX_ALLOWED_BANNER_TO_TOWN_DOWNWARD_ELEVATION_DIFFERENCE);
	}

	public static double getWarSiegeAttackerCostUpFrontPerPlot() {
		return getDouble(ConfigNodes.WAR_SIEGE_ATTACKER_COST_UPFRONT_PER_PLOT);
	}

	public static long getWarSiegeTimerIntervalSeconds() {
		return getInt(ConfigNodes.WAR_SIEGE_TIMER_TICK_INTERVAL_SECONDS);
	}

	public static double getWarSiegeSiegeImmunityTimeNewTownsHours() {
		return getDouble(ConfigNodes.WAR_SIEGE_SIEGE_IMMUNITY_TIME_NEW_TOWN_HOURS);
	}
	public static double getWarSiegeSiegeImmunityTimeModifier() {
		return getDouble(ConfigNodes.WAR_SIEGE_SIEGE_IMMUNITY_TIME_MODIFIER);
	}

	public static double getWarSiegeRevoltImmunityTimeHours() {
		return getDouble(ConfigNodes.WAR_SIEGE_REVOLT_IMMUNITY_TIME_HOURS);
	}

	public static double getWarSiegeAttackerPlunderAmountPerPlot() {
		return getDouble(ConfigNodes.WAR_SIEGE_ATTACKER_PLUNDER_AMOUNT_PER_PLOT);
	}

	public static double getWarSiegeMaxHoldoutTimeHours() {
		return getDouble(ConfigNodes.WAR_SIEGE_MAX_HOLDOUT_TIME_HOURS);
	}
	
	public static double getWarSiegeMinSiegeDurationBeforeSurrenderHours() {
		return getDouble(ConfigNodes.WAR_SIEGE_MIN_SIEGE_DURATION_BEFORE_SURRENDER_HOURS);
	}

	public static double getWarSiegeMinSiegeDurationBeforeAbandonHours() {
		return getDouble(ConfigNodes.WAR_SIEGE_MIN_SIEGE_DURATION_BEFORE_ABANDON_HOURS);
	}

	public static int getWarSiegePointsForAttackerOccupation() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_ATTACKER_OCCUPATION);
	}

	public static int getWarSiegePointsForDefenderOccupation() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_DEFENDER_OCCUPATION);
	}

	public static int getWarSiegePointsForAttackerDeath() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_ATTACKER_DEATH);
	}

	public static int getWarSiegePointsForDefenderDeath() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_DEFENDER_DEATH);
	}
	
	public static int getWarSiegeZoneDeathRadiusBlocks() {
		return getInt(ConfigNodes.WAR_SIEGE_ZONE_DEATH_RADIUS_BLOCKS);
	}

	public static double getWarSiegeZoneMaximumScoringDurationMinutes() {
		return getDouble(ConfigNodes.WAR_SIEGE_ZONE_MAXIMUM_SCORING_DURATION_MINUTES);
	}

	public static int getWarSiegeMaxPlayersPerSideForTimedPoints() {
		return getInt(ConfigNodes.WAR_SIEGE_MAX_PLAYERS_PER_SIDE_FOR_TIMED_POINTS);
	}

	public static boolean getWarSiegeAttackerSpawnIntoBesiegedTownDisabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_ATTACKER_SPAWN_INTO_BESIEGED_TOWN_DISABLED);
	}
	
	public static double getWarSiegeNationCostRefundPercentageOnDelete() {
		return getDouble(ConfigNodes.WAR_SIEGE_NATION_COST_REFUND_PERCENTAGE_ON_DELETE);
	}

	public static int getWarSiegeMaxActiveSiegeAttacksPerNation() {
		return getInt(ConfigNodes.WAR_SIEGE_MAX_ACTIVE_SIEGE_ATTACKS_PER_NATION);
	}

	public static boolean getWarSiegeRefundInitialNationCostOnDelete() {
		return getBoolean(ConfigNodes.WAR_SIEGE_REFUND_INITIAL_NATION_COST_ON_DELETE);
	}

	public static boolean getWarCommonPeacefulTownsEnabled() {
		return getBoolean(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_ENABLED);
	}

	public static int getWarCommonPeacefulTownsConfirmationRequirementDays() {
		return getInt(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_CONFIRMATION_REQUIREMENT_DAYS);
	}

	public static int getWarCommonPeacefulTownsResidentPostLeavePeacefulnessDurationHours() {
		return getInt(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_RESIDENT_POST_LEAVE_PEACEFULNESS_DURATION_HOURS);
	}

	public static boolean getWarSiegePillagingEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_PILLAGING_ENABLED);
	}

	public static double getWarSiegePillageAmountPerPlot() {
		return getDouble(ConfigNodes.WAR_SIEGE_PILLAGE_AMOUNT_PER_PLOT);
	}

	public static boolean getWarCommonPostRespawnPeacefulnessEnabled() {
		return getBoolean(ConfigNodes.WAR_COMMON_POST_RESPAWN_PEACEFULNESS_ENABLED);
	}

	public static int getWarCommonPostRespawnPeacefulnessDurationSeconds() {
		return getInt(ConfigNodes.WAR_COMMON_POST_RESPAWN_PEACEFULNESS_DURATION_SECONDS);
	}

	public static double getWarSiegeMaximumPillageAmountPerPlot() {
		return getDouble(ConfigNodes.WAR_SIEGE_MAXIMUM_PILLAGE_AMOUNT_PER_PLOT);
	}

	public static boolean getWarSiegeBesiegedTownRecruitmentDisabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_BESIEGED_TOWN_RECRUITMENT_DISABLED);
	}

	public static boolean getWarSiegeBesiegedTownClaimingDisabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_BESIEGED_TOWN_CLAIMING_DISABLED);
	}

	public static boolean getWarSiegePenaltyPointsEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_PENALTY_POINTS_ENABLED);
	}

	public static boolean getWarSiegeKeepInventoryOnSiegeDeath() {
		return getBoolean(ConfigNodes.WAR_SIEGE_KEEP_INVENTORY_ON_SIEGE_DEATH);
	}

	public static int getWarSiegeExtraMoneyPercentagePerTownLevel() {
		return getInt(ConfigNodes.WAR_SIEGE_EXTRA_MONEY_PERCENTAGE_PER_TOWN_LEVEL);
	}

	public static double getWarSiegePointsPercentageAdjustmentForLeaderProximity() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_PERCENTAGE_ADJUSTMENT_FOR_LEADER_PROXIMITY);
	}

	public static double getWarSiegePointsPercentageAdjustmentForLeaderDeath() {
		return getInt(ConfigNodes.WAR_SIEGE_POINTS_PERCENTAGE_ADJUSTMENT_FOR_LEADER_DEATH);
	}

	public static int getWarSiegeLeadershipAuraRadiusBlocks() {
		return getInt(ConfigNodes.WAR_SIEGE_LEADERSHIP_AURA_RADIUS_BLOCKS);
	}

	public static boolean getWarSiegeTacticalVisibilityEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_TACTICAL_VISIBILITY_ENABLED);
	}

	public static List<HeldItemsCombination> getWarSiegeTacticalVisibilityItems() {
		try {
			if (tacticalVisibilityItems.isEmpty()) {
				String itemsListAsString = getString(ConfigNodes.WAR_SIEGE_TACTICAL_VISIBILITY_ITEMS);
				String[] itemsListAsArray = itemsListAsString.split(",");
				String[] itemPair;
				boolean ignoreOffHand;
				boolean ignoreMainHand;
				Material offHandItem;
				Material mainHandItem;

				for (String itemAsString : itemsListAsArray) {
					itemPair = itemAsString.trim().split("\\|");

					if(itemPair[0].equalsIgnoreCase("any")) {
						ignoreOffHand = true;
						offHandItem = null;
					} else if (itemPair[0].equalsIgnoreCase("empty")){
						ignoreOffHand = false;
						offHandItem = Material.AIR;
					} else{
						ignoreOffHand = false;
						offHandItem = Material.matchMaterial(itemPair[0]);
					}

					if(itemPair[1].equalsIgnoreCase("any")) {
						ignoreMainHand = true;
						mainHandItem = null;
					} else if (itemPair[1].equalsIgnoreCase("empty")){
						ignoreMainHand = false;
						mainHandItem = Material.AIR;
					} else{
						ignoreMainHand = false;
						mainHandItem = Material.matchMaterial(itemPair[1]);
					}

					tacticalVisibilityItems.add(
						new HeldItemsCombination(offHandItem,mainHandItem,ignoreOffHand,ignoreMainHand));
				}
			}
		} catch (Exception e) {
			System.out.println("Problem reading tactical visibility items list. The list is config.yml may be misconfigured.");
			e.printStackTrace();
		}
		return tacticalVisibilityItems;
	}

	public static int getWarSiegeBannerControlSessionDurationMinutes() {
		return getInt(ConfigNodes.WAR_SIEGE_BANNER_CONTROL_SESSION_DURATION_MINUTES);
	}

	public static boolean getWarCommonTownRuinsEnabled() {
		return getBoolean(ConfigNodes.WAR_COMMON_TOWN_RUINS_ENABLED);
	}

	public static int getWarCommonTownRuinsMaxDurationHours() {
		return getInt(ConfigNodes.WAR_COMMON_TOWN_RUINS_MAX_DURATION_HOURS);
	}

	public static int getWarCommonTownRuinsMinDurationHours() {
		return getInt(ConfigNodes.WAR_COMMON_TOWN_RUINS_MIN_DURATION_HOURS);
	}

	public static boolean getWarCommonTownRuinsReclaimEnabled() {
		return getBoolean(ConfigNodes.WAR_COMMON_TOWN_RUINS_RECLAIM_ENABLED);
	}

	public static double getEcoPriceReclaimTown() {
		return getDouble(ConfigNodes.ECO_PRICE_RECLAIM_RUINED_TOWN);
	}

	public static Integer getWarSiegeMaxTimedPointsPerPlayerPerSiege() {
		return getInt(ConfigNodes.WAR_SIEGE_SCORING_MAX_TIMED_POINTS_PER_PLAYER_PER_SIEGE);
	}

	public static boolean getWarSiegePopulationBasedPointBoostsEnabled() {
		return getBoolean(ConfigNodes.WAR_SIEGE_POPULATION_BASED_POINT_BOOSTS_ENABLED);
	}

	public static double getWarSiegePopulationQuotientForMaxPointsBoost() {
		return getDouble(ConfigNodes.WAR_SIEGE_POPULATION_QUOTIENT_FOR_MAX_POINTS_BOOST);
	}

	public static double getWarSiegeMaxPopulationBasedPointBoost() {
		return getDouble(ConfigNodes.WAR_SIEGE_MAX_POPULATION_BASED_POINTS_BOOST);
	}
}