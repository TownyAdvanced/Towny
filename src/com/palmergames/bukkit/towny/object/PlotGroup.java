package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Suneet Tipirneni (Siris)
 * A simple class which encapsulates the grouping of townblocks.
 */
public class PlotGroup extends ObjectGroup implements TownBlockOwner, Savable {
	private Resident resident = null;
	private List<TownBlock> townBlocks;
	private double price = -1;
	private Town town;
	private TownyPermission permissions;
	private Set<Resident> trustedResidents = new HashSet<>();
	private Map<Resident, PermissionData> permissionOverrides = new HashMap<>();

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
	
	public void addTownBlock(TownBlock townBlock) {
		if (townBlocks == null)
			townBlocks = new ArrayList<>();
		
		townBlocks.add(townBlock);
	}

	public void removeTownBlock(TownBlock townBlock) {
		if (townBlocks != null)
			townBlocks.remove(townBlock);
	}
	
	public void setTownblocks(List<TownBlock> townBlocks) {
		this.townBlocks = townBlocks;
	}

	public Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks);
	}
	
	public boolean hasTownBlocks() {
		return townBlocks != null && !townBlocks.isEmpty();
	}

	@Override
	public boolean hasTownBlock(TownBlock townBlock) {
		return townBlocks.contains(townBlock);
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

	@Override
	public TownyPermission getPermissions() {
		return permissions;
	}

	public void setPermissions(TownyPermission permissions) {
		this.permissions = permissions;
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().savePlotGroup(this);
	}

	public void setTrustedResidents(Set<Resident> trustedResidents) {
		this.trustedResidents = new HashSet<>(trustedResidents);
	}

	public Set<Resident> getTrustedResidents() {
		return trustedResidents;
	}
	
	public void setPermissionOverrides(Map<Resident, PermissionData> permissionOverrides) {
		this.permissionOverrides = new HashMap<>(permissionOverrides);
	}

	public Map<Resident, PermissionData> getPermissionOverrides() {
		return permissionOverrides;
	}
	
	public boolean hasTrustedResident(Resident resident) {
		return trustedResidents.contains(resident);
	}
	
	public void addTrustedResident(Resident resident) {
		for (TownBlock townBlock : townBlocks) {
			if (!townBlock.hasTrustedResident(resident)) {
				townBlock.addTrustedResident(resident);
				townBlock.save();
			}
		}

		trustedResidents.add(resident);
	}
	
	public void removeTrustedResident(Resident resident) {
		if (!hasTrustedResident(resident))
			return;

		trustedResidents.remove(resident);

		for (TownBlock townBlock : townBlocks) {
			if (townBlock.hasTrustedResident(resident)) {
				townBlock.removeTrustedResident(resident);
				townBlock.save();
			}
		}
	}
	
	public void putPermissionOverride(Resident resident, PermissionData permissionData) {
		permissionOverrides.put(resident, permissionData);
		
		for (TownBlock townBlock : townBlocks) {
			townBlock.getPermissionOverrides().put(resident, permissionData);
			townBlock.save();
		}
	}
	
	public void removePermissionOverride(Resident resident) {
		if (!permissionOverrides.containsKey(resident))
			return;
		
		permissionOverrides.remove(resident);
			
		for (TownBlock townBlock : townBlocks) {
			if (townBlock.getPermissionOverrides().containsKey(resident)) {
				townBlock.getPermissionOverrides().remove(resident);
				townBlock.save();
			}
		}
	}
}
