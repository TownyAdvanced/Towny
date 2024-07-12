package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LlmDl
 */
public class District extends ObjectGroup implements Nameable, Savable {
	private List<TownBlock> townBlocks;
	private Town town;

	/**
	 * @param id   A unique identifier for the district id.
	 * @param name An alias for the id used for player in-game interaction via commands.
	 * @param town The town that this district is owned by.   
	 */
	public District(UUID id, String name, Town town) {
		super(id, name);
		this.town = town;
	}

	/**
	 * Store district in format "name,id,town,price"
	 * @return The string in the format described.
	 */
	@Override
	public String toString() {
		return super.toString() + "," + getTown().toString();
	}

	@Override
	public boolean exists() {
		return this.town != null && this.town.exists() && this.town.hasDistrictName(getName());
	}

	/**
	 * Override the name change method to internally rehash the district map.
	 * @param name The name of the district.
	 */
	@Override
	public void setName(String name) {
		if (getName() == null) {
			super.setName(name);
		}
		else {
			String oldName = getName();
			super.setName(name);
			town.renameDistrict(oldName, this);
		}
	}
	
	public void setTown(Town town) {
		this.town = town;
		
		try {
			town.addDistrict(this);
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(town, townBlocks, getName());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		District other = (District) obj;
		return Objects.equals(town, other.town) && Objects.equals(getName(), other.getName());
	}

	public Town getTown() {
		return town;
	}

	/**
	 *
	 * @return The qualified resident mode string.
	 */
	public String toModeString() {
		return "District{" + this.toString() + "}";
	}

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

	public boolean hasTownBlock(TownBlock townBlock) {
		return townBlocks.contains(townBlock);
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveDistrict(this);
	}
}
