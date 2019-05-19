package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

/**
 * Created by Anonymoose on 19/05/2019.
 */
public class SiegeWarUtil {

    public static boolean isPlayerWithinWarzone(Player player, Town town) {
        if(!town.hasHomeBlock())
            return true; //If town has no homeblock, all town is the warzone

        int warZoneRadius = TownySettings.getWarSiegeWarzoneRadius();

        try {
            //Player is too far north
            if (player.getLocation().getBlockZ() < town.getHomeBlock().getZ() - warZoneRadius)
                return false;

            //Player is too far south
            if (player.getLocation().getBlockZ() > town.getHomeBlock().getZ() + warZoneRadius)
                return false;

            //Player is too far east
            if (player.getLocation().getBlockX() > town.getHomeBlock().getX() + warZoneRadius)
                return false;

            //Player is too far west
            if (player.getLocation().getBlockX() < town.getHomeBlock().getX() - warZoneRadius)
                return false;

        } catch (TownyException x) {
            //We won't get here because we have checked for the homeblock earlier
        }

        return true; //Player is in the warzone
    }
}
