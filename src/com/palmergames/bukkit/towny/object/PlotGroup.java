package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * @author Suneet Tipirneni (Siris)
 * A simple class which encapsulates the grouping of townblocks.
 */
public class PlotGroup extends ObjectGroup implements TownBlockOwner, Permissible {
	private Resident resident = null;
	private final Map<WorldCoord,TownBlock> townBlocks = new TownBlockMap();
	private double price = -1;
	private Town town;
	private TownyPermission permissions;

	/**
	 * @param id   A unique identifier for the group id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 * @param town The town that this group is owned by.   
	 */
	public PlotGroup(UUID id, String name, Town town) {
		super(id, name);
		this.town = town;
	}

	/**
	 * Store plot group in format "name,id,town,price"
	 * @return The string in the format described.
	 */
	@Override
	public String toString() {
		return super.toString() + "," + getTown().toString() + "," + getPrice();
	}

	/**
	 * Override the name change method to internally rehash the plot group map.
	 * @param name The name of the group.
	 */
	@Override
	public void setName(String name) {
		if (getName() == null) {
			super.setName(name);
		}
		else {
			String oldName = getName();
			super.setName(name);
			town.renamePlotGroup(oldName, this);
		}
	}
	
	public void setTown(Town town) {
		this.town = town;
		
		try {
			town.addPlotGroup(this);
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
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

	public double getPrice() {
		return price;
	}
	
	public void setResident(Resident resident) {
		if (hasResident())
			this.resident = resident;
	}

	public Resident getResident() throws NotRegisteredException {
		if (!hasResident())
			throw new NotRegisteredException("The Group " + this.toString() + "is not registered to a resident.");
		return resident;
	}

	public boolean hasResident() { return resident != null; }
	
	public boolean addTownBlock(@NotNull TownBlock townBlock) {
		if (townBlocks.containsValue(townBlock)) {
			return false;
		}
		
		townBlocks.put(townBlock.getWorldCoord(), townBlock);
		return true;
	}

	public boolean removeTownBlock(@NotNull TownBlock townBlock) {
		if (!townBlocks.containsValue(townBlock)) {
			return false;
		}
		
		townBlocks.remove(townBlock.getWorldCoord());
		return true;
	}

	public @NotNull Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks.values());
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

	@Override
	public void setPermissions(String line) {
		this.permissions.load(line);
	}

	public TownyPermission getPermissions() {
		return permissions;
	}

	public void setPermissions(TownyPermission permissions) {
		this.permissions = permissions;
	}
	
}
