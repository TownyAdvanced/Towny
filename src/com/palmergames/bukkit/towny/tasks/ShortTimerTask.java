package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.PlayerHealthRegainLimiterUtil;
import com.palmergames.bukkit.towny.utils.PostRespawnPeacefulnessUtil;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarTimerTaskController;

/**
 * This class represents the short timer task
 *
 * It is generally set to run about once per 20 seconds
 * This rate can be configured.
 *
 * @author Goosius
 */
public class ShortTimerTask extends TownyTimerTask {

	public ShortTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if(TownySettings.getWarCommonPostRespawnPeacefulnessEnabled()) {
			PostRespawnPeacefulnessUtil.evaluatePostRespawnPeacefulnessRemovals();
		}

		if (TownySettings.isPlayerHealthRegainLimiterEnabled()) {
			PlayerHealthRegainLimiterUtil.clearAllRecentHealthGainAmounts();
		}

		if (TownySettings.getWarSiegeEnabled()) {
			SiegeWarTimerTaskController.evaluateBannerControl();
			SiegeWarTimerTaskController.evaluateTacticalVisibility();
			SiegeWarTimerTaskController.evaluateTimedSiegeOutcomes();
		}
	}
}