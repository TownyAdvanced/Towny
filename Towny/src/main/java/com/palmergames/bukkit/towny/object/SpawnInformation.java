package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.spawnlevel.NationSpawnLevel;
import com.palmergames.bukkit.towny.object.spawnlevel.TownSpawnLevel;

/**
 * This is an internal class used by towny to pass-around info related to spawning
 */
public class SpawnInformation {
	public boolean eventCancelled;
	public String eventCancellationMessage;
	public double travelCost;
	public TownSpawnLevel townSpawnLevel;
	public NationSpawnLevel nationSpawnLevel;
	public int cooldown;


	public SpawnInformation() {
		this.eventCancelled = false;
		this.eventCancellationMessage = null;
		this.travelCost = 0;
		this.townSpawnLevel = null;
		this.nationSpawnLevel = null;
		this.cooldown = 0;
	}
	
}
