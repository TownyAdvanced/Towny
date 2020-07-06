package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;

public class CleanupBackupTask implements Runnable {

	@Override
	public void run() {
		TownyUniverse.getInstance().getDataSource().cleanupBackups();
		Towny.getPlugin().getLogger().info("Successfully cleaned backups.");
	}
}
