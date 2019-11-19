package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayList;

/**
 * @author Goosius
 */
public class RemoveRuinedTowns {

    public static void removeRuinedTowns() {
        for (Town town : new ArrayList<>(TownyUniverse.getDataSource().getTowns())) {
            if(town.isRuined() && System.currentTimeMillis() > town.getRecentlyRuinedEndTime()) {
                TownyUniverse.getDataSource().removeRuinedTown(town);
            }
        }
    }
}
