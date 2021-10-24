package com.palmergames.bukkit.towny.war.eventwar.settings;

public enum EventWarConfigNodes {

	WAR(
			"war",
			"",
			"",
			"",
			"############################################################",
			"# +------------------------------------------------------+ #",
			"# |                     War settings                     | #",
			"# +------------------------------------------------------+ #",
			"############################################################",
			""),
	WARTIME_NATION_CAN_BE_NEUTRAL(
			"war.nation_can_be_neutral",
			"true",
			"",
			"#This setting allows you disable the ability for a nation to pay to remain neutral during a war."),
	WAR_EVENT(
			"war.event",
			"",
			"",
			"",
			"############################################################",
			"# +------------------------------------------------------+ #",
			"# |                 War Event settings                   | #",
			"# +------------------------------------------------------+ #",
			"############################################################",
			"",
			"# During the event a town losing a townblock pays the wartime_town_block_loss_price to the attacking town.",
			"# The war is won when the only nations left in the battle are allies, or only a single nation.",
			"#",
			"# The winning nations share half of the war spoils.",
			"# The remaining half is paid to the town which took the most town blocks, and lost the least.",
			""),
	WAR_EVENT_AWARD_WAR_TOKENS(
			"war.event.award_war_tokens",
			"false",
			"",
			"# When set to true, all non-peaceful towns and nations will accrue 1 war token per Towny day.",
			"# These tokens can be spent on Declarations of War, a method of allowing players to start wars independent of admins."),
	WAR_EVENT_USING_ECONOMY(
			"war.event.using_economy",
			"false",
			"",
			"# When set to true, war results in economic losses when players are killed, or their townblocks lose their HP."),
	WAR_EVENT_TOWNS_NEUTRAL(
			"war.event.towns_are_neutral",
			"true",
			"",
			"#If false all towns not in nations can be attacked during a war event."),

	WAR_EVENT_PLOTS_HEADER("war.event.plots","",""),
	WAR_EVENT_PLOTS_HEALABLE(
			"war.event.plots.healable",
			"true",
			"",
			"# If true, nation members and allies can regen health on plots during war."),
	WAR_EVENT_PLOTS_FIREWORK_ON_ATTACKED(
			"war.event.plots.firework_on_attacked",
			"true",
			"",
			"# If true, fireworks will be launched at plots being attacked or healed in war every war tick."),

	WAR_EVENT_BLOCK_GRIEFING(
			"war.event.allow_block_griefing",
			"false",
			"",
			"# If enabled players will be able to break/place any blocks in enemy plots during a war.",
			"# This setting SHOULD NOT BE USED unless you want the most chaotic war possible.",
			"# The editable_materials list in the Warzone Block Permission section should be used instead."),

	WAR_EVENT_BLOCK_HP_HEADER(
			"war.event.block_hp",
			"",
			"",
			"# A townblock takes damage every 5 seconds that an enemy is stood in it."),
	WAR_EVENT_TOWN_BLOCK_HP("war.event.block_hp.town_block_hp", "60"),
	WAR_EVENT_HOME_BLOCK_HP("war.event.block_hp.home_block_hp", "120"),

	WAR_EVENT_ECO_HEADER("war.event.eco", "", ""),
	WAR_EVENT_TOWN_BLOCK_LOSS_PRICE(
			"war.event.eco.wartime_town_block_loss_price",
			"100.0",
			"",
			"# This amount is taken from the losing town for each plot lost."),
	WAR_EVENT_PRICE_DEATH(
			"war.event.eco.price_death_wartime",
			"200.0",
			"",
			"# This amount is taken from the player if they die during the event"),
	WAR_EVENT_COSTS_TOWNBLOCKS(
			"war.event.costs_townblocks",
			"false",
			"",
			"# If set to true when a town drops an enemy townblock's HP to 0, the attacking town gains a bonus townblock,",
			"# and the losing town gains a negative (-1) bonus townblock."),
	WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNBLOCKS(
			"war.event.winner_takes_ownership_of_townblocks",
			"false",
			"",
			"# If set to true when a town drops an enemy townblock's HP to 0, the attacking town takes full control of the townblock.",
			"# One available (bonus) claim is given to the victorious town, one available (bonus) claim is removed from the losing town.",
			"# Will not have any effect if war.event.winner_takes_ownership_of_town is set to true."),
	WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWN(
			"war.event.winner_takes_ownership_of_town",
			"false",
			"",
			"# If set to true when a town knocks another town out of the war, the losing town will join the winning town's nation.",
			"# The losing town will enter a conquered state and be unable to leave the nation until the conquered time has passed."),
	WAR_EVENT_WINNER_TAKES_OWNERSHIP_OF_TOWNS_EXCLUDES_CAPITALS(
			"war.event.winner_takes_ownership_of_town_excludes_nation_capitals",
			"true",
			"",
			"# If set to true, and winner_takes_ownership_of_town is also true, nation capitals will not switch nations.",
			"# If false, and winner_takes_ownership_of_town is also true, the losing nation will be deleted and the capital will change nations."),
	WAR_EVENT_CONQUER_TIME(
			"war.event.conquer_time",
			"7",
			"",
			"# Number of Towny new days until a conquered town loses its conquered status."),
	WAR_EVENT_TEAM_SELECTION_DELAY(
			"war.event.team_selection_time",
			"120",
			"",
			"# Number of seconds given to choose your team when a riot or civil war is being launched."),
	WAR_EVENT_POINTS_HEADER("war.event.points", "", ""),
	WAR_EVENT_POINTS_TOWNBLOCK("war.event.points.points_townblock", "1"),
	WAR_EVENT_POINTS_TOWN("war.event.points.points_town", "10"),
	WAR_EVENT_POINTS_NATION("war.event.points.points_nation", "100"),
	WAR_EVENT_POINTS_KILL("war.event.points.points_kill", "1"),
	WAR_EVENT_MIN_HEIGHT(
			"war.event.min_height",
			"60",
			"",
			"# The minimum height at which a player must stand to count as an attacker."),

	WAR_WAR_TYPES(
			"war.war_types",
			"",
			"",
			"",
			"############################################################",
			"# +------------------------------------------------------+ #",
			"# |                War Types Configuration               | #",
			"# |                                                      | #",
			"# |                  Used in Event Wars                  | #",
			"# +------------------------------------------------------+ #",
			"############################################################",
			""),
	WAR_WAR_TYPES_RIOT(
			"war.war_types.riot",
			"",
			"",
			"# Riot wars involve a single town. Details to be developed."),
	WAR_WAR_TYPES_RIOT_ENABLE(
			"war.war_types.riot.enabled",
			"false",
			"",
			"# Does the server allow riots?"
			),
	WAR_WAR_TYPES_RIOT_DELAY(
			"war.war_types.riot.startup_delay",
			"30",
			"",
			"# How many seconds of delay before a riot war begins?"
			),
	WAR_WAR_TYPES_RIOT_COOLDOWN(
			"war.war_types.riot.cooldown",
			"1d",
			"",
			"# How much time must pass between a town's last war (of any type) before a town can start a riot war?"
			),
	WAR_WAR_TYPES_RIOT_RESIDENT_LIVES(
			"war.war_types.riot.resident_lives",
			"5",
			"",
			"# How many lives do normal residents get, before they are removed from the war."),
	WAR_WAR_TYPES_RIOT_MAYOR_LIVES(
			"war.war_types.riot.mayor_lives",
			"5",
			"",
			"# How many lives do mayors get, before they are removed from the war."),
	WAR_WAR_TYPES_RIOT_MAYOR_DEATH(
			"war.war_types.riot.mayor_death",
			"false",
			"",
			"# Does a riot war end if the mayor is killed to the point they have no lives left?"
			),
	WAR_WAR_TYPES_RIOT_WINNER_TAKES_OVER_TOWN(
			"war.war_types.riot.winner_takes_over_town",
			"false",
			"",
			"# Does the highest score of the riot take over the town (if they were on the rebel side.)"),
	WAR_WAR_TYPES_RIOT_BASE_SPOILS(
			"war.war_types.riot.base_spoils",
			"10.0",
			"",
			"# How much money is automatically put into the war spoils at the beginning of a riot."),
	WAR_WAR_TYPES_RIOT_POINTS_PER_KILL(
			"war.war_types.riot.points_per_kill",
			"10",
			"",
			"# How many points are awarded for killing an enemy."),
	WAR_WAR_TYPES_RIOT_TOKEN_COST(
			"war.war_types.riot.token_cost",
			"20",
			"",
			"# How many tokens does it cost a town to purchase a riot Declaration of War."),
	
	WAR_WAR_TYPES_TOWN_WAR(
			"war.war_types.town_war",
			"",
			"",
			"# Town War involves one town versus another single town."),
	WAR_WAR_TYPES_TOWN_WAR_ENABLE(
			"war.war_types.town_war.enabled",
			"false",
			"",
			"# Does the server allow town wars?"),
	WAR_WAR_TYPES_TOWN_WAR_DELAY(
			"war.war_types.town_war.startup_delay",
			"30",
			"",
			"# How many seconds of delay before a town war begins?"
			),
	WAR_WAR_TYPES_TOWN_WAR_COOLDOWN(
			"war.war_types.town_war.cooldown",
			"2d",
			"",
			"# How much time must pass between a town's last war (of any type) before a town can join a town war?"
			),
	WAR_WAR_TYPES_TOWN_WAR_TOWNBLOCK_HP(
			"war.war_types.town_war.townblock_hp",
			"false",
			"",
			"# In town war, do the townblocks have HP which can be fought over?"),
	WAR_WAR_TYPES_TOWN_WAR_RESIDENT_LIVES(
			"war.war_types.town_war.resident_lives",
			"5",
			"",
			"# How many lives do normal residents get, before they are removed from the war."),
	WAR_WAR_TYPES_TOWN_WAR_MAYOR_LIVES(
			"war.war_types.town_war.mayor_lives",
			"5",
			"",
			"# How many lives do mayors get, before they are removed from the war."),
	WAR_WAR_TYPES_TOWN_WAR_MAYOR_DEATH(
			"war.war_types.town_war.mayor_death",
			"false",
			"",
			"# Does a town war end if one of the mayors is killed to the point they have no lives left?"),
	WAR_WAR_TYPES_TOWN_WAR_WINNER_TAKES_OVER_TOWN(
			"war.war_types.town_war.winner_takes_over_town",
			"false",
			"",
			"# Does the losing town disolve and become merged with the winning town?"),
	WAR_WAR_TYPES_TOWN_WAR_BASE_SPOILS(
			"war.war_types.town_war.base_spoils",
			"100.0",
			"",
			"# How much money is automatically put into the war spoils at the beginning of a town war."),
	WAR_WAR_TYPES_TOWN_WAR_POINTS_PER_KILL(
			"war.war_types.town_war.points_per_kill",
			"10",
			"",
			"# How many points are awarded for killing an enemy."),
	WAR_WAR_TYPES_TOWN_WAR_TOKEN_COST(
			"war.war_types.town_war.token_cost",
			"45",
			"",
			"# How many tokens does it cost a town to purchase a town war Declaration of War."),
	
	WAR_WAR_TYPES_CIVIL_WAR(
			"war.war_types.civil_war",
			"",
			"",
			"# Civil War involves all of the towns in a nation."),
	WAR_WAR_TYPES_CIVIL_WAR_ENABLE(
			"war.war_types.civil_war.enabled",
			"false",
			"",
			"# Does the server allow civil wars?"),	
	WAR_WAR_TYPES_CIVIL_WAR_DELAY(
			"war.war_types.civil_war.startup_delay",
			"30",
			"",
			"# How many seconds of delay before a civil war begins?"
			),
	WAR_WAR_TYPES_CIVIL_WAR_COOLDOWN(
			"war.war_types.civil_war.cooldown",
			"3d",
			"",
			"# How much time must pass between a town's last war (of any type) before a town can join a civil war?"
			),
	WAR_WAR_TYPES_CIVIL_WAR_TOWNBLOCK_HP(
			"war.war_types.civil_war.townblock_hp",
			"true",
			"",
			"# In town war, do the townblocks have HP which can be fought over?"),
	WAR_WAR_TYPES_CIVIL_WAR_RESIDENT_LIVES(
			"war.war_types.civil_war.resident_lives",
			"5",
			"",
			"# How many lives do normal residents get, before they are removed from the war."),
	WAR_WAR_TYPES_CIVIL_WAR_MAYOR_LIVES(
			"war.war_types.civil_war.mayor_lives",
			"5",
			"",
			"# How many lives do mayors get, before they are removed from the war."),
	WAR_WAR_TYPES_CIVIL_WAR_MAYOR_DEATH(
			"war.war_types.civil_war.mayor_death",
			"false",
			"",
			"# Does a town get removed from the civil war if their mayor is killed to the point they have no lives left?"),
	WAR_WAR_TYPES_CIVIL_WAR_WINNER_TAKES_OVER_NATION(
			"war.war_types.civil_war.winner_takes_over_nation",
			"true",
			"",
			"# Does the winning town take over as capital of the nation? The winning town has to have been on the side of the rebels."),
	WAR_WAR_TYPES_CIVIL_WAR_BASE_SPOILS(
			"war.war_types.civil_war.base_spoils",
			"500.0",
			"",
			"# How much money is automatically put into the war spoils at the beginning of a civil war."),
	WAR_WAR_TYPES_CIVIL_WAR_POINTS_PER_KILL(
			"war.war_types.civil_war.points_per_kill",
			"10",
			"",
			"# How many points are awarded for killing an enemy."),
	WAR_WAR_TYPES_CIVIL_WAR_TOKEN_COST(
			"war.war_types.civil_war.token_cost",
			"90",
			"",
			"# How many tokens does it cost a town to purchase a civil war Declaration of War."),
	
	WAR_WAR_TYPES_NATION_WAR(
			"war.war_types.nation_war",
			"",
			"",
			"# Nation War involves one nation versus another single nation."),
	WAR_WAR_TYPES_NATION_WAR_ENABLE(
			"war.war_types.nation_war.enabled",
			"false",
			"",
			"# Does the server allow nation wars?"),
	WAR_WAR_TYPES_NATION_WAR_DELAY(
			"war.war_types.nation_war.startup_delay",
			"30",
			"",
			"# How many seconds of delay before a nation war begins?"
			),
	WAR_WAR_TYPES_NATION_WAR_COOLDOWN(
			"war.war_types.nation_war.cooldown",
			"5d",
			"",
			"# How much time must pass between a town's last war (of any type) before a town can join a nation war?"
			),
	WAR_WAR_TYPES_NATION_WAR_TOWNBLOCK_HP(
			"war.war_types.nation_war.townblock_hp",
			"true",
			"",
			"# In nation war, do the townblocks have HP which can be fought over?"),
	WAR_WAR_TYPES_NATION_WAR_RESIDENT_LIVES(
			"war.war_types.nation_war.resident_lives",
			"5",
			"",
			"# How many lives do normal residents get, before they are removed from the war."),
	WAR_WAR_TYPES_NATION_WAR_MAYOR_LIVES(
			"war.war_types.nation_war.mayor_lives",
			"5",
			"",
			"# How many lives do mayors get, before they are removed from the war."),
	WAR_WAR_TYPES_NATION_WAR_MAYOR_DEATH(
			"war.war_types.nation_war.mayor_death",
			"true",
			"",
			"# Does a town get removed from the nation war if their mayor is killed to the point they have no lives left?",
			"# Does a nation get removed from the nation war if their king is killed to the point they have no lives left?"),
	WAR_WAR_TYPES_NATION_WAR_WINNER_CONQUERS_TOWNS(
			"war.war_types.nation_war.winner_conquers_towns",
			"true",
			"",
			"# Does the winning nation conquer towns which are knocked out of the war?"),
	WAR_WAR_TYPES_NATION_WAR_BASE_SPOILS(
			"war.war_types.nation_war.base_spoils",
			"1000.0",
			"",
			"# How much money is automatically put into the war spoils at the beginning of a nation war."),
	WAR_WAR_TYPES_NATION_WAR_POINTS_PER_KILL(
			"war.war_types.nation_war.points_per_kill",
			"10",
			"",
			"# How many points are awarded for killing an enemy."),
	WAR_WAR_TYPES_NATION_WAR_TOKEN_COST(
			"war.war_types.nation_war.token_cost",
			"150",
			"",
			"# How many tokens does it cost a nation to purchase a nation war Declaration of War."),

	WAR_WAR_TYPES_WORLD_WAR(
			"war.war_types.world_war",
			"",
			"",
			"# World War potentially involves every town and nation on the server."),
	WAR_WAR_TYPES_WORLD_WAR_ENABLE(
			"war.war_types.world_war.enabled",
			"false",
			"",
			"# Does the server allow world wars?"),
	WAR_WAR_TYPES_WORLD_WAR_DELAY(
			"war.war_types.world_war.startup_delay",
			"120",
			"",
			"# How many seconds of delay before a world war begins?"
			),
	WAR_WAR_TYPES_WORLD_WAR_COOLDOWN(
			"war.war_types.world_war.cooldown",
			"7d",
			"",
			"# How much time must pass between a town's last war (of any type) before a town can join a world war?"
			),
	WAR_WAR_TYPES_WORLD_WAR_TOWNBLOCK_HP(
			"war.war_types.world_war.townblock_hp",
			"true",
			"",
			"# In world war, do the townblocks have HP which can be fought over?"),
	WAR_WAR_TYPES_WORLD_WAR_RESIDENT_LIVES(
			"war.war_types.world_war.resident_lives",
			"5",
			"",
			"# How many lives do normal residents get, before they are removed from the war."),
	WAR_WAR_TYPES_WORLD_WAR_MAYOR_LIVES(
			"war.war_types.world_war.mayor_lives",
			"5",
			"",
			"# How many lives do mayors get, before they are removed from the war."),
	WAR_WAR_TYPES_WORLD_WAR_MAYOR_DEATH(
			"war.war_types.world_war.mayor_death",
			"true",
			"",
			"# Does a town get removed from the world war if their mayor is killed to the point they have no lives left?",
			"# Does a nation get removed from the world war if their king is killed to the point they have no lives left?"),
	WAR_WAR_TYPES_WORLD_WAR_WINNER_CONQUERS_TOWNS(
			"war.war_types.world_war.winner_conquers_towns",
			"true",
			"",
			"# Does the winning nation conquer towns which are knocked out of the war?"),
	WAR_WAR_TYPES_WORLD_WAR_BASE_SPOILS(
			"war.war_types.world_war.base_spoils",
			"10000",
			"",
			"# How much money is automatically put into the war spoils at the beginning of a world war."),
	WAR_WAR_TYPES_WORLD_WAR_POINTS_PER_KILL(
			"war.war_types.world_war.points_per_kill",
			"10",
			"",
			"# How many points are awarded for killing an enemy."),
	WAR_WAR_TYPES_WORLD_WAR_TOKEN_COST(
			"war.war_types.town_war.token_cost",
			"300",
			"",
			"# How many tokens does it cost a nation to purchase a world war Declaration of War."),

	WAR_WARZONE(
			"war.warzone",
			"",
			"",
			"",
			"############################################################",
			"# +------------------------------------------------------+ #",
			"# |              Warzone Block Permissions               | #",
			"# |                                                      | #",
			"# |                  Used in Event Wars                  | #",
			"# +------------------------------------------------------+ #",
			"############################################################",
			""),
	WAR_WARZONE_EDITABLE_MATERIALS(
			"war.warzone.editable_materials",
			"tnt,oak_fence,birch_fence,spruce_fence,jungle_fence,dark_oak_fence,acacia_fence,ladder,oak_door,birch_door,spruce_door,jungle_door,dark_oak_door,acacia_fence,iron_door,fire",
			"",
			"# List of materials that can be modified in a warzone.",
			"# '*' = Allow all materials.",
			"# Prepend a '-' in front of a material to remove it. Used in conjunction with when you use '*'.",
			"# Eg: '*,-chest,-furnace'"),
	WAR_WARZONE_ITEM_USE("war.warzone.item_use", "true"),
	WAR_WARZONE_SWITCH("war.warzone.switch", "true"),
	WAR_WARZONE_FIRE(
			"war.warzone.fire",
			"true",
			"",
			"# Add '-fire' to editable materials for complete protection when setting is false. This prevents fire to be created and spread."),
	WAR_WARZONE_EXPLOSIONS("war.warzone.explosions", "true"),
	WAR_WARZONE_EXPLOSIONS_BREAK_BLOCKS(
			"war.warzone.explosions_break_blocks",
			"true"),
	WAR_WARZONE_EXPLOSIONS_REGEN_BLOCKS(
			"war.warzone.explosions_regen_blocks",
			"true",
			"",
			"# Only under affect when explosions_break_blocks is true."),
	WAR_WARZONE_EXPLOSIONS_IGNORE_LIST(
			"war.warzone.explosions_ignore_list",
			"WOODEN_DOOR,ACACIA_DOOR,DARK_OAK_DOOR,JUNGLE_DOOR,BIRCH_DOOR,SPRUCE_DOOR,IRON_DOOR,CHEST,TRAPPED_CHEST,FURNACE,BURNING_FURNACE,DROPPER,DISPENSER,HOPPER,ENDER_CHEST,WHITE_SHULKER_BOX,ORANGE_SHULKER_BOX,MAGENTA_SHULKER_BOX,LIGHT_BLUE_SHULKER_BOX,YELLOW_SHULKER_BOX,LIME_SHULKER_BOX,PINK_SHULKER_BOX,GRAY_SHULKER_BOX,SILVER_SHULKER_BOX,CYAN_SHULKER_BOX,PURPLE_SHULKER_BOX,BLUE_SHULKER_BOX,BROWN_SHULKER_BOX,GREEN_SHULKER_BOX,RED_SHULKER_BOX,BLACK_SHULKER_BOX,NOTE_BLOCK,LEVER,STONE_PLATE,IRON_DOOR_BLOCK,WOOD_PLATE,JUKEBOX,DIODE_BLOCK_OFF,DIODE_BLOCK_ON,FENCE_GATE,GOLD_PLATE,IRON_PLATE,REDSTONE_COMPARATOR_OFF,REDSTONE_COMPARATOR_ON,BEACON",
			"",
			"# A list of blocks that will not be exploded, mostly because they won't regenerate properly.",
			"# These blocks will also protect the block below them, so that blocks like doors do not dupe themselves.",
			"# Only under affect when explosions_break_blocks is true.");
	
	private final String Root;
	private final String Default;
	private String[] comments;

	EventWarConfigNodes(String root, String def, String... comments) {

		this.Root = root;
		this.Default = def;
		this.comments = comments;
	}

	/**
	 * Retrieves the root for a config option
	 *
	 * @return The root for a config option
	 */
	public String getRoot() {

		return Root;
	}

	/**
	 * Retrieves the default value for a config path
	 *
	 * @return The default value for a config path
	 */
	public String getDefault() {

		return Default;
	}

	/**
	 * Retrieves the comment for a config path
	 *
	 * @return The comments for a config path
	 */
	public String[] getComments() {

		if (comments != null) {
			return comments;
		}

		String[] comments = new String[1];
		comments[0] = "";
		return comments;
	}

}
