package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.util.TimeMgmt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Goosius
 */
public class SiegeWarUtil {

    public static boolean doesPlayerHaveANonAirBlockAboveThem(Player player) {
        return doesLocationHaveANonAirBlockAboveIt(player.getLocation());
    }

    public static boolean doesBlockHaveANonAirBlockAboveIt(Block block) {
        return doesLocationHaveANonAirBlockAboveIt(block.getLocation());
    }

    private static boolean doesLocationHaveANonAirBlockAboveIt(Location location) {
        location = location.add(0,1,0);

        while(location.getY() < 256)
        {
            if(location.getBlock().getType() != Material.AIR)
            {
                return true;   //There is a non-air block above them
            }
            location.add(0,1,0);
        }
        return false;  //There is nothing but air above them
    }

    public static void updateAndSaveSiegeCompletionValues(Siege siege,
                                                           SiegeStatus siegeStatus,
                                                           Nation winnerNation) {
        siege.setStatus(siegeStatus);
        siege.setActualEndTime(System.currentTimeMillis());
        siege.setAttackerWinner(winnerNation);
        siege.setAllSiegeZonesToInactive();
        activateSiegeImmunityTimer(siege.getDefendingTown());

        //Save to db
        TownyUniverse.getDataSource().saveTown(siege.getDefendingTown());
        for(SiegeZone siegeZone: siege.getSiegeZones().values()) {
            TownyUniverse.getDataSource().saveSiegeZone(siegeZone);
        }
    }

    private static void activateSiegeImmunityTimer(Town town) {
        double siegeDuration = town.getSiege().getActualEndTime() - town.getSiege().getStartTime();
        double cooldownDuration = siegeDuration * TownySettings.getWarSiegeSiegeImmunityTimeModifier();
        town.setSiegeImmunityEndTime(System.currentTimeMillis() + (long)(cooldownDuration + 0.5));
    }

    public static void activateRevoltImmunityTimer(Town town) {
        long immunityDuration = (long)(TownySettings.getWarSiegeRevoltImmunityTimeHours() * TimeMgmt.ONE_HOUR_IN_MILLIS);
        town.setRevoltImmunityEndTime(System.currentTimeMillis() + immunityDuration);
    }

    public static TownyObject calculateSiegeWinner(Siege siege) {
        //If all siege zones are negative points, defender wins
        //Otherwise, the siege zone attacker with the highest points wins

        TownyObject winner = siege.getDefendingTown();
        int winningPoints = 0;

        for(Map.Entry<Nation,SiegeZone> entry: siege.getSiegeZones().entrySet()) {
            if(entry.getValue().isActive()
                && entry.getValue().getSiegePoints() > winningPoints) {
                winner = entry.getKey();
                winningPoints = entry.getValue().getSiegePoints();
            }
        }

        return winner;
    }

    public static Siege getActiveSiegeGivenBannerLocation(Location location) {
        //Look through all siege zones
        //Note that we don't just look at the town at the given location
        //....because mayor may have unclaimed the plot after the siege started

        //Location must match
        //Siege must be in progress
        //Siege zone must be active
        for (SiegeZone siegeZone : TownyUniverse.getDataSource().getSiegeZones()) {
            if (siegeZone.getFlagLocation().equals(location)) {
                if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS && siegeZone.isActive()) {
                    return siegeZone.getSiege();
                } else {
                    return null;
                }
            }

    }
        //No siege banner found at the given location
        return null;
    }

    public static List<Siege> getAllSieges() {
        List<Siege> result = new ArrayList<>();
        for(SiegeZone siegeZone: TownyUniverse.getDataSource().getSiegeZones()) {
            if(!result.contains(siegeZone.getSiege())) {
                result.add(siegeZone.getSiege());
            }
        }
        return result;
    }

    //Must be in same world as flag
    //Must be within 1 townblock length of flag
    //Must be in wilderness
    public static boolean isPlayerInSiegePointZone(Player player, SiegeZone siegeZone) {

        if (player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
                && player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize()) {

            TownBlock townBlock = TownyUniverse.getTownBlock(player.getLocation());
            if (townBlock == null) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


}
