package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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

	public abstract boolean backup() throws IOException;

	public boolean loadAll() {

		return loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadJailList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadJails() && loadRegenList() && loadSnapshotList() && loadHibernatedResidents();
	}

	public boolean saveAll() {

		return saveWorldList() && saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveTownBlocks() && saveJails() && saveRegenList() && saveSnapshotList() && saveHibernatedResidents();
	}

	public boolean saveAllWorlds() {

		return saveWorldList() && saveWorlds();
	}

	public boolean saveQueues() {

		return saveRegenList() && saveSnapshotList();
	}

	abstract public void finishTasks();

	abstract public boolean loadTownBlockList();

	abstract public boolean loadResidentList();
	
	abstract public boolean loadHibernatedResidents();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadWorldList();

	abstract public boolean loadRegenList();

	abstract public boolean loadSnapshotList();

	abstract public boolean loadTownBlocks();

	abstract public boolean loadJailList();
	
	abstract public boolean loadResident(Resident resident);

	abstract public boolean loadTown(Town town);

	abstract public boolean loadNation(Nation nation);

	abstract public boolean loadWorld(TownyWorld world);
	
	abstract public boolean loadJail(Jail jail);

	abstract public boolean loadPlotGroupList();

	abstract public boolean loadPlotGroup(PlotGroup group);

	abstract public boolean saveWorldList();

	abstract public boolean saveRegenList();

	abstract public boolean saveSnapshotList();

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveHibernatedResident(UUID uuid);
	
	abstract public boolean saveTown(Town town);
	
	abstract public boolean savePlotGroup(PlotGroup group);
	
	abstract public boolean saveJail(Jail jail);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveTownBlock(TownBlock townBlock);

	abstract public boolean savePlotData(PlotBlockData plotChunk);

	abstract public PlotBlockData loadPlotData(String worldName, int x, int z);

	abstract public PlotBlockData loadPlotData(TownBlock townBlock);
	
	abstract public boolean hasPlotData(TownBlock townBlock);

	abstract public void deletePlotData(PlotBlockData plotChunk);

	abstract public void deleteResident(Resident resident);

	abstract public void deleteHibernatedResident(UUID uuid);
	
	abstract public void deleteTown(Town town);

	abstract public void deleteNation(Nation nation);

	abstract public void deleteWorld(TownyWorld world);

	abstract public void deleteTownBlock(TownBlock townBlock);

	abstract public void deleteFile(String file);
	
	abstract public void deletePlotGroup(PlotGroup group);
	
	abstract public void deleteJail(Jail jail);

	public boolean cleanup() {

		return true;

	}

	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		TownySettings.setUUIDCount(0);
		
		for (Resident resident : universe.getResidents()) {
			if (!loadResident(resident)) {
				plugin.getLogger().severe("Loading Error: Could not read resident data '" + resident.getName() + "'.");
				return false;
			}

			if (resident.hasUUID())
				TownySettings.incrementUUIDCount();
		}
		return true;
	}

	public boolean loadTowns() {

		TownyMessaging.sendDebugMsg("Loading Towns");
		for (Town town : universe.getTowns())
			if (!loadTown(town)) {
				plugin.getLogger().severe("Loading Error: Could not read town data '" + town.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadNations() {

		TownyMessaging.sendDebugMsg("Loading Nations");
		for (Nation nation : universe.getNations())
			if (!loadNation(nation)) {
				plugin.getLogger().severe("Loading Error: Could not read nation data '" + nation.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadWorlds() {

		TownyMessaging.sendDebugMsg("Loading Worlds");
		for (TownyWorld world : universe.getTownyWorlds())
			if (!loadWorld(world)) {
				plugin.getLogger().severe("Loading Error: Could not read world data '" + world.getName() + "'.");
				return false;
			}
		return true;
	}
	
	public boolean loadJails() {
		TownyMessaging.sendDebugMsg("Loading Jails");
		for (Jail jail : universe.getJails()) {
			if (!loadJail(jail)) {
				plugin.getLogger().severe("Loading Error: Could not read jail data '" + jail.getUUID() + "'.");
				return false;
			}
		}
		return true;
	}
	
	public boolean loadPlotGroups() {
		TownyMessaging.sendDebugMsg("Loading PlotGroups");
		for (PlotGroup group : universe.getGroups()) {
			if (!loadPlotGroup(group)) {
				plugin.getLogger().severe("Loading Error: Could not read PlotGroup data: '" + group.getID() + "'.");
				return false;
			}
		}
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {

		TownyMessaging.sendDebugMsg("Saving Residents");
		for (Resident resident : universe.getResidents())
			saveResident(resident);
		return true;
	}
	
	public boolean saveHibernatedResidents() { 
		for (UUID uuid : universe.getHibernatedResidents())
			saveHibernatedResident(uuid);
		return true;
	}
	
	public boolean savePlotGroups() {
		TownyMessaging.sendDebugMsg("Saving PlotGroups");
		for (PlotGroup plotGroup : universe.getGroups())
			/*
			 * Only save plotgroups which actually have townblocks associated with them.
			 */
			if (plotGroup.hasTownBlocks())
				savePlotGroup(plotGroup);
			else
				deletePlotGroup(plotGroup); 
		return true;
	}

	public boolean saveJails() {
		TownyMessaging.sendDebugMsg("Saving Jails");
		for (Jail jail : universe.getJails())
			saveJail(jail);
		return true;
	}
	
	public boolean saveTowns() {

		TownyMessaging.sendDebugMsg("Saving Towns");
		for (Town town : universe.getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {

		TownyMessaging.sendDebugMsg("Saving Nations");
		for (Nation nation : universe.getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {

		TownyMessaging.sendDebugMsg("Saving Worlds");
		for (TownyWorld world : universe.getTownyWorlds())
			saveWorld(world);
		return true;
	}
	
	public boolean saveTownBlocks() {
		TownyMessaging.sendDebugMsg("Saving Townblocks");
		for (Town town : universe.getTowns()) {
			for (TownBlock townBlock : town.getTownBlocks())
				saveTownBlock(townBlock);
		}
		return true;
	}

	// Database functions
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

	abstract public void removeNation(Nation nation);

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

	abstract public void removeResident(Resident resident);

	abstract public void removeHibernatedResident(UUID uuid);
	
	abstract public void removeTownBlock(TownBlock townBlock);

	abstract public void removeTownBlocks(Town town);

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTownBlocks} instead.
	 */
	@Deprecated
	abstract public Collection<TownBlock> getAllTownBlocks();

	abstract public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;
	
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

	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newNation(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newWorld(String name) throws AlreadyRegisteredException;

	abstract public void removeTown(Town town);

	abstract public void removeTown(Town town, boolean delayFullRemoval);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public void removeJail(Jail jail);
	
	abstract public void removePlotGroup(PlotGroup group);

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

	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation);

	abstract public void mergeTown(Town mergeInto, Town mergeFrom);

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;
}
