package com.palmergames.bukkit.towny.utils;


import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class OutpostUtil {
	
	/**
	 * Checks to make sure all requirements are met before an outpost can be
	 * claimed. Will check: <br>
	 * 
	 * - If there are not enough free outpost plots<br>
	 * - If the Outpost does not meet a minimum-distance requirement
	 * 
	 * @param town     Town the outpost belongs to
	 * @param resident Resident establishing the outpost
	 * @param world    TownyWorld in which the outpost will be in
	 * @param key      Coordinates where the outpost would be established
	 * @param isAdmin  If the Resident is a Towny Administrator
	 * 
	 * @return - Returns true if all required tests for outposts are met.
	 * @throws TownyException if a condition is not met.
	 */
	public static boolean OutpostTests(Town town, Resident resident, TownyWorld world, Coord key, boolean isAdmin) throws TownyException {

		// The config can be set up to dole out numbers of outposts to towns based on resident counts/belonging to a nation.
		if (TownySettings.isOutpostsLimitedByLevels() && (town.getMaxOutpostSpawn() >= town.getOutpostLimit()))
			throw new TownyException(Translatable.of("msg_err_not_enough_outposts_free_to_claim", town.getMaxOutpostSpawn(), town.getOutpostLimit()));

		// The config can be set to require a number of residents in a town before an outpost can be made.
		if (!TownySettings.isOutpostsLimitedByLevels() &&
			TownySettings.getAmountOfResidentsForOutpost() != 0 &&
			town.getResidents().size() < TownySettings.getAmountOfResidentsForOutpost())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents"));

		// Outposts can be limited per resident, with permission nodes.
		int maxOutposts = TownySettings.getMaxResidentOutposts(resident);
		if (!isAdmin && maxOutposts != -1 && (maxOutposts <= town.getAllOutpostSpawns().size()))
			throw new TownyException(Translatable.of("msg_max_outposts_own", maxOutposts));

		// Outposts can have a minimum required distance from homeblocks. 
		if (world.getMinDistanceFromOtherTownsHomeBlocks(key) < TownySettings.getMinDistanceFromTownHomeblocks())
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
		int minDistance = world.getMinDistanceFromOtherTownsPlots(key, town);
		// Outposts can have a minimum required distance from other outposts.
		if (minDistance < TownySettings.getMinDistanceFromTownPlotblocks() ||
			minDistance < TownySettings.getMinDistanceForOutpostsFromPlot())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));

		return true;		
	}

	public static void addOutpostComponent(Town town, StatusScreen screen, Translator translator) {
		String outpostLine = "";
		if (TownySettings.isOutpostsLimitedByLevels()) {
			outpostLine = TownyFormatter.colourKeyValue(translator.of("status_town_outposts"), translator.of("status_fractions", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
			if (town.hasNation()) {
				int nationBonus = town.getNationOrNull().getNationLevel().nationBonusOutpostLimit();
				if (nationBonus > 0)
					outpostLine += TownyFormatter.colourBracketElement(translator.of("status_town_size_nationbonus"), String.valueOf(nationBonus));
			}
		} else if (town.hasOutpostSpawn()) {
			outpostLine = TownyFormatter.colourKeyValue(translator.of("status_town_outposts"), String.valueOf(town.getMaxOutpostSpawn()));
		}
		screen.addComponentOf("outposts", outpostLine,
				HoverEvent.showText(translator.component("status_hover_click_for_more")),
				ClickEvent.runCommand("/towny:town outpost list"));
	}
}
