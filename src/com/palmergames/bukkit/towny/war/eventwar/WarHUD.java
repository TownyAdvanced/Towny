package com.palmergames.bukkit.towny.war.eventwar;


import java.util.Hashtable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;

public class WarHUD {

	//The maximum length for a scoreboard output. Will cut off at the maximum length. *CANT BE > 16*
	final int MAX_OUT_LEN = 16; 
	
	//Checks if attack edges only is on in the config
	boolean edges = TownySettings.getOnlyAttackEdgesInWar();
	
	String WAR_HUD_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "War";
	
	Team space1, town_title, town_score, space2, location_title, nation, town, edge, health, home, space3, top_title, first, second, third;
	
	String space1_player = ChatColor.DARK_PURPLE.toString();
	String town_title_player = ChatColor.YELLOW + "" + ChatColor.UNDERLINE;
	String town_score_player = ChatColor.WHITE + "Score: " + ChatColor.RED;
	String space2_player = ChatColor.DARK_BLUE.toString();
	String location_title_player = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "Location";
	String nation_player = ChatColor.WHITE + "Nation: " + ChatColor.GOLD;
	String town_player = ChatColor.WHITE + "Town: " + ChatColor.DARK_AQUA;
	String edge_player = ChatColor.WHITE + "Attackable: " + ChatColor.RED;
	String health_player = ChatColor.WHITE + "Health: " + ChatColor.RED;
	String home_player = ChatColor.RED + "";
	String space3_player = ChatColor.DARK_GREEN.toString();
	String top_title_player = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + "Top Towns";
	String first_player = ChatColor.DARK_GREEN + "" + ChatColor.DARK_AQUA + "";
	String second_player = ChatColor.BLACK + "" + ChatColor.DARK_AQUA + "";
	String third_player = ChatColor.YELLOW + "" + ChatColor.DARK_AQUA + "";
	
	Towny plugin;
	Player p;
	
	public WarHUD (Towny plugin, Player p) {
		this.plugin = plugin;
		this.p = p;
		init();		
		updateHomeTown();
		updateScore();
		updateLocation();
		updateTopThree();
	}
	
	@SuppressWarnings("deprecation")
	private void init() 
	{
		//init objective
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = board.registerNewObjective("WAR_HUD_OBJ", "dummy");
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.setDisplayName(WAR_HUD_TITLE);
		//register teams
		space1 = board.registerNewTeam("space1");
		town_title = board.registerNewTeam("town_title");
		town_score = board.registerNewTeam("town_score");
		space2 = board.registerNewTeam("space2");
		location_title = board.registerNewTeam("location_title");
		nation = board.registerNewTeam("nation");
		town = board.registerNewTeam("town");
		health = board.registerNewTeam("health");
		home = board.registerNewTeam("home");
		space3 = board.registerNewTeam("space3");
		top_title = board.registerNewTeam("top_title");
		first = board.registerNewTeam("first");
		second = board.registerNewTeam("second");
		third = board.registerNewTeam("third");
		//register players
		space1.addPlayer(Bukkit.getOfflinePlayer(space1_player));
		town_title.addPlayer(Bukkit.getOfflinePlayer(town_title_player));
		town_score.addPlayer(Bukkit.getOfflinePlayer(town_score_player));
		space2.addPlayer(Bukkit.getOfflinePlayer(space2_player));
		location_title.addPlayer(Bukkit.getOfflinePlayer(location_title_player));
		nation.addPlayer(Bukkit.getOfflinePlayer(nation_player));
		town.addPlayer(Bukkit.getOfflinePlayer(town_player));
		health.addPlayer(Bukkit.getOfflinePlayer(health_player));
		home.addPlayer(Bukkit.getOfflinePlayer(home_player));
		space3.addPlayer(Bukkit.getOfflinePlayer(space3_player));
		top_title.addPlayer(Bukkit.getOfflinePlayer(top_title_player));
		first.addPlayer(Bukkit.getOfflinePlayer(first_player));
		second.addPlayer(Bukkit.getOfflinePlayer(second_player));
		third.addPlayer(Bukkit.getOfflinePlayer(third_player));
		//set scores for positioning
		obj.getScore(space1_player).setScore(14);
		obj.getScore(town_title_player).setScore(13);
		obj.getScore(town_score_player).setScore(12);
		obj.getScore(space2_player).setScore(11);
		obj.getScore(location_title_player).setScore(10);
		obj.getScore(nation_player).setScore(9);
		obj.getScore(town_player).setScore(8);
		obj.getScore(health_player).setScore(edges ? 6 : 7);
		obj.getScore(home_player).setScore(edges ? 5 : 6);
		obj.getScore(space3_player).setScore(edges ? 4 : 5);
		obj.getScore(top_title_player).setScore(edges ? 3 : 4);
		obj.getScore(first_player).setScore(edges ? 2 : 3);
		obj.getScore(second_player).setScore(edges ? 1 : 2);
		obj.getScore(third_player).setScore(edges ? 0 : 1);
		
		if (edges) {
			edge = board.registerNewTeam("edge");
			edge.addPlayer(Bukkit.getOfflinePlayer(edge_player));
			obj.getScore(edge_player).setScore(7);
		}
		//set the board
		p.setScoreboard(board);
	}
	
	public void updateHomeTown()
	{
		String homeTown;
		try {
			homeTown = TownyUniverse.getDataSource().getResident(p.getName()).getTown().getName();
		} catch (NotRegisteredException e) {
			homeTown = "Townless!";
		}
		town_title.setSuffix(checkString(homeTown));
	}
	
	public void updateScore()
	{
		String score;
		try {
			Town home = TownyUniverse.getDataSource().getResident(p.getName()).getTown();
			Hashtable<Town, Integer> scores = plugin.getTownyUniverse().getWarEvent().getTownScores();
			if (scores.containsKey(home))
				score = scores.get(home) + "";
			else
				score = "";
		} catch (NotRegisteredException e) {
			score = "";
		}
		town_score.setSuffix(checkString(score));
	}
	
	public void updateLocation()
	{
		updateLocation(new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p)));
	}
	
	public void updateLocation(WorldCoord worldCoord)
	{
		String nation_loc, town_loc, homeblock_loc, hp, onEdge;
		boolean hasTown = false;
		War warEvent = plugin.getTownyUniverse().getWarEvent();
		try {
			Town town = worldCoord.getTownBlock().getTown();
			town_loc = town.getName();
			if (worldCoord.getTownBlock().isHomeBlock())
				homeblock_loc = "HOME BLOCK";
			else
				homeblock_loc = "";
			Hashtable<WorldCoord, Integer> warZone = warEvent.getWarZone();
			boolean inWar = warZone.containsKey(worldCoord);
			if (inWar)
				hp = warZone.get(worldCoord) + "";
			else 
				hp = "Fallen";
			hasTown = true;
			if (edges && inWar && isOnEdgeOfTown(worldCoord.getTownBlock(), worldCoord, warEvent))
				onEdge = "True";
			else 
				onEdge = "False";
		} catch (NotRegisteredException e) { town_loc = "Wilderness"; homeblock_loc = ""; hp = ""; onEdge = "";}
		try {
			nation_loc = worldCoord.getTownBlock().getTown().getNation().getName();
		} catch (NotRegisteredException e) { nation_loc = ""; hp = "Neutral";}
		if (!hasTown)
			hp = "";
		nation.setSuffix(checkString(nation_loc));
		town.setSuffix(checkString(town_loc));
		home.setSuffix(checkString(homeblock_loc));
		health.setSuffix(hp);
		edge.setSuffix(onEdge);
	}
	
	public void updateTopThree()
	{
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(plugin.getTownyUniverse().getWarEvent().getTownScores());
		kvTable.sortByValue();
		kvTable.revese();
		KeyValue<Town, Integer> first = null;
		KeyValue<Town, Integer> second = null;
		KeyValue<Town, Integer> third = null;
		if (kvTable.getKeyValues().size() >= 1)
			first = kvTable.getKeyValues().get(0);
		if (kvTable.getKeyValues().size() >= 2)
			second = kvTable.getKeyValues().get(1);
		if (kvTable.getKeyValues().size() >= 3)
			third = kvTable.getKeyValues().get(2);
		updateTopThree(first, second, third);
	}
	
	public void updateTopThree(KeyValue<Town, Integer> f, KeyValue<Town, Integer> s, KeyValue<Town, Integer> t)
	{
		System.out.println("[HUD] Update Top Three (first): " + (f != null ? f.value + " " + f.key.getName() : "Null"));
		first.setSuffix((f != null && f.value > 0) ? (f.value + "-" + f.key.getName()) : "" );
		second.setSuffix((s != null && s.value > 0) ? (s.value + "-" + s.key.getName()) : "" );
		third.setSuffix((t != null && t.value > 0) ? (t.value + "-" + t.key.getName()) : "" );
	}
	
	public void updateHealth(int hp)
	{
		if (hp > 0)
			health.setSuffix(hp + "");
		else
			health.setSuffix("Fallen");
	}
	
	private String checkString(String checkme)
	{
		return checkme.length() > 16 ? checkme.substring(0, MAX_OUT_LEN) : checkme;
	}
	
	public static boolean isOnEdgeOfTown(TownBlock townBlock, WorldCoord worldCoord, War warEvent) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == townBlock.getTown();
				TownyMessaging.sendDebugMsg("[WAR] Ole8pieTesting: (For townBlock:" + edgeTownBlock.getCoord().toString() + ")  SameTown:" + sameTown + "  IsWarZone:" + warEvent.isWarZone(edgeTownBlock.getWorldCoord()));
				if (!sameTown || (sameTown && !warEvent.isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
	
}
