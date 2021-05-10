package com.palmergames.bukkit.towny.war.flagwar;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.TimeTools;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;

/** @deprecated for removal in a future release. Please use <a href="https://github.com/TownyAdvanced/Flagwar">the FlagWar plugin</a> for continued support. */
@Deprecated
public class FlagWarConfig {

	public static final DyeColor[] woolColors = new DyeColor[] {
			DyeColor.LIME, DyeColor.GREEN, DyeColor.BLUE, DyeColor.CYAN,
			DyeColor.LIGHT_BLUE, DyeColor.SILVER, DyeColor.WHITE,
			DyeColor.PINK, DyeColor.ORANGE, DyeColor.RED };


	private static Material flagBaseMaterial = null;
	private static Material flagLightMaterial = null;
	private static Material beaconWireFrameMaterial = null;

	public static boolean isAffectedMaterial(Material material) {

		return Tag.WOOL.isTagged(material) || material == getFlagBaseMaterial() || material == getFlagLightMaterial() || material == getBeaconWireFrameMaterial();
	}

	public static DyeColor[] getWoolColors() {

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

		FlagWarConfig.flagBaseMaterial = flagBaseMaterial;
	}

	public static void setFlagLightMaterial(Material flagLightMaterial) {

		FlagWarConfig.flagLightMaterial = flagLightMaterial;
	}

	public static void setBeaconWireFrameMaterial(Material beaconWireFrameMaterial) {

		FlagWarConfig.beaconWireFrameMaterial = beaconWireFrameMaterial;
	}

	public static int getMinPlayersOnlineInTownForWar() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_MIN_PLAYERS_ONLINE_IN_TOWN);
	}

	public static int getMinPlayersOnlineInNationForWar() {

		return TownySettings.getInt(ConfigNodes.WAR_ENEMY_MIN_PLAYERS_ONLINE_IN_NATION);
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
