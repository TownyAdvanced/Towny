package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;

public class DrawSmokeTask extends TownyTimerTask {
	
	// --- CONFIG PLACEHOLDERS - xghostxcodex additions ---
    // leaving these here to make wiring up the config options easier later...
    // quick breakdown of how I set up the logic:
    // - USE_3D_BOX = false -> completely bypasses my addition regardless of if colors are true or not, your original code kicks in
    // - USE_3D_BOX = true, but others false -> my 3D moving(y-axis) box, but with your original green/grey colors
    // - All true = the full xghostxcodex way (3dbox, rainbow town, red wilderness)
    // Feel free to delete these notes as I just wanted to explain it all
    private final boolean USE_3D_BOX = true;
    private final boolean USE_RAINBOW = true;
    private final boolean SHOW_WILDERNESS_RED = true;

	private static final Cache<String, List<CellBorder>> cellBorderCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

	public DrawSmokeTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Resident resident = TownyAPI.getInstance().getResident(player);
			if (resident == null)
				continue;
			// xghostxcodex addition
            if (USE_3D_BOX) {
                double y1 = player.getLocation().getY() + 0.1;
                double y2 = y1 + 4.0;

                if (resident.hasMode("constantplotborder")) {
                    WorldCoord wc = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player.getLocation()));
                    boolean isWilderness = (wc.getTownOrNull() == null);
                    drawBox(player, wc.getX(), wc.getZ(), y1, y2, isWilderness, USE_RAINBOW);
                    continue;
                }

                if (resident.hasMode("townborder")) {
                    Town town = TownyAPI.getInstance().getTown(player.getLocation());
                    if (town == null) 
						continue;

                    for (TownBlock tb : town.getTownBlocks()) {
                        if (!tb.getWorld().getName().equals(player.getWorld().getName())) 
							continue;
                        int tbX = tb.getX();
                        int tbZ = tb.getZ();

                        double minX = tbX * 16.0;
                        double minZ = tbZ * 16.0;
                        double maxX = minX + 16.0;
                        double maxZ = minZ + 16.0;

                        if (!hasSameTown(town, tbX, tbZ - 1)) drawFace(player, minX, minZ, maxX, minZ, y1, y2, false, USE_RAINBOW);
                        if (!hasSameTown(town, tbX, tbZ + 1)) drawFace(player, minX, maxZ, maxX, maxZ, y1, y2, false, USE_RAINBOW);
                        if (!hasSameTown(town, tbX - 1, tbZ)) drawFace(player, minX, minZ, minX, maxZ, y1, y2, false, USE_RAINBOW);
                        if (!hasSameTown(town, tbX + 1, tbZ)) drawFace(player, maxX, minZ, maxX, maxZ, y1, y2, false, USE_RAINBOW);
                    }
                }
                continue;
            }
            // original
			if (resident.hasMode("constantplotborder")) {
				WorldCoord wc = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player.getLocation()));
				CellBorder cellBorder = BorderUtil.getPlotBorder(wc);

				plugin.getScheduler().run(cellBorder.getLowerMostCornerLocation(), () -> cellBorder.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, getColor(resident, wc))));
				continue;
			}

			if (resident.hasMode("townborder")) {
				Town town = TownyAPI.getInstance().getTown(player.getLocation());
				if (town == null)
					continue;

				List<CellBorder> cellBorders = getCellBorders(town, player.getWorld());
				if (cellBorders == null)
					continue;

				Color color = getColor(resident, cellBorders.get(0));
				cellBorders.forEach(cb -> plugin.getScheduler().run(cb.getLowerMostCornerLocation(), () -> cb.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, color))));
			}
		}
	}

	private Color getColor(Resident resident, WorldCoord wc) {
		return DrawSmokeTaskFactory.getAffiliationColor(resident, wc);
	}

	@Nullable
	private List<CellBorder> getCellBorders(final Town town, final World world) {
		try {
			return cellBorderCache.get(town.getName() + ":" + world.getName(), () -> getCellBordersForTownInWorld(town, world));
		} catch (ExecutionException ignored) {
			return null;
		}
	}

	@Nullable
	private static List<CellBorder> getCellBordersForTownInWorld(final Town town, final World world) {
		List<WorldCoord> wcs = town.getTownBlocks().stream()
				.map(TownBlock::getWorldCoord)
				.filter(wc -> world.equals(wc.getBukkitWorld()))
				.collect(Collectors.toList());
		
		if (wcs.isEmpty()) // Probably shouldn't ever happen.
			return null;
		
		return BorderUtil.getOuterBorder(wcs);
	}
	// --- Draw math for xghostxcodex addition---
	private boolean hasSameTown(Town town, int x, int z) {
		for (TownBlock tb : town.getTownBlocks()) {
			if (tb.getX() == x && tb.getZ() == z) 
				return true;
		}
		return false;
	}

	private void drawBox(Player player, int tbX, int tbZ, double y1, double y2, boolean isWilderness, boolean useRainbow) {
		double minX = tbX * 16.0;
		double minZ = tbZ * 16.0;
		double maxX = minX + 16.0;
		double maxZ = minZ + 16.0;

		drawFace(player, minX, minZ, maxX, minZ, y1, y2, isWilderness, useRainbow);
		drawFace(player, minX, maxZ, maxX, maxZ, y1, y2, isWilderness, useRainbow);
		drawFace(player, minX, minZ, minX, maxZ, y1, y2, isWilderness, useRainbow);
		drawFace(player, maxX, minZ, maxX, maxZ, y1, y2, isWilderness, useRainbow);
	}

	private void drawFace(Player player, double x1, double z1, double x2, double z2, double y1, double y2, boolean isWilderness, boolean useRainbow) {
		drawLine(player, x1, y1, z1, x2, y1, z2, isWilderness, useRainbow);
		drawLine(player, x1, y2, z1, x2, y2, z2, isWilderness, useRainbow);
		drawLine(player, x1, y1, z1, x1, y2, z1, isWilderness, useRainbow);
		drawLine(player, x2, y1, z2, x2, y2, z2, isWilderness, useRainbow);
	}

	private void drawLine(Player player, double x1, double y1, double z1, double x2, double y2, double z2, boolean isWilderness, boolean useRainbow) {
		double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
		int particles = (int) (distance * 2);
		if (particles == 0) 
			return;

		long time = System.currentTimeMillis();

		for (int i = 0; i <= particles; i++) {
			double lerpX = x1 + (x2 - x1) * ((double) i / particles);
			double lerpY = y1 + (y2 - y1) * ((double) i / particles);
			double lerpZ = z1 + (z2 - z1) * ((double) i / particles);

			Color color;
			if (isWilderness) {
                    color = SHOW_WILDERNESS_RED ? Color.RED : Color.GRAY;
			} else if (useRainbow) {
				float hue = (float) ((Math.abs(lerpX * 15 + lerpZ * 15 + (time / 15)) % 360) / 360.0);
				java.awt.Color awt = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
				color = Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
			} else {
				color = Color.GREEN;
			}

			Particle.DustOptions dust = new Particle.DustOptions(color, 1.5F);
			Location particleLoc = new Location(player.getWorld(), lerpX, lerpY, lerpZ);
			player.spawnParticle(com.palmergames.bukkit.util.BukkitParticle.getBorderParticle(), particleLoc, 1, 0, 0, 0, 0, dust);
		}
	}	// thank you for trying this out, hope you like it! xghostxcodex
}