package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
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
	 * This method determines if a players is in the 'point scoring zone' of a siegezone
	 * 
	 * - Must be in same world as flag
	 * - Must be in wilderness  (This is important, otherwise the defender could create a 'safe space' 
	 *                           inside a perm-protected town block, and gain points there with no threat.)
	 * - Must be within 1 townblock length of the flag
	 *
	 * @param player the player
	 * @param siegeZone the siege zone
	 * @return true if a player in in the siege point zone
	 */
	public static boolean isPlayerInSiegePointZone(Player player, SiegeZone siegeZone) {

		return player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
				&& !TownyAPI.getInstance().hasTownBlock(player.getLocation())
				&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize();
	}

	/**
	 * This method determines if a siege point penalty should be applied
	 *
	 * This method is used in all cases where a resident leaves a siege point zone unexpectedly
	 * 
	 * e.g.
	 * - if the resident dies
	 * - if the resident leaves the town
	 * - if the resident's soldier rank is removed
	 * 
	 * @param resident the player leaving the zone
	 * @param unformattedErrorMessage the error message to be shown if points are deducted
	 * @param possibleTownPenalty true if the town might get a siege point penalty
	 * @param possibleNationPenalty true if the nation might get a siege point penalty
	 * @return true if penalty points were awarded                                
	 */
	public static boolean evaluateSiegePenaltyPoints(Resident resident,
												 String unformattedErrorMessage,
												 boolean possibleTownPenalty,
												 boolean possibleNationPenalty) {
		try {
			Player player = TownyAPI.getInstance().getPlayer(resident);

			if(player == null)
				return false;  //Player not online, or npc

			if(!resident.hasTown())
				return false;

			Town town;
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e) {
				return false;
			}

			if(town.isOccupied() )
				return false;  ///Residents of occupied towns cannot affect siege points.

			//Is the resident a member of a town under siege, and in the death zone ?
			if(possibleTownPenalty
				&& town.hasSiege()
				&& town.getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {

				for(SiegeZone siegeZone: town.getSiege().getSiegeZones().values()) {
					if (player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, unformattedErrorMessage);
						return true;
					}
				}

			} else if (possibleNationPenalty
				&& town.hasNation()) {

				Nation nation = town.getNation();

				//Is the resident a member of a nation which is attacking a town, and in the death zone?
				for(SiegeZone siegeZone: nation.getSiegeZones()) {
					if(siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
						&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

						awardSiegePenaltyPoints(true, siegeZone.getDefendingTown(), resident, siegeZone, unformattedErrorMessage);
						return true;
					}
				}

				//Is the resident a member of a nation which is defending a town, and in the death zone
				for(Town townBeingDefended: nation.getTownsUnderSiegeDefence()) {

					for (SiegeZone siegeZone : townBeingDefended.getSiege().getSiegeZones().values()) {
						if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
							&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

							awardSiegePenaltyPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, unformattedErrorMessage);
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege point penalty");
		}

		return false;
	}

	public static void awardSiegePenaltyPoints(boolean attackerDeath,
											   TownyObject pointsRecipient,
											   Resident deadResident,
											   SiegeZone siegeZone,
											   String unformattedErrorMessage) throws NotRegisteredException {

		//Give siege points to opposing side
		int siegePoints;
		if (attackerDeath) {
			siegePoints = TownySettings.getWarSiegePointsForAttackerDeath();
			siegeZone.adjustSiegePoints(-siegePoints);
		} else {
			siegePoints = TownySettings.getWarSiegePointsForDefenderDeath();
			siegeZone.adjustSiegePoints(siegePoints);
		}

		TownyUniverse.getInstance().getDataSource().saveSiegeZone(siegeZone);

		//Send messages to siege participants
		String message = String.format(
			unformattedErrorMessage,
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
