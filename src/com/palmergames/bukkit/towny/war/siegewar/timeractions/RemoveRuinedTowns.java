package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;

/**
 * @author Goosius
 */
public class RemoveRuinedTowns {

    public static void removeRuinedTowns() {
		TownyUniverse universe = TownyUniverse.getInstance();
        for (Town town : new ArrayList<>(universe.getDataSource().getTowns())) {
            if(town.isRuined() && System.currentTimeMillis() > town.getRecentlyRuinedEndTime()) {
				universe.getDataSource().removeRuinedTown(town);
            }
        }
    }
}
