package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.Siege;
import com.palmergames.bukkit.towny.war.siegewar.SiegeType;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anonymoose on 19/05/2019.
 */
public class SiegeWarUtil {

    public final static long ONE_MINUTE_IN_MILLIS = 60000;
    public final static long ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;


    //////////////////// PROCESS ATTACK REQUESTS //////////////////////////////////////


    public static void attemptToStartSiege(Player player) {

        try {
            if (!TownySettings.getWarSiegeEnabled())
                throw new TownyException("Siege war feature disabled");  //todo - replace w lang string

            if (!TownySettings.getWarSiegeAllowSieges())
                throw new TownyException("Sieges not allowed");

            if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ASSAULT_START.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

            TownBlock townBlockWherePlayerIsLocated = TownyUniverse.getTownBlockWherePlayerIsLocated(player);
            if (townBlockWherePlayerIsLocated == null)
                throw new TownyException("You must be standing in a town to start a siege.");

            if(!isTownBlockOnTheTownBorder(townBlockWherePlayerIsLocated))
                throw new TownyException("You must be in a town border block to start a siege.");

            Town defendingTown = townBlockWherePlayerIsLocated.getTown();
            Nation nationOfAttackingPlayer = TownyUniverse.getNationOfPlayer(player);

            if (defendingTown.hasNation()) {
                Nation nationOfDefendingTown = defendingTown.getNation();

                if(nationOfAttackingPlayer == nationOfDefendingTown)
                    throw new TownyException("You cannot attack a town in your own nation.");

                if (!nationOfAttackingPlayer.hasEnemy(nationOfDefendingTown))
                    throw new TownyException("You cannot attack a town unless the nation of that town is an enemy of your nation.");
            }

            if (nationOfAttackingPlayer.isNationAttackingTownNow(defendingTown))
                throw new TownyException("Your nation is already attacking this town.");

            if (nationOfAttackingPlayer.hasNationAttackedTownRecently(defendingTown))
                throw new TownyException("Your nation has recently attacked this town, you must wait until after the next siege cooldown before attacking again");

            if (defendingTown.isSiegeCooldownActive()) {
                throw new TownyException(
                        "This town is in a siege cooldown period. " +
                                "It cannot be attack for " +
                                defendingTown.getAssaultSiegeCooldownRemainingMinutes() + " minutes");
            }

            if (TownySettings.isUsingEconomy()) {
                double initialSiegeCost =
                        TownySettings.getWarSiegeAttackerCostUpFrontPerPlot()
                                * defendingTown.getTownBlocks().size();

                if (nationOfAttackingPlayer.canPayFromHoldings(initialSiegeCost))
                    //Deduct upfront cost
                    nationOfAttackingPlayer.pay(initialSiegeCost, "Cost of Initiating an assault siege.");
                else {
                    throw new TownyException(TownySettings.getLangString("msg_err_no_money."));
                }
            }

            if (player.isFlying())
                throw new TownyException("You cannot be flying to start a siege.");

            if (doesPlayerHaveANonAirBlockAboveThem(player))
                throw new TownyException("The god(s) favour wars on the land surface. You must have only sky above you to start a siege.");

            if (TownySettings.getNationRequiresProximity() > 0) {
                Coord capitalCoord = nationOfAttackingPlayer.getCapital().getHomeBlock().getCoord();
                Coord townCoord = defendingTown.getHomeBlock().getCoord();
                if (!nationOfAttackingPlayer.getCapital().getHomeBlock().getWorld().getName().equals(defendingTown.getHomeBlock().getWorld().getName())) {
                    throw new TownyException("This town cannot join your nation because the capital of your your nation is in a different world.");
                }
                double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
                if (distance > TownySettings.getNationRequiresProximity()) {
                    throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), defendingTown.getName()));
                }
            }

            if (TownySettings.getMaxTownsPerNation() > 0) {
                if (nationOfAttackingPlayer.getTowns().size() >= TownySettings.getMaxTownsPerNation()){
                    throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_over_town_limit"), TownySettings.getMaxTownsPerNation()));
                }
            }

            //Setup Siege
            newSiege(nationOfAttackingPlayer, defendingTown);

        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        } catch (EconomyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    private static Siege newSiege(Nation attackingNation,
                           Town defendingTown) throws TownyException {
        //Setup the siege
        TownyUniverse.getDataSource().newSiege(attackingNation, defendingTown);
        Siege siege = TownyUniverse.getDataSource().getSiege(attackingNation, defendingTown);

        //Add siege to nation
        attackingNation.addSiege(siege);

        //Add siege to town
        defendingTown.addSiege(siege);

        TownyMessaging.sendGlobalMessage(
                TownyFormatter.getFormattedNationName(siege.getAttackingNation()) +
                        " has started a siege on " +
                        TownyFormatter.getFormattedTownName(siege.getDefendingTown()));

        //Save the siege to DB
        TownyUniverse.getDataSource().saveNation(attackingNation);
        TownyUniverse.getDataSource().saveTown(defendingTown);
        TownyUniverse.getDataSource().saveSiege(siege);
        TownyUniverse.getDataSource().saveSiegeList();

        //BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));
        //TODO - Do we announce a new siege event like this???

        return siege;
    }

    /////////////////////PROCESS REVOLT REQUESTS ///////////////////////////////////

    public static void attemptToRevolt(Towny plugin, Player player) {

        try {
            if (!TownySettings.getWarSiegeEnabled())
                throw new TownyException("Siege war disabled");  //todo - replace w lang string

            if (!TownySettings.getWarSiegeAllowRevolts())
                throw new TownyException("Siege war revolts are not allowed");

            if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SIEGE_REVOLT_START.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

            TownBlock townBlockWherePlayerIsLocated = TownyUniverse.getTownBlockWherePlayerIsLocated(player);
            if (townBlockWherePlayerIsLocated == null)
                throw new TownyException("You must be standing in your town to start a revolt.");

            Town defendingTown = townBlockWherePlayerIsLocated.getTown();
            Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
            if(!(defendingTown == resident.getTown())) {
                throw new TownyException("You cannot start a revolt in a town other than your own.");
            }

            if(!defendingTown.hasNation()) {
                throw new TownyException("Your town is not ruled by any nation. You have nobody to revolt against.");
            }

            if (defendingTown.isRevoltCooldownActive()) {
                throw new TownyException(
                        "This town is in a revolt cooldown period. " +
                                "It cannot revolt for " +
                                defendingTown.getRevoltSiegeCooldownRemainingMinutes() + " minutes");
            }

            revolt(plugin, resident, defendingTown);

        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }

    private static void revolt(Towny plugin, Resident resident, Town town) {
        try {
            Nation nation = town.getNation();
            removeTownFromNation(plugin, town, nation);

            TownyMessaging.sendGlobalMessage(
                    String.format(
                            TownySettings.getLangString("msg_siege_war_revolt"),
                            TownyFormatter.getFormattedTownName(town),
                            TownyFormatter.getFormattedResidentName(resident),
                            TownyFormatter.getFormattedNationName(town.getNation())));

            //Turn OFF siege cooldown
            town.setSiegeCooldownEndTime(System.currentTimeMillis());

            //Tell town that siege cooldown has been reset to off
            TownyMessaging.sendTownMessage(town, TownySettings.getLangString("msg_siege_war_post_revolt_siege_cooldown_reset"));

            //Turn ON revolt cooldown
            long revoltCooldownDurationMillis = TownySettings.getWarSiegeRevoltCooldownHours() * ONE_HOUR_IN_MILLIS;
            long revoltCooldownEndTime= System.currentTimeMillis() + revoltCooldownDurationMillis;
            town.setRevoltCooldownEndTime(revoltCooldownEndTime);

            if(nation.getTowns().size() == 0) {
                TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                        TownySettings.getLangString("msg_siege_war_nation_defeated"),
                        TownyFormatter.getFormattedNationName(nation)
                )));
            }

        } catch (NotRegisteredException x) {
            //We shouldn't get here as we already checked for nation
        }
    }


    ///////////////////PROCESS ONGOING SIEGE ACTIVITY //////////////////////

    public static boolean isPlayerWithinSiegeZone(Player player,
                                                  Town town) {
        return isWorldCoordWithinSiegeZone(WorldCoord.parseWorldCoord(player), town);
    }


    public static boolean isTownBlockWithinSiegeZone(TownBlock townBlock,
                                                     Town town) {

        return isWorldCoordWithinSiegeZone(townBlock.getWorldCoord(), town);
    }


    public static boolean isWorldCoordWithinSiegeZone(WorldCoord worldCoord,
                                                      Town town)  {
        if(!town.hasHomeBlock())
            return false;

        TownBlock homeBlock = null;
        try {
            homeBlock = town.getHomeBlock();
        } catch (TownyException x) {
            //We won't get here as we returned earlier if there was no homeblock.
        }

        int siegeZoneRadiusInTownBlocks = TownySettings.getWarSiegeZoneDistanceFromTown();

        //Player is too far north
        if (worldCoord.getZ() < homeBlock.getZ() - siegeZoneRadiusInTownBlocks)
            return false;

        //Player is too far south
        if (worldCoord.getZ() > homeBlock.getZ() + siegeZoneRadiusInTownBlocks)
            return false;

        //Player is too far east
        if (worldCoord.getX() > homeBlock.getX() + siegeZoneRadiusInTownBlocks)
            return false;

        //Player is too far west
        if (worldCoord.getX() < homeBlock.getX() - siegeZoneRadiusInTownBlocks)
            return false;

        return true; //Co-ordinate is within the siegezone
    }

    public static boolean isTownBlockOnTheTownBorder(TownBlock townBlock) {
        WorldCoord worldCoord = townBlock.getWorldCoord();

        int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int i = 0; i < 4; i++)
            try {
                TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
                boolean sameTown = edgeTownBlock.getTown() == townBlock.getTown();
                if (!sameTown)
                    return true; //If the adjacent plot is in a different town, return true
            } catch (NotRegisteredException e) {
                return true;  //If the adjacent plot is not in a town, return true
            }
        return false;
    }

    public static boolean doesPlayerHaveANonAirBlockAboveThem(Player player) {
        Location loc = player.getEyeLocation().add(0,1,0);

        while(loc.getY() < 256)
        {
            if(loc.getBlock().getType() != Material.AIR)
            {
                return true;   //There is a non-air block above them
            }
            loc.add(0,1,0);
        }
        return false;  //There is nothing but air above them
    }


    public static void captureTown(Towny plugin, Siege siege) {
        if(siege.getDefendingTown().hasNation()) {

            Nation nationOfCapturedTown = null;
            try {
                nationOfCapturedTown = siege.getDefendingTown().getNation();
            } catch (NotRegisteredException x) {
                //This won't happen because we checked for a nation just above
            }

            removeTownFromNation(plugin, siege.getDefendingTown(), nationOfCapturedTown);

            addTownToNation(plugin, siege.getDefendingTown(), siege.getAttackingNation());

            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_nation_town_captured"),
                    TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
                    TownyFormatter.getFormattedNationName(nationOfCapturedTown),
                    TownyFormatter.getFormattedNationName(siege.getAttackingNation())
            )));

            if(nationOfCapturedTown.getTowns().size() == 0) {
                TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                        TownySettings.getLangString("msg_siege_war_nation_defeated"),
                        TownyFormatter.getFormattedNationName(nationOfCapturedTown)
                )));
            }
        } else {
            addTownToNation(plugin, siege.getDefendingTown(), siege.getAttackingNation());

            TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                    TownySettings.getLangString("msg_siege_war_neutral_town_captured"),
                    TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
                    TownyFormatter.getFormattedNationName(siege.getAttackingNation())
            )));
        }
    }

    public static void removeTownFromNation(Towny plugin, Town town, Nation nation) {
        boolean removeNation = false;

        try {
            nation.removeTown(town);
        } catch(NotRegisteredException x) {
            TownyMessaging.sendErrorMsg("Attempted to remove town from nation but Town was already removed.");
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


    public static void plunderTown(Siege siege) {

        if (TownySettings.isUsingEconomy()) {
            double plunder =
                    TownySettings.getWarSiegeAttackerPlunderAmountPerPlot()
                            * siege.getDefendingTown().getTownBlocks().size();

            try {
                if (siege.getDefendingTown().canPayFromHoldings(plunder))
                    siege.getDefendingTown().pay(plunder, "Town was plundered by attacker");
                else {
                    TownyMessaging.sendGlobalMessage("The town " + siege.getDefendingTown().getName() + " was destroyed by " +siege.getAttackingNation().getName());
                    TownyUniverse.getDataSource().removeTown(siege.getDefendingTown());
                }
            } catch (EconomyException x) {
                TownyMessaging.sendErrorMsg(x.getMessage());
            }
        }
    }

    public static void applyUpkeepCost(Siege siege, double upkeepCost) {
        try {
            if (siege.getAttackingNation().canPayFromHoldings(upkeepCost))
                siege.getAttackingNation().pay(upkeepCost, "Cost of maintaining siege.");
            else {
                TownyMessaging.sendGlobalMessage(siege.getAttackingNation().getName() +
                        " cannot afford to continue the siege on " + siege.getDefendingTown().getName() + "." +
                        "The siege has been automatically abandoned.");
                siege.setComplete(true);
            }
        } catch (EconomyException x) {
            TownyMessaging.sendErrorMsg(x.getMessage());
        }
    }


    public static void attackerWin(Towny plugin, Siege siege) {
        captureTown(plugin, siege);
        if (TownySettings.isUsingEconomy()) {
            plunderTown(siege);
        }
    }


    public static void defenderWin(Siege siege) {
        TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                TownySettings.getLangString("msg_siege_war_defender_win"),
                TownyFormatter.getFormattedTownName(siege.getDefendingTown()),
                TownyFormatter.getFormattedNationName(siege.getAttackingNation())
        )));
    }


    public static void checkForCompletionOfAllRecentTownSieges(Siege siege) {

        //If all sieges on the town are now complete,
        //activate cooldowns and clear the recent sieges list.
        if(siege.getDefendingTown().areAllSiegesComplete()) {

            //Activate siege cooldown
            long totalDurationOfRecentSiegesMillis = siege.getDefendingTown().getTotalDurationOfRecentSiegesMillis();
            long cooldownDurationMillis = totalDurationOfRecentSiegesMillis * TownySettings.getWarSiegeSiegeCooldownModifier();
            long cooldownEndTimeMillis= System.currentTimeMillis() + cooldownDurationMillis;
            siege.getDefendingTown().setSiegeCooldownEndTime(cooldownEndTimeMillis);

            //Remove recent sieges
            for(Siege siegeToDelete: new ArrayList<>(siege.getDefendingTown().getSieges())) {
                TownyUniverse.getDataSource().removeSiege(siegeToDelete);
            }

        }
    }

}
