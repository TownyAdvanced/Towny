package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

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
	 * @param townToBeInvaded the town to be invaded
	 * @param event the place block event
	 */
    public static void processInvadeTownRequest(Towny plugin,
                                                Player player,
                                                Town townToBeInvaded,
                                                BlockPlaceEvent event) {
        try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Resident resident = universe.getDataSource().getResident(player.getName());
			Town townOfInvadingResident = resident.getTown();

			if (!universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_INVADE.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(townOfInvadingResident == townToBeInvaded)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_own_town"));

			Siege siege = townToBeInvaded.getSiege();
			if (siege.getStatus() != SiegeStatus.ATTACKER_WIN && siege.getStatus() != SiegeStatus.DEFENDER_SURRENDER)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_without_victory"));

			Nation nationOfInvadingResident = townOfInvadingResident.getNation();
			Nation attackerWinner = siege.getAttackerWinner();
			
			if (nationOfInvadingResident != attackerWinner)
				throw new TownyException(TownySettings.getLangString("msg_err_siege_war_cannot_invade_without_victory"));

            if (siege.isTownInvaded())
                throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_invaded"), townToBeInvaded.getName()));

			if(townToBeInvaded.hasNation() && townToBeInvaded.getNation() == attackerWinner)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_town_already_belongs_to_your_nation"), townToBeInvaded.getName()));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = attackerWinner.getCapital().getHomeBlock().getCoord();
				Coord townCoord = townToBeInvaded.getHomeBlock().getCoord();
				if (!attackerWinner.getCapital().getHomeBlock().getWorld().getName().equals(townToBeInvaded.getHomeBlock().getWorld().getName())) {
					throw new TownyException(TownySettings.getLangString("msg_err_nation_homeblock_in_another_world"));
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), townToBeInvaded.getName()));
				}
			}

			if (TownySettings.getMaxTownsPerNation() > 0) {
				if (attackerWinner.getTowns().size() >= TownySettings.getMaxTownsPerNation()){
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_over_town_limit"), TownySettings.getMaxTownsPerNation()));
				}
			}

			captureTown(plugin, siege, attackerWinner, townToBeInvaded);

        } catch (TownyException x) {
			event.setBuild(false);
			event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    private static void captureTown(Towny plugin, Siege siege, Nation attackingNation, Town defendingTown) {
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

		defendingTown.setOccupied(true);

		//Save the town to ensure data is saved even if only town/siege was updated
		TownyUniverse.getInstance().getDataSource().saveTown(defendingTown);
    }

    private static void removeTownFromNation(Towny plugin, Town town, Nation nation) {
        boolean removeNation = false;
        Resident king = nation.getKing();

        try {
            nation.removeTown(town);
        } catch(NotRegisteredException x) {
            return;  //Town was already removed
        } catch(EmptyNationException x) {
            removeNation = true;  //Set flag to remove nation at end of this method
        }

		TownyUniverse universe = TownyUniverse.getInstance();

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
