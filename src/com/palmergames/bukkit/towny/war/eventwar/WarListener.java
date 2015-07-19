package com.palmergames.bukkit.towny.war.eventwar;

import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;

public class WarListener implements Listener {

	Towny plugin;

	public WarListener(Towny plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		System.out.println("[HUD]  player quit game");
		if (!TownyUniverse.isWarTime())
			return;
		War warEvent = plugin.getTownyUniverse().getWarEvent();
		//Removes a player from the HUD list on logout
		Player p = event.getPlayer();
		if (warEvent.isWarTime() && warEvent.getPlayersWithHUD().containsKey(p)){
			warEvent.togglePlayerHud(p);
		}
	}

	@EventHandler
	public void onPlayerMoveDuringWar(PlayerChangePlotEvent event) throws NotRegisteredException
	{
		System.out.println("[HUD]  player change plot");
		if (!TownyUniverse.isWarTime())
			return;
		War warEvent = plugin.getTownyUniverse().getWarEvent();
		Player p = event.getPlayer();
		if (!warEvent.getPlayersWithHUD().containsKey(p))
			return;
		warEvent.getPlayersWithHUD().get(p).updateLocation();
	}

	@EventHandler
	public void onTownScored (TownScoredEvent event)
	{
		System.out.println("[HUD]  town scored event caught");
		if (!TownyUniverse.isWarTime())
			return;
		//Update town score
		War warEvent = plugin.getTownyUniverse().getWarEvent();
		for (Resident r : event.getTown().getResidents())
		{
			Player player = BukkitTools.getPlayer(r.getName());
			if (player != null && warEvent.getPlayersWithHUD().containsKey(player))
				warEvent.getPlayersWithHUD().get(player).updateScore();
		}
		//Update top scores for all HUD users
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(warEvent.getTownScores());
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
		for (Entry<Player, WarHUD> hud : warEvent.getPlayersWithHUD().entrySet())
			hud.getValue().updateTopThree(first, second, third);
	}
}
