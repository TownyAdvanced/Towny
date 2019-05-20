package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;

/**
 * Created by Anonymoose on 19/05/2019.
 */
public class SiegeWarUtil {

    public static boolean isPlayerWithinWarzone(TownBlock townBlockWherePlayerIs,
                                                Town town) {
        TownBlock homeBlock = null;

        if(!town.hasHomeBlock()) {
            return true; //If town has no homeblock, all town is the warzone
        } else {
            try {
                homeBlock = town.getHomeBlock();
            } catch (Exception e) {
                //We won't get here because we already checked for the homeblock
            }
        }

        int warZoneRadiusTownBlocks = TownySettings.getWarSiegeWarzoneRadiusTownBlocks();

        //Player is too far north
        if (townBlockWherePlayerIs.getZ() < homeBlock.getZ() - warZoneRadiusTownBlocks)
            return false;

        //Player is too far south
        if (townBlockWherePlayerIs.getZ() > homeBlock.getZ() + warZoneRadiusTownBlocks)
            return false;

        //Player is too far east
        if (townBlockWherePlayerIs.getX() > homeBlock.getX() + warZoneRadiusTownBlocks)
            return false;

        //Player is too far west
        if (townBlockWherePlayerIs.getX() < homeBlock.getX() - warZoneRadiusTownBlocks)
            return false;

        return true; //Player is in the warzone
    }
}
