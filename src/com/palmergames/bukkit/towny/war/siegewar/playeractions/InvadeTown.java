package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
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
 * This class is responsible for processing requests to invade towns
 *
 * @author Goosius
 */
public class InvadeTown {

	/**
	 * Process an invade town request
	 *
	 * This method does some final checks and if they pass, the invasion is executed.
	 *
	 * @param plugin the town plugin object
	 * @param player the player who placed the invade banner
	 * @param resident the resident associated with the player object
	 * @param town the town where the banner was placed
	 * @param siege the siege on the town
	 * @param event the place block event
	 */
    public static void processInvadeTownRequest(Towny plugin,
                                                Player player,
                                                Resident resident,
                                                Town town,
                                                Siege siege,
                                                BlockPlaceEvent event) {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();

			if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_INVADE.getNode()))
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

    private static void captureTown(Towny plugin, Siege siege, Nation attackingNation, Town defendingTown) {
        siege.setTownInvaded(true);
		defendingTown.setOccupied(true);
		
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

            TownyMessaging.sendGlobalMessage(String.format(
                    TownySettings.getLangString("msg_siege_war_nation_town_captured"),
                    TownyFormatter.getFormattedTownName(defendingTown),
                    TownyFormatter.getFormattedNationName(nationOfDefendingTown),
                    TownyFormatter.getFormattedNationName(attackingNation)
            ));

            if(nationOfDefendingTown.getTowns().size() == 0) {
                TownyMessaging.sendGlobalMessage(String.format(
                        TownySettings.getLangString("msg_siege_war_nation_defeated"),
                        TownyFormatter.getFormattedNationName(nationOfDefendingTown)
                ));
            }
        } else {
            addTownToNation(plugin, defendingTown, attackingNation);

            TownyMessaging.sendGlobalMessage(String.format(
                    TownySettings.getLangString("msg_siege_war_neutral_town_captured"),
                    TownyFormatter.getFormattedTownName(defendingTown),
                    TownyFormatter.getFormattedNationName(attackingNation)
            ));
        }

		//Save the town to ensure data is saved even if only town/siege was updated
		TownyUniverse.getInstance().getDataSource().saveTown(defendingTown);
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

		TownyUniverse universe = TownyUniverse.getInstance();

		//FYI We use the same sequence of saving here as found in NationCommand.nationLeave()
		if(removeNation) {
			universe.getDataSource().removeNation(nation);
			universe.getDataSource().saveNationList();
        } else {
			universe.getDataSource().saveNation(nation);
			universe.getDataSource().saveNationList();
			plugin.resetCache();
		}

		universe.getDataSource().saveTown(town);
    }

    private static void addTownToNation(Towny plugin, Town town,Nation nation) {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
			nation.addTown(town);
			universe.getDataSource().saveTown(town);
            plugin.resetCache();
			universe.getDataSource().saveNation(nation);
        } catch (AlreadyRegisteredException x) {
            return;   //Town already in nation
        }
    }
}
