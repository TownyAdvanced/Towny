package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.eventwar.events.PlotAttackedEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.TownScoredEvent;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class HUDManager implements Listener{

	static List<Player> warUsers;
	static List<Player> permUsers;
	static List<Player> mapUsers;

	public HUDManager (Towny plugin) {
		warUsers = new ArrayList<>();
		permUsers = new ArrayList<>();
		mapUsers = new ArrayList<>();
	}

	//**TOGGLES**//
	public static void toggleWarHUD (Player p) {
		if (!warUsers.contains(p)){
			War war = TownyUniverse.getInstance().getWarEvent(p);
			if (war != null) {
				toggleAllOff(p);
				warUsers.add(p);
				WarHUD.toggleOn(p, war);
			}
		} else 
			toggleAllOff(p);
	}

	public static void togglePermHUD (Player p) {
		if (!permUsers.contains(p)) {
			toggleAllOff(p);
			permUsers.add(p);
			PermHUD.toggleOn(p);
		} else 
			toggleAllOff(p);
	}
	
	public static void toggleMapHud(Player player) {
		if (!mapUsers.contains(player)) {
			toggleAllOff(player);
			mapUsers.add(player);
			MapHUD.toggleOn(player);
		} else
			toggleAllOff(player);
	}

	public void toggleAllWarHUD (War war) {
		for (Player p : warUsers)
			if (WarUtil.sameWar(TownyUniverse.getInstance().getWarEvent(p), war)) {
				toggleOff(p);
				warUsers.remove(p);
			}
	}

	public static void toggleAllOff (Player p) {
		warUsers.remove(p);
		permUsers.remove(p);
		mapUsers.remove(p);
		if (p.isOnline())
			toggleOff(p);
	}
	
	public static void toggleOff(Player p) {
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	}

	//**EVENTS**//
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		warUsers.remove(event.getPlayer());
		permUsers.remove(event.getPlayer());
		mapUsers.remove(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) throws NotRegisteredException {
		Player p = event.getPlayer();
		if (warUsers.contains(p)) {
			if (!isWarHUDActive(p))
				warUsers.remove(p);
			else {
				War war = TownyUniverse.getInstance().getWarEvent(p);
				WarHUD.updateLocation(p, event.getTo());
				WarHUD.updateAttackable(p, event.getTo(), war);
				WarHUD.updateHealth(p, event.getTo(), war);
			}
		} else if (permUsers.contains(p)) {
			if (!isPermHUDActive(p))
				permUsers.remove(p);
			else {
				if (event.getTo().getTownyWorld().isUsingTowny())
					PermHUD.updatePerms(p, event.getTo());
				else
					toggleAllOff(p);
			}	
		} else if (mapUsers.contains(p)) {
			if (!isMapHudActive(p))
				mapUsers.remove(p);
			else
				if (event.getTo().getTownyWorld().isUsingTowny())
					MapHUD.updateMap(p, event.getTo());
				else
					toggleAllOff(p);
		}
	}

	//War specific//
	
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

	//Perm Specific
	@EventHandler
	public void onTownBlockSettingsChanged (TownBlockSettingsChangedEvent e) {

		if (e.getTownyWorld() != null)
			for (Player p : permUsers)
				PermHUD.updatePerms(p);
		else if (e.getTown() != null)
			for (Player p : permUsers)
				try {
					if (new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p)).getTownBlock().getTown() == e.getTown())
						PermHUD.updatePerms(p);
				} catch (Exception ex) {}
		else if (e.getTownBlock() != null)
			for (Player p : permUsers)
				try {
					if (new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p)).getTownBlock() == e.getTownBlock())
						PermHUD.updatePerms(p);
				} catch (Exception ex) {}
	}

	public static String check(String string) {
		return string.length() > 64 ? string.substring(0, 64) : string;
	}

	public static boolean isUsingTownyHUD(Player player) {
		return permUsers.contains(player) || mapUsers.contains(player) || warUsers.contains(player);
	}

	public static List<Player> getPermHUDUsers() {
		return permUsers;
	}

	public static List<Player> getWarHUDUsers() {
		return warUsers;
	}
	
	public static List<Player> getMapHUDUsers() {
		return mapUsers;
	}

	public static void removePermHUDUser(Player player) {
		if (permUsers.remove(player)) {
			toggleOff(player);
		}
	}

	public static void removeWarHUDUser(Player player) {
		if (warUsers.remove(player)) {
			toggleOff(player);
		}
	}
	
	public static void removeMapHUDUser(Player player) {
		if (mapUsers.remove(player)) {
			toggleOff(player);
		}
	}

	public static boolean isPermHUDActive(Player player) {
		return player.getScoreboard().getTeam("plot") != null;
	}

	public static boolean isWarHUDActive(Player player) {
		return player.getScoreboard().getTeam("space1") != null;
	}
	
	public static boolean isMapHudActive(Player player) {
		return player.getScoreboard().getTeam("mapTeam1") != null;
	}
}
