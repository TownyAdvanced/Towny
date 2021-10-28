/**
 * WarZone Configuration Checks
 */
package com.palmergames.bukkit.towny.war.eventwar.settings;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.Material;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EventWarSettings {
	private static CommentedConfiguration config, newConfig;
	private static Set<Material> editableMaterialsInWarZone = null;

	public static void loadConfig(Path configPath, String version) {
		if (!FileMgmt.checkOrCreateFile(configPath.toString())) {
			throw new TownyInitException("Failed to touch '" + configPath + "'.", TownyInitException.TownyError.WAR_CONFIG);
		}

		// read the config.yml into memory
		config = new CommentedConfiguration(configPath);
		if (!config.load()) {
			throw new TownyInitException("Failed to load Towny's warconfig.yml.", TownyInitException.TownyError.WAR_CONFIG);
		}

		setDefaults(version, configPath);

		config.save();
	}
	
	public static void addComment(String root, String... comments) {
		newConfig.addComment(root.toLowerCase(), comments);
	}
	
	public static void setProperty(String root, Object value) {
		config.set(root.toLowerCase(), value.toString());
	}
	
	private static void setNewProperty(String root, Object value) {
		if (value == null)
			value = "";
		newConfig.set(root.toLowerCase(), value.toString());
	}
	
	/**
	 * Builds a new config reading old config data.
	 */
	private static void setDefaults(String version, Path configPath) {
		newConfig = new CommentedConfiguration(configPath);
		newConfig.load();

		for (EventWarConfigNodes root : EventWarConfigNodes.values()) {
			if (root.getComments().length > 0)
				addComment(root.getRoot(), root.getComments());

			setNewProperty(root.getRoot(), (config.get(root.getRoot().toLowerCase()) != null) ? config.get(root.getRoot().toLowerCase()) : root.getDefault());

		}

		config = newConfig;
		newConfig = null;
	}

	public static boolean getBoolean(EventWarConfigNodes node) {
		return Boolean.parseBoolean(config.getString(node.getRoot().toLowerCase(), node.getDefault()));
	}

	public static double getDouble(EventWarConfigNodes node) {
		try {
			return Double.parseDouble(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			Towny.getPlugin().getLogger().warning("Error could not read " + node.getRoot().toLowerCase() + " from warconfig.yml");
			return 0.0;
		}
	}

	public static int getInt(EventWarConfigNodes node) {
		try {
			return Integer.parseInt(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			Towny.getPlugin().getLogger().warning("Error could not read " + node.getRoot().toLowerCase() + " from warconfig.yml");
			return 0;
		}
	}

	public static String getString(EventWarConfigNodes node) {
		return config.getString(node.getRoot().toLowerCase(), node.getDefault());
	}
	
	private static List<String> getStrArr(EventWarConfigNodes node) {
//		String[] arr = getString(node).split(",");
		return Arrays.stream(getString(node).split(",")).collect(Collectors.toList());
	}
	
	/*
	 * Editable Materials
	 */

	public static void loadWarMaterialsLists() {
		// Load allowed blocks in warzone.
		EventWarSettings.setEditableMaterialsInWarZone(getAllowedMaterials(EventWarConfigNodes.WAR_WARZONE_EDITABLE_MATERIALS));
	}
	
	private static Set<Material> getAllowedMaterials(EventWarConfigNodes node) {
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
	
	private static void setEditableMaterialsInWarZone(Set<Material> editableMaterialsInWarZone) {
		EventWarSettings.editableMaterialsInWarZone = editableMaterialsInWarZone;
	}

	public static boolean isEditableMaterialInWarZone(Material material) {
		return editableMaterialsInWarZone.contains(material);
	}
	
	/*
	 * Towny Actions/Environmental.
	 */

	public static boolean isAllowingSwitchesInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_SWITCH);
	}

	public static boolean isAllowingFireInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_FIRE);
	}

	public static boolean isAllowingItemUseInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_ITEM_USE);
	}

	public static boolean isAllowingExplosionsInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_EXPLOSIONS);
	}

	public static boolean explosionsBreakBlocksInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_EXPLOSIONS_BREAK_BLOCKS);
	}

	public static boolean regenBlocksAfterExplosionInWarZone() {
		return getBoolean(EventWarConfigNodes.WAR_WARZONE_EXPLOSIONS_REGEN_BLOCKS);
	}

	public static List<String> getExplosionsIgnoreList() {
		return getStrArr(EventWarConfigNodes.WAR_WARZONE_EXPLOSIONS_IGNORE_LIST);
	}
	
	/*
	 * Points for scoring kills/townblocks/towns/nations.
	 */
	
	public static int getWarPointsForTownBlock() {
		return getInt(EventWarConfigNodes.WAR_EVENT_POINTS_TOWNBLOCK);
	}

	public static int getWarPointsForTown() {
		return getInt(EventWarConfigNodes.WAR_EVENT_POINTS_TOWN);
	}

	public static int getWarPointsForNation() {
		return getInt(EventWarConfigNodes.WAR_EVENT_POINTS_NATION);
	}

	public static int getWarPointsForKill() {
		return getInt(EventWarConfigNodes.WAR_EVENT_POINTS_KILL);
	}
	
	/*
	 * TownBlock HP.
	 */

	public static int getWarzoneTownBlockHealth() {
		return getInt(EventWarConfigNodes.WAR_EVENT_TOWN_BLOCK_HP);
	}

	public static int getWarzoneHomeBlockHealth() {
		return getInt(EventWarConfigNodes.WAR_EVENT_HOME_BLOCK_HP);
	}
	
	public static boolean getPlotsHealableInWar() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_PLOTS_HEALABLE);
	}
	
	/*
	 * Economic loss costs.
	 */

	public static double getWartimeTownBlockLossPrice() {
		return getDouble(EventWarConfigNodes.WAR_EVENT_TOWN_BLOCK_LOSS_PRICE);
	}

	public static double getWartimeDeathPrice() {
		return getDouble(EventWarConfigNodes.WAR_EVENT_PRICE_DEATH);
	}
	
	/*
	 * Generic Settings
	 */
	
	public static boolean isUsingEconomy() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_USING_ECONOMY);
	}
	
	public static int getMinWarHeight() {
		return getInt(EventWarConfigNodes.WAR_EVENT_MIN_HEIGHT);
	}
	
	public static boolean areWarTokensGivenOnNewDay() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_AWARD_WAR_TOKENS);
	}
	
	public static int teamSelectionSeconds() {
		return getInt(EventWarConfigNodes.WAR_EVENT_TEAM_SELECTION_DELAY);
	}

	public static boolean isDeclaringNeutral() {
		return getBoolean(EventWarConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL);
	}

	public static void setDeclaringNeutral(boolean choice) {
		setProperty(EventWarConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL.getRoot(), choice);
	}

	public static boolean getPlotsFireworkOnAttacked() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_PLOTS_FIREWORK_ON_ATTACKED);
	}

	public static boolean getWarEventCostsTownblocks() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_COSTS_TOWNBLOCKS);
	}

	public static boolean getWarEventWinnerTakesOwnershipOfTownblocks() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNBLOCKS);
	}
	
	public static boolean getWarEventWinnerTakesOwnershipOfTown() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWN);
	}
	
	public static boolean getWarEventWinnerTakesOwnershipOfTownsExcludesCapitals() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNS_EXCLUDES_CAPITALS);
	}
	
	public static int getWarEventConquerTime() {
		return getInt(EventWarConfigNodes.WAR_EVENT_CONQUER_TIME);
	}
	
	public static boolean isWarTimeTownsNeutral() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_TOWNS_NEUTRAL);
	}

	public static boolean isAllowWarBlockGriefing() {
		return getBoolean(EventWarConfigNodes.WAR_EVENT_BLOCK_GRIEFING);
	}
	
	/*
	 * Riot War Settings
	 */
	
	public static boolean riotsEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_ENABLE);
	}
	
	public static int riotDelay() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_DELAY);
	}
	
	public static long riotCooldown() {
		return TimeTools.getMillis(getString(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_COOLDOWN));
	}
	
	public static boolean riotsMayorDeathEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_MAYOR_DEATH);
	}
	
	public static boolean riotsWinnerTakesOverTown() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_WINNER_TAKES_OVER_TOWN);
	}
	
	public static int riotResidentLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_RESIDENT_LIVES);
	}
	
	public static int riotMayorLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_MAYOR_LIVES);
	}
	
	public static double riotBaseSpoils() {
		return getDouble(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_BASE_SPOILS);
	}
	
	public static int riotPointsPerKill() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_POINTS_PER_KILL);
	}
	
	public static int riotTokenCost() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_RIOT_TOKEN_COST);
	}
	
	/*
	 * Town War Settings
	 */
	
	public static boolean townWarEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_ENABLE);
	}
	
	public static int townWarDelay() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_DELAY);
	}
	
	public static long townWarCooldown() {
		return TimeTools.getMillis(getString(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_COOLDOWN));
	}
	
	public static boolean townWarTownBlockHPEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_TOWNBLOCK_HP);
	}
	
	public static int townWarResidentLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_RESIDENT_LIVES);
	}
	
	public static int townWarMayorLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_MAYOR_LIVES);
	}
	
	public static boolean townWarMayorDeathEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_MAYOR_DEATH);
	}

	public static boolean townWarWinnerTakesOverTown() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_WINNER_TAKES_OVER_TOWN);
	}
	
	public static double townWarBaseSpoils() {
		return getDouble(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_BASE_SPOILS);
	}
	
	public static int townWarPointsPerKill() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_POINTS_PER_KILL);
	}
	
	public static int townWarTokenCost() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_TOWN_WAR_TOKEN_COST);
	}
	
	/*
	 * Civil War Settings
	 */
	
	public static boolean civilWarEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_ENABLE);
	}

	public static int civilWarDelay() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_DELAY);
	}
	
	public static long civilWarCooldown() {
		return TimeTools.getMillis(getString(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_COOLDOWN));
	}
		
	public static boolean civilWarTownBlockHPEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_TOWNBLOCK_HP);
	}
	
	public static int civilWarResidentLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_RESIDENT_LIVES);
	}
	
	public static int civilWarMayorLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_MAYOR_LIVES);
	}
	
	public static boolean civilWarMayorDeathEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_MAYOR_DEATH);
	}

	public static boolean civilWarWinnerTakesOverNation() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_WINNER_TAKES_OVER_NATION);
	}
	
	public static double civilWarBaseSpoils() {
		return getDouble(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_BASE_SPOILS);
	}
	
	public static int civilWarPointsPerKill() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_POINTS_PER_KILL);
	}

	public static int civilWarTokenCost() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_TOKEN_COST);
	}
	
	/*
	 * Nation War Settings
	 */
	
	public static boolean nationWarEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_ENABLE);
	}

	public static int nationWarDelay() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_DELAY);
	}
	
	public static long nationWarCooldown() {
		return TimeTools.getMillis(getString(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_COOLDOWN));
	}
	
	public static boolean nationWarTownBlockHPEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_TOWNBLOCK_HP);
	}
	
	public static int nationWarResidentLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_RESIDENT_LIVES);
	}
	
	public static int nationWarMayorLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_MAYOR_LIVES);
	}
	
	public static boolean nationWarMayorDeathEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_MAYOR_DEATH);
	}

	public static boolean nationWarWinnerConquersTowns() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_WINNER_CONQUERS_TOWNS);
	}
	
	public static double nationWarBaseSpoils() {
		return getDouble(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_BASE_SPOILS);
	}
	
	public static int nationWarPointsPerKill() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_POINTS_PER_KILL);
	}
	
	public static int nationWarTokenCost() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_NATION_WAR_TOKEN_COST);
	}
	
	/*
	 * World War Settings
	 */
	
	public static boolean worldWarEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_ENABLE);
	}
	
	public static int worldWarDelay() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_DELAY);
	}
	
	public static long worldWarCooldown() {
		return TimeTools.getMillis(getString(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_COOLDOWN));
	}
	
	public static boolean worldWarTownBlockHPEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_TOWNBLOCK_HP);
	}
	
	public static int worldWarResidentLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_RESIDENT_LIVES);
	}
	
	public static int worldWarMayorLives() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_MAYOR_LIVES);
	}
	
	public static boolean worldWarMayorDeathEnabled() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_MAYOR_DEATH);
	}

	public static boolean worldWarWinnerConquersTowns() {
		return getBoolean(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_WINNER_CONQUERS_TOWNS);
	}
	
	public static double worldWarBaseSpoils() {
		return getDouble(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_BASE_SPOILS);
	}
	
	public static int worldWarPointsPerKill() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_POINTS_PER_KILL);
	}
	
	public static int worldWarTokenCost() {
		return getInt(EventWarConfigNodes.WAR_WAR_TYPES_WORLD_WAR_TOKEN_COST);
	}
	
}
