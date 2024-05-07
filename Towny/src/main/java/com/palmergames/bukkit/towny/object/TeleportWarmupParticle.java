package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import com.github.bsideup.jabel.Desugar;
import com.palmergames.bukkit.towny.Towny;

public class TeleportWarmupParticle {

	private static final List<RingCoord> RING_PATTERN = createRingOffsets();
	public static final int RING_POINT_COUNT = 12;
	public static final int RING_DELAY_TICKS = 2;
	public final Location loc;

	public TeleportWarmupParticle(Location loc) {
		this.loc = loc;
		drawParticle();
	}

	private void drawParticle() {
		final World world = loc.getWorld();
		if (world == null)
			return;
		int i = 0;
		for (RingCoord ringPosition : RING_PATTERN) {
			Location point = loc.clone().add(ringPosition.x(), 0.0, ringPosition.z());
			Towny.getPlugin().getScheduler().runAsyncLater(() -> {
				try {
					// This can potentially throw an exception if we're running this async and a player disconnects while it's sending particles.
					world.spawnParticle(Particle.CRIT_MAGIC, point, 1, 0.0, 0.0, 0.0, 0.0);
				} catch (Exception ignored) {}
			}, (long) i * RING_DELAY_TICKS);
			i++;
		}
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

	@Desugar
	private record RingCoord(double x, double z) {
		private static RingCoord offset(double a, double b) {
			return new RingCoord(a, b);
		}
	}
}
