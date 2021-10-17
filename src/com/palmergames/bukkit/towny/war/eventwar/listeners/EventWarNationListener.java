package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

public class EventWarNationListener implements Listener {

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onNationToggleNeutral(NationToggleNeutralEvent event) {
		if (!TownySettings.isDeclaringNeutral() && event.getFutureState()) {
			event.setCancelled(true);
			event.setCancelMessage(Translatable.of("msg_err_fight_like_king").forLocale(event.getPlayer()));
		}
	}

	@EventHandler
	public void onTownLeavesNation(NationPreTownLeaveEvent event) { // Also picks up towns being kicked using /n kick.
		if (event.getTown().hasActiveWar()) {
			event.setCancelled(true);
			event.setCancelMessage(Translation.of("msg_war_cannot_do"));
		}
	}
}
