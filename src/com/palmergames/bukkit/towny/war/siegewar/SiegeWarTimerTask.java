package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.AttackerWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.DefenderWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemovePostSpawnDamageImmunity;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemoveRuinedTowns;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDynmapUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBannerControlUtil;

import static com.palmergames.util.TimeMgmt.ONE_MINUTE_IN_MILLIS;

/**
 * This class represents the siegewar timer task
 * 
 * The task is recommended to run about once every 20 seconds. 
 * This rate can be configured.
 *
 * @author Goosius
 */
public class SiegeWarTimerTask extends TownyTimerTask {
	private long nextRuinsRemovalsTick;

	public SiegeWarTimerTask(Towny plugin) {
		super(plugin);
		nextRuinsRemovalsTick = System.currentTimeMillis() + (long)(TownySettings.getWarSiegeRuinsRemovalsTickIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
	}

	@Override
	public void run() {
		if (TownySettings.getWarSiegeEnabled()) {

			evaluateBannerControl();

			evaluateTimedSiegeOutcomes();

			evaluateRuinsRemovals();

			evaluatePostSpawnDamageImmunityRemovals();

			evaluateTacticalVisibility();
		}
	}

	/**
	 * Evaluate the visibility of players on the dynmap
	 * when using the 'tactical visibility' feature
	 */
	private void evaluateTacticalVisibility() {
		if(TownySettings.getWarSiegeTacticalVisibilityEnabled()) {
			SiegeWarDynmapUtil.evaluateTacticalVisibilityOfPlayers();
		}
	}

	/**
	 * Evaluate banner control for all siege zones
	 */
	private void evaluateBannerControl() {
		for(SiegeZone siegeZone: TownyUniverse.getInstance().getDataSource().getSiegeZones()) {
			SiegeWarBannerControlUtil.evaluateBannerControl(siegeZone);
		}
	}

	/**
	 * Evaluate timed siege outcomes
	 * e.g. who wins if siege victory timer runs out ?
	 */
	private void evaluateTimedSiegeOutcomes() {
		for(Siege siege: TownyUniverse.getInstance().getAllSieges()) {
			evaluateTimedSiegeOutcome(siege);
		}
	}

	/**
	 * Evaluate ruins removals
	 */
	public void evaluateRuinsRemovals() {
		if(TownySettings.getWarSiegeDelayFullTownRemoval() && System.currentTimeMillis() > nextRuinsRemovalsTick) {
			TownyMessaging.sendDebugMsg("Checking ruined towns now for deletion.");
			RemoveRuinedTowns.deleteRuinedTowns();
			nextRuinsRemovalsTick = System.currentTimeMillis() + (long)(TownySettings.getWarSiegeRuinsRemovalsTickIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
		}
	}

	/**
	 * Evaluate post spawn damage immunity removals
	 */
	private void evaluatePostSpawnDamageImmunityRemovals() {
		if(TownySettings.getWarSiegePostSpawnDamageImmunityEnabled()) {
			RemovePostSpawnDamageImmunity.removePostSpawnDamageImmunity();
		}
	}

	/**
	 * Evaluate the timed outcome of 1 siege
	 * 
	 * @param siege
	 */
	private static void evaluateTimedSiegeOutcome(Siege siege) {
		TownyUniverse universe = TownyUniverse.getInstance();
		
		//Process active siege
		if (siege.getStatus() == SiegeStatus.IN_PROGRESS) {

			//If scheduled end time has arrived, choose winner
			if (System.currentTimeMillis() > siege.getScheduledEndTime()) {
				TownyObject siegeWinner = SiegeWarPointsUtil.calculateSiegeWinner(siege);
				if (siegeWinner instanceof Town) {
					DefenderWin.defenderWin(siege, (Town) siegeWinner);
				} else {
					AttackerWin.attackerWin(siege, (Nation) siegeWinner);
				}

				//Save changes to db
				com.palmergames.bukkit.towny.TownyUniverse townyUniverse = com.palmergames.bukkit.towny.TownyUniverse.getInstance();
				townyUniverse.getDataSource().saveTown(siege.getDefendingTown());
			}

		} else {

			//Siege is finished.
			//Wait for siege immunity timer to end then delete siege
			if (System.currentTimeMillis() > siege.getDefendingTown().getSiegeImmunityEndTime()) {
				universe.getDataSource().removeSiege(siege);
			}
		}
	}
}