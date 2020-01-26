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
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;

import static com.palmergames.util.TimeMgmt.ONE_HOUR_IN_MILLIS;

/**
 * This class is responsible for processing requests to surrender towns
 *
 * @author Goosius
 */
public class SurrenderTown {

	/**
	 * Process a surrender town request
	 * 
	 * This method does some final checks and if they pass, the surrender is executed.
	 * 
	 * @param player the player who placed the surrender banner
	 * @param townWhereBlockWasPlaced the town where the banner was placed
	 * @param event the place block event
	 */
    public static void processTownSurrenderRequest(Player player,
                                                   Town townWhereBlockWasPlaced,
                                                   BlockPlaceEvent event) {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Resident resident = universe.getDataSource().getResident(player.getName());
            if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_action_not_a_town_member"));

			Town townOfAttackingResident = resident.getTown();
			if(townOfAttackingResident != townWhereBlockWasPlaced)
                throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_surrender_not_your_town"));
			
			if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_TOWN_SIEGE_SURRENDER.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

            Siege siege = townWhereBlockWasPlaced.getSiege();
            if(siege.getStatus() != SiegeStatus.IN_PROGRESS)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_surrender_siege_finished"));

            if(siege.getSiegeZones().size() > 1)
                throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_surrender_multiple_attackers"));

            long timeUntilSurrenderIsAllowedMillis = siege.getTimeUntilSurrenderIsAllowedMillis();
            if(timeUntilSurrenderIsAllowedMillis > 0) {
				String message = String.format(TownySettings.getLangString("msg_err_siege_war_cannot_surrender_yet"), 
					TimeMgmt.getFormattedTimeValue(timeUntilSurrenderIsAllowedMillis));
				throw new TownyException(message);
			}
            
            //Surrender
            defenderSurrender(siege);

        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
			event.setBuild(false);
			event.setCancelled(true);
        }
    }

    private static void defenderSurrender(Siege siege) {
    	Nation winnerNation = new ArrayList<>(siege.getSiegeZones().keySet()).get(0);
    	
        SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege,
                                            SiegeStatus.DEFENDER_SURRENDER,
											winnerNation);

        TownyMessaging.sendGlobalMessage(String.format(
        	TownySettings.getLangString("msg_siege_war_town_surrender"),
			TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
			TownyFormatter.getFormattedNationName(siege.getAttackerWinner())));

		SiegeWarMoneyUtil.giveWarChestsToWinnerNation(siege, siege.getAttackerWinner());
    }
}
