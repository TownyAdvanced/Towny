package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.regen.NeedsPlaceholder;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;

import java.util.ArrayList;
import java.util.List;

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

@SuppressWarnings("deprecation")
public class ProtectionRegenTask extends TownyTimerTask {

	private final BlockState state;
	private BlockLocation blockLocation;
	private int TaskId;
	private List<ItemStack> contents = new ArrayList<ItemStack>();
	private static final Material placeholder = Material.DIRT;
	
	public ProtectionRegenTask(Towny plugin, Block block) {

		super(plugin);
		this.state = block.getState();
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
				if (state.getType().equals(Material.WALL_SIGN)) {
					Block attachedBlock = block.getRelative(oldSign.getAttachedFace());
					if (attachedBlock.getType().equals(Material.AIR)) {
						attachedBlock.setType(placeholder, false);
						TownyRegenAPI.addPlaceholder(attachedBlock);
					}					
				}
				block.setType(state.getType(), false);
				MaterialData signData = state.getData();
				BlockFace facing = ((Directional) state.getData()).getFacing();
				((Directional) signData).setFacingDirection(facing);
				state.setData(signData);
				state.update();

				int i = 0;
				for (String line : ((Sign) state).getLines())
					((Sign) state).setLine(i++, line);

				state.update(true);

			} else if (state instanceof CreatureSpawner) {

				block.setType(Material.MOB_SPAWNER);				
				CreatureSpawner spawner = ((CreatureSpawner)state);
				EntityType type = ((CreatureSpawner) state).getSpawnedType();
				spawner.setSpawnedType(type);
				state.update();

			} else if (state instanceof Chest) {
				
				block.setType(state.getType(), false);
				BlockFace facing = ((Directional) state.getData()).getFacing();
				MaterialData chestData = state.getData(); 
				((Directional) chestData).setFacingDirection(facing);				
				Inventory container = ((Chest) block.getState()).getBlockInventory();				
				container.setContents(contents.toArray(new ItemStack[0]));
				state.setData(chestData);
				state.update();	
			
			} else if (state instanceof ShulkerBox) {
				
				block.setType(state.getType());
				MaterialData shulkerData = state.getData(); 
				Inventory container = ((ShulkerBox) block.getState()).getInventory();
				container.setContents(contents.toArray(new ItemStack[0]));
				state.setData(shulkerData);
				state.update();				
				
			} else if (state instanceof InventoryHolder) {
				
				block.setType(state.getType(), false);				
				BlockFace facing = ((Directional) state.getData()).getFacing();
				MaterialData holderData = state.getData(); 
				((Directional) holderData).setFacingDirection(facing);
				Inventory container = ((InventoryHolder) block.getState()).getInventory();
				container.setContents(contents.toArray(new ItemStack[0]));
				state.setData(holderData);
				state.update();

			} else if (state.getData() instanceof PistonBaseMaterial) {
				/*
				 * Sometimes the pistons dont extend when powered, sometimes they do.
				 * Hopefully the new Data system post 1.13 has better control over this.
				 * 				
				 */
				// TODO: Improve piston protectionregentask code post 1.13/new data system.
				if (block.getType().equals(Material.AIR)) {					
					if (state.getType().equals(Material.PISTON_BASE)) {
						block.setType(Material.PISTON_BASE);
					} else if (state.getType().equals(Material.PISTON_STICKY_BASE)) {
						block.setType(Material.PISTON_STICKY_BASE);			
					}					
					org.bukkit.material.PistonBaseMaterial baseData = (org.bukkit.material.PistonBaseMaterial) state.getData();					
					BlockFace facing = ((Directional) state.getData()).getFacing();
					baseData.setFacingDirection(facing);
					baseData.setPowered(false);
					state.setData(baseData);
					state.update();
				}
				
			} else if (state.getData() instanceof Attachable) {
				Block attachedBlock;
				if (state.getData().getItemType().equals(Material.COCOA)) {
					// For whatever reason (probably a bukkit api bug,) cocoa beans don't return the correct block face to which they are attached to.
					attachedBlock = block.getRelative(((Attachable) state.getData()).getAttachedFace().getOppositeFace());
				} else {
					attachedBlock = block.getRelative(((Attachable) state.getData()).getAttachedFace());
				}
				BlockFace attachedfacing = block.getRelative(((Attachable) state.getData()).getAttachedFace().getOppositeFace()).getFace(block);
				// attachedfacing is used to stop attachables from leaving dirt block placeholders in cases where players 
				// are breaking blocks manually below an attachable, after an explosion has removed the attachable.
				if (attachedBlock.getType().equals(Material.AIR) && !attachedfacing.equals(BlockFace.DOWN)) {
					attachedBlock.setType(placeholder, false);
					TownyRegenAPI.addPlaceholder(attachedBlock);
				}
				block.setType(state.getType());
				BlockFace facing = ((Directional) state.getData()).getFacing();
				MaterialData stateData = state.getData();				
				((Directional) stateData).setFacingDirection(facing);
				state.setData(stateData);
				state.update();
				
			} else if (state.getData() instanceof Tree) {

				block.setType(state.getType());
				Tree stateData = (Tree) state.getData();
				BlockFace facing = ((Tree) state.getData()).getDirection();
				stateData.setDirection(facing);
				state.setData(stateData);
				state.update();
				
			} else if (state.getData() instanceof Stairs) {
			
				block.setType(state.getType());
				Stairs stateData = (Stairs) state.getData();
				BlockFace facing = ((Directional) state.getData()).getFacing().getOppositeFace();
				boolean isInverted = ((Stairs) state.getData()).isInverted();
				((Directional) stateData).setFacingDirection(facing);
				stateData.setInverted(isInverted);				
				state.setData(stateData);
				state.update();
				
			} else if (state.getData() instanceof Gate) {
				
				block.setType(state.getType());
				Gate stateData = (Gate) state.getData();
				BlockFace facing = ((Directional) state.getData()).getFacing();				
				((Directional) stateData).setFacingDirection(facing);
				state.setData(stateData);
				state.update();
				
			} else if (state.getData() instanceof WoodenStep) {
				
				block.setType(state.getType());
				WoodenStep stateData = (WoodenStep) state.getData();
				boolean inverted = ((WoodenStep) state.getData()).isInverted();	
				((WoodenStep) stateData).setInverted(inverted);
				state.setData(stateData);
				state.update();

			} else if (state.getData() instanceof Step) {
				
				block.setType(state.getType());
				Step stateData = (Step) state.getData();
				boolean inverted = ((Step) state.getData()).isInverted();	
				((Step) stateData).setInverted(inverted);
				state.setData(stateData);
				state.update();
			
			} else if (state.getData() instanceof Colorable) {
				
				block.setType(state.getType());
				Colorable stateData = (Colorable) state.getData();
				DyeColor colour = ((Colorable) state.getData()).getColor();
				((Colorable) stateData).setColor(colour);
				state.setData((MaterialData) stateData);
				state.update();

			} else if (state.getData() instanceof LongGrass) {
				
				block.setType(state.getType());
				LongGrass stateData = (LongGrass) state.getData();
				GrassSpecies species =  ((LongGrass) state.getData()).getSpecies();
				((LongGrass) stateData).setSpecies(species);
				state.setData((MaterialData) stateData);
				state.update();
				
			} else if (state.getType().equals(Material.CONCRETE) || state.getType().equals(Material.CONCRETE_POWDER) 
					|| state.getType().equals(Material.STAINED_CLAY) || state.getType().equals(Material.STAINED_GLASS)
					|| state.getType().equals(Material.STAINED_GLASS_PANE) ) {
				// TODO Make this not use bytes for colour after the new api is out in 1.13
				block.setType(state.getType());
				Byte b = state.getRawData();
				state.setRawData(b);
				state.update();

			} else {

				if (NeedsPlaceholder.contains(state.getType())) {
					Block blockBelow = block.getRelative(BlockFace.DOWN);
					if (blockBelow.getType().equals(Material.AIR)) {
						if (state.getType().equals(Material.CROPS)) {
							blockBelow.setType(Material.SOIL, true);
						} else {
							blockBelow.setType(placeholder, true);
						}
						TownyRegenAPI.addPlaceholder(blockBelow);
					}
				}
				if (!state.getType().equals(Material.AIR)) {					
					block.setType(state.getType());
					//state.update();
				}
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
