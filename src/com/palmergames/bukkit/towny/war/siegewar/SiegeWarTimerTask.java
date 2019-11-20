package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.AttackerWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.DefenderWin;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemoveRuinedTowns;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Map;

import static com.palmergames.util.TimeMgmt.ONE_MINUTE_IN_MILLIS;

/**
 * @author Goosius
 */
public class SiegeWarTimerTask extends TownyTimerTask {

	private static long nextTimeToRemoveRuinedTowns;

	static
	{
		nextTimeToRemoveRuinedTowns =
				System.currentTimeMillis() +
				(TownySettings.getWarSiegeTownRuinsRemovalTimerIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
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
								(TownySettings.getWarSiegeTownRuinsRemovalTimerIntervalMinutes() * ONE_MINUTE_IN_MILLIS);
				RemoveRuinedTowns.removeRuinedTowns();
			}
		}
	}

	//Cycle through all siege zones
	private void evaluateSiegeZones() {
		for(com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone siegeZone: TownyUniverse.getDataSource().getSiegeZones()) {
				evaluateSiegeZone(siegeZone);
		}
	}

	//Cycle through all sieges
	private void evaluateSieges() {
		for(com.palmergames.bukkit.towny.war.siegewar.locations.Siege siege: SiegeWarUtil.getAllSieges()) {
			evaluateSiege(siege);
		}
	}


	private static void evaluateSiegeZone(com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone siegeZone) {
		if(siegeZone.isActive()) {
			boolean siegeZoneChanged = false;
			Resident resident;

			//Cycle all online players
			for (Player player : BukkitTools.getOnlinePlayers()) {

				try {
					resident = TownyUniverse.getDataSource().getResident(player.getName());

					if (resident.hasTown()) {
						//TODO - DEHARDCODE THE POINT VALUES

						if (resident.getTown() == siegeZone.getDefendingTown()) {

							//Resident of defending town
							siegeZoneChanged =
									siegeZoneChanged ||
											evaluateSiegeZoneOccupant(
													player,
													siegeZone,
													siegeZone.getDefenderPlayerScoreTimeMap(),
													TownySettings.getSiegeWarPointsPerDefendingPlayer());

						}

						if (resident.getTown().hasNation()) {

							if (siegeZone.getDefendingTown().hasNation()
									&& siegeZone.getDefendingTown().getNation()
									== resident.getTown().getNation()) {

								//Nation member of defending town
								siegeZoneChanged =
										siegeZoneChanged ||
												evaluateSiegeZoneOccupant(
														player,
														siegeZone,
														siegeZone.getDefenderPlayerScoreTimeMap(),
														TownySettings.getSiegeWarPointsPerDefendingPlayer());
							}

							if (siegeZone.getAttackingNation()
									== resident.getTown().getNation()) {

								//Nation member of attacking nation
								siegeZoneChanged =
										siegeZoneChanged ||
												evaluateSiegeZoneOccupant(
														player,
														siegeZone,
														siegeZone.getAttackerPlayerScoreTimeMap(),
														TownySettings.getSiegeWarPointsPerAttackingPlayer());
							}
						}
					}
				} catch (NotRegisteredException e) {
				}
			}

			//Remove garbage from player score time maps
			siegeZoneChanged =
					siegeZoneChanged ||
							removeGarbageFromPlayerScoreTimeMap(siegeZone.getDefenderPlayerScoreTimeMap(),
									siegeZone.getDefendingTown(),
									null);

			siegeZoneChanged =
					siegeZoneChanged ||
							removeGarbageFromPlayerScoreTimeMap(siegeZone.getDefenderPlayerScoreTimeMap(),
									null,
									siegeZone.getAttackingNation());

			//Save siege zone to db if it was changed
			if(siegeZoneChanged) {
				TownyUniverse.getDataSource().saveSiegeZone(siegeZone);
			}
		}
	}

	private static void evaluateSiege(com.palmergames.bukkit.towny.war.siegewar.locations.Siege siege) {
		//Process active siege
		if (siege.getStatus() == SiegeStatus.IN_PROGRESS) {

			//If scheduled end time has arrived, choose winner
			if (System.currentTimeMillis() > siege.getScheduledEndTime()) {
				TownyObject siegeWinner = SiegeWarUtil.calculateSiegeWinner(siege);
				if (siegeWinner instanceof Town) {
					DefenderWin.defenderWin(siege, (Town) siegeWinner);
				} else {
					AttackerWin.attackerWin(siege, (Nation) siegeWinner);
				}

				//Save changes to db
				TownyUniverse.getDataSource().saveTown(siege.getDefendingTown());
			}

		} else {

			//Siege is finished.
			//Wait for siege immunity timer to end then delete siege
			if (System.currentTimeMillis() > siege.getDefendingTown().getSiegeImmunityEndTime()) {
				TownyUniverse.getDataSource().removeSiege(siege);
			}
		}
	}


	private static boolean evaluateSiegeZoneOccupant(Player player,
													 com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone siegeZone,
													 Map<Player, Long> playerScoreTimeMap,
													 int siegePointsForZoneOccupation) {

		//Is the player already registered as being in the siege zone ?
		if (playerScoreTimeMap.containsKey(player)) {

			//Player must still be in zone
			if (!SiegeWarUtil.isPlayerInSiegePointZone(player, siegeZone)) {
				playerScoreTimeMap.remove(player);
				return true;
			}

			//Player must still be in the open
			if(SiegeWarUtil.doesPlayerHaveANonAirBlockAboveThem(player)) {
				playerScoreTimeMap.remove(player);
				return true;
			}

			//Player must have been there long enough
			if (System.currentTimeMillis() > playerScoreTimeMap.get(player)) {
				siegeZone.adjustSiegePoints(siegePointsForZoneOccupation);
				playerScoreTimeMap.put(player,
						System.currentTimeMillis()
								+ (TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds() * TimeMgmt.ONE_SECOND_IN_MILLIS));
				return true;
			}

			return false; //No change

		} else {
			//Player must be in zone
			if (!SiegeWarUtil.isPlayerInSiegePointZone(player, siegeZone)) {
				return false;
			}

			//Player must be in the open
			if(SiegeWarUtil.doesPlayerHaveANonAirBlockAboveThem(player)) {
				return false;
			}

			//Player must not be flying or invisible
			if(player.isFlying() || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null) {
				return false;
			}

			playerScoreTimeMap.put(player,
					System.currentTimeMillis()
							+ (TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds() * TimeMgmt.ONE_SECOND_IN_MILLIS));

			System.out.println("DURATION MILLIS: " + System.currentTimeMillis()
					+ (TownySettings.getWarSiegeZoneOccupationScoringTimeRequirementSeconds()
					* TimeMgmt.ONE_SECOND_IN_MILLIS));

			return true; //Player added to zone
		}
	}


	private static boolean removeGarbageFromPlayerScoreTimeMap(Map<Player, Long> map,
															   Town townFilter,
															   Nation nationFilter) {

		boolean siegeZoneChanged = false;

		try {
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Player,Long> pair = (Map.Entry)it.next();

				if(!pair.getKey().isOnline()) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				Resident resident = TownyUniverse.getDataSource().getResident(pair.getKey().getName());

				if(!resident.hasTown()) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				if(townFilter != null && resident.getTown() != townFilter) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}

				if(nationFilter != null && resident.getTown().getNation() != nationFilter) {
					it.remove();
					siegeZoneChanged = true;
					continue;
				}
			}

		} catch (Exception e) {
		}

		return siegeZoneChanged;
	}

}