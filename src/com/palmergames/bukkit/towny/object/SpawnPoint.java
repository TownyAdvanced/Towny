package com.palmergames.bukkit.towny.object;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.palmergames.bukkit.towny.Towny;

public class SpawnPoint {
	private final Location location;
	private final WorldCoord wc;
	private final SpawnPointType type;
	private final SpawnPointLocation spawnLocation;
	
	public SpawnPoint(Location loc, SpawnPointType type) {
		this.location = loc;
		this.type = type;
		this.wc = WorldCoord.parseWorldCoord(loc);
		this.spawnLocation = new SpawnPointLocation(loc);
	}

	public WorldCoord getWorldCoord() {
		return wc;
	}
	
	public SpawnPointType getType() {
		return type;
	}

	public Location getBukkitLocation() {
		return location;
	}
	
	public SpawnPointLocation getSpawnPointLocation() {
		return spawnLocation;
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
	
	/**
	 * A worldname, x, y, z location used to validate SpawnPoints. 
	 */
	public class SpawnPointLocation {
		private final String world; 
		private final int x;
		private final int y;
		private final int z;
		
		public SpawnPointLocation(Location loc) {
			this.world = loc.getWorld().getName();
			this.x = loc.getBlockX();
			this.y = loc.getBlockY();
			this.z = loc.getBlockZ();
		}

		public String getWorld() {
			return world;
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
