package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;

public abstract class TownyTimerTask implements Runnable {

	protected final TownyUniverse universe;
	protected final Towny plugin;

	public TownyTimerTask(Towny plugin) {
		this.plugin = plugin;
		this.universe = TownyUniverse.getInstance();
		
	}
}
