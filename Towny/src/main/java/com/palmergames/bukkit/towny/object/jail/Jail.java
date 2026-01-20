package com.palmergames.bukkit.towny.object.jail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.Loadable;
import com.palmergames.bukkit.towny.object.Position;
import com.palmergames.bukkit.towny.object.Savable;
import com.palmergames.bukkit.towny.object.SpawnPoint;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectSaveException;

import org.jetbrains.annotations.ApiStatus;

public class Jail extends Loadable implements Savable {

	private UUID uuid;
	private Town town;
	private TownBlock townBlock;
	private final Map<SpawnPointLocation, Position> jailCellMap = new ConcurrentHashMap<>();
	private final List<Position> jailCells = new ArrayList<>();
	
	public Jail(UUID uuid, Town town, TownBlock townBlock, List<Location> jailCells) {
		this(uuid, town, townBlock, jailCells.stream().map(Position::ofLocation).collect(Collectors.toList()));
	}
	
	public Jail(UUID uuid, Town town, TownBlock townBlock, Collection<Position> jailCells) {
		this.uuid = uuid;
		this.town = town;
		this.townBlock = townBlock;
		
		for (Position cell : jailCells)
			addJailCell(cell);
	}

	public UUID getUUID() {
		return uuid;
	}

	@ApiStatus.Internal
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
		return Collections.unmodifiableList(Lists.transform(this.jailCells, Position::asLocation));
	}
	
	public List<Position> getJailCellPositions() {
		return Collections.unmodifiableList(this.jailCells);
	}
	
	public int getJailCellCount() {
		return this.jailCellMap.size();
	}

	public void setJailCells(List<Location> jailCells) {
		this.jailCellMap.clear();
		this.jailCells.clear();
		
		for (Location jail : jailCells)
			addJailCell(jail);
	}
	
	public void addJailCell(Location location) {
		Position pos = Position.ofLocation(location);
		
		jailCells.add(pos);
		jailCellMap.put(SpawnPointLocation.parseSpawnPointLocation(location), pos);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(pos, SpawnPointType.JAIL_SPAWN));
	}
	
	public void addJailCell(Position position) {
		jailCells.add(position);
		jailCellMap.put(SpawnPointLocation.parsePos(position), position);
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(position, SpawnPointType.JAIL_SPAWN));
	}
	
	public void removeJailCell(Location loc) {
		removeJailCell(SpawnPointLocation.parseSpawnPointLocation(loc));
	}
	
	public void removeJailCell(SpawnPointLocation pos) {
		TownyUniverse.getInstance().removeSpawnPoint(pos);
		jailCellMap.remove(pos);
	}
	
	public void removeAllCells() {
		for (SpawnPointLocation pos : this.jailCellMap.keySet())
			removeJailCell(pos);
	}
	
	public boolean hasJailCell(SpawnPointLocation loc) {
		return jailCellMap.containsKey(loc);
	}
	
	public boolean hasJailCell(int index) {
		return jailCellMap.size() - 1 >= index;
	}
	
	public String getWildName() {
		return getTownBlock().getWorld().getFormattedUnclaimedZoneName();
	}
	
	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveJail(this);
	}

	@Override
	public Map<String, Object> getObjectDataMap() throws ObjectSaveException {
		try {
			Map<String, Object> jail_hm = new HashMap<>();
			jail_hm.put("uuid", getUUID());
			jail_hm.put("townBlock", getTownBlockForSaving(getTownBlock()));
			
			StringBuilder jailCellArray = new StringBuilder();
			if (hasCells())
				for (Location cell : new ArrayList<>(getJailCellLocations()))
					jailCellArray.append(parseLocationForSaving(cell)).append(";");

			jail_hm.put("spawns", jailCellArray);
			
			return jail_hm;
		} catch (Exception e) {
			throw new ObjectSaveException("An exception occurred when constructing data for jail " + getName() + " (" + getUUID() + ").");
		}
	}

	public boolean load(Map<String, String> jailAsMap) {
		String line = "";
		line = jailAsMap.get("townblock");
		if (line != null) {
			try {
				TownBlock tb = parseTownBlockFromDB(line);
				setTownBlock(tb);
				setTown(tb.getTownOrNull());
				tb.setJail(this);
				tb.getTown().addJail(this);
			} catch (NumberFormatException | NotRegisteredException e) {
				TownyMessaging.sendErrorMsg("Jail " + getUUID() + " tried to load invalid townblock " + line + " deleting ");
				TownyUniverse.getInstance().getDataSource().removeJail(this);
				TownyUniverse.getInstance().getDataSource().deleteJail(this);
				return true;
			}
		}
		line = jailAsMap.get("spawns");
		if (line != null) {
			String[] jails = line.split(";");
			for (String spawn : jails) {
				Location loc = parseSpawnLocationFromDB(spawn);
				if (loc != null)
					addJailCell(loc);
			}
			if (getJailCellLocations().isEmpty()) {
				TownyMessaging.sendErrorMsg("Jail " + getUUID() + " loaded with zero spawns " + line + " deleting ");
				TownyUniverse.getInstance().getDataSource().removeJail(this);
				TownyUniverse.getInstance().getDataSource().deleteJail(this);
				return true;
			}
		}
		return true;
	}

	public boolean hasCells() {
		return !jailCellMap.isEmpty();
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
