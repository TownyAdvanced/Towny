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
	 * @param deadResident The resident who died
	 * @param killerResident The resident who did the killing
	 *  
	 */
	public static void evaluateSiegePvPDeath(Player deadPlayer, Resident deadResident, Resident killerResident)  {
		
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
			if (deadResidentTown.hasSiege()
				&& killerResidentTown.hasNation()
				&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
				&& deadResidentTown.getSiege().getSiegeZones().containsKey(killerResidentTown.getNation())) {

				SiegeZone siegeZone = deadResidentTown.getSiege().getSiegeZones().get(killerResidentTown.getNation());

				//Did the death occur in the siege death point zone?
				if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
					awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
				}
			}

			if(deadResidentTown.hasNation()) {
				Nation deadResidentNation = deadResidentTown.getNation();

				//Was the dead player a member of a besieging nation, killed by a siege defender, in the siege death point zone ?
				if (killerResidentTown.hasSiege()
					&& killerResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& killerResidentTown.getSiege().getSiegeZones().containsKey(deadResidentNation)) {

					SiegeZone siegeZone = killerResidentTown.getSiege().getSiegeZones().get(deadResidentNation);

					//Did the death occur in the siege death point zone?
					if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						awardSiegeDeathPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone);
					}
				}

				//Was the dead player a member of a nation under attack, killed by a siege attacker, in the death points zone ?
				if(killerResidentTown.hasNation()) {
					for (SiegeZone siegeZone : killerResidentTown.getNation().getSiegeZones()) {
						if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
							&& siegeZone.getDefendingTown() != deadResidentTown	//already evaluated above
							&& siegeZone.getDefendingTown().hasNation()
							&& siegeZone.getDefendingTown().getNation() == deadResidentNation) {

							//Did the death occur in the siege death point zone?
							if (deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
								awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);
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
