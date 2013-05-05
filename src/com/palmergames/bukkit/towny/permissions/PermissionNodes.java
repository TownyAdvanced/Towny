package com.palmergames.bukkit.towny.permissions;

/**
 * @author ElgarL
 * 
 */
public enum PermissionNodes {
	TOWNY_ADMIN("towny.admin"),

	//TOWNY_ADMIN_COMMAND("towny.admin.*"),

	CHEAT_BYPASS("towny.cheat.bypass"),
	
	/*
	 * Nation command permissions
	 */
	TOWNY_COMMAND_NATION_LIST("towny.command.nation.list"),
	TOWNY_COMMAND_NATION_NEW("towny.command.nation.new"),
	TOWNY_COMMAND_NATION_LEAVE("towny.command.nation.leave"),
	TOWNY_COMMAND_NATION_WITHDRAW("towny.command.nation.withdraw"),
	TOWNY_COMMAND_NATION_DEPOSIT("towny.command.nation.deposit"),
	
	// Covers all assignable ranks
	TOWNY_COMMAND_NATION_RANK("towny.command.nation.rank.*"),
		TOWNY_COMMAND_NATION_RANK_ASSISTANT("towny.command.nation.rank.assistant"),
	
	TOWNY_COMMAND_NATION_KING("towny.command.nation.king"),
	
	//TOWNY_COMMAND_NATION_ASSISTANT("towny.command.nation.assistant"),
	
	TOWNY_COMMAND_NATION_SET("towny.command.nation.set.*"),
		TOWNY_COMMAND_NATION_SET_KING("towny.command.nation.set.king"),
		TOWNY_COMMAND_NATION_SET_CAPITOL("towny.command.nation.set.capitol"),
		TOWNY_COMMAND_NATION_SET_TAXES("towny.command.nation.set.taxes"),
		TOWNY_COMMAND_NATION_SET_NAME("towny.command.nation.set.name"),
		TOWNY_COMMAND_NATION_SET_TITLE("towny.command.nation.set.title"),
		TOWNY_COMMAND_NATION_SET_SURNAME("towny.command.nation.set.surname"),
		TOWNY_COMMAND_NATION_SET_TAG("towny.command.nation.set.tag"),

	TOWNY_COMMAND_NATION_TOGGLE("towny.command.nation.toggle.*"),
		TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL("towny.command.nation.toggle.neutral"),
	
	TOWNY_COMMAND_NATION_ALLY("towny.command.nation.ally"),
	TOWNY_COMMAND_NATION_ENEMY("towny.command.nation.enemy"),
	TOWNY_COMMAND_NATION_DELETE("towny.command.nation.delete"),
	TOWNY_COMMAND_NATION_ONLINE("towny.command.nation.online"),
	TOWNY_COMMAND_NATION_ADD("towny.command.nation.add"),
	TOWNY_COMMAND_NATION_KICK("towny.command.nation.kick"),
	
	/*
	 * Town command permissions
	 */
	TOWNY_COMMAND_TOWN("towny.command.town.*"),
		TOWNY_COMMAND_TOWN_HERE("towny.command.town.here"),
		TOWNY_COMMAND_TOWN_LIST("towny.command.town.list"),
		TOWNY_COMMAND_TOWN_NEW("towny.command.town.new"),
		TOWNY_COMMAND_TOWN_LEAVE("towny.command.town.leave"),
		TOWNY_COMMAND_TOWN_WITHDRAW("towny.command.town.withdraw"),
		TOWNY_COMMAND_TOWN_DEPOSIT("towny.command.town.deposit"),
		
		// Covers all assignable ranks
		TOWNY_COMMAND_TOWN_RANK("towny.command.town.rank.*"),
		TOWNY_COMMAND_TOWN_RANKLIST("towny.command.town.ranklist"),
		
		TOWNY_COMMAND_TOWN_SET("towny.command.town.set.*"),
			TOWNY_COMMAND_TOWN_SET_BOARD("towny.command.town.set.board"),
			TOWNY_COMMAND_TOWN_SET_MAYOR("towny.command.town.set.mayor"),
			TOWNY_COMMAND_TOWN_SET_HOMEBLOCK("towny.command.town.set.homeblock"),
			TOWNY_COMMAND_TOWN_SET_SPAWN("towny.command.town.set.spawn"),
			TOWNY_COMMAND_TOWN_SET_OUTPOST("towny.command.town.set.outpost"),
			TOWNY_COMMAND_TOWN_SET_PERM("towny.command.town.set.perm"),
			TOWNY_COMMAND_TOWN_SET_TAXES("towny.command.town.set.taxes"),
			TOWNY_COMMAND_TOWN_SET_PLOTTAX("towny.command.town.set.plottax"),
			TOWNY_COMMAND_TOWN_SET_SHOPTAX("towny.command.town.set.shoptax"),
			TOWNY_COMMAND_TOWN_SET_EMBASSYTAX("towny.command.town.set.embassytax"),
			TOWNY_COMMAND_TOWN_SET_PLOTPRICE("towny.command.town.set.plotprice"),
			TOWNY_COMMAND_TOWN_SET_NAME("towny.command.town.set.name"),
			TOWNY_COMMAND_TOWN_SET_TAG("towny.command.town.set.tag"),
		
		TOWNY_COMMAND_TOWN_BUY("towny.command.town.buy"),
		
		TOWNY_COMMAND_TOWN_TOGGLE("towny.command.town.toggle.*"),
			TOWNY_COMMAND_TOWN_TOGGLE_PVP("towny.command.town.toggle.pvp"),
			TOWNY_COMMAND_TOWN_TOGGLE_PUBLIC("towny.command.town.toggle.public"),
			TOWNY_COMMAND_TOWN_TOGGLE_EXPLOSION("towny.command.town.toggle.explosion"),
			TOWNY_COMMAND_TOWN_TOGGLE_FIRE("towny.command.town.toggle.fire"),
			TOWNY_COMMAND_TOWN_TOGGLE_MOBS("towny.command.town.toggle.mobs"),
			TOWNY_COMMAND_TOWN_TOGGLE_TAXPERCENT("towny.command.town.toggle.taxpercent"),
			TOWNY_COMMAND_TOWN_TOGGLE_OPEN("towny.command.town.toggle.open"),
		
		
		TOWNY_COMMAND_TOWN_MAYOR("towny.command.town.mayor"),
		TOWNY_COMMAND_TOWN_DELETE("towny.command.town.delete"),
		TOWNY_COMMAND_TOWN_JOIN("towny.command.town.join"),
		TOWNY_COMMAND_TOWN_ADD("towny.command.town.add"),
		TOWNY_COMMAND_TOWN_KICK("towny.command.town.kick"),
		
		TOWNY_COMMAND_TOWN_CLAIM("towny.command.town.claim.*"),
			TOWNY_COMMAND_TOWN_CLAIM_TOWN("towny.command.town.claim.town"),
			TOWNY_COMMAND_TOWN_CLAIM_OUPTPOST("towny.command.town.claim.outpost"),
		
		TOWNY_COMMAND_TOWN_UNCLAIM("towny.command.town.unclaim"),
		TOWNY_COMMAND_TOWN_ONLINE("towny.command.town.online"),
	
	/*
	 * Plot command permissions
	 */
	TOWNY_COMMAND_PLOT("towny.command.plot.*"),
		TOWNY_COMMAND_PLOT_CLAIM("towny.command.plot.claim"),
		TOWNY_COMMAND_PLOT_UNCLAIM("towny.command.plot.unclaim"),
		TOWNY_COMMAND_PLOT_NOTFORSALE("towny.command.plot.notforsale"),
		TOWNY_COMMAND_PLOT_FORSALE("towny.command.plot.forsale"),
		TOWNY_COMMAND_PLOT_PERM("towny.command.plot.perm"),
		
		TOWNY_COMMAND_PLOT_TOGGLE("towny.command.plot.toggle.*"),
			TOWNY_COMMAND_PLOT_TOGGLE_PVP("towny.command.plot.toggle.pvp"),
			TOWNY_COMMAND_PLOT_TOGGLE_EXPLOSION("towny.command.plot.toggle.explosion"),
			TOWNY_COMMAND_PLOT_TOGGLE_FIRE("towny.command.plot.toggle.fire"),
			TOWNY_COMMAND_PLOT_TOGGLE_MOBS("towny.command.plot.toggle.mobs"),
		
		TOWNY_COMMAND_PLOT_SET("towny.command.plot.set.*"),
			TOWNY_COMMAND_PLOT_SET_PERM("towny.command.plot.set.perm"),
			TOWNY_COMMAND_PLOT_SET_RESET("towny.command.plot.set.reset"),
			TOWNY_COMMAND_PLOT_SET_SHOP("towny.command.plot.set.shop"),
			TOWNY_COMMAND_PLOT_SET_EMBASSY("towny.command.plot.set.embassy"),
			TOWNY_COMMAND_PLOT_SET_ARENA("towny.command.plot.set.arena"),
			TOWNY_COMMAND_PLOT_SET_WILDS("towny.command.plot.set.wilds"),
			TOWNY_COMMAND_PLOT_SET_SPLEEF("towny.command.plot.set.spleef"),
		
		TOWNY_COMMAND_PLOT_CLEAR("towny.command.plot.clear"),
	
	/*
	 * Resident command permissions
	 */
	TOWNY_COMMAND_RESIDENT_LIST("towny.command.resident.list"),
	TOWNY_COMMAND_RESIDENT_TAX("towny.command.resident.tax"),
	TOWNY_COMMAND_RESIDENT_SET("towny.command.resident.set.*"),
		TOWNY_COMMAND_RESIDENT_SET_PERM("towny.command.resident.set.perm"),
		TOWNY_COMMAND_RESIDENT_SET_MODE("towny.command.resident.set.mode"),
	
	TOWNY_COMMAND_RESIDENT_TOGGLE("towny.command.resident.toggle.*"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_PVP("towny.command.resident.toggle.pvp"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_EXPLOSION("towny.command.resident.toggle.explosion"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_FIRE("towny.command.resident.toggle.fire"),
		TOWNY_COMMAND_RESIDENT_TOGGLE_MOBS("towny.command.resident.toggle.mobs"),
	
	TOWNY_COMMAND_RESIDENT_FRIEND("towny.command.resident.friend"),
	
	
	/*
	 * TownyAdmin command permissions
	 */
	TOWNY_COMMAND_TOWNYADMIN("towny.command.townyadmin.*"),
	TOWNY_COMMAND_TOWNYADMIN_SET("towny.command.townyadmin.set.*"),
		TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR("towny.command.townyadmin.set.mayor"),
	
	TOWNY_COMMAND_TOWNYADMIN_TOWN("towny.command.townyadmin.town.*"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW("towny.command.townyadmin.town.new"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_ADD("towny.command.townyadmin.town.add"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_KICK("towny.command.townyadmin.town.kick"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE("towny.command.townyadmin.town.delete"),
		TOWNY_COMMAND_TOWNYADMIN_TOWN_RENAME("towny.command.townyadmin.town.rename"),
	
	TOWNY_COMMAND_TOWNYADMIN_NATION("towny.command.townyadmin.nation.*"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_ADD("towny.command.townyadmin.nation.add"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE("towny.command.townyadmin.nation.delete"),
		TOWNY_COMMAND_TOWNYADMIN_NATION_RENAME("towny.command.townyadmin.nation.rename"),
	
	TOWNY_COMMAND_TOWNYADMIN_TOGGLE("towny.command.townyadmin.toggle.*"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_WAR("towny.command.townyadmin.toggle.war"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NEUTRAL("towny.command.townyadmin.toggle.neutral"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEVMODE("towny.command.townyadmin.toggle.devmode"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEBUG("towny.command.townyadmin.toggle.debug"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_TOWNWITHDRAW("towny.command.townyadmin.toggle.townwithdraw"),
		TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NATIONWITHDRAW("towny.command.townyadmin.toggle.nationwithdraw"),
	
	TOWNY_COMMAND_TOWNYADMIN_GIVEBONUS("towny.command.townyadmin.givebonus"),
	TOWNY_COMMAND_TOWNYADMIN_RELOAD("towny.command.townyadmin.reload"),
	TOWNY_COMMAND_TOWNYADMIN_RESET("towny.command.townyadmin.reset"),
	TOWNY_COMMAND_TOWNYADMIN_BACKUP("towny.command.townyadmin.backup"),
	TOWNY_COMMAND_TOWNYADMIN_NEWDAY("towny.command.townyadmin.newday"),
	TOWNY_COMMAND_TOWNYADMIN_PURGE("towny.command.townyadmin.purge"),
	TOWNY_COMMAND_TOWNYADMIN_UNCLAIM("towny.command.townyadmin.unclaim"),
	TOWNY_COMMAND_TOWNYADMIN_RESIDNET_DELETE("towny.command.townyadmin.resident.delete"),
	
	
	/*
	 * Towny command permissions
	 */
	TOWNY_COMMAND_TOWNY("towny.command.towny.*"),
	TOWNY_COMMAND_TOWNY_MAP("towny.command.towny.map"),
	TOWNY_COMMAND_TOWNY_TOP("towny.command.towny.top"),
	TOWNY_COMMAND_TOWNY_TREE("towny.command.towny.tree"),
	TOWNY_COMMAND_TOWNY_TIME("towny.command.towny.time"),
	TOWNY_COMMAND_TOWNY_UNIVERSE("towny.command.towny.universe"),
	TOWNY_COMMAND_TOWNY_VERSION("towny.command.towny.version"),
	TOWNY_COMMAND_TOWNY_WAR("towny.command.towny.war"),
	TOWNY_COMMAND_TOWNY_SPY("towny.command.towny.spy"),
	
	/*
	 * TownyWorld command permissions
	 */
	TOWNY_COMMAND_TOWNYWORLD_LIST("towny.command.townyworld.list"),
	TOWNY_COMMAND_TOWNYWORLD_SET("towny.command.townyworld.set"),
	
	TOWNY_COMMAND_TOWNYWORLD_TOGGLE("towny.command.townyworld.toggle.*"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_CLAIMABLE("towny.command.townyworld.toggle.claimable"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_USINGTOWNY("towny.command.townyworld.toggle.usingtowny"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PVP("towny.command.townyworld.toggle.pvp"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEPVP("towny.command.townyworld.toggle.forcepvp"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_EXPLOSION("towny.command.townyworld.toggle.explosion"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEEXPLOSION("towny.command.townyworld.toggle.forceexplosion"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FIRE("towny.command.townyworld.toggle.fire"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEFIRE("towny.command.townyworld.toggle.forcefire"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_TOWNMOBS("towny.command.townyworld.toggle.townmobs"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WORLDMOBS("towny.command.townyworld.toggle.worldmobs"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTUNCLAIM("towny.command.townyworld.toggle.revertunclaim"),
		TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTEXPL("towny.command.townyworld.toggle.revertexpl"),
	
	TOWNY_COMMAND_TOWNYWORLD_REGEN("towny.command.townyworld.regen"),
	TOWNY_COMMAND_TOWNYWORLD_UNDO("towny.command.townyworld.undo"),
	
	
	
	//TOWNY_TOP("towny.top"),
	//TOWNY_TOWN_ALL("towny.town.*"),

	
	TOWNY_TOWN_RESIDENT("towny.town.resident"),
	
	//TOWNY_TOWN_NEW("towny.town.new"),
	//TOWNY_TOWN_DELETE("towny.town.delete"),
	//TOWNY_TOWN_RENAME("towny.town.rename"),
	//TOWNY_TOWN_CLAIM("towny.town.claim"),
	//TOWNY_TOWN_CLAIM_OUTPOST("towny.town.claim.outpost"),
	//TOWNY_TOWN_PLOT("towny.town.plot"),

	//TOWNY_TOWN_PLOT_COMMAND("towny.town.plot.*"),

	//TOWNY_TOWN_PLOTTYPE("towny.town.plottype"),

	TOWNY_SPAWN_ALL("towny.town.spawn.*"),

	TOWNY_SPAWN_TOWN("towny.town.spawn.town"),
	TOWNY_SPAWN_OUTPOST("towny.town.spawn.outpost"),
	TOWNY_SPAWN_NATION("towny.town.spawn.nation"),
	TOWNY_SPAWN_ALLY("towny.town.spawn.ally"),
	TOWNY_SPAWN_PUBLIC("towny.town.spawn.public"),

	//TOWNY_TOGGLE_ALL("towny.town.toggle.*"),

	//TOWNY_TOGGLE_PVP("towny.town.toggle.pvp"),
	//TOWNY_TOGGLE_PUBLIC("towny.town.toggle.public"),
	//TOWNY_TOGGLE_EXPLOSION("towny.town.toggle.explosions"),
	//TOWNY_TOGGLE_FIRE("towny.town.toggle.fire"),
	//TOWNY_TOGGLE_MOBS("towny.town.toggle.mobs"),
	//TOWNY_TOGGLE_OPEN("towny.town.toggle.open"),

	//TOWNY_NATION_ALL("towny.nation.*"),

	//TOWNY_NATION_NEW("towny.nation.new"),
	//TOWNY_NATION_DELETE("towny.nation.delete"),
	//TOWNY_NATION_RENAME("towny.nation.rename"),
	//TOWNY_NATION_GRANT_TITLES("towny.nation.grant-titles"),

	TOWNY_WILD_ALL("towny.wild.*"),

	TOWNY_WILD_BLOCK_BUILD("towny.wild.build.*"),
	TOWNY_WILD_BLOCK_DESTROY("towny.wild.destroy.*"),
	TOWNY_WILD_BLOCK_SWITCH("towny.wild.switch.*"),
	TOWNY_WILD_BLOCK_ITEM_USE("towny.wild.item_use.*"),

	TOWNY_CLAIMED_ALL("towny.claimed.*"),

	TOWNY_CLAIMED_ALLTOWN_BLOCK_BUILD("towny.claimed.alltown.build.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_DESTROY("towny.claimed.alltown.destroy.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_SWITCH("towny.claimed.alltown.switch.*"),
	TOWNY_CLAIMED_ALLTOWN_BLOCK_ITEM_USE("towny.claimed.alltown.item_use.*"),

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
	TOWNY_MAX_PLOTS("towny_maxplots"), 
	TOWNY_MAX_OUTPOSTS("towny_maxoutposts");

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