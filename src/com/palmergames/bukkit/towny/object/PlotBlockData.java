package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownySettings;

public class PlotBlockData {
	
	private int defaultVersion = 1;
	
	private String worldName;
	private int x, z, size, height, version;

	private List<Integer> blockList = new ArrayList<Integer>(); // Stores the original plot blocks
	private int blockListRestored; // counter for the next block to test
	
	public PlotBlockData(TownBlock townBlock) {
		setX(townBlock.getX());
		setZ(townBlock.getZ());
		setSize(TownySettings.getTownBlockSize());
		this.worldName = townBlock.getWorld().getName();
		this.setVersion(defaultVersion);
		try {
			setHeight(TownyUniverse.plugin.getServerWorld(worldName).getMaxHeight()-1);
		} catch (NotRegisteredException e) {
			setHeight(127);
		}
		this.blockListRestored = 0;
	}
	
	public void initialize() {
		setBlockList(getBlockArr()); //fill array
		resetBlockListRestored();
	}
	
	/**
	 * Fills an array with the current Block types from the plot.
	 * 
	 * @return
	 */
	private List<Integer> getBlockArr() {  
        List<Integer> list = new ArrayList<Integer>();
        Block block = null;
        
        
        try {
        	World world = TownyUniverse.plugin.getServerWorld(worldName);
			
			for (int z = 0; z < size; z++)
	    		for (int x = 0; x < size; x++)
	    			for (int y = height; y > 0; y--) { // Top down to account for falling blocks.
	    				block = world.getBlockAt((getX()*size) + x, y, (getZ()*size) + z);
	    				switch (defaultVersion) {
	    				
	    				case 1:
	    					list.add(block.getTypeId());
	    					list.add((int) block.getData());
	    					break;
	    					
	    				default:
	    					list.add(block.getTypeId());
	    				}
    					
	    			}
	        
		} catch (NotRegisteredException e1) {
			// Failed to fetch world
			e1.printStackTrace();
		}
        
        return list;
    }
	
	/**
	 * Reverts an area to the stored image.
	 * 
	 * @return true if there are more blocks to check.
	 */
	public boolean restoreNextBlock() {
		Block block = null;
		int x, y, z, blockId, reverse, scale;
		int worldx = getX()*size, worldz = getZ()*size;
		blockObject storedData;
		
		try {
			World world = TownyUniverse.plugin.getServerWorld(worldName);
			
			//Scale for the number of elements
			switch (version) {
			
			case 1:
				scale = 2;
				break;
				
			default:
				scale = 1;
			}
				
			reverse = (blockList.size() - blockListRestored) / scale ;
			
			while (reverse > 0) {
				
				reverse--; //regen bottom up to stand a better chance of restoring tree's and plants.
				y = height - (reverse % height);
				x = (int)(reverse/height) % size;
				z = ((int)(reverse/height) / size) % size;

				block = world.getBlockAt(worldx + x, y, worldz + z);					
				blockId = block.getTypeId();
				storedData = getStoredBlockData((blockList.size()-1) - blockListRestored);
				
				// Increment based upon number of elements
				blockListRestored += scale;				
				
				// If this block isn't correct, replace
				// and return as done.
				if ((blockId != storedData.getTypeID())) {
					if (!TownyUniverse.getWorld(worldName).isPlotManagementIgnoreIds(storedData.getTypeID())) {
						
						//System.out.print("regen x: " + x + " y: " + y + " z: " + z + " ID: " + blockId); 
						
						//restore based upon version
						switch (version) {
						
						case 1:
							block.setTypeIdAndData(storedData.getTypeID(), storedData.getData(), false);
							
							break;
						default:
							block.setTypeId(storedData.getTypeID());
						}
						
					} else
						block.setTypeId(0);
						
					return true;
				}
			}
		} catch (NotRegisteredException e1) {
			// Failed to get world.
			e1.printStackTrace();
		}

		// reset as we are finished with the regeneration
		resetBlockListRestored();
		return false;
	}
	
	private blockObject getStoredBlockData(int index) {	
		//return based upon version
		switch (version) {
		
		case 1:
			return new blockObject(blockList.get(index-1), (byte)(blockList.get(index) & 0xff));
			
		default:
			return new blockObject(blockList.get(index), (byte) 0);
		}
			
	}

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public String getWorldName() {
		return worldName;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the blockList
	 */
	public List<Integer> getBlockList() {
		return blockList;
	}

	/**
	 * fills the BlockList
	 * 
	 * @param blockList
	 */
	public void setBlockList(List<Integer> blockList) {
		this.blockList = blockList;
	}

	/**
	 * fills BlockListRestored with zero's to indicate
	 * no blocks have been restored yet 
	 */
	public void resetBlockListRestored() {
		blockListRestored = 0;
	}
	
	
}