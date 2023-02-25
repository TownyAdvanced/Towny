package com.palmergames.bukkit.towny.utils;


import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;

public class OutpostUtil {
	
	/** 
	 * Checks to make sure all requirements are met before an outpost can be claimed.
	 * Will check:
	 *   - If there are not enough free outpost plots
	 *   - If the Outpost does not meet a minimum-distance requirement
	 * 
	 * @param town - Town the outpost belongs to
	 * @param resident - Resident establishing the outpost
	 * @param world - TownyWorld in which the outpost will be in
	 * @param key - Coordinates where the outpost would be established
	 * @param isAdmin - If the Resident is a Towny Administrator
	 * @param isPlotSetOutpost - If a plot is already an outpost
	 *    
	 * @return - Returns true if all required tests for outposts are met.
	 * @throws TownyException if a condition is not met.
	 */
	public static boolean OutpostTests(Town town, Resident resident, TownyWorld world, Coord key, boolean isAdmin, boolean isPlotSetOutpost) throws TownyException {

		// The config can be set up to dole out numbers of outposts to towns based on resident counts/belonging to a nation.
		if (TownySettings.isOutpostsLimitedByLevels() && (town.getMaxOutpostSpawn() >= town.getOutpostLimit()))
			throw new TownyException(Translatable.of("msg_err_not_enough_outposts_free_to_claim", town.getMaxOutpostSpawn(), town.getOutpostLimit()));

		// The config can be set to require a number of residents in a town before an outpost can be made.
		if (TownySettings.getAmountOfResidentsForOutpost() != 0 && town.getResidents().size() < TownySettings.getAmountOfResidentsForOutpost())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents"));

		// Outposts can be limited per resident, with permission nodes.
		int maxOutposts = TownySettings.getMaxResidentOutposts(resident);
		if (!isAdmin && maxOutposts != -1 && (maxOutposts <= town.getAllOutpostSpawns().size()))
			throw new TownyException(Translatable.of("msg_max_outposts_own", maxOutposts));

		// Outposts can have a minimum required distance from homeblocks. 
		if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

		int maxDistance = TownySettings.getMaxDistanceForOutpostsFromTown();
		// Outposts can have a maximum distance they can be from their town's plots.
		if (maxDistance > 0) {
			// Doesn't match the world.
			if (!world.getName().equalsIgnoreCase(town.getHomeblockWorld().getName()))
				throw new TownyException(Translatable.of("msg_err_you_can_only_claim_outposts_in_your_homeblocks_world"));
			
			int distance = world.getMinDistanceFromOtherPlotsOwnedByTown(key, town);
			// Is too far from the nearest townblock.
			if (distance > maxDistance)
				throw new TownyException(Translatable.of("msg_err_not_close_enough_to_your_town_nearest_plot", distance, maxDistance));
		}
		// Outposts can have a minimum required distance from other towns' townblocks.
		int minDistance = world.getMinDistanceFromOtherTownsPlots(key, isPlotSetOutpost ? town : null);
		// Outposts can have a minimum required distance from other outposts.
		if (minDistance < TownySettings.getMinDistanceFromTownPlotblocks() ||
			minDistance < TownySettings.getMinDistanceForOutpostsFromPlot())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));

		return true;		
	}

}
