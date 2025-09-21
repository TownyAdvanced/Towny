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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ProtectionRegenTask extends TownyTimerTask {

	private final BlockState state;
	private BlockLocation blockLocation;
	private ScheduledTask task;
	private ItemStack[] contents;

	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
		this.setBlockLocation(new BlockLocation(block.getLocation()));
		
		// If the block has an inventory it implements the BlockInventoryHolder interface.
		if (state instanceof BlockInventoryHolder) {
			
			// Cast the block to the interface representation.
			BlockInventoryHolder container = (BlockInventoryHolder) state;
			
			// Capture inventory.
			Inventory inventory = container.getInventory();
			
			// Chests are special.
			if (state instanceof Chest) {
				inventory = ((Chest) state).getBlockInventory();
			}

			// Copy the contents over.
			contents = inventory.getContents().clone();
			
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
		
		// If the state is a creature spawner, then replace properly.
		if (state instanceof CreatureSpawner) {
			// Up cast to interface.
			CreatureSpawner spawner = (CreatureSpawner) state;
			
			// Capture spawn type and set it.
			EntityType type = spawner.getSpawnedType();
			((CreatureSpawner) state).setSpawnedType(type);

			// update blocks.
			state.update();
		}
		
		// Add inventory back to the block if it conforms to BlockInventoryHolder.
		if (state instanceof BlockInventoryHolder) {
			// Up cast to interface.
			BlockInventoryHolder container = (BlockInventoryHolder) state;
			
			// Check for chest.
			if (container instanceof Chest) {
				((Chest) state).getBlockInventory().setContents(contents);
			} else {
				((BlockInventoryHolder) state).getInventory().setContents(contents);
			}
			
			// update blocks.
			state.update();
		}
		
		if (state instanceof Banner) {
			Banner banner = (Banner) state;
			
			((Banner) state).setPatterns(banner.getPatterns());
			
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
