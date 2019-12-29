package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;
import org.bukkit.entity.Player;

/**
 * This class intercepts 'player death' events coming from the towny entity monitor listener class.
 *
 * This class evaluates the death, and determines if the player is involved in any nearby sieges.
 * If so, their opponents gain siege points.
 * 
 * @author Goosius
 */
public class SiegeWarDeathController {

	/**
	 * Evaluates a PVP death event.
	 * 
	 * If both players are directly involved in a nearby siege, the killer's side gains siege points:
	 * 
	 * NOTE: 
	 * Allied nations or friendly towns can still be involved in sieges,
	 * (e.g. via resource support, scouting, spying, diversions, or attacking enemy combatants),
	 * but they cannot directly affect the siege points totals. 
	 *
	 * @param deadPlayer The player who died
	 * @param killerPlayer The player who did the killing
	 * @param deadResident The resident who died
	 * @param killerResident The resident who did the killing
	 *  
	 */
	public static void evaluateSiegePvPDeath(Player deadPlayer, Player killerPlayer, Resident deadResident, Resident killerResident)  {
		
		try {
			if (!deadResident.hasTown())
				return;

			if (!killerResident.hasTown())
				return;

			Town deadResidentTown = deadResident.getTown();
			Town killerResidentTown = killerResident.getTown();

			//Residents of occupied towns cannot affect siege points.
			if(deadResidentTown.isOccupied() || killerResidentTown.isOccupied())
				return;

			//Was the dead player a resident of a besieged town, killed by a siege attacker, in the siege death point zone ?
			TownyUniverse universe = TownyUniverse.getInstance();
			if (deadResidentTown.hasSiege()
				&& killerResidentTown.hasNation()
				&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
				&& deadResidentTown.getSiege().getSiegeZones().containsKey(killerResidentTown.getNation())
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {

				if (!universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode()))
					return;

				SiegeZone siegeZone = deadResidentTown.getSiege().getSiegeZones().get(killerResidentTown.getNation());

				//Did the death occur in the siege death point zone?
				if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
					SiegeWarPointsUtil.awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone, TownySettings.getLangString("msg_siege_war_participant_death"));
					return;
				}

			} else if (deadResidentTown.hasNation()
				&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

				//Was the dead player a member of a besieging nation, killed by a siege defender, in the siege death point zone ?
				Nation deadResidentNation = deadResidentTown.getNation();
				if (killerResidentTown.hasSiege()
					&& killerResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& killerResidentTown.getSiege().getSiegeZones().containsKey(deadResidentNation)) {

					if (!universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode()))
						return;

					SiegeZone siegeZone = killerResidentTown.getSiege().getSiegeZones().get(deadResidentNation);

					//Did the death occur in the siege death point zone?
					if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						SiegeWarPointsUtil.awardSiegePenaltyPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone, TownySettings.getLangString("msg_siege_war_participant_death"));
						return;
					}

				//Was the dead player a member of a nation under attack, killed by a siege attacker, in the death points zone ?
				} else if (killerResidentTown.hasNation()) {
					for (SiegeZone siegeZone : killerResidentTown.getNation().getSiegeZones()) {
						if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
							&& siegeZone.getDefendingTown().hasNation()
							&& siegeZone.getDefendingTown().getNation() == deadResidentNation) {

							if (!universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode()))
								return;

							//Did the death occur in the siege death point zone?
							if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
								SiegeWarPointsUtil.awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone, TownySettings.getLangString("msg_siege_war_participant_death"));
								return;
							}
						}
					}
				}
			}

		} catch (NotRegisteredException e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege pvp death");
		}
	}
	
}
