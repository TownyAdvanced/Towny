package com.palmergames.bukkit.towny.object;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import com.github.bsideup.jabel.Desugar;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.BlockUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;

public class CellSurface {
	private final Particle.DustOptions CLAIMING_PARTICLE = new Particle.DustOptions(Color.LIME, 2);
	private final WorldCoord worldCoord;

	public CellSurface(WorldCoord worldCoord) {
		this.worldCoord = worldCoord;
	}

	public static CellSurface getCellSurface(WorldCoord worldCoord) {
		return new CellSurface(worldCoord);
	}

	public void runClaimingParticleOverSurfaceAtPlayer(Player player, Color color, int edgeHeight) {

		int startingX = getX(player.getLocation());
		int startingZ = getZ(player.getLocation());
		Map<Integer, Set<BlockPos>> toRender = mapRingsOfClaimParticles(startingX, startingZ);

		// Spread particles outwards from the player location.
		for (Entry<Integer, Set<BlockPos>> entry : toRender.entrySet()) {
			// Key stores the int which will also act as a delay added to each successive ring's rendering.
			for (BlockPos position : entry.getValue()) {
				int x = position.x;
				int z = position.z;
				Towny.getPlugin().getScheduler().runAsyncLater(()-> drawClaimingParticleOnTopOfBlock(player, x, z), entry.getKey() * 1L);
			}
		}

		// Splash the edes of the plot last with extra height.
		long finalDelay = toRender.keySet().size() + 1 * 1L;
		Towny.getPlugin().getScheduler().runAsyncLater(()-> 
			BorderUtil.getPlotBorder(worldCoord).runBorderedOnSurface(edgeHeight, edgeHeight, DrawSmokeTaskFactory.showToPlayer(player, color)), finalDelay);
		
	}

	private Map<Integer, Set<BlockPos>> mapRingsOfClaimParticles(int startingX, int startingZ) {
		Set<BlockPos> traveled = new HashSet<>();
		Map<Integer, Set<BlockPos>> toRender = new HashMap<Integer, Set<BlockPos>>();

		BlockPos pos;
		Set<BlockPos> localRing = new HashSet<>();
		int maxRadius = TownySettings.getTownBlockSize();
		for (int ringNum = 1; ringNum <= maxRadius; ringNum++) {
			for (int x = startingX + Math.negateExact(ringNum); x <= startingX + ringNum; x++) {
				for (int z = startingZ + Math.negateExact(ringNum); z <= startingZ + ringNum; z++) {

					pos = BlockPos.of(x, z);
					// We've already covered this in an earlier ring.
					if (traveled.contains(pos))
						continue;

					// Mark pos as traveled so we're not re-parsing things (also when it is outside
					// of the WorldCoord.)
					traveled.add(pos);

					// We might be outside of the WorldCoord.
					if (!locationWithinCell(x, z))
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

	private void drawClaimingParticleOnTopOfBlock(Player player, int x, int z) {
		if (!player.isOnline())
			return;
		Location loc = new Location(worldCoord.getBukkitWorld(), x, BlockUtil.getHighestNonLeafY(worldCoord.getBukkitWorld(), x, z) + 1.0, z);
		player.spawnParticle(Particle.REDSTONE, loc, 5, CLAIMING_PARTICLE);
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

	private boolean locationWithinCell(int x, int z) {
		return worldCoord.getBoundingBox().contains(x, 1, z);
	}

	@Desugar
	private record BlockPos(int x, int z) {
		public static BlockPos of(int x, int z) {
			return new BlockPos(x, z);
		}
	}
}
