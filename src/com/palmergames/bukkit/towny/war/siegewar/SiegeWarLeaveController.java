package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;

/**
 * This class intercepts 'leave' requests, where a resident leaves a town or a town leaves a nation.
 *
 * The class evaluates the requests and determines if any siege updates are needed.
 * 
 * @author Goosius
 */
public class SiegeWarLeaveController {

	/**
	 * Evaluates a town removing a resident
	 * 
	 * If the resident is in a siegezone which involves the town, siege points are deducted
	 *
	 * @param town The town which is removing the resident
	 * @param resident The resident who is being removed
	 *  
	 */
	public static void evaluateTownRemoveResident(Town town, Resident resident)  {

		try {
			Player player = TownyAPI.getInstance().getPlayer(resident);
			
		    if(player == null)
		    	return;  //Player not online, or npc
			
			//Residents of occupied towns cannot affect siege points.
			if(town.isOccupied() )
				return;

			//Is the resident a member of a town under siege, and in the death zone ?
			if(town.hasSiege()
				&& town.getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {

				for(SiegeZone siegeZone: town.getSiege().getSiegeZones().values()) {
					if (player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
						SiegeWarDeathController.awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, TownySettings.getLangString("msg_siege_war_resident_leave_town"));
					}	
				}
			}
			
			if(town.hasNation()) {
				Nation nation = town.getNation();
				
				//Is the resident a member of a nation which is attacking a town, and in the death zone?
				for(SiegeZone siegeZone: nation.getSiegeZones()) {
					if(siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					  	&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) { 
						
						SiegeWarDeathController.awardSiegeDeathPoints(true, siegeZone.getDefendingTown(), resident, siegeZone, TownySettings.getLangString("msg_siege_war_resident_leave_town"));
					}
				}
				
				//Is the resident a member of a nation which is defending a town, and in the death zone
				for(Town townBeingDefended: nation.getTownsUnderSiegeDefence()) {
					
					if(townBeingDefended == town)
						continue;  //do not evaluate own town, it was done above
						
					for (SiegeZone siegeZone : townBeingDefended.getSiege().getSiegeZones().values()) {
						if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
							&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

							SiegeWarDeathController.awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, TownySettings.getLangString("msg_siege_war_resident_leave_town"));
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege leave");
		}
	}
	
	/**
	 * Evaluates a nation removing a town
	 *
	 * If any town members are in siegezones which involves the nation, siege points are deducted
	 *
	 * @param nation the nation which is removing the town
	 * @param town The town which is being removed
	 *
	 */
	public static void evaluateNationRemoveTown(Nation nation, Town town)  {

		try {
			//Occupied towns cannot affect siege points.
			if(town.isOccupied() )
				return;

			//Check all residents
			Player player;
			for(Resident resident: town.getResidents()) {
				player = TownyAPI.getInstance().getPlayer(resident);

				if(player == null)
					return;  //Player not online, or npc

				//Is the player in the death zone of any siege attacks ?
				for(SiegeZone siegeZone: nation.getSiegeZones()) {
					if(siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {
						if (player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {
							SiegeWarDeathController.awardSiegeDeathPoints(true, siegeZone.getDefendingTown(), resident, siegeZone, TownySettings.getLangString("msg_siege_war_town_leave_nation"));
						}	
					}
				}
				
				//Is the player in the death zone of any siege defences ?
				for(Town townBeingDefended: nation.getTownsUnderSiegeDefence()) {

					if(townBeingDefended == town)
						continue;  //do not evaluate own town, as resident is still involved in siege there

					for (SiegeZone siegeZone : townBeingDefended.getSiege().getSiegeZones().values()) {
						if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
							&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getWarSiegeZoneDeathRadiusBlocks()) {

							SiegeWarDeathController.awardSiegeDeathPoints(false, siegeZone.getAttackingNation(), resident, siegeZone, TownySettings.getLangString("msg_siege_war_town_leave_nation"));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege leave");
		}
	}

}
