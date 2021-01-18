package com.palmergames.bukkit.towny.object.jail;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.TownyUniverse;

public class Jail {

	private UUID uuid;
	private Town town;
	private TownBlock townBlock;
	private List<Location> jailCells;
	public Jail(UUID uuid, Town town, TownBlock townBlock, List<Location> jailCells) {
		this.uuid = uuid;
		this.town = town;
		this.townBlock = townBlock;
		this.jailCells = jailCells;
	}

	public UUID getUUID() {
		return uuid;
	}

	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	public Town getTown() {
		return town;
	}
	
	public void setTown(Town town) {
		this.town = town;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public void setTownBlock(TownBlock townBlock) {
		this.townBlock = townBlock;
	}

	public List<Location> getJailCellLocations() {
		return jailCells;
	}

	public void setJailCells(List<Location> jailCells) {
		this.jailCells = jailCells;
	}
	
	public void addJailCell(Location location) {
		jailCells.add(location);
	}
	
	public void removeJailCell(int index) {
		if (index > jailCells.size())
			return;
		jailCells.remove(--index);
	}
	
	public boolean hasJailCell(int index) {
		if (jailCells == null || jailCells.size() < index)
			return false;
		return true;
	}
	
	public String getWildName() {
		return getTownBlock().getWorld().getUnclaimedZoneName();
	}
	
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveJail(this);
	}

	public boolean hasCells() {
		return !jailCells.isEmpty();
	}
}
