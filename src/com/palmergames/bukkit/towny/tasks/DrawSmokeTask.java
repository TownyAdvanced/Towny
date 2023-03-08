package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

	Cache<String, List<CellBorder>> cellBorderCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(30))
			.build();

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
				
				cellBorder.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, DrawSmokeTaskFactory.getAffiliationColor(resident, wc)));
				continue;
			}

			if (resident.hasMode("townborder")) {
				Town town = TownyAPI.getInstance().getTown(player.getLocation());
				if (town == null)
					continue;

				List<CellBorder> cellBorders = getCellBorders(new TownWorldPair(town, player.getWorld()));
				if (cellBorders == null)
					continue;

				Color color = DrawSmokeTaskFactory.getAffiliationColor(resident, cellBorders.get(0));
				cellBorders.forEach(cb -> cb.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, color)));
				continue;
			}
		}
	}

	@Nullable
	private List<CellBorder> getCellBorders(TownWorldPair pair) {
		List<CellBorder> cellBorders = null;
		try {
			// Keyed to a String so that it is always a single memory address.
			cellBorders = cellBorderCache.get(pair.toString(), new Callable<List<CellBorder>>() {
				@Override
				public List<CellBorder> call() {
					return pair.getCellBordersForTownInWorld();
				}
			});
		} catch (ExecutionException ignored) {}
		return cellBorders;
	}

	private class TownWorldPair {
		final private Town town;
		final private World world;

		private TownWorldPair(Town town, World world) {
			this.town = town;
			this.world = world;
		}

		public String toString() {
			return town.getName() + ":" + world.getName();
		}

		private List<CellBorder> getCellBordersForTownInWorld() {
			List<CellBorder> cellBorders = null;
			List<WorldCoord> wcs = town.getTownBlocks().stream()
					.map(TownBlock::getWorldCoord)
					.filter(wc -> wc.getBukkitWorld().getName().equals(world.getName()))
					.collect(Collectors.toList());
			if (wcs.isEmpty()) // Probably shouldn't ever happen.
				return cellBorders;
			cellBorders = BorderUtil.getOuterBorder(wcs);
			return cellBorders;
		}
	}
}
