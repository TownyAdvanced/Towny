package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.util.TimeMgmt;

/**
 * This class contains utility functions related siege timings
 * 
 * @author Goosius
 */
public class SiegeWarTimeUtil {

	/**
	 * Activate the siege immunity timer after a siege ends
	 * 
	 * While this timer is active, the town cannot be attacked
	 * 
	 * @param town the town
	 * @param siege the siege which was previously in progress
	 */
	public static void activateSiegeImmunityTimer(Town town, Siege siege) {
        double siegeDuration = siege.getActualEndTime() - siege.getStartTime();
        double cooldownDuration = siegeDuration * SiegeWarSettings.getWarSiegeSiegeImmunityTimeModifier();
        town.setSiegeImmunityEndTime(System.currentTimeMillis() + (long)(cooldownDuration + 0.5));
    }

	/**
	 * Activate the revolt immunity timer for a town
	 * 
	 * While this timer is active, the town cannot revolt
	 *
	 * @param town the town
	 */
	public static void activateRevoltImmunityTimer(Town town) {
        long immunityDuration = (long)(SiegeWarSettings.getWarSiegeRevoltImmunityTimeHours() * TimeMgmt.ONE_HOUR_IN_MILLIS);
        town.setRevoltImmunityEndTime(System.currentTimeMillis() + immunityDuration);
    }
}
