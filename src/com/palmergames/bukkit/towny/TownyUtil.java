package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.StringMgmt;

public class TownyUtil {
	public static List<WorldCoord> selectWorldCoordArea(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		
		if (args.length == 0) {
			// claim with no sub command entered so attempt selection of one plot
			if (pos.getWorld().isClaimable())
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
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_property"), StringMgmt.join(args," ")));
					}
				} else if (args[0].equalsIgnoreCase("auto")) {
					out = selectWorldCoordAreaRect(owner, pos, args);
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
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		if (pos.getWorld().isClaimable()) {
			if (args.length > 0) {
				int r;
				if (args[0].equalsIgnoreCase("auto")) {
					// Attempt to select outwards until no town blocks remain
					int available;
					if (owner instanceof Town) {
							Town town = (Town)owner;
							available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
					} else if (owner instanceof Resident) {
						available = TownySettings.getMaxResidentPlots((Resident)owner);
					} else
						throw new TownyException(TownySettings.getLangString("msg_err_rect_auto"));
					
					r = 0;
					while (available - Math.pow((r + 1) * 2 - 1, 2) >= 0)
						r += 1;

				} else {
					try {
						r = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
					}	
				}
				r -= 1;
				for (int z = pos.getZ() - r; z <= pos.getZ() + r; z++)
					for (int x = pos.getX() - r; x <= pos.getX() + r; x++)
						out.add(new WorldCoord(pos.getWorld(), x, z));
			} else {
				throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
			}
		}

		return out;
	}
	
	public static List<WorldCoord> selectWorldCoordAreaCircle(TownBlockOwner owner, WorldCoord pos, String[] args) throws TownyException {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		if (pos.getWorld().isClaimable()) {
			if (args.length > 0) {
				int r;
				if (args[0].equalsIgnoreCase("auto")) {
					// Attempt to select outwards until no town blocks remain
					int available;
					if (owner instanceof Town) {
							Town town = (Town)owner;
							available = TownySettings.getMaxTownBlocks(town) - town.getTownBlocks().size();
					} else if (owner instanceof Resident) {
						available = TownySettings.getMaxResidentPlots((Resident)owner);
					} else
						throw new TownyException(TownySettings.getLangString("msg_err_rect_auto"));
						
					r = 0;
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
				for (int z = -r; z <= r; z++)
					for (int x = -r; x <= r; x++)
						if (x*x+z*z <= r*r)
							out.add(new WorldCoord(pos.getWorld(), pos.getX()+x, pos.getZ()+z));
			} else {
				throw new TownyException(TownySettings.getLangString("msg_err_invalid_radius"));
			}
		}

		return out;
	}
	
	public static List<WorldCoord> filterTownOwnedBlocks(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (!worldCoord.getTownBlock().hasTown())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
				out.add(worldCoord);
			}
		return out;
	}
	
	public static List<WorldCoord> filterOwnedBlocks(TownBlockOwner owner, List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isOwner(owner))
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
			}
		return out;
	}
	
	public static List<WorldCoord> filterPlotsForSale(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isForSale())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
			}
		return out;
	}
	
	public static List<WorldCoord> filterPlotsNotForSale(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().isForSale())
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
			}
		return out;
	}
	
	public static List<WorldCoord> filterUnownedPlots(List<WorldCoord> selection) {
		List<WorldCoord> out = new ArrayList<WorldCoord>();
		for (WorldCoord worldCoord : selection)
			try {
				if (worldCoord.getTownBlock().getPlotPrice() > -1)
					out.add(worldCoord);
			} catch (NotRegisteredException e) {
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
                TownBlock edgeTownBlock = worldCoord.getWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
                if (!edgeTownBlock.isOwner(owner)) {
                    return true;
                }
            } catch (NotRegisteredException e) {
            	return true;
            }
        return false;
	}
	
	public static Long townyTime() {
		return ((TownySettings.getDayInterval()*1000) - (System.currentTimeMillis() % (TownySettings.getDayInterval()*1000)))/1000;
	}
}
