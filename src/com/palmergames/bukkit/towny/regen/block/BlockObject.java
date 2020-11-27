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

	private String key;
	private int typeId = -1;
	private byte data;
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

	/**
	 * @deprecated Use {@link #BlockObject(String)} instead.
	 * @param typeId Block Type ID
	 */
	@Deprecated
	public BlockObject(int typeId) {

		this.typeId = typeId;
		this.data = 0;
	}

	/**
	 * @deprecated Use {@link #BlockObject(String)} instead.
	 * @param typeId Block Type ID
	 * @param loc Location
	 */
	@Deprecated
	public BlockObject(int typeId, Location loc) {

		this.typeId = typeId;
		this.data = 0;
		setLocation(loc);
	}

	/**
	 * @deprecated Use {@link #BlockObject(String)} instead.
	 * @param key Attempt to parse block type id from string.
	 * @param loc Location
	 */
	@Deprecated
	public BlockObject(String key, Location loc) {
		
		try {
			typeId = Integer.parseInt(key);
		} catch(NumberFormatException ignore) {
			this.key = key;
		}
		this.data = 0;
		setLocation(loc);
	}

	/**
	 * @deprecated Use {@link #BlockObject(String)} instead.
	 * @param typeId Block TypeID
	 * @param data Block Data
	 */
	@Deprecated
	public BlockObject(int typeId, byte data) {

		this.typeId = typeId;
		this.data = data;
	}

	/**
	 * @deprecated  Use {@link #BlockObject(String)} instead.
	 * @param key String containing typeId integer.
	 * @param data Block Data.
	 */
	@Deprecated
	public BlockObject(String key, byte data) {
		try {
			typeId = Integer.parseInt(key);
		} catch(NumberFormatException ignore) {
			this.key = key;
		}
		this.data = data;
	}

	/**
	 * @deprecated Use {@link #BlockObject(String)} instead.
	 * @param typeId Block ID
	 * @param data Block Data
	 * @param loc Location
	 */
	@Deprecated
	public BlockObject(int typeId, byte data, Location loc) {

		this.typeId = typeId;
		this.data = data;
		setLocation(loc);
	}

	/**
	 * Use {@link #BlockObject(String)} instead.
	 * 
	 * @param key String containing Block ID
	 * @param data Block Data
	 * @param loc Location
	 *               
	 * @deprecated Use {@link #BlockObject(String)} instead.   
	 */
	@Deprecated
	public BlockObject(String key, byte data, Location loc) {
		try {
			typeId = Integer.parseInt(key);
		} catch(NumberFormatException ignore) {
			this.key = key;
		}
		this.data = data;
		setLocation(loc);
	}

	/**
	 * @deprecated No longer used.
	 * @return If the Block ID > -1.
	 */
	@Deprecated
	public boolean usesID() {
		return typeId  > -1;
	}

	/**
	 * @deprecated No longer used.
	 * @return The {@link #key}
	 */
	@Deprecated
	public String getKey() {
		return key;
	}

	/**
	 * Sets the {@link #key}
	 * @deprecated No longer used.
	 * @param key 
	 */
	@Deprecated
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * @return the id
	 * 
	 * @deprecated You should be using methods that make use of Materials and Blocks directly.   
	 */
	@Deprecated
	public int getTypeId() {

		return typeId;
	}

	/**
	 * @param typeId the id to set
	 *
	 * @deprecated You should be using methods that make use of Materials and Blocks directly.
	 */
	@Deprecated
	public void setTypeId(int typeId) {

		this.typeId = typeId;
	}

	/**
	 * @return the data
	 * 
	 * @deprecated You should be using methods that make use of Materials and Blocks directly.   
	 */
	@Deprecated
	public byte getData() {

		return data;
	}

	/**
	 * @param data the data to set
	 *                
	 * @deprecated You should be using methods that make use of Materials and Blocks directly.   
	 */
	@Deprecated
	public void setData(byte data) {

		this.data = data;
	}

	/**
	 * @param typeId  - the typeId to set
	 * @param data - the data to set
	 *                
	 * @deprecated You should be using methods that make use of Materials and Blocks directly.   
	 */
	@Deprecated
	public void setTypeIdAndData(int typeId, byte data) {

		this.typeId = typeId;
		this.data = data;
	}

}