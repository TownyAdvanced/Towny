package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.db.DatabaseConfig;
import com.palmergames.bukkit.towny.event.NationBonusCalculationEvent;
import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent;
import com.palmergames.bukkit.towny.event.TownUpkeepPenalityCalculationEvent;
import com.palmergames.bukkit.towny.event.town.TownCalculateMaxTownBlocksEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.spawnlevel.SpawnLevel;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TownySettings {

	// Town Level
	public record TownLevel(
			String namePrefix,
			String namePostfix,
			String mayorPrefix,
			String mayorPostfix,
			int townBlockLimit,
			double upkeepModifier,
			double peacefulCostMultiplier,
			int townOutpostLimit,
			int townBlockBuyBonusLimit,
			double debtCapModifier,
			double resourceProductionModifier,
			double bankCapModifier, 
			Map<String, Integer> townBlockTypeLimits) {}

	// Nation Level
	public record NationLevel(
			String namePrefix,
			String namePostfix,
			String capitalPrefix,
			String capitalPostfix,
			String kingPrefix,
			String kingPostfix,
			int townBlockLimitBonus,
			double upkeepModifier,
			double nationTownUpkeepModifier,
			double peacefulCostMultiplier,
			double bankCapModifier,
			int nationZonesSize,
			int nationBonusOutpostLimit,
			int nationCapitalBonusOutpostLimit) {}

	private static CommentedConfiguration config;
	private static CommentedConfiguration newConfig;
	private static boolean areLevelTypeLimitsConfigured;

	private static final SortedMap<Integer, TownLevel> configTownLevel = Collections.synchronizedSortedMap(new TreeMap<>(Collections.reverseOrder()));
	private static final SortedMap<Integer, NationLevel> configNationLevel = Collections.synchronizedSortedMap(new TreeMap<>(Collections.reverseOrder()));
	
	private static final Set<Material> itemUseMaterials = new LinkedHashSet<>();
	private static final Set<Material> switchUseMaterials = new LinkedHashSet<>();
	private static final List<Class<?>> protectedMobs = new ArrayList<>();
	
	private static final Map<NamespacedKey, Consumer<CommentedConfiguration>> CONFIG_RELOAD_LISTENERS = new HashMap<>();
	
	public static void newTownLevel(
			int numResidents,
			String namePrefix,
			String namePostfix,
			String mayorPrefix,
			String mayorPostfix,
			int townBlockLimit,
			double townUpkeepMultiplier,
			double peacefulCostMultiplier,
			int townOutpostLimit,
			int townBlockBuyBonusLimit,
			double debtCapModifier,
			double resourceProductionModifier,
			double bankCapModifier,
			Map<String, Integer> townBlockTypeLimits) {

		configTownLevel.put(numResidents, new TownLevel(
			namePrefix,
			namePostfix,
			mayorPrefix,
			mayorPostfix,
			townBlockLimit,
			townUpkeepMultiplier,
			peacefulCostMultiplier,
			townOutpostLimit,
			townBlockBuyBonusLimit,
			debtCapModifier,
			resourceProductionModifier,
			bankCapModifier,
			townBlockTypeLimits.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(Locale.ROOT), Map.Entry::getValue))
		));
	}

	public static void newNationLevel(
			int numResidents,
			String namePrefix,
			String namePostfix,
			String capitalPrefix,
			String capitalPostfix,
			String kingPrefix,
			String kingPostfix,
			int townBlockLimitBonus,
			double nationUpkeepMultiplier,
			double nationTownUpkeepMultiplier,
			double peacefulCostMultiplier,
			double bankCapModifier,
			int nationZonesSize,
			int nationBonusOutpostLimit,
			int nationCapitalBonusOutpostLimit) {

		configNationLevel.put(numResidents, new NationLevel(
			namePrefix,
			namePostfix,
			capitalPrefix,
			capitalPostfix,
			kingPrefix,
			kingPostfix,
			townBlockLimitBonus,
			nationUpkeepMultiplier,
			nationTownUpkeepMultiplier,
			peacefulCostMultiplier,
			bankCapModifier,
			nationZonesSize,
			nationBonusOutpostLimit,
			nationCapitalBonusOutpostLimit
		));
	}

	/**
	 * Loads town levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * <p>
	 * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:
	 * townBlockLimit
	 * <p>
	 * townBlockLimit is a required field even if using a calculated ratio.
	 *
	 * @throws TownyException if unable to load the Town Levels
	 */
	@SuppressWarnings("unchecked")
	public static void loadTownLevelConfig() throws TownyException {

		// Some configs end up having their numResident: 0 level removed which causes big errors.
		// Add a 0 level town_level here which may get replaced when the config's town_levels are loaded below.
		newTownLevel(0, "", " Ruins", "Spirit", "", 1, 1.0, 1.0, 0, 0, 1.0, 1.0, 1.0, new HashMap<>());
		
		List<Map<?, ?>> levels = config.getMapList("levels.town_level");
		for (int i = 0; i < levels.size(); i++) {
			Map<String, Object> level = (Map<String, Object>) levels.get(i);

			Map<String, Integer> townBlockTypeLimits;
			if (level.get("townBlockTypeLimits") instanceof List<?> list)
				townBlockTypeLimits = ((List<Object>) list).stream().map(Object::toString).map(s -> s.replaceAll("[{}]", "")).collect(Collectors.toMap(e -> e.split("=")[0], e -> Integer.parseInt(e.split("=")[1])));
			else 
				townBlockTypeLimits = new HashMap<>();

			if (!townBlockTypeLimits.isEmpty())
				areLevelTypeLimitsConfigured = true;

			// Num residents or index used for error messages
			final String numResidentsIndex = level.containsKey("numResidents") ? "numResidents = " + level.get("numResidents") : "index " + i;
			final String description = "town levels";
			try {
				newTownLevel(
					levelGetAndParse(level, description, numResidentsIndex, "numResidents", null, Integer::parseInt),
					levelGet(level, description, numResidentsIndex, "namePrefix", ""),
					levelGet(level, description, numResidentsIndex, "namePostfix", ""),
					levelGet(level, description, numResidentsIndex, "mayorPrefix", ""),
					levelGet(level, description, numResidentsIndex, "mayorPostfix", ""),
					levelGetAndParse(level, description, numResidentsIndex, "townBlockLimit", 0, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "upkeepModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "peacefulCostMultiplier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "townOutpostLimit", 0, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "townBlockBuyBonusLimit", 0, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "debtCapModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "resourceProductionModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "bankCapModifier", 1.0, Double::parseDouble),
					townBlockTypeLimits
				);
			} catch (Exception e) {
				Towny.getPlugin().getLogger().warning("An exception occurred when loading a town level at " + numResidentsIndex + ", this can be caused by having an outdated town_level section.");
				Towny.getPlugin().getLogger().warning("This can be fixed automatically by deleting the town_level section and letting Towny remake it on the next startup.");

				if (e instanceof TownyException)
					throw e;
				else
					throw new TownyException("An error occurred when loading a town level at " + numResidentsIndex, e);
			}

		}
	}

	/**
	 * Loads nation levels. Level format ignores lines starting with #.
	 * Each line is considered a level. Each level is loaded as such:
	 * <p>
	 * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:
	 * kingPrefix:kingPostfix
	 *
	 * @throws TownyException if Nation Levels cannot be loaded from config
	 */
	public static void loadNationLevelConfig() throws TownyException {
		
		// Some configs end up having their numResident: 0 level removed which causes big errors.
		// Add a 0 level nation_level here which may get replaced when the config's nation_levels are loaded below.
		newNationLevel(0, "Land of ", " (Nation)", "", "", "Leader ", "", 10, 1.0, 1.0, 1.0, 1.0, 1, 0, 0);

		List<Map<?, ?>> levels = config.getMapList("levels.nation_level");
		for (int i = 0; i < levels.size(); i++) {
			Map<?, ?> level = levels.get(i);
			
			// Num residents or index used for error messages
			final String numResidentsIndex = level.containsKey("numResidents") ? "numResidents = " + level.get("numResidents") : "index " + i;
			final String description = "nation levels";
			try {
				newNationLevel(
					levelGetAndParse(level, description, numResidentsIndex, "numResidents", null, Integer::parseInt), // Intentionally null to error out if left out
					levelGet(level, description, numResidentsIndex, "namePrefix", ""),
					levelGet(level, description, numResidentsIndex, "namePostfix", ""),
					levelGet(level, description, numResidentsIndex, "capitalPrefix", ""),
					levelGet(level, description, numResidentsIndex, "capitalPostfix", ""),
					levelGet(level, description, numResidentsIndex, "kingPrefix", ""),
					levelGet(level, description, numResidentsIndex, "kingPostfix", ""),
					levelGetAndParse(level, description, numResidentsIndex, "townBlockLimitBonus", 1, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "upkeepModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "nationTownUpkeepModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "peacefulCostMultiplier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "bankCapModifier", 1.0, Double::parseDouble),
					levelGetAndParse(level, description, numResidentsIndex, "nationZonesSize", 1, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "nationBonusOutpostLimit", 0, Integer::parseInt),
					levelGetAndParse(level, description, numResidentsIndex, "nationCapitalBonusOutpostLimit", 0, Integer::parseInt)
				);
			} catch (Exception e) {
				Towny.getPlugin().getLogger().warning("An exception occurred when a loading nation level with " + numResidentsIndex + ", this can be caused by having an outdated nation_level section.");
				Towny.getPlugin().getLogger().warning("This can be fixed automatically by deleting the nation_level section and letting Towny remake it on the next startup.");

				if (e instanceof TownyException)
					throw e;
				else
					throw new TownyException("An error occurred when loading a nation level at " + numResidentsIndex, e);
			}

		}
	}

	/**
	 * Used for getting values from the various levels and logging helpful warning messages if a key is missing.
	 */
	private static String levelGet(Map<?, ?> map, String mapDescribedAs, String indexString, String key, Object defaultValue) {
		Object value = map.get(key);
		if (value == null) {
			value = defaultValue;
			Towny.getPlugin().getLogger().warning("The '" + key + "' option in the " + mapDescribedAs + " at " + indexString + " does not have a value, falling back to '" + defaultValue + "'.");
		}
		
		return value.toString();
	}
	
	private static <T> T levelGetAndParse(Map<?, ?> map, String mapDescribedAs, String indexString, String key, T defaultValue, Function<String, T> parse) throws TownyException {
		String value = levelGet(map, mapDescribedAs, indexString, key, defaultValue);
		
		try {
			return parse.apply(value);
		} catch (Exception e) {
			throw new TownyException("Could not deserialize option '" + key + "' in the " + mapDescribedAs + " at " + indexString + ".", e);
		}
	}

	public static TownLevel getTownLevel(int numResidents) {
		return configTownLevel.get(numResidents);
	}

	public static TownLevel getTownLevel(Town town) {
		// In order to look up the town level we always have to reference a number of
		// residents (the key by which TownLevels are mapped,) even when dealing with
		// manually-set TownLevels.
		int numResidents = getResidentCountForTownLevel(town.getLevelNumber());
		return getTownLevel(numResidents);
	}

	public static TownLevel getTownLevelWithModifier(int modifier, Town town) {
		return getTownLevel(getTownLevelFromGivenInt(modifier, town));
	}

	/**
	 * Get the town level for a given population size.
	 * <p>
	 *     Great for debugging, or just to see what the town level is for a given amount of residents. 
	 *     But for most cases you'll want to use {@link Town#getTownLevel()}, which uses the town's current population.
	 *     <br />
	 *     Note that Town Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.
	 * </p>
	 * @param threshold Number of residents used to calculate the level.
	 * @param town the Town from which to get a TownLevel.
	 * @return The calculated Town Level. 0, if the town is ruined, or the method otherwise fails through.
	 */
	@ApiStatus.Internal
	public static int getTownLevelFromGivenInt(int threshold, Town town) {
		if (town.isRuined())
			return 0;

		for (int level : configTownLevel.keySet())
			if (threshold >= level)
				return level;
		return 0;
	}

	/**
	 * Gets the number of residents required to look up the TownLevel in the SortedMap.
	 * @param level The number used to get the key from the keySet array. 
	 * @return the number of residents which will get us the correct TownLevel in the TownLevel SortedMap.
	 */
	public static int getResidentCountForTownLevel(int level) {
		
		Integer[] keys = configTownLevel.keySet().toArray(new Integer[] {});
		// keys is always ordered from biggest to lowest (despite what the javadocs say
		// about being sorted in Ascending order, this is not the case for a SortedMap.)
		// We have to get it from lowest to largest.
		Arrays.sort(keys);
		level = Math.min(level, keys.length);
		return keys[level];
	}

	/**
	 * Gets the number of the TownLevel for towns, returning the position in the
	 * SortedMap which corresponds with the given number of residents.
	 * 
	 * @param residents The number used to get the key from the keySet array.
	 * @param town The town being checked, in case it is ruined.
	 * @return the number of the TownLevel.
	 */
	public static int getTownLevelWhichIsNotManuallySet(int residents, Town town) {
		if (town.isRuined())
			return 0;

		int i = TownySettings.getTownLevelMax() - 1; // Remove one in order to get the index of an array.
		for (int level : configTownLevel.keySet()) {
			if (residents >= level)
				return i;

			i--;
		}
		return 0;
	}
	
	public static int getTownLevelMax() {
		return configTownLevel.size();
	}

	public static NationLevel getNationLevel(int levelNumber) {
		return configNationLevel.get(levelNumber);
	}

	public static NationLevel getNationLevel(Nation nation) {
		return getNationLevel(nation.getLevelNumber());
	}

	public static NationLevel getNationLevelWithModifier(int modifier) {
		return getNationLevel(getNationLevelFromGivenInt(modifier));
	}

	/**
	 * Get the Nation's Level for a supposed population size or town amount (depending on server configuration.)
	 * <p>
	 *     Note that Nation Levels are not hard-coded. They can be defined by the server administrator,
	 *     and may be different from the default configuration.	 
	 * </p>
	 * @param threshold Number of residents or towns in the Nation, theoretical or real.
	 * @return Nation Level (int) for the supplied threshold.
	 */
	@ApiStatus.Internal
	public static int getNationLevelFromGivenInt(int threshold) {
		for (Integer level : configNationLevel.keySet())
			if (threshold >= level)
				return level;
		return 0;
	}

	public static int getNationLevelMax() {
		return configNationLevel.size();
	}

	public static boolean isNationLevelDeterminedByTownCount() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATION_LEVEL_IS_DETERMINED_BY_TOWNS_COUNT);
	}
	
	public static CommentedConfiguration getConfig() {
		return config;
	}

	public static void loadConfig(Path configPath, String version) {
		if (!FileMgmt.checkOrCreateFile(configPath.toString())) {
			throw new TownyInitException("Failed to touch '" + configPath + "'.", TownyInitException.TownyError.MAIN_CONFIG);
		}

		// read the config.yml into memory
		config = new CommentedConfiguration(configPath);
		if (!config.load()) {
			throw new TownyInitException("Failed to load Towny's config.yml.", TownyInitException.TownyError.MAIN_CONFIG);
		}

		setDefaults(version, configPath);

		config.save();

		loadSwitchAndItemUseMaterialsLists();
		loadProtectedMobsList();
		ChunkNotification.loadFormatStrings();
		TownBlockTypeHandler.Migrator.migrate();
		
		// Always run reload consumers after everything else is reloaded.
		CONFIG_RELOAD_LISTENERS.values().forEach(consumer -> consumer.accept(config));
	}
	
	@VisibleForTesting
	public static void loadDefaultConfig() {
		try {
			loadConfig(Files.createTempFile("towny-temp-config", ".yml"), "0.0.0.0");
			loadTownLevelConfig();
		} catch (IOException e) {
			throw new RuntimeException("Could not create temporary file", e);
		} catch (TownyException e) {
			throw new RuntimeException("Could not load town level config", e);
		}
	}
	
	private static void loadProtectedMobsList() {
		protectedMobs.clear();
		protectedMobs.addAll(EntityTypeUtil.parseLivingEntityClassNames(getStrArr(ConfigNodes.PROT_MOB_TYPES), "TownMobPVM:"));
	}

	private static void loadSwitchAndItemUseMaterialsLists() {

		switchUseMaterials.clear();
		itemUseMaterials.clear();
		
		/*
		 * Load switches from config value.
		 * Scan over them and replace any grouping with the contents of the group.
		 * Add single item or grouping to SwitchUseMaterials.
		 */
		List<String> switches = getStrArr(ConfigNodes.PROT_SWITCH_MAT);
		for (String matName : switches) {
			if (ItemLists.GROUPS.contains(matName)) {
				switchUseMaterials.addAll(ItemLists.getGrouping(matName));
			} else {
				Material material = BukkitTools.matchRegistry(Registry.MATERIAL, matName);
				if (material != null)
					switchUseMaterials.add(material);
			}
		}

		/*
		 * Load items from config value.
		 * Scan over them and replace any grouping with the contents of the group.
		 * Add single item or grouping to ItemUseMaterials.
		 */
		List<String> items = getStrArr(ConfigNodes.PROT_ITEM_USE_MAT);
		for (String matName : items) {
			if (ItemLists.GROUPS.contains(matName)) {
				itemUseMaterials.addAll(ItemLists.getGrouping(matName));
			} else {
				Material material = BukkitTools.matchRegistry(Registry.MATERIAL, matName);
				if (material != null)
					itemUseMaterials.add(material);
			}
		}
	}

	public static Set<EntityType> toEntityTypeSet(final List<String> entityList) {
		final Set<EntityType> entities = new HashSet<>();
		
		for (final String entityName : entityList) {
			final EntityType type = BukkitTools.matchRegistry(Registry.ENTITY_TYPE, switch (entityName.toLowerCase(Locale.ROOT)) {
				// This is needed because some of the entity type fields don't/didn't match the actual key.
				//<editor-fold desc="Lots of switch cases">
				case "primed_tnt" -> "tnt";
				case "minecart_tnt" -> "tnt_minecart";
				case "ender_crystal" -> "end_crystal";
				case "fishing_hook" -> "fishing_bobber";
				case "minecart_chest" -> "chest_minecart";
				case "minecart_hopper" -> "hopper_minecart";
				case "minecart_furnace" -> "furnace_minecart";
				case "minecart_command" -> "command_block_minecart";
				case "thrown_exp_bottle" -> "experience_bottle";
				case "ender_signal" -> "eye_of_ender";
				case "mushroom_cow" -> "mooshroom";
				case "splash_potion" -> MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.MINECRAFT_1_21_5) ? "splash_potion" : "potion";
				case "leash_hitch" -> "leash_knot";
				case "lightning" -> "lightning_bolt";
				case "dropped_item" -> "item";
				case "minecart_mob_spawner" -> "spawner_minecart";
				case "snowman" -> "snow_golem";
				case "firework" -> "firework_rocket";
				//</editor-fold>
				default -> entityName;
			});
			
			if (type != null)
				entities.add(type);
			else
				System.out.println("Unmatched entity: " + entityName);
		}

		return entities;
	}

	public static Collection<Material> toMaterialSet(List<String> materialList) {
		Set<Material> materials = new HashSet<>();
		
		for (String materialName : materialList) {
			if (materialName.isEmpty())
				continue;
			
			if (ItemLists.GROUPS.contains(materialName.toUpperCase(Locale.ROOT))) {
				materials.addAll(ItemLists.getGrouping(materialName.toUpperCase(Locale.ROOT)));
			} else {
				Material material = BukkitTools.matchRegistry(Registry.MATERIAL, materialName);
				if (material != null)
					materials.add(material);
			}
		}
		
		return materials;
	}

	public static void sendError(String msg) {
		Towny.getPlugin().getLogger().warning(() -> String.format("Error could not read %s",msg));
	}
	
	public static SpawnLevel getSpawnLevel(ConfigNodes node)
	{
		String configString = config.getString(node.getRoot());
		SpawnLevel spawnLevel;
		if (configString != null) {
			spawnLevel = SpawnLevel.valueOf(configString.toUpperCase(Locale.ROOT));
		} else {
			spawnLevel = SpawnLevel.valueOf(node.getDefault().toUpperCase(Locale.ROOT));
		}
		return spawnLevel;
	}

	public static boolean getBoolean(ConfigNodes node) {

		return Boolean.parseBoolean(config.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()));
	}

	public static double getDouble(ConfigNodes node) {

		try {
			return Double.parseDouble(config.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase(Locale.ROOT) + " from config.yml");
			return 0.0;
		}
	}

	public static int getInt(ConfigNodes node) {

		try {
			return Integer.parseInt(config.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase(Locale.ROOT) + " from config.yml");
			return 0;
		}
	}

	public static String getString(ConfigNodes node) {

		return config.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault());
	}

	public static String getString(String root, String def) {

		String data = config.getString(root.toLowerCase(Locale.ROOT), def);
		if (data == null) {
			sendError(root.toLowerCase(Locale.ROOT) + " from config.yml");
			return "";
		}
		return data;
	}

	public static List<Integer> getIntArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot(), node.getDefault()).split(",");
		List<Integer> list = new ArrayList<>();
		for (String aStrArray : strArray)
			if (aStrArray != null) {
				try {
					list.add(Integer.parseInt(aStrArray.trim()));
				} catch (NumberFormatException e) {
					sendError(node.getRoot().toLowerCase(Locale.ROOT) + " from config.yml");
				}
			}
		return list;
	}

	public static List<String> getStrArr(ConfigNodes node) {

		String[] strArray = getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()).split(",");
		List<String> list = new ArrayList<>();
		
		for (String string : strArray)
			if (string != null && !string.isEmpty())
				list.add(string.trim());
		
		return list;
	}

	public static long getSeconds(ConfigNodes node) {

		try {
			return TimeTools.getSeconds(getString(node));
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase(Locale.ROOT) + " from config.yml");
			return 1;
		}
	}

	public static long getMillis(ConfigNodes node) {

		try {
			return TimeTools.getMillis(getString(node));
		} catch (NumberFormatException e) {
			sendError(node.getRoot().toLowerCase(Locale.ROOT) + " from config.yml");
			return 1;
		}
	}

	public static void addComment(String root, String... comments) {

		newConfig.addComment(root.toLowerCase(Locale.ROOT), comments);
	}

	/**
	 * Builds a new config reading old config data.
	 */
	private static void setDefaults(String version, Path configPath) {

		newConfig = new CommentedConfiguration(configPath);
		newConfig.load();

		for (ConfigNodes root : ConfigNodes.values()) {
			if (root.getComments().length > 0)
				addComment(root.getRoot(), root.getComments());

			if (root.getRoot().equals(ConfigNodes.LEVELS.getRoot())) {
				
				setDefaultLevels();
				
			} else if ( (root.getRoot().equals(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()))
				|| (root.getRoot().equals(ConfigNodes.LEVELS_NATION_LEVEL.getRoot()))){

				// Do nothing here as setDefaultLevels configured town and
				// nation levels.

			} else if (root.getRoot().equals(ConfigNodes.VERSION.getRoot())) {
				setNewProperty(root.getRoot(), version);
			} else if (root.getRoot().equals(ConfigNodes.LAST_RUN_VERSION.getRoot())) {
				setNewProperty(root.getRoot(), getLastRunVersion(version));
			} else if (root.getRoot().equals(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot())) {
				setNewProperty(root.getRoot(), root.getDefault());
				setTownBlockTypes();
			} else	
				setNewProperty(root.getRoot(), (config.get(root.getRoot().toLowerCase(Locale.ROOT)) != null) ? config.get(root.getRoot().toLowerCase(Locale.ROOT)) : root.getDefault());

		}

		config = newConfig;
		newConfig = null;
	}

	private static void setDefaultLevels() {

		addComment(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), "# default Town levels.");
		if (!config.contains(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot())) {
			List<Map<String, Object>> levels = new ArrayList<>();
			Map<String, Object> level = new HashMap<>();
			level.put("numResidents", 0);
			level.put("namePrefix", "");
			level.put("namePostfix", " Ruins");
			level.put("mayorPrefix", "Spirit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 1);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 1);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Settlement)");
			level.put("mayorPrefix", "Hermit ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 16);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 0);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 2);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Hamlet)");
			level.put("mayorPrefix", "Chief ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 32);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 6);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Village)");
			level.put("mayorPrefix", "Baron Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 96);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 1);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 10);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Town)");
			level.put("mayorPrefix", "Viscount ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 160);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 14);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large Town)");
			level.put("mayorPrefix", "Count Von ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 224);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 2);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 20);
			level.put("namePrefix", "");
			level.put("namePostfix", " (City)");
			level.put("mayorPrefix", "Earl ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 320);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 24);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Large City)");
			level.put("mayorPrefix", "Duke ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 384);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 3);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			level.put("numResidents", 28);
			level.put("namePrefix", "");
			level.put("namePostfix", " (Metropolis)");
			level.put("mayorPrefix", "Lord ");
			level.put("mayorPostfix", "");
			level.put("townBlockLimit", 448);
			level.put("upkeepModifier", 1.0);
			level.put("peacefulCostMultiplier", 1.0);
			level.put("townOutpostLimit", 4);
			level.put("townBlockBuyBonusLimit", 0);
			level.put("debtCapModifier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("resourceProductionModifier", 1.0);
			level.put("townBlockTypeLimits", new HashMap<>());
			levels.add(new HashMap<>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), levels);
		} else {
			newConfig.set(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()));
		}

		addComment(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), "# default Nation levels.");

		if (!config.contains(ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 0);
			level.put("nationCapitalBonusOutpostLimit", 0);
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 1);
			level.put("nationCapitalBonusOutpostLimit", 0);
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 1);
			level.put("nationBonusOutpostLimit", 2);
			level.put("nationCapitalBonusOutpostLimit", 0);
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 3);
			level.put("nationCapitalBonusOutpostLimit", 0);
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 2);
			level.put("nationBonusOutpostLimit", 4);
			level.put("nationCapitalBonusOutpostLimit", 0);
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
			level.put("peacefulCostMultiplier", 1.0);
			level.put("bankCapModifier", 1.0);
			level.put("nationZonesSize", 3);
			level.put("nationBonusOutpostLimit", 5);
			level.put("nationCapitalBonusOutpostLimit", 0);
			levels.add(new HashMap<>(level));
			level.clear();
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), levels);
		} else
			newConfig.set(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), config.get(ConfigNodes.LEVELS_NATION_LEVEL.getRoot()));
	}
	
	private static void setTownBlockTypes() {
		if (!config.contains(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot())) {
			// The TownBlockTypes section does not exist yet. 
			List<Map<String, Object>> types = new ArrayList<>();
			Map<String, Object> type = new LinkedHashMap<>();
			
			type.put("name", "default");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "+");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "shop");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "C");
			type.put("colour", "<blue>");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "arena");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "A");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();
			
			type.put("name", "embassy");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "E");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "wilds");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "W");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", getDefaultWildsblocks());
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "inn");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "I");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "jail");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "J");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "farm");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "F");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", getDefaultFarmblocks());
			types.add(new LinkedHashMap<>(type));
			type.clear();

			type.put("name", "bank");
			type.put("cost", 0.0);
			type.put("tax", 0.0);
			type.put("mapKey", "B");
			type.put("colour", "");
			type.put("itemUseIds", "");
			type.put("switchIds", "");
			type.put("allowedBlocks", "");
			types.add(new LinkedHashMap<>(type));
			type.clear();
			newConfig.set(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot(), types);
		} else
			// The TownBlockTypes section exists, use the existing config's values.
			newConfig.set(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot(), config.get(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot()));
	}
	
	public static String getDefaultFarmblocks() {
		Set<String> farmMaterials = new HashSet<>();
		farmMaterials.addAll(ItemLists.SAPLINGS.getMaterialNameCollection());
		farmMaterials.addAll(ItemLists.TREES.getMaterialNameCollection()); // Includes Leaves.
		farmMaterials.addAll(ItemLists.PLANTS.getMaterialNameCollection());
		farmMaterials.addAll(ItemLists.CROPS.getMaterialNameCollection());
		farmMaterials.addAll(Arrays.asList("COW_SPAWN_EGG,GOAT_SPAWN_EGG,MOOSHROOM_SPAWN_EGG")); // For milking tests.
		farmMaterials.add("SHROOMLIGHT");
		farmMaterials.add("SHEARS");
		return StringMgmt.join(farmMaterials, ",");
	}
	
	public static String getDefaultWildsblocks() {
		return "GOLD_ORE,IRON_ORE,COAL_ORE,COPPER_ORE,REDSTONE_ORE,EMERALD_ORE,LAPIS_ORE,DIAMOND_ORE,DEEPSLATE_COAL_ORE,DEEPSLATE_IRON_ORE,DEEPSLATE_COPPER_ORE,DEEPSLATE_GOLD_ORE,DEEPSLATE_EMERALD_ORE,DEEPSLATE_REDSTONE_ORE,DEEPSLATE_LAPIS_ORE,DEEPSLATE_DIAMOND_ORE,NETHER_GOLD_ORE,NETHER_QUARTZ_ORE,ANCIENT_DEBRIS,OAK_LOG,SPRUCE_LOG,BIRCH_LOG,JUNGLE_LOG,ACACIA_LOG,DARK_OAK_LOG,CRIMSON_STEM,WARPED_STEM,ACACIA_LEAVES,OAK_LEAVES,DARK_OAK_LEAVES,JUNGLE_LEAVES,BIRCH_LEAVES,SPRUCE_LEAVES,CRIMSON_HYPHAE,WARPED_HYPHAE,ACACIA_SAPLING,BAMBOO_SAPLING,BIRCH_SAPLING,DARK_OAK_SAPLING,JUNGLE_SAPLING,OAK_SAPLING,SPRUCE_SAPLING,TALL_GRASS,BROWN_MUSHROOM,RED_MUSHROOM,CACTUS,ALLIUM,AZURE_BLUET,BLUE_ORCHID,CORNFLOWER,DANDELION,LILAC,LILY_OF_THE_VALLEY,ORANGE_TULIP,OXEYE_DAISY,PEONY,PINK_TULIP,POPPY,RED_TULIP,ROSE_BUSH,SUNFLOWER,WHITE_TULIP,WITHER_ROSE,CRIMSON_FUNGUS,LARGE_FERN,TORCH,LADDER,CLAY,PUMPKIN,GLOWSTONE,VINE,NETHER_WART_BLOCK,COCOA";
	}

	public static String getKingPrefix(Resident resident) {
		return resident.isKing() ? resident.getNationOrNull().getNationLevel().kingPrefix() : "";
	}

	public static String getMayorPrefix(Resident resident) {
		return resident.isMayor() ? resident.getTownOrNull().getTownLevel().mayorPrefix() : "";
	}

	public static String getCapitalPostfix(Town town) {
		return town.hasNation() ? getCapitalPostfix(town.getNationOrNull()) : "";
	}

	public static String getCapitalPostfix(Nation nation) {
		return Colors.translateColorCodes(nation.getNationLevel().capitalPostfix);
	}

	public static String getTownPostfix(Town town) {

		try {
			return Colors.translateColorCodes(town.getTownLevel().namePostfix());
		} catch (Exception e) {
			sendError("getTownPostfix.");
			return "";
		}
	}

	public static String getNationPostfix(Nation nation) {

		try {
			return Colors.translateColorCodes(nation.getNationLevel().namePostfix());
		} catch (Exception e) {
			sendError("getNationPostfix.");
			return "";
		}
	}

	public static String getNationPrefix(Nation nation) {

		try {
			return Colors.translateColorCodes(nation.getNationLevel().namePrefix());
		} catch (Exception e) {
			sendError("getNationPrefix.");
			return "";
		}
	}

	public static String getTownPrefix(Town town) {

		try {
			return Colors.translateColorCodes(town.getTownLevel().namePrefix());
		} catch (Exception e) {
			sendError("getTownPrefix.");
			return "";
		}
	}

	public static String getCapitalPrefix(Town town) {
		return town.hasNation() ? getCapitalPrefix(town.getNationOrNull()) : "";
	}

	public static String getCapitalPrefix(Nation nation) {
		return Colors.translateColorCodes(nation.getNationLevel().capitalPrefix);
	}

	public static String getKingPostfix(Resident resident) {
		return resident.isKing() ? resident.getNationOrNull().getNationLevel().kingPostfix() : "";
	}

	public static String getMayorPostfix(Resident resident) {
		return resident.isMayor() ? resident.getTownOrNull().getTownLevel().mayorPostfix() : "";
	}

	public static String getNPCPrefix() {

		return getString(ConfigNodes.FILTERS_NPC_PREFIX.getRoot(), ConfigNodes.FILTERS_NPC_PREFIX.getDefault());
	}

	public static boolean getBedUse() {

		return getBoolean(ConfigNodes.RES_SETTING_DENY_BED_USE);
	}
	
	
	public static String getDatabaseVersion() {
		return DatabaseConfig.getString(DatabaseConfig.DATEBASE_VERSION);
	}
	
	public static void setDatabaseVersion(String version) {
		DatabaseConfig.setDatabaseVersion(version);
	}

	public static String getLoadDatabase() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_LOAD);
	}

	public static String getSaveDatabase() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_SAVE);
	}

	// SQL
	public static String getSQLHostName() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_HOSTNAME);
	}

	public static String getSQLPort() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_PORT);
	}

	public static String getSQLDBName() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_DBNAME);
	}

	public static String getSQLTablePrefix() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_TABLEPREFIX);
	}

	public static String getSQLUsername() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_USERNAME);
	}

	public static String getSQLPassword() {

		return DatabaseConfig.getString(DatabaseConfig.DATABASE_PASSWORD);
	}
	
	public static String getSQLFlags() {
		
		return DatabaseConfig.getString(DatabaseConfig.DATABASE_FLAGS);
	}

	public static int getMaxPoolSize() {
		return DatabaseConfig.getInt(DatabaseConfig.DATABASE_POOLING_MAX_POOL_SIZE);
	}

	public static int getMaxLifetime() {
		return DatabaseConfig.getInt(DatabaseConfig.DATABASE_POOLING_MAX_LIFETIME);
	}

	public static int getConnectionTimeout() {
		return DatabaseConfig.getInt(DatabaseConfig.DATABASE_POOLING_CONNECTION_TIMEOUT);
	}

	public static int getMaxTownBlocks(Town town) {

		int ratio = getTownBlockRatio();
		int n = town.getBonusBlocks() + town.getPurchasedBlocks();

		if (ratio == 0)
			n += town.getTownLevel().townBlockLimit();
		else
			n += town.getNumResidents() * ratio;

		n += getNationBonusBlocks(town);
		
		int ratioSizeLimit = getInt(ConfigNodes.CLAIMING_TOWN_BLOCK_LIMIT);
		if (ratio != 0 && ratioSizeLimit > 0)
			n = Math.min(ratioSizeLimit, n);

		TownCalculateMaxTownBlocksEvent event = new TownCalculateMaxTownBlocksEvent(town, n);
		BukkitTools.fireEvent(event);

		return event.getTownBlockCount();
	}

	public static int getMaxTownBlocks(Town town, int residents) {
		int ratio = getTownBlockRatio();
		int amount = town.getBonusBlocks() + town.getPurchasedBlocks();

		if (ratio == 0)
			amount += getTownLevelWithModifier(residents, town).townBlockLimit();
		else
			amount += residents * ratio;

		amount += getNationBonusBlocks(town);

		TownCalculateMaxTownBlocksEvent event = new TownCalculateMaxTownBlocksEvent(town, amount);
		BukkitTools.fireEvent(event);

		return event.getTownBlockCount();
	}
	
	public static int getMaxOutposts(Town town, int residents) {
		return getMaxOutposts(town, residents, town.hasNation() ? town.getNationOrNull().getTowns().size() : 1);
	}

	public static int getMaxOutposts(Town town, int residentsAmount, int townsAmount) {
		
		int townOutposts = getTownLevelWithModifier(residentsAmount, town).townOutpostLimit();
		int nationOutposts = 0;
		if (town.hasNation()) {
			int modifier = TownySettings.isNationLevelDeterminedByTownCount() ? townsAmount : residentsAmount;
			nationOutposts = getNationLevelWithModifier(modifier).nationBonusOutpostLimit();
		}
		return townOutposts + nationOutposts;
	}
	
	public static int getMaxOutposts(Town town) {
		
		int townOutposts = town.getTownLevel().townOutpostLimit();
		int nationOutposts = 0;
		if (town.hasNation()) {
			Nation nation = town.getNationOrNull();
			if (nation != null)
				nationOutposts = nation.getNationLevel().nationBonusOutpostLimit();
			if (town.isCapital())
				nationOutposts += nation.getNationLevel().nationCapitalBonusOutpostLimit();
		}
		
		return townOutposts + nationOutposts;
	}
	
	public static int getMaxBonusBlocks(Town town, int residents) {
		
		return getTownLevelWithModifier(residents, town).townBlockBuyBonusLimit();
	}
	
	public static int getMaxBonusBlocks(Town town) {
		
		return getMaxBonusBlocks(town, town.getNumResidents());
	}

	public static int getNationBonusBlocks(Nation nation) {
		int bonusBlocks = nation.getNationLevel().townBlockLimitBonus();
		NationBonusCalculationEvent calculationEvent = new NationBonusCalculationEvent(nation, bonusBlocks);
		BukkitTools.fireEvent(calculationEvent);
		return calculationEvent.getBonusBlocks();
	}

	public static int getNationBonusBlocks(Town town) {

		if (town.hasNation())
			return getNationBonusBlocks(town.getNationOrNull());
		return 0;
	}

	public static boolean areTownBlocksUnlimited() {
		return getTownBlockRatio() < 0;
	}
	
	public static int getTownBlockRatio() {

		return getInt(ConfigNodes.CLAIMING_TOWN_BLOCK_RATIO);
	}

	public static int getNewTownBonusBlocks() {
		return getInt(ConfigNodes.CLAIMING_DEF_BONUS_CLAIMS);
	}

	public static int getTownBlockSize() {

		return getInt(ConfigNodes.CLAIMING_TOWN_BLOCK_SIZE);
	}

	public static boolean isShowingClaimParticleEffect() {
		return getBoolean(ConfigNodes.CLAIMING_SHOW_CLAIM_PARTICLES);
	}

	public static boolean isFriendlyFireEnabled() {

		return getBoolean(ConfigNodes.NWS_FRIENDLY_FIRE_ENABLED);
	}
	
	public static boolean isUsingEconomy() {

		return getBoolean(ConfigNodes.ECO_USING_ECONOMY);
	}

	public static boolean isFakeResident(String name) {

		return StringMgmt.containsIgnoreCase(getStrArr(ConfigNodes.PLUGIN_MODS_FAKE_RESIDENTS), name);
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
	
	public static boolean isNotificationOwnerShowingVerboseName() {
		
		return getBoolean(ConfigNodes.NOTIFICATION_OWNER_SHOWS_VERBOSE_NAME);
	}

	public static boolean isNotificationsAppearingInActionBar() {
		return getString(ConfigNodes.NOTIFICATION_NOTIFICATIONS_APPEAR_AS).equalsIgnoreCase("action_bar");
	}

	public static boolean getShowTownBoardOnLogin() {

		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DISPLAY_TOWNBOARD_ONLOGIN);
	}
	
	public static boolean getShowNationBoardOnLogin() {
		
		return getBoolean(ConfigNodes.GNATION_SETTINGS_DISPLAY_NATIONBOARD_ONLOGIN);
	}
	
	public static boolean nationCapitalsCantBeNeutral() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_CAPITAL_CANNOT_BE_NEUTRAL);
	}

	public static String getUnclaimedZoneName() {

		return Translation.of("UNCLAIMED_ZONE_NAME");
	}

	public static boolean doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() {
		return getBoolean(ConfigNodes.FILTERS_MODIFY_CHAT_DO_TITLES_AND_SURTITLES_REQUIRE_PERMISSION_FOR_COLOUR_CODES);
	}

	public static int getMaxTitleLength() {

		return getInt(ConfigNodes.FILTERS_MODIFY_CHAT_MAX_TITLE_LENGTH);
	}

	public static int getMaxNameLength() {

		return getInt(ConfigNodes.FILTERS_MAX_NAME_LGTH);
	}

	public static int getMaxNameCapitalLetters() {

		return getInt(ConfigNodes.FILTERS_MAX_NAME_CAPITAL_LETTERS);
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

	public static String getDefaultTownName() {

		return getString(ConfigNodes.RES_SETTING_DEFAULT_TOWN_NAME);
	}

	public static List<String> getWorldMobRemovalEntities() {
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WORLD);
	}

	public static List<String> getWildernessMobRemovalEntities() {
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WILDERNESS);
	}

	public static List<String> getTownMobRemovalEntities() {
		return getStrArr(ConfigNodes.PROT_MOB_REMOVE_TOWN);
	}

	public static boolean isEconomyAsync() {

		return getBoolean(ConfigNodes.ECO_USE_ASYNC);
	}
	
	public static long getCachedBankTimeout() {
		return getSeconds(ConfigNodes.ECO_BANK_CACHE_TIMEOUT) * 1000;
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
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_ENTITY_REVERT_LIST);
	}

	public static List<String> getWildExplosionRevertBlockWhitelist() {
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_REVERT_BLOCK_WHITELIST);
	}

	public static List<String> getWildExplosionRevertMaterialsToNotOverwrite() {
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_REVERT_BLOCKS_TO_NOT_OVERWRITE);
	}

	public static List<String> getWildExplosionProtectionBlocks() {
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_BLOCK_REVERT_LIST);
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

	public static boolean preventSaturationLoss() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_REGEN_PREVENT_SATURATION_LOSS);
	}

	public static boolean beaconsForTownMembersOnly() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_BEACONS_FOR_ALLIES_ONLY);
	}

	public static boolean beaconsExcludeConqueredTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_BEACONS_EXCLUDE_CONQUERED_TOWNS);
	}

	public static boolean getTownDefaultPublic() {

		return getBoolean(ConfigNodes.TOWN_DEF_PUBLIC);
	}

	public static boolean getTownDefaultOpen() {

		return getBoolean(ConfigNodes.TOWN_DEF_OPEN);
	}
	
	public static boolean getTownDefaultNeutral() {

		return getBoolean(ConfigNodes.TOWN_DEF_NEUTRAL); 
	}

	public static String getTownDefaultBoard() {

		return getString(ConfigNodes.TOWN_DEF_BOARD);
	}

	public static boolean getNationDefaultOpen() {

		return getBoolean(ConfigNodes.NATION_DEF_OPEN);
	}
	
	public static boolean isNationTagSetAutomatically() {
		return getBoolean(ConfigNodes.NATION_DEF_TAG);
	}
	
	public static boolean isTownTagSetAutomatically() {
		return getBoolean(ConfigNodes.TOWN_DEF_TAG);
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
	
	public static boolean doesPlotTaxNonPaymentSetPlotForSale() {
		return getBoolean(ConfigNodes.TOWN_DEF_TAXES_PLOT_TAX_PUTS_PLOT_FOR_SALE);
	}

	public static boolean getTownDefaultTaxPercentage() {

		return getBoolean(ConfigNodes.TOWN_DEF_TAXES_TAXPERCENTAGE);
	}
	
	public static double getTownDefaultTaxMinimumTax() {
		
		return getDouble(ConfigNodes.TOWN_DEF_TAXES_MINIMUMTAX);
	}
	
	public static boolean getTownDefaultAllowedToWar() {
		return getBoolean(ConfigNodes.TOWN_DEF_ALLOWED_TO_WAR);
	}
	
	public static boolean hasTownLimit() {

		return getTownLimit() != 0;
	}

	public static int getTownLimit() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_LIMIT);
	}

	public static int getMaxPurchasedBlocks(Town town, int residents) {
		if (isBonusBlocksPerTownLevel())
			return getMaxBonusBlocks(town, residents);
		else
			return getInt(ConfigNodes.CLAIMING_MAX_PURCHASED_BLOCKS);
	}
	
	public static int getMaxPurchasedBlocks(Town town) {

		if (isBonusBlocksPerTownLevel())
			return getMaxBonusBlocks(town);
		else
			return getInt(ConfigNodes.CLAIMING_MAX_PURCHASED_BLOCKS);
	}
	
	public static int getMaxPurchasedBlocksNode() {
		
			return getInt(ConfigNodes.CLAIMING_MAX_PURCHASED_BLOCKS);
	}
	
	public static int getMaxClaimRadiusValue() {
		
		return getInt(ConfigNodes.CLAIMING_MAX_CLAIM_RADIUS_VALUE);
	}

	public static boolean isUnwantedBiomeClaimingEnabled() {
		return getBoolean(ConfigNodes.CLAIMING_BIOME_UNWANTED_BIOMES_ENABLED);
	}

	public static double getUnwantedBiomeThreshold() {
		return getDouble(ConfigNodes.CLAIMING_BIOME_UNWANTED_BIOMES_THRESHOLD) / 100;
	}

	public static List<String> getUnwantedBiomeNames() {
		return getStrArr(ConfigNodes.CLAIMING_BIOME_UNWANTED_BIOMES);
	}

	public static boolean isOceanClaimingBlocked() {
		return getBoolean(ConfigNodes.CLAIMING_BIOME_BLOCK_OCEAN_CLAIMS);
	}

	public static double getOceanBlockThreshold() {
		return getDouble(ConfigNodes.CLAIMING_BIOME_BLOCK_OCEAN_THRESHOLD) / 100;
	}

	public static boolean isOverClaimingAllowingStolenLand() {
		return getBoolean(ConfigNodes.CLAIMING_OVER_ALLOWED_CLAIM_LIMITS_ALLOWS_STEALING_LAND);
	}

	public static boolean isOverClaimingPreventedByHomeBlockRadius() {
		return getBoolean(ConfigNodes.CLAIMING_OVERCLAIMING_PREVENTED_BY_HOMEBLOCK_RADIUS);
	}

	public static long getOverclaimingTownAgeRequirement() {
		return getMillis(ConfigNodes.CLAIMING_OVERCLAIMING_TOWN_AGE_REQUIREMENT);
	}

	public static int getOverclaimingCommandCooldownInSeconds() {
		return (int) getSeconds(ConfigNodes.CLAIMING_OVERCLAIMING_COMMAND_COOLDOWN);
	}

	public static boolean isOverclaimingWithNationsRequiringEnemy() {
		return getBoolean(ConfigNodes.CLAIMING_OVERCLAIMING_REQUIRES_NATIONS_TO_BE_ENEMIES);
	}

	public static boolean isSellingBonusBlocks(Town town) {

		return getMaxPurchasedBlocks(town) != 0;
	}
	
	public static boolean isBonusBlocksPerTownLevel() { 
		
		return getBoolean(ConfigNodes.CLAIMING_MAX_PURCHASED_BLOCKS_USES_TOWN_LEVELS);
	}

	public static double getPurchasedBonusBlocksCost() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK);
	}

	public static double getPurchasedBonusBlocksIncreaseValue() {

		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE);
	}
	
	public static double getPurchasedBonusBlocksMaxPrice() {
		
		return getDouble(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCKS_MAXIMUM);
	}
	
	public static boolean isTownSpawnPaidToTown() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_SPAWN_PAID_TO_TOWN);
	}

	public static double getNationNeutralityCost(Nation nation) {
		double cost = nation.getNationLevel().peacefulCostMultiplier() * getNationNeutralityCost();
		return isNationNeutralityCostMultipliedByNationTownAmount() ? cost * nation.getTowns().size() : cost;
	}

	public static double getNationNeutralityCost() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY);
	}

	public static boolean isNationNeutralityCostMultipliedByNationTownAmount() {
		return getBoolean(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY_CHARGES_PER_TOWN);
	}

	public static double getTownNeutralityCost(Town town) {
		double cost = town.getTownLevel().peacefulCostMultiplier() * getTownNeutralityCost();
		return isTownNeutralityCostMultipliedByTownClaimsSize() ? cost * town.getTownBlocks().size() : cost;
	}

	public static double getTownNeutralityCost() {
		
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_NEUTRALITY);
	}

	public static boolean isTownNeutralityCostMultipliedByTownClaimsSize() {
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_NEUTRALITY_CHARGES_PER_PLOT);
	}

	public static boolean isAllowingOutposts() {

		return getBoolean(ConfigNodes.CLAIMING_ALLOW_OUTPOSTS);
	}
	
	public static boolean isOutpostsLimitedByLevels() {

		return getBoolean(ConfigNodes.CLAIMING_LIMIT_OUTPOST_USING_LEVELS);
	}
	
	public static boolean isOutpostLimitStoppingTeleports() {
		
		return getBoolean(ConfigNodes.CLAIMING_OVER_OUTPOST_LIMIT_STOP_TELEPORT);
	}

	public static double getOutpostCost() {

		return getDouble(ConfigNodes.ECO_PRICE_OUTPOST);
	}

	public static Set<Material> getSwitchMaterials() {

		return switchUseMaterials;
	}
	
	public static Set<Material> getItemUseMaterials() {

		return itemUseMaterials;
	}

	public static boolean isSwitchMaterial(Material material, Location location) {
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
		
		if (townBlock != null)
			return townBlock.getData().getSwitchIds().contains(material);
		else
			return switchUseMaterials.contains(material);
	}

	public static boolean isItemUseMaterial(Material material, Location location) {
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
		
		if (townBlock != null)
			return townBlock.getData().getItemUseIds().contains(material);
		else
			return itemUseMaterials.contains(material);
	}
	
	public static List<String> getFireSpreadBypassMaterials() {
		
		return getStrArr(ConfigNodes.PROT_FIRE_SPREAD_BYPASS);
	}
	
	public static boolean isFireSpreadBypassMaterial(String mat) {
		
		return getFireSpreadBypassMaterials().contains(mat);
	}
	
	public static Collection<Material> getUnclaimedZoneIgnoreMaterials() {

		return toMaterialSet(getStrArr(ConfigNodes.UNCLAIMED_ZONE_IGNORE));
	}
	
	public static List<Class<?>> getProtectedEntityTypes() {
		return protectedMobs;
	}
	
	public static List<String> getPotionTypes() {

		return getStrArr(ConfigNodes.PROT_POTION_TYPES);
	}

	public static void setProperty(String root, Object value) {

		config.set(root.toLowerCase(Locale.ROOT), value.toString());
	}

	private static void setNewProperty(String root, Object value) {

		if (value == null) {
			TownyMessaging.sendDebugMsg("value is null for " + root.toLowerCase(Locale.ROOT));
			value = "";
		}
		newConfig.set(root.toLowerCase(Locale.ROOT), value.toString());
	}
	
	public static void setLanguage(String lang) {
		config.set(ConfigNodes.LANGUAGE.getRoot(), lang);
		config.save();
	}

	public static Object getProperty(String root) {

		return config.get(root.toLowerCase(Locale.ROOT));
	}

	public static double getClaimPrice() {

		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK);
	}
	
	public static double getClaimPriceIncreaseValue() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_INCREASE);
	}
	
	public static double getMaxClaimPrice() {
		
		return getDouble(ConfigNodes.ECO_MAX_PRICE_CLAIM_TOWNBLOCK);
	}
	
	public static double getClaimRefundPrice() {
		
		return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK_REFUND);
	}

	public static double getTakeoverClaimPrice() {
		return getDouble(ConfigNodes.ECO_PRICE_TAKEOVERCLAIM_PRICE);
	}

	public static boolean getUnclaimedZoneSwitchRights() {

		return getBoolean(ConfigNodes.UNCLAIMED_ZONE_SWITCH);
	}

	public static boolean getEndermanProtect() {

		return getBoolean(ConfigNodes.NWS_WORLD_ENDERMAN);
	}

	public static long getDayInterval() {

		// Returns the lesser over the day interval or 1 day. 
		return Math.min(getSeconds(ConfigNodes.PLUGIN_DAY_INTERVAL), TimeTools.getSeconds("1d"));
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
		return getSpawnLevel(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN);
	}

	public static SpawnLevel isAllowingPublicTownSpawnTravel() {
		return getSpawnLevel(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL);
	}

	public static boolean isConfigAllowingTownSpawn() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN);
	}

	public static boolean isConfigAllowingPublicTownSpawnTravel() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL);
	}

	public static boolean isConfigAllowingTownSpawnNationTravel() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL_NATION);
	}

	public static boolean isConfigAllowingTownSpawnNationAllyTravel() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL_ALLY);
	}

	public static boolean isConfigAllowingNationSpawn() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN);
	}

	public static boolean isConfigAllowingPublicNationSpawnTravel() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN_TRAVEL);
	}

	public static boolean isConfigAllowingNationSpawnAllyTravel() {
		return getBoolean(ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN_TRAVEL_ALLY);
	}

	public static List<String> getDisallowedTownSpawnZones() {
		return getStrArr(ConfigNodes.SPAWNING_PREVENT_TOWN_SPAWN_IN);
	}

	public static boolean areEnemiesAllowedToSpawnToPeacefulTowns() {
		return getBoolean(ConfigNodes.SPAWNING_ENEMIES_ALLOWED_TO_SPAWN_TO_PEACEFUL_TOWNS);
	}

	public static boolean isSpawnWarnConfirmationUsed() {
		return getBoolean(ConfigNodes.SPAWNING_COST_SPAWN_WARNINGS);
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
	
	public static double getMaxTownTaxPercentAmount() { 
		
		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_TOWN_TAX_PERCENT_AMOUNT); 
	}

	public static double getMaxNationTaxPercentAmount() {

		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_NATION_TAX_PERCENT_AMOUNT);
	}

	public static double getMaxNationConqueredTaxAmount() {
		return getDouble(ConfigNodes.NATION_DEF_TAXES_MAX_CONQUEREDTAX);
	}

	public static double getDefaultNationConqueredTaxAmount() {
		return getDouble(ConfigNodes.NATION_DEF_TAXES_CONQUEREDTAX);
	}

	public static double getMaxNationTaxPercent() {

		return getDouble(ConfigNodes.ECO_DAILY_TAXES_MAX_NATION_TAX_PERCENT);
	}
	
	public static boolean isBackingUpDaily() {

		return getBoolean(ConfigNodes.PLUGIN_DAILY_BACKUPS);
	}
	
	public static boolean isChargingDeath() {
		
		return (getDeathPrice()>0 || getDeathPriceTown()>0 || getDeathPriceNation()>0 );
	}
	
	public static boolean isDeathPriceType() {

		return getString(ConfigNodes.ECO_PRICE_DEATH_TYPE).equalsIgnoreCase("fixed");
	}
	
	public static boolean isDeathPricePercentBased() {
		return !isDeathPriceType();
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
	
	public static boolean isDeletedObjectBalancePaidToOwner() {
		return getBoolean(ConfigNodes.ECO_BANK_IS_DELETED_OBJECT_BALANCE_PAID_TO_OWNER);
	}

	public static boolean isEcoClosedEconomyEnabled() {
		
		return getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED);
	}
	
	public static boolean isJailingAttackingEnemies() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_ENEMIES);
	}
	
	public static int getMaxJailedNewJailBehavior() {
		return getInt(ConfigNodes.JAIL_MAX_JAILED_NEWJAIL_BEHAVIOR);
	}

	public static boolean isJailBookEnabled() {
		return getBoolean(ConfigNodes.JAIL_IS_JAILBOOK_ENABLED);
	}

	public static boolean isJailingAttackingOutlaws() {
		
		return getBoolean(ConfigNodes.JAIL_IS_JAILING_ATTACKING_OUTLAWS);
	}
	
	public static int getJailedOutlawJailHours() {
		
		return getInt(ConfigNodes.JAIL_OUTLAW_JAIL_HOURS);
	}

	public static int getJailedPOWJailHours() {
		return getInt(ConfigNodes.JAIL_POW_JAIL_HOURS);
	}

	public static int getJailedMaxHours() {
		return getInt(ConfigNodes.JAIL_MAX_JAIL_HOURS);
	}

	public static boolean JailAllowsTeleportItems() {
		
		return getBoolean(ConfigNodes.JAIL_JAIL_ALLOWS_TELEPORT_ITEMS);
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

	public static double getBailMaxAmount() {
		return getDouble(ConfigNodes.JAIL_BAIL_BAILMAX_AMOUNT);
	}

	public static double initialJailFee() {
		return getDouble(ConfigNodes.JAIL_FEE_INITIAL_AMOUNT);
	}

	public static double hourlyJailFee() {
		return getDouble(ConfigNodes.JAIL_FEE_HOURLY_AMOUNT);
	}

	public static int getMaxJailedPlayerCount() {
		return getInt(ConfigNodes.JAIL_MAX_JAILED_COUNT);
	}
	
	public static double getBailAmountMayor() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_MAYOR);
	}
	
	public static double getBailAmountKing() {
		
		return getDouble(ConfigNodes.JAIL_BAIL_BAIL_AMOUNT_KING);
	}
	
	public static boolean doJailPlotsPreventPVP() {
		return getBoolean(ConfigNodes.JAIL_PLOTS_DENY_PVP);
	}
	
	public static boolean doesJailingPreventLoggingOut() {
		return getBoolean(ConfigNodes.JAIL_PREVENTS_LOGGING_OUT);
	}
	
	public static long newPlayerJailImmunity() {
		return TimeTools.getMillis(getString(ConfigNodes.JAIL_NEW_PLAYER_IMMUNITY));
	}

	public static boolean doesUnjailingTeleportPlayer() {
		return getBoolean(ConfigNodes.JAIL_UNJAIL_TELEPORT);
	}

	public static boolean showBailTitle() {
		return getBoolean(ConfigNodes.JAIL_SHOW_BAIL_TITLE);
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

	public static double getTownUpkeepCost(Town town) {
		if (!TownySettings.isTaxingDaily())
			return 0.0;
		
		TownUpkeepCalculationEvent event = new TownUpkeepCalculationEvent(town, getTownUpkeepCostRaw(town));
		BukkitTools.fireEvent(event);
		return event.getUpkeep();
	}

	private static double getTownUpkeepCostRaw(Town town) {
		if (town == null || !town.hasUpkeep())
			return 0.0;

		// When we are doing per-plot-upkeep we use the town size instead of the upkeep modified in the Town Level.
		double townMultiplier = isUpkeepByPlot() ? town.getTownBlocks().size() : town.getTownLevel().upkeepModifier();
		// If the town has a nation we will be altering thing with the nation's TownUpkeepModifier, or 1.0 if no nation.
		double nationMultiplier = town.hasNation() ? town.getNationOrNull().getNationLevel().nationTownUpkeepModifier() : 1.0;
		// Nation's multiplier should only affect plot-based-upkeep if the config is set for it.
		if (isUpkeepByPlot() && !isTownLevelModifiersAffectingPlotBasedUpkeep())
			nationMultiplier = 1.0;

		// There's the chance that even with per-plot-upkeep, the townLevel upkeep modifier is still used, or 1.0 if not. 
		double townLevelPlotModifier = isUpkeepByPlot() && isTownLevelModifiersAffectingPlotBasedUpkeep() ? town.getTownLevel().upkeepModifier() : 1.0;

		// outposts can have an added cost to the town's upkeep.
		double outpostCost = getPerOutpostUpkeepCost() * town.getMaxOutpostSpawn();
		// outposts having a cost will mess up the per-plot-upkeep feature, so add that on later.
		double baseUpkeep = getTownUpkeep() + (isUpkeepByPlot() ? 0 : outpostCost);
		// Amount is calculated using the above multipliers.
		double amount = ((baseUpkeep * townMultiplier) * townLevelPlotModifier) * nationMultiplier;

		// When per-plot-upkeep is in use, there can be min/max amounts.
		if (isUpkeepByPlot()) {
			// Tack on the outpost cost here when isUpkeepByPlot is used.
			amount += outpostCost;
			if (TownySettings.getPlotBasedUpkeepMinimumAmount() > 0.0)
				amount = Math.max(amount, TownySettings.getPlotBasedUpkeepMinimumAmount());
			if (TownySettings.getPlotBasedUpkeepMaximumAmount() > 0.0) 
				amount = Math.min(amount, TownySettings.getPlotBasedUpkeepMaximumAmount());
		}
		return amount;
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

	public static double getPlotBasedUpkeepMaximumAmount () {
		
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_MAXIMUM_AMOUNT);
		
	}

	public static boolean isTownLevelModifiersAffectingPlotBasedUpkeep() {
		
		return getBoolean(ConfigNodes.ECO_PRICE_TOWN_UPKEEP_PLOTBASED_TOWNLEVEL_MODIFIER);
	
	}

	public static boolean isUpkeepPayingPlots() {

		return getBoolean(ConfigNodes.ECO_TAXES_ALLOW_PLOT_PAYMENTS);
	}
	
	public static boolean isNegativePlotTaxAllowed() {
		return getBoolean(ConfigNodes.ECO_TAXES_ALLOW_PLAYER_OWNED_PLOT_PAYMENTS);
	}

	public static boolean isNegativeTownTaxAllowed() {
		return getBoolean(ConfigNodes.ECO_TAXES_ALLOW_NEGATIVE_TOWN_TAX);
	}

	public static boolean isNegativeNationTaxAllowed() {
		return getBoolean(ConfigNodes.ECO_TAXES_ALLOW_NEGATIVE_NATION_TAX);
	}

	public static double getTownPenaltyUpkeepCost(Town town) {
		TownUpkeepPenalityCalculationEvent event = new TownUpkeepPenalityCalculationEvent(town, getTownPenaltyUpkeepCostRaw(town));
		BukkitTools.fireEvent(event);
		return event.getUpkeep();
	}

	private static double getTownPenaltyUpkeepCostRaw(Town town) {

		if (getUpkeepPenalty() > 0 && !town.hasUnlimitedClaims()) {
			
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

	public static double getPerOutpostUpkeepCost() {
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_OUTPOST_UPKEEP_COST);
	}

	public static double getNationUpkeep() {

		return getDouble(ConfigNodes.ECO_PRICE_NATION_UPKEEP);
	}

	public static double getNationUpkeepCost(Nation nation) {
		if (!TownySettings.isTaxingDaily())
			return 0.0;
		
		NationUpkeepCalculationEvent event = new NationUpkeepCalculationEvent(nation, getNationUpkeepCostRaw(nation));
		BukkitTools.fireEvent(event);
		return event.getUpkeep();
	}

	private static double getNationUpkeepCostRaw(Nation nation) {
		if (nation != null && nation.getCapital() != null && !nation.getCapital().hasUpkeep())
			return 0.0;

		double multiplier = 1.0;

		if (nation != null) {
			if (isNationUpkeepPerPlot()) {
				int plotCount = nation.getTowns().stream().mapToInt(town -> town.getTownBlocks().size()).sum();
				if (isNationLevelModifierAffectingNationUpkeepPerTown())
					return (getNationUpkeep() * plotCount) * nation.getNationLevel().upkeepModifier();
				else
					return (getNationUpkeep() * plotCount);
			} else if (isNationUpkeepPerTown()) {
				if (isNationLevelModifierAffectingNationUpkeepPerTown())
					return (getNationUpkeep() * nation.getTowns().size()) * nation.getNationLevel().upkeepModifier();
				else
					return (getNationUpkeep() * nation.getTowns().size());
			} else {
				multiplier = nation.getNationLevel().upkeepModifier();
			}
		}
		return getNationUpkeep() * multiplier;
	}

	private static boolean isNationLevelModifierAffectingNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN_NATIONLEVEL_MODIFIER);
	}
	
	public static boolean isNationUpkeepPerPlot() {
		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERPLOT);
	}

	public static boolean isNationUpkeepPerTown() {

		return getBoolean(ConfigNodes.ECO_PRICE_NATION_UPKEEP_PERTOWN);
	}

	public static boolean getNationDefaultPublic(){

		return getBoolean(ConfigNodes.NATION_DEF_PUBLIC);
	}

	public static String getNationDefaultBoard(){

		return getString(ConfigNodes.NATION_DEF_BOARD);
	}

	public static double getNationDefaultTax() {

		return getDouble(ConfigNodes.NATION_DEF_TAXES_TAX);
	}

	public static boolean getNationDefaultTaxPercentage() {

		return getBoolean(ConfigNodes.NATION_DEF_TAXES_TAXPERCENTAGE);
	}

	public static double getNationDefaultTaxMinimumTax() {

		return getDouble(ConfigNodes.NATION_DEF_TAXES_MINIMUMTAX);
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
	
	public static boolean isNewWorldClaimable() {
		return getBoolean(ConfigNodes.NWS_WORLD_CLAIMABLE);
	}

	public static boolean isPvP() {

		return getBoolean(ConfigNodes.NWS_WORLD_PVP);
	}

	public static boolean isForcingPvP() {

		return getBoolean(ConfigNodes.NWS_FORCE_PVP_ON);
	}

	public static boolean isCreatureTramplingCropsDisabled() {

		return getBoolean(ConfigNodes.NWS_DISABLE_CREATURE_CROP_TRAMPLING);
	}

	public static boolean isWorldMonstersOn() {

		return getBoolean(ConfigNodes.NWS_WORLD_MONSTERS_ON);
	}
	
	public static boolean isWildernessMonstersOn() {
		return getBoolean(ConfigNodes.NWS_WILDERNESS_MONSTERS_ON);
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

	public static boolean isDeletingEntitiesOnUnclaim() {
		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_ENTITY_DELETE_ENABLE);
	}
	
	public static List<String> getUnclaimDeleteEntityTypes() {
		return getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_ENTITY_DELETE);
	}
	
	public static boolean isUsingPlotManagementDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE_ENABLE);
	}

	public static Collection<Material> getPlotManagementDeleteIds() {

		return toMaterialSet(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_DELETE));
	}

	public static boolean isUsingPlotManagementMayorDelete() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE_ENABLE);
	}

	public static Collection<Material> getPlotManagementMayorDelete() {

		return toMaterialSet(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_MAYOR_DELETE));
	}

	public static boolean isUsingPlotManagementRevert() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_ENABLE);
	}

	public static long getPlotManagementSpeed() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_TIME);
	}

	public static boolean isUsingPlotManagementWildEntityRegen() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_ENABLE);
	}

	public static long getPlotManagementWildRegenDelay() {

		return getSeconds(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_MOB_REVERT_TIME);
	}
	
	public static boolean isUsingPlotManagementWildBlockRegen() {

		return getBoolean(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_BLOCK_REVERT_ENABLE);
	}

	public static Collection<Material> getPlotManagementIgnoreIds() {

		return toMaterialSet(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_IGNORE));
	}

	public static Collection<Material> getRevertOnUnclaimWhitelistMaterials() {
		return toMaterialSet(getStrArr(ConfigNodes.NWS_PLOT_MANAGEMENT_REVERT_WHITELIST));
	}

	public static boolean isTownRespawning() {

		return getBoolean(ConfigNodes.SPAWNING_TOWN_RESPAWN);
	}

	public static boolean isTownRespawningInOtherWorlds() {

		return getBoolean(ConfigNodes.SPAWNING_TOWN_RESPAWN_SAME_WORLD_ONLY);
	}
	
	public static boolean isRespawnAnchorHigherPrecedence() {
		return getBoolean(ConfigNodes.SPAWNING_RESPAWN_ANCHOR_HIGHER_PRECEDENCE);
	}
	
	public static boolean isConqueredTownsDeniedNationSpawn() {
		return getBoolean(ConfigNodes.SPAWNING_DENY_CONQUERED_TOWNS_USE_OF_NATION_SPAWN);
	}
	
	public static int getHomeBlockMovementCooldownHours() {
		return getInt(ConfigNodes.GTOWN_HOMEBLOCK_MOVEMENT_COOLDOWN);
	}
	
	public static int getHomeBlockMovementDistanceInTownBlocks() {
		return getInt(ConfigNodes.GTOWN_HOMEBLOCK_MOVEMENT_DISTANCE);
	}
	
	public static int getMaxResidentsPerTown() {
		
		return getInt(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN);
	}
	
	public static int getMaxResidentsPerTownCapitalOverride() {
		
		return Math.max(getInt(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE), getMaxResidentsPerTown());
	}
	
	public static int getMaxResidentsForTown(Town town) {
		if (town.isCapital())
			return getMaxResidentsPerTownCapitalOverride();
		else 
			return !town.hasNation()
					? town.getMaxAllowedNumberOfResidentsWithoutNation()
					: getMaxResidentsPerTown();
	}

	public static boolean isTownyUpdating(String currentVersion) {
		return Version.fromString(getLastRunVersion(currentVersion)).isOlderThan(Version.fromString(currentVersion));
	}

	public static boolean isTownyUpToDate(String currentVersion) {

		return currentVersion.equals(getLastRunVersion(currentVersion));
	}

	public static String getLastRunVersion(String currentVersion) {

		return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
	}
	
	public static String getLastRunVersion() {
		return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), "0.0.0.0");
	}

	public static void setLastRunVersion(String currentVersion) {

		setProperty(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
		config.save();
	}

	public static int getMinDistanceFromTownHomeblocks() {

		return getInt(ConfigNodes.CLAIMING_MIN_DISTANCE_FROM_TOWN_HOMEBLOCK);
	}
	
	public static int getMinDistanceForOutpostsFromPlot() {
		
		return getInt(ConfigNodes.CLAIMING_MIN_DISTANCE_FOR_OUTPOST_FROM_PLOT);
	}
	
	public static int getMaxDistanceForOutpostsFromTown() {
		return getInt(ConfigNodes.CLAIMING_MAX_DISTANCE_FOR_OUTPOST_FROM_TOWN_PLOT);
	}

	public static int getMinDistanceFromTownPlotblocks() {

		return getInt(ConfigNodes.CLAIMING_MIN_PLOT_DISTANCE_FROM_TOWN_PLOT);
	}

	public static int getMaxDistanceForTownMerge() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_MAX_DISTANCE_FOR_MERGE);
	}

	public static int getBaseCostForTownMerge() {
		return getInt(ConfigNodes.ECO_PRICE_TOWN_MERGE);
	}

	public static int getPercentageCostPerPlot() {
		return getInt(ConfigNodes.ECO_PRICE_TOWN_MERGE_PER_PLOT_PERCENTAGE);
	}
	
	public static boolean isMinDistanceIgnoringTownsInSameNation() {

		return getBoolean(ConfigNodes.CLAIMING_MIN_DISTANCE_IGNORED_FOR_NATIONS);
	}

	public static boolean isMinDistanceIgnoringTownsInAlliedNation() {
		return getBoolean(ConfigNodes.CLAIMING_MIN_DISTANCE_IGNORED_FOR_ALLIES);
	}

	public static int getMinDistanceBetweenHomeblocks() {
		return getInt(ConfigNodes.CLAIMING_MIN_DISTANCE_BETWEEN_HOMEBLOCKS);
	}
	
	public static int getMaxDistanceBetweenHomeblocks() {

		return getInt(ConfigNodes.CLAIMING_MAX_DISTANCE_BETWEEN_HOMEBLOCKS);
	}

	public static int getMaxResidentPlots(Resident resident) {

		int maxPlots = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_PLOTS.getNode());
		if (maxPlots == -1)
			maxPlots = getInt(ConfigNodes.GTOWN_SETTINGS_MAX_PLOTS_PER_RESIDENT);
		return maxPlots;
	}
	
	public static int getMaxResidentExtraPlots(Resident resident) {

		int extraPlots = TownyUniverse.getInstance().getPermissionSource().getPlayerPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_EXTRA_PLOTS.getNode());
		if (extraPlots == -1)
			extraPlots = 0;
		return extraPlots;
	}

	public static int getMaxResidentOutposts(Resident resident) {

		return TownyUniverse.getInstance().getPermissionSource()
			.getGroupPermissionIntNode(resident.getName(), PermissionNodes.TOWNY_MAX_OUTPOSTS.getNode());
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
			return switch (type) {
				case BUILD -> getPermFlag_Resident_Friend_Build();
				case DESTROY -> getPermFlag_Resident_Friend_Destroy();
				case SWITCH -> getPermFlag_Resident_Friend_Switch();
				case ITEM_USE -> getPermFlag_Resident_Friend_ItemUse();
			};
		else if (owner instanceof Town)
			return switch (type) {
				case BUILD -> getPermFlag_Town_Resident_Build();
				case DESTROY -> getPermFlag_Town_Resident_Destroy();
				case SWITCH -> getPermFlag_Town_Resident_Switch();
				case ITEM_USE -> getPermFlag_Town_Resident_ItemUse();
			};
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultNationPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			return switch (type) {
				case BUILD -> getPermFlag_Resident_Town_Build();
				case DESTROY -> getPermFlag_Resident_Town_Destroy();
				case SWITCH -> getPermFlag_Resident_Town_Switch();
				case ITEM_USE -> getPermFlag_Resident_Town_ItemUse();
			};
		else if (owner instanceof Town)
			return switch (type) {
				case BUILD -> getPermFlag_Town_Nation_Build();
				case DESTROY -> getPermFlag_Town_Nation_Destroy();
				case SWITCH -> getPermFlag_Town_Nation_Switch();
				case ITEM_USE -> getPermFlag_Town_Nation_ItemUse();
			};
		else
			throw new UnsupportedOperationException();
	}
	
	public static boolean getDefaultAllyPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			return switch (type) {
				case BUILD -> getPermFlag_Resident_Ally_Build();
				case DESTROY -> getPermFlag_Resident_Ally_Destroy();
				case SWITCH -> getPermFlag_Resident_Ally_Switch();
				case ITEM_USE -> getPermFlag_Resident_Ally_ItemUse();
			};
		else if (owner instanceof Town)
			return switch (type) {
				case BUILD -> getPermFlag_Town_Ally_Build();
				case DESTROY -> getPermFlag_Town_Ally_Destroy();
				case SWITCH -> getPermFlag_Town_Ally_Switch();
				case ITEM_USE -> getPermFlag_Town_Ally_ItemUse();
			};
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultOutsiderPermission(TownBlockOwner owner, ActionType type) {

		if (owner instanceof Resident)
			return switch (type) {
				case BUILD -> getPermFlag_Resident_Outsider_Build();
				case DESTROY -> getPermFlag_Resident_Outsider_Destroy();
				case SWITCH -> getPermFlag_Resident_Outsider_Switch();
				case ITEM_USE -> getPermFlag_Resident_Outsider_ItemUse();
			};
		else if (owner instanceof Town)
			return switch (type) {
				case BUILD -> getPermFlag_Town_Outsider_Build();
				case DESTROY -> getPermFlag_Town_Outsider_Destroy();
				case SWITCH -> getPermFlag_Town_Outsider_Switch();
				case ITEM_USE -> getPermFlag_Town_Outsider_ItemUse();
			};
		else
			throw new UnsupportedOperationException();
	}

	public static boolean getDefaultPermission(TownBlockOwner owner, PermLevel level, ActionType type) {

		return switch (level) {
			case RESIDENT -> getDefaultResidentPermission(owner, type);
			case NATION -> getDefaultNationPermission(owner, type);
			case ALLY -> getDefaultAllyPermission(owner, type);
			case OUTSIDER -> getDefaultOutsiderPermission(owner, type);
		};
	}

	public static String getAcceptCommand() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_ACCEPT_COMMAND) : ConfigNodes.INVITE_SYSTEM_ACCEPT_COMMAND.getDefault();
	}

	public static String getDenyCommand() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_DENY_COMMAND) : ConfigNodes.INVITE_SYSTEM_DENY_COMMAND.getDefault();
	}

	public static String getConfirmCommand() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_CONFIRM_COMMAND) : ConfigNodes.INVITE_SYSTEM_CONFIRM_COMMAND.getDefault();
	}

	public static String getCancelCommand() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_CANCEL_COMMAND) : ConfigNodes.INVITE_SYSTEM_CANCEL_COMMAND.getDefault();
	}

	public static String getConfirmationCommandFormat() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_CONFIRMATION_FORMAT) : ConfigNodes.INVITE_SYSTEM_CONFIRMATION_FORMAT.getDefault();
	}

	public static String getConfirmationCommandYesColour() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_CONFIRMATION_YES_COLOUR) : ConfigNodes.INVITE_SYSTEM_CONFIRMATION_YES_COLOUR.getDefault();
	}

	public static String getConfirmationCommandNoColour() {
		return config != null ? getString(ConfigNodes.INVITE_SYSTEM_CONFIRMATION_NO_COLOUR) : ConfigNodes.INVITE_SYSTEM_CONFIRMATION_NO_COLOUR.getDefault();
	}

	public static boolean getOutsidersPreventPVPToggle() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OUTSIDERS_PREVENT_PVP_TOGGLE);
	}
	
	public static boolean getOutsidersUnclaimingTownBlocks() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OUTSIDERS_PREVENT_UNCLAIM_TOWNBLOCK);
	}
	
	public static boolean isForcePvpNotAffectingHomeblocks() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_HOMEBLOCKS_PREVENT_FORCEPVP);
	}

	public static boolean isPVPAlwaysAllowedForAdmins() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ADMINS_CAN_ALWAYS_PVP);
	}

	public static int getConfirmationTimeoutSeconds() {
		return getInt(ConfigNodes.INVITE_SYSTEM_CONFIRMATION_TIMEOUT);
	}
	
	public static long getTownInviteCooldown() {

		return getSeconds(ConfigNodes.INVITE_SYSTEM_COOLDOWN_TIME);
	}
	
	public static long getInviteExpirationTime() {
		
		return getSeconds(ConfigNodes.INVITE_SYSTEM_EXPIRATION_TIME);
	}

	public static boolean isAppendingToLog() {

		return !getBoolean(ConfigNodes.PLUGIN_RESET_LOG_ON_BOOT);
	}
	
	public static int getTownyTopSize() {
		return getInt(ConfigNodes.PLUGIN_TOWNY_TOP_SIZE);
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

		return getInt(ConfigNodes.SPAWNING_WARMUP_TIMER);
	}
	
	public static boolean isMovementCancellingSpawnWarmup() {

		return getBoolean(ConfigNodes.SPAWNING_MOVEMENT_CANCELS_SPAWN_WARMUP);
	}
	
	public static boolean isDamageCancellingSpawnWarmup() {
		
		return getBoolean(ConfigNodes.SPAWNING_DAMAGE_CANCELS_SPAWN_WARMUP);
	}
	
	public static boolean isTeleportWarmupUsingTitleMessage() {
		return getBoolean(ConfigNodes.SPAWNING_WARMUP_USES_TITLE_MESSAGE);
	}
	
	public static boolean isTeleportWarmupShowingParticleEffect() {
		return getBoolean(ConfigNodes.SPAWNING_WARMUP_SHOWS_PARTICLE);
	}
	
	public static int getSpawnCooldownTime() {
		
		return getInt(ConfigNodes.SPAWNING_TOWN_SPAWN_COOLDOWN_TIMER);
	}
	
	public static int getPVPCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_PVP_COOLDOWN_TIMER);
	}

	public static int getPeacefulCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_NEUTRAL_COOLDOWN_TIMER);
	}
	
	public static int getTownDeleteCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_TOWN_DELETE_COOLDOWN_TIMER);
	}
	
	public static int getTownUnclaimCoolDownTime() {

		return getInt(ConfigNodes.GTOWN_SETTINGS_TOWN_UNCLAIM_COOLDOWN_TIMER);
	}
	
	public static String getTownAccountPrefix() {

		return getString(ConfigNodes.ECO_TOWN_PREFIX);
	}

	public static String getNationAccountPrefix() {

		return getString(ConfigNodes.ECO_NATION_PREFIX);
	}

	public static double getTownBankCap(Town town) {
		return town.getTownLevel().bankCapModifier * getTownBankCap(); 
	}

	public static double getTownBankCap() {

		return getDouble(ConfigNodes.ECO_BANK_CAP_TOWN);
	}

	public static int getTownMinDeposit() {

		return getInt(ConfigNodes.ECO_MIN_DEPOSIT_TOWN);
	}

	public static int getTownMinWithdraw() {

		return getInt(ConfigNodes.ECO_MIN_WITHDRAW_TOWN);
	}

	public static double getNationBankCap(Nation nation) {
		return nation.getNationLevel().bankCapModifier * getNationBankCap();
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

	public static boolean getNationBankAllowWithdrawls() {

		return getBoolean(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS);
	}

	public static int getNationMinDeposit() {

		return getInt(ConfigNodes.ECO_MIN_DEPOSIT_NATION);
	}

	public static int getNationMinWithdraw() {

		return getInt(ConfigNodes.ECO_MIN_WITHDRAW_NATION);
	}
	
	public static boolean isBankActionDisallowedOutsideTown() {
		
		return getBoolean(ConfigNodes.ECO_BANK_DISALLOW_BANK_ACTIONS_OUTSIDE_TOWN);
	}
	
	public static boolean isBankActionLimitedToBankPlots() {
		
		return getBoolean(ConfigNodes.BANK_IS_LIMTED_TO_BANK_PLOTS);
	}

	public static boolean doHomeblocksNoLongerWorkWhenATownHasBankPlots() {
		return getBoolean(ConfigNodes.BANK_BANK_PLOTS_STOP_HOME_BLOCK_BEING_USED);
	}

	public static void SetNationBankAllowWithdrawls(boolean newSetting) {

		setProperty(ConfigNodes.ECO_BANK_NATION_ALLOW_WITHDRAWALS.getRoot(), newSetting);
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
	
	/**
	 * @deprecated since 0.100.0.9, use {@link #getNationProximityToCapital()} instead.
	 * @return getNationProximityToCapital()
	 */
	@Deprecated
	public static double getNationRequiresProximity() {
		return getNationProximityToCapital();
	}

	public static double getNationProximityToCapital() {
		return getDouble(ConfigNodes.GNATION_SETTINGS_NATION_PROXIMITY_TO_CAPITAL);
	}

	public static double getNationProximityToOtherNationTowns() {
		return getDouble(ConfigNodes.GNATION_SETTINGS_NATION_PROXIMITY_TO_OTHER_NATION_TOWNS);
	}

	public static double getNationProximityAbsoluteMaximum() {
		return getDouble(ConfigNodes.GNATION_SETTINGS_NATION_PROXIMITY_TO_CAPITAL_CAP);
	}

	public static List<String> getFarmAnimals() {
		return getStrArr(ConfigNodes.GTOWN_FARM_ANIMALS);
	}

	public static boolean getKeepInventoryInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_TOWN);
	}
	
	public static boolean getKeepInventoryInOwnTown() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_OWN_TOWN);
	}
	
	public static boolean getKeepInventoryInAlliedTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_ALLIED_TOWN);
	}
	
	public static boolean getKeepExperienceInArenas() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_EXPERIENCE_ON_DEATH_IN_ARENA);
	}
	
	public static boolean arenaPlotPreventArmourDegrade() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_PREVENT_ITEM_DEGRADE_IN_ARENAS);
	}

	public static boolean getKeepInventoryInArenas() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_INVENTORY_ON_DEATH_IN_ARENA);
	}

	public static boolean getKeepExperienceInTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_KEEP_EXPERIENCE_ON_DEATH_IN_TOWN);
	}

	public static boolean isNotificationUsingTitles() {
		return getBoolean(ConfigNodes.NOTIFICATION_USING_TITLES);
	}

	public static int getAmountOfResidentsForOutpost() {
		return getInt(ConfigNodes.CLAIMING_MINIMUM_AMOUNT_RESIDENTS_FOR_OUTPOSTS);
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
	
	public static boolean getNationZonesSkipConqueredTowns() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_SKIPS_CONQUERED_TOWNS);
	}
	
	public static boolean getNationZonesProtectsConqueredTowns() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_NATIONZONE_PROTECTS_CONQUERED_TOWNS);
	}
	
	public static int getNationZonesCapitalBonusSize() {
		return getInt(ConfigNodes.GNATION_SETTINGS_NATIONZONE_CAPITAL_BONUS_SIZE);
	}
	
	public static boolean isNationSpawnOnlyAllowedInCapital() { 
		return getBoolean(ConfigNodes.SPAWNING_FORCE_NATION_SPAWN_IN_CAPITAL);
	}
	
	public static int getMaxTownsPerNation() {
		return getInt(ConfigNodes.GNATION_SETTINGS_MAX_TOWNS_PER_NATION);
	}

    public static int getMaxResidentsPerNation() {
        return getInt(ConfigNodes.GNATION_SETTINGS_MAX_RESIDENTS_PER_NATION);
    }
	
	public static double getSpawnTravelCost() {
		return getDouble(ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC);
	}
	
	public static boolean isPublicSpawnCostAffectedByTownSpawncost() {
		return getBoolean(ConfigNodes.ECO_PRICE_ALLOW_MAYORS_TO_OVERRIDE_PUBLIC_SPAWN_COST);
	}
	
	public static boolean isAllySpawningRequiringPublicStatus() {
		return getBoolean(ConfigNodes.SPAWNING_IS_ALLY_TOWN_SPAWNING_REQUIRING_PUBLIC_STATUS);
	}
	
	public static boolean trustedResidentsGetToSpawnToTown() {
		return getBoolean(ConfigNodes.SPAWNING_SETTINGS_IS_TRUSTED_RESIDENTS_COUNT_AS_RESIDENTS);
	}
	
	public static boolean isPromptingNewResidentsToTownSpawn() {
		return getBoolean(ConfigNodes.SPAWNING_IS_NEW_RESIDENT_PROMPTED_TO_SPAWN);
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

	public static String getNotificationTitlesNationNameFormat() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_NATIONNAME_FORMAT);
	}
	
	public static String getNotificationTitlesNationCapitalFormat() {
		return getString(ConfigNodes.NOTIFICATION_TITLES_NATIONCAPITAL_FORMAT);
	}
	
	public static int getNotificationTitlesDurationTicks() {
		return getInt(ConfigNodes.NOTIFICATION_TITLES_DURATION);
	}

	public static double getTownRenameCost() {
		return getDouble(ConfigNodes.ECO_TOWN_RENAME_COST);
	}

	public static double getNationRenameCost() {
		return getDouble(ConfigNodes.ECO_NATION_RENAME_COST);
	}

	public static double getTownSetMapColourCost() {
		return getDouble(ConfigNodes.ECO_TOWN_MAPCOLOUR_COST);
	}

	public static double getNationSetMapColourCost() {
		return getDouble(ConfigNodes.ECO_NATION_MAPCOLOUR_COST);
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

	public static String getPAPILeaderboardFormat() {
		return getString(ConfigNodes.FILTERS_PAPI_LEADERBOARD_FORMATTING);
	}

	public static String getPAPIRelationNone() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_NONE);
	}
	
	public static String getPAPIRelationNoTown() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_NO_TOWN);
	}
	
	public static String getPAPIRelationSameTown() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_SAME_TOWN);
	}
	
	public static String getPAPIRelationSameNation() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_SAME_NATION);
	}
	
	public static String getPAPIRelationConqueredTown() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_CONQUERED_TOWN);
	}
	
	public static String getPAPIRelationAlly() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_ALLY);
	}
	
	public static String getPAPIRelationEnemy() {
		return getString(ConfigNodes.FILTERS_PAPI_REL_FORMATTING_ENEMY);
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

	public static List<String> getOrderOfMayoralSuccession() {
		return getStrArr(ConfigNodes.GTOWN_ORDER_OF_MAYORAL_SUCCESSION);
	}

	public static boolean isWarAllowed() {
		return getBoolean(ConfigNodes.NWS_WAR_ALLOWED);
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
	
	public static Map<String,String> getTownColorsMap() {
		List<String> townColorsList = getStrArr(ConfigNodes.GTOWN_SETTINGS_ALLOWED_TOWN_COLORS);
		Map<String,String> townColorsMap = new HashMap<>();
		String[] keyValuePair;
		for(String nationColor: townColorsList) {
			keyValuePair = nationColor.trim().split(":");
			townColorsMap.put(keyValuePair[0], keyValuePair[1]);
		}
		return townColorsMap;
	}

	public static String getDefaultNationMapColor() {
		String colorName = getString(ConfigNodes.NATION_DEF_MAP_COLOR);
		if (colorName.isEmpty() || !getNationColorsMap().containsKey(colorName))
			return MapUtil.generateRandomNationColourAsHexCode();
		return getNationColorsMap().get(colorName);
	}

	public static String getDefaultTownMapColor() {
		String colorName = getString(ConfigNodes.TOWN_DEF_MAP_COLOR);
		if (colorName.isEmpty() || !getTownColorsMap().containsKey(colorName))
			return MapUtil.generateRandomTownColourAsHexCode();
		return getTownColorsMap().get(colorName);
	}

	public static boolean isTownBankruptcyEnabled() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_ENABLED);
	}

	public static double getDebtCapMaximum() {
		return getDouble(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_MAXIMUM);
	}
	
	public static double getDebtCapOverride() {
		return getDouble(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_OVERRIDE);
	}
	
	public static boolean isDebtCapDeterminedByTownLevel() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_USES_TOWN_LEVELS);
	}

	public static boolean isDebtCapAFixedNumberOfDays() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_USES_FIXED_DAYS);
	}

	public static int getDebtCapFixedDays() {
		return getInt(ConfigNodes.ECO_BANKRUPTCY_DEBT_CAP_ALLOWED_DAYS);
	}

	public static boolean isUpkeepDeletingTownsThatReachDebtCap() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_UPKEEP_DELETE_TOWNS_THAT_REACH_DEBT_CAP);
	}
	
	public static boolean canBankruptTownsPayForNeutrality() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_NEUTRALITY_CAN_BANKRUPT_TOWNS_PAY_NEUTRALITY);
	}
	
	public static boolean isNationTaxKickingTownsThatReachDebtCap() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_NATION_KICKS_TOWNS_THAT_REACH_DEBT_CAP);
	}
	
	public static boolean doesNationTaxDeleteConqueredTownsWhichCannotPay() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DOES_NATION_TAX_DELETE_CONQUERED_TOWNS);
	}

	public static boolean doBankruptTownsPayNationTax() {
		return getBoolean(ConfigNodes.ECO_BANKRUPTCY_DO_BANKRUPT_TOWNS_PAY_NATION_TAX);
	}

	public static boolean forcePVPForTownOutlaws() {
		return getBoolean(ConfigNodes.GTOWN_SETTING_FORCE_PVP_ON_OUTLAWS);
	}

	public static boolean outlawsAlwaysAllowedToPVP() {
		return getBoolean(ConfigNodes.GTOWN_SETTING_ALLOW_OUTLAWS_TO_ALWAYS_PVP);
	}

	public static boolean canOutlawsEnterTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTLAWS_TO_ENTER_TOWN);
	}

	public static boolean canOutlawsTeleportOutOfTowns() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTLAWS_TO_TELEPORT_OUT_OF_TOWN);
	}

	public static boolean canOutlawsUseTeleportItems() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTLAWS_USE_TELEPORT_ITEMS);
	}
	
	public static boolean areNewOutlawsTeleportedAway() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_OUTLAW_TELEPORT_ON_BECOMING_OUTLAWED);
	}
	
	public static int getOutlawTeleportWarmup() {
		return getInt(ConfigNodes.GTOWN_SETTINGS_OUTLAW_TELEPORT_WARMUP);
	}
	
	public static String getOutlawTeleportWorld() { 
		return getString(ConfigNodes.GTOWN_SETTINGS_OUTLAW_TELEPORT_WORLD); 
	}
	public static boolean doTownsGetWarnedOnOutlaw() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_WARN_TOWN_ON_OUTLAW);
	}
	
	public static int getOutlawWarningMessageCooldown() {
		return getInt(ConfigNodes.GTOWN_SETTING_WARN_TOWN_ON_OUTLAW_MESSAGE_COOLDOWN_TIME);
	}

	public static List<String> getOutlawBlacklistedCommands() {
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_OUTLAW_BLACKLISTED_COMMANDS);
	}

	public static List<String> getWarBlacklistedCommands() {
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_WAR_BLACKLISTED_COMMANDS);
	}

	public static boolean getVisualizedSpawnPointsEnabled() {
		return getBoolean(ConfigNodes.SPAWNING_VISUALIZED_SPAWN_POINTS_ENABLED);
	}

	public static List<String> getBlacklistedNames() {
		return getStrArr(ConfigNodes.PLUGIN_NAME_BLACKLIST);
	}
	
	public static boolean doesFrostWalkerRequireBuildPerms() {
		return getBoolean(ConfigNodes.PROT_FROST_WALKER);
	}
	
	public static boolean isPlayerCropTramplePrevented() {
		return getBoolean(ConfigNodes.PROT_CROP_TRAMPLE);
	}

	public static boolean isNotificationsAppearingOnBossbar() {
		return getString(ConfigNodes.NOTIFICATION_NOTIFICATIONS_APPEAR_AS).equalsIgnoreCase("bossbar");
	}
	
	public static boolean allowTownCommandBlacklisting() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ENABLE_COMMAND_BLACKLISTING);
	}
	
	public static List<String> getTownBlacklistedCommands() {
		return getStrArr(ConfigNodes.GTOWN_TOWN_BLACKLISTED_COMMANDS);
	}
	
	public static List<String> getPlayerOwnedPlotLimitedCommands() {
		return getStrArr(ConfigNodes.GTOWN_TOWN_LIMITED_COMMANDS);
	}

	public static boolean getPreventFluidGriefingEnabled() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_PREVENT_FLUID_GRIEFING);
	}

	public static int getMaxTagLength() {
		return getInt(ConfigNodes.FILTERS_MAX_TAG_LENGTH);
	}
	
	public static boolean getTownAutomaticCapitalisationEnabled(){
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_AUTOMATIC_CAPITALISATION);
	}
	
	public static List<String> getTouristBlockedCommands() {
		return getStrArr(ConfigNodes.GTOWN_TOWN_TOURIST_BLOCKED_COMMANDS);
	}

	public static boolean doTrustedResidentsBypassTownBlockedCommands() {
		return getBoolean(ConfigNodes.GTOWN_TOWN_TOURIST_BLOCKED_COMMANDS_TRUSTED_BYPASS);
	}

	public static boolean doAlliesBypassTownBlockedCommands() {
		return getBoolean(ConfigNodes.GTOWN_TOWN_TOURIST_BLOCKED_COMMANDS_ALLIES_BYPASS);
	}

	public static boolean isContextsEnabled() {
		return getBoolean(ConfigNodes.PLUGIN_LUCKPERMS_CONTEXTS);
	}
	
	public static boolean isShowingUpdateNotifications() {
		return getBoolean(ConfigNodes.PLUGIN_UPDATE_NOTIFICATIONS_ALERTS);
	}
	
	public static boolean isUpdateNotificationsMajorOnly() {
		return getBoolean(ConfigNodes.PLUGIN_UPDATE_NOTIFICATIONS_MAJOR_ONLY);
	}
	
	public static int getMaxNationAllies() {
		return getInt(ConfigNodes.GNATION_SETTINGS_MAX_ALLIES);
	}
	
	public static boolean areConqueredTownsConsideredAllied() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_ARE_CONQUERED_TOWNS_CONSIDERED_ALLIES);
	}

	public static boolean areConqueredTownsGivenNationPlotPerms() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_ARE_CONQUERED_TOWNS_GIVEN_NATION_PLOT_PERMS);
	}
	
	public static String getBankHistoryBookFormat() {
		return getString(ConfigNodes.BANKHISTORY_BOOK);
	}
	
	public static boolean getTownRuinsEnabled() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_ENABLED);
	}

	public static int getTownRuinsMaxDurationHours() {
		return Math.min(getInt(ConfigNodes.TOWN_RUINING_TOWN_RUINS_MAX_DURATION_HOURS), 8760);
	}

	public static int getTownRuinsMinDurationHours() {
		return getInt(ConfigNodes.TOWN_RUINING_TOWN_RUINS_MIN_DURATION_HOURS);
	}

	public static boolean getTownRuinsReclaimEnabled() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_RECLAIM_ENABLED);
	}

	public static double getEcoPriceReclaimTown() {
		return getDouble(ConfigNodes.ECO_PRICE_RECLAIM_RUINED_TOWN);
	}
	
	public static boolean areRuinsMadePublic() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWNS_BECOME_PUBLIC);
	}

	public static boolean areRuinsMadeOpen() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWNS_BECOME_OPEN);
	}

	public static boolean areRuinedTownsBanksPaidToNation() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWN_DEPOSITS_BANK_TO_NATION);
	}

	public static boolean doRuinsPlotPermissionsProgressivelyAllowAll() {
		return getBoolean(ConfigNodes.TOWN_RUINING_TOWN_PLOTS_PERMISSIONS_OPEN_UP_PROGRESSIVELY);
	}

	public static void saveConfig() {
		config.save();
	}

	public static long getSpawnProtectionDuration() {
		return TimeTools.getTicks(getString(ConfigNodes.SPAWNING_RESPAWN_PROTECTION_TIME));
	}
	
	public static boolean isUsingWebMapStatusScreens() {
		return getBoolean(ConfigNodes.PLUGIN_WEB_MAP_USING_STATUSSCREEN);
	}

	public static boolean isWebMapLinkShownForNonPublicTowns() {
		return getBoolean(ConfigNodes.PLUGIN_WEB_MAP_SHOW_LINK_FOR_NONPUBLIC_TOWNS);
	}

	public static boolean isUsingWorldKeyForWorldName() {
		return getBoolean(ConfigNodes.PLUGIN_WEB_MAP_WORLD_NAME_USES_KEY);
	}
	
	public static String getWebMapUrl() {
		return getString(ConfigNodes.PLUGIN_WEB_MAP_URL);
	}
	
	public static boolean isSafeTeleportUsed() { 
		return getBoolean(ConfigNodes.SPAWNING_SAFE_TELEPORT);
	}
	
	public static boolean isStrictSafeTeleportUsed() { 
		return getBoolean(ConfigNodes.SPAWNING_STRICT_SAFE_TELEPORT);
	}
	
	public static Map<Integer, TownLevel> getConfigTownLevel() {
		return configTownLevel;
	}
	
	public static Map<Integer, NationLevel> getConfigNationLevel() {
		return configNationLevel;
	}
	
	public static boolean isLanguageEnabled(@NotNull String locale) {
		// Either all languages are enabled or, we auto-enable English: Addons that only
		// have english translations and/or are missing a translation for the enabled
		// language(s) on this server need to be able to inject their english
		// tranlations.
		locale = locale.replace("-", "_");
		if (getString(ConfigNodes.ENABLED_LANGUAGES).equals("*") || locale.equalsIgnoreCase("en_us"))
			return true;
		
		String defaultLocale = getString(ConfigNodes.LANGUAGE);
		if (defaultLocale.isEmpty())
			defaultLocale = ConfigNodes.LANGUAGE.getDefault();
		
		if (locale.equalsIgnoreCase(defaultLocale))
			return true;

		List<String> enabledLanguages = new ArrayList<>();
		for (String string : getStrArr(ConfigNodes.ENABLED_LANGUAGES))
			enabledLanguages.add(string.toLowerCase(Locale.ROOT).replaceAll("-", "_").replaceAll(".yml", ""));

		return enabledLanguages.contains(locale.toLowerCase(Locale.ROOT));
	}
	
	public static boolean doMayorsPayTownTax() {
		return getBoolean(ConfigNodes.ECO_DAILY_TAXES_DO_MAYORS_PAY_TOWN_TAX);
	}
	
	public static boolean doCapitalsPayNationTax() {
		return getBoolean(ConfigNodes.ECO_DAILY_TAXES_DO_CAPITALS_PAY_NATION_TAX);
	}
	
	public static boolean isContextEnabled(String id) {
		if (getString(ConfigNodes.PLUGIN_ENABLED_CONTEXTS).equals("*"))
			return true;
		
		return getStrArr(ConfigNodes.PLUGIN_ENABLED_CONTEXTS).contains(id);
	}
	
	public static boolean getRespawnProtectionAllowPickup() {
		return getBoolean(ConfigNodes.SPAWNING_RESPAWN_PROTECTION_ALLOW_PICKUP);
	}
	
	public static String getNotificationsAppearAs() {
		return getString(ConfigNodes.NOTIFICATION_NOTIFICATIONS_APPEAR_AS);
	}
	
	public static boolean areNumbersAllowedInTownNames() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_NUMBERS_IN_TOWN_NAME);
	}
	
	public static boolean areNumbersAllowedInNationNames() {
		return getBoolean(ConfigNodes.GNATION_SETTINGS_ALLOW_NUMBERS_IN_NATION_NAME);
	}
	
	@ApiStatus.Internal
	public static boolean areLevelTypeLimitsConfigured() {
		return areLevelTypeLimitsConfigured;
	}
	
	public static String homeBlockMapSymbol() {
		return getString(ConfigNodes.ASCII_MAP_SYMBOLS_HOME);
	}
	
	public static String outpostMapSymbol() {
		return getString(ConfigNodes.ASCII_MAP_SYMBOLS_OUTPOST);
	}
	
	public static String forSaleMapSymbol() {
		return getString(ConfigNodes.ASCII_MAP_SYMBOLS_FORSALE);
	}

	public static String wildernessMapSymbol() {
		return getString(ConfigNodes.ASCII_MAP_SYMBOLS_WILDERNESS);
	}

	public static int asciiMapHeight() {
		return getInt(ConfigNodes.ASCII_MAP_HEIGHT);
	}

	public static int asciiMapWidth() {
		return getInt(ConfigNodes.ASCII_MAP_WIDTH);
	}

	public static void addReloadListener(NamespacedKey key, @NotNull Consumer<CommentedConfiguration> consumer) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		
		CONFIG_RELOAD_LISTENERS.putIfAbsent(key, consumer);
	}
	
	public static void addReloadListener(NamespacedKey key, @NotNull Runnable runnable) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");

		CONFIG_RELOAD_LISTENERS.putIfAbsent(key, config -> runnable.run());
	}
	
	public static void removeReloadListener(@NotNull NamespacedKey key) {
		CONFIG_RELOAD_LISTENERS.remove(key);
	}
	
	public static List<String> getTownUnkickableRanks() {
		return getStrArr(ConfigNodes.GTOWN_SETTINGS_UNKICKABLE_RANKS);
	}
	
	public static boolean doTrustedPlayersGetPermsOnPersonallyOwnedLand() {
		return getBoolean(ConfigNodes.GTOWN_SETTINGS_DO_TRUSTED_PLAYERS_GET_PERMS_ON_PERSONALLY_OWNED_LAND);
	}

	public static boolean areProtectedEntitiesProtectedAgainstMobs() {
		return getBoolean(ConfigNodes.PROT_MOB_TYPES_MOB_VS_MOB_BYPASS);
	}
	
	public static String getBossBarNotificationColor() {
		return getString(ConfigNodes.NOTIFICATION_BOSSBARS_COLOR);
	}
	
	public static String getBossBarNotificationOverlay() {
		return getString(ConfigNodes.NOTIFICATION_BOSSBARS_OVERLAY);
	}
	
	public static float getBossBarNotificationProgress() {
		return (float) getDouble(ConfigNodes.NOTIFICATION_BOSSBARS_PROGRESS);
	}
	
	public static int getNewTownMinDistanceFromTownPlots() {
		return getInt(ConfigNodes.CLAIMING_NEW_TOWN_MIN_DISTANCE_FROM_TOWN_PLOT);
	}
	
	public static int getNewTownMinDistanceFromTownHomeblocks() {
		return getInt(ConfigNodes.CLAIMING_NEW_TOWN_MIN_DISTANCE_FROM_TOWN_HOMEBLOCK);
	}
	
	public static int getMinAdjacentBlocks() {
		return Math.min(3, getInt(ConfigNodes.CLAIMING_MIN_ADJACENT_BLOCKS));
	}
	
	public static boolean isDeletingOldResidentsRemovingTownOnly() {
		return getBoolean(ConfigNodes.RES_SETTINGS_DELETE_OLD_RESIDENTS_REMOVE_TOWN_ONLY);
	}
	
	public static boolean disableMySQLBackupWarning() {
		return DatabaseConfig.getBoolean(DatabaseConfig.DATABASE_SQL_DISABLE_BACKUP_WARNING);
	}

	public static boolean isTownyPreventingProtectedMobsEnteringBoatsInTown() {
		return getBoolean(ConfigNodes.PROT_MOB_TYPES_BOAT_THEFT);
	}
	
	public static boolean coreProtectSupport() {
		return getBoolean(ConfigNodes.PLUGIN_COREPROTECT_SUPPORT);
	}

	public static String getDefaultResidentAbout() {
		return getString(ConfigNodes.RES_SETTING_DEFAULT_ABOUT);
	}

	public static long getResidentMinTimeToJoinTown() {
		return TimeTools.getMillis(getString(ConfigNodes.RES_SETTING_MIN_TIME_TO_JOIN_TOWN));
	}

	public static double maxBuyTownPrice() {
		return getDouble(ConfigNodes.GTOWN_SETTINGS_MAX_BUYTOWN_PRICE);
	}

	public static boolean isWorldJailingEnabled() {
		return getBoolean(ConfigNodes.NWS_JAILING_ENABLE);
	}
}

