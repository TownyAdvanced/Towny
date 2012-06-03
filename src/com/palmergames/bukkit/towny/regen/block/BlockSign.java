/**
 * 
 */
package com.palmergames.bukkit.towny.regen.block;

/**
 * @author ElgarL
 * 
 */
public class BlockSign extends BlockObject {

	private String[] lines;
	
	/**
	 * Constructor for a sign with text.
	 * 
	 * @param type
	 * @param data
	 * @param text
	 */
	public BlockSign(int type, byte data, String[] lines) {

		super(type, data);
		this.lines = lines;
	}

	/**
	 * Constructor for a sign without text.
	 * 
	 * @param type
	 * @param data
	 */
	public BlockSign(int type, byte data) {

		super(type, data);
		this.lines = new String[] { "", "", "", "" };
	}

	/**
	 * @return the text
	 */
	public String[] getLines() {

		return lines;
	}

	/**
	 * @param text the text to set
	 */
	public void setlines(String[] lines) {

		this.lines = lines;
	}
}
