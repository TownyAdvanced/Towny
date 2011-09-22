package com.palmergames.bukkit.townywar;

import org.bukkit.DyeColor;
import org.bukkit.Material;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.TimeTools;

public class TownyWarConfig {
	public static final DyeColor[] woolColors = new DyeColor[] {
		DyeColor.LIME,
		DyeColor.GREEN,
		DyeColor.BLUE,
		DyeColor.CYAN,
		DyeColor.LIGHT_BLUE,
		DyeColor.SILVER,
		DyeColor.WHITE,
		DyeColor.PINK,
		DyeColor.ORANGE,
		DyeColor.RED
	};
	
	private static Material flagBaseMaterial = null;
	private static Material flagLightMaterial = null;
	private static Material beaconWireFrameMaterial = null;
	
	public static boolean isAffectedMaterial(Material material) {
		return material == Material.WOOL
			|| material == getFlagBaseMaterial()
			|| material == getFlagLightMaterial()
			|| material == getBeaconWireFrameMaterial();
	}
	
	public static String parseSingleLineString(String str) {
        return str.replaceAll("&", "\u00A7");
	}
	
	public static DyeColor[] getWoolColors() {
		return woolColors;
	}
	
	public static boolean isAllowingAttacks() {
		return TownySettings.getBoolean(ConfigNodes.WAR_ENEMY_ALLOW_ATTACKS);
	}
	
	public static long getFlagWaitingTime() {
		return TimeTools.getMillis(TownySettings.getString(ConfigNodes.WAR_ENEMY_FLAG_WAITING_TIME));
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
}
