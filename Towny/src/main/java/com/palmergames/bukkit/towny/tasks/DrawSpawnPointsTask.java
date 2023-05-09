package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.SpawnPoint;

public class DrawSpawnPointsTask extends TownyTimerTask {
	public DrawSpawnPointsTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		for (SpawnPoint spawnPoint : TownyUniverse.getInstance().getSpawnPoints().values())
			spawnPoint.drawParticle();
	}
}
