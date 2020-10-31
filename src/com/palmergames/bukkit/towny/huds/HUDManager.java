package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.PlotAttackedEvent;
import com.palmergames.bukkit.towny.war.eventwar.TownScoredEvent;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

public class HUDManager implements Listener{

	ArrayList<Player> warUsers;
	ArrayList<Player> permUsers;

	public HUDManager (Towny plugin) {
		warUsers = new ArrayList<>();
		permUsers = new ArrayList<>();
	}

	//**TOGGLES**//
	public void toggleWarHUD (Player p) {
		if (!warUsers.contains(p)){
			toggleAllOff(p);
			warUsers.add(p);
			WarHUD.toggleOn(p, TownyUniverse.getInstance().getWarEvent());
		} else 
			toggleAllOff(p);
	}

	public void togglePermHUD (Player p) {
		if (!permUsers.contains(p)) {
			toggleAllOff(p);
			permUsers.add(p);
			PermHUD.toggleOn(p);
		} else 
			toggleAllOff(p);
	}

	public void toggleAllWarHUD (War war) {
		for (Player p : warUsers)
			if (TownyUniverse.getInstance().getWarEvent(p).equals(war))
				toggleOff(p);
		warUsers.clear();
	}

	public void toggleAllOff (Player p) {
		warUsers.remove(p);
		permUsers.remove(p);
		if (p.isOnline()) toggleOff(p);
	}

	public void toggleAllOffForQuit (Player p) {
		warUsers.remove(p);
		permUsers.remove(p);
	}
	
	public static void toggleOff(Player p) {
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	}

	//**EVENTS**//
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		toggleAllOffForQuit(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) throws NotRegisteredException
	{
		Player p = event.getPlayer();
		if (warUsers.contains(p)) {
			WarHUD.updateLocation(p, event.getTo());
			WarHUD.updateAttackable(p, event.getTo(), TownyUniverse.getInstance().getWarEvent());
			WarHUD.updateHealth(p, event.getTo(), TownyUniverse.getInstance().getWarEvent(p));
		} else if (permUsers.contains(p) && p.getScoreboard().getTeam("plot") != null) {
			if (event.getTo().getTownyWorld().isUsingTowny())
				PermHUD.updatePerms(p, event.getTo());
			else
				toggleOff(p);
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
	public void onTownScored (TownScoredEvent event)
	{
		//Update town score
		War warEvent = TownyUniverse.getInstance().getWarEvent();
		for (Resident r : event.getTown().getResidents())
		{
			Player player = BukkitTools.getPlayer(r.getName());
			if (player != null && warUsers.contains(player))
				WarHUD.updateScore(player, event.getScore());
		}
		//Update top scores for all HUD users
		String[] top = warEvent.getTopThree();
		for (Player p : warUsers)
			WarHUD.updateTopScores(p, top);
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

	//**UTILS**//
	public static String check(String check) {
		return check.length() > 16 ? check.substring(0, 16) : check;
	}
}
