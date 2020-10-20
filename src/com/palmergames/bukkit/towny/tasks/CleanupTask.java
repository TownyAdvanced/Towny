package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;

public class CleanupTask implements Runnable {

	@Override
	public void run() {
		Towny.getPlugin().getLogger().info("Cleaning up old backups...");
		TownyUniverse.getInstance().getDataSource().cleanupBackups();
		Towny.getPlugin().getLogger().info("Successfully cleaned backups.");
		TownyUniverse.getInstance().getDataSource().cleanupPlotBlockData();
	}
}

