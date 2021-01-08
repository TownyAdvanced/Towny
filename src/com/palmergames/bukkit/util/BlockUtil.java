package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class BlockUtil {
	
	public static List<BlockFace> CARDINAL_BLOCKFACES = new ArrayList<>(Arrays.asList(BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST));

	public static boolean sameOwner(Block b1, Block b2) {
		WorldCoord wc = WorldCoord.parseWorldCoord(b1);
		WorldCoord wc2 = WorldCoord.parseWorldCoord(b2);
		if (wc.getCoord().equals(wc2.getCoord()) || (!wc.hasTownBlock() && !wc2.hasTownBlock())) // The blocks are on the same townblock or both are wilderness.
			return true;
		
		if (wc.hasTownBlock() && wc2.hasTownBlock()) {
			TownBlock tb = null;
			TownBlock tb2 = null;
			try {
				tb = wc.getTownBlock();
				tb2 = wc2.getTownBlock();
				if (tb.hasResident() != tb2.hasResident()) // One is player-owned and one isn't.
					return false;

				if (!tb.hasResident() && !tb2.hasResident() && tb.getTown().getUUID().equals(tb2.getTown().getUUID())) // Both plots are town-owned, by the same town.
					return true;

				if (tb.hasResident() && tb2.hasResident() && tb.getResident().getName().equals(tb2.getResident().getName())) // Both plots are owned by the same resident.
					return true;

			} catch (NotRegisteredException ignored) {}
		}
		// return false, as these blocks do not share an owner.
		return false;
	}
}
