package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import net.coreprotect.CoreProtect;

import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;

public class ProtectionRegenTask extends TownyTimerTask {

	private final BlockState state;
	private BlockLocation blockLocation;
	private ScheduledTask task;

	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
		this.setBlockLocation(new BlockLocation(block.getLocation()));
		
		// If the block has an inventory it implements the BlockInventoryHolder interface.
		if (block.getState(false) instanceof BlockInventoryHolder container) { // Intentionally get the state without a snapshot so we can be sure the inventory is cleared
			Inventory inventory = container.getInventory();
			
			// Chests are special.
			if (state instanceof Chest chest) {
				inventory = chest.getBlockInventory();
			}
			
			// Clear the inventory so no items drops and causes dupes.
			inventory.clear();
		}
	}

	@Override
	public void run() {

		this.state.getWorld().getChunkAtAsync(this.state.getLocation()).thenRun(() -> plugin.getScheduler().run(this.state.getLocation(), this::replaceProtections));
		
		TownyRegenAPI.removeProtectionRegenTask(this);
	}

	public void replaceProtections() {
		// Don't replace blocks if a new block has appeared which should not be
		// overwritten with the pre-explosion block. ie: death-chests.
		if (unreplaceableBlockHasAppeared())
			return;
		
		Block block = state.getBlock();

		final boolean logWithCoreProtect = TownySettings.coreProtectSupport() && PluginIntegrations.getInstance().isPluginEnabled("CoreProtect");

		if (logWithCoreProtect && !block.getType().isAir()) {
			CoreProtect.getInstance().getAPI().logRemoval("#towny", block.getLocation(), block.getType(), block.getBlockData());
		}
		
		// Replace physical block.
		BlockData blockData = state.getBlockData().clone();
		block.setType(state.getType(), false);
		block.setBlockData(blockData);
		
		if (logWithCoreProtect && !state.getType().isAir()) {
			CoreProtect.getInstance().getAPI().logPlacement("#towny", state.getLocation(), state.getType(), blockData);
		}
		
		if (state instanceof CreatureSpawner || state instanceof Banner || state instanceof Sign || state instanceof BlockInventoryHolder) {
			state.update();
		}
	}

	private boolean unreplaceableBlockHasAppeared() {
		Block block = blockLocation.getBlock();
		// We should only skip replacement when we're in the wilderness and not air.
		if (block.getType() == Material.AIR || !TownyAPI.getInstance().isWilderness(block))
			return false;

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(blockLocation.getWorld());
		return world != null && world.isMaterialNotAllowedToBeOverwrittenByWildRevert(block.getType());
	}


	/**
	 * @return the blockLocation
	 */
	public BlockLocation getBlockLocation() {

		return blockLocation;
	}

	/**
	 * @param blockLocation the blockLocation to set
	 */
	private void setBlockLocation(BlockLocation blockLocation) {

		this.blockLocation = blockLocation;
	}

	public BlockState getState() {

		return state;
	}

	public ScheduledTask getTask() {
		return this.task;
	}
	
	public void setTask(ScheduledTask task) {
		this.task = task;
	}
}
