package com.palmergames.bukkit.towny.object.jail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.SpawnPoint;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.TownyUniverse;

public class Jail {

	private UUID uuid;
	private Town town;
	private TownBlock townBlock;
	private Map<SpawnPointLocation, Location> jailCellMap;
	private List<Location> jailCells;
	
	public Jail(UUID uuid, Town town, TownBlock townBlock, List<Location> jailCells) {
		this.uuid = uuid;
		this.town = town;
		this.townBlock = townBlock;
		this.jailCells = jailCells;
		
		for (Location loc : jailCells)
			jailCellMap.put(SpawnPointLocation.parseSpawnPointLocaiton(loc), loc);
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
		jailCellMap.put(SpawnPointLocation.parseSpawnPointLocaiton(location), location);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(location, SpawnPointType.JAIL_SPAWN));
	}
	
//	public void removeJailCell(int index) {
//		if (index > jailCells.size())
//			return;
//		TownyUniverse.getInstance().removeSpawnPoint(jailCells.get(--index));
//		jailCellMap.remove(SpawnPointLocation.parseSpawnPointLocaiton(jailCells.get(--index)));
//		jailCells.remove(--index);
//	}
	
	public void removeJailCell(Location loc) {
		TownyUniverse.getInstance().removeSpawnPoint(loc);
		jailCellMap.remove(SpawnPointLocation.parseSpawnPointLocaiton(loc));
		jailCells.remove(loc);
	}
	
	public boolean hasJailCell(int index) {
		if (jailCells == null || jailCells.size() < index)
			return false;
		return true;
	}
	
	public Map<SpawnPointLocation, Location> getCellMap() {
		return jailCellMap;
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
