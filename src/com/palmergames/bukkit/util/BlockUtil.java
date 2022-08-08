package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public class BlockUtil {
	
	public static List<BlockFace> CARDINAL_BLOCKFACES = new ArrayList<>(Arrays.asList(BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST));

	/**
	 * Are the blocks owned by the same resident or same town (if not player-owned.)
	 * @param b1 Block 1
	 * @param b2 Block 2
	 * @return True if the blocks have the same owner.
	 */
	public static boolean sameOwner(Block b1, Block b2) {
		WorldCoord wc = WorldCoord.parseWorldCoord(b1);
		WorldCoord wc2 = WorldCoord.parseWorldCoord(b2);
		if (sameWorldCoord(wc, wc2) || bothWilderness(wc, wc2))
			return true;
		
		if (wc.hasTownBlock() && wc2.hasTownBlock()) {
			TownBlock tb = wc.getTownBlockOrNull();
			TownBlock tb2 = wc2.getTownBlockOrNull();
			Town town1 = wc.getTownOrNull();
			Town town2 = wc2.getTownOrNull();
			
			if (!town1.getUUID().equals(town2.getUUID())) // Not the same town.
				return false;
			
			if (tb.hasResident() != tb2.hasResident()) // One is player-owned and one isn't.
				return false;

			if (!tb.hasResident() && !tb2.hasResident() && town1.getUUID().equals(town2.getUUID())) // Both plots are town-owned, by the same town.
				return true;

			if (tb.hasResident() && tb2.hasResident() && tb.getResidentOrNull().getName().equals(tb2.getResidentOrNull().getName())) // Both plots are owned by the same resident.
				return true;
		}
		// return false, as these blocks do not share an owner.
		return false;
	}

	/**
	 * Are the blocks owned by the same resident, same town, or does the given Player
	 * have the towny.command.plot.asmayor permission node used to discern mayor-like
	 * priviledges.
	 * @param b1 Block 1
	 * @param b2 Block 2
	 * @param player Player who is acting.
	 * @return True if the blocks have the same owner, or a mayor-like player has override.
	 */
	public static boolean sameOwnerOrHasMayorOverride(Block b1, Block b2, Player player) {
		WorldCoord wc = WorldCoord.parseWorldCoord(b1);
		WorldCoord wc2 = WorldCoord.parseWorldCoord(b2);
		if (sameWorldCoord(wc, wc2) || bothWilderness(wc, wc2))
			return true;
		
		if (wc.hasTownBlock() && wc2.hasTownBlock()) {
			TownBlock tb = wc.getTownBlockOrNull();
			TownBlock tb2 = wc2.getTownBlockOrNull();
			Town town1 = wc.getTownOrNull();
			Town town2 = wc2.getTownOrNull();
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

			if (resident == null)
				return false;

			if (!town1.getUUID().equals(town2.getUUID())) // Not the same town.
				return false;
			
			if (tb.hasResident() != tb2.hasResident()) // One is player-owned and one isn't.
				if (isResidentActingMayorOfTown(resident, tb.getTownOrNull()))
					return true;
				else
					return false;

			if (!tb.hasResident() && !tb2.hasResident()) // Both plots are town-owned.
				return true;

			if (tb.hasResident() && tb2.hasResident() && tb.getResidentOrNull().getName().equals(tb2.getResidentOrNull().getName())) // Both plots are owned by the same resident.
				return true;
		}
		// return false, as these blocks do not share an owner.
		return false;
	}
	
	private static boolean bothWilderness(WorldCoord wc, WorldCoord wc2) {
		return !wc.hasTownBlock() && !wc2.hasTownBlock();
	}
	
	private static boolean isResidentActingMayorOfTown(Resident resident, Town town) {
		return TownyUniverse.getInstance().getPermissionSource().testPermission(resident.getPlayer(), PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())
				&& resident.hasTown()
				&& town.getUUID().equals(TownyAPI.getInstance().getResidentTownOrNull(resident).getUUID());
	}

	public static boolean sameWorldCoord(Block b1, Block b2) {
		return sameWorldCoord(WorldCoord.parseWorldCoord(b1), WorldCoord.parseWorldCoord(b2));
	}
	
	public static boolean sameWorldCoord(WorldCoord wc, WorldCoord wc2) {
		return (wc.getCoord().equals(wc2.getCoord()));
	}
}
