package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
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

	private static final Cache<String, List<CellBorder>> cellBorderCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

	public DrawSmokeTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Resident resident = TownyAPI.getInstance().getResident(player);
			if (resident == null)
				continue;

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
}
