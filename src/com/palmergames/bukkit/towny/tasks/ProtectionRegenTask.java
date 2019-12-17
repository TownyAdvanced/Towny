package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ProtectionRegenTask extends TownyTimerTask {

	private BlockState state;
	@SuppressWarnings("unused")
	private BlockState altState;
	private BlockLocation blockLocation;
	private int TaskId;
	private ItemStack[] contents;
	private ItemStack[] leftSideContents;
	private ItemStack[] rightSideContents;
	
	//Tekkit - InventoryView

	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
		this.altState = null;
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

		replaceProtections();
		TownyRegenAPI.removeProtectionRegenTask(this);
	}

	public void replaceProtections() {
		
		Block block = state.getBlock();
		
		// Replace physical block.
		try {
			BlockData blockData = state.getBlockData().clone();			
			block.setType(state.getType(), false);
			block.setBlockData(blockData);
		} catch (Exception ex) {
			ex.printStackTrace();
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

	/**
	 * @return the taskId
	 */
	public int getTaskId() {

		return TaskId;
	}

	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(int taskId) {

		TaskId = taskId;
	}
}
