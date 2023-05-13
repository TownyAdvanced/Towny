package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.spawnlevel.NationSpawnLevel;
import com.palmergames.bukkit.towny.object.spawnlevel.TownSpawnLevel;

public class SpawnInfo {
	public boolean eventCancelled;
	public String eventCancellationMessage;
	public Town town;
	public Nation nation;
	public double travelCost;
	public TownSpawnLevel townSpawnLevel;
	public NationSpawnLevel nationSpawnLevel;
	public Resident resident;
	public int cooldown;


	public SpawnInfo() {
		this.eventCancelled = false;
		this.eventCancellationMessage = null;
		this.town = null;
		this.nation = null;
		this.travelCost = 0;
		townSpawnLevel = null;
		nationSpawnLevel = null;
		resident = null;
		cooldown = 0;
	}
	
}
