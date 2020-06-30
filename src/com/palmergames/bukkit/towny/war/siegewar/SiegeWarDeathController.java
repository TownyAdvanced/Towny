package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * This class intercepts 'player death' events coming from the TownyPlayerListener class.
 *
 * This class evaluates the death, and determines if the player is involved in any nearby sieges.
 * If so, their opponents gain siege points, and the player keeps inventory.
 *
 * @author Goosius
 */
public class SiegeWarDeathController {

	/**
	 * Evaluates a siege death event.
	 *
	 * If the dead player is officially involved in a nearby siege, 
	 * their side loses siege points AND they keep their inventory.
	 *
	 * NOTE: 
	 * This mechanic allows for a wide range of siege-kill-tactics.
	 * Examples:
	 * - Players from non-nation towns can contribute to siege points
	 * - Players from secretly-allied nations can contribute to siege points
	 * - Players without official military rank can contribute to siege points
	 * - Devices (cannons, traps, bombs etc.) can be used to gain siege points
	 * - Note that players from Neutral towns can use devices, but are technically prevented from non-device kills.
	 *
	 * @param deadPlayer The player who died
	 * @param playerDeathEvent The player death event
	 *
	 */
	public static void evaluateSiegePlayerDeath(Player deadPlayer, PlayerDeathEvent playerDeathEvent)  {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Resident deadResident = universe.getDataSource().getResident(deadPlayer.getName());

			if (!deadResident.hasTown())
				return;

			Town deadResidentTown = deadResident.getTown();

			//Residents of occupied towns do not give siege points if killed
			if (deadResidentTown.isOccupied())
				return;

			//Declare local variables
			Siege confirmedCandidateSiege = null;
			SiegeSide confirmedCandidateSiegePlayerSide = SiegeSide.NOBODY;
			double confirmedCandidateDistanceToPlayer = 0;
			double candidateSiegeDistanceToPlayer;
			SiegeSide candidateSiegePlayerSide;

			//Find nearest eligible siege
			for (Siege candidateSiege : universe.getDataSource().getSieges()) {

				//Is siege in a different world
				if(!deadPlayer.getLocation().getWorld().getName().equalsIgnoreCase(candidateSiege.getFlagLocation().getWorld().getName()))
					continue;

				//If siege further than the confirmed candidate ?
				candidateSiegeDistanceToPlayer = deadPlayer.getLocation().distance(candidateSiege.getFlagLocation());
				if(confirmedCandidateSiege != null && candidateSiegeDistanceToPlayer > confirmedCandidateDistanceToPlayer)
					continue;

				//Is player eligible ?
				if (deadResidentTown.hasSiege()
					&& deadResidentTown.getSiege().getStatus().isActive()
					&& deadResidentTown.getSiege() == candidateSiege
					&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
				) {
					candidateSiegePlayerSide = SiegeSide.DEFENDERS; //Candidate siege has player defending own-town

				} else if (deadResidentTown.hasNation()
					&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
					&& candidateSiege.getDefendingTown().hasNation()
					&& candidateSiege.getStatus().isActive()
					&& (deadResidentTown.getNation() == candidateSiege.getDefendingTown().getNation()
						|| deadResidentTown.getNation().hasMutualAlly(candidateSiege.getDefendingTown().getNation()))) {

					candidateSiegePlayerSide = SiegeSide.DEFENDERS; //Candidate siege has player defending another town

				} else if (deadResidentTown.hasNation()
					&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
					&& candidateSiege.getStatus().isActive()
					&& (deadResidentTown.getNation() == candidateSiege.getAttackingNation() 
						|| deadResidentTown.getNation().hasMutualAlly(candidateSiege.getAttackingNation()))) {

					candidateSiegePlayerSide = SiegeSide.ATTACKERS; //Candidate siege has player attacking

				} else {
					continue;
				}

				//Now we know candidate is closer than current confirmed candidate, and player is eligible
				confirmedCandidateSiege = candidateSiege;
				confirmedCandidateSiegePlayerSide = candidateSiegePlayerSide;
				confirmedCandidateDistanceToPlayer = candidateSiegeDistanceToPlayer;
			}

			//If player is confirmed as close to one or more sieges in which they are eligible to be involved, 
			// apply siege point penalty for the nearest one, and keep inventory
			if(confirmedCandidateSiege != null) {

				if(confirmedCandidateSiegePlayerSide == SiegeSide.DEFENDERS) {
					SiegeWarPointsUtil.awardPenaltyPoints(
						false,
						deadPlayer,
						deadResident,
						confirmedCandidateSiege,
						TownySettings.getLangString("msg_siege_war_defender_death"));

				} else {
					SiegeWarPointsUtil.awardPenaltyPoints(
						true,
						deadPlayer,
						deadResident,
						confirmedCandidateSiege,
						TownySettings.getLangString("msg_siege_war_attacker_death"));
				}

				keepInventory(playerDeathEvent);
			}
		} catch (Exception e) {
			try {
				System.out.println("Error evaluating siege death for player " + deadPlayer.getName());
			} catch (Exception e2) {
				System.out.println("Error evaluating siege death (could not read player name)");
			}
			e.printStackTrace();
		}
	}

	private static void keepInventory(PlayerDeathEvent playerDeathEvent) {
		if(TownySettings.getWarSiegeKeepInventoryOnSiegeDeath() && !playerDeathEvent.getKeepInventory()) {
			playerDeathEvent.setKeepInventory(true);
			playerDeathEvent.getDrops().clear();
		}
	}

}
