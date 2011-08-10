package com.palmergames.bukkit.towny;

import java.util.TimerTask;

import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyTimerTask extends TimerTask {
	protected TownyUniverse universe;
	protected Towny plugin;

	public TownyTimerTask(TownyUniverse universe) {
		this.universe = universe;
		this.plugin = universe.getPlugin();
	}

	@Override
	public void run() {

	}

}
