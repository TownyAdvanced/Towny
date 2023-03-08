package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.World;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;

public class DrawSmokeTask extends TownyTimerTask {

	LoadingCache<TownWorldPair, List<CellBorder>> cellBorderCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(30))
			.build(new CacheLoader<TownWorldPair, List<CellBorder>>() {
						@Override
						public List<CellBorder> load(TownWorldPair key) throws Exception {
							return getCellBordersForTownInWorld(key.getTown(), key.getWorld());
						}
					});

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

				List<CellBorder> cellBorders;
				try {
					cellBorders = cellBorderCache.get(TownWorldPair.of(town, player.getWorld()));
				} catch (ExecutionException e) {
					continue;
				}

				if (cellBorders.isEmpty())
					continue;

				Color color = DrawSmokeTaskFactory.getAffiliationColor(resident, cellBorders.get(0));
				cellBorders.forEach(cb -> cb.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, color)));
				continue;
			}
		}
	}

	private List<CellBorder> getCellBordersForTownInWorld(Town town, World world) {
		System.out.println("new cache loading");
		List<CellBorder> cellBorders = new ArrayList<>();
		List<WorldCoord> wcs = town.getTownBlocks().stream()
				.map(TownBlock::getWorldCoord)
				.filter(wc -> wc.getBukkitWorld().getName().equals(world.getName()))
				.collect(Collectors.toList());
		if (wcs.isEmpty())
			return cellBorders;
		 cellBorders = BorderUtil.getOuterBorder(wcs);
		 return cellBorders;
	}

	private static class TownWorldPair {
		final private Town town;
		final private World world;

		private TownWorldPair(Town town, World world) {
			this.town = town;
			this.world = world;
		}

		public World getWorld() {
			return world;
		}

		public Town getTown() {
			return town;
		}
		
		private static TownWorldPair of(Town town, World world) {
			return new TownWorldPair(town, world);
		}
	}
}
