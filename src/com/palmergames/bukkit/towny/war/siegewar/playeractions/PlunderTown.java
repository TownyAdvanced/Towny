package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author Goosius
 */
public class PlunderTown {

    public static void processPlunderTownRequest(Player player,
												 Resident resident,
												 Town town,
												 Siege siege,
												 BlockPlaceEvent event) {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
			
			if(!TownySettings.isUsingEconomy())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_economy"));

			if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_PLUNDER.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(resident.getTown() == siege.getDefendingTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_own_town"));
			
			if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));
			
			if(resident.getTown().getNation() != siege.getAttackerWinner())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));
			
            if(siege.isTownPlundered())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_plundered"), town));

            plunderTown(siege, town, siege.getAttackerWinner());
            
        } catch (TownyException x) {
            event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    public static void plunderTown(Siege siege, Town defendingTown, Nation winnerNation) {
        siege.setTownPlundered(true);

        double fullPlunderAmount =
                TownySettings.getWarSiegeAttackerPlunderAmountPerPlot()
                        * defendingTown.getTownBlocks().size();
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
			
			if (defendingTown.canPayFromHoldings(fullPlunderAmount)) {
                defendingTown.payTo(fullPlunderAmount, winnerNation, "Town was plundered by attacker");
                sendPlunderSuccessMessage(defendingTown, winnerNation, fullPlunderAmount);
				universe.getDataSource().saveTown(defendingTown);
            } else {
                double actualPlunderAmount = defendingTown.getHoldingBalance();
                defendingTown.payTo(actualPlunderAmount, winnerNation, "Town was plundered by attacker");
                sendPlunderSuccessMessage(defendingTown, winnerNation, actualPlunderAmount);
                TownyMessaging.sendGlobalMessage(
                	String.format(
						TownySettings.getLangString("msg_siege_war_town_ruined_from_plunder"),
						TownyFormatter.getFormattedTownName(defendingTown),
						TownyFormatter.getFormattedNationName(winnerNation)));
				universe.getDataSource().removeTown(defendingTown);
            }
        } catch (EconomyException x) {
            TownyMessaging.sendErrorMsg(x.getMessage());
        }
    }

    private static void sendPlunderSuccessMessage(Town defendingTown, Nation winnerNation, double plunderAmount) {
        //Same messages for now but may diverge in future (if we decide to track the original nation of the town)
    	if(defendingTown.hasNation()) {
			TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
					TownySettings.getLangString("msg_siege_war_nation_town_plundered"),
					TownyFormatter.getFormattedTownName(defendingTown),
				TownyEconomyHandler.getFormattedBalance(plunderAmount),
				TownyFormatter.getFormattedNationName(winnerNation)
			)));
        } else {
            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_neutral_town_plundered"),
                    TownyFormatter.getFormattedTownName(defendingTown),
   				    TownyEconomyHandler.getFormattedBalance(plunderAmount),
                    TownyFormatter.getFormattedNationName(winnerNation))));
        }
    }

}
