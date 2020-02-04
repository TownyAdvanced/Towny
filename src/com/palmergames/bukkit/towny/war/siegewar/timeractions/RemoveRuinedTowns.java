package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only remove ruined town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName())) {
				if(town.isRuined()) {
					if(town.getRecentlyRuinedEndTime() != 999) {
						//Prepare to delete in next cycle. 999 is just an arbitrary number to signify delete
						town.setRecentlyRuinedEndTime(999);
						townyUniverse.getDataSource().saveTown(town);
					} else {
						townyUniverse.getDataSource().removeRuinedTown(town);
					}
				}
			} 
		}
    }
    
}
