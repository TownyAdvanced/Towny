package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.util.BukkitParticle;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Location;
import org.bukkit.Particle;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.entity.Player;

public class TeleportWarmupParticle {

	private static final List<RingCoord> RING_PATTERN = createRingOffsets();
	public static final int RING_POINT_COUNT = 12;
	public static final int RING_DELAY_TICKS = 2;

	public static void drawParticles(final Player player, final double yOffset) {
		Particle spawnParticle = BukkitParticle.getSpawnPointParticle();
		int i = 0;
		for (RingCoord ringPosition : RING_PATTERN) {
			Towny.getPlugin().getScheduler().runLater(player, () -> {
				final Location point = player.getLocation().add(ringPosition.x(), yOffset, ringPosition.z());

				player.spawnParticle(spawnParticle, point, 1, 0.0, 0.0, 0.0, 0.0);

				// Don't show particles for other players if the player is invisible
				if (player.isInvisible())
					return;

				if (MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.MINECRAFT_1_20_2)) {
					for (final Player trackingPlayer : player.getTrackedBy()) {
						trackingPlayer.spawnParticle(spawnParticle, point, 1, 0.0, 0.0, 0.0, 0.0);
					}
				} else if (!BukkitTools.hasVanishedMeta(player)) {
					player.getWorld().spawnParticle(spawnParticle, point, 1, 0.0, 0.0, 0.0, 0.0);
				}
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

	private record RingCoord(double x, double z) {
		private static RingCoord offset(double a, double b) {
			return new RingCoord(a, b);
		}
	}
}
