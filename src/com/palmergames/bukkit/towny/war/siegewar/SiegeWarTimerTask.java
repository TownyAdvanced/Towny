package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.AttackerWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.DefenderWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemoveRuinedTowns;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.palmergames.util.TimeMgmt.ONE_MINUTE_IN_MILLIS;
import static com.palmergames.util.TimeMgmt.ONE_SECOND_IN_MILLIS;

/**
 * This class represents the siegewar timer task
 * 
 * The task is recommended to run about once every 10 seconds. 
 * This rate can be configured.
 * 
 * The task does 3 things when it runs:
 * 1. Evaluate each siege zone, and adjust siege points depending on which players are in the zones.
 * 2. Evaluate each siege, to determine if the main phase is completed, or the aftermath is completed.
 * 3. Completely delete towns which have already been ruined for a certain amount of time.
 * 
 * @author Goosius
 */
public class SiegeWarTimerTask extends TownyTimerTask {

	public SiegeWarTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownySettings.getWarSiegeEnabled()) {
			
			evaluateSiegeZones();

			evaluateSieges();
		}
	}

	/**
	 * Evaluate all siege zones
	 */
	private void evaluateSiegeZones() {
		TownyUniverse universe = TownyUniverse.getInstance();
		for(SiegeZone siegeZone: universe.getDataSource().getSiegeZones()) {
			try {
				evaluateSiegeZone(siegeZone);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Evaluate all sieges
	 */
	private void evaluateSieges() {
		for(Siege siege: getAllSieges()) {
			evaluateSiege(siege);
		}
	}

	/**
	 * Evaluate just 1 siege zone
	 */
	private static void evaluateSiegeZone(SiegeZone siegeZone) {
		boolean attackPointsAwarded;
		boolean defencePointsAwarded;

		int attackPointInstancesAwarded = 0;
		int defencePointInstancesAwarded = 0;

		//Evaluate the siege zone only if the siege is 'in progress'.
		if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS) 
			return;
		
		TownyUniverse universe = TownyUniverse.getInstance();
		Resident resident;

		//Cycle all online players
		for (Player player : BukkitTools.getOnlinePlayers()) {

			try {
				resident = universe.getDataSource().getResident(player.getName());

				if (resident.hasTown()) {
					Town residentTown= resident.getTown();

					//Residents of occupied towns cannot affect siege points
					if(resident.getTown().isOccupied())
						continue;

					if (defencePointInstancesAwarded <= TownySettings.getWarSiegeMaxPlayersPerSideForTimedPoints()
						&& residentTown == siegeZone.getDefendingTown()
						&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {

						//Resident of defending town
						defencePointsAwarded = evaluateSiegeZoneOccupant(
												player,
												siegeZone,
												siegeZone.getDefenderPlayerScoreTimeMap(),
												-TownySettings.getWarSiegePointsForDefenderOccupation());

						if(defencePointsAwarded) {
							defencePointInstancesAwarded++;
						}

					} else if (residentTown.hasNation()
						&& universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

						if (defencePointInstancesAwarded <= TownySettings.getWarSiegeMaxPlayersPerSideForTimedPoints()
							    && siegeZone.getDefendingTown().hasNation()
								&& siegeZone.getDefendingTown().getNation() == residentTown.getNation()) {

							//Nation member of defending town
							defencePointsAwarded =
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getDefenderPlayerScoreTimeMap(),
													-TownySettings.getWarSiegePointsForDefenderOccupation());

							if(defencePointsAwarded) {
								defencePointInstancesAwarded++;
							}

						} else if (attackPointInstancesAwarded <= TownySettings.getWarSiegeMaxPlayersPerSideForTimedPoints()
							&& siegeZone.getAttackingNation() 
							== residentTown.getNation()) {

							//Nation member of attacking nation
							attackPointsAwarded = evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getAttackerPlayerScoreTimeMap(),
													TownySettings.getWarSiegePointsForAttackerOccupation());

							if(attackPointsAwarded) {
								attackPointInstancesAwarded++;
								//Pillage
								if(TownySettings.getWarSiegePillagingEnabled() && TownySettings.isUsingEconomy() && !siegeZone.getDefendingTown().isNeutral()) {
									SiegeWarMoneyUtil.pillageTown(player, siegeZone.getAttackingNation(), siegeZone.getDefendingTown());
								}
							}

						} else if (defencePointInstancesAwarded <= TownySettings.getWarSiegeMaxPlayersPerSideForTimedPoints()
							&& siegeZone.getDefendingTown().hasNation()
							&& siegeZone.getDefendingTown().getNation().hasMutualAlly(residentTown.getNation())) {

							//Nation member of ally of defending nation
							defencePointsAwarded =
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getDefenderPlayerScoreTimeMap(),
													-TownySettings.getWarSiegePointsForDefenderOccupation());

							if(defencePointsAwarded) {
								defencePointInstancesAwarded++;
							}

						} else if (attackPointInstancesAwarded <= TownySettings.getWarSiegeMaxPlayersPerSideForTimedPoints()
							&& siegeZone.getAttackingNation().hasMutualAlly(residentTown.getNation())) {

							//Nation member of ally of attacking nation
							attackPointsAwarded =
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getAttackerPlayerScoreTimeMap(),
													TownySettings.getWarSiegePointsForAttackerOccupation());

							if(attackPointsAwarded) {
								attackPointInstancesAwarded++;
								//Pillage
								if(TownySettings.getWarSiegePillagingEnabled() && TownySettings.isUsingEconomy() && !siegeZone.getDefendingTown().isNeutral()) {
									SiegeWarMoneyUtil.pillageTown(player, siegeZone.getAttackingNation(), siegeZone.getDefendingTown());
								}
							}
						}
					}
				}
			} catch (NotRegisteredException e) {
			}
		}
		
		//Save siege zone to db if it was changed
		if(attackPointInstancesAwarded > 0 || defencePointInstancesAwarded > 0) {
			universe.getDataSource().saveSiegeZone(siegeZone);
		}
	}

	/**
	 * Evaluate just 1 siege
	 * 
	 * @param siege
	 */
	private static void evaluateSiege(Siege siege) {
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
	
	/**
	 * Evaluate 1 siege zone player occupant.
	 * Adjust siege points depending on how long the player has remained-in/occupied the scoring zone
	 * The occupation time requirement is configurable
	 * The siege points adjustment is configurable
	 * 
	 * @param player the player in the siegezone
	 * @param siegeZone the siegezone
	 * @param playerScoreTimeMap the map recording player arrival times 
	 * @param siegePointsForZoneOccupation the int siege points adjustment which will occur if occupation is verified
	 * @return true if the siege zone has been updated
	 */
	private static boolean evaluateSiegeZoneOccupant(Player player,
													 SiegeZone siegeZone,
													 Map<Player, Long> playerScoreTimeMap,
													 int siegePointsForZoneOccupation) {
		
		//Is the player already registered as being in the siege zone ?
		if (playerScoreTimeMap.containsKey(player)) {
			
			//Player must still be in zone
			if (!SiegeWarPointsUtil.isPlayerInSiegePointZone(player, siegeZone)) {
				playerScoreTimeMap.remove(player);
				siegeZone.getPlayerAfkTimeMap().remove(player);
				return false;
			}

			//Player must be alive
			if(player.isDead()) {
				playerScoreTimeMap.remove(player);
				siegeZone.getPlayerAfkTimeMap().remove(player);
				return false;
			}

			//Player must not be flying or invisible
			if(player.isFlying() || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null) {
				playerScoreTimeMap.remove(player);
				siegeZone.getPlayerAfkTimeMap().remove(player);
				return false;
			}

			//Player must still be in the open
			if(SiegeWarBlockUtil.doesPlayerHaveANonAirBlockAboveThem(player)) {
				playerScoreTimeMap.remove(player);
				siegeZone.getPlayerAfkTimeMap().remove(player);
				return false;
			}

			//Player must not have been in zone too long (anti-afk feature)
			if (System.currentTimeMillis() > siegeZone.getPlayerAfkTimeMap().get(player)) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_cannot_occupy_zone_for_too_long"));
				playerScoreTimeMap.remove(player);
				return false;
			}

			//Points awarded
			siegeZone.adjustSiegePoints(siegePointsForZoneOccupation);

			return true;

		} else {

			//Player must be in zone
			if (!SiegeWarPointsUtil.isPlayerInSiegePointZone(player, siegeZone)) {
				return false;
			}

			//Player must be alive
			if(player.isDead()) {
				return false;
			}

			//Player must not be flying or invisible
			if(player.isFlying() || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null) {
				return false;
			}

			//Player must be in the open
			if(SiegeWarBlockUtil.doesPlayerHaveANonAirBlockAboveThem(player)) {
				return false;
			}

			playerScoreTimeMap.put(player, 0L);

			siegeZone.getPlayerAfkTimeMap().put(player,
					System.currentTimeMillis()
							+ (long)(TownySettings.getWarSiegeZoneMaximumScoringDurationMinutes() * ONE_MINUTE_IN_MILLIS));

			return false; //Player added to zone
		}
	}
	
	/**
	 * Get all the sieges in the universe
	 * 
	 * @return list of all the sieges in the universe
	 */
	private static List<Siege> getAllSieges() {
		List<Siege> result = new ArrayList<>();
		for(Town town: TownyUniverse.getInstance().getDataSource().getTowns()) {
			if(town.hasSiege()) {
				result.add(town.getSiege());
			}
		}
		return result;
	}

}