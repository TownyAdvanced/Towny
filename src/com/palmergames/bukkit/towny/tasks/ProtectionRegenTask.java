package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
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
	private List<ItemStack> contents = new ArrayList<ItemStack>();

	//Tekkit - InventoryView

	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
		this.altState = null;
		this.setBlockLocation(new BlockLocation(block.getLocation()));

		if (state instanceof InventoryHolder) {
			Inventory inven;

			if (state instanceof Chest) {
				inven = ((Chest) state).getBlockInventory();
			} else {
				// Contents we are respawning.
				inven = ((InventoryHolder) state).getInventory();
			}

			for (ItemStack item : inven.getContents()) {
				contents.add((item != null) ? item.clone() : null);
			}

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
		} catch (Exception ex) {
			ex.printStackTrace();
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
		
		/* Add inventory back to the block if it conforms to BlockInventoryHolder.
		*
		* Unusable on 1.13
		* 
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
		 */
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
