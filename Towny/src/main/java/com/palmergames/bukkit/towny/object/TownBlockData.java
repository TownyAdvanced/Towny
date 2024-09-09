package com.palmergames.bukkit.towny.object;

 import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownySettings;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
 import java.util.EnumSet;
 import java.util.LinkedHashSet;
 import java.util.Set;

public class TownBlockData {
	private String mapKey = "+";
	private NamedTextColor colour = null;
	private double cost = 0.0;
	private double tax = 0.0;
	private final Set<Material> itemUseIds = new LinkedHashSet<>(); // List of item names that will trigger an item use test.
	private final Set<Material> switchIds = new LinkedHashSet<>(); // List of item names that will trigger a switch test.
	private final Set<Material> allowedBlocks = new LinkedHashSet<>(); // List of item names that will always be allowed.
	
	public String getMapKey() {
		return mapKey;
	}
	
	public void setMapKey(String mapKey) {
		this.mapKey = TownyAsciiMap.parseSymbol(mapKey);
	}
	
	public boolean hasColour() {
		return colour != null;
	}

	@Nullable
	public NamedTextColor getColour() {
		return colour;
	}

	public void setColour(NamedTextColor colour) {
		this.colour = colour;
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
	
	public void setItemUseIds(Collection<Material> itemUseIds) {
		this.itemUseIds.clear();
		this.itemUseIds.addAll(itemUseIds);
	}
	
	public void setSwitchIds(Collection<Material> switchIds) {
		this.switchIds.clear();
		this.switchIds.addAll(switchIds);
	}
	
	public void setAllowedBlocks(Collection<Material> allowedBlocks) {
		this.allowedBlocks.clear();
		this.allowedBlocks.addAll(allowedBlocks);
	}

	// These bridge methods were added during 0.99.2.*
	@SuppressWarnings({"unused", "unchecked"})
	private void setItemUseIds$$bridge$$public(EnumSet<?> itemUseIds) {
		setItemUseIds((Collection<Material>) itemUseIds);
	}

	@SuppressWarnings({"unused", "unchecked"})
	private void setSwitchIds$$bridge$$public(EnumSet<?> switchIds) {
		setSwitchIds((Collection<Material>) switchIds);
	}

	@SuppressWarnings({"unused", "unchecked"})
	private void setAllowedBlocks$$bridge$$public(EnumSet<?> allowedBlocks) {
		setAllowedBlocks((Collection<Material>) allowedBlocks);
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
