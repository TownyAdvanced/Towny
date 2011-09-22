package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.material.Attachable;
import org.bukkit.material.Door;
import org.bukkit.material.PistonExtensionMaterial;

import com.palmergames.bukkit.towny.object.BlockLocation;
import com.palmergames.bukkit.towny.object.NeedsPlaceholder;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class ProtectionRegenTask extends TownyTimerTask {
    
    private BlockState state;
    private BlockState altState;
    private BlockLocation blockLocation;
    private int TaskId;
    
    private static final Material placeholder = Material.DIRT;

    public ProtectionRegenTask(TownyUniverse universe, Block block, boolean update) {
        super(universe);
        this.state = block.getState();
        this.altState = null;
        this.setBlockLocation(new BlockLocation(block.getLocation()));
        
        if (update)
	        if(state.getData() instanceof Door) {
	            Door door = (Door)state.getData();
	            Block topHalf;
	            Block bottomHalf;
	            if(door.isTopHalf()) {
	                topHalf = block;
	                bottomHalf = block.getRelative(BlockFace.DOWN);
	            } else {
	                bottomHalf = block;
	                topHalf = block.getRelative(BlockFace.UP);
	            }
	            bottomHalf.setTypeId(0);
	            topHalf.setTypeId(0);
	        } else if(state.getData() instanceof PistonExtensionMaterial) {
	            PistonExtensionMaterial extension = (PistonExtensionMaterial)state.getData();
	            Block piston = block.getRelative(extension.getAttachedFace());
	            if(piston.getTypeId() != 0) {
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
        universe.removeProtectionRegenTask(this);
    }
    
    public void replaceProtections() {
        Block block = state.getBlock();
        if(state.getData() instanceof Door) {
            Door door = (Door)state.getData();
            Block topHalf;
            Block bottomHalf;
            if(door.isTopHalf()) {
                topHalf = block;
                bottomHalf = block.getRelative(BlockFace.DOWN);
            } else {
                bottomHalf = block;
                topHalf = block.getRelative(BlockFace.UP);
            }
            door.setTopHalf(true);
            topHalf.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
            door.setTopHalf(false);
            bottomHalf.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
        } else if(state instanceof Sign) {
            block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
            Sign sign = (Sign)block.getState();
            int i = 0;
            for(String line : ((Sign)state).getLines())
                sign.setLine(i++, line);
        } else if(state.getData() instanceof PistonExtensionMaterial) {
            PistonExtensionMaterial extension = (PistonExtensionMaterial)state.getData();
            Block piston = block.getRelative(extension.getAttachedFace());
            block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
            if(altState != null) {
                piston.setTypeIdAndData(altState.getTypeId(), altState.getData().getData(), false);
            }
        } else if(state.getData() instanceof Attachable) {
            Block attachedBlock = block.getRelative(((Attachable)state.getData()).getAttachedFace());
            if(attachedBlock.getTypeId() == 0) {
                attachedBlock.setTypeId(placeholder.getId(), false);
                universe.addPlaceholder(attachedBlock);
            }
            block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), false);
        } else {
            if(NeedsPlaceholder.contains(state.getType())) {
                Block blockBelow = block.getRelative(BlockFace.DOWN);
                if(blockBelow.getTypeId() == 0) {
                    if(state.getType().equals(Material.CROPS)) {
                        blockBelow.setTypeId(Material.SOIL.getId(), true);
                    } else {
                        blockBelow.setTypeId(placeholder.getId(), true);
                    }
                    universe.addPlaceholder(blockBelow);
                }
            }
            block.setTypeIdAndData(state.getTypeId(), state.getData().getData(), !NeedsPlaceholder.contains(state.getType()));
        }
        universe.removePlaceholder(block);
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
