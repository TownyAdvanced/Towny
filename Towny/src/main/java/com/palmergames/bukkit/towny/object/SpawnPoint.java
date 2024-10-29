package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.util.BukkitParticle;

import org.bukkit.World;

public class SpawnPoint {
	private final Position position;
	private final WorldCoord wc;
	private final SpawnPointType type;
	private final SpawnPointLocation spawnLocation;
	
	private static final List<RingCoord> RING_PATTERN = createRingOffsets();
	public static final int RING_POINT_COUNT = 12;
	public static final int RING_DELAY_TICKS = 4;
	
	public SpawnPoint(Location loc, SpawnPointType type) {
		this(Position.ofLocation(loc), type);
	}
	
	public SpawnPoint(Position pos, SpawnPointType type) {
		this.position = pos;
		this.type = type;
		this.wc = pos.worldCoord();
		this.spawnLocation = SpawnPointLocation.parsePos(pos);
	}

	public WorldCoord getWorldCoord() {
		return wc;
	}
	
	public SpawnPointType getType() {
		return type;
	}

	public Location getBukkitLocation() {
		return this.position.asLocation();
	}
	
	public Position getPosition() {
		return this.position;
	}
	
	public SpawnPointLocation getSpawnPointLocation() {
		return spawnLocation;
	}

	public void drawParticle() {
		if (!Towny.getPlugin().isEnabled())
			return;
		
		final World world = position.world().getBukkitWorld();
		if (world == null)
			return;
		
		Location origin = centreLocation(position.asLocation());
		int i = 0;

		for (RingCoord ringPosition : RING_PATTERN) {
			Location point = origin.clone().add(ringPosition.x(), 0.0d, ringPosition.z());
			Towny.getPlugin().getScheduler().runAsyncLater(() -> {
				try {
					// This can potentially throw an exception if we're running this async and a player disconnects while it's sending particles.
					world.spawnParticle(BukkitParticle.getSpawnPointParticle(), point, 1, 0.0, 0.0, 0.0, 0.0);
				} catch (Exception ignored) {}
			}, (long) i * RING_DELAY_TICKS);
			i++;
		}
	}
	
	private Location centreLocation(Location loc) {
		loc.setX(Math.floor(loc.getX()) + 0.5);
		loc.setY(Math.floor(loc.getY()) + 0.1);
		loc.setZ(Math.floor(loc.getZ()) + 0.5);
		return loc;
	}
	
	private static List<RingCoord> createRingOffsets() {
		ArrayList<RingCoord> ring = new ArrayList<>();

		final double radius = 0.45;
		final double angleIncrement = 2 * Math.PI / RING_POINT_COUNT;

		for (int i = 0; i < RING_POINT_COUNT; i++) {
			double angle = i * angleIncrement;
			double x = radius * Math.sin(angle);
			double y = radius * Math.cos(angle);
			ring.add(RingCoord.offset(x, y));
		}

		return ring;
	}
	
	public enum SpawnPointType {
		TOWN_SPAWN,
		NATION_SPAWN,
		OUTPOST_SPAWN,
		JAIL_SPAWN
	}
	
	private record RingCoord(double x, double z) {
		private static RingCoord offset(double a, double b) {
			return new RingCoord(a, b);
		}
	}
}
