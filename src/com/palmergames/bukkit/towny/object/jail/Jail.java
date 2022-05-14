package com.palmergames.bukkit.towny.object.jail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.Savable;
import com.palmergames.bukkit.towny.object.SpawnPoint;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource.TownyDBFileType;
import com.palmergames.bukkit.towny.db.TownySQLSource.TownyDBTableType;

public class Jail implements Savable {

	private UUID uuid;
	private Town town;
	private TownBlock townBlock;
	private Map<String, Location> jailCellMap = new ConcurrentHashMap<String, Location>();
	private List<Location> jailCells = new ArrayList<>();
	
	public Jail(UUID uuid, Town town, TownBlock townBlock, List<Location> jailCells) {
		this.uuid = uuid;
		this.town = town;
		this.townBlock = townBlock;
		if (jailCells != null)
			jailCells.stream().forEach(loc -> addJailCell(loc));
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
		return Collections.unmodifiableList(jailCells);
	}

	public void setJailCells(List<Location> jailCells) {
		this.jailCells = jailCells;
	}
	
	public void addJailCell(Location location) {
		jailCells.add(location);
		jailCellMap.put(SpawnPointLocation.parseSpawnPointLocation(location).toString(), location);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(location, SpawnPointType.JAIL_SPAWN));
	}
	
	public void removeJailCell(Location loc) {
		TownyUniverse.getInstance().removeSpawnPoint(loc);
		String spawn = SpawnPointLocation.parseSpawnPointLocation(loc).toString();
		jailCells.remove(jailCellMap.get(spawn));
		jailCellMap.remove(spawn);
	}
	
	public void removeAllCells() {
		for (Location loc : new ArrayList<>(jailCells))
			removeJailCell(loc);
	}
	
	public boolean hasJailCell(SpawnPointLocation loc) {
		return jailCellMap.keySet().stream().anyMatch(spawn -> spawn.equals(loc.toString()));
	}
	
	public boolean hasJailCell(int index) {
		if (jailCells == null || jailCells.size() < index)
			return false;
		return true;
	}
	
	public Map<String, Location> getCellMap() {
		return jailCellMap;
	}
	
	public String getWildName() {
		return getTownBlock().getWorld().getUnclaimedZoneName();
	}
	
	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveJail(this);
	}
	
	@Override
	public String getSaveLocation() {
		if (TownyUniverse.getInstance().getDataSource().isFlatFile())
			return TownyDBFileType.JAIL.getSaveLocation(getUUID().toString());
		else 
			return TownyDBTableType.JAIL.getSaveLocation(getUUID().toString());
	}

	public boolean hasCells() {
		return !jailCells.isEmpty();
	}
	
	public boolean hasName() {
		return !getTownBlock().getName().isEmpty();
	}
	
	public String getName() {
		if (hasName())
			return getTownBlock().getName();
		return "";
	}
}
