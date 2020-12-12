package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
	 * - Their side loses siege points 
	 * - Their inventory items degrade a little (e.g. 10%)
	 *
	 * This mechanic allows for a wide range of siege-kill-tactics.
	 * Examples:
	 * - Players without towns can contribute to siege points
	 * - Players from non-nation towns can contribute to siege points
	 * - Players from secretly-allied nations can contribute to siege points
	 * - Devices (cannons, traps, bombs etc.) can be used to gain siege points
	 *
	 * @param deadPlayer The player who died
	 * @param playerDeathEvent The player death event
	 *
	 */
	public static void evaluateSiegePlayerDeath(Player deadPlayer, PlayerDeathEvent playerDeathEvent)  {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Resident deadResident = universe.getResident(deadPlayer.getUniqueId());

			if (deadResident == null || !deadResident.hasTown())
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

				//Skip if player is not is siege-zone
				if(!SiegeWarDistanceUtil.isInSiegeZone(deadPlayer, candidateSiege))
					continue;

				//Is player eligible ?
				if (deadResidentTown.hasSiege()
					&& deadResidentTown.getSiege().getStatus().isActive()
					&& deadResidentTown.getSiege() == candidateSiege
					&& (universe.getPermissionSource().testPermission(deadPlayer, SiegeWarPermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
						|| hasTownMilitaryRank(deadResident))
				) {
					candidateSiegePlayerSide = SiegeSide.DEFENDERS; //Candidate siege has player defending own-town

				} else if (deadResidentTown.hasNation()
					&& candidateSiege.getDefendingTown().hasNation()
					&& candidateSiege.getStatus().isActive()
					&& (universe.getPermissionSource().testPermission(deadPlayer, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
						|| hasNationMilitaryRank(deadResident))
					&& (deadResidentTown.getNation() == candidateSiege.getDefendingTown().getNation()
						|| deadResidentTown.getNation().hasMutualAlly(candidateSiege.getDefendingTown().getNation()))) {

					candidateSiegePlayerSide = SiegeSide.DEFENDERS; //Candidate siege has player defending another town

				} else if (deadResidentTown.hasNation()
					&& candidateSiege.getStatus().isActive()
					&& (universe.getPermissionSource().testPermission(deadPlayer, SiegeWarPermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
						|| hasNationMilitaryRank(deadResident))
					&& (deadResidentTown.getNation() == candidateSiege.getAttackingNation() 
						|| deadResidentTown.getNation().hasMutualAlly(candidateSiege.getAttackingNation()))) {

					candidateSiegePlayerSide = SiegeSide.ATTACKERS; //Candidate siege has player attacking

				} else {
					continue;
				}

				//Confirm candidate siege if it is 1st viable one OR closer than confirmed candidate
				candidateSiegeDistanceToPlayer = deadPlayer.getLocation().distance(candidateSiege.getFlagLocation());
				if(confirmedCandidateSiege == null || candidateSiegeDistanceToPlayer < confirmedCandidateDistanceToPlayer) {
					confirmedCandidateSiege = candidateSiege;
					confirmedCandidateSiegePlayerSide = candidateSiegePlayerSide;
					confirmedCandidateDistanceToPlayer = candidateSiegeDistanceToPlayer;
				}
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
						Translation.of("msg_siege_war_defender_death"));

				} else {
					SiegeWarPointsUtil.awardPenaltyPoints(
						true,
						deadPlayer,
						deadResident,
						confirmedCandidateSiege,
						Translation.of("msg_siege_war_attacker_death"));
				}

				degradeInventory(playerDeathEvent);
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

	private static void degradeInventory(PlayerDeathEvent playerDeathEvent) {
		Damageable damageable;
		double maxDurability;
		int currentDurability;
		int damageToInflict;
		int newDurability;
		if(SiegeWarSettings.getWarSiegeDeathPenaltyDegradeInventoryEnabled()) {
			for(ItemStack itemStack: playerDeathEvent.getEntity().getInventory().getContents()) {
				if (itemStack != null && itemStack.getItemMeta() instanceof Damageable) {
					damageable = ((Damageable) itemStack.getItemMeta());
					maxDurability = itemStack.getType().getMaxDurability();
					currentDurability = damageable.getDamage();
					damageToInflict = (int)(maxDurability / 100 * SiegeWarSettings.getWarSiegeDeathPenaltyDegradeInventoryPercentage());
					newDurability = currentDurability + damageToInflict;
					damageable.setDamage(newDurability);
					itemStack.setItemMeta((ItemMeta)damageable);
				}
			}
		}
	}

	private static void keepInventory(PlayerDeathEvent playerDeathEvent) {
		if(SiegeWarSettings.getWarSiegeDeathPenaltyKeepInventoryEnabled() && !playerDeathEvent.getKeepInventory()) {
			playerDeathEvent.setKeepInventory(true);
			playerDeathEvent.getDrops().clear();
		}
	}

	//LP Glitch mitigation (TODO - remove this pattern by resolving on the lp config side)
	private static boolean hasTownMilitaryRank(Resident resident) {
		return resident.isMayor()
			|| resident.getTownRanks().contains("guard")
			|| resident.getTownRanks().contains("sheriff");
	}

	private static boolean hasNationMilitaryRank(Resident resident) {
		return resident.isKing()
			|| resident.getNationRanks().contains("soldier")
			|| resident.getNationRanks().contains("captain")
			|| resident.getNationRanks().contains("general");
	}
}
