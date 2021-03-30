package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;

public class DrawSpawnPointsTask extends TownyTimerTask {
	public DrawSpawnPointsTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		TownyUniverse.getInstance().getSpawnPoints().values().stream()
			.forEach(spawn -> spawn.drawParticle());
	}
}
