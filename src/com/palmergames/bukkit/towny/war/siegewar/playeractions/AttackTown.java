package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockPlacingUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.util.TimeMgmt;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * @author Goosius
 */
public class AttackTown {

    public static void processAttackTownRequest(Player player,
                                                Block block,
                                                List<TownBlock> nearbyTownBlocks,
                                                BlockPlaceEvent event) {

        try {
            if (!TownySettings.getWarSiegeAttackEnabled())
                throw new TownyException("Siege Attacks not allowed");

            Resident attackingResident = TownyUniverse.getDataSource().getResident(player.getName());
            if(!attackingResident.hasTown())
                throw new TownyException("You must belong to a town to start a siege");

            Town townOfAttackingResident = attackingResident.getTown();
            if(!townOfAttackingResident.hasNation())
                throw new TownyException("You must belong to a nation to start a siege");

            Town defendingTown = nearbyTownBlocks.get(0).getTown();
            if(townOfAttackingResident == defendingTown)
                throw new TownyException("You cannot attack your own town");

            Nation nationOfAttackingPlayer= townOfAttackingResident.getNation();
            if (defendingTown.hasNation()) {
                Nation nationOfDefendingTown = defendingTown.getNation();

                if(nationOfAttackingPlayer == nationOfDefendingTown)
                    throw new TownyException("You cannot attack a town in your own nation.");

                if (!nationOfAttackingPlayer.hasEnemy(nationOfDefendingTown))
                    throw new TownyException("You cannot attack a town unless the nation of that town is an enemy of your nation.");
            }


            if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ATTACK.getNode()))
                throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

            if (nearbyTownBlocks.size() > 1)
                throw new TownyException("To start a siege attack, " +
                        "the wilderness plot containing the banner must be facing just one town plot. " +
                        "Try a different location for the banner");

            if (nationOfAttackingPlayer.isNationAttackingTown(defendingTown))
                throw new TownyException("Your nation is already attacking this town.");

            if (defendingTown.isSiegeCooldownActive()) {
                throw new TownyException(
                        "This town is in a siege cooldown period. It cannot be attacked for " +
                                defendingTown.getFormattedHoursUntilSiegeImmunityEnds() + " hours");
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

            //THE FOLLOWING MATTER FOR POINTS BUT NOT FOR STARTINGSIEGE
            //if (player.isFlying())
            //    throw new TownyException("You cannot be flying to start a siege.");

            //if(player.getPotionEffect(PotionEffectType.INVISIBILITY) != null)
            //  throw new TownyException("The god(s) favour the brave. You cannot be invisible to start a siege");

            if (SiegeWarBlockPlacingUtil.doesBlockHaveANonAirBlockAboveIt(block))
                throw new TownyException("The god(s) favour wars on the land surface. You must place the banner where there is only sky above it.");

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

            if(defendingTown.isRuined())
                throw new TownyException("You cannot attack a ruined town.");

            //Setup attack
            attackTown(block, nationOfAttackingPlayer, defendingTown);
        } catch (TownyException x) {
            TownyMessaging.sendErrorMsg(player, x.getMessage());
            event.setCancelled(true);
        } catch (EconomyException x) {
            event.setCancelled(true);
            TownyMessaging.sendErrorMsg(player, x.getMessage());
        }
    }


    public static void attackTown(Block block,
                                  Nation attackingNation,
                                    Town defendingTown) throws TownyException {

        Siege siege;
        SiegeZone siegeZone;
        boolean newSiege;

        if(!defendingTown.hasSiege()) {
            newSiege = true;

            //Create Siege
            siege = new Siege(defendingTown);
            siege.setStatus(SiegeStatus.IN_PROGRESS);
            siege.setTownPlundered(false);
            siege.setTownInvaded(false);
            siege.setAttackerWinner(null);
            siege.setStartTime(System.currentTimeMillis());
            siege.setScheduledEndTime(
                    (System.currentTimeMillis() +
                    ((long)(TownySettings.getWarSiegeMaxHoldoutTimeHours() * TimeMgmt.ONE_HOUR_IN_MILLIS))));
            siege.setActualEndTime(0);
            defendingTown.setSiege(siege);

        } else {
            //Get Siege
            newSiege = false;
            siege = defendingTown.getSiege();
        }

        //Create siege zone
        TownyUniverse.getDataSource().newSiegeZone(
                attackingNation.getName(),
                defendingTown.getName());
        siegeZone = TownyUniverse.getDataSource().getSiegeZone(
                attackingNation.getName(),
                defendingTown.getName());
        siegeZone.setActive(true);

        siegeZone.setFlagLocation(block.getLocation());
        siege.getSiegeZones().put(attackingNation, siegeZone);
        attackingNation.addSiegeZone(siegeZone);

        //Save siegezone, siege, nation, and town to DB
        TownyUniverse.getDataSource().saveSiegeZone(siegeZone);
        TownyUniverse.getDataSource().saveNation(attackingNation);
        TownyUniverse.getDataSource().saveTown(defendingTown);
        TownyUniverse.getDataSource().saveSiegeZoneList();

        //Send global message;
        if(newSiege) {
            TownyMessaging.sendGlobalMessage(
                    "The nation of " + attackingNation.getName() +
                            " has initiated a siege on the town of " + defendingTown.getName());
        } else {
            TownyMessaging.sendGlobalMessage(
                    "The nation of " + attackingNation.getName() +
                            " has joined the siege on the town of " + defendingTown.getName());
        }


        //BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));
        //TODO - Do we announce a new siege event like this???
    }

}
