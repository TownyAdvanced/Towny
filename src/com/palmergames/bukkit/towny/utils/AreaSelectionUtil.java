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
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Location;

public class AreaSelectionUtil {

	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();

		if (args.length == 0) {
			// claim with no sub command entered so attempt selection of one plot
			out.add(pos);

		} else {
			if (args.length > 1) {
				if (args[0].equalsIgnoreCase("rect")) {
					out = selectWorldCoordAreaRect(owner, pos, StringMgmt.remFirstArg(args));
				} else if (args[0].equalsIgnoreCase("circle")) {
					out = selectWorldCoordAreaCircle(owner, pos, StringMgmt.remFirstArg(args));
				} else {
					throw new TownyException(Translation.of("msg_err_invalid_property", StringMgmt.join(args, " ")));
				}
			} else if (args[0].equalsIgnoreCase("auto")) {
				out = selectWorldCoordAreaRect(owner, pos, args);
			} else if (args[0].equalsIgnoreCase("outpost")) {
				TownBlock tb = pos.getTownBlock();
				if (!tb.isOutpost() && tb.hasTown()) { // isOutpost(), only for mysql however, if we include this we can skip the outposts on flatfile so less laggy!
					Town town = tb.getTown();
					if (isTownBlockLocContainedInTownOutposts(town.getAllOutpostSpawns(), tb)) {
						tb.setOutpost(true);
						out.add(pos);
					} else {
						throw new TownyException(Translation.of("msg_err_unclaim_not_outpost"));
						// Lang String required.
					}
				}
				if (tb.isOutpost()) { // flatfile skipper
					out.add(pos);
				}
			} else {
				try {
					Integer.parseInt(args[0]);
					// Treat as rect to serve for backwards capability.
					out = selectWorldCoordAreaRect(owner, pos, args);
				} catch (NumberFormatException e) {
					throw new TownyException(Translation.of("msg_err_invalid_property", args[0]));
				}
			}
		}

		return out;
	}

	private static List<WorldCoord> selectWorldCoordAreaRect(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();
		if (pos.getTownyWorld().isClaimable()) {
			if (args.length > 0) {
				int r = 0, available = 1000;

				if (owner instanceof Town) {
					Town town = (Town) owner;
					available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
				} else if (owner instanceof Resident) {
					available = TownySettings.getMaxResidentPlots((Resident) owner);
				}

				if (args[0].equalsIgnoreCase("auto")) {
					// Attempt to select outwards until no town blocks remain. 
					// This will make a less perfect claim than /t claim rect # or /t claim circle auto.
					r = 1;
					int total = 0;
					while (available - ((r * 8) + total) >= 0) { // Radius 1 grabs 8 blocks, 2 grabs 16 more requiring 24, 3 grabs 24 more requiring 48, etc...
						total += r * 8;
						r++;
					}
					
					if (TownySettings.getMaxClaimRadiusValue() > 0) 
						r = Math.min(r, TownySettings.getMaxClaimRadiusValue());

				} else {
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(Translation.of("msg_err_invalid_radius"));
					}
					if (TownySettings.getMaxClaimRadiusValue() > 0 && r > TownySettings.getMaxClaimRadiusValue())
						throw new TownyException(Translation.of("msg_err_invalid_radius_number", TownySettings.getMaxClaimRadiusValue()));

					// Calculate how many TownBlocks are needed to claim the radius. 
					// Start blocks at 0 if the plot is unclaimed.
					int neededBlocks = pos.getTownBlock().hasTown() ? 0 : 1;
					for (int i = 1; i <= r; i++)
						neededBlocks += i * 8;
					
					// Rethink how much of a radius will be used, as there's not enough available TownBlocks.
					if (neededBlocks > available) {
						r = 1; 
						int total = 0;
						while (available - ((r * 8) + total) >= 0) { // Radius 1 grabs 8 blocks, 2 grabs 16 more requiring 24, 3 grabs 24 more requiring 48, etc...
							total += r * 8;
							r++;
						}
						r--; // Finally reduce the radius by 1 (so that we have a perfect ring of claims,) and replace the original r.
					}
				}
					
				if (r > 1000)
					r = 1000;
				for (int z = -r; z <= r; z++)
					for (int x = -r; x <= r; x++)
						if (out.size() <= available) {
							out.add(new WorldCoord(pos.getWorldName(), pos.getX() + x, pos.getZ() + z));
						}
			} else {
				throw new TownyException(Translation.of("msg_err_invalid_radius"));
			}
		}

		return out;
	}

	private static List<WorldCoord> selectWorldCoordAreaCircle(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();
		if (pos.getTownyWorld().isClaimable()) {
			if (args.length > 0) {
				int r = 0, available = 0;
				if (owner instanceof Town) {
					Town town = (Town) owner;
					available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
				} else if (owner instanceof Resident) {
					available = TownySettings.getMaxResidentPlots((Resident) owner);
				}

				if (args[0].equalsIgnoreCase("auto")) {
					// Attempt to select outwards until no town blocks remain

					if (available > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
						while (available - Math.ceil(Math.PI * r * r) >= 0)
							r += 1;
					
					if (TownySettings.getMaxClaimRadiusValue() > 0) 
						r = Math.min(r, TownySettings.getMaxClaimRadiusValue());

				} else {
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(Translation.of("msg_err_invalid_radius"));
					}
					
					if (r > TownySettings.getMaxClaimRadiusValue() && TownySettings.getMaxClaimRadiusValue() > 0) {
						throw new TownyException(Translation.of("msg_err_invalid_radius_number", TownySettings.getMaxClaimRadiusValue()));
					}
					
					int radius = 0;
					if (available > 0) // Since: 0 - ceil(Pi * 0^2) >= 0 is a true statement.
						while (available - Math.ceil(Math.PI * radius * radius) >= 0)
							radius += 1;
					
					radius--;// We lower the radius by one so that we get only perfect circle claims.
					
					r = Math.min(r, radius); // This will ensure that if they've give too high of a radius we lower it to what they are able to actually claim.
					
				}
				
				if (r > 1000)
					r = 1000;
				for (int z = -r; z <= r; z++)
					for (int x = -r; x <= r; x++)
						if ((x * x + z * z <= r * r) && (out.size() <= available)) {
							out.add(new WorldCoord(pos.getWorldName(), pos.getX() + x, pos.getZ() + z));
						}
							
			} else {
				throw new TownyException(Translation.of("msg_err_invalid_radius"));
			}
		}

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
			try {
				if (worldCoord.getTownyWorld().getMinDistanceFromOtherTownsPlots(worldCoord, town) >= TownySettings.getMinDistanceFromTownPlotblocks()) {
					out.add(worldCoord);
				} else {
					TownyMessaging.sendDebugMsg("AreaSelectionUtil:filterInvalidProximity - Coord: " + worldCoord + " too close to another town." );					
				}
			} catch (NotRegisteredException ignored) {
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
			try {
				if (worldCoord.getTownyWorld().getMinDistanceFromOtherTowns(worldCoord, town) >= TownySettings.getMinDistanceFromTownHomeblocks()) {
					out.add(worldCoord);
				} else {
					TownyMessaging.sendDebugMsg("AreaSelectionUtil:filterInvalidProximity - Coord: " + worldCoord + " too close to another town's homeblock." );					
				}
			} catch (NotRegisteredException ignored) {
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
	 * Returns a List containing only claimed townblocks, which are not personally owned by a resident.
	 * 
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks not owned by the given owner.
	 */
	public static List<WorldCoord> filterOutResidentBlocks(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasResident())
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
	
    /**
     * Pretty much this method checks if a townblock is contained within a list of locations.
     *
     * @param minecraftcoordinates - List of minecraft coordinates you should probably parse town.getAllOutpostSpawns()
     * @param tb                   - TownBlock to check if its contained..
     * @return true if the TownBlock is considered an outpost by it's Town.
     * @author Lukas Mansour (Articdive)
     */
    public static boolean isTownBlockLocContainedInTownOutposts(List<Location> minecraftcoordinates, TownBlock tb) {
        if (minecraftcoordinates != null && tb != null) {
            for (Location minecraftcoordinate : minecraftcoordinates) {
                if (Coord.parseCoord(minecraftcoordinate).equals(tb.getCoord())) {
                    return true; // Yes the TownBlock is considered an outpost by the Town
                }
            }
        }
        return false;
    }

}
