package com.palmergames.bukkit.towny.war.common;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;

public class WarConfig {
	public static boolean isAllowingAttacks() {

		return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_ALLOW_ATTACKS);
	}
}
