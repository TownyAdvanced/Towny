package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
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

    public static void processPlunderTownRequest(Player player, String townName, BlockPlaceEvent event) {
        try {
			if(!TownySettings.isUsingEconomy())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_economy"));

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_PLUNDER.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

            if(!TownyUniverse.getDataSource().hasTown(townName))
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), townName));

            if(!TownyUniverse.getDataSource().getTown(townName).hasSiege())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_no_siege_on_target_town"), townName));

            final Siege siege = TownyUniverse.getDataSource().getTown(townName).getSiege();

            if(siege.getStatus() == SiegeStatus.IN_PROGRESS)
                throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));

            if(siege.getStatus() == SiegeStatus.DEFENDER_WIN)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));

            if(siege.getStatus() == SiegeStatus.ATTACKER_ABANDON)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));

            Resident resident = TownyUniverse.getDataSource().getResident(player.getName());

            if(!resident.hasTown()
                    || !resident.hasNation()
                    || resident.getTown().getNation() != siege.getAttackerWinner()) {
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_plunder_without_victory"));
            }

            if(siege.isTownPlundered())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_plundered"), townName));

            plunderTown(siege, siege.getDefendingTown(), siege.getAttackerWinner());
        } catch (TownyException x) {
            event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    public static void plunderTown(Siege siege, Town defendingTown, Nation winnerNation) {
        siege.setTownPlundered(true);

        double plunder =
                TownySettings.getWarSiegeAttackerPlunderAmountPerPlot()
                        * defendingTown.getTownBlocks().size();
        try {
            if (defendingTown.canPayFromHoldings(plunder)) {
                defendingTown.payTo(plunder, winnerNation, "Town was plundered by attacker");
                sendPlunderSuccessMessage(defendingTown, winnerNation);
            } else {
                double actualPlunder = defendingTown.getHoldingBalance();
                defendingTown.payTo(actualPlunder, winnerNation, "Town was plundered by attacker");
                sendPlunderSuccessMessage(defendingTown, winnerNation);
                TownyMessaging.sendGlobalMessage(
                	String.format(
						TownySettings.getLangString("msg_siege_war_town_ruined_from_plunder"),
						TownyFormatter.getFormattedNationName(winnerNation),
						TownyFormatter.getFormattedTownName(defendingTown)));
                TownyUniverse.getDataSource().removeTown(defendingTown);
            }
        } catch (EconomyException x) {
            TownyMessaging.sendErrorMsg(x.getMessage());
        }
    }

    private static void sendPlunderSuccessMessage(Town defendingTown, Nation winnerNation) {
        //Same messages for now but may diverge in future (if we decide to track the original nation of the town)
    	if(defendingTown.hasNation()) {
			TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
					TownySettings.getLangString("msg_siege_war_nation_town_plundered"),
					TownyFormatter.getFormattedTownName(defendingTown),
					TownyFormatter.getFormattedNationName(winnerNation)
			)));
        } else {
            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_neutral_town_plundered"),
                    TownyFormatter.getFormattedTownName(defendingTown),
                    TownyFormatter.getFormattedNationName(winnerNation))));
        }
    }

}
