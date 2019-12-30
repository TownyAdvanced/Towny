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
	 * @param resident The resident who is being removed
	 *  
	 */
	public static void evaluateTownRemoveResident(Resident resident) {
		SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_resident_leave_town"));
	}
	
	/**
	 * Evaluates a nation removing a town, and determines if any siege penalty points apply
	 *
	 * @param town The town which is being removed
	 *
	 */
	public static void evaluateNationRemoveTown(Town town) {
		for (Resident resident : town.getResidents()) {
				SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_town_leave_nation"));
		}
	}

	/**
	 * Evaluates a nation removing an ally, and determines if any siege penalty points apply
	 *
	 * @param ally The ally being removed
	 * 
	 */
	public static void evaluateNationRemoveAlly(Nation nation, Nation ally) {
		for (Resident resident : nation.getResidents()) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_ally_removed"));
		}
		for (Resident resident : ally.getResidents()) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_ally_removed"));
		}
	}

	/**
	 * Evaluates two nations forming a new alliance, and determines if any siege penalty points apply
	 *
	 * @param ally The ally being added
	 *
	 */
	public static void evaluateNationsFormNewAlliance(Nation nation, Nation ally) {
		for (Resident resident : nation.getResidents()) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_new_alliance_formed"));
		}
		for (Resident resident : ally.getResidents()) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(resident, TownySettings.getLangString("msg_siege_war_new_alliance_formed"));
		}
	}
}
