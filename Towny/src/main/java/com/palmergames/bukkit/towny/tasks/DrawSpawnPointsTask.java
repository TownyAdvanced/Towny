package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.SpawnPoint;
import org.bukkit.NamespacedKey;

public class DrawSpawnPointsTask extends TownyTimerTask {
	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:spawnpoint-task"), () -> TownyTimerHandler.toggleDrawSpointsTask(TownySettings.getVisualizedSpawnPointsEnabled()));
	}
	
	public DrawSpawnPointsTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		for (SpawnPoint spawnPoint : TownyUniverse.getInstance().getSpawnPoints().values())
			spawnPoint.drawParticle();
	}
}
