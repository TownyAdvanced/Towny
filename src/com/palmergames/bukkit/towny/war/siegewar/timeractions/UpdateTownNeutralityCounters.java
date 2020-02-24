package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class is responsible for updating the neutrality counters of all towns
 *
 * @author Goosius
 */
public class UpdateTownNeutralityCounters {

	/**
	 * This method adjust the neutrality counters of all towns, where required
	 */
	public static void updateTownNeutralityCounters() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only adjust neutrality counter for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) && !town.isRuined())
				updateTownNeutralityCounter(town);
		}
    }

	public static void updateTownNeutralityCounter(Town town) {
		if(town.getNeutralityChangeConfirmationCounterDays() != 0) {
			town.decrementNeutralityChangeConfirmationCounterDays();
			
			if(town.getNeutralityChangeConfirmationCounterDays() == 0) {
				town.flipNeutral();
			
				if(town.isNeutral()) {
					TownyMessaging.sendGlobalMessage(
						String.format(TownySettings.getLangString("msg_siege_war_town_became_neutral"), 
						town.getFormattedName()));
				} else {
					TownyMessaging.sendGlobalMessage(
						String.format(TownySettings.getLangString("msg_siege_war_town_became_non_neutral"),
						town.getFormattedName()));
				}
			}
		}
	}
}
