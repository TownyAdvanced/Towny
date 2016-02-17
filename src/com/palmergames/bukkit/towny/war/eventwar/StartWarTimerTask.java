package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {
        if (!TownySettings.isUsingEconomy()) {
        	TownyMessaging.sendErrorMsg("War Event cannot function while using_economy: false in the config.yml. Economy Required.");
        	return;
        } else {
        	universe.getWarEvent().start();
        }
	}
}