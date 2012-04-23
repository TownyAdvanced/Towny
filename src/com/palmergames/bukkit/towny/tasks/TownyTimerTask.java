package com.palmergames.bukkit.towny.tasks;

import java.util.TimerTask;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public abstract class TownyTimerTask extends TimerTask {

	protected TownyUniverse universe;
	protected Towny plugin;

	public TownyTimerTask(Towny plugin) {

		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();
		
	}

	//@Override
	//public void run() {

	//}

}
