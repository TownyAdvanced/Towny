package com.palmergames.bukkit.towny.huds;

import java.util.Hashtable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.War;

public class WarHUD {

	final static int home_health = TownySettings.getWarzoneHomeBlockHealth();
	final static int town_health = TownySettings.getWarzoneTownBlockHealth();

	public static void updateLocation(Player p, WorldCoord at) {
		String nation_loc, town_loc, homeblock;
		try {
			town_loc = at.getTownBlock().getTown().getName();
			if (at.getTownBlock().isHomeBlock())
				homeblock = "HOMEBLOCK";
			else
				homeblock = "";
		} catch (NotRegisteredException e) {town_loc = "Wilderness"; homeblock = "";}
		try {
			nation_loc = at.getTownBlock().getTown().getNation().getName();
		} catch (NotRegisteredException e) {nation_loc = "";}
		p.getScoreboard().getTeam("nation").setSuffix(HUDManager.check(nation_loc));
		p.getScoreboard().getTeam("town").setSuffix(HUDManager.check(town_loc));
		p.getScoreboard().getTeam("home").setSuffix(HUDManager.check(homeblock));
	}

	public static void updateAttackable(Player p, WorldCoord at, War war) {
		if (!TownySettings.getOnlyAttackEdgesInWar())
			return;
		String onEdge;
		if (isOnEdgeOfTown(at, war))
			onEdge = "True";
		else
			onEdge = "False";
		p.getScoreboard().getTeam("edge").setSuffix(HUDManager.check(onEdge));
	}

	public static void updateHealth(Player p, WorldCoord at, War war) {
		String health;
		boolean isTown = false;
		try { 
			if (War.isWarZone(at.getTownBlock().getWorldCoord())) {
				health = war.getWarZone().get(at) + "" + ChatColor.AQUA + "/" + (at.getTownBlock().isHomeBlock() ? home_health : town_health);
			} else {
				isTown = true;
				if (at.getTownBlock().getTown().getNation().isNeutral())
					health = "Peaceful";
				else
					health = "Fallen";
			}
		} catch (NotRegisteredException e) {
			if (isTown)
				health = "Peaceful";
			else
				health = "";
		}
		p.getScoreboard().getTeam("health").setSuffix(health);
	}

	public static void updateHealth (Player p, int health, boolean home) {
		if (health > 0) 
			p.getScoreboard().getTeam("health").setSuffix(health + "" + ChatColor.AQUA + "/" + (home ? home_health : town_health));
		else {
			p.getScoreboard().getTeam("health").setSuffix("Fallen");
			if (TownySettings.getOnlyAttackEdgesInWar())
				p.getScoreboard().getTeam("edge").setSuffix("False");
		}
	}

	public static void updateHomeTown(Player p) {
		String homeTown;
		try {
			homeTown = TownyUniverse.getDataSource().getResident(p.getName()).getTown().getName();
		} catch (NotRegisteredException e) {
			homeTown = "Townless!";
		}
		p.getScoreboard().getTeam("town_title").setSuffix(HUDManager.check(homeTown));
	}

	public static void updateScore(Player p, War war) {
		String score;
		try {
			Town home = TownyUniverse.getDataSource().getResident(p.getName()).getTown();
			Hashtable<Town, Integer> scores = war.getTownScores();
			if (scores.containsKey(home))
				score = scores.get(home) + "";
			else
				score = "";
		} catch (NotRegisteredException e) {score = "";}
		p.getScoreboard().getTeam("town_score").setSuffix(HUDManager.check(score));
	}

	public static void updateTopScores(Player p, String[] top) {
		String fprefix = top[0].contains("-") ? ChatColor.GOLD + top[0].split("-")[0] + ChatColor.WHITE + "-": "";
		String sprefix = top[1].contains("-") ? ChatColor.GRAY + top[1].split("-")[0] + ChatColor.WHITE + "-": "";
		String tprefix = top[2].contains("-") ? ChatColor.GRAY + top[2].split("-")[0] + ChatColor.WHITE + "-": "";
		String fsuffix = top[0].contains("-") ? top[0].split("-")[1] : "";
		String ssuffix = top[1].contains("-") ? top[1].split("-")[1] : "";
		String tsuffix = top[2].contains("-") ? top[2].split("-")[1] : "";
		p.getScoreboard().getTeam("first").setPrefix(HUDManager.check(fprefix));
		p.getScoreboard().getTeam("first").setSuffix(HUDManager.check(fsuffix));
		p.getScoreboard().getTeam("second").setPrefix(HUDManager.check(sprefix));
		p.getScoreboard().getTeam("second").setSuffix(HUDManager.check(ssuffix));
		p.getScoreboard().getTeam("third").setPrefix(HUDManager.check(tprefix));
		p.getScoreboard().getTeam("third").setSuffix(HUDManager.check(tsuffix));
	}

	public static void updateScore(Player p, int score) {
		p.getScoreboard().getTeam("town_score").setSuffix(HUDManager.check(score + ""));
	}

	@SuppressWarnings("deprecation")
	public static void toggleOn (Player p, War war) {
		boolean edges = TownySettings.getOnlyAttackEdgesInWar();
		String WAR_HUD_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "War";
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
		//init objective
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = board.registerNewObjective("WAR_HUD_OBJ", "dummy");
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.setDisplayName(WAR_HUD_TITLE);
		//register teams
		Team space1 = board.registerNewTeam("space1");
		Team town_title = board.registerNewTeam("town_title");
		Team town_score = board.registerNewTeam("town_score");
		Team space2 = board.registerNewTeam("space2");
		Team location_title = board.registerNewTeam("location_title");
		Team nation = board.registerNewTeam("nation");
		Team town = board.registerNewTeam("town");
		Team health = board.registerNewTeam("health");
		Team home = board.registerNewTeam("home");
		Team space3 = board.registerNewTeam("space3");
		Team top_title = board.registerNewTeam("top_title");
		Team first = board.registerNewTeam("first");
		Team second = board.registerNewTeam("second");
		Team third = board.registerNewTeam("third");
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
			Team edge = board.registerNewTeam("edge");
			edge.addPlayer(Bukkit.getOfflinePlayer(edge_player));
			obj.getScore(edge_player).setScore(7);
		}
		//set the board
		p.setScoreboard(board);
		WorldCoord at = new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p));
		updateLocation(p, at);
		updateAttackable(p, at, war);
		updateHealth(p, at, war);
		updateHomeTown(p);
		updateScore(p, war);
		updateTopScores(p, war.getTopThree());
	}

	public static boolean isOnEdgeOfTown(WorldCoord worldCoord, War war) {

		Town currentTown;
		
		//Checks to make sure the worldCoord is actually in war
		try {
			currentTown = worldCoord.getTownBlock().getTown();
			if (!War.isWarZone(worldCoord))
				return false;
		} catch (NotRegisteredException e) {
			return false;
		}
		
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == currentTown;
				if (!sameTown || (sameTown && !War.isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}
