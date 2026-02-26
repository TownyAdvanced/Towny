package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.BlockUtil;
import com.palmergames.bukkit.util.ItemLists;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import java.util.ArrayList;
import java.util.List;

public class TownyBlockListener implements Listener {

	private final Towny plugin;

	public TownyBlockListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();		
		if (!TownyAPI.getInstance().isTownyWorld(block.getWorld()))
			return;

		//Cancel based on whether this is allowed using the PlayerCache and then a cancellable event.
		event.setCancelled(!TownyActionEventExecutor.canDestroy(event.getPlayer(), block.getLocation(), block.getType()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();
		if (!TownyAPI.getInstance().isTownyWorld(block.getWorld()))
			return;

		/*
		 * Allow portals to be made.
		 */
		if (block.getType() == Material.FIRE && block.getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN)
			return;

		//Cancel based on whether this is allowed using the PlayerCache and then a cancellable event.
		if (!TownyActionEventExecutor.canBuild(event.getPlayer(), block.getLocation(), block.getType())) {
			event.setBuild(false);
			event.setCancelled(true);
		}
		
		//noinspection IsCancelled
		if (!event.isCancelled() && block.getType() == Material.CHEST && !TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer()))
			testDoubleChest(event.getPlayer(), event.getBlock());
	}

	private void testDoubleChest(Player player, Block block) {
		List<Block> blocksToUpdate = new ArrayList<>(); // To avoid glitchy-looking chests, we need to update the blocks later on.
		List<WorldCoord> safeWorldCoords = new ArrayList<>(); // Some worldcoords will be concidered safe;

		for (BlockFace face : BlockUtil.CARDINAL_BLOCKFACES) {
			Block testBlock = block.getRelative(face); // The block which we do not want to combine with.

			if (BlockUtil.sameWorldCoord(block, testBlock)) // Same worldCoord, continue;
				continue;

			if (testBlock.getType() != Material.CHEST) // Not a chest, continue.
				continue;

			WorldCoord wc = WorldCoord.parseWorldCoord(testBlock);
			if (safeWorldCoords.contains(wc)) {
				continue;
			}
			Chest data = (Chest) block.getBlockData();            // We are only going to glitch out chests which are facing
			Chest testData = (Chest) testBlock.getBlockData();    // the same direction as our newly-placed chest. 

			if (data.getType() != Chest.Type.SINGLE) // The chest is already a double chest, so it cannot be merged into.
				continue;

			if (!data.getFacing().equals(testData.getFacing())) // The chests aren't facing the same direction. 
				continue;

			if ((data.getFacing().equals(BlockFace.SOUTH) || data.getFacing().equals(BlockFace.NORTH))
					&& block.getZ() != testBlock.getZ()) // The two chests are not on the axis, although they face the same direction.
				continue;
			
			if ((data.getFacing().equals(BlockFace.EAST) || data.getFacing().equals(BlockFace.WEST)) 
					&& block.getX() != testBlock.getX()) // The two chests are not on the axis, although they face the same direction.
				continue;

			if (BlockUtil.sameOwnerOrHasMayorOverride(block, testBlock, player)) { // If the blocks have a same-owner relationship, continue.
				safeWorldCoords.add(wc);
				continue;
			}
			
			blocksToUpdate.add(testBlock); // This chest could potentially snap to the given Block based on proximity and facing.
			
			data.setType(Type.SINGLE);  // Set the chest just-placed to a single chest.
			block.setBlockData(data);
			
			testData.setType(Type.SINGLE); // Set the existing chest to a single chest.
			testBlock.setBlockData(testData);
		}
		
		if (!blocksToUpdate.isEmpty())  // Update the player with the new chest appearances.
			for (Block b : blocksToUpdate)
				player.sendBlockChange(b.getLocation(), b.getBlockData());
	}

	// prevent blocks igniting if within a protected town area when fire spread is set to off.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		event.setCancelled(!TownyActionEventExecutor.canBurn(event.getBlock()));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		event.setCancelled(!TownyActionEventExecutor.canBurn(event.getBlock()));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		if (!canBlockMove(event.getBlock(), event.isSticky() ? event.getBlock().getRelative(event.getDirection().getOppositeFace()) : event.getBlock().getRelative(event.getDirection()), false))
			event.setCancelled(true);

		List<Block> blocks = event.getBlocks();
		
		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (!canBlockMove(block, block.getRelative(event.getDirection()), false))
					event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(event.getBlock().getWorld());
		if (world == null || !world.isUsingTowny())
			return;
		
		boolean allowWild = world.getUnclaimedZoneBuild();

		if (!canBlockMove(event.getBlock(), event.getBlock().getRelative(event.getDirection()), allowWild))
			event.setCancelled(true);
		
		List<Block> blocks = event.getBlocks();

		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (!canBlockMove(block, block.getRelative(event.getDirection()), allowWild))
					event.setCancelled(true);
			}
		}
	}

	/**
	 * Decides whether blocks moved by pistons or fluids flowing follow the rules.
	 * 
	 * @param block - block that is being moved.
	 * @param blockTo - block that is being moved to.
	 * @param allowWild - Whether the block should be allowed to move into the wilderness.
	 * 
	 * @return true if block the block can move.
	 */
	private boolean canBlockMove(Block block, Block blockTo, boolean allowWild) {
		WorldCoord from = WorldCoord.parseWorldCoord(block);
		WorldCoord to = WorldCoord.parseWorldCoord(blockTo);

		// Same WorldCoord, Both are Wilderness, or We allow moving from Town to Wild.
		if (from.equals(to) || (to.isWilderness() && from.isWilderness()) || (allowWild && to.isWilderness()))
			return true;

		TownBlock currentTownBlock = from.getTownBlockOrNull();
		TownBlock destinationTownBlock = to.getTownBlockOrNull();

		// If only one of 'from' and 'to' is wilderness and allowWild is false, this isn't allowed.
		if (currentTownBlock == null || destinationTownBlock == null)
			return false;

		//Both townblocks are owned by the same resident.
		if (currentTownBlock.hasResident() && destinationTownBlock.hasResident() && currentTownBlock.getResidentOrNull() == destinationTownBlock.getResidentOrNull())
			return true;

		//Both townblocks are owned by the same town.
		return currentTownBlock.getTownOrNull() == destinationTownBlock.getTownOrNull() && !currentTownBlock.hasResident() && !destinationTownBlock.hasResident();
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onCreateExplosion(BlockExplodeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getBlock().getWorld());
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;
		
		Material material = event.getBlock().getType();
		/*
		 * event.getBlock() doesn't return the bed when a bed or respawn anchor is the cause of the explosion, so we use this workaround.
		 */
		if (material == Material.AIR && townyWorld.hasBedExplosionAtBlock(event.getBlock().getLocation()))
			material = townyWorld.getBedExplosionMaterial(event.getBlock().getLocation());
		
		List<Block> blocks = TownyActionEventExecutor.filterExplodableBlocks(event.blockList(), material, null, event);
		event.blockList().clear();
		event.blockList().addAll(blocks);

		if (event.blockList().isEmpty())
			return;
		
		/*
		 * Don't regenerate block explosions unless they are on the list of blocks whose explosions regenerate.
		 */
		if (townyWorld.isUsingPlotManagementWildBlockRevert() && townyWorld.isProtectingExplosionBlock(material)) {
			int count = 0;
			for (Block block : event.blockList()) {
				// Only regenerate in the wilderness.
				if (!TownyAPI.getInstance().isWilderness(block))
					continue;
				// Check the white/blacklist
				if (!townyWorld.isExplodedBlockAllowedToRevert(block.getType()))
					continue;
				// Don't start a revert on a block that is going to be reverted.
				if (TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(block.getLocation())))
					continue;
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, townyWorld, event);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFrostWalkerFreezeWater(EntityBlockFormEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()) || !TownySettings.doesFrostWalkerRequireBuildPerms())
			return;

		// Snowmen making snow will also throw this event. 
		if (event.getEntity() instanceof Player player) {
			//Cancel based on whether this is allowed using the PlayerCache and then a cancellable event.
			event.setCancelled(!TownyActionEventExecutor.canBuild(player, event.getBlock().getLocation(), event.getBlock().getType()));
		}
	}
	
	/*
	* Prevents water or lava from going into other people's plots.
	*/
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockFromToEvent(BlockFromToEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		// Prevent liquid spilling in areas that are being reverted to a pre-claim snapshot. 
		if (TownyRegenAPI.hasActiveRegeneration(WorldCoord.parseWorldCoord(event.getBlock())))
			event.setCancelled(true);

		if (!TownySettings.getPreventFluidGriefingEnabled() || event.getBlock().getType() == Material.DRAGON_EGG)
			return;
		
		if (!canBlockMove(event.getBlock(), event.getToBlock(), true))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockDispense(BlockDispenseEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;
		
		if (event.getBlock().getType() != Material.DISPENSER)
			return;
		
		Material mat = event.getItem().getType();

		if (ItemLists.BUCKETS.contains(mat) && !TownySettings.getPreventFluidGriefingEnabled())
			return;
		
		if (!ItemLists.BUCKETS.contains(mat) && mat != Material.BONE_MEAL && mat != Material.HONEYCOMB)
			return;
		
		if (!canBlockMove(event.getBlock(), event.getBlock().getRelative(((Directional) event.getBlock().getBlockData()).getFacing()), true))
			event.setCancelled(true);
	}

	/*
	 * Used to prevent bonemeal and moss growing into areas it shouldn't.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFertilize(BlockFertilizeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;
		
		List<BlockState> allowed = BorderUtil.allowedBlocks(event.getBlocks(), event.getBlock(), event.getPlayer());
		event.getBlocks().clear();
        event.getBlocks().addAll(allowed);
	}

	/*
	 * Used to prevent Sculk Spread.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSculkSpread(BlockSpreadEvent event) {
		String sourceName = event.getSource().getType().getKey().getKey();
		if (!sourceName.startsWith("sculk"))
			return;

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		if (sourceName.equalsIgnoreCase("sculk_catalyst")) {
			// Check if the sculk is passing across a border with differing owners, allowing
			// sculk to spread from a town into the wilderness.
			event.setCancelled(!canBlockMove(event.getSource(), event.getBlock(), true));
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		switch (event.getReason()) {
			case BOTTLE_EMPTY, BUCKET_EMPTY -> event.setCancelled(!TownyActionEventExecutor.canBuild(player, event.getBlock()));
			case BUCKET_FILL, BOTTLE_FILL, ARMOR_WASH, SHULKER_WASH, BANNER_WASH ->
				// Technically the event.getBlock() is WATER_CAULDRON but this doesn't exist as an item that can be
				// loaded for a townblock type's allowedBlocks, which will replace it with a normal CAULDRON.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getBlock().getLocation(), Material.CAULDRON));
			case EXTINGUISH -> {
				if (!TownyActionEventExecutor.canDestroy(player, event.getBlock())) {
					event.setCancelled(true);
					
					// Extinguish the player instead of letting them burn
					if (player.getFireTicks() > 0) {
						player.setFireTicks(0);
						player.getWorld().playSound(player, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.7f, 1.6f);
					}
				}
			}
			default -> {}
		}
	}
}
