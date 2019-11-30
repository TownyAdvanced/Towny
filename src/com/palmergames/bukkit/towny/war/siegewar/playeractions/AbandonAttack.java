package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author Goosius
 */
public class AbandonAttack {

    public static void processAbandonSiegeRequest(Player player, 
												  SiegeZone nearestSiegeZone,
												  BlockPlaceEvent event)  {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
            Resident resident = universe.getDataSource().getResident(player.getName());
            if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_town_member"));

            Town townOfResident = resident.getTown();
            if(!townOfResident.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_nation_member"));

            //If player has no permission to abandon,send error
            if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ABANDON.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
            
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
		TownyUniverse universe = TownyUniverse.getInstance();
		universe.getDataSource().removeSiegeZone(siegeZone);
        
		TownyMessaging.sendGlobalMessage(
			String.format(TownySettings.getLangString("msg_siege_war_attacker_abandon"),
				TownyFormatter.getFormattedNationName(siegeZone.getAttackingNation()),
        		TownyFormatter.getFormattedTownName(siegeZone.getDefendingTown())));
		
        if (siegeZone.getSiege().getSiegeZones().size() == 0) {
            SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siegeZone.getSiege(),
                    SiegeStatus.ATTACKER_ABANDON,
                    null);
			TownyMessaging.sendGlobalMessage(
				String.format(TownySettings.getLangString("msg_siege_war_siege_abandon"),
					TownyFormatter.getFormattedTownName(siegeZone.getDefendingTown())));
		}
    }
}
