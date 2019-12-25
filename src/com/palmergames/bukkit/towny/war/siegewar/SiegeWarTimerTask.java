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
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.AttackerWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.DefenderWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemoveRuinedTowns;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
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

	private static long nextTimeToRemoveRuinedTowns;

	static
	{
		nextTimeToRemoveRuinedTowns =
				System.currentTimeMillis() +
					(long)(TownySettings.getWarSiegeTownRuinsRemovalTimerIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
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
				nextTimeToRemoveRuinedTowns =
						System.currentTimeMillis() +
							(long)(TownySettings.getWarSiegeTownRuinsRemovalTimerIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
				RemoveRuinedTowns.removeRuinedTowns();
			}
		}
	}

	/**
	 * Evaluate all siege zones
	 */
	private void evaluateSiegeZones() {
		TownyUniverse universe = TownyUniverse.getInstance();
		for(SiegeZone siegeZone: universe.getDataSource().getSiegeZones()) {
			evaluateSiegeZone(siegeZone);
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
		//Evaluate the siege zone only if the siege is 'in progress'.
		if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS) 
			return;
		
		TownyUniverse universe = TownyUniverse.getInstance();
		boolean siegeZoneChanged = false;
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

					if (residentTown == siegeZone.getDefendingTown()) {

						//Resident of defending town
						siegeZoneChanged =
								siegeZoneChanged ||
										evaluateSiegeZoneOccupant(
												player,
												siegeZone,
												siegeZone.getDefenderPlayerScoreTimeMap(),
												-TownySettings.getWarSiegePointsForDefenderOccupation());

					} else if (residentTown.hasNation()) {

						if (siegeZone.getDefendingTown().hasNation()
								&& siegeZone.getDefendingTown().getNation()
								== residentTown.getNation()) {

							//Nation member of defending town
							siegeZoneChanged =
									siegeZoneChanged ||
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getDefenderPlayerScoreTimeMap(),
													-TownySettings.getWarSiegePointsForDefenderOccupation());

						} else if (siegeZone.getAttackingNation() 
							== residentTown.getNation()) {

							//Nation member of attacking nation
							siegeZoneChanged =
									siegeZoneChanged ||
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getAttackerPlayerScoreTimeMap(),
													TownySettings.getWarSiegePointsForAttackerOccupation());
						}
					}
				}
			} catch (NotRegisteredException e) {
			}
		}
		
		//Remove garbage from player time maps
		Town defendingTown =siegeZone.getDefendingTown();
		Nation defendingNation= null;
		if(defendingTown.hasNation()) {
			try {
				defendingNation = defendingTown.getNation();
			} catch (NotRegisteredException e) {
			}
		}

		siegeZoneChanged =
				siegeZoneChanged ||
						removeGarbageFromPlayerTimeMap(siegeZone.getDefenderPlayerScoreTimeMap(),
								defendingTown,
								defendingNation);

		siegeZoneChanged =
				siegeZoneChanged ||
						removeGarbageFromPlayerTimeMap(siegeZone.getAttackerPlayerScoreTimeMap(),
								null,
								siegeZone.getAttackingNation());

		siegeZoneChanged =
				siegeZoneChanged ||
						removeGarbageFromPlayerTimeMap(siegeZone.getPlayerAfkTimeMap(),
								null,
								null);
		
		//Save siege zone to db if it was changed
		if(siegeZoneChanged) {
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
				return true;
			}

			//Player must have been in zone long enough
			if (System.currentTimeMillis() < playerScoreTimeMap.get(player)) {
				return false;
			}
			
			//Player must not have been in zone too long (anti-afk feature)
			if (System.currentTimeMillis() > siegeZone.getPlayerAfkTimeMap().get(player)) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_cannot_occupy_zone_for_too_long"));
				playerScoreTimeMap.put(player,
					System.currentTimeMillis()
						+ (long)(TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds() * ONE_SECOND_IN_MILLIS));
				return false;
			}

			//Player must still be in the open
			if(SiegeWarBlockUtil.doesPlayerHaveANonAirBlockAboveThem(player)) {
				playerScoreTimeMap.remove(player);
				siegeZone.getPlayerAfkTimeMap().remove(player);
				return true;
			}
			
			//Player has been in zone long enough. Points awarded
			siegeZone.adjustSiegePoints(siegePointsForZoneOccupation);
			playerScoreTimeMap.put(player,
					System.currentTimeMillis()
							+ (long)(TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds() * ONE_SECOND_IN_MILLIS));
			return true;

		} else {

			//Player must be in zone
			if (!SiegeWarPointsUtil.isPlayerInSiegePointZone(player, siegeZone)) {
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

			playerScoreTimeMap.put(player,
					System.currentTimeMillis()
							+ (long)(TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds() * ONE_SECOND_IN_MILLIS));
			
			siegeZone.getPlayerAfkTimeMap().put(player,
					System.currentTimeMillis()
							+ (long)(TownySettings.getWarSiegeZoneMaximumScoringDurationMinutes() * ONE_MINUTE_IN_MILLIS));
			
			return true; //Player added to zone
		}
	}

	/**
	 * This method removes 'garbage' from a player time map.
	 * 
	 * For example, if a player has left the area, or logged off,
	 * then although they will no longer affect the siege, the entry remains in the time map as 'garbage'
	 * 
	 * @param map the map recording player to time
	 * @param townFilter the town which players must be in to avoid being removed from the map
	 * @param nationFilter the nation which players must be in to avoid being removed from the map
	 * @return true if the siegezone has been updated
	 */
	private static boolean removeGarbageFromPlayerTimeMap(Map<Player, Long> map,
														  Town townFilter,
														  Nation nationFilter) {

		boolean siegeZoneChanged = false;

		try {
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Player,Long> pair = (Map.Entry)it.next();

				//Remove player if offline
				if(!pair.getKey().isOnline()) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				Resident resident = TownyUniverse.getInstance().getDataSource().getResident(pair.getKey().getName());

				//Remove player if they have no town
				if(!resident.hasTown()) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				//If only town filter is used, remove player if they are not in the right town
				if(townFilter != null && nationFilter == null && resident.getTown() != townFilter) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				//If nation filter is used, remove player if they are not in the right nation
				if(nationFilter != null && (!resident.hasNation() || resident.getTown().getNation() != nationFilter)) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}
			}

		} catch (Exception e) {
		}

		return siegeZoneChanged;
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