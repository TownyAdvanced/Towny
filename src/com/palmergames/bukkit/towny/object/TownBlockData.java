package com.palmergames.bukkit.towny.object;

 import com.palmergames.bukkit.towny.TownySettings;
 import org.bukkit.Material;

 import java.util.LinkedHashSet;
 import java.util.Set;

public class TownBlockData {
	private String mapKey = "+";
	private double cost = 0.0;
	private double tax = 0.0;
	private Set<Material> itemUseIds = new LinkedHashSet<>(); // List of item names that will trigger an item use test.
	private Set<Material> switchIds = new LinkedHashSet<>(); // List of item names that will trigger a switch test.
	private Set<Material> allowedBlocks = new LinkedHashSet<>(); // List of item names that will always be allowed.
	
	public String getMapKey() {
		return mapKey;
	}
	
	public void setMapKey(String mapKey) {
		this.mapKey = mapKey.substring(0, 1);
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
	
	public void setItemUseIds(Set<Material> itemUseIds) {
		this.itemUseIds = new LinkedHashSet<>(itemUseIds);
	}
	
	public void setSwitchIds(Set<Material> switchIds) {
		this.switchIds = new LinkedHashSet<>(switchIds);
	}
	
	public void setAllowedBlocks(Set<Material> allowedBlocks) {
		this.allowedBlocks = new LinkedHashSet<>(allowedBlocks);
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
