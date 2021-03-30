/**
 * WarZone Configuration Checks
 */
package com.palmergames.bukkit.towny.war.common;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
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
}
