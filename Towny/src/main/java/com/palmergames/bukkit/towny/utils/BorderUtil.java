package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
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
		
		Resident resident = player != null ? TownyAPI.getInstance().getResident(player) : null;

		// Player is trusted in one of the townblocks but not the other
		if (resident != null && currentTownBlock.hasTrustedResident(resident) && !destinationTownBlock.hasTrustedResident(resident) && destinationTownBlock.getResidentOrNull() != resident)
			return false;

		// Both townblocks are owned by the same resident.
		if (currentTownBlock.hasResident() && destinationTownBlock.hasResident() 
			&& currentTownBlock.getResidentOrNull() == destinationTownBlock.getResidentOrNull())
			return true;

		// Both townblocks are owned by the same town.
		return currentTownBlock.getTownOrNull() == destinationTownBlock.getTownOrNull() 
			&& !currentTownBlock.hasResident() && !destinationTownBlock.hasResident();
		
	}

	/**
	 * Decides whether a copper golem can move an item from one block to another.
	 * 
	 * @param blockLoc   Location (of the Copper Chest) where the item originated from.
	 * @param blockToLoc Location (of the Normal Chest) where the item is moving to.
	 * @return true if the blocks are considered same-owner.
	 */	
	public static boolean allowedCopperGolemMove(Location blockLoc, Location blockToLoc) {

		if(!WorldCoord.cellChanged(blockLoc, blockToLoc))
			return true;
		WorldCoord from = WorldCoord.parseWorldCoord(blockLoc);
		WorldCoord to = WorldCoord.parseWorldCoord(blockToLoc);

		// One side is wilderness and the other is not.
		if (from.hasTownBlock() != to.hasTownBlock())
			return false;

		if (from.isWilderness() && to.isWilderness())
			return true;

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

	private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

	@ApiStatus.Internal
	public static @NotNull FloodfillResult getFloodFillableCoords(final @NotNull Town town, final @NotNull WorldCoord origin) {
		final TownyWorld originWorld = origin.getTownyWorld();
		if (originWorld == null)
			return FloodfillResult.fail(null);

		if (origin.hasTownBlock())
			return FloodfillResult.fail(Translatable.of("msg_err_floodfill_not_in_wild"));

		// Filter out any coords not in the same world
		final Set<WorldCoord> coords = new HashSet<>(town.getTownBlockMap().keySet());
		coords.removeIf(coord -> !originWorld.equals(coord.getTownyWorld()));
		if (coords.isEmpty())
			return FloodfillResult.fail(null);

		int minX = origin.getX();
		int maxX = origin.getX();
		int minZ = origin.getZ();
		int maxZ = origin.getZ();

		// Establish a min and max X & Z to avoid possibly looking very far
		for (final WorldCoord coord : coords) {
			minX = Math.min(minX, coord.getX());
			maxX = Math.max(maxX, coord.getX());
			minZ = Math.min(minZ, coord.getZ());
			maxZ = Math.max(maxZ, coord.getZ());
		}

		final Set<WorldCoord> valid = new HashSet<>();
		final Set<WorldCoord> visited = new HashSet<>();

		final Queue<WorldCoord> queue = new LinkedList<>();
		queue.offer(origin);
		visited.add(origin);

		while (!queue.isEmpty()) {
			final WorldCoord current = queue.poll();

			valid.add(current);

			for (final int[] direction : DIRECTIONS) {
				final int xOffset = direction[0];
				final int zOffset = direction[1];

				final WorldCoord candidate = current.add(xOffset, zOffset);

				if (!coords.contains(candidate) && (candidate.getX() >= maxX || candidate.getX() <= minX || candidate.getZ() >= maxZ || candidate.getZ() <= minZ)) {
					return FloodfillResult.oob();
				}

				final TownBlock townBlock = candidate.getTownBlockOrNull();

				// Fail if we're touching another town
				if (townBlock != null && townBlock.hasTown() && !town.equals(townBlock.getTownOrNull())) {
					return FloodfillResult.fail(Translatable.of("msg_err_floodfill_cannot_contain_towns"));
				}

				if (townBlock == null && !visited.contains(candidate) && !coords.contains(candidate)) {
					queue.offer(candidate);
					visited.add(candidate);
				}
			}
		}

		return FloodfillResult.success(valid);
	}

	public record FloodfillResult(@NotNull Type type, @Nullable Translatable feedback, @NotNull Collection<WorldCoord> coords) {
		public enum Type {
			SUCCESS,
			FAIL,
			OUT_OF_BOUNDS
		}

		static FloodfillResult fail(final @Nullable Translatable feedback) {
			return new FloodfillResult(Type.FAIL, feedback, Collections.emptySet());
		}

		static FloodfillResult oob() {
			return new FloodfillResult(Type.OUT_OF_BOUNDS, Translatable.of("msg_err_floodfill_out_of_bounds"), Collections.emptySet());
		}

		static FloodfillResult success(final Collection<WorldCoord> coords) {
			return new FloodfillResult(Type.SUCCESS, null, coords);
		}
	}
}
