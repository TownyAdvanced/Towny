package com.palmergames.bukkit.towny.war;

import com.palmergames.bukkit.towny.TownyTimerTask;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		universe.getWarEvent().start();
	}

}
