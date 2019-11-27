package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDbUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Goosius
 */
public class AbandonAttack {

    public static void processAbandonSiegeRequest(Player player,
                                                  Block block,
                                                  BlockPlaceEvent event)  {
        try {
            Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
            if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_town_member"));

            Town townOfResident = resident.getTown();
            if(!townOfResident.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_nation_member"));

            //If player has no permission to abandon,send error
            if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ABANDON.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
            
            //Find the nearest siege zone to the player,from IN_PROGRESS sieges
            SiegeZone nearestSiegeZone = null;
            double distanceToNearestSiegeZone = -1;
            for(SiegeZone siegeZone: com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getSiegeZones()) {
                
            	if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
            		continue;
            		
            	if (nearestSiegeZone == null) {
					nearestSiegeZone = siegeZone;
                    distanceToNearestSiegeZone = block.getLocation().distance(nearestSiegeZone.getFlagLocation());
                } else {
                    double distanceToNewTarget = block.getLocation().distance(siegeZone.getFlagLocation());
                    if(distanceToNewTarget < distanceToNearestSiegeZone) {
						nearestSiegeZone = siegeZone;
                        distanceToNearestSiegeZone = distanceToNewTarget;
                    }
                }
            }
            
            //If there are no in-progress sieges at all, error
			if(nearestSiegeZone == null)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_no_nearby_siege_attacks"));
			
			//If the player is too far from the nearest zone, error
			if(distanceToNearestSiegeZone > TownySettings.getTownBlockSize())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_no_nearby_siege_attacks"));

			//If the player's nation is not the attacker, send error
            Nation nationOfResident = townOfResident.getNation();
            if(nearestSiegeZone.getAttackingNation() != nationOfResident)
                throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_nation_not_attacking_zone"));
            
            attackerAbandon(nearestSiegeZone);

        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
            event.setCancelled(true);
        }
    }

    private static void attackerAbandon(SiegeZone siegeZone) {
        //Here we simply remove the siege zone
		TownyUniverse.getDataSource().removeSiegeZone(siegeZone);
        
		TownyMessaging.sendGlobalMessage(
			String.format(TownySettings.getLangString("msg_siege_war_attacker_abandon"),
				TownyFormatter.getFormattedNationName(siegeZone.getAttackingNation()),
        		TownyFormatter.getFormattedTownName(siegeZone.getDefendingTown())));
		
        if (siegeZone.getSiege().getSiegeZones().size() == 0) {
            SiegeWarDbUtil.updateAndSaveSiegeCompletionValues(siegeZone.getSiege(),
                    SiegeStatus.ATTACKER_ABANDON,
                    null);
			TownyMessaging.sendGlobalMessage(
				String.format(TownySettings.getLangString("msg_siege_war_siege_abandon"),
					TownyFormatter.getFormattedTownName(siegeZone.getDefendingTown())));
		}
    }
}
