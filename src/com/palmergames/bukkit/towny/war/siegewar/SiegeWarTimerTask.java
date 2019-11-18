package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;

import java.util.ArrayList;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_HOUR_IN_MILLIS;

public class SiegeWarTimerTask extends TownyTimerTask {

	private static long nextTimeToRemoveRuinedTowns;

	static
	{
		nextTimeToRemoveRuinedTowns = System.currentTimeMillis() + ONE_HOUR_IN_MILLIS;
	}

	public SiegeWarTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownySettings.getWarSiegeEnabled()) {

			evaluateSiegeZones();

			evaluateSieges();

			if (System.currentTimeMillis() > nextTimeToRemoveRuinedTowns) {
				nextTimeToRemoveRuinedTowns = System.currentTimeMillis() + ONE_HOUR_IN_MILLIS;
				removeRuinedTowns();
			}
		}
	}

	private void removeRuinedTowns() {
		for (Town town : new ArrayList<>(TownyUniverse.getDataSource().getTowns())) {
			if(town.isRuined() && System.currentTimeMillis() > town.getRecentlyRuinedEndTime()) {
				TownyUniverse.getDataSource().removeRuinedTown(town);
			}
		}
	}

	//Cycle through all siege zones
	private void evaluateSiegeZones() {
		for(SiegeZone siegeZone: TownyUniverse.getDataSource().getSiegeZones()) {
				SiegeWarUtil.evaluateSiegeZone(siegeZone);
		}
	}

	//Cycle through all sieges
	private void evaluateSieges() {
		for(Siege siege: SiegeWarUtil.getAllSieges()) {
			SiegeWarUtil.evaluateSiege(siege);
		}
	}
}