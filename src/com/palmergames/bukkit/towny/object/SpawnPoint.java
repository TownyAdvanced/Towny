package com.palmergames.bukkit.towny.object;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

public class SpawnPoint {
	private final Location location;
	private final WorldCoord wc;
	private final SpawnPointType type;
	
	public SpawnPoint(Location loc, SpawnPointType type) {
		this.location = loc;
		this.type = type;
		this.wc = WorldCoord.parseWorldCoord(loc);
	}

	public WorldCoord getWorldCoord() {
		return wc;
	}
	
	public SpawnPointType getType() {
		return type;
	}

	public Location getLocation() {
		return location;
	}

	public enum SpawnPointType {
		TOWN_SPAWN,
		NATION_SPAWN,
		OUTPOST_SPAWN,
		JAIL_SPAWN
	}

	public void drawParticle() {
		Bukkit.getWorld(location.getWorld().getName()).spawnParticle(Particle.WATER_SPLASH, centreLocation(location), 100);	
	}
	
	private Location centreLocation(Location loc) {
		loc.setX(Math.floor(loc.getX()) + 0.5);
		loc.setY(Math.floor(loc.getY()) + 0.1);
		loc.setZ(Math.floor(loc.getZ()) + 0.5);
		return loc;
	}
}
