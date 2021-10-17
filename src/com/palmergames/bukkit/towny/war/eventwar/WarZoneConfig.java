/**
 * WarZone Configuration Checks
 */
package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.TimeTools;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

public class WarZoneConfig {
	public static Set<Material> editableMaterialsInWarZone = null;

	public static void setEditableMaterialsInWarZone(Set<Material> editableMaterialsInWarZone) {

		WarZoneConfig.editableMaterialsInWarZone = editableMaterialsInWarZone;
	}

	public static boolean isEditableMaterialInWarZone(Material material) {

		return editableMaterialsInWarZone.contains(material);
	}

	public static boolean isAllowingSwitchesInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_SWITCH);
	}

	public static boolean isAllowingFireInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_FIRE);
	}

	public static boolean isAllowingItemUseInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_ITEM_USE);
	}

	public static boolean isAllowingExplosionsInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_EXPLOSIONS);
	}

	public static boolean explosionsBreakBlocksInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_EXPLOSIONS_BREAK_BLOCKS);
	}

	public static boolean regenBlocksAfterExplosionInWarZone() {

		return TownySettings.getBoolean(ConfigNodes.WAR_WARZONE_EXPLOSIONS_REGEN_BLOCKS);
	}

	public static List<String> getExplosionsIgnoreList() {

		return TownySettings.getStrArr(ConfigNodes.WAR_WARZONE_EXPLOSIONS_IGNORE_LIST);
	}
	
	public static int teamSelectionSeconds() {
		return TownySettings.getInt(ConfigNodes.WAR_EVENT_TEAM_SELECTION_DELAY);
	}
	
	public static boolean riotsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_RIOT_ENABLE);
	}
	
	public static int riotDelay() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_RIOT_DELAY);
	}
	
	public static long riotCooldown() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_WAR_TYPES_RIOT_COOLDOWN));
	}
	
	public static boolean riotsMayorDeathEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_RIOT_MAYOR_DEATH);
	}
	
	public static boolean riotsWinnerTakesOverTown() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_RIOT_WINNER_TAKES_OVER_TOWN);
	}
	
	public static int riotResidentLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_RIOT_RESIDENT_LIVES);
	}
	
	public static int riotMayorLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_RIOT_MAYOR_LIVES);
	}
	
	public static double riotBaseSpoils() {
		return TownySettings.getDouble(ConfigNodes.WAR_WAR_TYPES_RIOT_BASE_SPOILS);
	}
	
	public static int riotPointsPerKill() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_RIOT_POINTS_PER_KILL);
	}
	
	public static boolean townWarEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_ENABLE);
	}
	
	public static int townWarDelay() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_DELAY);
	}
	
	public static long townWarCooldown() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_COOLDOWN));
	}
	
	public static boolean townWarTownBlockHPEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_TOWNBLOCK_HP);
	}
	
	public static int townWarResidentLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_RESIDENT_LIVES);
	}
	
	public static int townWarMayorLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_MAYOR_LIVES);
	}
	
	public static boolean townWarMayorDeathEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_MAYOR_DEATH);
	}

	public static boolean townWarWinnerTakesOverTown() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_WINNER_TAKES_OVER_TOWN);
	}
	
	public static double townWarBaseSpoils() {
		return TownySettings.getDouble(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_BASE_SPOILS);
	}
	
	public static int townWarPointsPerKill() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_TOWN_WAR_POINTS_PER_KILL);
	}
	
	public static boolean civilWarEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_ENABLE);
	}

	public static int civilWarDelay() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_DELAY);
	}
	
	public static long civilWarCooldown() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_COOLDOWN));
	}
		
	public static boolean civilWarTownBlockHPEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_TOWNBLOCK_HP);
	}
	
	public static int civilWarResidentLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_RESIDENT_LIVES);
	}
	
	public static int civilWarMayorLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_MAYOR_LIVES);
	}
	
	public static boolean civilWarMayorDeathEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_MAYOR_DEATH);
	}

	public static boolean civilWarWinnerTakesOverNation() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_WINNER_TAKES_OVER_NATION);
	}
	
	public static double civilWarBaseSpoils() {
		return TownySettings.getDouble(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_BASE_SPOILS);
	}
	
	public static int civilWarPointsPerKill() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_CIVIL_WAR_POINTS_PER_KILL);
	}
	
	public static boolean nationWarEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_ENABLE);
	}

	public static int nationWarDelay() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_DELAY);
	}
	
	public static long nationWarCooldown() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_COOLDOWN));
	}
	
	public static boolean nationWarTownBlockHPEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_TOWNBLOCK_HP);
	}
	
	public static int nationWarResidentLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_RESIDENT_LIVES);
	}
	
	public static int nationWarMayorLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_MAYOR_LIVES);
	}
	
	public static boolean nationWarMayorDeathEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_MAYOR_DEATH);
	}

	public static boolean nationWarWinnerConquersTowns() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_WINNER_CONQUERS_TOWNS);
	}
	
	public static double nationWarBaseSpoils() {
		return TownySettings.getDouble(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_BASE_SPOILS);
	}
	
	public static int nationWarPointsPerKill() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_NATION_WAR_POINTS_PER_KILL);
	}
	
	public static boolean worldWarEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_ENABLE);
	}
	
	public static int worldWarDelay() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_DELAY);
	}
	
	public static long worldWarCooldown() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_COOLDOWN));
	}
	
	public static boolean worldWarTownBlockHPEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_TOWNBLOCK_HP);
	}
	
	public static int worldWarResidentLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_RESIDENT_LIVES);
	}
	
	public static int worldWarMayorLives() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_MAYOR_LIVES);
	}
	
	public static boolean worldWarMayorDeathEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_MAYOR_DEATH);
	}

	public static boolean worldWarWinnerConquersTowns() {
		return TownySettings.getBoolean(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_WINNER_CONQUERS_TOWNS);
	}
	
	public static double worldWarBaseSpoils() {
		return TownySettings.getDouble(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_BASE_SPOILS);
	}
	
	public static int worldWarPointsPerKill() {
		return TownySettings.getInt(ConfigNodes.WAR_WAR_TYPES_WORLD_WAR_POINTS_PER_KILL);
	}
}
