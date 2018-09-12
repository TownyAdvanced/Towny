package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.regen.NeedsPlaceholder;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import org.bukkit.DyeColor;
import org.bukkit.GrassSpecies;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Colorable;
import org.bukkit.material.Directional;
import org.bukkit.material.Door;
import org.bukkit.material.Gate;
import org.bukkit.material.LongGrass;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.Stairs;
import org.bukkit.material.Step;
import org.bukkit.material.Tree;
import org.bukkit.material.WoodenStep;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ProtectionRegenTask extends TownyTimerTask {

	private BlockState state;
	@SuppressWarnings("unused")
	private BlockState altState;
	private BlockLocation blockLocation;
	private int TaskId;
	private List<ItemStack> contents = new ArrayList<ItemStack>();
	
	//Tekkit - InventoryView

	private static final Material placeholder = Material.DIRT;

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
