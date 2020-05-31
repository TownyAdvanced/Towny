package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AreaSelectionUtil {

	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

		List<WorldCoord> out = new ArrayList<>();

		if (args.length == 0) {
			// claim with no sub command entered so attempt selection of one plot
			if (pos.getTownyWorld().isClaimable())
				out.add(pos);
			else
				throw new TownyException(TownySettings.getLangString("msg_not_claimable"));
		} else {
			if (args.length > 1) {
				if (args[0].equalsIgnoreCase("rect")) {
					out = selectWorldCoordAreaRect(owner, pos, StringMgmt.remFirstArg(args));
				} else if (args[0].equalsIgnoreCase("circle")) {
					out = selectWorldCoordAreaCircle(owner, pos, StringMgmt.remFirstArg(args));
				} else {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), StringMgmt.join(args, " ")));
				}
			} else if (args[0].equalsIgnoreCase("auto")) {
				out = selectWorldCoordAreaRect(owner, pos, args);
			} else if (args[0].equalsIgnoreCase("outpost")) {
				TownBlock tb = pos.getTownBlock();
				if (!tb.isOutpost() && tb.hasTown()) { // isOutpost(), only for mysql however, if we include this we can skip the outposts on flatfile so less laggy!
					Town town = tb.getTown();
					if (TownyUniverse.getInstance().isTownBlockLocContainedInTownOutposts(town.getAllOutpostSpawns(), tb)) {
						tb.setOutpost(true);
						out.add(pos);
					} else {
						throw new TownyException(TownySettings.getLangString("msg_err_unclaim_not_outpost"));
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
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), args[0]));
				}
			}
		}

		return out;
	}

	public static List<WorldCoord> selectWorldCoordAreaRect(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

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
					// Attempt to select outwards until no town blocks remain

					while (available - Math.pow((r + 1) * 2 - 1, 2) >= 0)
						r += 1;

				} else {
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
					}
				}
				if (r > TownySettings.getMaxClaimRadiusValue() && TownySettings.getMaxClaimRadiusValue() > 0) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_radius_number"),TownySettings.getMaxClaimRadiusValue()));
				}
					
				if (r > 1000)
					r = 1000;
				for (int z = -r; z <= r; z++)
					for (int x = -r; x <= r; x++)
						if (out.size() < available)
							out.add(new WorldCoord(pos.getWorldName(), pos.getX() + x, pos.getZ() + z));
			} else {
				throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
			}
		}

		return out;
	}

	public static List<WorldCoord> selectWorldCoordAreaCircle(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {

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

				} else {
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
					}
				}
				
				if (r > TownySettings.getMaxClaimRadiusValue() && TownySettings.getMaxClaimRadiusValue() > 0) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_radius_number"),TownySettings.getMaxClaimRadiusValue()));
				}
				
				if (r > 1000)
					r = 1000;
				for (int z = -r; z <= r; z++)
					for (int x = -r; x <= r; x++)
						if ((x * x + z * z <= r * r) && (out.size() < available))
							out.add(new WorldCoord(pos.getWorldName(), pos.getX() + x, pos.getZ() + z));
			} else {
				throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
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
	 * Returns a list containing only wilderness townblocks.
	 * 
	 * @param selection - List of Coordinates (List&lt;WorldCoord&gt;)
	 * @return List of townblocks
	 */
	public static List<WorldCoord> filterTownOwnedBlocks(List<WorldCoord> selection) {

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
	public static List<WorldCoord> filterWildernessBlocks(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}

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

	public static List<WorldCoord> filterPlotsNotForSale(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isForSale())
					out.add(worldCoord);
			} catch (NotRegisteredException ignored) {
			}
		return out;
	}

	public static List<WorldCoord> filterUnownedPlots(List<WorldCoord> selection) {

		List<WorldCoord> out = new ArrayList<>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().getPlotPrice() > -1)
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
