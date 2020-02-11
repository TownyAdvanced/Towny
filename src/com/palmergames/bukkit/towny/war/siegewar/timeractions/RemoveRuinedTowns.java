package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
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

				try {
					if (town.getRecentlyRuinedEndTime() == 999 || town.getNumResidents() == 0) {
						//phase 2 ruins, or legacy ruins. Remove now.
						townyUniverse.getDataSource().removeTown(town, false);
						continue;
					}

					if (town.getRecentlyRuinedEndTime() == 888) {
						//Town is in phase 1 ruined state. Wait
						town.setRecentlyRuinedEndTime(999);
						townyUniverse.getDataSource().saveTown(town);
						continue;
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg("Problem removing ruined town " + town.getName());
					e.printStackTrace();
				}
			} 
		}
    }

}
