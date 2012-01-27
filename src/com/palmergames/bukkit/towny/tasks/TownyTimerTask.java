package com.palmergames.bukkit.towny.tasks;

import java.util.TimerTask;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public abstract class TownyTimerTask extends TimerTask {
	protected TownyUniverse universe;
	protected Towny plugin;

	public TownyTimerTask(TownyUniverse universe) {
		this.universe = universe;
		this.plugin = TownyUniverse.getPlugin();
	}

	//@Override
	//public void run() {

	//}

}
