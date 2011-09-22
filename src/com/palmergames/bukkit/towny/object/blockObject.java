package com.palmergames.bukkit.towny.object;

public class blockObject {
	private int TypeID;
	private byte Data;
	
	blockObject (int typeID, byte data) {
		TypeID = typeID;
		Data = data;
	}
	
	/**
	 * @return the typeID
	 */
	public int getTypeID() {
		return TypeID;
	}
	
	/**
	 * @return the Data
	 */
	public byte getData() {
		return Data;
	}
	
	/**
	 * @param typeID the typeID to set
	 */
	public void setTypeIdAndData(int typeID, byte data) {
		TypeID = typeID;
		Data = data;
	}
	
	
	
	
}