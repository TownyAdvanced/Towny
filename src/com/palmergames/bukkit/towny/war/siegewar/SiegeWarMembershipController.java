package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;

/**
 * This class intercepts 'remove' requests, where a resident is removed from a town,
 * or a town is removed from a nation.
 *
 * The class evaluates the requests and determines if any siege updates are needed.
 * 
 * @author Goosius
 */
public class SiegeWarMembershipController {

	/**
	 * Evaluates a town removing a resident, and determines if any siege penalty points apply
	 * 
	 * If the resident has guard rank, and the town has a siege, points are awarded.
	 * If the resident has soldier/general rank, and the nation and/or allies have any sieges, points are awarded
	 * 
	 * @param resident The resident who is being removed
	 *  
	 */
	public static void evaluateTownRemoveResident(Town town, Resident resident) {
		SiegeWarPointsUtil.evaluateFighterRemovalPenalty(town, resident, TownySettings.getLangString("msg_siege_war_resident_leave_town"));
	}

	/**
	 * Evaluates a nation removing a town, and determines if any siege penalty points apply
	 *
	 * @param town The town which is being removed
	 *
	 */
	public static void evaluateNationRemoveTown(Town town) {
		for (Resident resident : town.getResidents()) {
			SiegeWarPointsUtil.evaluateFighterRemovalPenalty(town, resident, TownySettings.getLangString("msg_siege_war_town_leave_nation"));
		}
	}

	/**
	 * Evaluates a nation removing an ally, and determines if any siege penalty points apply
	 *
	 * @param ally The ally being removed
	 *
	 */
	public static void evaluateNationRemoveAlly(Nation nation, Nation ally) {
		//Apply penalty to nation for each allied soldier
		for (Resident resident : ally.getResidents()) {
			SiegeWarPointsUtil.evaluateSoldierRemovalPenalty(nation, resident, null, TownySettings.getLangString("msg_siege_war_ally_removed"));
		}
	}
}
