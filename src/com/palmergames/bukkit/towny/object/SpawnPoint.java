package com.palmergames.bukkit.towny.object;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.palmergames.bukkit.towny.Towny;

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
	
	public SpawnPointLocation getSpawnPointLocation() {
		return new SpawnPointLocation(location);
	}

	public void drawParticle() {
		Bukkit.getWorld(location.getWorld().getName()).spawnParticle(Particle.WATER_SPLASH, centreLocation(location), 20, 0.0, 0.0, 0.0, 0.1);
		if (Towny.is116Plus())
			Bukkit.getWorld(location.getWorld().getName()).spawnParticle(Particle.SOUL_FIRE_FLAME, centreLocation(location), 4, 0.15, 0.0, 0.15, 0.0);
	}
	
	private Location centreLocation(Location loc) {
		loc.setX(Math.floor(loc.getX()) + 0.5);
		loc.setY(Math.floor(loc.getY()) + 0.1);
		loc.setZ(Math.floor(loc.getZ()) + 0.5);
		return loc;
	}
	
	public enum SpawnPointType {
		TOWN_SPAWN,
		NATION_SPAWN,
		OUTPOST_SPAWN,
		JAIL_SPAWN
	}
	
	public class SpawnPointLocation {
		private final int x;
		private final int y;
		private final int z;
		
		public SpawnPointLocation(Location loc) {
			this.x = loc.getBlockX();
			this.y = loc.getBlockY();
			this.z = loc.getBlockZ();
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getZ() {
			return z;
		}
	}
}
