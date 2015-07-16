package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;

public class WarListener implements Listener {

	War warEvent;
	
	public WarListener(Towny plugin)
	{
		warEvent = plugin.getTownyUniverse().getWarEvent();
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		//Removes a player from the HUD list on logout
		Player p = event.getPlayer();
		if (warEvent.isWarTime() && warEvent.getPlayersWithHUD().containsKey(p)){
			warEvent.togglePlayerHud(p);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) throws NotRegisteredException
	{
		Player p = event.getPlayer();
		if (!warEvent.getPlayersWithHUD().containsKey(p))
			return;
		if (!event.getFrom().getChunk().equals(event.getTo().getChunk()))
		{
			warEvent.getPlayersWithHUD().get(p).updateLocation(true);
		}
	}
	
	@EventHandler
	public void onTownScored (TownScoredEvent event)
	{
		for (Resident r : event.getTown().getResidents())
		{
			Player player = BukkitTools.getPlayer(r.getName());
			if (!warEvent.getPlayersWithHUD().containsKey(player))
				continue;
			warEvent.getPlayersWithHUD().get(player).updateScore(true);
		}
	}
}
