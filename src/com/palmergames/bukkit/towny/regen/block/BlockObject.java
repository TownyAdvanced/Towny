package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.Location;

/**
 * 
 * @author ElgarL
 * 
 */
public class BlockObject {

	private int typeId;
	private byte data;
	private BlockLocation location;

	public BlockObject(int typeId) {

		this.typeId = typeId;
		this.data = 0;
	}
	
	public BlockObject(int typeId, Location loc) {

		this.typeId = typeId;
		this.data = 0;
		setLocation(loc);
	}

	public BlockObject(int typeId, byte data) {

		this.typeId = typeId;
		this.data = data;
	}
	
	public BlockObject(int typeId, byte data, Location loc) {

		this.typeId = typeId;
		this.data = data;
		setLocation(loc);
	}

	/**
	 * @return the id
	 */
	public int getTypeId() {

		return typeId;
	}

	/**
	 * @param typeId the id to set
	 */
	public void setTypeId(int typeId) {

		this.typeId = typeId;
	}

	/**
	 * @return the data
	 */
	public byte getData() {

		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(byte data) {

		this.data = data;
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
	 * @param typeId the typeId to set
	 */
	public void setTypeIdAndData(int typeId, byte data) {

		this.typeId = typeId;
		this.data = data;
	}

}