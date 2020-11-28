package com.palmergames.bukkit.towny.war.common.townruin;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;

public class TownRuinSettings {

	public static boolean getWarCommonTownRuinsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_COMMON_TOWN_RUINS_ENABLED);
	}

	public static int getWarCommonTownRuinsMaxDurationHours() {
		return TownySettings.getInt(ConfigNodes.WAR_COMMON_TOWN_RUINS_MAX_DURATION_HOURS);
	}

	public static int getWarCommonTownRuinsMinDurationHours() {
		return TownySettings.getInt(ConfigNodes.WAR_COMMON_TOWN_RUINS_MIN_DURATION_HOURS);
	}

	public static boolean getWarCommonTownRuinsReclaimEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_COMMON_TOWN_RUINS_RECLAIM_ENABLED);
	}

	public static double getEcoPriceReclaimTown() {
		return TownySettings.getDouble(ConfigNodes.ECO_PRICE_RECLAIM_RUINED_TOWN);
	}


}
