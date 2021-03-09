package com.palmergames.bukkit.towny.object;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

public class SpawnPoint {
	private final Location location;
	private final WorldCoord wc;
	private final SpawnPointType type;
	private final SpawnPointLocation spawnLocation;
	
	private static final Map<Double, Double> RING_PATTERN = createRing();
	
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
		Location origin = centreLocation(location);

		for (double posX : RING_PATTERN.keySet()) {
		    Location point = origin.clone().add(posX, 0.0d, RING_PATTERN.get(posX));
		    Bukkit.getWorld(location.getWorld().getName()).spawnParticle(Particle.CRIT_MAGIC, point, 1, 0.0, 0.0, 0.0, 0.0);
		}
		
	}
	
	private Location centreLocation(Location loc) {
		loc.setX(Math.floor(loc.getX()) + 0.5);
		loc.setY(Math.floor(loc.getY()) + 0.1);
		loc.setZ(Math.floor(loc.getZ()) + 0.5);
		return loc;
	}
	
	private static Map<Double, Double> createRing() {
		Map<Double, Double> ring = new HashMap<Double, Double>();
		ring.put(0.0, 0.45);
		ring.put(0.22499999999999998, 0.38971143170299744);
		ring.put(0.3897114317029974, 0.22500000000000006);
		ring.put(0.45, 0.000000000000000027554552980815448);
		ring.put(0.38971143170299744, -0.2249999999999999);
		ring.put(0.22499999999999998, -0.38971143170299744);
		ring.put(0.000000000000000055109105961630896, -0.45);
		ring.put(-0.22499999999999987, -0.3897114317029975);
		ring.put(-0.3897114317029973, -0.2250000000000002);
		ring.put(-0.45, 0.00000000000000000266365894244634);
		ring.put(-0.3897114317029974, 0.22500000000000006);
		ring.put(-0.2250000000000002, 0.38971143170299727);
		return ring;		
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
