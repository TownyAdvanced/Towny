package com.palmergames.bukkit.towny.object;

 import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownySettings;

import org.bukkit.Material;

 import java.util.EnumSet;
 import java.util.Set;

public class TownBlockData {
	private String mapKey = "+";
	private double cost = 0.0;
	private double tax = 0.0;
	private EnumSet<Material> itemUseIds = EnumSet.noneOf(Material.class); // List of item names that will trigger an item use test.
	private EnumSet<Material> switchIds = EnumSet.noneOf(Material.class); // List of item names that will trigger a switch test.
	private EnumSet<Material> allowedBlocks = EnumSet.noneOf(Material.class); // List of item names that will always be allowed.
	
	public String getMapKey() {
		return mapKey;
	}
	
	public void setMapKey(String mapKey) {
		this.mapKey = TownyAsciiMap.parseSymbol(mapKey);
	}
	
	public double getCost() {
		return cost;
	}

	/**
	 * Sets how much it costs for a player to set to plot to this type.
	 * @param cost The cost
	 */
	public void setCost(double cost) {
		this.cost = cost;
	}

	public Set<Material> getItemUseIds() {
		if (itemUseIds.isEmpty())
			return TownySettings.getItemUseMaterials();
		else
			return itemUseIds;
	}

	public Set<Material> getSwitchIds() {
		if (switchIds.isEmpty())
			return TownySettings.getSwitchMaterials();
		else
			return switchIds;
	}

	public Set<Material> getAllowedBlocks() {
		return allowedBlocks;
	}
	
	public void setItemUseIds(EnumSet<Material> itemUseIds) {
		this.itemUseIds = EnumSet.copyOf(itemUseIds);
	}
	
	public void setSwitchIds(EnumSet<Material> switchIds) {
		this.switchIds = EnumSet.copyOf(switchIds);
	}
	
	public void setAllowedBlocks(EnumSet<Material> allowedBlocks) {
		this.allowedBlocks = EnumSet.copyOf(allowedBlocks);
	}

	public void setTax(double tax) {
		this.tax = tax;
	}

	public double getTax(Town town) {
		if (tax == 0)
			return town.getPlotTax();
		else
			return tax;
	}
}
