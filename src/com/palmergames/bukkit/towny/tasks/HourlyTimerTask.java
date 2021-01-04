package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;

/**
 * This class represents the hourly timer task
 * It is generally set to run once per hour
 * This rate can be configured.
 *
 * @author Goosius
 */
public class HourlyTimerTask extends TownyTimerTask {

	public HourlyTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownRuinSettings.getTownRuinsEnabled()) {
			TownRuinUtil.evaluateRuinedTownRemovals();
		}
		
		if (TownySettings.getInviteExpirationTime() > 0)
			InviteHandler.searchForExpiredInvites();
		
		/*
		 * Fire an event other plugins can use.
		 */
		Bukkit.getPluginManager().callEvent(new NewHourEvent(System.currentTimeMillis()));
	}
}