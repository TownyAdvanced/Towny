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
 * The TownyDataSource acts as an abstract plan for operating upon
 * the Towny database. Methods are contained primarily in the 
 * TownyDatabaseHandler class, which relies upon a Source class: 
 * ie: TownyFlatfileSource or TownySQLSource, which will then 
 * complete operations that require directly reading/writing from
 * the database. 
 * 
 * TownyDatabaseHandler is responsible for using the database source
 * methods, removing individual objects, renaming objects, operating
 * with aspects of the Database which are always stored in Flatfile:
 * PlotBlockData, Snapshot and Regen queues.
 * 
 * The database source classes are responsible for providing keys,
 * loading and saving objects using HashMaps, deleting objects.  
 * 
 * Creating new database sources is achieved by creating a new class
 * which extends TownyDatabaseHandler, and implementing the required
 * methods, following the instructions found in those methods'
 * javadocs.
 * 
 * --- : Loading process : ---
 *
 * - Load all the keys for each world, nation, town, and resident, jail,
 *   plotgroup, townblock into TownyUniverse.
 * - Parse over each key loaded into TownyUniverse, loading each object
 *   by requesting HashMaps made up of each object's data from the 
 *   DatabaseSource classes.
 *   
 * --- : Saving process : ---
 * 
 * - Save objects by dumping their data into HashMaps which are then
 *   processed by the DatabaseSource classes.
 */

/**
 * @author LlmDl
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

	/**
	 * @return true after loading all of the Jails' UUIDs into {@link TownyUniverse#newJailInternal(UUID)}
	 */
	abstract public boolean loadJailList();

	/**
	 * @return true after loading all of the PlotGroups' UUIDs into {@link TownyUniverse#newPlotGroupInternal(UUID)}
	 */
	abstract public boolean loadPlotGroupList();

	/**
	 * @return true after loading all of the Residents' UUIDs into {@link TownyUniverse#newResidentInternal(UUID)}
	 */
	abstract public boolean loadResidentList();

	/**
	 * @return true after loading all of the Towns' UUIDs into {@link TownyUniverse#newTownInternal(UUID)}
	 */
	abstract public boolean loadTownList();

	/**
	 * @return true after loading all of the Nations' UUIDs into {@link TownyUniverse#newNationInternal(UUID)}
	 */
	abstract public boolean loadNationList();

	/**
	 * @return true after loading all of the Worlds' UUIDs into {@link TownyUniverse#newWorldInternal(UUID)}
	 */
	abstract public boolean loadWorldList();

	/**
	 * @return true after loading all of the TownBlocks into {@link TownyUniverse#addTownBlock(TownBlock)}
	 */
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

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadJailData(UUID)} on each of the given
	 *         UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadJailData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadJailUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadPlotGroupData(UUID)} on each of the
	 *         given UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadPlotGroupData(UUID)}
	 *                                         is unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadPlotGroupUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadResidentData(UUID)} on each of the
	 *         given UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadResidentData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadResidentUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadTownData(UUID)} on each of the given
	 *         UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadTownData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadTownUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadNationData(UUID)} on each of the given
	 *         UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadNationData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadNationUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  uuids Set of UUIDs to use.
	 * @return true after calling {@link #loadWorldData(UUID)} on each of the given
	 *         UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadWorldData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadWorldUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

	/**
	 * @param  townBlocks Collection of TownBlocks to use.
	 * @return true after calling {@link #loadTownBlock(TownBlock)} on each of the
	 *         given TownBlocks.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadTownBlock(TownBlock)}
	 *                                         is unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadTownBlocks(Collection<TownBlock> townBlocks) throws ObjectCouldNotBeLoadedException;
	
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

	abstract public boolean loadTownBlock(TownBlock townBlock);

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

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a jail with the given UUID, which will be used to load the
	 *         jail with data.
	 */
	abstract public HashMap<String, String> getJailMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a plot group with the given UUID, which will be used to
	 *         load the plot group with data.
	 */
	abstract public HashMap<String, String> getPlotGroupMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a resident with the given UUID, which will be used to load
	 *         the resident with data.
	 */
	abstract public HashMap<String, String> getResidentMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a town with the given UUID, which will be used to load the
	 *         town with data.
	 */
	abstract public HashMap<String, String> getTownMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a nation with the given UUID, which will be used to load
	 *         the nation with data.
	 */
	abstract public HashMap<String, String> getNationMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for a world with the given UUID, which will be used to load
	 *         the world with data.
	 */
	abstract public HashMap<String, String> getWorldMap(UUID uuid);

	/**
	 * @param townBlock TownBlock to use.
	 * @return HashMap&lt;String, String&gt; populated with the keys and their
	 *         values for the given TownBlock, which will be used to load the
	 *         townblock with data.
	 */
	abstract public HashMap<String, String> getTownBlockMap(TownBlock townBlock);

	/*
	 * Legacy database entries that still store a list of keys in a file.
	 * Methods are found in TownyDatabaseHandler.
	 */

	abstract public boolean saveRegenList();

	abstract public boolean saveSnapshotList();

	/*
	 * Individual objects saving methods. Methods are found in TownyFlatfile/SQlSource classes.
	 */


	/**
	 * @param jail Jail to save.
	 * @param data HashMap&lt;String, Object&gt; which contains the keys and values
	 *             representing a Jail's data.
	 * @return true when the Jail is saved to the database successfully.
	 */
	abstract public boolean saveJail(Jail jail, HashMap<String, Object> data);

	/**
	 * @param group PlotGroup to save.
	 * @param data  HashMap&lt;String, Object&gt; which contains the keys and values
	 *              representing a PlotGroup's data.
	 * @return true when the PlotGroup is saved to the database successfully.
	 */
	abstract public boolean savePlotGroup(PlotGroup group, HashMap<String, Object> data);

	/**
	 * @param resident Resident to save.
	 * @param data     HashMap&lt;String, Object&gt; which contains the keys and
	 *                 values representing a Resident's data.
	 * @return true when the Resident is saved to the database successfully.
	 */
	abstract public boolean saveResident(Resident resident, HashMap<String, Object> data);

	/**
	 * @param uuid UUID to save.
	 * @param data HashMap&lt;String, Object&gt; which contains the keys and values
	 *             representing a HibernatedResident's data.
	 * @return true when the HibernatedResident is saved to the database
	 *         successfully.
	 */
	abstract public boolean saveHibernatedResident(UUID uuid, HashMap<String, Object> data);

	/**
	 * @param town Town to save.
	 * @param data HashMap&lt;String, Object&gt; which contains the keys and values
	 *             representing a Town's data.
	 * @return true when the Town is saved to the database successfully.
	 */
	abstract public boolean saveTown(Town town, HashMap<String, Object> data);

	/**
	 * @param nation Nation to save.
	 * @param data   HashMap&lt;String, Object&gt; which contains the keys and
	 *               values representing a Nation's data.
	 * @return true when the Nation is saved to the database successfully.
	 */
	abstract public boolean saveNation(Nation nation, HashMap<String, Object> data);

	/**
	 * @param world TownyWorld to save.
	 * @param data  HashMap&lt;String, Object&gt; which contains the keys and values
	 *              representing a TownyWorld's data.
	 * @return true when the TownyWorld is saved to the database successfully.
	 */
	abstract public boolean saveWorld(TownyWorld world, HashMap<String, Object> data);

	/**
	 * @param townBlock TownBlock to save.
	 * @param data      HashMap&lt;String, Object&gt; which contains the keys and
	 *                  values representing a TownBlock's data.
	 * @return true when the TownBlock is saved to the database successfully.
	 */
	abstract public boolean saveTownBlock(TownBlock townBlock, HashMap<String, Object> data);

	/*
	 * Individual objects saving methods. Methods are found in TownyDataBaseHandler.
	 */

	abstract public boolean saveJail(Jail jail);

	abstract public boolean savePlotGroup(PlotGroup group);

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveHibernatedResident(UUID uuid, long registered);
	
	abstract public boolean saveTown(Town town);

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

	/**
	 * @param jail Jail to delete from the Database.
	 */
	abstract public void deleteJail(Jail jail);

	/**
	 * @param group PlotGroup to delete from the Database.
	 */
	abstract public void deletePlotGroup(PlotGroup group);

	/**
	 * @param resident Resident to delete from the Database.
	 */
	abstract public void deleteResident(Resident resident);

	/**
	 * @param uuid UUID of the HibernatedResident to delete from the Database.
	 */
	abstract public void deleteHibernatedResident(UUID uuid);

	/**
	 * @param town Town to delete from the Database.
	 */
	abstract public void deleteTown(Town town);

	/**
	 * @param nation Nation to delete from the Database.
	 */
	abstract public void deleteNation(Nation nation);

	/**
	 * @param world TownyWorld to delete from the Database.
	 */
	abstract public void deleteWorld(TownyWorld world);

	/**
	 * @param townBlock TownBlock to delete from the Database.
	 */
	abstract public void deleteTownBlock(TownBlock townBlock);

	/*
	 * Used in TownyDatabaseHandler.
	 */
	abstract public void deleteFile(String file);

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

	/**
	 * @param type Object type by name, RESIDENT, TOWN, etc.
	 * @param uuid UUID of the Object to get a name for.
	 * @return name by which the given UUID is known.
	 */
	abstract public String getNameOfObject(String type, UUID uuid);

	/**
	 * @param uuid UUID of the HibernatedResident
	 * @return a CompletableFuture that should result in the Long value representing
	 *         the residents registered time.
	 */
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
