package com.palmergames.bukkit.towny.object;

import org.apache.commons.lang.StringUtils;

/**
 * @author Suneet Tipirneni (Siris)
 * A simple class which encapsulates the grouping of townblocks.
 */
public class PlotGroup {
	private int id;
	private String name;
	private double price = -1;

	/**
	 * 
	 * @param id A unique identifier (from the context of the parent town) of the plot group id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 */
	public PlotGroup(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public PlotGroup(int id, String name, double price) {
		this.id = id;
		this.name = name;
		this.price = price;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * Determines whether a group is equivalent or not.
	 * @param obj The group to be compared
	 * @return Returns true if the id of the group matches the given group object.
	 * False otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PlotGroup) {
			return ((PlotGroup) obj).id == this.id;
		}
		return false;
	}

	@Override
	public String toString() {
		return name + "," + id + "," + price;
	}
	
	public static PlotGroup fromString(String str) {
		String[] fields = str.split(",");
		return new PlotGroup(Integer.parseInt(fields[1]), fields[0], Double.parseDouble(fields[2]));
	}

	/**
	 *
	 * @return The qualified resident mode string.
	 */
	public String toModeString() {
		return "Group{" + this.toString() + "}";
	}

	/**
	 * 
	 * @return The plot group given from the mode string.
	 */
	public static PlotGroup fromModeString(String modeStr) {
		String objString = StringUtils.substringBetween(modeStr, "{", "}");
		return PlotGroup.fromString(objString);
	}

	public double getPrice() {
		return price;
	}
	
	public void addPlotPrice(double pPrice) {
		if (getPrice() == -1) {
			this.price = pPrice;
			return;
		}
		
		this.price += pPrice;
	}
}
