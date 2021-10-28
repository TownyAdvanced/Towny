package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.palmergames.bukkit.towny.war.eventwar.WarUniverse;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;

public class EventWarBukkitListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		War war = WarUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().addOnlineWarrior(event.getPlayer());
		war.getScoreManager().sendScores(event.getPlayer(), 3);

	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		War war = WarUniverse.getInstance().getWarEvent(event.getPlayer());
		if (war == null)
			return;
		war.getWarParticipants().removeOnlineWarrior(event.getPlayer());
	}
}
