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
				.add("remove", Translatable.of("townyadmin_database_help_2"));
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
				.add("nationrank addrank [rank]", Translatable.of("help_ta_perms_nationrankadd"))
				.add("nationrank removerank [rank]", Translatable.of("help_ta_perms_nationrankremove"));
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
	
	TA_TOWN_META {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin town [townname] meta")
				.add("", Translatable.of("ta_townmeta_help_1"))
				.add("set [key] [value]", Translatable.of("ta_townmeta_help_2"))
				.add("add|remove [key]", Translatable.of("ta_townmeta_help_3"));
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
	
	TA_DEPOSITALL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("townyadmin depositall")
				.add("[amount]", Translatable.of("ta_depositall_help_0"));
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
				.add("unclaimblockdelete", Translatable.of("world_toggle_help_11"));
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
				.add("unclaimblockdelete", Translatable.of("world_toggle_help_11"));
		}
	},
	
	TOWN_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town", Translatable.of("town_help_1"))
				.add("[town]", Translatable.of("town_help_3"))
				.add("new [name]", Translatable.of("town_help_11"))
				.add("here", Translatable.of("town_help_4"))
				.add("list", Translatable.of("town_help_26"))
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
			return new MenuBuilder("town", Translatable.of("town_help_1"))
				.add("[town]", Translatable.of("town_help_3"))
				.add("list", Translatable.of("town_help_26"));
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
	
	TOWN_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town set")
				.add("board [message ... ]", "")
				.add("mayor " + Translation.of("town_help_2"), "")
				.add("homeblock", "")
				.add("spawn/outpost/jail", "")
				.add("perm ...", "'/town set perm' " + Translation.of("res_5"))
				.add("taxes [$]", "")
				.add("[plottax/shoptax/embassytax] [$]", "")
				.add("[plotprice/shopprice/embassyprice] [$]", "")
				.add("spawncost [$]", "")
				.add("name [name]", "")
				.add("tag [upto 4 letters] or clear", "")
				.add("title/surname [resident] [text]", "")
				.add("taxpercentcap [amount]", "");
		}
	},
	
	TOWN_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town toggle")
				.add("pvp", "")
				.add("public", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "")
				.add("taxpercent", "")
				.add("open", "");
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
				.add("list", "")
				.add("[resident]", "")
				.add("[resident] [hours]", "")
				.add("[resident] [hours] [jail]", "")
				.add("[resident] [hours] [jail] [cell]", "");
		}
	},

	TOWN_JAILWITHBAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town jail")
				.add("list", "")
				.add("[resident]", "")
				.add("[resident] [hours]", "")
				.add("[resident] [hours] [bail]", "")
				.add("[resident] [hours] [bail] [jail]", "")
				.add("[resident] [hours] [bail] [jail] [cell]", "");
		}
	},
	
	TOWN_UNJAIL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town unjail")
				.add("[resident]", "");
		}
	},
	
	TOWN_PURGE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town purge")
				.add("[days]", "");
		}
	},
	

	TOWN_INVITE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town invite")
				.add("[player]", Translatable.of("town_invite_help_1"))
				.add("-[player]", Translatable.of("town_invite_help_2"))
				.add("sent", Translatable.of("town_invite_help_3"))
				.add("received", Translatable.of("town_invite_help_4"))
				.add("accept [nation]", Translatable.of("town_invite_help_5"))
				.add("deny [nation]", Translatable.of("town_invite_help_6"));
		}
	},

	RESIDENT_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident", Translatable.of("res_1"))
				.add(Translation.of("res_2"), Translatable.of("res_3"))
				.add("list", Translatable.of("res_4"))
				.add("tax", "")
				.add("jail", "")
				.add("toggle", "[mode]...[mode]")
				.add("set [] .. []", "'/resident set' " + Translation.of("res_5"))
				.add("friend [add/remove] " + Translation.of("res_2"), Translatable.of("res_6"))
				.add("friend [add+/remove+] " + Translation.of("res_2") + " ", Translatable.of("res_7"))
				.add("spawn", "");
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
				.add("pvp", "")
				.add("fire", "")
				.add("mobs", "")
				.add("explosion", "")
				.add("plotborder", "")
				.add("constantplotborder", "")
				.add("townborder", "")
				.add("ignoreplots", "")
				.add("townclaim", "")
				.add("map", "")
				.add("reset|clear", "")
				.add("spy", "");
		}
	},

	RESIDENT_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident set")
				.add("about ...", "'/resident set about' " + Translation.of("res_5"))
				.add("perm ...", "'/resident set perm' " + Translation.of("res_5"))
				.add("mode ...", "'/resident set mode' " + Translation.of("res_5"));
		}
	},

	RESIDENT_SET_MODE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident set mode")
				.add("", "/resident set mode", "clear", "")
				.add("", "/resident set mode", "[mode]...[mode]", "")
				.add("MODE", "map", "", Translatable.of("mode_1"))
				.add("MODE", "townclaim", "", Translatable.of("mode_2"))
				.add("MODE", "townunclaim", "", Translatable.of("mode_3"))
				.add("Mode", "plotgroup", "", "runs /plot group add with your last-used plot group name.")
				.add("Mode", "tc", "", Translatable.of("mode_4"))
				.add("Mode", "nc", "", Translatable.of("mode_5"))
				.add("Mode", "ignoreplots", "", "")
				.add("Mode", "constantplotborder", "", "")
				.add("Mode", "plotborder", "", "")
				.add("Eg", "/resident set mode", "map townclaim town nation general", "");
		}
	},

	RESIDENT_FRIEND {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident friend")
				.add("add ", Translatable.of("res_2"))
				.add("remove ", Translatable.of("res_2"))
				.add("list", "")
				.add("clear", "");
		}
	},

	RESIDENT_JAIL_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("resident jail", "")
				.add("", "/resident jail", "paybail", "Pays the bail cost to get out of jail.")
				.add(Colors.LightBlue + Translation.of("msg_resident_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmount())
				.add(Colors.LightBlue + Translation.of("msg_mayor_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountMayor())
				.add(Colors.LightBlue + Translation.of("msg_king_bail_amount") + Colors.Green + "$" + TownySettings.getBailAmountKing());
		}
	},

	PLOT_HELP {
		@Override
		protected MenuBuilder load() {
			String resReq = Translation.of("res_sing");
			return new MenuBuilder("plot", resReq + "/" + Translation.of("mayor_sing"))
				.add(resReq, "/plot claim", "", Translatable.of("msg_block_claim"))
				.add(resReq, "/plot claim", "[rect/circle] [radius]", "")
				.add(resReq, "/plot perm", "[hud]", "")
				.addCmd("/plot notforsale", "", Translatable.of("msg_plot_nfs"))
				.addCmd("/plot notforsale", "[rect/circle] [radius]", "")
				.addCmd("/plot forsale [$]", "", Translatable.of("msg_plot_fs"))
				.addCmd("/plot forsale [$]", "within [rect/circle] [radius]", "")
				.addCmd("/plot evict", "", "")
				.addCmd("/plot clear", "", "")
				.addCmd("/plot set ...", "", Translatable.of("msg_plot_fs"))
				.add(resReq, "/plot toggle", "[pvp/fire/explosion/mobs]", "")
				.add(resReq, "/plot group", "?", "")
				.add(Translatable.of("msg_nfs_abr"));
		}
	},
	
	PLOT_GROUP_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group")
				.add("add|new|create [name]", "Ex: /plot group new ExpensivePlots")
				.add("remove", "Removes a plot from the specified group.")
				.add("delete", "Deletes a plotgroup completely.")
				.add("rename [newName]", "Renames the group you are standing in.")
				.add("set ...", "Ex: /plot group set perm resident on.")
				.add("toggle ...", "Ex: /plot group toggle [pvp|fire|mobs]")
				.add("forsale|fs [price]", "Ex: /plot group forsale 50")
				.add("notforsale|nfs", "Ex: /plot group notforsale")
				.add("trust [add/remove] [resident", "Adds or removes a resident as trusted.");
		}
	},
	
	PLOT_GROUP_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group toggle")
				.add("pvp", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "");
		}
	},
	
	PLOT_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot set")
				.add("[plottype]", "Ex: Inn, Wilds, Farm, Embassy etc")
				.add("outpost")
				.add("reset", "Removes a plot type")
				.add("[name]", "Names a plot")
				.add("Level", "[resident/ally/outsider]", "", "")
				.add("Type", "[build/destroy/switch/itemuse]", "", "")
				.add("perm [on/off]", "Toggle all permissions")
				.add("perm [level/type] [on/off]", "")
				.add("perm [level] [type] [on/off]", "")
				.add("perm reset", "")
				.add("Eg", "/plot set perm", "ally off", "")
				.add("Eg", "/plot set perm", "friend build on", "")
				.add(Translation.of("plot_perms", "'friend'", "'resident'"))
				.add(Translatable.of("plot_perms_1"));
		}
	},

	PLOT_TOGGLE {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot toggle")
				.add("pvp", "")
				.add("explosion", "")
				.add("fire", "")
				.add("mobs", "");
		}
	},
	
	NATION_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation", Translatable.of("nation_help_1"))
				.add(Translation.of("nation_help_2"), Translatable.of("nation_help_3"))
				.add("list", Translatable.of("nation_help_4"))
				.add("townlist (nation)", "")
				.add("allylist (nation)", "")
				.add("enemylist (nation)", "")
				.add("online", Translatable.of("nation_help_9"))
				.add("spawn", Translatable.of("nation_help_10"))
				.add("join (nation)", "Used to join open nations.")
				.add(Translation.of("res_sing"), "deposit [$]", "")
				.add(Translation.of("mayor_sing"), "leave", Translatable.of("nation_help_5"))
				.add(Translation.of("king_sing"), "king ?", Translatable.of("nation_help_7"))
				.add(Translation.of("admin_sing"), "new " + Translation.of("nation_help_2") + " [capital]", Translatable.of("nation_help_8"))
				.add(Translation.of("admin_sing"), "delete " + Translation.of("nation_help_2"), "")
				.add(Translation.of("admin_sing"), "say", "[message]");
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
				.add("add/remove [resident] rank", "");
		}
	},
	
	NATION_LIST {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation list")
				.add("{page #}", "")
				.add("{page #} by residents", "")
				.add("{page #} by towns", "")
				.add("{page #} by open", "")
				.add("{page #} by balance", "")
				.add("{page #} by name", "")
				.add("{page #} by townblocks", "")
				.add("{page #} by online", "");
		}
	},
	
	NATION_SET {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation set")
				.add("king " + Translation.of("res_2"), "")
				.add("capital [town]", "")
				.add("taxes [$]", "")
				.add("conqueredtax [$]", "")
				.add("name [name]", "")
				.add("title/surname [resident] [text]", "")
				.add("tag [upto 4 letters] or clear", "")
				.add("board [message ... ]", "")
				.add("spawn", "")
				.add("spawncost [$]", "")
				.add("mapcolor [color]", "");
		}
	},

	KING_HELP {
		@Override
		protected MenuBuilder load() {
			MenuBuilder builder = new MenuBuilder("nation", false);
			builder.requirement = Translation.of("king_sing");
			return builder.addTitle(Translation.of("king_help_1"))
				.add("withdraw [$]", "")
				.add("[add/kick] [town] .. [town]", "")
				.add("rank [add/remove] " + Translation.of("res_2"), "[Rank]")
				.add("set [] .. []", "")
				.add("toggle [] .. []", "")
				.add("ally [] .. [] " + Translation.of("nation_help_2"), Translatable.of("king_help_2"))
				.add("enemy [add/remove] " + Translation.of("nation_help_2"), Translatable.of("king_help_3"))
				.add("delete", "")
				.add("merge {nation}", "")
				.add("say", "[message]");
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
			return new MenuBuilder("invite", "")
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
				.add(Translation.of("mayor_sing"), "/town", "withdraw [$]", "")
				.add(Translation.of("mayor_sing"), "/town", "claim", "'/town claim ?' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "unclaim", "'/town " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "[add/kick] " + Translation.of("res_2") + " .. []", Translation.of("res_6"))
				.add(Translation.of("mayor_sing"), "/town", "set [] .. []", "'/town set' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "buy [] .. []", "'/town buy' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "plots", "")
				.add(Translation.of("mayor_sing"), "/town", "toggle", "")
				.add(Translation.of("mayor_sing"), "/town", "rank add/remove [resident] [rank]", "'/town rank ?' " + Translation.of("res_5"))
				.add(Translation.of("mayor_sing"), "/town", "delete", "");
		}
	},
	
	NATION_TOGGLE_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("nation toggle")
				.add("", "/nation toggle", "peaceful/neutral", "")
				.add("", "/nation toggle", "public", "")
				.add("", "/nation toggle", "open", "")
				.add("", "/nation toggle", "taxpercent", "");
		}
	}, 
	
	PLOT_JAILCELL {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot jailcell")
				.add("", "/plot jailcell", "add", "Adds a JailCell where you stand.")
				.add("", "/plot jailcell", "remove", "Removes a JailCell where you stand.");
		}
	},
	
	PLOT_PERM_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot perm")
				.add("hud", "Opens the permissions hud.")
				.add("remove [resident]", "Removes permission overrides for a player.")
				.add("add [resident]", "Adds default permission overrides for a player.")
				.add("gui", "Opens the permission editor gui.");
		}
	},
	
	PLOT_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot trust")
				.add("add [resident]", "")
				.add("remove [resident]", "");
		}
	},
	
	TOWN_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town trust")
				.add("add [resident]", "")
				.add("remove [resident]", "")
				.add("list", "");
		}
	},
	
	TOWN_TRUSTTOWN_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("town trusttown")
				.add("add [town]", "")
				.add("remove [town]", "")
				.add("list", "");
		}
	},
	
	PLOT_GROUP_TRUST_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group trust")
				.add("add [resident]", "")
				.add("remove [resident]", "");
		}
	},
	
	PLOT_GROUP_PERM_HELP {
		@Override
		protected MenuBuilder load() {
			return new MenuBuilder("plot group perm")
				.add("gui", "")
				.add("add [player]", "")
				.add("remove [player]", "");
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
			String separator = Translation.of("help_menu_explanation") + (!message.isEmpty() ? " : " : "");
			if (line.getDesc() != null)
				message += separator + line.getDesc().forLocale(sender);
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

		MenuBuilder(String cmd, String desc) {
			this(cmd);
			if (!desc.isEmpty())
				add("", desc);
		}

		MenuBuilder(String cmd, Translatable desc) {
			this(cmd);
			add("", desc);
		}

		@SuppressWarnings("unused")
		MenuBuilder(String cmd, String requirement, String desc) {
			this(cmd);
			this.requirement = requirement;
			if (!desc.isEmpty())
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

		MenuBuilder add(String subCmd, String desc) {
			return add(this.requirement, subCmd, desc);
		}

		MenuBuilder add(String subCmd, Translatable desc) {
			return add(this.requirement, subCmd, desc);
		}

		MenuBuilder add(String requirement, String subCmd, String desc) {
			this.lines.add(MenuLine.of(ChatTools.formatCommand(requirement, "/" + command, subCmd, desc), null));
			return this;
		}

		MenuBuilder add(String requirement, String subCmd, Translatable desc) {
			this.lines.add(MenuLine.of(ChatTools.formatCommand(requirement, "/" + command, subCmd, ""), desc));
			return this;
		}

		MenuBuilder add(String requirement, String command, String subCmd, String desc) {
			this.lines.add(MenuLine.of(ChatTools.formatCommand(requirement, command, subCmd, desc), null));
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

		MenuBuilder addCmd(String cmd, String subCmd, String desc) {
			return add(requirement, cmd, subCmd, desc);
		}
		
		MenuBuilder addCmd(String cmd, String subCmd, Translatable desc) {
			return add(requirement, cmd, subCmd, desc);
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
