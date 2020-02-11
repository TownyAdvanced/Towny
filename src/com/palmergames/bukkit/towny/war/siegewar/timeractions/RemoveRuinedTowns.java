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
    public static void deleteRuinedTowns() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only delete ruined town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName())) {

				if(town.isRuined() && System.currentTimeMillis() > town.getRecentlyRuinedEndTime()) {
					try {
						//Ruin found & recently ruined end time reached. Delete town now.
						townyUniverse.getDataSource().removeTown(town, false);
					} catch (Exception e){
						TownyMessaging.sendErrorMsg("Problem deleting ruined town " + town.getName());
						e.printStackTrace();
					}
				}
			} 
		}
    }

}
