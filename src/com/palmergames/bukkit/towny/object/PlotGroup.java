package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Suneet Tipirneni (Siris)
 * A simple class which encapsulates the grouping of townblocks.
 */
public class PlotGroup extends Group {
	private List<TownBlock> townBlocks;
	private double price;
	private Town town;

	/**
	 * @param id   A unique identifier for the group id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 */
	public PlotGroup(int id, String name) {
		super(id, name);
	}
	
	public PlotGroup(String townName) {
		super(-1, null);
		this.town = new Town(townName);
	}

	public static PlotGroup fromString(String str) {
		String[] fields = str.split(",");
		String name = fields[0];
		int id = Integer.parseInt(fields[1]);
		double price = Double.parseDouble(fields[2]);
		
		PlotGroup newGroup = new PlotGroup(id, name);
		newGroup.setPrice(price);
		
		return newGroup;
	}

	@Override
	public String toString() {
		return super.toString() + "," + getPrice() + ",";
	}
	
	public void setTown(Town town) {
		
		this.town = town;
		
		try {
			town.addPlotGroup(this);
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
	}
	
	public void setTownBlocks(String str) {
		
	}
	
	public Town getTown() {
		return town;
	}

	/**
	 *
	 * @return The qualified resident mode string.
	 */
	public String toModeString() {
		return "Group{" + this.toString() + "}";
	}

	/**
	 * @param modeStr The string in the resident mode format.
	 * @return The plot group given from the mode string.
	 */
	public static PlotGroup fromModeString(String modeStr) {
		String objString = StringUtils.substringBetween(modeStr, "{", "}");
		return PlotGroup.fromString(objString);
	}

	public double getPrice() {
		return price;
	}
	
	public void addTownBlock(TownBlock townBlock) {
		if (townBlocks == null)
			townBlocks = new ArrayList<>();
		
		townBlocks.add(townBlock);
	}
	
	public List<TownBlock> getTownBlocks() {
		return townBlocks;
	}
	
	public void setPrice(double price) {
		this.price = price;
	}
	
	public void addPlotPrice(double pPrice) {
		if (getPrice() == -1) {
			this.price = pPrice;
			return;
		}
		
		this.price += pPrice;
	}
}
