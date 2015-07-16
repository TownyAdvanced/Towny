package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WarHUD {

	Line R15 = new Line (" ", null, 15);
	Line HOMETOWN = new Line(ChatColor.GOLD + "", "", 14);
	Line SCORE = new Line (ChatColor.WHITE + "Points: ", "", 13);
	Line R12 = new Line ("  ", null, 12);
	Line LOCATIONTITLE = new Line (ChatColor.GOLD + "" + ChatColor.BOLD + "Location", null, 11);
	Line NATION = new Line (ChatColor.WHITE + "Nation: " + ChatColor.YELLOW, "", 10);
	Line TOWN = new Line (ChatColor.WHITE + "Town: " + ChatColor.YELLOW, "", 9);
	Line HOME = new Line ("" + ChatColor.RED, "", 8);
	
	ArrayList<Line> table = new ArrayList<Line>();
	
	Scoreboard board;
	Objective obj;
	Objective buffer;
	Towny plugin;
	Player p;
	
	public WarHUD (Towny plugin, Player p) {
		this.plugin = plugin;
		this.p = p;
		ScoreboardManager manager = Bukkit.getScoreboardManager();
	    board = manager.getNewScoreboard();
		obj = board.registerNewObjective("OBJ", "dummy");
		buffer = board.registerNewObjective("BUFFER", "dummy");
		updateHomeTown(false);
		updateScore(false);
		updateLocation(false);
		update();
		p.setScoreboard(board);
	}
	
	public void updateHomeTown(boolean scoreboardUpdate)
	{
		try {
			HOMETOWN.value = TownyUniverse.getDataSource().getResident(p.getName()).getTown().getName();
		} catch (NotRegisteredException e) {
			HOMETOWN.value = "";
		}
		if (scoreboardUpdate)
			update();
	}
	
	public void updateScore(boolean scoreboardUpdate)
	{
		try {
			Town home = TownyUniverse.getDataSource().getResident(p.getName()).getTown();
			SCORE.value = plugin.getTownyUniverse().getWarEvent().getTownScores().get(home) + "";
		} catch (NotRegisteredException e) {
			SCORE.value = "";
		}
	}
	
	/**
	 * Update the location variables of the player. 
	 * 
	 * @param scoreboardUpdate If true, the score board will be rebuilt with the new data
	 * @throws NotRegisteredException
	 */
	public void updateLocation(boolean scoreboardUpdate)
	{
		WorldCoord worldCoord = new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p));
		try {
			TOWN.value = worldCoord.getTownBlock().getTown().getName();
			if (worldCoord.getTownBlock().isHomeBlock())
				HOME.value = "HOME BLOCK";
			else
				HOME.value = "";
		} catch (NotRegisteredException e) { TOWN.value = "Wilderness"; HOME.value = "";}
		try {
			NATION.value = worldCoord.getTownBlock().getTown().getNation().getName();
		} catch (NotRegisteredException e) { NATION.value = ""; }
		if (scoreboardUpdate)
			update();
	}
	
	public void update()
	{
		buffer.unregister();
		buffer = board.registerNewObjective("BUFFER", "dummy");
		buffer.setDisplayName(ChatColor.GOLD + "WAR");
		buffer.getScore(R15.label).setScore(R15.row);
		buffer.getScore(HOMETOWN.label).setScore(HOMETOWN.row);
		buffer.getScore(SCORE.label + SCORE.value).setScore(SCORE.row);
		buffer.getScore(R12.label).setScore(R12.row);
		buffer.getScore(LOCATIONTITLE.label).setScore(LOCATIONTITLE.row);
		buffer.getScore(NATION.label + NATION.value).setScore(NATION.row);
		buffer.getScore(TOWN.label + TOWN.value).setScore(TOWN.row);
		buffer.getScore(HOME.label + HOME.value).setScore(HOME.row);
		buffer.setDisplaySlot(DisplaySlot.SIDEBAR);
		Objective temp = obj;
		obj = buffer;
		buffer = temp;
	}
	
	static class Line {
		
		String label;
		String value;
		int row;
		
		public Line (String label, String value, int row)
		{
			this.label = label;
			this.value = value;
			this.row = row;
		}
	}
	
}
