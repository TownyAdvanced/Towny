package com.palmergames.bukkit.towny.war.common.townruin;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;

public class TownRuinSettings {
	private TownRuinSettings() {
		// Privatize implied public constructor.
	}

	public static boolean getTownRuinsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_ENABLED);
	}

	public static int getTownRuinsMaxDurationHours() {
		return Math.min(TownySettings.getInt(ConfigNodes.TOWN_RUINING_TOWN_RUINS_MAX_DURATION_HOURS), 8760);
	}

	public static int getTownRuinsMinDurationHours() {
		return TownySettings.getInt(ConfigNodes.TOWN_RUINING_TOWN_RUINS_MIN_DURATION_HOURS);
	}

	public static boolean getTownRuinsReclaimEnabled() {
		return TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_RECLAIM_ENABLED);
	}

	public static double getEcoPriceReclaimTown() {
		return TownySettings.getDouble(ConfigNodes.ECO_PRICE_RECLAIM_RUINED_TOWN);
	}
	
	public static boolean areRuinsMadePublic() {
		return TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWNS_BECOME_PUBLIC);
	}

}
