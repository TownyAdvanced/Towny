package com.palmergames.bukkit.towny.permissions;

/**
 * @author ElgarL
 * 
 */
public enum PermissionNodes {
	TOWNY_ADMIN("towny.admin"),

	TOWNY_ADMIN_COMMAND("towny.admin.*"),

	CHEAT_BYPASS("towny.cheat.bypass"),
	TOWNY_TOP("towny.top"),
	TOWNY_TOWN_ALL("towny.town.*"),

	TOWNY_TOWN_NEW("towny.town.new"),
	TOWNY_TOWN_RESIDENT("towny.town.resident"),
	TOWNY_TOWN_DELETE("towny.town.delete"),
	TOWNY_TOWN_RENAME("towny.town.rename"),
	TOWNY_TOWN_CLAIM("towny.town.claim"),
	TOWNY_TOWN_CLAIM_OUTPOST("towny.town.claim.outpost"),
	TOWNY_TOWN_PLOT("towny.town.plot"),

	TOWNY_TOWN_PLOT_COMMAND("towny.town.plot.*"),

	TOWNY_TOWN_PLOTTYPE("towny.town.plottype"),

	TOWNY_SPAWN_ALL("towny.town.spawn.*"),

	TOWNY_SPAWN_TOWN("towny.town.spawn.town"),
	TOWNY_SPAWN_OUTPOST("towny.town.spawn.outpost"),
	TOWNY_SPAWN_NATION("towny.town.spawn.nation"),
	TOWNY_SPAWN_ALLY("towny.town.spawn.ally"),
	TOWNY_SPAWN_PUBLIC("towny.town.spawn.public"),

	TOWNY_TOGGLE_ALL("towny.town.toggle.*"),

	TOWNY_TOGGLE_PVP("towny.town.toggle.pvp"),
	TOWNY_TOGGLE_PUBLIC("towny.town.toggle.public"),
	TOWNY_TOGGLE_EXPLOSION("towny.town.toggle.explosions"),
	TOWNY_TOGGLE_FIRE("towny.town.toggle.fire"),
	TOWNY_TOGGLE_MOBS("towny.town.toggle.mobs"),
	TOWNY_TOGGLE_OPEN("towny.town.toggle.open"),

	TOWNY_NATION_ALL("towny.nation.*"),

	TOWNY_NATION_NEW("towny.nation.new"),
	TOWNY_NATION_DELETE("towny.nation.delete"),
	TOWNY_NATION_RENAME("towny.nation.rename"),
	TOWNY_NATION_GRANT_TITLES("towny.nation.grant-titles"),

	TOWNY_WILD_ALL("towny.wild.*"),

	//TOWNY_WILD_BUILD("towny.wild.build"),
	//TOWNY_WILD_DESTROY("towny.wild.destroy"),
	//TOWNY_WILD_SWITCH("towny.wild.switch"),
	//TOWNY_WILD_ITEM_USE("towny.wild.item_use"),

	TOWNY_WILD_BLOCK_BUILD("towny.wild.build.*"),
	TOWNY_WILD_BLOCK_DESTROY("towny.wild.destroy.*"),
	TOWNY_WILD_BLOCK_SWITCH("towny.wild.switch.*"),
	TOWNY_WILD_BLOCK_ITEM_USE("towny.wild.item_use.*"),

	TOWNY_CLAIMED_ALL("towny.claimed.*"),

	//TOWNY_CLAIMED_BUILD("towny.claimed.build"),
	//TOWNY_CLAIMED_DESTROY("towny.claimed.destroy"),
	//TOWNY_CLAIMED_SWITCH("towny.claimed.switch"),
	//TOWNY_CLAIMED_ITEM_USE("towny.claimed.item_use"),

	//TOWNY_CLAIMED_ALLTOWN_BLOCK("towny.claimed.alltown.*"),

	TOWNY_CLAIMED_ALLTOWN_BLOCK_BUILD("towny.claimed.alltown.build.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_DESTROY("towny.claimed.alltown.destroy.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_SWITCH("towny.claimed.alltown.switch.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_ITEM_USE("towny.claimed.alltown.item_use.*"),

	//TOWNY_CLAIMED_OWNTOWN_BLOCK("towny.claimed.owntown.*"),

	TOWNY_CLAIMED_OWNTOWN_BLOCK_BUILD("towny.claimed.owntown.build.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_DESTROY("towny.claimed.owntown.destroy.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_SWITCH("towny.claimed.owntown.switch.*"),
	TOWNY_CLAIMED_OWNTOWN_BLOCK_ITEM_USE("towny.claimed.owntown.item_use.*"),

	TOWNY_CHAT_ALL("towny.chat.*"),

	TOWNY_CHAT_TOWN("towny.chat.town"),
	TOWNY_CHAT_NATION("towny.chat.nation"),
	TOWNY_CHAT_ADMIN("towny.chat.admin"),
	TOWNY_CHAT_MOD("towny.chat.mod"),
	TOWNY_CHAT_GLOBAL("towny.chat.global"),
	TOWNY_CHAT_SPY("towny.chat.spy"),

	// Info nodes

	TOWNY_DEFAULT_MODES("towny_default_modes"),
	TOWNY_MAX_PLOTS("towny_maxplots"), ;

	private String value;

	/**
	 * Constructor
	 * 
	 * @param permission
	 */
	private PermissionNodes(String permission) {

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
	 * @return The permission node
	 */
	public String getNode(String replace) {

		return value.replace("*", replace);
	}

	public String getNode(int replace) {

		return value.replace("*", replace + "");
	}
}