package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.ChatTools;

public class HelpMenus {

	static final List<String> towny_general_help = new ArrayList<>();
	static final List<String> towny_help = new ArrayList<>();
	
	static final List<String> ta_help = new ArrayList<>();
	static final List<String> ta_unclaim = new ArrayList<>();
	
	static final List<String> townyworld_help = new ArrayList<>();
	static final List<String> townyworld_help_console = new ArrayList<>();
	static final List<String> townyworld_set = new ArrayList<>();
	static final List<String> townyworld_set_console = new ArrayList<>();
	
	static final List<String> town_help = new ArrayList<>();
	static final List<String> town_invite = new ArrayList<>();

	static final List<String> resident_help = new ArrayList<>();

	static final List<String> plot_help = new ArrayList<>();
	
	static final List<String> nation_help = new ArrayList<>();
	static final List<String> king_help = new ArrayList<>();
	static final List<String> alliesstring = new ArrayList<>();
    static final List<String> nation_invite = new ArrayList<>();

    static final List<String> invite_help = new ArrayList<>();
	
    public static void loadHelpMenus() {
    	
		/*
		 * Towny Command Helps
		 */
		towny_help.add(ChatTools.formatTitle("/towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "prices", "Display the prices used with Economy"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "top", "Display highscores"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "time", "Display time until a new day"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "v", "Displays the version of Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "war", "'/towny war' for more info"));
		
		towny_general_help.add(ChatTools.formatTitle(Translation.of("help_0")));
		towny_general_help.add(Translation.of("help_1"));
		towny_general_help.add(ChatTools.formatCommand("", "/resident", "?", "") + ", " + ChatTools.formatCommand("", "/town", "?", "") + ", " + ChatTools.formatCommand("", "/nation", "?", "") + ", " + ChatTools.formatCommand("", "/plot", "?", "") + ", " + ChatTools.formatCommand("", "/towny", "?", ""));
		towny_general_help.add(ChatTools.formatCommand("", "/tc", "[msg]", Translation.of("help_2")) + ", " + ChatTools.formatCommand("", "/nc", "[msg]", Translation.of("help_3")).trim());
		towny_general_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin", "?", ""));
		
		/*
		 * TownyAdmin Command Helps
		 */
		ta_help.add(ChatTools.formatTitle("/townyadmin"));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "", Translation.of("admin_panel_1")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' " + Translation.of("res_5")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "unclaim [radius]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "town/nation", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "plot", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "givebonus [town/player] [num]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "toggle peaceful/war/debug/devmode", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "resident/town/nation", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "tpplot {world} {x} {z}", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "checkperm {name} {node}", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reload", Translation.of("admin_panel_2")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reset", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "backup", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "mysqldump", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "database [save/load]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newday", Translation.of("admin_panel_3")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "purge [number of days]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "delete [] .. []", "delete a residents data files."));
		
		ta_unclaim.add(ChatTools.formatTitle("/townyadmin unclaim"));
		ta_unclaim.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin unclaim", "", Translation.of("townyadmin_help_1")));
		ta_unclaim.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyadmin unclaim", "[radius]", Translation.of("townyadmin_help_2")));
		
		/*
		 * TownyWorld Command Helps 
		 */
		townyworld_help.add(ChatTools.formatTitle("/townyworld"));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "", Translation.of("world_help_1")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", Translation.of("world_help_2"), Translation.of("world_help_3")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "list", Translation.of("world_help_4")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "toggle", ""));
		townyworld_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyworld", "set [] .. []", ""));
		townyworld_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyworld", "regen", Translation.of("world_help_5")));

		townyworld_set.add(ChatTools.formatTitle("/townyworld set"));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildname [name]", ""));

		townyworld_help_console.add(ChatTools.formatTitle("/townyworld"));
		townyworld_help_console.add(ChatTools.formatCommand("", "/townyworld {world}", "", Translation.of("world_help_1")));
		townyworld_help_console.add(ChatTools.formatCommand("", "/townyworld {world}", Translation.of("world_help_2"), Translation.of("world_help_3")));
		townyworld_help_console.add(ChatTools.formatCommand("", "/townyworld {world}", "list", Translation.of("world_help_4")));
		townyworld_help_console.add(ChatTools.formatCommand("", "/townyworld {world}", "toggle", ""));
		townyworld_help_console.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyworld {world}", "set [] .. []", ""));
		townyworld_help_console.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/townyworld {world}", "regen", Translation.of("world_help_5")));

		townyworld_set_console.add(ChatTools.formatTitle("/townyworld set"));
		townyworld_set_console.add(ChatTools.formatCommand("", "/townyworld {world} set", "wildname [name]", ""));
		
		/*
		 * Town Command Helps
		 */
		town_help.add(ChatTools.formatTitle("/town"));
		town_help.add(ChatTools.formatCommand("", "/town", "", Translation.of("town_help_1")));
		town_help.add(ChatTools.formatCommand("", "/town", "[town]", Translation.of("town_help_3")));
		town_help.add(ChatTools.formatCommand("", "/town", "new [name]", Translation.of("town_help_11")));
		town_help.add(ChatTools.formatCommand("", "/town", "here", Translation.of("town_help_4")));
		town_help.add(ChatTools.formatCommand("", "/town", "list", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "online", Translation.of("town_help_10")));
		town_help.add(ChatTools.formatCommand("", "/town", "leave", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "reslist", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "ranklist", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "outlawlist", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "plots", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "outlaw add/remove [name]", ""));
		town_help.add(ChatTools.formatCommand("", "/town", "say", "[message]"));
		town_help.add(ChatTools.formatCommand("", "/town", "spawn", Translation.of("town_help_5")));
		town_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/town", "deposit [$]", ""));
		town_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/town", "rank add/remove [resident] [rank]", ""));
		town_help.add(ChatTools.formatCommand(Translation.of("mayor_sing"), "/town", "mayor ?", Translation.of("town_help_8")));
		town_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/town", "delete [town]", ""));
		
		town_invite.add(ChatTools.formatTitle("/town invite"));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite [player]", Translation.of("town_invite_help_1")));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite -[player]", Translation.of("town_invite_help_2")));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite sent", Translation.of("town_invite_help_3")));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite received", Translation.of("town_invite_help_4")));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite accept [nation]", Translation.of("town_invite_help_5")));
		town_invite.add(ChatTools.formatCommand("", "/town", "invite deny [nation]", Translation.of("town_invite_help_6")));
		
		/*
		 * Resident Command Helps
		 */
		resident_help.add(ChatTools.formatTitle("/resident"));
		resident_help.add(ChatTools.formatCommand("", "/resident", "", Translation.of("res_1")));
		resident_help.add(ChatTools.formatCommand("", "/resident", Translation.of("res_2"), Translation.of("res_3")));
		resident_help.add(ChatTools.formatCommand("", "/resident", "list", Translation.of("res_4")));
		resident_help.add(ChatTools.formatCommand("", "/resident", "tax", ""));
		resident_help.add(ChatTools.formatCommand("", "/resident", "jail", ""));
		resident_help.add(ChatTools.formatCommand("", "/resident", "toggle", "[mode]...[mode]"));
		resident_help.add(ChatTools.formatCommand("", "/resident", "set [] .. []", "'/resident set' " + Translation.of("res_5")));
		resident_help.add(ChatTools.formatCommand("", "/resident", "friend [add/remove] " + Translation.of("res_2"), Translation.of("res_6")));
		resident_help.add(ChatTools.formatCommand("", "/resident", "friend [add+/remove+] " + Translation.of("res_2") + " ", Translation.of("res_7")));
		resident_help.add(ChatTools.formatCommand("", "/resident", "spawn", ""));
		
		/*
		 * Plot Command Helps
		 */
		plot_help.add(ChatTools.formatTitle("/plot"));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/plot claim", "", Translation.of("msg_block_claim")));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/plot claim", "[rect/circle] [radius]", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/plot perm", "[hud]", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot notforsale", "", Translation.of("msg_plot_nfs")));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot notforsale", "[rect/circle] [radius]", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot forsale [$]", "", Translation.of("msg_plot_fs")));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot forsale [$]", "within [rect/circle] [radius]", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot evict", "" , ""));		
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot clear", "", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing") + "/" + Translation.of("mayor_sing"), "/plot set ...", "", Translation.of("msg_plot_fs")));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/plot toggle", "[pvp/fire/explosion/mobs]", ""));
		plot_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/plot group", "?", ""));
		plot_help.add(Translation.of("msg_nfs_abr"));
		
		/*
		 * Nation Command Helps
		 */
		// Basic nation help screen.
		nation_help.add(ChatTools.formatTitle("/nation"));
		nation_help.add(ChatTools.formatCommand("", "/nation", "", Translation.of("nation_help_1")));
		nation_help.add(ChatTools.formatCommand("", "/nation", Translation.of("nation_help_2"), Translation.of("nation_help_3")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "list", Translation.of("nation_help_4")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "townlist (nation)", ""));
		nation_help.add(ChatTools.formatCommand("", "/nation", "allylist (nation)", ""));
		nation_help.add(ChatTools.formatCommand("", "/nation", "enemylist (nation)", ""));
		nation_help.add(ChatTools.formatCommand("", "/nation", "online", Translation.of("nation_help_9")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "spawn", Translation.of("nation_help_10")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "join (nation)", "Used to join open nations."));		
		nation_help.add(ChatTools.formatCommand(Translation.of("res_sing"), "/nation", "deposit [$]", ""));
		nation_help.add(ChatTools.formatCommand(Translation.of("mayor_sing"), "/nation", "leave", Translation.of("nation_help_5")));
		nation_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "king ?", Translation.of("nation_help_7")));
		nation_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/nation", "new " + Translation.of("nation_help_2") + " [capital]", Translation.of("nation_help_8")));
		nation_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/nation", "delete " + Translation.of("nation_help_2"), ""));
		nation_help.add(ChatTools.formatCommand(Translation.of("admin_sing"), "/nation", "say", "[message]"));

		// King specific help screen.
		king_help.add(ChatTools.formatTitle(Translation.of("king_help_1")));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "withdraw [$]", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "[add/kick] [town] .. [town]", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "rank [add/remove] " + Translation.of("res_2"), "[Rank]"));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "set [] .. []", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "toggle [] .. []", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "ally [] .. [] " + Translation.of("nation_help_2"), Translation.of("king_help_2")));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "enemy [add/remove] " + Translation.of("nation_help_2"), Translation.of("king_help_3")));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "delete", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "merge {nation}", ""));
		king_help.add(ChatTools.formatCommand(Translation.of("king_sing"), "/nation", "say", "[message]"));

		// Used for inviting allies to the nation.
		alliesstring.add(ChatTools.formatTitle("/nation invite"));
		alliesstring.add(ChatTools.formatCommand("", "/nation", "ally add [nation]", Translation.of("nation_ally_help_1")));
		if (TownySettings.isDisallowOneWayAlliance()) {
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally add -[nation]", Translation.of("nation_ally_help_7")));
		}
		alliesstring.add(ChatTools.formatCommand("", "/nation", "ally remove [nation]", Translation.of("nation_ally_help_2")));
		if (TownySettings.isDisallowOneWayAlliance()) {
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally sent", Translation.of("nation_ally_help_3")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally received", Translation.of("nation_ally_help_4")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally accept [nation]", Translation.of("nation_ally_help_5")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally deny [nation]", Translation.of("nation_ally_help_6")));
		}

		// Used for inviting Towns to the nation.
		nation_invite.add(ChatTools.formatTitle("/town invite"));
		nation_invite.add(ChatTools.formatCommand("", "/nation", "invite [town]", Translation.of("nation_invite_help_1")));
		nation_invite.add(ChatTools.formatCommand("", "/nation", "invite -[town]", Translation.of("nation_invite_help_2")));
		nation_invite.add(ChatTools.formatCommand("", "/nation", "invite sent", Translation.of("nation_invite_help_3")));
		
		/*
		 * Invite Command Helps
		 */
		invite_help.add(ChatTools.formatTitle("/invite"));
		invite_help.add(ChatTools.formatCommand("", "/invite", TownySettings.getAcceptCommand() + " [town]", Translation.of("invite_help_1")));
		invite_help.add(ChatTools.formatCommand("", "/invite", TownySettings.getDenyCommand() + " [town]", Translation.of("invite_help_2")));
		invite_help.add(ChatTools.formatCommand("", "/invite", "list", Translation.of("invite_help_3")));
	}
}
