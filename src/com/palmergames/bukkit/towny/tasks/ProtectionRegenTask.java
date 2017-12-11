package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.regen.NeedsPlaceholder;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Directional;
import org.bukkit.material.Door;
import org.bukkit.material.MaterialData;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ProtectionRegenTask extends TownyTimerTask {

	private BlockState state;
	private BlockState altState;
	private BlockLocation blockLocation;
	private int TaskId;
	private List<ItemStack> contents = new ArrayList<ItemStack>();
	
	//Tekkit - InventoryView

	private static final Material placeholder = Material.DIRT;

	public ProtectionRegenTask(Towny plugin, Block block, boolean update) {

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
		
		if (update)
			if (state.getData() instanceof Door) {
				Door door = (Door) state.getData();
				Block topHalf;
				Block bottomHalf;
				if (door.isTopHalf()) {
					topHalf = block;
					bottomHalf = block.getRelative(BlockFace.DOWN);
				} else {
					bottomHalf = block;
					topHalf = block.getRelative(BlockFace.UP);
				}
				bottomHalf.setTypeId(0);
				topHalf.setTypeId(0);
			} else if (state.getData() instanceof PistonExtensionMaterial) {
				PistonExtensionMaterial extension = (PistonExtensionMaterial) state.getData();
				Block piston = block.getRelative(extension.getAttachedFace());
				if (piston.getTypeId() != 0) {
					this.altState = piston.getState();
					piston.setTypeId(0, false);
				}
				block.setTypeId(0, false);
			} else {
				block.setTypeId(0, false);
			}
	}

	@Override
	public void run() {

		replaceProtections();
		TownyRegenAPI.removeProtectionRegenTask(this);
	}

	public void replaceProtections() {

		try {

			Block block = state.getBlock();

			if (state.getData() instanceof Door) {
				
				Door door = (Door) state.getData();
								
				BlockFace face = null;
                boolean isOpen = false;
                boolean isHinge = false;
				Block topHalf = null;
				Block bottomHalf = null;

				if (door.isTopHalf()) {
					topHalf = block;
					bottomHalf = block.getRelative(BlockFace.DOWN);
				} else {
					bottomHalf = block;
					topHalf = block.getRelative(BlockFace.UP);
				}

				if (!door.isTopHalf()) {
					
					// Gather old door's material data from lower door block
					isOpen = door.isOpen(); 					
					face = door.getFacing();
					
					bottomHalf.setType(state.getType(), false);
					
					// Placeholder topblock, required or double doors lower blocks will break before the top blocks can be regenerated.
					topHalf.setType(state.getType(), false);
					BlockState topHalfState = topHalf.getState();
					Door topHalfData = (Door) topHalfState.getData();
					topHalfData.setTopHalf(true);
					topHalfState.setData(topHalfData);
					topHalfState.update();
									
					// Set lower door block Material Data. 
					BlockState bottomHalfState = bottomHalf.getState();
					Door bottomHalfData = (Door) bottomHalfState.getData();
					bottomHalfData.setOpen(isOpen);
					bottomHalfData.setFacingDirection(face);
					bottomHalfState.setData(bottomHalfData);
					bottomHalfState.update();

				} else {
					
					topHalf.setType(state.getType(), false);
					BlockState topHalfState = topHalf.getState();
					Door topHalfData = (Door) topHalfState.getData();
					// Gather last part of the Material Data from upper door block.
					isHinge = door.getHinge();
					// Gather previous parts of Material Data from lower door block.
					Door otherdoor = (Door) topHalf.getRelative(BlockFace.DOWN).getState().getData();
					isOpen = otherdoor.isOpen();
					face = otherdoor.getFacing();					
					// Set top block Material Data.
					topHalfData.setFacingDirection(face);
					topHalfData.setOpen(isOpen);
					topHalfData.setHinge(isHinge);
					topHalfData.setTopHalf(true);
					topHalfState.setData(topHalfData);
					topHalfState.update();

				}

			} else if (state instanceof Sign) {

				org.bukkit.material.Sign oldSign = (org.bukkit.material.Sign) state.getData();
				Block sign = block;
				
				if (state.getType().equals(Material.WALL_SIGN)) {
					Block attachedBlock = block.getRelative(oldSign.getAttachedFace());
					if (attachedBlock.getType().equals(Material.AIR)) {
						attachedBlock.setType(placeholder, false);
						TownyRegenAPI.addPlaceholder(attachedBlock);
					}					
				}
				sign.setType(state.getType(), false);				
				BlockState signState = sign.getState();
				
				org.bukkit.material.Sign signData = (org.bukkit.material.Sign) signState.getData();					

				BlockFace facing = ((Directional) oldSign).getFacing();				
				((Directional) signData).setFacingDirection(facing);					
				signState.setData((MaterialData) signData);
				signState.update();

				int i = 0;
				for (String line : ((Sign) state).getLines())
					((Sign) state).setLine(i++, line);

				state.update(true);

			} else if (state instanceof CreatureSpawner) {

				block.setType(Material.MOB_SPAWNER);
				BlockState blockState = block.getState();
				CreatureSpawner spawner = ((CreatureSpawner)blockState);
				EntityType type = ((CreatureSpawner) state).getSpawnedType();
				spawner.setSpawnedType(type);
				blockState.update();

			} else if (state instanceof InventoryHolder) {

				block.setTypeId(state.getTypeId(), false);

				Inventory container;
				if (state instanceof Chest) {
					container = ((Chest) block.getState()).getBlockInventory();
				} else {
					container = ((InventoryHolder) block.getState()).getInventory();
				}
				container.setContents(contents.toArray(new ItemStack[0]));

				block.setData(state.getData().getData(), false);

			} else if (state.getData() instanceof PistonExtensionMaterial) {

				PistonExtensionMaterial extension = (PistonExtensionMaterial) state.getData();
				Block piston = block.getRelative(extension.getAttachedFace());
				block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
				if (altState != null) {
					piston.setTypeIdAndData(altState.getTypeId(), altState.getData().getData(), false);
				}
			} else if (state.getData() instanceof Attachable) {
				
				Block attachedBlock;
				if (state.getData().getItemType().equals(Material.COCOA)) {
					// For whatever reason (probably a bukkit api bug,) cocoa beans don't return the correct block face to which they are attached to.
					attachedBlock = block.getRelative(((Attachable) state.getData()).getAttachedFace().getOppositeFace());
				} else {
					attachedBlock = block.getRelative(((Attachable) state.getData()).getAttachedFace());
				}
				if (attachedBlock.getTypeId() == 0) {
					attachedBlock.setTypeId(placeholder.getId(), false);
					TownyRegenAPI.addPlaceholder(attachedBlock);
				}
				block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);

			} else {

				if (NeedsPlaceholder.contains(state.getType())) {
					Block blockBelow = block.getRelative(BlockFace.DOWN);
					if (blockBelow.getTypeId() == 0) {
						if (state.getType().equals(Material.CROPS)) {
							blockBelow.setTypeId(Material.SOIL.getId(), true);
						} else {
							blockBelow.setTypeId(placeholder.getId(), true);
						}
						TownyRegenAPI.addPlaceholder(blockBelow);
					}
				}
				block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), !NeedsPlaceholder.contains(state.getType()));
			}
			TownyRegenAPI.removePlaceholder(block);

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
