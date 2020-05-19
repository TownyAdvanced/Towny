package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.AttackerWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.DefenderWin;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBannerControlUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDynmapUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.util.TimeMgmt;

/**
 * This class intercepts siege related instructions coming from timer tasks.
 * and takes action as appropriate
 *
 * @author Goosius
 */
public class SiegeWarTimerTaskController {

	/**
	 * Evaluate timed siege outcomes
	 * e.g. who wins if siege victory timer runs out ?
	 */
	public static void evaluateTimedSiegeOutcomes() {
		for (Siege siege : TownyUniverse.getInstance().getAllSieges()) {
			evaluateTimedSiegeOutcome(siege);
		}
	}

	/**
	 * Evaluate the timed outcome of 1 siege
	 *
	 * @param siege
	 */
	private static void evaluateTimedSiegeOutcome(Siege siege) {
		TownyUniverse universe = TownyUniverse.getInstance();

		switch(siege.getStatus()) {
			case IN_PROGRESS:
				//If scheduled end time has arrived, choose winner
				if (System.currentTimeMillis() > siege.getScheduledEndTime()) {
					TownyObject siegeWinner = SiegeWarPointsUtil.calculateSiegeWinner(siege);
					if (siegeWinner instanceof Town) {
						DefenderWin.defenderWin(siege, (Town) siegeWinner);
					} else {
						AttackerWin.attackerWin(siege, (Nation) siegeWinner);
					}

					//Save changes to db
					universe.getDataSource().saveTown(siege.getDefendingTown());
				}
				break;

			case PENDING_DEFENDER_SURRENDER:
				if(siege.getDurationMillis() > (TownySettings.getWarSiegeMinSiegeDurationBeforeSurrenderHours() * TimeMgmt.ONE_HOUR_IN_MILLIS )) {
					SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.DEFENDER_SURRENDER);
				}
				break;

			case PENDING_ATTACKER_ABANDON:
				if(siege.getDurationMillis() > (TownySettings.getWarSiegeMinSiegeDurationBeforeAbandonHours() * TimeMgmt.ONE_HOUR_IN_MILLIS )) {
					SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.ATTACKER_ABANDON);
				}
				break;

			default:
				//Siege is inactive
				//Wait for siege immunity timer to end then delete siege
				if (System.currentTimeMillis() > siege.getDefendingTown().getSiegeImmunityEndTime()) {
					universe.getDataSource().removeSiege(siege);
				}
		}
	}

	/**
	 * Evaluate the visibility of players on the dynmap
	 * when using the 'tactical visibility' feature
	 */
	public static void evaluateTacticalVisibility() {
		if (TownySettings.getWarSiegeTacticalVisibilityEnabled()) {
			SiegeWarDynmapUtil.evaluatePlayerTacticalInvisibility();
		}
	}

	/**
	 * Evaluate banner control for all sieges
	 */
	public static void evaluateBannerControl() {
		for (Siege siege : TownyUniverse.getInstance().getDataSource().getSieges()) {
			SiegeWarBannerControlUtil.evaluateBannerControl(siege);
		}
	}

	public static void updatePopulationBasedSiegePointModifiers() {
		if(TownySettings.getWarSiegePopulationBasedPointBoostsEnabled()) {
			SiegeWarPointsUtil.updatePopulationBasedSiegePointModifiers();
		}
	}
}
