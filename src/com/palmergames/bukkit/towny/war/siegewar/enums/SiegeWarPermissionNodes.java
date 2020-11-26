package com.palmergames.bukkit.towny.war.siegewar.enums;

public enum SiegeWarPermissionNodes {

	TOWNY_NATION_SIEGE_POINTS("towny.nation.siege.points"),
	TOWNY_NATION_SIEGE_LEADERSHIP("towny.nation.siege.leadership"),
	TOWNY_NATION_SIEGE_ATTACK("towny.nation.siege.attack"),
	TOWNY_NATION_SIEGE_ABANDON("towny.nation.siege.abandon"),
	TOWNY_NATION_SIEGE_INVADE("towny.nation.siege.invade"),
	TOWNY_NATION_SIEGE_PLUNDER("towny.nation.siege.plunder"),
	TOWNY_TOWN_SIEGE_POINTS("towny.town.siege.points"),
	TOWNY_TOWN_SIEGE_SURRENDER("towny.town.siege.surrender"),
	TOWNY_COMMAND_TOWNYADMIN_SET_SIEGEIMMUNITIES("towny.command.townyadmin.set.siegeimmunities"),
	// Siegewar related war sickness immunities
	TOWNY_SIEGE_WAR_IMMUNE_TO_WAR_NAUSEA("towny.siege.war.immune.to.war.nausea"),
	TOWNY_SIEGE_WAR_IMMUNE_TO_BATTLE_FATIGUE("towny.siege.war.immune.to.battle.fatigue");
	
	private String value;

	/**
	 * Constructor
	 * 
	 * @param permission - Permission.
	 */
	SiegeWarPermissionNodes(String permission) {

		this.value = permission;
	}

	/**
	 * Retrieves the permission node
	 * 
	 * @return The permission node
	 */
	public String getNode() {

		return value;
	}

	/**
	 * Retrieves the permission node
	 * replacing the character *
	 * 
	 * @param replace - String
	 * @return The permission node
	 */
	public String getNode(String replace) {

		return value.replace("*", replace);
	}

	public String getNode(int replace) {

		return value.replace("*", replace + "");
	}
}
