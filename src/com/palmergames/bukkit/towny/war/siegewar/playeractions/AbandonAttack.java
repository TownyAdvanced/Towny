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
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This class is responsible for processing requests to Abandon siege attacks
 *
 * @author Goosius
 */
public class AbandonAttack {

	/**
	 * Process an abandon attack request
	 *
	 * This method does some final checks and if they pass, the abandon is executed
	 *
	 * @param player the player who placed the abandon banner
	 * @param siegeZone the siege zone
	 * @param event the place block event
	 */
    public static void processAbandonSiegeRequest(Player player, 
												  SiegeZone siegeZone,
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
            if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_ABANDON.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
            
            //If the siege is not in progress, send error
			Siege siege = siegeZone.getSiege();
			if (siege.getStatus() != SiegeStatus.IN_PROGRESS)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_siege_over"));
			
			//If the player's nation does not own the nearby siegezone, send error
            if(siegeZone.getAttackingNation() != townOfResident.getNation())
                throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_nation_not_attacking_zone"));

			long timeUntilAbandonIsAllowedMillis = siege.getTimeUntilAbandonIsAllowedMillis();
			if(timeUntilAbandonIsAllowedMillis > 0) {
				String message = String.format(TownySettings.getLangString("msg_err_siege_war_cannot_abandon_yet"),
					TimeMgmt.getFormattedTimeValue(timeUntilAbandonIsAllowedMillis));
				throw new TownyException(message);
			}

			attackerAbandon(siegeZone);

        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
			event.setBuild(false);
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

		SiegeWarMoneyUtil.giveOneWarChestToWinnerTown(siegeZone, siegeZone.getDefendingTown());
    }
}
