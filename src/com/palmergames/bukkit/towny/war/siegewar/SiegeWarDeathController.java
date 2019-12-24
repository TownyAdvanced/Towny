package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

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
	 * Evaluates a player death event.
	 * If the player is involved in any nearby sieges, their opponents gain siege points.
	 * 
	 * NOTE: We do not try to determine the cause of death
	 * This is useful because it allows a siege participant to gain points from kills by:
	 *   1. Devices / Traps 
	 *   2. Friends who are nomads, in friendly non-nation towns, or secretly allied to the participant.
	 *   
	 * @param deadPlayer The player who died
	 */
	public static void evaluateSiegeWarDeath(Player deadPlayer)  {
		TownyUniverse universe = TownyUniverse.getInstance();
		
		//Cycle through all siege zones
		for(SiegeZone siegeZone: TownyUniverse.getInstance().getDataSource().getSiegeZones()) {
			
			try {
				//Is the siege in progress?
				if (siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
					continue;

				//Did the death occur in the 'death points zone' ?
				if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) > TownySettings.getWarSiegeZoneDeathRadiusBlocks())
					continue;

				Resident deadResident = universe.getDataSource().getResident(deadPlayer.getName());

				if (!deadResident.hasTown())
					continue;

				Town residentTown = deadResident.getTown();

				//Residents of occupied towns cannot affect siege points.
				if(residentTown.isOccupied())
					continue;

				//Is resident a defender ?
				if (residentTown == siegeZone.getDefendingTown()) {
					awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
					continue;
				}

				if (!residentTown.hasNation())
					continue;

				Nation residentNation = residentTown.getNation();
				
				//Is resident an attacker?
				if (residentNation == siegeZone.getAttackingNation()) {
					awardSiegeDeathPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone);
					continue;
				}

				if (!siegeZone.getDefendingTown().hasNation())
					continue;

				Nation defendingNation = siegeZone.getDefendingTown().getNation();

				//Is resident a member of the defending nation
				if (residentNation == defendingNation) {
					awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
					continue;
				}
				
			} catch (NotRegisteredException e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private static void awardSiegeDeathPoints(boolean attackerDeath,
									   TownyObject pointsRecipient, 
									   Resident deadResident,
									   SiegeZone siegeZone) throws NotRegisteredException {
		
		//Give siege points to opposing side
		int siegePoints;
		if (attackerDeath) {
			siegePoints = TownySettings.getWarSiegePointsForAttackerDeath();
			siegeZone.adjustSiegePoints(-siegePoints);
		} else {
			siegePoints = TownySettings.getWarSiegePointsForDefenderDeath();
			siegeZone.adjustSiegePoints(siegePoints);
		}

		//Send messages to siege participants
		String message = String.format(
			TownySettings.getLangString("msg_siege_war_participant_death"),
			TownyFormatter.getFormattedName(deadResident),
			TownyFormatter.getFormattedName(siegeZone.getDefendingTown()),
			siegePoints,
			TownyFormatter.getFormattedName(pointsRecipient));

		TownyMessaging.sendPrefixedNationMessage(siegeZone.getAttackingNation(), message);
		if (siegeZone.getDefendingTown().hasNation()) {
			TownyMessaging.sendPrefixedNationMessage(siegeZone.getDefendingTown().getNation(), message);
		} else {
			TownyMessaging.sendPrefixedTownMessage(siegeZone.getDefendingTown(), message);
		}
	}
	
}
