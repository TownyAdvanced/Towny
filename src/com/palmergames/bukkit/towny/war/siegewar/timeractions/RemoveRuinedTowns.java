package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;

/**
 * This class is responsible for removing ruined towns completely
 *
 * @author Goosius
 */
public class RemoveRuinedTowns {

	/**
	 * This method cycles through all towns
	 * It determines which towns have lain in ruins for long enough, and deletes them.
	 */
    public static void removeRuinedTowns() {
		TownyUniverse universe = TownyUniverse.getInstance();
        for (Town town : new ArrayList<>(universe.getDataSource().getTowns())) {
            if(town.isRuined() && System.currentTimeMillis() > town.getRecentlyRuinedEndTime()) {
				universe.getDataSource().removeRuinedTown(town);
            }
        }
    }
}
