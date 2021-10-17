package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;

public class EventWarTownListener implements Listener {
	
	@EventHandler
	public void onTownMoveHomeblock(TownPreSetHomeBlockEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onNewTown(PreNewTownEvent event) {
		if (WarUtil.hasWorldWar(TownyAPI.getInstance().getTownyWorld(event.getPlayer().getWorld().getName()))) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownLeave(TownLeaveEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
	
	@EventHandler
	public void onTownClaim(TownPreClaimEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}

	@EventHandler
	public void onTownUnclaim(TownPreUnclaimCmdEvent event) {
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
}
