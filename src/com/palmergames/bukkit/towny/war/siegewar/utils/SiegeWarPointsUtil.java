package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains utility functions related to siege points
 * 
 * @author Goosius
 */
public class SiegeWarPointsUtil {

	/**
	 * This method calculates who has won a siege
	 * 
	 * Defending Town - The defending town has won the siege if all of the siege zones are in negative points.
	 * Attacking Nation - an attacking nation has won the siege if its siege points are positive,
	 *                    and higher than the siegepoints of any other attacker.
	 *
	 * @param siege the siege
	 * @return the winner of the siege
	 */
	public static TownyObject calculateSiegeWinner(Siege siege) {
		if(siege.getSiegePoints() > 0) {
			return siege.getAttackingNation();
		} else {
			return siege.getDefendingTown();
		}
    }

	/**
	 * This method determines if a players is in the 'timed point zone' of a siege
	 * 
	 * - Must be in same world as flag
	 * - Must be in wilderness  (This is important, otherwise the defender could create a 'safe space' 
	 *                           inside a perm-protected town block, and gain points there with no threat.)
	 * - Must be within 1 townblock length of the flag
	 *
	 * @param player the player
	 * @param siege the siege
	 * @return true if a player in in the timed point zone
	 */
	public static boolean isPlayerInTimedPointZone(Player player, Siege siege) {
		return TownyAPI.getInstance().isWilderness(player.getLocation())
				&& SiegeWarDistanceUtil.isInTimedPointZone(player, siege);
	}

	/**
	 * This method applies penalty points to a player if they are in the given siegezone
	 * Offline players will also be punished
	 *
	 * @param residentIsAttacker is the resident an attacker or defender?
	 * @param player the player who the penalty relates to
	 * @param resident the resident who the penalty relates to
	 * @param siege the siege to apply the penalty to
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 */
	public static void awardPenaltyPoints(boolean residentIsAttacker,
											 Player player,
											 Resident resident,
											 Siege siege,
											 String unformattedErrorMessage) {
		//Give siege points to opposing side
		int siegePoints;
		if (residentIsAttacker) {
			siegePoints = -TownySettings.getWarSiegePointsForAttackerDeath();
			siegePoints = adjustSiegePointPenaltyForBannerControl(true, siegePoints, siege);
			siegePoints = adjustSiegePenaltyPointsForMilitaryLeadership(true, siegePoints, player, resident, siege);
			siegePoints = adjustSiegePointsForPopulationQuotient(false, siegePoints, siege);
			siege.adjustSiegePoints(siegePoints);
		} else {
			siegePoints = TownySettings.getWarSiegePointsForDefenderDeath();
			siegePoints = adjustSiegePointPenaltyForBannerControl(false, siegePoints, siege);
			siegePoints = adjustSiegePenaltyPointsForMilitaryLeadership(false, siegePoints, player, resident, siege);
			siegePoints = adjustSiegePointsForPopulationQuotient(true, siegePoints, siege);
			siege.adjustSiegePoints(siegePoints);
		}

		TownyUniverse.getInstance().getDataSource().saveSiege(siege);

		//Send messages to siege participants
		String residentInformationString;
		try {
			if(resident.hasTown()) {
				Town residentTown = resident.getTown();
				if(residentTown.hasNation())
					residentInformationString = resident.getName() + " (" + residentTown.getName() + " | " + residentTown.getNation().getName() + ")";
				else
					residentInformationString = resident.getName() + " (" + residentTown.getName() + ")";
			} else {
				residentInformationString = resident.getName();
			}
		} catch (NotRegisteredException e) { residentInformationString = ""; }

		String message = String.format(
			unformattedErrorMessage,
			siege.getDefendingTown().getName(),
			residentInformationString,
			Math.abs(siegePoints));

		SiegeWarNotificationUtil.informSiegeParticipants(siege, message);
	}

	private static int adjustSiegePenaltyPointsForMilitaryLeadership(boolean residentIsAttacker,
																	 double siegePoints,
																	 Player player,
																	 Resident resident,
																	 Siege siege) {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();

			//Resident town has nation
			if(resident.getTown().hasNation()) {

				if(universe.getPermissionSource().testPermission(resident.getPlayer(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP.getNode())) {
					//Player is Leader. Apply points increase
					double modifier = 1 + (TownySettings.getWarSiegePointsPercentageAdjustmentForLeaderDeath() / 100);
					return (int)(siegePoints * modifier);

				} else {
					//Player is not leader
					if(player == null) {
						//Player is null. Apply points increase regardless of player location/online/offline, to avoid exploits
						double modifier = 1 + (TownySettings.getWarSiegePointsPercentageAdjustmentForLeaderProximity() / 100);
						return (int)(siegePoints * modifier);
					} else {
						//Player is online. Look for nearby friendly/hostile leaders
						Resident otherResident;
						boolean friendlyLeaderNearby = false;
						boolean hostileLeaderNearby = false;

						for (Player otherPlayer : BukkitTools.getOnlinePlayers()) {
							if (friendlyLeaderNearby && hostileLeaderNearby)
								break;

							//Look for friendly military leader 
							if (!friendlyLeaderNearby) {
								otherResident = universe.getDataSource().getResident(otherPlayer.getName());
								if (otherResident.hasTown()
									&& otherResident.hasNation()
									&& universe.getPermissionSource().testPermission(otherResident.getPlayer(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP.getNode())
									&& (otherResident.getTown().getNation() == resident.getTown().getNation() || otherResident.getTown().getNation().hasMutualAlly(resident.getTown().getNation()))
									&& SiegeWarDistanceUtil.isCloseToLeader(player, otherPlayer)) {
									friendlyLeaderNearby = true;
									continue;
								}
							}

							//As attacker, look for hostile military leader
							if (!hostileLeaderNearby && residentIsAttacker) {
								otherResident = universe.getDataSource().getResident(otherPlayer.getName());

								if (otherResident.hasTown()
									&& otherResident.getTown().hasNation()
									&& siege.getDefendingTown().hasNation()
									&& universe.getPermissionSource().testPermission(otherResident.getPlayer(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP.getNode())
									&& (otherResident.getTown().getNation() == siege.getDefendingTown().getNation() || otherResident.getTown().getNation().hasMutualAlly(siege.getDefendingTown().getNation()))
									&& SiegeWarDistanceUtil.isCloseToLeader(player, otherPlayer)) {
									hostileLeaderNearby = true;
									continue;
								}
							}

							//As defender, look for hostile military leader
							if (!hostileLeaderNearby && !residentIsAttacker) {
								otherResident = universe.getDataSource().getResident(otherPlayer.getName());

								if (otherResident.hasTown()
									&& otherResident.getTown().hasNation()
									&& universe.getPermissionSource().testPermission(otherResident.getPlayer(), SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP.getNode())
									&& (otherResident.getTown().getNation() == siege.getAttackingNation() || otherResident.getTown().getNation().hasMutualAlly(siege.getAttackingNation()))
									&& SiegeWarDistanceUtil.isCloseToLeader(player, otherPlayer)) {
									hostileLeaderNearby = true;
									continue;
								}
							}
						}

						if (friendlyLeaderNearby && !hostileLeaderNearby) {
							//Friendly leader nearby. Apply points decrease
							double modifier = 1 - (TownySettings.getWarSiegePointsPercentageAdjustmentForLeaderProximity() / 100);
							return (int) (siegePoints * modifier);
						} else if (hostileLeaderNearby && !friendlyLeaderNearby) {
							//Enemy leader nearby. Apply points increase
							double modifier = 1 + (TownySettings.getWarSiegePointsPercentageAdjustmentForLeaderProximity() / 100);
							return (int) (siegePoints * modifier);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problem adjusting siege point penalty for military leadership");
			e.printStackTrace();
		}

		return (int)siegePoints;
	}

	public static void updatePopulationBasedSiegePointModifiers() {
		Map<Nation,Integer> nationSidePopulationsCache = new HashMap<>();
		for (Siege siege : TownyUniverse.getInstance().getDataSource().getSieges()) {
			updateSiegePointPopulationModifier(siege, nationSidePopulationsCache);
		}
	}

	private static void updateSiegePointPopulationModifier(Siege siege, Map<Nation,Integer> nationSidePopulationsCache) {
		Nation nation = null;
		int attackerPopulation;
		int defenderPopulation;

		//Calculate defender population
		if(siege.getDefendingTown().hasNation()) {
			try {
				nation = siege.getDefendingTown().getNation();
			} catch (NotRegisteredException e) {
			}
			if(nationSidePopulationsCache != null && nationSidePopulationsCache.containsKey(nation)) {
				defenderPopulation = nationSidePopulationsCache.get(nation);
			} else {
				defenderPopulation = nation.getNumResidents();
				for(Nation alliedNation: nation.getMutualAllies()) {
					defenderPopulation += alliedNation.getNumResidents();
				}
				if(nationSidePopulationsCache != null) 
					nationSidePopulationsCache.put(nation, defenderPopulation);
			}
		} else {
			defenderPopulation = siege.getDefendingTown().getNumResidents();
		}

		//Calculate attacker population
		nation = siege.getAttackingNation();
		if(nationSidePopulationsCache != null && nationSidePopulationsCache.containsKey(nation)) {
			attackerPopulation = nationSidePopulationsCache.get(nation);
		} else {
			attackerPopulation = nation.getNumResidents();
			for (Nation alliedNation : nation.getMutualAllies()) {
				attackerPopulation += alliedNation.getNumResidents();
			}
			if (nationSidePopulationsCache != null)
				nationSidePopulationsCache.put(nation, attackerPopulation);
		}

		//Note which side has the lower population
		siege.setAttackerHasLowestPopulation(attackerPopulation < defenderPopulation);

		/*
		 * Calculate siege point modifier
		 * 
		 * Terminology: 
		 * The 'quotient' is the number of times the smaller population is contained in the larger one
		 */
		double maxPopulationQuotient = TownySettings.getWarSiegePopulationQuotientForMaxPointsBoost();
		double actualPopulationQuotient;
			if(siege.isAttackerHasLowestPopulation()) {
				actualPopulationQuotient = (double) defenderPopulation / attackerPopulation;
			} else {
				actualPopulationQuotient = (double) attackerPopulation / defenderPopulation;
			}
		double appliedPopulationQuotient;
			if(actualPopulationQuotient < maxPopulationQuotient) {
				appliedPopulationQuotient = actualPopulationQuotient;
			} else {
				appliedPopulationQuotient = maxPopulationQuotient;
			}
			
		//Normalized point boost
		//0 represents no boost
		//1 represents max boost
		double normalizedPointBoost = (appliedPopulationQuotient -1) / (maxPopulationQuotient -1);
	
		//Siege Point modifier
		//Lowest possible value should be 1.
		//Highest possible value should be the max boost value in the config
		double siegePointModifier = 1 + (normalizedPointBoost * (TownySettings.getWarSiegeMaxPopulationBasedPointBoost() -1));
		
		siege.setSiegePointModifierForSideWithLowestPopulation(siegePointModifier);
	}

	public static int adjustSiegePointsForPopulationQuotient(boolean attackerGain, int siegePoints, Siege siege) {
		if(!TownySettings.getWarSiegePopulationBasedPointBoostsEnabled()) {
			return siegePoints;
		}

		if (siege.getSiegePointModifierForSideWithLowestPopulation() == 0) {
			updateSiegePointPopulationModifier(siege, null); //Init values
		}

		if((attackerGain && !siege.isAttackerHasLowestPopulation())
			|| (!attackerGain && siege.isAttackerHasLowestPopulation())) {
			return siegePoints;
		}

		double modifier = siege.getSiegePointModifierForSideWithLowestPopulation();
		return (int) (siegePoints * modifier);
	}

	public static int adjustSiegePointPenaltyForBannerControl(boolean residentIsAttacker, int siegePoints, Siege siege) {
		if(!TownySettings.isWarSiegeCounterattackBoosterEnabled())
			return siegePoints;

		if(
			(residentIsAttacker && siege.getBannerControllingSide() == SiegeSide.ATTACKERS)
			||
			(!residentIsAttacker && siege.getBannerControllingSide() == SiegeSide.DEFENDERS)
		) {
			return siegePoints + (int)(siegePoints * siege.getBannerControllingResidents().size() /100 * TownySettings.getWarSiegeCounterattackBoosterExtraDeathPointsPerPlayerPercent());
		} else {
			return siegePoints;
		}
	}
}
