package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
        TownyObject winner = siege.getDefendingTown();
        int winningPoints = 0;

        for(Map.Entry<Nation, SiegeZone> entry: siege.getSiegeZones().entrySet()) {
            if(entry.getValue().getSiegePoints() > winningPoints) {
                winner = entry.getKey();
                winningPoints = entry.getValue().getSiegePoints();
            }
        }

        return winner;
    }

	/**
	 * This method determines if a players is in the 'timed point zone' of a siegezone
	 * 
	 * - Must be in same world as flag
	 * - Must be in wilderness  (This is important, otherwise the defender could create a 'safe space' 
	 *                           inside a perm-protected town block, and gain points there with no threat.)
	 * - Must be within 1 townblock length of the flag
	 *
	 * @param player the player
	 * @param siegeZone the siege zone
	 * @return true if a player in in the timed point zone
	 */
	public static boolean isPlayerInTimedPointZone(Player player, SiegeZone siegeZone) {
		return player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
				&& !TownyAPI.getInstance().hasTownBlock(player.getLocation())
				&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize();
	}

	/**
	 * This method determines if a player is in the 'death point zone' of a siegezone
	 * This zone is usually a large (configurable) simple radius around the siege banner
	 *
	 * @param player the player
	 * @param resident the resident
	 * @param siegeZone the siege zone
	 * @return true if a player in in the death point zone
	 */
	private static boolean isPlayerInDeathPointZone(Player player, Resident resident, SiegeZone siegeZone) {
		if(player == null)
			player = TownyAPI.getInstance().getPlayer(resident); //Gets player if online, null otherwise

		if(player != null) {
			//User location of online player
			return player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks();
		} else {
			//Use location of logged out player
			Map<Resident, Location> recentlyLoggedOutResidentLocationMap = TownyUniverse.getInstance().getRecentlyLoggedOutResidentLocationMap();
			if(recentlyLoggedOutResidentLocationMap.containsKey(resident)) {
				return recentlyLoggedOutResidentLocationMap.get(resident).distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks();
			} else {
				/*
				 * We get here if the player is offline, and a server reboot has occurred since they logged off.
				 * We don't have location information any more, so cannot determine if player was in zone.
				 * No punish (because this is very unlikely to be exploited).
				 */
				return false;
			}
		}
	}

	/**
	 * This method evaluates a fighter being 'removed' in some way while a siege is ongoing,
	 * and determines if a siege point penalty applies
	 *
	 * @param town the town which the player belongs to
	 * @param resident the possible fighter being removed
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 */
	public static void evaluateFighterRemovalPenalty(Town town, Resident resident, String unformattedErrorMessage) {
		//Apply to town
		Town townWhereResidentIsGuard =
			SiegeWarPointsUtil.evaluateGuardRemovalPenalty(
				town,
				resident,
				unformattedErrorMessage);

		//Apply to nation
		if(town.hasNation()) {

			Nation nation;
			try {
				nation = town.getNation();
			} catch (NotRegisteredException e) { return; }

			SiegeWarPointsUtil.evaluateSoldierRemovalPenalty(
				nation,
				resident,
				townWhereResidentIsGuard,
				unformattedErrorMessage);

			//Apply to mutual allies
			for(Nation alliedNation: nation.getMutualAllies()) {
				SiegeWarPointsUtil.evaluateSoldierRemovalPenalty(
					alliedNation,
					resident,
					null,
					unformattedErrorMessage);
			}
		}
	}

	/**
	 * This method evaluates a guard being 'removed' in some way while a siege is ongoing,
	 * and determines if a siege point penalty applies
	 *
	 * @param town the town which the player belongs to
	 * @param resident the possible guard being removed
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 */
	public static Town evaluateGuardRemovalPenalty(Town town,
													  Resident resident,
													  String unformattedErrorMessage) {

		if(!(town.hasSiege() && town.getSiege().getStatus() == SiegeStatus.IN_PROGRESS))
			return null;

		TownyUniverse universe = TownyUniverse.getInstance();
		if(universe.getPermissionSource().has(resident, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS)) {
			for(SiegeZone siegeZone: town.getSiege().getSiegeZones().values()) {
				awardPointsIfPlayerIsInDeathPointZone(false, null, resident, siegeZone, unformattedErrorMessage);
			}
			return town;
		} else {
			return null;
		}
	}

	/**
	 * This method evaluates a soldier being 'removed' in some way while a siege is ongoing,
	 * and determines if a siege point penalty applies
	 *
	 * @param nation the nation to apply the penalty to
	 * @param resident the possible soldier being removed
	 * @param townToExclude (optional) a town to be excluded from the penalty
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 */
	public static void evaluateSoldierRemovalPenalty(Nation nation,
													 	Resident resident,
													  	Town townToExclude,
													  	String unformattedErrorMessage) {
		TownyUniverse universe = TownyUniverse.getInstance();
		if(universe.getPermissionSource().has(resident, PermissionNodes.TOWNY_NATION_SIEGE_POINTS)) {
			//Apply penalty to siege-zones where the nation is attacking
			for(SiegeZone siegeZone: nation.getActiveSiegeAttackZones()) {
				awardPointsIfPlayerIsInDeathPointZone(true, null, resident, siegeZone, unformattedErrorMessage);
			}
			//Apply penalty to siege-zones where the nation is defending
			for(SiegeZone siegeZone: nation.getActiveSiegeDefenceZones(townToExclude)) {
				awardPointsIfPlayerIsInDeathPointZone(false, null, resident, siegeZone, unformattedErrorMessage);
			}
		}
	}

	/**
	 * This method applies penalty points to a player if they are in the given siegezone
	 * Offline players will also be punished
	 *
	 * @param residentIsAttacker is the resident an attacker or defender?
	 * @param player the player who the penalty relates to
	 * @param resident the resident who the penalty relates to
	 * @param siegeZone to siegezone to apply the penalty to
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 * @return true if points awarded
	 */
	public static boolean awardPointsIfPlayerIsInDeathPointZone(boolean residentIsAttacker,
																Player player,
																Resident resident,
																SiegeZone siegeZone,
																String unformattedErrorMessage) {
		//Return false if player is not in death point zone
		if(!isPlayerInDeathPointZone(player, resident, siegeZone))
			return false;

		//Give siege points to opposing side
		int siegePoints;
		if (residentIsAttacker) {
			siegePoints = -TownySettings.getWarSiegePointsForAttackerDeath();
			siegePoints = adjustSiegePointGainForCurrentSiegePointBalance(siegePoints, siegeZone);
			siegePoints = adjustSiegePenaltyPointsForMilitaryLeadership(residentIsAttacker, siegePoints, player, resident, siegeZone);
			siegeZone.adjustSiegePoints(siegePoints);
		} else {
			siegePoints = TownySettings.getWarSiegePointsForDefenderDeath();
			siegePoints = adjustSiegePointGainForCurrentSiegePointBalance(siegePoints, siegeZone);
			siegePoints = adjustSiegePenaltyPointsForMilitaryLeadership(residentIsAttacker, siegePoints, player, resident, siegeZone);
			siegeZone.adjustSiegePoints(siegePoints);
		}

		TownyUniverse.getInstance().getDataSource().saveSiegeZone(siegeZone);

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
			siegeZone.getDefendingTown().getFormattedName(),
			residentInformationString,
			siegePoints);

		SiegeWarNotificationUtil.informSiegeParticipants(siegeZone, message);

		return true;
	}

	/**
	 * This method returns an altered siege point gain, depending on the current siege point balance
	 *
	 * @param baseSiegePointGain the base  siege point gain
	 * @param siegeZone to siegezone where the gain will be applied
	 * @return the altered gain
	 */
	public static int adjustSiegePointGainForCurrentSiegePointBalance(double baseSiegePointGain, SiegeZone siegeZone) {
		//Reduce gain if you already have an advantage
		if(TownySettings.getWarSiegePercentagePointsGainDecreasePer1000Advantage() > 0) {
			if(
				(siegeZone.getSiegePoints() > 0 && baseSiegePointGain > 0)
					||
					(siegeZone.getSiegePoints() < 0 && baseSiegePointGain < 0)
			) {
				int numThousands = Math.abs(siegeZone.getSiegePoints() / 1000);
				int percentageDecrease = numThousands * TownySettings.getWarSiegePercentagePointsGainDecreasePer1000Advantage();
				double actualDecrease = baseSiegePointGain / 100 * percentageDecrease;
				baseSiegePointGain -= actualDecrease;
				return (int)baseSiegePointGain;
			}
		}

		//Increase gain if you already have a disadvantage
		if(TownySettings.getWarSiegePercentagePointsGainIncreasePer1000Disadvantage() > 0) {
			if(
				(siegeZone.getSiegePoints() > 0 && baseSiegePointGain < 0)
					||
					(siegeZone.getSiegePoints() < 0 && baseSiegePointGain > 0)
			) {
				int numThousands = Math.abs(siegeZone.getSiegePoints() / 1000);
				int percentageIncrease = numThousands * TownySettings.getWarSiegePercentagePointsGainIncreasePer1000Disadvantage();
				double actualIncrease = baseSiegePointGain / 100 * percentageIncrease;
				baseSiegePointGain += actualIncrease;
				return (int)baseSiegePointGain;
			}
		}

		return (int)baseSiegePointGain;
	}

	private static int adjustSiegePenaltyPointsForMilitaryLeadership(boolean residentIsAttacker,
																	 double siegePoints,
																	 Player player,
																	 Resident resident,
																	 SiegeZone siegeZone) {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();

			//Resident town has nation
			if(resident.getTown().hasNation()) {

				if(universe.getPermissionSource().has(resident, PermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP)) {
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
									&& universe.getPermissionSource().has(otherResident, PermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP)
									&& (otherResident.getTown().getNation() == resident.getTown().getNation() || otherResident.getTown().getNation().hasMutualAlly(resident.getTown().getNation()))
									&& player.getLocation().distance(otherPlayer.getLocation()) < TownySettings.getWarSiegeLeadershipAuraRadiusBlocks()) {
									friendlyLeaderNearby = true;
									continue;
								}
							}

							//As attacker, look for hostile military leader
							if (!hostileLeaderNearby && residentIsAttacker) {
								otherResident = universe.getDataSource().getResident(otherPlayer.getName());

								if (otherResident.hasTown()
									&& otherResident.getTown().hasNation()
									&& siegeZone.getDefendingTown().hasNation()
									&& universe.getPermissionSource().has(otherResident, PermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP)
									&& (otherResident.getTown().getNation() == siegeZone.getDefendingTown().getNation() || otherResident.getTown().getNation().hasMutualAlly(siegeZone.getDefendingTown().getNation()))
									&& player.getLocation().distance(otherPlayer.getLocation()) < TownySettings.getWarSiegeLeadershipAuraRadiusBlocks()) {
									hostileLeaderNearby = true;
									continue;
								}
							}

							//As defender, look for hostile military leader
							if (!hostileLeaderNearby && !residentIsAttacker) {
								otherResident = universe.getDataSource().getResident(otherPlayer.getName());

								if (otherResident.hasTown()
									&& otherResident.getTown().hasNation()
									&& universe.getPermissionSource().has(otherResident, PermissionNodes.TOWNY_NATION_SIEGE_LEADERSHIP)
									&& (otherResident.getTown().getNation() == siegeZone.getAttackingNation() || otherResident.getTown().getNation().hasMutualAlly(siegeZone.getAttackingNation()))
									&& player.getLocation().distance(otherPlayer.getLocation()) < TownySettings.getWarSiegeLeadershipAuraRadiusBlocks()) {
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
}
