package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {

		universe.getWarEvent().start();
	}

}
