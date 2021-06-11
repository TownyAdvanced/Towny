package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class BorderUtil {

	public static List<CellBorder> getOuterBorder(List<WorldCoord> worldCoords) {

		List<CellBorder> borderCoords = new ArrayList<CellBorder>();
		for (WorldCoord worldCoord : worldCoords) {
			CellBorder border = new CellBorder(worldCoord, new boolean[] {
					!worldCoords.contains(worldCoord.add(-1, 0)),
					!worldCoords.contains(worldCoord.add(-1, -1)),
					!worldCoords.contains(worldCoord.add(0, -1)),
					!worldCoords.contains(worldCoord.add(1, -1)),
					!worldCoords.contains(worldCoord.add(1, 0)),
					!worldCoords.contains(worldCoord.add(1, 1)),
					!worldCoords.contains(worldCoord.add(0, 1)),
					!worldCoords.contains(worldCoord.add(-1, 1)) });
			if (border.hasAnyBorder())
				borderCoords.add(border);
		}
		return borderCoords;
	}

	public static List<CellBorder> getPlotBorder(List<WorldCoord> worldCoords) {

		List<CellBorder> borderCoords = new ArrayList<CellBorder>();
		for (WorldCoord worldCoord : worldCoords) {
			CellBorder border = getPlotBorder(worldCoord);
			borderCoords.add(border);
		}
		return borderCoords;
	}

	public static CellBorder getPlotBorder(WorldCoord worldCoord) {

		return new CellBorder(worldCoord, new boolean[] {
				true, true, true, true, true, true, true, true });
	}
	
	public static List<BlockState> allowedBlocks(List<BlockState> blocks, Block originBlock) {
		return blocks.stream()
			.filter(blockState -> allowedMove(originBlock, blockState.getBlock()))
			.collect(Collectors.toList());
	}
	
	public static List<BlockState> disallowedBlocks(List<BlockState> blocks, Block originBlock) {
		return blocks.stream()
			.filter(blockState -> !allowedMove(originBlock, blockState.getBlock()))
			.collect(Collectors.toList());
	}
	
	public static boolean allowedMove(Block block, Block blockTo) {
		WorldCoord from = WorldCoord.parseWorldCoord(block);
		WorldCoord to = WorldCoord.parseWorldCoord(blockTo);
		if (from.equals(to) || TownyAPI.getInstance().isWilderness(to))
			return true;
		
		// From is wilderness and To is a town.
		if (!from.hasTownBlock())
			return false;

		TownBlock currentTownBlock = from.getTownBlockOrNull();
		TownBlock destinationTownBlock = to.getTownBlockOrNull();
		
		// One is player owned and the other isn't.
		if (currentTownBlock.hasResident() != destinationTownBlock.hasResident())
			return false;

		// Both townblocks are owned by the same resident.
		if (currentTownBlock.hasResident() && destinationTownBlock.hasResident() 
			&& currentTownBlock.getResidentOrNull() == destinationTownBlock.getResidentOrNull())
			return true;

		// Both townblocks are owned by the same town.
		return currentTownBlock.getTownOrNull() == destinationTownBlock.getTownOrNull() 
			&& !currentTownBlock.hasResident() && !destinationTownBlock.hasResident();
		
	}
}
