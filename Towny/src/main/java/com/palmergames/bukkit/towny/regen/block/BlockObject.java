package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * 
 * @author ElgarL
 * 
 */
public class BlockObject {

	private BlockLocation location;
	private BlockData blockData;
	
	public BlockObject(String key) {
		
		this.blockData = Bukkit.getServer().createBlockData(key);
	}
	
	public Material getMaterial() {
		return this.blockData.getMaterial();
	}
	
	public BlockData getBlockData() {
		return this.blockData;
	}
	
	public void setBlockData(BlockData blockData) {
		this.blockData = blockData;
	}
	
	/**
	 * @return the location
	 */
	public BlockLocation getLocation() {

		return location;
	}

	/**
	 * @param loc the location to set
	 */
	public void setLocation(Location loc) {

		this.location = new BlockLocation(loc);
	}
}