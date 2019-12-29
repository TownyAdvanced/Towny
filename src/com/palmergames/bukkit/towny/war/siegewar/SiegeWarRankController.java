package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarPointsUtil;

/**
 * This class intercepts 'remove rank' requests, where a resident's rank is removed.
 *
 * The class evaluates the requests and determines if any siege updates are needed.
 * 
 * @author Goosius
 */
public class SiegeWarRankController {

	/**
	 * Evaluates a town rank being removed
	 * 
	 * If the resident is a guard in a siegezone which involves the town, siege points are deducted
	 *
	 * @param resident The affected resident
	 * @param rank The rank being removed                   
	 *  
	 */
	public static void evaluateTownRemoveRank(Resident resident, String rank) {
		if(TownyPerms.getTownRank(rank).contains(PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(
				resident,
				TownySettings.getLangString("msg_siege_war_resident_town_rank_removed"),
				true,
				false);
		}
	}
	
	/**
	 * Evaluates a nation rank being removed
	 *
	 * If the resident is a soldier in a siegezone which involves the nation, siege points are deducted
	 *
	 * @param resident The affected resident
	 * @param rank The rank being removed                   
	 *
	 */
	public static void evaluateNationRemoveRank(Resident resident, String rank) {
		if(TownyPerms.getNationRank(rank).contains(PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {
			SiegeWarPointsUtil.evaluateSiegePenaltyPoints(
				resident,
				TownySettings.getLangString("msg_siege_war_resident_nation_rank_removed"),
				false,
				true);
		}
	}
	
}
