package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AreaSelectionUtil {
	
	private final static int MAX_RECT_RADIUS = 15; // A maximum radius of 15 will garner 961 townblocks. Capped to prevent servers from dying.
	private final static int MAX_CIRC_RADIUS = 18; // A maximum radius of 18 will garner 1009 townblocks. Capped to prevent servers from dying.

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
			 * Either /{command} {claim|unclaim} or /town claim {outpost}
			 */
			out.add(pos);

		} else {
			
			/*
			 * First, determine what is available to be claimed, if this selection is for claiming land.
			 * Besides claiming land this method is used to unclaim land, as well as to set plots forsale/notforsale. 
			 * When this is not a claim we're supplying the largest amount possible to be selected.
			 * When it is a claim, we're starting from 0 and using what the town or resident is able to claim.
			 */
			int available = claim ? 0 : 1009;
			if (claim) {
				if (owner instanceof Town town) {
					available = town.hasUnlimitedClaims() ? 1009 : town.availableTownBlocks();
				} else if (owner instanceof Resident resident) {
					available = TownySettings.getMaxResidentPlots(resident) - resident.getTownBlocks().size();
				}
			}
			/*
			 * Second, handle different subcommands for /{command} {claim|unclaim} {rect|circle|auto|#} {#}
			 */
			if (args.length > 1) { // Has to be /{command} {claim|unclaim} {rect|circle} {auto|#}
				if (args[0].equalsIgnoreCase("rect")) {
					out = selectWorldCoordAreaRect(available, pos, StringMgmt.remFirstArg(args), claim);
				} else if (args[0].equalsIgnoreCase("circle")) {
					out = selectWorldCoordAreaCircle(available, pos, StringMgmt.remFirstArg(args), claim);
				} else if (args.length == 3 && args[1].startsWith("x") && args[2].startsWith("z")) {
					// "/plot claim world x# z#" was run by clicking on the towny map.
					out.add(new WorldCoord(args[0], Integer.parseInt(args[1].replace("x","")), Integer.parseInt(args[2].replace("z",""))));
				} else {
					throw new TownyException(Translatable.of("msg_err_invalid_property", StringMgmt.join(args, " ")));
				}
			} else if (args[0].equalsIgnoreCase("auto")) { // Is /{command} {claim|unclaim} {auto}
				out = selectWorldCoordAreaRect(available, pos, args, claim);
			} else { // Is /{command} {claim|unclaim} #
				try {
					Integer.parseInt(args[0]);
					// Treat as rect to serve for backwards capability.
					out = selectWorldCoordAreaRect(available, pos, args, claim);
				} catch (NumberFormatException e) {
					throw new TownyException(Translatable.of("msg_err_invalid_property", args[0]));
				}
			}
		}

		return out;
	}

	/**
	 * Selects a square shaped area of WorldCoords. Works in a spiral out fashion.
	 * 
	 * @param available - How many TownBlocks the TownBlockOwner has available to claim.
	 * @param pos - WorldCoord where the selection is centered at.
	 * @param args - subcommand arguments like auto or a number.
	 * @param claim - This selection will result in claiming for a resident or town.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid radii are given.
	 */
	private static List<WorldCoord> selectWorldCoordAreaRect(int available, WorldCoord pos, String[] args, boolean claim) throws TownyException {

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
				int needed = pos.getTownBlock().hasTown() ? 0 : 1;
				int claimRadius = 1;
				while (claimRadius <= r && needed < available) {
				    needed += (claimRadius * 8);
				    claimRadius++;
				}
				// Claim Radius will always overshoot by 1
				r = claimRadius - 1;
				available = needed + 1;
			}

			/*
			 * Adds WorldCoords in a spiral-out pattern.
			 */
			int halfSideLength = ((r * 2) + 1) / 2;
			int x = 0, z = 0, dx = 0, dz = -1;
			for (int i = 0; i <= available; i++) {
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
	 * @param available - How many TownBlocks the TownBlockOwner has available to claim.
	 * @param pos - WorldCoord where the selection is centered at.
	 * @param args - subcommand arguments like auto or a number.
	 * @param claim - This selection will result in claiming for a resident or town.
	 * @return List&lt;WorldCoord&gt; of {@link com.palmergames.bukkit.towny.object.WorldCoord}.
	 * @throws TownyException - Thrown when invalid radii are given.
	 */
	private static List<WorldCoord> selectWorldCoordAreaCircle(int available, WorldCoord pos, String[] args, boolean claim) throws TownyException {

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
				
				if (available > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
					while (available - Math.ceil(Math.PI * r * r) >= 0)
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
				
				if (TownySettings.getMaxClaimRadiusValue() > 0 && r > TownySettings.getMaxClaimRadiusValue())
					throw new TownyException(Translatable.of("msg_err_invalid_radius_number", TownySettings.getMaxClaimRadiusValue()));
				
				int radius = 0;
				if (available > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
					while (available - Math.ceil(Math.PI * radius * radius) >= 0)
						radius += 1;
				
				radius--;// We lower the radius by one so that we get only perfect circle claims.
				
				r = Math.min(r, radius); // This will ensure that if they've give too high of a radius we lower it to what they are able to actually claim.
				
			}
			
			/*
			 * Adds WorldCoords in a spiral-out pattern.
			 */
			int halfSideLength = ((r * 2) + 1) / 2;
			int x = 0, z = 0, dx = 0, dz = -1;
			for (int i = 0; i <= available; i++) {
				if ((-halfSideLength <= x) && (x <= halfSideLength) && (-halfSideLength <= z) && (z <= halfSideLength)) {
					if (MathUtil.distanceSquared(x, z) <= MathUtil.sqr(r) && (out.size() <= available)) {
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
	 * @return List of townblocks
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

	 * @param selection - List&lt;WorldCoord&gt; of coordinates
	 * @param town - Town to check distance from
	 * @return List of townblocks
	 */
	public static List<WorldCoord> filterInvalidProximityToHomeblock(List<WorldCoord> selection, Town town) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			if (worldCoord.getTownyWorld().getMinDistanceFromOtherTowns(worldCoord, town) >= TownySettings.getMinDistanceFromTownHomeblocks()) {
				out.add(worldCoord);
			} else {
				TownyMessaging.sendDebugMsg("AreaSelectionUtil:filterInvalidProximity - Coord: " + worldCoord + " too close to another town's homeblock." );
			}
		return out;
	}
	
	/**
	 * Returns a list containing only wilderness townblocks.
	 * 
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks
	 */
	public static List<WorldCoord> filterOutTownOwnedBlocks(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
				out.add(worldCoord);
			}
		return out;
	}
	
	/**
	 * Returns a List containing only claimed townblocks.
	 * 
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks
	 */
	public static List<WorldCoord> filterOutWildernessBlocks(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}

	/**
	 * Returns a List containing only claimed townblocks, owned by the given owner.
	 * 
	 * @param owner - TownBlockOwner which owns the townblock.
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks owned by the given owner.
	 */
	public static List<WorldCoord> filterOwnedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isOwner(owner))
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}
	
	/**
	 * Returns a List containing only claimed townblocks, which are not owned by the given owner.
	 * 
	 * @param owner - TownBlockOwner which owns the townblock.
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks not owned by the given owner.
	 */
	public static List<WorldCoord> filterUnownedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().isOwner(owner))
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}
	
	public static boolean filterHomeBlock(Town town, List<WorldCoord> selection) {
		WorldCoord homeCoord;
		
		try {
			homeCoord = town.getHomeBlock().getWorldCoord();
		} catch (TownyException ignore) {
			return false;
		}
		
		return selection.removeIf(worldCoord -> worldCoord.equals(homeCoord));
	}

	/**
	 * Gives a list of townblocks that have membership to the specified group.
	 * @param group The plot group to filter against.
	 * @param selection The selection of townblocks.
	 * @return A List of {@link WorldCoord} that contains the coordinates of townblocks part of the specified group.
	 * @author Suneet Tipirneni (Siris)
	 */
	public static List<WorldCoord> filterPlotsByGroup(PlotGroup group, List<WorldCoord> selection) {
		List<WorldCoord> out =  new ArrayList<>();
		
		for (WorldCoord worldCoord : selection) {
			
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().equals(group)) {
					out.add(worldCoord);
				}
			} catch (NotRegisteredException ignored) {}
		}
		
		return out;
	}
	
	public static HashSet<PlotGroup> getPlotGroupsFromSelection(List<WorldCoord> selection) {
		HashSet<PlotGroup> seenGroups = new HashSet<>();
		
		for (WorldCoord coord : selection) {
			
			PlotGroup group = null;
			try {
				group = coord.getTownBlock().getPlotObjectGroup();
			} catch (NotRegisteredException ignored) {}
			
			if (seenGroups.contains(group))
				continue;
			
			seenGroups.add(group);
			
		}
		
		return seenGroups;
	}

	/**
	 * Gather plots that are for sale only.
	 * @param selection - List&lt;WorldCoord&gt; from which to get plots that are for sale.
	 * @return List&lt;WorldCoord&gt; that are all for sale.
	 */
	public static List<WorldCoord> filterPlotsForSale(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				// Plot Groups do not set a townblock's individual plot price. 
				if (worldCoord.getTownBlock().hasPlotObjectGroup() && worldCoord.getTownBlock().getPlotObjectGroup().getPrice() != -1) {
					out.clear();             // Remove any other plots from the selection. 
					out.add(worldCoord);     // Put in the one plot-group-having townblock, the rest of the group will be added later.
					return out;              // Return the one plot-group-having townblock.
				}

				if (worldCoord.getTownBlock().isForSale())
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}

	/**
	 * Gather plots that are not for sale only.
	 * @param selection - List&lt;WorldCoord&gt; from which to get plots that are not for sale.
	 * @return List&lt;WorldCoord&gt; that are all not for sale.
	 */
	public static List<WorldCoord> filterPlotsNotForSale(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().isForSale())
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
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

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasResident() || (worldCoord.getTownBlock().hasResident() && worldCoord.getTownBlock().getResidentOrNull().equals(resident)))
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}

	public static int getAreaSelectPivot(String[] args) {

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("within"))
				return i;
		}
		return -1;
	}

	public static boolean isOnEdgeOfOwnership(TownBlockOwner owner, WorldCoord worldCoord) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				if (!edgeTownBlock.isOwner(owner)) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}
