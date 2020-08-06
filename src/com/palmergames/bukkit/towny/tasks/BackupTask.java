package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;

import java.io.IOException;

public class BackupTask implements Runnable {
	@Override
	public void run() {

		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();
		TownyUniverse universe = TownyUniverse.getInstance();
		try {
			dataSource.backup();

			if (universe.getLoadDbType().equalsIgnoreCase("flatfile") || universe.getSaveDbType().equalsIgnoreCase("flatfile")) {
				dataSource.deleteUnusedResidents();
			}

		} catch (IOException e) {
			System.out.println("[Towny] Error: Could not create backup.");
			e.printStackTrace();
			return;
		}

		Towny.getPlugin().getLogger().info("Towny Database Successfully backed up.");
	}
}
