package com.palmergames.bukkit.towny.huds;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.war.eventwar.PlotAttackedEvent;
import com.palmergames.bukkit.towny.war.eventwar.TownScoredEvent;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;

public class HUDManager implements Listener{

	ArrayList<Player> warUsers;

	Towny plugin;
	public HUDManager (Towny plugin) {
		this.plugin = plugin;
		warUsers = new ArrayList<Player>();
	}

	//**TOGGLES**//
	public void toggleWarHUD (Player p) {
		if (!warUsers.contains(p)){
			toggleAllOff(p);
			WarHUD.toggleOn(p, plugin.getTownyUniverse().getWarEvent());
		} else {
			toggleAllOff(p);
		}
	}

	public void toggleAllWarHUD () {
		for (Player p : warUsers)
			toggleOff(p);
		warUsers.clear();
	}

	public void toggleAllOff (Player p) {
		warUsers.remove(p);
		toggleOff(p);
	}
	
	public static void toggleOff(Player p) {
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
	}

	//**EVENTS**//
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		toggleAllOff(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) throws NotRegisteredException
	{
		Player p = event.getPlayer();
		if (warUsers.contains(p)) {
			WarHUD.updateLocation(p, event.getTo());
			WarHUD.updateAttackable(p, event.getTo(), plugin.getTownyUniverse().getWarEvent());
			WarHUD.updateHealth(p, event.getTo(), plugin.getTownyUniverse().getWarEvent());
		}
	}

	//War specific//
	@EventHandler
	public void onPlotAttacked(PlotAttackedEvent event) 
	{
		TownBlock townBlock = event.getTownBlock();
		for (Player p : warUsers){
			WorldCoord worldCoord = new WorldCoord(p.getWorld().getName(), Coord.parseCoord(p));
			boolean home = townBlock.isHomeBlock();
			try {
				if (worldCoord.getTownBlock().equals(townBlock))
					WarHUD.updateHealth(p, event.getHP(), home);
			} catch (NotRegisteredException e) {}
		}
	}

	@EventHandler
	public void onTownScored (TownScoredEvent event)
	{
		//Update town score
		War warEvent = plugin.getTownyUniverse().getWarEvent();
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

	//**UTILS**//
	public static String check(String check) {
		return check.length() > 16 ? check.substring(0, 16) : check;
	}
}
