package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.BiomeUtil;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

public class AreaSelectionUtil {
	
	/**  A maximum radius of 15 will garner 961 townblocks. Capped to prevent servers from dying. */
	private final static int MAX_RECT_RADIUS = 15;
	/** A maximum radius of 18 will garner 1009 townblocks. Capped to prevent servers from dying. */
	private final static int MAX_CIRC_RADIUS = 18;
	/** The largest we ever want to select at once. */
	private final static int MAX_SIZE = 1009;

	/**
	 * Method to select a List&lt;WorldCoord&gt; of coordinates. 
	 * Area claims can be either circular or rectangular.
	 * 
	 * @param owner - {@link com.palmergames.bukkit.towny.object.TownBlockOwner} making the selection. 
	 * @param pos - WorldCoord where the selection is being made from.
	 * @param args - Subcommands like rect, circle, auto or #.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid subcommands are given.
	 */
	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {
		return selectWorldCoordArea(owner, pos, args, false);
	}

	/**
	 * Method to select a List&lt;WorldCoord&gt; of coordinates. 
	 * Area claims can be either circular or rectangular.
	 * 
	 * @param owner - {@link com.palmergames.bukkit.towny.object.TownBlockOwner} making the selection. 
	 * @param pos - WorldCoord where the selection is being made from.
	 * @param args - Subcommands like rect, circle, auto or #.
	 * @param claim - This selection will result in claiming for a resident or town.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid subcommands are given.
	 */
	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args, boolean claim) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();

		if (args.length == 0) {
			/*
			 * Either /{command} {claim|unclaim}
			 */
			out.add(pos);

		} else {

			/*
			 * This will determine how many townblocks we can select for. We only really set
			 * a limit if any version of the "auto" commands are used.
			 */
			int maxSelectionSize = claim ? getSelectionSize(args, owner) : MAX_SIZE;

			/*
			 * Handle different subcommands for /{command} {claim|unclaim} {rect|circle|auto|#} {#}
			 */
			if (args.length > 1) { // Has to be /{command} {claim|unclaim} {rect|circle} {auto|#}
				if (args[0].equalsIgnoreCase("rect")) {
					out = selectWorldCoordAreaRect(maxSelectionSize, pos, StringMgmt.remFirstArg(args), claim);
				} else if (args[0].equalsIgnoreCase("circle")) {
					out = selectWorldCoordAreaCircle(maxSelectionSize, pos, StringMgmt.remFirstArg(args), claim);
				} else if (args.length == 3 && args[1].startsWith("x") && args[2].startsWith("z")) {
					// "/plot claim world x# z#" was run by clicking on the towny map.
					out.add(new WorldCoord(args[0], Integer.parseInt(args[1].replace("x","")), Integer.parseInt(args[2].replace("z",""))));
				} else {
					throw new TownyException(Translatable.of("msg_err_invalid_property", StringMgmt.join(args, " ")));
				}
			} else if (args[0].equalsIgnoreCase("auto")) { // Is /{command} {claim|unclaim} {auto}
				out = selectWorldCoordAreaRect(maxSelectionSize, pos, args, claim);
			} else { // Is /{command} {claim|unclaim} #
				try {
					Integer.parseInt(args[0]);
					// Treat as rect to serve for backwards capability.
					out = selectWorldCoordAreaRect(maxSelectionSize, pos, args, claim);
				} catch (NumberFormatException e) {
					throw new TownyException(Translatable.of("msg_err_invalid_property", args[0]));
				}
			}
		}

		return out;
	}

	/**
	 * Returns the maximum selection size. We are only limiting it when a version of
	 * the "auto" command is being used.
	 * 
	 * @param args subcommands.
	 * @param owner TownBlockOwner which is doing the claiming.
	 * @return MAX_SIZE or whatever the owner is able to claim.
	 */
	private static int getSelectionSize(String[] args, TownBlockOwner owner) {
		if (args.length == 1 && args[0].equalsIgnoreCase("auto") || 
				args.length == 2 && args[1].equalsIgnoreCase("auto"))
			return getAvailableClaimsFrom(owner);

		return MAX_SIZE;
	}

	/**
	 * Returns what the town or resident is allowed to claim.
	 * @param owner TownBlockOwner, either a town or a resident.
	 * @return number of plots they're allowed to claim.
	 */
	private static int getAvailableClaimsFrom(TownBlockOwner owner) {
		if (owner instanceof Town town)
			return town.hasUnlimitedClaims() ? 1009 : town.availableTownBlocks();

		if (owner instanceof Resident resident)
			return TownySettings.getMaxResidentPlots(resident) - resident.getTownBlocks().size();

		return 0;
	}

	/**
	 * Selects a square shaped area of WorldCoords. Works in a spiral out fashion.
	 * 
	 * @param maxSelectionSize maximum number of TownBlocks we can select for.
	 * @param pos - WorldCoord where the selection is centered at.
	 * @param args - subcommand arguments like auto or a number.
	 * @param claim - This selection will result in claiming for a resident or town.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid radii are given.
	 */
	private static List<WorldCoord> selectWorldCoordAreaRect(int maxSelectionSize, WorldCoord pos, String[] args, boolean claim) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();
		if (args.length > 0) {
			int r = MAX_RECT_RADIUS;  // The greatest possible radius of a selection.

			/*
			 *  Area selections are capped at a 15 radius which should be a 31x31 (or a square with a side of 496 blocks in length.)  
			 *  Players need a permission node to use area claims and the max radius usable defaults to 4 (set in the config.)
			 */

			if (args[0].equalsIgnoreCase("auto")) {
				
				/*
				 * Select everything possible in a rectangle shape.
				 */

				if (TownySettings.getMaxClaimRadiusValue() > 0) 
					r = Math.min(r, TownySettings.getMaxClaimRadiusValue());

			} else {
			
				/*
				 * Select an area that will claim a perfect square shape, using a given radius
				 * or the reduced radius that will give a perfect square.
				 */
				
				try {
					r = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					throw new TownyException(Translatable.of("msg_err_invalid_radius"));
				}
				if (TownySettings.getMaxClaimRadiusValue() > 0 && r > TownySettings.getMaxClaimRadiusValue())
					throw new TownyException(Translatable.of("msg_err_invalid_radius_number", TownySettings.getMaxClaimRadiusValue()));

				/*
				 * Calculate how many townblocks will be needed to claim the desired radius,
				 * dropping the radius if it will be required, to make a perfect a perfect square.
				 */
				int needed = pos.isWilderness() ? 1 : 0;
				int claimRadius = 1;
				while (claimRadius <= r && needed < maxSelectionSize) {
				    needed += (claimRadius * 8);
				    claimRadius++;
				}
				// Claim Radius will always overshoot by 1
				r = claimRadius - 1;
				maxSelectionSize = needed + 1;
			}

			/*
			 * Adds WorldCoords in a spiral-out pattern.
			 */
			int halfSideLength = ((r * 2) + 1) / 2;
			int x = 0, z = 0, dx = 0, dz = -1;
			for (int i = 0; i <= maxSelectionSize; i++) {
				if ((-halfSideLength <= x) && (x <= halfSideLength) && (-halfSideLength <= z) && (z <= halfSideLength)) {
					out.add(pos.add(x,z));
				}

				if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
					int swap = dx;
					dx = -dz;
					dz = swap;
				}
				x += dx;
				z += dz;
			}

		} else {
			throw new TownyException(Translatable.of("msg_err_invalid_radius"));
		}
		
		// We remove the first pos if this is a claim and it is not wilderness.
		if (claim && !pos.isWilderness())
			out.remove(0);

		return out;
	}

	/**
	 * Selects a circle shaped area of WorldCoords. Works in a spiral out fashion.
	 * 
	 * @param maxSelectionSize maximum number of TownBlocks we can select for.
	 * @param pos - WorldCoord where the selection is centered at.
	 * @param args - subcommand arguments like auto or a number.
	 * @param claim - This selection will result in claiming for a resident or town.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid radii are given.
	 */
	private static List<WorldCoord> selectWorldCoordAreaCircle(int maxSelectionSize, WorldCoord pos, String[] args, boolean claim) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();
		if (args.length > 0) {
			int r = MAX_CIRC_RADIUS; // The greatest possible radius of a selection.

			/*
			 *  Area selections are capped at a 18 radius (1009 maximum.)
			 *  Players need a permission node to use area claims and the max radius usable defaults to 4 (set in the config.)
			 */
			
			if (args[0].equalsIgnoreCase("auto")) {
				
				/*
				 * Select everything possible in a circle shape.
				 */
				if (maxSelectionSize > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
					while (maxSelectionSize - Math.ceil(Math.PI * r * r) >= 0)
						r += 1;
				
				if (TownySettings.getMaxClaimRadiusValue() > 0) 
					r = Math.min(r, TownySettings.getMaxClaimRadiusValue());

			} else {
				
				/*
				 * Select an area that will claim a perfect circle shape, using a given radius
				 * or the reduced radius that will give a perfect circle.
				 */
				
				try {
					r = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					throw new TownyException(Translatable.of("msg_err_invalid_radius"));
				}
				
				if (r < 0)
					throw new TownyException(Translatable.of("msg_err_invalid_radius"));
				
				if (TownySettings.getMaxClaimRadiusValue() > 0 && r > TownySettings.getMaxClaimRadiusValue())
					throw new TownyException(Translatable.of("msg_err_invalid_radius_number", TownySettings.getMaxClaimRadiusValue()));
				
				int radius = 0;
				if (maxSelectionSize > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
					while (maxSelectionSize - Math.ceil(Math.PI * radius * radius) >= 0)
						radius += 1;
				
				radius--;// We lower the radius by one so that we get only perfect circle claims.
				
				r = Math.min(r, radius); // This will ensure that if they've give too high of a radius we lower it to what they are able to actually claim.
				
			}
			
			/*
			 * Adds WorldCoords in a spiral-out pattern.
			 */
			int halfSideLength = ((r * 2) + 1) / 2;
			int x = 0, z = 0, dx = 0, dz = -1;
			for (int i = 0; i <= maxSelectionSize; i++) {
				if ((-halfSideLength <= x) && (x <= halfSideLength) && (-halfSideLength <= z) && (z <= halfSideLength)) {
					if (MathUtil.distanceSquared(x, z) <= MathUtil.sqr(r) && (out.size() <= maxSelectionSize)) {
						out.add(pos.add(x,z));
					}
				}

				if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
					int swap = dx;
					dx = -dz;
					dz = swap;
				}
				x += dx;
				z += dz;
			}

		} else {
			throw new TownyException(Translatable.of("msg_err_invalid_radius"));
		}
		
		// We remove the first pos if this is a claim and it is not wilderness.
		if (claim && !pos.isWilderness())
			out.remove(0);

		return out;
	}

	/**
	 * Returns a list containing only townblocks that can be claimed.
	 * Filters out townblocks too close to other towns as set in the config.
	 * 
	 * @param selection - List&lt;WorldCoord&gt; of coordinates
	 * @param town - Town to check distance from
	 * @return List of {@link WorldCoord}
	 */
	public static List<WorldCoord> filterInvalidProximityTownBlocks(List<WorldCoord> selection, Town town) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			if (worldCoord.getTownyWorld().getMinDistanceFromOtherTownsPlots(worldCoord, town) >= TownySettings.getMinDistanceFromTownPlotblocks()) {
				out.add(worldCoord);
			} else {
				TownyMessaging.sendDebugMsg("AreaSelectionUtil:filterInvalidProximity - Coord: " + worldCoord + " too close to another town." );
			}
		return out;
	}
	
	/**
	 * Returns a list containing only townblocks that can be claimed.
	 * Filters out townblocks too close to other town homeblocks as set in the config.
	 * 
	 * @param selection List&lt;WorldCoord&gt; of coordinates
	 * @param town Town to check distance from
	 * @return List of {@link WorldCoord}
	 */
	public static List<WorldCoord> filterInvalidProximityToHomeblock(List<WorldCoord> selection, Town town) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			if (!isTooCloseToHomeBlock(worldCoord, town)) {
				out.add(worldCoord);
			} else {
				TownyMessaging.sendDebugMsg("AreaSelectionUtil:filterInvalidProximity - Coord: " + worldCoord + " too close to another town's homeblock." );
			}
		return out;
	}

	/**
	 * Is the WorldCoord too close to another town's HomeBlock.
	 * 
	 * @param wc WorldCoord to check distance from.
	 * @param town Town whos homeblock we don't have to account for.
	 * @return true if the WorldCoord is too close to a homeblock.
	 */
	public static boolean isTooCloseToHomeBlock(WorldCoord wc, Town town) {
		return wc.getTownyWorld().getMinDistanceFromOtherTownsHomeBlocks(wc, town) < TownySettings.getMinDistanceFromTownHomeblocks();
	}

	/**
	 * Returns a list containing only wilderness townblocks.
	 * 
	 * @param selection List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of {@link WorldCoord}.
	 */
	public static List<WorldCoord> filterOutTownOwnedBlocks(List<WorldCoord> selection) {

		return selection.stream().filter(WorldCoord::isWilderness).collect(Collectors.toList());
	}
	
	/**
	 * Returns a List containing only claimed townblocks.
	 * 
	 * @param selection List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of {@link WorldCoord}
	 */
	public static List<WorldCoord> filterOutWildernessBlocks(List<WorldCoord> selection) {

		return selection.stream().filter(WorldCoord::hasTownBlock).collect(Collectors.toList());
	}

	/**
	 * Returns a List containing only claimed townblocks, owned by the given owner.
	 * 
	 * @param owner TownBlockOwner which owns the townblock.
	 * @param selection List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of {@link WorldCoord} owned by the given owner.
	 */
	public static List<WorldCoord> filterOwnedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {

		return filterOutWildernessBlocks(selection).stream()
				.filter(wc -> wc.getTownBlockOrNull().isOwner(owner))
				.collect(Collectors.toList());
	}
	
	/**
	 * Returns a List containing only claimed townblocks, which are not owned by the given owner.
	 * 
	 * @param owner TownBlockOwner which owns the townblock.
	 * @param selection List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of {@link WorldCoord} not owned by the given owner.
	 */
	public static List<WorldCoord> filterUnownedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {

		return filterOutWildernessBlocks(selection).stream()
				.filter(wc -> !wc.getTownBlockOrNull().isOwner(owner))
				.collect(Collectors.toList());
	}
	
	public static boolean filterHomeBlock(Town town, List<WorldCoord> selection) {
		if (!town.hasHomeBlock())
			return false;
		WorldCoord homeCoord = town.getHomeBlockOrNull().getWorldCoord();
		return selection.removeIf(worldCoord -> worldCoord.equals(homeCoord));
	}

	/**
	 * Gives a list of townblocks that have membership to the specified group.
	 * @param group The plot group to filter against.
	 * @param selection The selection of townblocks.
	 * @return A List of {@link WorldCoord} that contains the coordinates of townblocks part of the specified group.
	 */
	public static List<WorldCoord> filterPlotsByGroup(PlotGroup group, List<WorldCoord> selection) {
		
		return filterOutWildernessBlocks(selection).stream()
				.map(WorldCoord::getTownBlockOrNull)
				.filter(TownBlock::hasPlotObjectGroup)
				.filter(tb -> group.hasTownBlock(tb))
				.map(TownBlock::getWorldCoord)
				.collect(Collectors.toList());
	}
	
	public static HashSet<PlotGroup> getPlotGroupsFromSelection(List<WorldCoord> selection) {
		HashSet<PlotGroup> seenGroups = new HashSet<>();
		for (WorldCoord coord : selection) {
			if (!coord.hasTownBlock() || !coord.getTownBlockOrNull().hasPlotObjectGroup())
				continue;
			seenGroups.add(coord.getTownBlockOrNull().getPlotObjectGroup());
		}
		return seenGroups;
	}

	/**
	 * Gather plots that are for sale only, using a resident to determine whether they can be bought.
	 * 
	 * @param resident Resident who would be buying the plots.
	 * @param selection List&lt;WorldCoord&gt; from which to get plots that are for sale.
	 * @return List&lt;WorldCoord&gt; that are all for sale.
	 */
	public static List<WorldCoord> filterPlotsForSale(Resident resident, List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection) {
			TownBlock townBlock = worldCoord.getTownBlockOrNull();
			if (townBlock == null || !residentCanBuyTownBlock(resident, townBlock))
				continue;

			// Plot Groups do not set a townblock's individual plot price. 
			if (townBlock.hasPlotObjectGroup()) {
				out.clear();             // Remove any other plots from the selection. 
				out.add(worldCoord);     // Put in the one plot-group-having townblock, the rest of the group will be added later.
				return out;              // Return the one plot-group-having townblock.
			}

			out.add(worldCoord);
		}
		return out;
	}

	/**
	 * Gather plots that are for sale only, currently used only for setting plots to notforsale.
	 * 
	 * @param selection List&lt;WorldCoord&gt; from which to get plots that are for sale.
	 * @return List&lt;WorldCoord&gt; that are all for sale.
	 */
	public static List<WorldCoord> filterPlotsForSale(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection) {
			TownBlock townBlock = worldCoord.getTownBlockOrNull();
			if (townBlock == null || !townBlockIsForSale(townBlock))
				continue;

			// Plot Groups do not set a townblock's individual plot price. 
			if (townBlock.hasPlotObjectGroup()) {
				out.clear();             // Remove any other plots from the selection. 
				out.add(worldCoord);     // Put in the one plot-group-having townblock, the rest of the group will be added later.
				return out;              // Return the one plot-group-having townblock.
			}

			out.add(worldCoord);
		}
		return out;
	}

	private static boolean residentCanBuyTownBlock(Resident resident, TownBlock townBlock) {
		try {
			townBlock.testTownMembershipAgePreventsThisClaimOrThrow(resident);
		} catch (TownyException e) {
			if (resident.isOnline())
				TownyMessaging.sendErrorMsg(resident.getPlayer(), e.getMessage(resident.getPlayer()));
			return false;
		}
		Town town = townBlock.getTownOrNull();
		return town != null && townBlockIsForSale(townBlock) && (town.hasResident(resident) || townBlock.getType().equals(TownBlockType.EMBASSY));
	}

	private static boolean townBlockIsForSale(TownBlock townBlock) {
		return townBlock.isForSale() || (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPrice() != -1);
	}

	/**
	 * Gather plots that are not for sale only.
	 * @param selection List&lt;WorldCoord&gt; from which to get plots that are not for sale.
	 * @return List&lt;WorldCoord&gt; that are all not for sale.
	 */
	public static List<WorldCoord> filterPlotsNotForSale(List<WorldCoord> selection) {

		return selection.stream().filter(wc -> wc.hasTownBlock() && wc.getTownBlockOrNull().isForSale()).collect(Collectors.toList());
	}
	
	/**
	 * Returns a List containing only claimed townblocks, which are:
	 * - not personally owned by a resident who isn't the given resident 
	 * 
	 * @param resident The Resident to ignore.
	 * @param selection List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks which does not includes townblocks owned by players who aren't the given resident.
	 */
	public static List<WorldCoord> filterOutResidentBlocks(Resident resident, List<WorldCoord> selection) {

		return selection.stream().filter(wc -> wc.hasTownBlock()
				&& !wc.getTownBlockOrNull().hasResident() || !wc.getTownBlockOrNull().hasResident(resident))
				.collect(Collectors.toList());
	}

	/**
	 * Returns a List containing only WorldCoords which are not composed of "too much"
	 * bad biomes, with the threshold determined by the config.
	 * 
	 * @param player Player trying to claim.
	 * @param selection List of WorldCoords.
	 * @return a List of WorldCoords which have passed the biome requirements.
	 */
	public static List<WorldCoord> filterOutUnwantedBiomeWorldCoords(Player player, List<WorldCoord> selection) {
		if (!TownySettings.isUnwantedBiomeClaimingEnabled())
			return selection;
		Predicate<WorldCoord> biomeThresholdTest = wc -> BiomeUtil.getWorldCoordUnwantedBiomePercent(wc) < TownySettings.getUnwantedBiomeThreshold();
		return filterOutByBiome(player, selection, biomeThresholdTest, "msg_err_cannot_claim_the_following_worldcoords_because_of_unwanted_biome");
	}

	/**
	 * Returns a List containing only WorldCoords which are not composed of "too much"
	 * ocean biomes, with the threshold determined by the config.
	 * 
	 * @param player Player trying to claim.
	 * @param selection List of WorldCoords.
	 * @return a List of WorldCoords which have passed the biome requirements.
	 */
	public static List<WorldCoord> filterOutOceanBiomeWorldCoords(Player player, List<WorldCoord> selection) {
		if (!TownySettings.isOceanClaimingBlocked())
			return selection;

		Predicate<WorldCoord> biomeThresholdTest = wc -> BiomeUtil.getWorldCoordOceanBiomePercent(wc) < TownySettings.getOceanBlockThreshold();
		return filterOutByBiome(player, selection, biomeThresholdTest, "msg_err_cannot_claim_the_following_worldcoords_because_of_ocean_biome");
	}

	public static List<WorldCoord> filterOutByBiome(Player player, List<WorldCoord> selection, Predicate<WorldCoord> biomeThresholdTest, String errorMsg) {
		// Strip list into succesful and failing lists of WorldCoords.
		Map<Boolean, List<WorldCoord>> worldCoords = selection.stream().collect(Collectors.partitioningBy(biomeThresholdTest));

		// Feedback as to why a plot isn't claimable due to biome.
		if (!worldCoords.get(false).isEmpty())
			TownyMessaging.sendErrorMsg(player, Translatable.of(errorMsg, prettyWorldCoordList(worldCoords.get(false))));

		// Return successful selections.
		return worldCoords.get(true);
	}

	private static String prettyWorldCoordList(List<WorldCoord> worldCoords) {
		return StringMgmt.join(worldCoords.stream()
				.map(wc -> String.format("(%s)", wc.getCoord().toString()))
				.collect(Collectors.toList()), ", ");
	}

	public static int getAreaSelectPivot(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("within"))
				return i;
		}
		return -1;
	}

	public static boolean isOnEdgeOfOwnership(TownBlockOwner owner, WorldCoord worldCoord) {
		return worldCoord.getCardinallyAdjacentWorldCoords(false).stream().filter(wc -> !wc.hasTownBlock() || !wc.getTownBlockOrNull().isOwner(owner)).findAny().isPresent();
	}
}
