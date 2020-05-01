package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
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

			//If resident was a defending guard, award siege points
			if(deadResidentTown.hasSiege()
				&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
			) {
				boolean pointsAwarded = SiegeWarPointsUtil.awardPointsIfPlayerIsInDeathPointZone(
					false,
					deadPlayer,
					deadResident,
					deadResidentTown.getSiege(),
					TownySettings.getLangString("msg_siege_war_defender_death"));

				if(pointsAwarded) {
					keepInventory(playerDeathEvent);
					return;
				}
			}

			//If resident was a defending soldier, award siege points
			if (deadResidentTown.hasNation()
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			) {
				for (Siege siege : universe.getDataSource().getSieges()) {

					try {
						if (siege.getDefendingTown().hasNation()
							&& siege.getStatus() == SiegeStatus.IN_PROGRESS
							&& (deadResidentTown.getNation() == siege.getDefendingTown().getNation() || deadResidentTown.getNation().hasMutualAlly(siege.getDefendingTown().getNation()))
						) {
							boolean pointsAwarded = SiegeWarPointsUtil.awardPointsIfPlayerIsInDeathPointZone(
								false,
								deadPlayer,
								deadResident,
								siege,
								TownySettings.getLangString("msg_siege_war_defender_death"));

							if (pointsAwarded) {
								keepInventory(playerDeathEvent);
								return;
							}
						}

					} catch (Exception e) {
						try {
							System.out.println("Problem reading siege zone for player death " + siege.getName());
						} catch (Exception e2) {
							System.out.println("Problem reading siege zone for player death (could not read name");
						}
						e.printStackTrace();
					}
				}
			}

			//If resident was an attacking soldier, award siege points
			if (deadResidentTown.hasNation()
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			) {
				for (Siege siege : universe.getDataSource().getSieges()) {

					try {
						if (siege.getStatus() == SiegeStatus.IN_PROGRESS
							&& (deadResidentTown.getNation() == siege.getAttackingNation() || deadResidentTown.getNation().hasMutualAlly(siege.getAttackingNation()))
						) {
							boolean pointsAwarded = SiegeWarPointsUtil.awardPointsIfPlayerIsInDeathPointZone(
								true,
								deadPlayer,
								deadResident,
								siege,
								TownySettings.getLangString("msg_siege_war_attacker_death"));

							if(pointsAwarded) {
								keepInventory(playerDeathEvent);
								return;
							}
						}
					} catch (Exception e) {
						try {
							System.out.println("Problem reading siege zone for player death " + siege.getName());
						} catch (Exception e2) {
							System.out.println("Problem reading siege zone for player death (could not read name");
						}
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error evaluating siege player death");
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
