package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;

import java.util.TimerTask;

public abstract class TownyTimerTask extends TimerTask {

	protected TownyUniverse universe;
	protected Towny plugin;

	public TownyTimerTask(Towny plugin) {
		this.plugin = plugin;
		this.universe = TownyUniverse.getInstance();
		
	}
}
