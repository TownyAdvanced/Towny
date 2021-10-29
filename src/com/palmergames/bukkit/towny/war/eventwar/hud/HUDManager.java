package com.palmergames.bukkit.towny.war.eventwar.hud;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.war.eventwar.WarUniverse;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.eventwar.events.PlotAttackedEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.TownScoredEvent;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;

public class HUDManager implements Listener {

	static List<Player> warUsers;
	
	public HUDManager() {
		warUsers = new ArrayList<>();
	}

	public static void toggleWarHUD (Player p) {
		if (!warUsers.contains(p)){
			War war = WarUniverse.getInstance().getWarEvent(p);
			if (war != null) {
				com.palmergames.bukkit.towny.huds.HUDManager.toggleAllOff(p);
				warUsers.add(p);
				WarHUD.toggleOn(p, war);
			}
		} else 
			p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	}
	
	public static void toggleAllWarHUD (War war) {
		for (Player p : warUsers)
			if (WarUtil.sameWar(WarUniverse.getInstance().getWarEvent(p), war)) {
				p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
				warUsers.remove(p);
			}
	}
	
	public static boolean isWarHUDActive(Player player) {
		return player.getScoreboard().getTeam("space1") != null;
	}
	
	/* EVENTS */
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		warUsers.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) throws NotRegisteredException {
		Player p = event.getPlayer();
		if (warUsers.contains(p)) {
			if (!isWarHUDActive(p))
				warUsers.remove(p);
			else {
				War war = WarUniverse.getInstance().getWarEvent(p);
				WarHUD.updateLocation(p, event.getTo());
				WarHUD.updateAttackable(p, event.getTo(), war);
				WarHUD.updateHealth(p, event.getTo(), war);
			}
		}
	}
	
	@EventHandler
	public void onPlotAttacked(PlotAttackedEvent event) 
	{
		boolean home = event.getTownBlock().isHomeBlock();
		for (Player p : event.getPlayers()){
			if (warUsers.contains(p))
				WarHUD.updateHealth(p, event.getHP(), home);
		}
	}

	@EventHandler
	public void onTownScored (TownScoredEvent event) {
		//Update town score
		War war = event.getWar();
		for (Resident r : event.getTown().getResidents()) {
			if (!r.isOnline())
				continue;
			Player player = r.getPlayer();
			if (player != null && warUsers.contains(player))
				WarHUD.updateScore(player, event.getScore());
		}
		//Update top scores for all HUD users
		String[] top = war.getScoreManager().getTopThree();
		for (Player p : war.getWarParticipants().getOnlineWarriors()) {
			if (!warUsers.contains(p))
				continue;
			WarHUD.updateTopScores(p, top);
		}
	}
}
