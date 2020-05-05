package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.PostRespawnPeacefulnessUtil;
import com.palmergames.bukkit.towny.utils.TownPeacefulnessUtil;
import com.palmergames.bukkit.towny.war.common.ruins.RuinsUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarTimerTaskController;

/**
 * This class represents the hourly timer task
 * It is generally set to run once per hour
 * This rate can be configured.
 *
 * @author Goosius
 */
public class HourlyTimerTask extends TownyTimerTask {

	public HourlyTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownySettings.getWarCommonTownRuinsEnabled()) {
			RuinsUtil.evaluateRuinedTownRemovals();
		}

		if(TownySettings.getWarCommonPeacefulTownsEnabled()) {
			TownPeacefulnessUtil.updatePostTownLeavePeacefulnessCounters();
		}

		if(TownySettings.getWarSiegeEnabled()) {
			SiegeWarTimerTaskController.updatePopulationBasedSiegePointModifiers();
		}
	}
}