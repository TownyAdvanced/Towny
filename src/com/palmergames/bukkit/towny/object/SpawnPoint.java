package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.palmergames.bukkit.towny.Towny;

public class SpawnPoint {
	private final Location location;
	private final WorldCoord wc;
	private final SpawnPointType type;
	private final SpawnPointLocation spawnLocation;
	
	private static final ArrayList<RingCoord> RING_PATTERN = createRing();
	
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

		for (RingCoord ringPosition : RING_PATTERN) {
		    Location point = origin.clone().add(ringPosition.getX(), 0.0d, ringPosition.getZ());
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
	
	private static ArrayList<RingCoord> createRing() {
		ArrayList<RingCoord> ring = new ArrayList<>();
		ring.add(RingCoord.of(0.0, 0.45));
		ring.add(RingCoord.of(0.225, 0.3897));
		ring.add(RingCoord.of(0.3897, 0.225));
		ring.add(RingCoord.of(0.45, 0.00));
		ring.add(RingCoord.of(0.3897, -0.225));
		ring.add(RingCoord.of(0.225, -0.3897));
		ring.add(RingCoord.of(0.00, -0.45));
		ring.add(RingCoord.of(-0.225, -0.3897));
		ring.add(RingCoord.of(-0.3897, -0.225));
		ring.add(RingCoord.of(-0.45, 0.0));
		ring.add(RingCoord.of(-0.3897, 0.225));
		ring.add(RingCoord.of(-0.225, 0.3897));
		return ring;		
	}
	
	public enum SpawnPointType {
		TOWN_SPAWN,
		NATION_SPAWN,
		OUTPOST_SPAWN,
		JAIL_SPAWN
	}
	
	private static class RingCoord {
		private double x;
		private double z;
		
		private RingCoord(double x, double z) {
			this.x = x;
			this.z = z;
		}
		
		private double getX() {
			return this.x;
		}
		
		private double getZ() {
			return this.z;
		}
		
		private static RingCoord of(double a, double b) {
			return new RingCoord(a, b);
		}
	}
}
