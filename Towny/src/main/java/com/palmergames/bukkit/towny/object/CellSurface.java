package com.palmergames.bukkit.towny.object;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.BukkitParticle;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import org.bukkit.util.BoundingBox;

public class CellSurface {
	private static final long PARTICLE_DELAY = 1L;
	private final Particle.DustOptions CLAIMING_PARTICLE = new Particle.DustOptions(Color.LIME, 2);
	private final WorldCoord worldCoord;

	public CellSurface(WorldCoord worldCoord) {
		this.worldCoord = worldCoord;
	}

	public static CellSurface getCellSurface(WorldCoord worldCoord) {
		return new CellSurface(worldCoord);
	}

	private record BlockPos(int x, int z) {}

	public void runClaimingParticleOverSurfaceAtPlayer(Player player) {
		final World world = worldCoord.getBukkitWorld();
		if (world == null)
			return;

		// Create a Map of rings of BlockPos' which will expand outwards from the Player
		// location if they are stood in the WorldCoord (or from the correct edge block
		// of the WorldCoord if they are stood outside of it.)
		Map<Integer, Set<BlockPos>> toRender = mapRingsOfClaimParticles(getX(player.getLocation()), getZ(player.getLocation()));

		// Parse over the Map to generate particles on each successive ring with an
		// added tick of delay (using the Map's Integer key to determine delay.)
		toRender.forEach((key, value) -> value.forEach(pos ->
                Towny.getPlugin().getScheduler().runLater(player, () -> drawClaimingParticleOnTopOfBlock(player, world, pos.x, pos.z), key * PARTICLE_DELAY)));

		// Splash the edges of the WorldCoord last with extra height to add definition to the boundaries.
		long finalDelay = toRender.keySet().size() + PARTICLE_DELAY;
		Towny.getPlugin().getScheduler().runLater(player, ()-> 
			BorderUtil.getPlotBorder(worldCoord).runBorderedOnSurface(2, 2, DrawSmokeTaskFactory.showToPlayer(player, Color.GREEN)), finalDelay);
		
	}

	private Map<Integer, Set<BlockPos>> mapRingsOfClaimParticles(int startingX, int startingZ) {
		Set<BlockPos> traveled = new HashSet<>();
		Map<Integer, Set<BlockPos>> toRender = new HashMap<>();

		BlockPos pos;
		Set<BlockPos> localRing = new HashSet<>();
		int maxRadius = TownySettings.getTownBlockSize();
		final BoundingBox worldCoordBB = worldCoord.getBoundingBox();
		for (int ringNum = 1; ringNum <= maxRadius; ringNum++) {
			for (int x = startingX + Math.negateExact(ringNum); x <= startingX + ringNum; x++) {
				for (int z = startingZ + Math.negateExact(ringNum); z <= startingZ + ringNum; z++) {

					pos = new BlockPos(x, z);
					// We've already covered this in an earlier ring.
					if (traveled.contains(pos))
						continue;

					// Mark pos as traveled so we're not re-parsing things (also when it is outside
					// of the WorldCoord.)
					traveled.add(pos);

					// We might be outside of the WorldCoord.
					if (!worldCoordBB.contains(x, 1, z))
						continue;

					localRing.add(pos);
				}
			}
			if (localRing.isEmpty())
				break;
			toRender.put(ringNum, new HashSet<>(localRing));
			localRing.clear();
		}
		return toRender;
	}

	private void drawClaimingParticleOnTopOfBlock(Player player, World world, int x, int z) {
		Location loc = getParticleLocation(world, x, z);
		Towny.getPlugin().getScheduler().runAsync(() -> player.spawnParticle(BukkitParticle.getBorderParticle(), loc, 5, CLAIMING_PARTICLE));
	}

	private Location getParticleLocation(World world, int x, int z) {
		return new Location(world, x, world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES), z).add(0.5, 0.95, 0.5); // centre and raise slightly.
	}

	private int getX(Location playerLoc) {
		if (WorldCoord.parseWorldCoord(playerLoc).equals(worldCoord))
			return playerLoc.getBlockX();

		// Player is outside of the WorldCoord, try to match up their X with an X in the WorldCoord or hit the correct corner.
		return findSuitableXorZ(playerLoc.getBlockX(), worldCoord.getBoundingBox().getMaxX(), worldCoord.getBoundingBox().getMinX());
	}

	private int getZ(Location playerLoc) {
		if (WorldCoord.parseWorldCoord(playerLoc).equals(worldCoord))
			return playerLoc.getBlockZ();

		// Player is outside of the WorldCoord, try to match up their Z with an Z in the WorldCoord or hit the correct corner.
		return findSuitableXorZ(playerLoc.getBlockZ(), worldCoord.getBoundingBox().getMaxZ(), worldCoord.getBoundingBox().getMinZ());
	}

	private int findSuitableXorZ(int player, double max, double min) {
		return (int) Math.max(Math.min(player, max), min);
	}
}
