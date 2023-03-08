package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import com.palmergames.util.TimeMgmt;

public class DrawSmokeTask extends TownyTimerTask {

	Map<UUID, List<CellBorder>> townCellBorderMap = new HashMap<>();
	Map<UUID, Long> townCachedTime = new HashMap<>();
	Cache<UUID, Map<UUID, List<CellBorder>>> cellBorderCache = CacheBuilder.newBuilder()
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

				// Check if this town's cellBorders are already cached and less than 30 seconds old.
				List<CellBorder> cellBorders = null;
				if (townCachedTime.containsKey(town.getUUID()) &&
					townCachedTime.get(town.getUUID()) > System.currentTimeMillis()) {
						cellBorders = townCellBorderMap.get(town.getUUID());
				}

				// Not cached or cached List was stale.
				if (cellBorders == null) {
					List<WorldCoord> wcs = town.getTownBlocks().stream()
							.map(TownBlock::getWorldCoord)
							.filter(wc -> wc.getBukkitWorld().getName().equals(player.getWorld().getName()))
							.filter(wc -> wc.getNormalizedDistanceFromLocation(player.getLocation()) <= 200)
							.collect(Collectors.toList());
					if (wcs.isEmpty())
						continue;
					cellBorders = BorderUtil.getOuterBorder(wcs);
					townCachedTime.put(town.getUUID(), (System.currentTimeMillis() + (long) TimeMgmt.ONE_SECOND_IN_MILLIS * 30));
					townCellBorderMap.put(town.getUUID(), cellBorders);
				}
				Color color = DrawSmokeTaskFactory.getAffiliationColor(resident, cellBorders.get(0));
				cellBorders.forEach(cb -> cb.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, color)));
				continue;
			}
			
		}
	}
}
