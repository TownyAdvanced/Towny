package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Goosius
 */
public class InvadeTown {

    public static void processInvadeTownRequest(Towny plugin,
                                                Player player,
                                                Resident resident,
                                                Town town,
                                                Siege siege,
                                                BlockPlaceEvent event) {
        try {
        	
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_INVADE.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(resident.getTown() == siege.getDefendingTown())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_own_town"));

			if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_without_victory"));
			
            if (resident.getTown().getNation() != siege.getAttackerWinner())
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_without_victory"));

            if (siege.isTownInvaded())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_invaded"), town.getName()));

			Nation attackerWinner = siege.getAttackerWinner();
			
			if(town.hasNation() && town.getNation() == attackerWinner)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_belongs_to_your_nation"), town.getName()));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = attackerWinner.getCapital().getHomeBlock().getCoord();
				Coord townCoord = town.getHomeBlock().getCoord();
				if (!attackerWinner.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), town.getName()));
				}
				double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), town.getName()));
				}
			}

			if (TownySettings.getMaxTownsPerNation() > 0) {
				if (attackerWinner.getTowns().size() >= TownySettings.getMaxTownsPerNation()){
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_over_town_limit"), TownySettings.getMaxTownsPerNation()));
				}
			}

			captureTown(plugin, siege, attackerWinner, town);

        } catch (TownyException x) {
            event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    public static void captureTown(Towny plugin, Siege siege, Nation attackingNation, Town defendingTown) {
        siege.setTownInvaded(true);
		
        //Reset revolt immunity, to prevent immediate revolt after invasion 
        SiegeWarTimeUtil.activateRevoltImmunityTimer(defendingTown);
		
        if(defendingTown.hasNation()) {
            Nation nationOfDefendingTown = null;
            try {
                nationOfDefendingTown = defendingTown.getNation();
            } catch (NotRegisteredException x) {
            }

            //Remove town from nation (and nation itself if empty)
            removeTownFromNation(plugin, defendingTown, nationOfDefendingTown);

            addTownToNation(plugin, defendingTown, attackingNation);

            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_nation_town_captured"),
                    TownyFormatter.getFormattedTownName(defendingTown),
                    TownyFormatter.getFormattedNationName(nationOfDefendingTown),
                    TownyFormatter.getFormattedNationName(attackingNation)
            )));

            if(nationOfDefendingTown.getTowns().size() == 0) {
                TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                        TownySettings.getLangString("msg_siege_war_nation_defeated"),
                        TownyFormatter.getFormattedNationName(nationOfDefendingTown)
                )));
            }
        } else {
            addTownToNation(plugin, defendingTown, attackingNation);

            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_neutral_town_captured"),
                    TownyFormatter.getFormattedTownName(defendingTown),
                    TownyFormatter.getFormattedNationName(attackingNation)
            )));
        }
    }

    private static void removeTownFromNation(Towny plugin, Town town, Nation nation) {
        boolean removeNation = false;

        try {
            nation.removeTown(town);
        } catch(NotRegisteredException x) {
            return;  //Town was already removed
        } catch(EmptyNationException x) {
            removeNation = true;  //Set flag to remove nation at end of this method
        }
        /*
         * Remove all resident titles/nationRanks before saving the town itself.
         */
        List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

        for (Resident res : titleRemove) {
            if (res.hasTitle() || res.hasSurname()) {
                res.setTitle("");
                res.setSurname("");
            }
            res.updatePermsForNationRemoval(); // Clears the nationRanks.
            TownyUniverse.getDataSource().saveResident(res);
        }

        if(removeNation) {
            TownyUniverse.getDataSource().removeNation(nation);
            TownyUniverse.getDataSource().saveNationList();
        } else {
            TownyUniverse.getDataSource().saveNation(nation);
            TownyUniverse.getDataSource().saveNationList();
            plugin.resetCache();
        }

        TownyUniverse.getDataSource().saveTown(town);
    }

    private static void addTownToNation(Towny plugin, Town town,Nation nation) {
        try {
            nation.addTown(town);
            TownyUniverse.getDataSource().saveTown(town);
            plugin.resetCache();
            TownyUniverse.getDataSource().saveNation(nation);
        } catch (AlreadyRegisteredException x) {
            return;   //Town already in nation
        }
    }
}
