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
            if(town.isRuined()) {
            	if(town.getRecentlyRuinedEndTime() != 999) {
            		//Prepare to delete in next cycle. 999 is just an arbitrary number to signify delete
					town.setRecentlyRuinedEndTime(999);
					universe.getDataSource().saveTown(town);
				} else {
					universe.getDataSource().removeRuinedTown(town);
				}
            }
        }
    }
}
