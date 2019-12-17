package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ProtectionRegenTask extends TownyTimerTask {

	private BlockState state;
	private BlockLocation blockLocation;
	private int TaskId;
	private ItemStack[] items;

	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
		this.setBlockLocation(new BlockLocation(block.getLocation()));

		if (state instanceof BlockInventoryHolder) {
			Inventory inven = ((BlockInventoryHolder) state).getInventory(); 
			items = inven.getContents();				
			inven.clear();				
		}
	}

	@Override
	public void run() {

		replaceProtections();
		TownyRegenAPI.removeProtectionRegenTask(this);
	}

	public void replaceProtections() {
		
		Block block = state.getBlock();
		try {
			BlockData blockData = state.getBlockData().clone();			
			block.setType(state.getType(), false);
			block.setBlockData(blockData);
			if (block.getState() instanceof BlockInventoryHolder)
				((BlockInventoryHolder) block.getState()).getInventory().setContents(items);
		} catch (Exception ex) {
			ex.printStackTrace();
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
