package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
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
	 * Evaluates a town rank being removed, and determines if a siege point penalty applies
	 * 
	 * @param resident The affected resident
	 * @param rank The rank being removed                   
	 *  
	 */
	public static void evaluateTownRemoveRank(Resident resident, String rank) {
		if(TownySettings.getWarSiegePenaltyPointsEnabled())
			return;

		TownyUniverse universe = TownyUniverse.getInstance();
		if(universe.getPermissionSource().doesTownRankAllowPermissionNode(rank, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS)) {

			Town town ;
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e) { return; }

			SiegeWarPointsUtil.evaluateGuardRemovalPenalty(town, resident, TownySettings.getLangString("msg_siege_war_resident_town_rank_removed"));
		}
	}

	/**
	 * Evaluates a nation rank being removed, and determines if a siege point penalty applies
	 *
	 * @param resident The affected resident
	 * @param rank The rank being removed                   
	 *
	 */
	public static void evaluateNationRemoveRank(Resident resident, String rank) {
		if(TownySettings.getWarSiegePenaltyPointsEnabled())
			return;

		TownyUniverse universe = TownyUniverse.getInstance();

		if(universe.getPermissionSource().doesNationRankAllowPermissionNode(rank, PermissionNodes.TOWNY_NATION_SIEGE_POINTS)) {
			Nation nation;
			try {
				 nation = resident.getTown().getNation();
			} catch (NotRegisteredException e) { return; }

			//Penalty for nation
			SiegeWarPointsUtil.evaluateSoldierRemovalPenalty(
				nation, 
				resident,
				null, 
				TownySettings.getLangString("msg_siege_war_resident_nation_rank_removed"));

			//Penalty for to mutual allies
			for(Nation alliedNation: nation.getMutualAllies()) {
				SiegeWarPointsUtil.evaluateSoldierRemovalPenalty(
					alliedNation,
					resident,
					null,
					TownySettings.getLangString("msg_siege_war_resident_nation_rank_removed"));
			}
		}
	}
}