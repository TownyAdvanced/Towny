package com.palmergames.bukkit.towny.object;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.palmergames.bukkit.towny.Towny;

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
		int i = 0;

		for (Entry<Double, Double> ringPosition : RING_PATTERN.entrySet()) {
		    Location point = origin.clone().add(ringPosition.getKey(), 0.0d, ringPosition.getValue());
		    Bukkit.getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), ()-> Bukkit.getWorld(location.getWorld().getName()).spawnParticle(Particle.CRIT_MAGIC, point, 1, 0.0, 0.0, 0.0, 0.0), i*4);		    
		    i++;
		}
	}
	
	private Location centreLocation(Location loc) {
		loc.setX(Math.floor(loc.getX()) + 0.5);
		loc.setY(Math.floor(loc.getY()) + 0.1);
		loc.setZ(Math.floor(loc.getZ()) + 0.5);
		return loc;
	}
	
	private static Map<Double, Double> createRing() {
		Map<Double, Double> ring = new LinkedHashMap<Double, Double>();
		ring.put(0.0, 0.45);
		ring.put(0.225, 0.3897);
		ring.put(0.3897, 0.225);
		ring.put(0.45, 0.00);
		ring.put(0.3897, -0.225);
		ring.put(0.225, -0.3897);
		ring.put(0.00, -0.45);
		ring.put(-0.225, -0.3897);
		ring.put(-0.3897, -0.225);
		ring.put(-0.45, 0.0);
		ring.put(-0.3897, 0.225);
		ring.put(-0.225, 0.3897);
		return ring;		
	}
	
	public enum SpawnPointType {
		TOWN_SPAWN,
		NATION_SPAWN,
		OUTPOST_SPAWN,
		JAIL_SPAWN
	}
}
