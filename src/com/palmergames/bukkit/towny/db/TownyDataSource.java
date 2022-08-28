package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectCouldNotBeLoadedException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.regen.PlotBlockData;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * --- : Loading process : ---
 *
 * Load all the names/keys for each world, nation, town, and resident.
 * Load each world, which loads it's town blocks.
 * Load nations, towns, and residents.
 */

/*
 * Loading Towns:
 * Make sure to load TownBlocks, then HomeBlock, then Spawn.
 */

public abstract class TownyDataSource {
	final Lock lock = new ReentrantLock();
	protected final Towny plugin;
	protected final TownyUniverse universe;

	TownyDataSource(Towny plugin, TownyUniverse universe) {
		this.plugin = plugin;
		this.universe = universe;
	}

	public boolean isFlatFile() {
		return this instanceof TownyFlatFileSource;
	}

	public boolean isMySQL() {
		return this instanceof TownySQLSource;
	}

	public abstract boolean backup() throws IOException;

	public boolean loadAll() {

		return loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadJailList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadJails() && loadRegenList() && loadSnapshotList();
	}

	public boolean saveAll() {

		return saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveTownBlocks() && saveJails() && saveRegenList() && saveSnapshotList();
	}

	public boolean saveAllWorlds() {

		return saveWorlds();
	}

	public boolean saveQueues() {

		return saveRegenList() && saveSnapshotList();
	}

	abstract public void finishTasks();

	/*
	 * Load Lists (Gathering UUIDs to load in full later.)
	 * Methods are found in TownyFlatfile/SQlSource classes.
	 */

	abstract public boolean loadJailList();

	abstract public boolean loadPlotGroupList();

	abstract public boolean loadResidentList();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadWorldList();

	abstract public boolean loadTownBlockList();

	abstract public boolean loadRegenList();

	abstract public boolean loadSnapshotList();

	/*
	 * Load all objects of the given type, using the UUIDs gathered into TownyUniverse.
	 * Methods are found in TownyDatabaseHandler.
	 */

	abstract public boolean loadJails();

	abstract public boolean loadPlotGroups();

	abstract public boolean loadResidents();

	abstract public boolean loadTowns();

	abstract public boolean loadNations();

	abstract public boolean loadWorlds();

	abstract public boolean loadTownBlocks();

	/*
	 * Load all objects of the given type, using the UUIDs gathered into TownyUniverse.
	 * Methods are found in TownyFlatfile/SQlSource classes.
	 */

	abstract public boolean loadJailUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	abstract public boolean loadPlotGroupUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	abstract public boolean loadResidentUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	abstract public boolean loadTownUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	abstract public boolean loadNationUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	abstract public boolean loadWorldUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/*
	 * Load object Data from the database into Memory, to be entered into the Objects themselves.
	 * Methods are found in the TownyDatabaseHandler class.
	 */

	abstract public boolean loadJailData(UUID uuid);

	abstract public boolean loadPlotGroupData(UUID uuid);

	abstract public boolean loadResidentData(UUID uuid);

	abstract public boolean loadTownData(UUID uuid);

	abstract public boolean loadNationData(UUID uuid);

	abstract public boolean loadWorldData(UUID uuid);

	/*
	 * Load object from the database into Memory, to be entered into the Objects
	 * themselves, not used by Towny itself.
	 */
	public boolean loadJail(Jail jail) {
		return loadJailData(jail.getUUID());
	}

	public boolean loadPlotGroup(PlotGroup group) {
		return loadPlotGroupData(group.getUUID());
	}

	public boolean loadResident(Resident resident) {
		return loadResidentData(resident.getUUID());
	}

	public boolean loadTown(Town town) {
		return loadTownData(town.getUUID());
	}

	public boolean loadNation(Nation nation) {
		return loadNationData(nation.getUUID());
	}

	public boolean loadWorld(TownyWorld world) {
		return loadWorldData(world.getUUID());
	}

	/*
	 * Get objects as Maps for loading. Methods found in TownyFlatfile/SQLSource classes.
	 */

	abstract public HashMap<String, String> getJailMap(UUID uuid);

	abstract public HashMap<String, String> getPlotGroupMap(UUID uuid);

	abstract public HashMap<String, String> getResidentMap(UUID uuid);

	abstract public HashMap<String, String> getTownMap(UUID uuid);

	abstract public HashMap<String, String> getNationMap(UUID uuid);

	abstract public HashMap<String, String> getWorldMap(UUID uuid);

	/*
	 * Legacy database entries that still store a list of keys in a file.
	 * Methods are found in TownyDatabaseHandler.
	 */

	abstract public boolean saveRegenList();

	abstract public boolean saveSnapshotList();

	/*
	 * Individual objects saving methods. Methods are found in TownyFlatfile/SQlSource classes.
	 */

	abstract public boolean saveResident(Resident resident, HashMap<String, Object> data);

	abstract public boolean saveHibernatedResident(UUID uuid, HashMap<String, Object> data);

	abstract public boolean saveTown(Town town, HashMap<String, Object> data);

	abstract public boolean savePlotGroup(PlotGroup group, HashMap<String, Object> data);

	abstract public boolean saveJail(Jail jail, HashMap<String, Object> data);

	abstract public boolean saveNation(Nation nation, HashMap<String, Object> data);

	abstract public boolean saveWorld(TownyWorld world, HashMap<String, Object> data);

	abstract public boolean saveTownBlock(TownBlock townBlock, HashMap<String, Object> data);

	/*
	 * Individual objects saving methods. Methods are found in TownyDataBaseHandler.
	 */
	
	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveHibernatedResident(UUID uuid, long registered);
	
	abstract public boolean saveTown(Town town);
	
	abstract public boolean savePlotGroup(PlotGroup group);
	
	abstract public boolean saveJail(Jail jail);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveTownBlock(TownBlock townBlock);

	/*
	 * Save all of category
	 */

	public boolean saveJails() {
		TownyMessaging.sendDebugMsg("Saving all Jails");
		universe.getJails().stream().forEach(j -> saveJail(j));
		return true;
	}

	public boolean savePlotGroups() {
		TownyMessaging.sendDebugMsg("Saving all PlotGroups");
		universe.getGroups().stream().forEach(g -> vetPlotGroupForSaving(g));
		return true;
	}
	
	private void vetPlotGroupForSaving(PlotGroup g) {
		// Only save plotgroups which actually have townblocks associated with them.
		if (g.hasTownBlocks())
			savePlotGroup(g);
		else
			deletePlotGroup(g);
	}

	public boolean saveResidents() {
		TownyMessaging.sendDebugMsg("Saving all Residents");
		universe.getResidents().stream().forEach(r -> saveResident(r));
		return true;
	}

	public boolean saveTowns() {
		TownyMessaging.sendDebugMsg("Saving all Towns");
		universe.getTowns().stream().forEach(t -> saveTown(t));
		return true;
	}

	public boolean saveNations() {
		TownyMessaging.sendDebugMsg("Saving all Nations");
		universe.getNations().stream().forEach(n -> saveNation(n));
		return true;
	}

	public boolean saveWorlds() {
		TownyMessaging.sendDebugMsg("Saving all Worlds");
		universe.getTownyWorlds().stream().forEach(w -> saveWorld(w));
		return true;
	}

	public boolean saveTownBlocks() {
		TownyMessaging.sendDebugMsg("Saving all Townblocks");
		universe.getTowns().stream().forEach(t -> t.saveTownBlocks());
		return true;
	}

	/*
	 * Delete methods found in the TownyFlatfile/SQLSource classes.
	 */

	abstract public void deleteResident(Resident resident);

	abstract public void deleteHibernatedResident(UUID uuid);

	abstract public void deleteTown(Town town);

	abstract public void deleteNation(Nation nation);

	abstract public void deleteWorld(TownyWorld world);

	abstract public void deleteTownBlock(TownBlock townBlock);

	abstract public void deleteFile(String file);

	abstract public void deletePlotGroup(PlotGroup group);

	abstract public void deleteJail(Jail jail);

	/*
	 * PlotBlockData methods found in TownyDatabaseHandler (used by Flatfile and SQL Sources.)
	 */

	abstract public boolean savePlotData(PlotBlockData plotChunk);

	abstract public PlotBlockData loadPlotData(String worldName, int x, int z);

	abstract public PlotBlockData loadPlotData(TownBlock townBlock);

	abstract public boolean hasPlotData(TownBlock townBlock);

	abstract public void deletePlotData(PlotBlockData plotChunk);

	/*
	 * Remove Object methods found in TownyDatabaseHandler
	 */

	abstract public void removeResident(Resident resident);

	abstract public void removeTownBlock(TownBlock townBlock);

	abstract public void removeTownBlocks(Town town);

	abstract public void removeTown(Town town);

	abstract public void removeTown(Town town, boolean delayFullRemoval);

	abstract public void removeNation(Nation nation);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public void removeJail(Jail jail);
	
	abstract public void removePlotGroup(PlotGroup group);
	
	/*
	 * Rename Object methods found in TownyDatabaseHandler
	 */
	
	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	/*
	 * Misc
	 */

	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation);

	abstract public void mergeTown(Town mergeInto, Town mergeFrom);

	abstract public String getNameOfObject(String type, UUID uuid);

	abstract public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid);

	public boolean cleanup() {

		return true;

	}

	/*
	 * Deprecated Methods follow:
	 */

	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getResidents()} instead.
	 * 
	 * Gets a list of all Towny residents.
	 * @return list of all towny residents
	 */
	@Deprecated
	public List<Resident> getResidents() {
		return new ArrayList<>(universe.getResidents());
	}
	
	/**
	 * @deprecated since 0.97.5.18 use {@link TownyUniverse#getGroups()} instead.
	 * @return List of PlotGroups. 
	 */
	abstract public List<PlotGroup> getAllPlotGroups();

	/**
	 * @deprecated since 0.97.5.18 use {@link TownyUniverse#getJails()} instead.
	 * @return List of jails. 
	 */
	@Deprecated
	abstract public List<Jail> getAllJails();

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidents(String[])} instead.
	 */
	@Deprecated
	abstract public List<Resident> getResidents(String[] names);
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidents(UUID[])} instead.
	 */
	@Deprecated
	abstract public List<Resident> getResidents(UUID[] uuids);

	/**
	 * @deprecated as of 0.97.5.3, use {@link TownyUniverse#hasTown(String)} instead.
	 * 
	 * Checks if a town with the name exists.
	 * 
	 * @param name Name of the town to check.
	 * @return whether the town exists.
	 */
	@Deprecated
	public boolean hasTown(String name) {
		return universe.hasTown(name);
	}

	/**
	 * @deprecated as of 0.97.5.3, use {@link TownyUniverse#hasNation(String)} instead.
	 * 
	 * Check if a nation with the given name exists.
	 * 
	 * @param name Name of the nation to check.
	 * @return whether the nation with the given name exists.
	 */
	@Deprecated
	public boolean hasNation(String name) {
		return universe.hasNation(name);
	}

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTowns(String[])} instead.
	 */
	abstract public List<Town> getTowns(String[] names);

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTowns(List)} instead.
	 */
	abstract public List<Town> getTowns(List<UUID> uuids);
	
	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getTowns()} instead.
	 * 
	 * @return a list of all towns.
	 */
	@Deprecated
	public List<Town> getTowns() {
		return new ArrayList<>(universe.getTowns());
	}

	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getTown(String)} instead.
	 * 
	 * Gets a town from the passed-in name.
	 * @param name Town Name
	 * @return town associated with the name.
	 * @throws NotRegisteredException Town does not exist.
	 */
	@Deprecated
	public Town getTown(String name) throws NotRegisteredException {
		Town town = universe.getTown(name);
		
		if (town == null)
			throw new NotRegisteredException(String.format("The town with name '%s' is not registered!", name));
		
		return town;
	}

	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getTown(UUID)} instead.
	 * 
	 * Returns the associated town with the passed-in uuid.
	 * 
	 * @param uuid UUID of the town to fetch.
	 *                
	 * @return town associated with the uuid.
	 * 
	 * @throws NotRegisteredException Thrown if town doesn't exist.
	 */
	@Deprecated
	public Town getTown(UUID uuid) throws NotRegisteredException {
		Town town = universe.getTown(uuid);	
		
		if (town == null)
			throw new NotRegisteredException(String.format("The town with uuid '%s' is not registered.", uuid));
		
		return town;
	}

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getNations(String[])} instead.
	 */
	@Deprecated
	abstract public List<Nation> getNations(String[] names);

	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getNations()} instead.
	 * 
	 * Get all nations.
	 * 
	 * @return all nations.
	 */
	@Deprecated
	public List<Nation> getNations() {
		return new ArrayList<>(universe.getNations());
	}

	/**
	 * @deprecated as of 0.97.5.3, Please use {@link TownyUniverse#getNation(String)} instead.
	 * 
	 * Get the nation matching the passed-in name.
	 * 
	 * @param name Name of the nation to get.
	 * @return the nation that matches the name
	 * @throws NotRegisteredException if no nation is found matching the given name.
	 */
	@Deprecated
	public Nation getNation(String name) throws NotRegisteredException {
		Nation nation = universe.getNation(name);

		if (nation == null)
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return nation;
	}

	/**
	 * @deprecated as of 0.97.5.3, Use {@link TownyUniverse#getNation(UUID)} instead.
	 * 
	 * Get the nation matching the given UUID.
	 * 
	 * @param uuid UUID of nation to get.
	 * @return the nation matching the given UUID.
	 * @throws NotRegisteredException if no nation is found matching the given UUID.
	 */
	@Deprecated
	public Nation getNation(UUID uuid) throws NotRegisteredException {
		Nation nation = universe.getNation(uuid);
		
		if (nation == null)
			throw new NotRegisteredException(String.format("The nation with uuid '%s' is not registered.", uuid.toString()));
		
		return nation;
	}

	/**
	 * @deprecated as of 0.97.5.18, Use {@link TownyUniverse#getWorld(String)} instead.
	 *  
	 * @param name Name of TownyWorld
	 * @return TownyWorld matching the name or Null.
	 */
	@Deprecated
	@Nullable
	abstract public TownyWorld getWorld(String name);

	/**
	 * @deprecated as of 0.97.5.18, Use {@link TownyUniverse#getTownyWorlds()} instead.
	 * 
	 * @return List of TownyWorlds.
	 */
	@Deprecated
	abstract public List<TownyWorld> getWorlds();

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTownBlocks} instead.
	 */
	@Deprecated
	abstract public Collection<TownBlock> getAllTownBlocks();

	/**
	 * @deprecated as of 0.97.5.18 use {@link TownyAPI#getTownsWithoutNation} instead.
	 */
	@Deprecated
	abstract public List<Town> getTownsWithoutNation();

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidentsWithoutTown()} instead.
	 */
	@Deprecated
	abstract public List<Resident> getResidentsWithoutTown();

	/*
	 * New Object methods which have been deprecated.
	 */

	/**
	 * @deprecated as of 0.98.4.0 use {@link TownyUniverse#newResident(UUID, String) instead.
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 * @throws InvalidNameException 
	 */
	@Deprecated
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException, InvalidNameException {
		universe.newResident(UUID.randomUUID(), name);
	}

	/**
	 * @deprecated as of 0.98.4.0 use {@link TownyUniverse#newResident(UUID, String) instead.
	 * @param name
	 * @param uuid
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 * @throws InvalidNameException 
	 */
	@Deprecated
	public void newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException, InvalidNameException {
		universe.newResident(uuid, name);
	}
	
	/**
	 * @deprecated as of 0.97.5.3, use {@link TownyUniverse#newTown(String)} instead.
	 * Create a new town from a name
	 * 
	 * @param name town name
	 * @throws AlreadyRegisteredException thrown if town already exists.
	 * @throws NotRegisteredException thrown if town has an invalid name.
	 */
	@Deprecated
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		try {
			universe.newTown(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}
	}

	/**
	 * @deprecated as of 0.98.4.0 use {@link TownyUniverse#newNation(String) instead.
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 * @throws InvalidNameException 
	 */
	@Deprecated
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException, InvalidNameException {
		universe.newNation(name);
	}

	/**
	 * @deprecated as of 0.98.4.0 use {@link TownyUniverse#newNation(String) instead.
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 * @throws InvalidNameException 
	 */
	@Deprecated
	public void newNation(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException, InvalidNameException {
		universe.newNation(name);
	}

	/**
	 * @deprecated as of 0.98.4.0 use {@link TownyUniverse#newWorld(String) instead.
	 * @param world
	 */
	@Deprecated
	public void newWorld(World world) {
		universe.newWorld(world);
	}
}
