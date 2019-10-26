package com.palmergames.bukkit.towny.utils;


import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class OutpostUtil {
	
	/** 
	 * @return - Returns true if all required tests for outposts are met.
	 * @throws TownyException 
	 */
	public static boolean OutpostTests(Town town, Resident resident, TownyWorld world, Coord key, boolean isAdmin) throws TownyException {

		// The config can be set up to dole out numbers of outposts to towns based on resident counts/belonging to a nation.
		if (TownySettings.isOutpostsLimitedByLevels() && (town.getMaxOutpostSpawn() >= town.getOutpostLimit()))
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_enough_outposts_free_to_claim"), town.getMaxOutpostSpawn(), town.getOutpostLimit()));

		// The config can be set to require a number of residents in a town before an outpost can be made.
		if (TownySettings.getAmountOfResidentsForOutpost() != 0 && town.getResidents().size() < TownySettings.getAmountOfResidentsForOutpost())
			throw new TownyException(TownySettings.getLangString("msg_err_not_enough_residents"));

		// Outposts can be limited per resident, with permission nodes.
		int maxOutposts = TownySettings.getMaxResidentOutposts(resident);
		if (!isAdmin && maxOutposts != -1 && (maxOutposts <= resident.getTown().getAllOutpostSpawns().size()))
			throw new TownyException(String.format(TownySettings.getLangString("msg_max_outposts_own"), maxOutposts));

		// Outposts can have a minimum required distance from homeblocks. 
		if (world.getMinDistanceFromOtherTowns(key) < TownySettings.getMinDistanceFromTownHomeblocks())
			throw new TownyException(String.format(TownySettings.getLangString("msg_too_close2"), TownySettings.getLangString("homeblock")));

		// Outposts can have a minimum required distance from other towns' townblocks.
		if (world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceFromTownPlotblocks())
			throw new TownyException(String.format(TownySettings.getLangString("msg_too_close2"), TownySettings.getLangString("townblock")));

		// Outposts can have a minimum required distance from other outposts.
		if (world.getMinDistanceFromOtherTownsPlots(key) < TownySettings.getMinDistanceForOutpostsFromPlot())
			throw new TownyException(String.format(TownySettings.getLangString("msg_too_close2"), TownySettings.getLangString("outpost")));

		return true;		
	}

}
