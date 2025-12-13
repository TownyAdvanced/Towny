package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum HelpMenu {
	
	GENERAL_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder()
				.addTitle(Translation.of("help_0"))
				.add(Translatable.of("help_1"))
				.add("/resident", "?", Translatable.of("help_4"))
				.add("/town", "?", Translatable.of("help_5"))
				.add("/nation", "?", Translatable.of("help_6"))
				.add("/plot", "?", Translatable.of("help_7"))
				.add("/towny", "?", Translatable.of("help_8"))
				.add("/tc", "[msg]", Translatable.of("help_2"))
				.add("/nc", "[msg]", Translatable.of("help_3"));
		}
	},

	GENERAL_HELP_ADMIN {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder()
				.addTitle(Translation.of("help_0"))
				.add(Translatable.of("help_1"))
				.add("/resident", "?", Translatable.of("help_4"))
				.add("/town", "?", Translatable.of("help_5"))
				.add("/nation", "?", Translatable.of("help_6"))
				.add("/plot", "?", Translatable.of("help_7"))
				.add("/towny", "?", Translatable.of("help_8"))
				.add("/tc", "[msg]", Translatable.of("help_2"))
				.add("/nc", "[msg]", Translatable.of("help_3"))
				.add(Translation.of("admin_sing"), "/townyadmin", "?", Translatable.of("help_9"));
		}
	},

	// Towny Help
	HELP {
		@Override
		public MenuBuilder load() {
			return new MenuBuilder("towny", Translatable.of("towny_help_0"))
				.add("map", Translatable.of("towny_help_1"))
				.add("prices", Translatable.of("towny_help_2"))
				.add("top", Translatable.of("towny_help_3"))
				.add("time", Translatable.of("towny_help_4"))
				.add("universe", Translatable.of("towny_help_5"))
				.add("v", Translatable.of("towny_help_6"));
		}
	},

	TOWNY_TOP_HELP {
		@Override
		public MenuBuilder load() {
			return new MenuBuilder("towny top")
				.add("residents", "[all/town/nation]", Translatable.of("towny_top_help_0"))
				.add("land", " [all/resident/town]", Translatable.of("towny_top_help_1"))
				.add("balance", " [all/town/nation]", Translatable.of("towny_top_help_2"));
		}
	},

	TA_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin")
				.add("set [] .. []", Translatable.of("admin_panel_1"))
				.add("unclaim [radius]", Translatable.of("admin_panel_4"))
				.add("plot", Translatable.of("admin_panel_5"))
				.add("givebonus [town/player] [num]", Translatable.of("admin_panel_6"))
				.add("toggle", Translatable.of("admin_panel_7"))
				.add("resident/town/nation", Translatable.of("admin_panel_8"))
				.add("tpplot {world} {x} {z}", Translatable.of("admin_panel_9"))
				.add("checkperm {name} {node}", Translatable.of("admin_panel_10"))
				.add("reload", Translatable.of("admin_panel_2"))
				.add("reset", Translatable.of("admin_panel_11"))
				.add("backup", Translatable.of("admin_panel_12"))
				.add("mysqldump", Translatable.of("admin_panel_13"))
				.add("database [save/load]", Translatable.of("admin_panel_14"))
				.add("newday", Translatable.of("admin_panel_3"))
				.add("newhour", Translatable.of("admin_panel_15"))
				.add("purge [number of days]", Translatable.of("admin_panel_16"));
		}
	},
	
	TA_TOWN {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin town")
				.add("new [name] [mayor]", Translatable.of("townyadmin_town_help_0"))
				.add("[town]", Translatable.of("townyadmin_town_help_1"))
				.add("[town] add/kick [] .. []", Translatable.of("townyadmin_town_help_2"))
				.add("[town] rename [newname]", Translatable.of("townyadmin_town_help_3"))
				.add("[town] delete", Translatable.of("townyadmin_town_help_4"))
				.add("[town] spawn", Translatable.of("townyadmin_town_help_5"))
				.add("[town] outpost #", Translatable.of("townyadmin_town_help_6"))
				.add("[town] rank", Translatable.of("townyadmin_town_help_7"))
				.add("[town] set", Translatable.of("townyadmin_town_help_8"))
				.add("[town] toggle", Translatable.of("townyadmin_town_help_9"))
				.add("[town] meta", Translatable.of("townyadmin_town_help_10"))
				.add("[town] merge [townname]", Translatable.of("townyadmin_town_help_11"))
				.add("[town] forcemerge [townname]", Translatable.of("townyadmin_town_help_12"))
				.add("[town] deposit [amount]", Translatable.of("townyadmin_town_help_13"))
				.add("[town] withdraw [amount]", Translatable.of("townyadmin_town_help_14"))
				.add("[town] bankhistory", Translatable.of("townyadmin_town_help_15"))
				.add("[town] outlaw [add|remove] [name]", Translatable.of("townyadmin_town_help_16"))
				.add("[town] leavenation", Translatable.of("townyadmin_town_help_17"))
				.add("[town] conquered", Translatable.of("townyadmin_town_help_18"));
		}
	},

	TA_TOWN_OUTLAW {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta town [town] outlaw")
				.add("[add|remove] [name]", Translatable.of("townyadmin_town_help_16"));
		}
	},
	TA_TOWN_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta town [town] set")
				.add("foundingdate [unix-timestamp]", Translatable.of("townyadmin_town_set_help_0"))
				.add("board [message ... ]", Translatable.of("town_set_help_0"))
				.add("mayor " + Translation.of("town_help_2"), Translatable.of("ta_set_help_0"))
				.add("homeblock", Translatable.of("town_set_help_1"))
				.add("spawn/outpost", Translatable.of("town_set_help_2"))
				.add("perm ...", Translatable.of("town_set_help_3"))
				.add("taxes [$]", Translatable.of("town_set_help_4"))
				.add("[plottax/shoptax/embassytax] [$]", Translatable.of("town_set_help_5"))
				.add("[plotprice/shopprice/embassyprice] [$]", Translatable.of("town_set_help_6"))
				.add("spawncost [$]", Translatable.of("town_set_help_7"))
				.add("name [name]", Translatable.of("town_set_help_8"))
				.add("tag [upto 4 letters] or clear", Translatable.of("town_set_help_9"))
				.add("title/surname [resident] [text]", Translatable.of("town_set_help_10"))
				.add("taxpercentcap [amount]", Translatable.of("town_set_help_11"));
		}
	},
	
	TA_TOWN_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta town {townname} toggle")
				.add("pvp", Translatable.of("townyadmin_town_toggle_help_0"))
				.add("forcepvp", Translatable.of("townyadmin_town_toggle_help_1"))
				.add("public", Translatable.of("townyadmin_town_toggle_help_2"))
				.add("explosion", Translatable.of("townyadmin_town_toggle_help_3"))
				.add("fire", Translatable.of("townyadmin_town_toggle_help_4"))
				.add("mobs", Translatable.of("townyadmin_town_toggle_help_5"))
				.add("taxpercent", Translatable.of("townyadmin_town_toggle_help_6"))
				.add("open", Translatable.of("townyadmin_town_toggle_help_7"));
		}
	},

	
	TA_NATION {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin nation")
				.add("new [name] [capital]", Translatable.of("townyadmin_nation_help_0"))
				.add("[nation]", Translatable.of("townyadmin_nation_help_1"))
				.add("[nation] add [] .. []", Translatable.of("townyadmin_nation_help_2"))
				.add("[nation] kick [] .. []", Translatable.of("townyadmin_nation_help_3"))
				.add("[nation] rename [newname]", Translatable.of("townyadmin_nation_help_4"))
				.add("[nation] delete", Translatable.of("townyadmin_nation_help_5"))
				.add("[nation] recheck", Translatable.of("townyadmin_nation_help_6"))
				.add("[nation] merge [nationname]", Translatable.of("townyadmin_nation_help_7"))
				.add("[nation] forcemerge [nationname]", Translatable.of("townyadmin_nation_help_8"))
				.add("[nation] toggle", Translatable.of("townyadmin_nation_help_9"))
				.add("[nation] set", Translatable.of("townyadmin_nation_help_10"))
				.add("[nation] deposit [amount]", Translatable.of("townyadmin_nation_help_11"))
				.add("[nation] withdraw [amount]", Translatable.of("townyadmin_nation_help_12"))
				.add("[nation] bankhistory", Translatable.of("townyadmin_nation_help_13"))
				.add("[nation] transfer [townname]", Translatable.of("townyadmin_nation_help_14"))
				.add("rank [add/remove] [resident] [rank]", Translatable.of("townyadmin_nation_help_15"));
		}
	}, 
	
	TA_NATION_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta nation [nation] set")
				.add("foundingdate [unix-timestamp]", Translatable.of("townyadmin_nation_set_help_0"))
				.add("new [name] [capital]", Translatable.of("townyadmin_nation_help_0"))
				.add("[nation]", Translatable.of("townyadmin_nation_help_1"))
				.add("[nation] add [] .. []", Translatable.of("townyadmin_nation_help_2"))
				.add("[nation] kick [] .. []", Translatable.of("townyadmin_nation_help_3"))
				.add("[nation] rename [newname]", Translatable.of("townyadmin_nation_help_4"))
				.add("[nation] delete", Translatable.of("townyadmin_nation_help_5"))
				.add("[nation] recheck", Translatable.of("townyadmin_nation_help_6"))
				.add("[nation] merge [nationname]", Translatable.of("townyadmin_nation_help_7"))
				.add("[nation] forcemerge [nationname]", Translatable.of("townyadmin_nation_help_8"))
				.add("[nation] toggle", Translatable.of("townyadmin_nation_help_9"))
				.add("[nation] set", Translatable.of("townyadmin_nation_help_10"))
				.add("[nation] deposit [amount]", Translatable.of("townyadmin_nation_help_11"))
				.add("[nation] withdraw [amount]", Translatable.of("townyadmin_nation_help_12"))
				.add("[nation] bankhistory", Translatable.of("townyadmin_nation_help_13"))
				.add("[nation] transfer [townname]", Translatable.of("townyadmin_nation_help_14"))
				.add("rank [add/remove] [resident] [rank]", Translatable.of("townyadmin_nation_help_15"));
		}
	},
	
	TA_NATION_RANK {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta nation [nation] rank")
				.add("add [resident] [rank]", Translatable.of("townyadmin_nationrank_help_0"))
				.add("remove [resident] [rank]", Translatable.of("townyadmin_nationrank_help_1"));
		}
	},

	TA_UNCLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin unclaim", Translation.of("admin_sing"),
				Translatable.of("townyadmin_help_1"))
				.add("[radius]", Translatable.of("townyadmin_help_2"));
		}
	},
	
	TA_DATABASE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin database")
				.add("save", Translatable.of("townyadmin_database_help_0"))
				.add("load", Translatable.of("townyadmin_database_help_1"))
				.add("remove titles", Translatable.of("townyadmin_database_help_2"));
		}
	},
	
	TA_PLOT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin plot")
				.add("claim [player]", Translatable.of("ta_plot_help_0"))
				.add("meta", Translatable.of("ta_plot_help_1"))
				.add("meta set [key] [value]", Translatable.of("ta_plot_help_2"))
				.add("meta [add|remove] [key]", Translatable.of("ta_plot_help_3"));
		}
	},
	
	TA_RESIDENT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin resident")
				.add("[resident]", Translatable.of("res_3"))
				.add("[resident] about clear", Translatable.of("ta_resident_help_4"))
				.add("[resident] rename [newname]", Translatable.of("ta_resident_help_0"))
				.add("[resident] friend... [add|remove] [resident]", Translatable.of("ta_resident_help_1"))
				.add("[resident] friend... [list|clear]", Translatable.of("ta_resident_help_2"))
				.add("[resident] delete", Translatable.of("ta_resident_help_3"));
		}
	},

	TA_RESIDENT_FRIEND {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin resident [resident] friend")
				.add("[add|remove] [resident]", Translatable.of("ta_resident_help_1"))
				.add("list|clear", Translatable.of("ta_resident_help_2"));
		}
	},
	
	TA_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin toggle")
				.add("wildernessuse", Translatable.of("ta_toggle_help_0"))
				.add("regenerations", Translatable.of("ta_toggle_help_1"))
				.add("devmode", Translatable.of("ta_toggle_help_2"))
				.add("debug", Translatable.of("ta_toggle_help_3"))
				.add("townwithdraw/nationwithdraw", Translatable.of("ta_toggle_help_4"))
				.add("npc [resident]", Translatable.of("ta_toggle_help_5"));
		}
	},
	
	TA_TOWNYPERMS {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("ta townyperms")
				.add("listgroups", Translatable.of("help_ta_perms_listgroups"))
				.add("group [group]", Translatable.of("help_ta_perms_group"))
				.add("group [group] addperm [node]", Translatable.of("help_ta_perms_groupaddpermnode"))
				.add("group [group] removeperm [node]", Translatable.of("help_ta_perms_groupremovepermnode"))
				.add("townrank addrank [rank]", Translatable.of("help_ta_perms_townrankadd"))
				.add("townrank removerank [rank]", Translatable.of("help_ta_perms_townrankremove"))
				.add("townrank renamerank [oldrank] [newrank]", Translatable.of("help_ta_perms_townrankrename"))
				.add("nationrank addrank [rank]", Translatable.of("help_ta_perms_nationrankadd"))
				.add("nationrank removerank [rank]", Translatable.of("help_ta_perms_nationrankremove"))
				.add("nationrank renamerank [oldrank] [newrank]", Translatable.of("help_ta_perms_nationrankrename"));
		}
	},
	
	TA_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set")
				.add("mayor [town] " + Translatable.of("town_help_2"), Translatable.of("ta_set_help_0"))
				.add("mayor [town] npc", Translatable.of("ta_set_help_1"))
				.add("capital [town] [nation]", Translatable.of("ta_set_help_2"))
				.add("nationzoneoverride [town name] [size]", Translatable.of("ta_set_help_3"))
				.add("title [resident] [title]", Translatable.of("ta_set_help_4"))
				.add("surname [resident] [surname]", Translatable.of("ta_set_help_5"))
				.add("plot [town]", Translatable.of("ta_set_help_6"))
				.add("founder [town] foundername", Translatable.of("ta_set_help_7"));
		}
	},
	
	TA_SET_MAYOR {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set mayor")
				.add("[town] " + Translatable.of("town_help_2"), Translatable.of("ta_set_help_0"))
				.add("[town] npc", Translatable.of("ta_set_help_1"));
		}
	},

	TA_SET_CAPITAL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set capital")
				.add("[town] [nation]", Translatable.of("ta_set_help_2"));
		}
	},

	TA_SET_FOUNDER {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set founder")
				.add("[town] [foundername]", Translatable.of("ta_set_help_7"));
		}
	},

	TA_SET_PLOT {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set plot")
				.add("[town]",  Translatable.of("msg_admin_set_plot_help_1"))
				.add("[town name] {rect|circle} {radius}", Translatable.of("msg_admin_set_plot_help_2"))
				.add("[town name] {rect|circle} auto", Translatable.of("msg_admin_set_plot_help_2"));
		}
	},
	
	TA_SET_NATIONZONE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin set nationzoneoverride")
				.add("[town name] [size]", Translatable.of("ta_set_help_3"))
				.add("[town name] 0", Translatable.of("ta_set_help_8"));
		}
	},
	
	TA_PURGE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin purge")
				.add("[number of days] {townless|townname}", Translatable.of("ta_purge_help_0"));
		}
	},

	TA_NATION_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin nation [nationname] meta")
				.add("", Translatable.of("ta_nationmeta_help_1"))
				.add("set [key] [value]", Translatable.of("ta_nationmeta_help_2"))
				.add("add|remove [key]", Translatable.of("ta_nationmeta_help_3"));
		}
	},
	
	TA_TOWN_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin town [townname] meta")
				.add("", Translatable.of("ta_townmeta_help_1"))
				.add("set [key] [value]", Translatable.of("ta_townmeta_help_2"))
				.add("add|remove [key]", Translatable.of("ta_townmeta_help_3"));
		}
	},

	TA_RESIDENT_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin resident [residentname] meta")
				.add("", Translatable.of("ta_residentmeta_help_1"))
				.add("set [key] [value]", Translatable.of("ta_residentmeta_help_2"))
				.add("add|remove [key]", Translatable.of("ta_residentmeta_help_3"));
		}
	},

	TA_PLOT_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin plot meta")
				.add("", Translatable.of("ta_plot_help_1"))
				.add("set [key] [value]", Translatable.of("ta_plot_help_2"))
				.add("add|remove [key]", Translatable.of("ta_plot_help_3"));
		}
	},
	
	TA_RELOAD {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin reload")
				.add("database", Translatable.of("ta_reload_help_0"))
				.add("config", Translatable.of("ta_reload_help_1"))
				.add("lang", Translatable.of("ta_reload_help_2"))
				.add("perms", Translatable.of("ta_reload_help_3"))
				.add("all", Translatable.of("ta_reload_help_4"));
		}
	},

	TA_ECO {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin eco")
				.add("resetbanks {amount}", Translatable.of("ta_eco_resetbanks_help"))
				.add("depositall [amount]", Translatable.of("ta_depositall_help_0"))
				.add("depositalltowns [amount]", Translatable.of("ta_depositall_help_1"))
				.add("depositallnations [amount]", Translatable.of("ta_depositall_help_2"))
				.add("convert modern", Translatable.of("ta_eco_convert_modern_help"))
				.add("convert [economy]", Translatable.of("ta_eco_convert_help"))
				.add("info ?", Translatable.of("ta_eco_info_help"));
		}
	},

	TA_DEPOSITALL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin eco depositall")
				.add("[amount]", Translatable.of("ta_depositall_help_0"));
		}
	},

	TA_DEPOSITALLTOWNS {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin eco depositalltowns")
				.add("[amount]", Translatable.of("ta_depositall_help_1"));
		}
	},

	TA_DEPOSITALLNATIONS {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin eco depositallnations")
				.add("[amount]", Translatable.of("ta_depositall_help_2"));
		}
	},

	TA_ECO_INFO {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin eco info")
				.add("nation [nationname]", Translatable.of("ta_info_help_0"))
				.add("resident [residentname]", Translatable.of("ta_info_help_1"))
				.add("serveraccount", Translatable.of("ta_info_help_2"))
				.add("town [townname]", Translatable.of("ta_info_help_3"));
		}
	},

	TOWNYWORLD_HELP {
		@Override
		protected MenuBuilder load(MenuBuilder builder) {
			return builder
				.add(Translation.of("world_help_2"), Translatable.of("world_help_3"))
				.add("list", Translatable.of("world_help_4"))
				.add("toggle", Translatable.of("world_help_6"))
				.add(Translation.of("admin_sing"), "set [] .. []", Translatable.of("world_help_7"));
		}

		@Override
		protected MenuBuilder load() {
			return load(new MenuBuilder("townyworld", Translatable.of("world_help_1")));
		}
	},

	TOWNYWORLD_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return TOWNYWORLD_HELP.load(new MenuBuilder("townyworld {world}", Translatable.of("world_help_1")));
		}
	},

	TOWNYWORLD_SET {
		@Override
		protected MenuBuilder load(MenuBuilder builder) {
			return builder.add("wildname [name]", Translatable.of("world_set_help_0"));
		}

		@Override
		protected MenuBuilder load() {
			return load(new MenuBuilder("townyworld set"));
		}
	},

	TOWNYWORLD_SET_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return TOWNYWORLD_SET.load(new MenuBuilder("townyworld set {world}"));
		}
	},
	
	TOWNYWORLD_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyworld toggle")
				.add("claimable", Translatable.of("world_toggle_help_0"))
				.add("usingtowny", Translatable.of("world_toggle_help_1"))
				.add("warallowed", Translatable.of("world_toggle_help_2"))
				.add("pvp/forcepvp", Translatable.of("world_toggle_help_3"))
				.add("friendlyfire", Translatable.of("world_toggle_help_4"))
				.add("explosion/forceexplosion", Translatable.of("world_toggle_help_5"))
				.add("fire/forcefire", Translatable.of("world_toggle_help_6"))
				.add("townmobs/wildernessmobs/worldmobs", Translatable.of("world_toggle_help_7"))
				.add("revertunclaim", Translatable.of("world_toggle_help_8"))
				.add("revertentityexpl/revertblockexpl", Translatable.of("world_toggle_help_9"))
				.add("plotcleardelete", Translatable.of("world_toggle_help_10"))
				.add("unclaimblockdelete", Translatable.of("world_toggle_help_11"))
				.add("jailing", Translatable.of("world_toggle_help_12"));
		}
	},

	TOWNYWORLD_TOGGLE_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyworld {worldname} toggle")
				.add("claimable", Translatable.of("world_toggle_help_0"))
				.add("usingtowny", Translatable.of("world_toggle_help_1"))
				.add("warallowed", Translatable.of("world_toggle_help_2"))
				.add("pvp/forcepvp", Translatable.of("world_toggle_help_3"))
				.add("friendlyfire", Translatable.of("world_toggle_help_4"))
				.add("explosion/forceexplosion", Translatable.of("world_toggle_help_5"))
				.add("fire/forcefire", Translatable.of("world_toggle_help_6"))
				.add("townmobs/wildernessmobs/worldmobs", Translatable.of("world_toggle_help_7"))
				.add("revertunclaim", Translatable.of("world_toggle_help_8"))
				.add("revertentityexpl/revertblockexpl", Translatable.of("world_toggle_help_9"))
				.add("plotcleardelete", Translatable.of("world_toggle_help_10"))
				.add("unclaimblockdelete", Translatable.of("world_toggle_help_11"))
				.add("jailing", Translatable.of("world_toggle_help_12"));
		}
	},
	
	TOWN_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town", Translatable.of("town_help_1"))
				.add("[town]", Translatable.of("town_help_3"))
				.add("new [name]", Translatable.of("town_help_12"))
				.add("here", Translatable.of("town_help_4"))
				.add("list", Translatable.of("town_help_26"))
				.add("nearby", Translatable.of("town_help_36"))
				.add("online", Translatable.of("town_help_10"))
				.add("leave", Translatable.of("town_help_27"))
				.add("reclaim", Translatable.of("town_help_12"))
				.add("reslist (town)", Translatable.of("town_help_13"))
				.add("ranklist (town)", Translatable.of("town_help_14"))
				.add("outlawlist (town)", Translatable.of("town_help_15"))
				.add("plotgrouplist (town) (page)", Translatable.of("town_help_16"))
				.add("plots (town)", Translatable.of("town_help_17"))
				.add("outlaw add/remove [name]", Translatable.of("town_help_25"))
				.add("say", "[message]", Translatable.of("town_help_18"))
				.add("spawn", Translatable.of("town_help_5"))
				.add("forsale [$]", Translatable.of("town_help_19"))
				.add("notforsale [$]", Translatable.of("town_help_20"))
				.add("buytown (town)", Translatable.of("town_help_21"))
				.add(Translation.of("res_sing"), "deposit [$]", Translatable.of("town_help_22"))
				.add(Translation.of("res_sing"), "rank add/remove [resident] [rank]", Translatable.of("town_help_23"))
				.add(Translation.of("mayor_sing"), "mayor ?", Translatable.of("town_help_8"))
				.add(Translation.of("admin_sing"), "delete [town]", Translatable.of("town_help_24"));
		}
	},
	
	TOWN_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town")
				.add("[town]", Translatable.of("town_help_3"))
				.add("list", Translatable.of("town_help_26"))
				.add("reslist [town]", Translatable.of("town_help_13"));
		}
	},
	
	TOWN_OUTLAW_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town outlaw")
				.add("add/remove [name]", Translatable.of("town_help_25"));
		}
	},
	
	TOWN_LIST {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town list")
				.add("{page #}", Translatable.of("town_list_help_0"))
				.add("{page #} by residents", Translatable.of("town_list_help_1"))
				.add("{page #} by open", Translatable.of("town_list_help_2"))
				.add("{page #} by balance", Translatable.of("town_list_help_3"))
				.add("{page #} by name", Translatable.of("town_list_help_4"))
				.add("{page #} by townblocks", Translatable.of("town_list_help_5"))
				.add("{page #} by online", Translatable.of("town_list_help_6"));
		}
	},

	TOWN_RANK {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town rank")
					.add("add|remove [resident] [rank]", Translatable.of("nation_help_17")); // This fits for the town command.
		}
	},
	
	TOWN_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town set")
				.add("board [message ... ]", Translatable.of("town_set_help_0"))
				.add("mayor " + Translation.of("town_help_2"), Translatable.of("ta_set_help_0"))
				.add("homeblock", Translatable.of("town_set_help_1"))
				.add("spawn/outpost", Translatable.of("town_set_help_2"))
				.add("perm ...", Translatable.of("town_set_help_3"))
				.add("taxes [$]", Translatable.of("town_set_help_4"))
				.add("[plottax/shoptax/embassytax] [$]", Translatable.of("town_set_help_5"))
				.add("[plotprice/shopprice/embassyprice] [$]", Translatable.of("town_set_help_6"))
				.add("spawncost [$]", Translatable.of("town_set_help_7"))
				.add("name [name]", Translatable.of("town_set_help_8"))
				.add("tag [upto 4 letters] or clear", Translatable.of("town_set_help_9"))
				.add("title/surname [resident] [text]", Translatable.of("town_set_help_10"))
				.add("taxpercentcap [amount]", Translatable.of("town_set_help_11"));
		}
	},
	
	TOWN_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town toggle")
				.add("pvp", Translatable.of("townyadmin_town_toggle_help_0"))
				.add("public", Translatable.of("townyadmin_town_toggle_help_2"))
				.add("explosion", Translatable.of("townyadmin_town_toggle_help_3"))
				.add("fire", Translatable.of("townyadmin_town_toggle_help_4"))
				.add("mobs", Translatable.of("townyadmin_town_toggle_help_5"))
				.add("taxpercent", Translatable.of("townyadmin_town_toggle_help_6"))
				.add("open", Translatable.of("townyadmin_town_toggle_help_7"));
		}
	},

	TOWN_CEDE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town cede")
				.add("plot [townname]", Translatable.of("town_cede_help"));
		}
	},

	TOWN_CLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town claim")
				.add("", Translatable.of("msg_block_claim"))
				.add("outpost", Translatable.of("mayor_help_3"))
				.add("[auto]", Translatable.of("mayor_help_5"))
				.add("[circle/rect] [radius]", Translatable.of("mayor_help_4"))
				.add("[circle/rect] auto", Translatable.of("mayor_help_5"));
		}
	},
	
	TOWN_UNCLAIM {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town unclaim")
				.add("", Translatable.of("mayor_help_6"))
				.add("[circle/rect] [radius]", Translatable.of("mayor_help_7"))
				.add("all", Translatable.of("mayor_help_8"));
		}
	},
	
	TOWN_JAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town jail")
				.add("list", Translatable.of("town_jail_help_0"))
				.add("[resident]", Translatable.of("town_jail_help_1"))
				.add("[resident] [hours]", Translatable.of("town_jail_help_2"))
				.add("[resident] [hours] [jail]", Translatable.of("town_jail_help_3"))
				.add("[resident] [hours] [jail] [cell]", Translatable.of("town_jail_help_4"));
		}
	},

	TOWN_JAILWITHBAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town jail")
				.add("list", Translatable.of("town_jail_help_0"))
				.add("[resident]", Translatable.of("town_jail_help_1"))
				.add("[resident] [hours]", Translatable.of("town_jail_help_2"))
				.add("[resident] [hours] [bail]", Translatable.of("town_jail_help_5"))
				.add("[resident] [hours] [bail] [jail]", Translatable.of("town_jail_help_6"))
				.add("[resident] [hours] [bail] [jail] [cell]", Translatable.of("town_jail_help_7"));
		}
	},
	
	TOWN_UNJAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town unjail")
				.add("[resident]", Translatable.of("town_jail_help_8"));
		}
	},
	
	TOWN_PURGE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town purge")
				.add("[days]", Translatable.of("town_purge_help"));
		}
	},
	

	TOWN_INVITE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town invite")
				.add("[player]", Translatable.of("town_invite_help_1"))
				.add("-[player]", Translatable.of("town_invite_help_2"))
				.add("sent", Translatable.of("town_invite_help_3"))
				.add("sent removeall", Translatable.of("town_invite_help_7"))
				.add("received", Translatable.of("town_invite_help_4"))
				.add("accept [nation]", Translatable.of("town_invite_help_5"))
				.add("deny [nation]", Translatable.of("town_invite_help_6"));
		}
	},

	TOWN_BUY {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town buy")
				.add("bonus [n]", Translatable.of("town_buy_help"));
		}
	},

	RESIDENT_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident", Translatable.of("res_1"))
				.add(Translation.of("res_2"), Translatable.of("res_3"))
				.add("list", Translatable.of("res_4"))
				.add("tax", Translatable.of("res_9"))
				.add("jail", Translatable.of("res_10"))
				.add("toggle", "[mode]...[mode]", Translatable.of("res_11"))
				.add("set [] .. []", Translatable.of("res_12"))
				.add("friend [add/remove] " + Translation.of("res_2"), Translatable.of("ta_resident_help_1"))
				.add("friend [add+/remove+] " + Translation.of("res_2") + " ", Translatable.of("ta_resident_help_1"))
				.add("spawn", Translatable.of("res_13"));
		}
	},
	
	RESIDENT_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident", Translatable.of("res_1"))
				.add(Translation.of("res_2"), Translatable.of("res_3"))
				.add("list", Translatable.of("res_4"));
		}
	},
	
	RESIDENT_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident toggle")
				.add("pvp", Translatable.of("res_toggle_help_0"))
				.add("fire", Translatable.of("res_toggle_help_1"))
				.add("mobs", Translatable.of("res_toggle_help_2"))
				.add("explosion", Translatable.of("res_toggle_help_3"))
				.add("plotborder", Translatable.of("res_toggle_help_4"))
				.add("constantplotborder", Translatable.of("res_toggle_help_5"))
				.add("townborder", Translatable.of("res_toggle_help_6"))
				.add("ignoreplots", Translatable.of("res_toggle_help_7"))
				.add("bordertitles", Translatable.of("res_toggle_help_13"))
				.add("townclaim", Translatable.of("res_toggle_help_8"))
				.add("townunclaim", Translatable.of("res_toggle_help_14"))
				.add("plotgroup", Translatable.of("res_toggle_help_12"))
				.add("map", Translatable.of("res_toggle_help_9"))
				.add("reset|clear", Translatable.of("res_toggle_help_10"))
				.add("spy", Translatable.of("res_toggle_help_11"))
				.add("infotool", Translatable.of("res_toggle_help_15"))
				.add("adminbypass", Translatable.of("res_toggle_help_16"));
		}
	},

	RESIDENT_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident set")
				.add("about ...", Translatable.of("res_set_help_0"))
				.add("perm ...", Translatable.of("res_set_help_1"))
				.add("mode ...",  Translatable.of("res_set_help_2"));
		}
	},

	RESIDENT_SET_MODE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident set mode")
				.add("", "/resident set mode", "clear", Translatable.of("res_toggle_help_10"))
				.add("", "/resident set mode", "reset", Translatable.of("res_toggle_help_10.5"))
				.add("/resident set mode [mode]...[mode]")
				.add("tc", "", Translatable.of("mode_4"))
				.add("nc", "", Translatable.of("mode_5"))
				.add("plotborder", Translatable.of("res_toggle_help_4"))
				.add("constantplotborder", Translatable.of("res_toggle_help_5"))
				.add("townborder", Translatable.of("res_toggle_help_6"))
				.add("ignoreplots", Translatable.of("res_toggle_help_7"))
				.add("bordertitles", Translatable.of("res_toggle_help_13"))
				.add("townclaim", Translatable.of("res_toggle_help_8"))
				.add("townunclaim", Translatable.of("res_toggle_help_14"))
				.add("plotgroup", Translatable.of("res_toggle_help_12"))
				.add("map", Translatable.of("res_toggle_help_9"))
				.add("reset|clear", Translatable.of("res_toggle_help_10"))
				.add("spy", Translatable.of("res_toggle_help_11"))
				.add("infotool", Translatable.of("res_toggle_help_15"))
				.add("adminbypass", Translatable.of("res_toggle_help_16"))
				.add("Eg: /resident set mode map townclaim town nation general");
		}
	},

	RESIDENT_FRIEND {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident friend")
				.add("add ", Translatable.of("res_2"))
				.add("remove ", Translatable.of("res_2"))
				.add("list|clear", Translatable.of("ta_resident_help_2"));
		}
	},

	RESIDENT_JAIL_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident jail")
				.add("", "/resident jail", "paybail", Translatable.of("res_jail_help_0"))
				.add(Colors.LightBlue + Translation.of("msg_resident_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmount())
				.add(Colors.LightBlue + Translation.of("msg_mayor_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountMayor())
				.add(Colors.LightBlue + Translation.of("msg_king_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountKing());
		}
	},

	PLOT_HELP {
		@Override
		protected MenuBuilder load() {
			String resReq = Translation.of("res_sing");
			return new MenuBuilder("plot")
				.add(resReq, "/plot claim", "", Translatable.of("msg_block_claim"))
				.add(resReq, "/plot claim", "[rect/circle] [radius]", Translatable.of("msg_block_claim_radius"))
				.add(resReq, "/plot perm", "[hud]", Translatable.of("plot_help_0"))
				.add("notforsale", "", Translatable.of("msg_plot_nfs"))
				.add("notforsale", "[rect/circle] [radius]", Translatable.of("msg_plot_nfs_radius"))
				.add("forsale [$]", "", Translatable.of("msg_plot_fs"))
				.add("forsale [$]", "within [rect/circle] [radius]", Translatable.of("msg_plot_fs_radius"))
				.add("evict", Translatable.of("plot_help_1"))
				.add("clear", Translatable.of("plot_help_2"))
				.add("set ...", Translatable.of("plot_help_3"))
				.add("trust", Translatable.of("plot_group_help_8"))
				.add(resReq, "toggle", Translatable.of("plot_help_4"))
				.add(resReq, "group", Translatable.of("plot_help_5"))
				.add(Translatable.of("msg_nfs_abr"));
		}
	},

	PLOT_DISTRICT_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot district")
				.add("add|new|create [name]", Translatable.of("plot_district_help_0"))
				.add("remove", Translatable.of("plot_district_help_1"))
				.add("delete", Translatable.of("plot_district_help_2"))
				.add("rename [newName]", Translatable.of("plot_district_help_3"));
		}
	},
	
	PLOT_GROUP_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group")
				.add("add|new|create [name]", Translatable.of("plot_group_help_0"))
				.add("remove", Translatable.of("plot_group_help_1"))
				.add("delete", Translatable.of("plot_group_help_2"))
				.add("rename [newName]", Translatable.of("plot_group_help_3"))
				.add("set ...", Translatable.of("plot_group_help_4"))
				.add("toggle ...", Translatable.of("plot_group_help_5"))
				.add("forsale|fs [price]", Translatable.of("plot_group_help_6"))
				.add("notforsale|nfs", Translatable.of("plot_group_help_7"))
				.add("trust [add/remove] [resident", Translatable.of("plot_group_help_8"));
		}
	},
	
	PLOT_GROUP_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group set")
				.add("maxjoindays", Translatable.of("plot_set_help_1.6"))
				.add("minjoindays", Translatable.of("plot_set_help_1.5"))
				.add("[plottype]", Translatable.of("plot_set_help_0"));
		}
	},

	PLOT_GROUP_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group toggle")
				.add("pvp", Translatable.of("plot_group_toggle_help_0"))
				.add("explosion", Translatable.of("plot_group_toggle_help_1"))
				.add("fire", Translatable.of("plot_group_toggle_help_2"))
				.add("mobs", Translatable.of("plot_group_toggle_help_3"))
				.add("taxed", Translatable.of("plot_group_toggle_help_4"));
		}
	},
	
	PLOT_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot set")
				.add("[plottype]", Translatable.of("plot_set_help_0"))
				.add("outpost", Translatable.of("plot_set_help_1"))
				.add("minjoindays", Translatable.of("plot_set_help_1.5"))
				.add("maxjoindays", Translatable.of("plot_set_help_1.6"))
				.add("reset", Translatable.of("plot_set_help_2"))
				.add("[name]", Translatable.of("plot_set_help_3"))
				.add("Valid Levels: [resident/ally/outsider]")
				.add("Valid Types: [build/destroy/switch/itemuse]")
				.add("perm [on/off]", Translatable.of("plot_set_help_4"))
				.add("perm [level/type] [on/off]", Translatable.of("plot_set_help_5"))
				.add("perm [level] [type] [on/off]", Translatable.of("plot_set_help_6"))
				.add("perm reset", Translatable.of("plot_set_help_7"))
				.add("Eg: /plot set perm ally off")
				.add("Eg: /plot set perm friend build on")
				.add(Translation.of("plot_perms", "'friend'", "'resident'"));
		}
	},

	PLOT_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot toggle")
				.add("pvp", Translatable.of("plot_toggle_help_0"))
				.add("explosion", Translatable.of("plot_toggle_help_1"))
				.add("fire", Translatable.of("plot_toggle_help_2"))
				.add("mobs", Translatable.of("plot_toggle_help_3"));
		}
	},
	
	NATION_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation", Translatable.of("nation_help_1"))
				.add(Translation.of("nation_help_2"), Translatable.of("nation_help_3"))
				.add("list", Translatable.of("nation_help_4"))
				.add("townlist (nation)", Translatable.of("nation_help_11"))
				.add("allylist (nation)", Translatable.of("nation_help_12"))
				.add("enemylist (nation)", Translatable.of("nation_help_13"))
				.add("online", Translatable.of("nation_help_9"))
				.add("spawn", Translatable.of("nation_help_10"))
				.add("join (nation)", Translatable.of("nation_help_14"))
				.add("rank", Translatable.of("nation_help_18"))
				.add("delete", Translatable.of("nation_help_16"))
				.add("merge [nation]", Translatable.of("king_help_8"))
				.add("say", "[message]", Translatable.of("king_help_9"))
				.add(Translation.of("res_sing"), "deposit [$]", Translatable.of("nation_help_15"))
				.add(Translation.of("mayor_sing"), "leave", Translatable.of("nation_help_5"))
				.add(Translation.of("king_sing"), "king ?", Translatable.of("nation_help_7"));
		}
	},
	
	NATION_HELP_CONSOLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation", Translatable.of("nation_help_1"))
				.add(Translation.of("nation_help_2"), Translatable.of("nation_help_3"))
				.add("list", Translatable.of("nation_help_4"));
		}
	},
	
	NATION_RANK {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation rank")
				.add("add/remove [resident] rank", Translatable.of("nation_help_17"));
		}
	},
	
	NATION_LIST {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation list")
				.add("{page #}", Translatable.of("nation_list_help_0"))
				.add("{page #} by residents", Translatable.of("nation_list_help_0"))
				.add("{page #} by towns", Translatable.of("nation_list_help_1"))
				.add("{page #} by open", Translatable.of("nation_list_help_2"))
				.add("{page #} by balance", Translatable.of("nation_list_help_3"))
				.add("{page #} by name", Translatable.of("nation_list_help_4"))
				.add("{page #} by townblocks", Translatable.of("nation_list_help_5"))
				.add("{page #} by online", Translatable.of("nation_list_help_6"));
		}
	},
	
	NATION_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation set")
				.add("king " + Translation.of("res_2"), Translatable.of("nation_set_help_0"))
				.add("capital [town]", Translatable.of("nation_set_help_1"))
				.add("taxes [$]", Translatable.of("nation_set_help_2"))
				.add("conqueredtax [$]", Translatable.of("nation_set_help_3"))
				.add("name [name]", Translatable.of("nation_set_help_4"))
				.add("title/surname [resident] [text]", Translatable.of("nation_set_help_5"))
				.add("tag [upto 4 letters] or clear", Translatable.of("nation_set_help_6"))
				.add("board [message ... ]", Translatable.of("nation_set_help_7"))
				.add("spawn", Translatable.of("nation_set_help_8"))
				.add("spawncost [$]", Translatable.of("nation_set_help_9"))
				.add("mapcolor [color]", Translatable.of("nation_set_help_10"));
		}
	},

	KING_HELP {
		@Override
		protected MenuBuilder load() {
			MenuBuilder builder = new MenuBuilder("nation", false);
			builder.requirement = Translation.of("king_sing");
			return builder.addTitle(Translation.of("king_help_1"))
				.add("withdraw [$]", Translatable.of("king_help_4"))
				.add("[add/kick] [town] .. [town]", Translatable.of("king_help_5"))
				.add("rank", Translatable.of("nation_help_18"))
				.add("set [] .. []", Translatable.of("king_help_6"))
				.add("toggle [] .. []", Translatable.of("king_help_7"))
				.add("ally [] .. [] " + Translation.of("nation_help_2"), Translatable.of("king_help_2"))
				.add("enemy [add/remove] " + Translation.of("nation_help_2"), Translatable.of("king_help_3"))
				.add("delete", Translatable.of("nation_help_16"))
				.add("merge [nation]", Translatable.of("king_help_8"))
				.add("say", "[message]", Translatable.of("king_help_9"));
		}
	},

	NATION_SANCTIONTOWN {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation sanctiontown")
				.add("add [town]", Translatable.of("nation_sanction_help_1"))
				.add("remove [town]", Translatable.of("nation_sanction_help_2"))
				.add("list", Translatable.of("nation_sanction_help_3"))
				.add("list [nation]", Translatable.of("nation_sanction_help_4"));
		}
	},

	ALLIES_STRING {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation ally")
				.add("add [nation]", Translatable.of("nation_ally_help_1"))
				.add("add -[nation]", Translatable.of("nation_ally_help_7"))
				.add("remove [nation]", Translatable.of("nation_ally_help_2"))
				.add("sent", Translatable.of("nation_ally_help_3"))
				.add("received", Translatable.of("nation_ally_help_4"))
				.add("accept [nation]", Translatable.of("nation_ally_help_5"))
				.add("deny [nation]", Translatable.of("nation_ally_help_6"));
		}
	},

	NATION_INVITE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation invite")
				.add("[town]", Translatable.of("nation_invite_help_1"))
				.add("-[town]", Translatable.of("nation_invite_help_2"))
				.add("sent", Translatable.of("nation_invite_help_3"));
		}
	},

	INVITE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("invite")
				.add(TownySettings.getAcceptCommand() + " [town]", Translatable.of("invite_help_1"))
				.add(TownySettings.getDenyCommand() + " [town]", Translatable.of("invite_help_2"))
				.add(TownySettings.getDenyCommand() + " all", Translatable.of("invite_help_4"))
				.add("list", Translatable.of("invite_help_3"));
		}
	},
	
	TOWN_MAYOR_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("Town Mayor Help", false)
				.add(Translation.of("mayor_sing"), "/town", "withdraw [$]", Translatable.of("town_help_28"))
				.add(Translation.of("mayor_sing"), "/town", "claim", Translatable.of("town_help_29"))
				.add(Translation.of("mayor_sing"), "/town", "unclaim", Translatable.of("town_help_30"))
				.add(Translation.of("mayor_sing"), "/town", "[add/kick] ", Translatable.of("town_help_31"))
				.add(Translation.of("mayor_sing"), "/town", "set [] .. []", Translatable.of("town_help_32"))
				.add(Translation.of("mayor_sing"), "/town", "buy [] .. []", Translatable.of("town_help_33"))
				.add(Translation.of("mayor_sing"), "/town", "plots", Translatable.of("town_help_17"))
				.add(Translation.of("mayor_sing"), "/town", "toggle", Translatable.of("town_help_34"))
				.add(Translation.of("mayor_sing"), "/town", "rank", Translatable.of("town_help_35"))
				.add(Translation.of("mayor_sing"), "/town", "delete", Translatable.of("town_help_24"));
		}
	},
	
	NATION_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation toggle")
				.add("peaceful/neutral", Translatable.of("nation_toggle_help_0"))
				.add("public", Translatable.of("nation_toggle_help_1"))
				.add("open", Translatable.of("nation_toggle_help_2"))
				.add("taxpercent", Translatable.of("nation_toggle_help_3"));
		}
	}, 
	
	PLOT_JAILCELL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot jailcell")
				.add("add", Translatable.of("plot_jailcell_help_0"))
				.add("remove", Translatable.of("plot_jailcell_help_1"));
		}
	},
	
	PLOT_PERM_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot perm")
				.add("hud", Translatable.of("plot_help_0"))
				.add("remove [resident]", Translatable.of("plot_help_6"))
				.add("add [resident]", Translatable.of("plot_help_7"))
				.add("gui", Translatable.of("plot_help_8"));
		}
	},
	
	PLOT_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot trust")
				.add("add [resident]", Translatable.of("plot_help_9"))
				.add("remove [resident]", Translatable.of("plot_help_10"));
		}
	},
	
	TOWN_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town trust")
				.add("add [resident]", Translatable.of("town_trust_help_0"))
				.add("remove [resident]", Translatable.of("town_trust_help_1"))
				.add("list", Translatable.of("town_trust_help_2"));
		}
	},
	
	TOWN_TRUSTTOWN_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town trusttown")
				.add("add [town]", Translatable.of("town_towntrust_help_0"))
				.add("remove [town]", Translatable.of("town_towntrust_help_1"))
				.add("list", Translatable.of("town_towntrust_help_2"));
		}
	},
	
	PLOT_GROUP_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group trust")
				.add("add [resident]", Translatable.of("plot_group_help_9"))
				.add("remove [resident]", Translatable.of("plot_group_help_10"));
		}
	},
	
	PLOT_GROUP_PERM_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group perm")
				.add("gui", Translatable.of("plot_group_help_11"))
				.add("add [player]", Translatable.of("plot_group_help_12"))
				.add("remove [player]", Translatable.of("plot_group_help_13"));
		}
	};


	HelpMenu(MenuLine... lines) {
		Collections.addAll(this.lines, lines);
	}

	public void loadMenu() {
		lines.clear();
		lines.addAll(load().lines);
	}

	private final List<MenuLine> lines = new ArrayList<>();

	protected MenuBuilder load(MenuBuilder builder) {
		return load();
	}

	protected abstract MenuBuilder load();

	public static void loadMenus() {
		for (HelpMenu menu : values()) {
			menu.loadMenu();
		}
	}
	
	public List<MenuLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

	public void send(CommandSender sender) {
		for (MenuLine line : lines) {
			String message = line.getLine();
			if (line.getDesc() != null) {
				String separator = Translation.of("help_menu_explanation") + (!message.isEmpty() ? " : " : "");
				message += separator + line.getDesc().forLocale(sender);
			}
			TownyMessaging.sendMessage(sender, message);
		}
	}

	// Class to ease making menus
	private static class MenuBuilder {
		final List<MenuLine> lines = new ArrayList<>();
		private String command;
		String requirement = "";

		MenuBuilder(String cmd, boolean cmdTitle) {
			this.command = cmd;
			if (cmdTitle)
				this.lines.add(MenuLine.of(ChatTools.formatTitle("/" + command), null));
		}

		MenuBuilder(String cmd) {
			this(cmd, true);
		}

		MenuBuilder(String cmd, Translatable desc) {
			this(cmd);
			add("", desc);
		}

		MenuBuilder(String cmd, String requirement, Translatable desc) {
			this(cmd);
			this.requirement = requirement;
			add("", desc);
		}

		MenuBuilder() {
			this.command = "";
		}

		MenuBuilder add(String subCmd, Translatable desc) {
			return add(this.requirement, subCmd, desc);
		}

		MenuBuilder add(String requirement, String subCmd, Translatable desc) {
			this.lines.add(MenuLine.of(ChatTools.formatCommand(requirement, "/" + command, subCmd, ""), desc));
			return this;
		}

		MenuBuilder add(String requirement, String command, String subCmd, Translatable desc) {
			this.lines.add(MenuLine.of(ChatTools.formatCommand(requirement, command, subCmd, ""), desc));
			return this;
		}

		MenuBuilder add(String line) {
			this.lines.add(MenuLine.of(line, null));
			return this;
		}

		public MenuBuilder add(Translatable desc) {
			this.lines.add(MenuLine.of("", desc));
			return this;
		}

		MenuBuilder addTitle(String title) {
			this.lines.add(MenuLine.of(ChatTools.formatTitle(title), null));
			return this;
		}
	}

	private static class MenuLine {
		private String line = "";
		private Translatable description = null;

		MenuLine(String line, Translatable desc) {
			this.line = line;
			this.description = desc;
		}

		public static MenuLine of(String line, Translatable desc) {
			return new MenuLine(line, desc);
		}
		
		public String getLine() {
			return line;
		}
		
		public Translatable getDesc() {
			return description;
		}
	}
}
