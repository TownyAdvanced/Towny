package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

import com.palmergames.bukkit.towny.object.WorldCoord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
	
	/**
	 * Will return a list of blocks which all either have the same town as an owner, 
	 * or the same player as owner. Any block in the wilderness is considered allowed. 
	 * 
	 * @param blocks List&lt;BlockState&gt; which hasn't been filtered yet.
	 * @param originBlock Block from which to test against.
	 * @return List&lt;BlockState&gt; which has been filtered to same-owner and wilderness.
	 */
	public static List<BlockState> allowedBlocks(List<BlockState> blocks, Block originBlock) {
		return allowedBlocks(blocks, originBlock, null);
	}
	
	/**
	 * Will return a list of blocks which all either have the same town as an owner, 
	 * or the same player as owner. Any block in the wilderness is considered allowed.
	 * Takes the player involved into account.
	 * 
	 * @param blocks List&lt;BlockState&gt; which hasn't been filtered yet.
	 * @param originBlock Block from which to test against.
	 * @param player Player which is involved in the test.
	 * @return List&lt;BlockState&gt; which has been filtered to same-owner and wilderness.
	 */
	public static List<BlockState> allowedBlocks(List<BlockState> blocks, Block originBlock, Player player) {
		return blocks.stream()
			.filter(blockState -> allowedMove(originBlock, blockState.getBlock(), player))
			.collect(Collectors.toList());
	}
	
	/**
	 * Will return a list of blocks which all either do not have the same town as an owner, 
	 * or do not have the same player as owner. Any block in the wilderness is considered allowed. 
	 * 
	 * @param blocks List&lt;BlockState&gt; which hasn't been filtered yet.
	 * @param originBlock Block from which to test against.
	 * @return List&lt;BlockState&gt; which has been filtered to same-owner and wilderness.
	 */	
	public static List<BlockState> disallowedBlocks(List<BlockState> blocks, Block originBlock) {
		return disallowedBlocks(blocks, originBlock, null);
	}

	/**
	 * Will return a list of blocks which all either do not have the same town as an owner, 
	 * or do not have the same player as owner. Any block in the wilderness is considered allowed.
	 * When Player is not null, player cache will be used to determine build rights. 
	 * 
	 * @param blocks List&lt;BlockState&gt; which hasn't been filtered yet.
	 * @param originBlock Block from which to test against.
	 * @param player Player the player involved in the move, when not null.
	 * @return List&lt;BlockState&gt; which has been filtered to same-owner and wilderness.
	 */	
	public static List<BlockState> disallowedBlocks(List<BlockState> blocks, Block originBlock, Player player) {
		return blocks.stream()
				.filter(blockState -> !allowedMove(originBlock, blockState.getBlock(), player))
				.collect(Collectors.toList());
	}
	
	/**
	 * Decides whether a block is in a same-owner relation ship with the given block.
	 * Wilderness blocks are considered allowed.
	 * 	
	 * @param block Block which is the original.
	 * @param blockTo Block to test the relation to.
	 * @return true if the blocks are considered same-owner or wilderness.
	 */
	public static boolean allowedMove(Block block, Block blockTo) {
		return allowedMove(block, blockTo, null); 
	}
	
	/**
	 * Decides whether a block is in a same-owner relation ship with the given block.
	 * Wilderness blocks are considered allowed. When player isn't null, the player
	 * cache is checked.
	 * 	
	 * @param block Block which is the original.
	 * @param blockTo Block to test the relation to.
	 * @param player Player involved in the move, when not null.
	 * @return true if the blocks are considered same-owner or wilderness or allowed via player cache.
	 */	
	public static boolean allowedMove(Block block, Block blockTo, @Nullable Player player) {
		// Player isn't null, lets test the player cache to see if they are able to build in both areas anyways.
		if (player != null 
			&& PlayerCacheUtil.getCachePermission(player, blockTo.getLocation(), block.getType(), ActionType.BUILD)
			&& PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), ActionType.BUILD))
			return true;

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
