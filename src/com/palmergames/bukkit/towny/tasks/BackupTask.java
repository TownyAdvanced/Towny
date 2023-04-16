package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;

import java.io.IOException;
import java.util.logging.Level;

public class BackupTask implements Runnable {
	@Override
	public void run() {

		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();
		Towny.getPlugin().getLogger().info("Making backup...");
		try {
			dataSource.backup();
		} catch (IOException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "Error: Could not create backup.", e);
			return;
		}

		Towny.getPlugin().getLogger().info("Towny flatfiles and settings successfully backed up.");
	}
}
