package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;

import java.io.IOException;

public class CleanupBackupTask implements Runnable {

	@Override
	public void run() {
		try {
			TownyUniverse.getInstance().getDataSource().backup();
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg("Could not clean backup.");
			e.printStackTrace();
			return;
		}
		
		Towny.getPlugin().getLogger().info("Successfully cleaned backups.");
	}
}
