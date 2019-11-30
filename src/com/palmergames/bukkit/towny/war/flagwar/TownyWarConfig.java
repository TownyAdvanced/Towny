package com.palmergames.bukkit.towny.war.flagwar;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.TimeTools;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.List;
import java.util.Set;

public class TownyWarConfig {

	public static final Material[] woolColors = new Material[] {
			Material.LIME_WOOL, Material.GREEN_WOOL, Material.BLUE_WOOL, Material.CYAN_WOOL,
			Material.LIGHT_BLUE_WOOL, Material.GRAY_WOOL, Material.WHITE_WOOL,
			Material.PINK_WOOL, Material.ORANGE_WOOL, Material.RED_WOOL };

	private static Material flagBaseMaterial = null;
	private static Material flagLightMaterial = null;
	private static Material beaconWireFrameMaterial = null;

	private static Set<Material> editableMaterialsInWarZone = null;

	public static boolean isAffectedMaterial(Material material) {

		return Tag.WOOL.isTagged(material) || material == getFlagBaseMaterial() || material == getFlagLightMaterial() || material == getBeaconWireFrameMaterial();
	}

	public static Material[] getWoolColors() {

		return woolColors;
	}

	public static boolean isAllowingAttacks() {

		return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_ALLOW_ATTACKS);
	}

	public static long getFlagWaitingTime() {

		return TimeTools.convertToTicks(TownySettings.getSeconds(ConfigNodes.WAR_ENEMY_FLAG_WAITING_TIME));
	}

	public static long getTimeBetweenFlagColorChange() {

		return getFlagWaitingTime() / getWoolColors().length;
	}

	public static boolean isDrawingBeacon() {

		return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_BEACON_DRAW);
	}

	public static int getMaxActiveFlagsPerPerson() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_MAX_ACTIVE_FLAGS_PER_PLAYER);
	}

	public static Material getFlagBaseMaterial() {

		return flagBaseMaterial;
	}

	public static Material getFlagLightMaterial() {

		return flagLightMaterial;
	}

	public static Material getBeaconWireFrameMaterial() {

		return beaconWireFrameMaterial;
	}

	public static int getBeaconRadius() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_BEACON_RADIUS);
	}

	public static int getBeaconSize() {

		return getBeaconRadius() * 2 - 1;
	}

	public static int getBeaconMinHeightAboveFlag() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_BEACON_HEIGHT_ABOVE_FLAG_MIN);
	}

	public static int getBeaconMaxHeightAboveFlag() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_BEACON_HEIGHT_ABOVE_FLAG_MAX);
	}

	public static void setFlagBaseMaterial(Material flagBaseMaterial) {

		TownyWarConfig.flagBaseMaterial = flagBaseMaterial;
	}

	public static void setFlagLightMaterial(Material flagLightMaterial) {

		TownyWarConfig.flagLightMaterial = flagLightMaterial;
	}

	public static void setBeaconWireFrameMaterial(Material beaconWireFrameMaterial) {

		TownyWarConfig.beaconWireFrameMaterial = beaconWireFrameMaterial;
	}

	public static int getMinPlayersOnlineInTownForWar() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_MIN_PLAYERS_ONLINE_IN_TOWN);
	}

	public static int getMinPlayersOnlineInNationForWar() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_MIN_PLAYERS_ONLINE_IN_NATION);
	}

	public static void setEditableMaterialsInWarZone(Set<Material> editableMaterialsInWarZone) {

		TownyWarConfig.editableMaterialsInWarZone = editableMaterialsInWarZone;
	}

	public static boolean isEditableMaterialInWarZone(Material material) {

		return TownyWarConfig.editableMaterialsInWarZone.contains(material);
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

	public static double getWonTownblockReward() {

		return TownySettings.getDouble(ConfigNodes.WAR_ECONOMY_TOWNBLOCK_WON);
	}

	public static double getWonHomeblockReward() {

		return TownySettings.getDouble(ConfigNodes.WAR_ECONOMY_HOMEBLOCK_WON);
	}

	public static double getCostToPlaceWarFlag() {

		return TownySettings.getDouble(ConfigNodes.WAR_ECONOMY_ENEMY_PLACE_FLAG);
	}

	public static double getDefendedAttackReward() {

		return TownySettings.getDouble(ConfigNodes.WAR_ECONOMY_ENEMY_DEFENDED_ATTACK);
	}

    public static boolean isAttackingBordersOnly() {

        return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_ONLY_ATTACK_BORDER);
    }

	public static boolean isFlaggedTownblockTransfered() {
		return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_FLAG_TAKES_OWNERSHIP_OF_TOWNBLOCKS);
	}
}
