package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent.Cause;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectCouldNotBeLoadedException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
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
 * loading and saving objects using Maps, deleting objects.  
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
 *   by requesting Maps made up of each object's data from the 
 *   DatabaseSource classes.
 *   
 * --- : Saving process : ---
 * 
 * - Save objects by dumping their data into Maps which are then
 *   processed by the DatabaseSource classes.
 */

/**
 * @author LlmDl
 */

public abstract class TownyDataSource {
	final Lock lock = new ReentrantLock();
	protected final Towny plugin;
	protected final TownyUniverse universe;
	protected final TownyAPI api;

	TownyDataSource(Towny plugin, TownyUniverse universe) {
		this.plugin = plugin;
		this.universe = universe;
		this.api = TownyAPI.getInstance();
	}

	public abstract boolean backup() throws IOException;

	public boolean loadAll() {

		final boolean loaded = loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadDistrictList() && loadJailList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadDistricts() && loadJails() && loadRegenList() && loadCooldowns();

		if (loaded) {
			postLoad();
		}

		return loaded;
	}
	
	public void postLoad() {}

	public boolean saveAll() {

		return saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveDistricts() && saveTownBlocks() && saveJails() && saveRegenList() && saveCooldowns();
	}

	public boolean saveAllWorlds() {

		return saveWorlds();
	}

	public boolean saveQueues() {

		return saveRegenList();
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
	 * @return true after loading all of the Districts' UUIDs into {@link TownyUniverse#newDistrictInternal(UUID)}
	 */
	abstract public boolean loadDistrictList();

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

	/*
	 * Load all objects of the given type, using the UUIDs gathered into TownyUniverse.
	 * Methods are found in TownyDatabaseHandler.
	 */

	abstract public boolean loadJails();

	abstract public boolean loadPlotGroups();
	
	abstract public boolean loadDistricts();

	abstract public boolean loadResidents();

	abstract public boolean loadTowns();

	abstract public boolean loadNations();

	abstract public boolean loadWorlds();

	abstract public boolean loadTownBlocks();

	abstract public boolean loadCooldowns();

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
	 * @return true after calling {@link #loadDistrictData(UUID)} on each of the
	 *         given UUIDs.
	 * @throws ObjectCouldNotBeLoadedException if {@link #loadDistrictData(UUID)} is
	 *                                         unsuccessful. Your error message
	 *                                         should specify which file failed to
	 *                                         load and where it is in the database.
	 */
	abstract public boolean loadDistrictUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException;

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

	abstract public boolean loadDistrictData(UUID uuid);

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

	public boolean loadDistrict(District district) {
		return loadDistrictData(district.getUUID());
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
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a jail with the given UUID, which will be used to load the
	 *         jail with data.
	 */
	abstract public Map<String, String> getJailMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a plot group with the given UUID, which will be used to
	 *         load the plot group with data.
	 */
	abstract public Map<String, String> getPlotGroupMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their values
	 *         for a district with the given UUID, which will be used to load the
	 *         district with data.
	 */
	abstract public Map<String, String> getDistrictMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a resident with the given UUID, which will be used to load
	 *         the resident with data.
	 */
	abstract public Map<String, String> getResidentMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a town with the given UUID, which will be used to load the
	 *         town with data.
	 */
	abstract public Map<String, String> getTownMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a nation with the given UUID, which will be used to load
	 *         the nation with data.
	 */
	abstract public Map<String, String> getNationMap(UUID uuid);

	/**
	 * @param uuid UUID to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for a world with the given UUID, which will be used to load
	 *         the world with data.
	 */
	abstract public Map<String, String> getWorldMap(UUID uuid);

	/**
	 * @param townBlock TownBlock to use.
	 * @return Map&lt;String, String&gt; populated with the keys and their
	 *         values for the given TownBlock, which will be used to load the
	 *         townblock with data.
	 */
	abstract public Map<String, String> getTownBlockMap(TownBlock townBlock);

	/*
	 * Legacy database entries that still store a list of keys in a file.
	 * Methods are found in TownyDatabaseHandler.
	 */

	abstract public boolean saveRegenList();

	/*
	 * Individual objects saving methods. Methods are found in TownyFlatfile/SQlSource classes.
	 */

	/**
	 * @param jail Jail to save.
	 * @param data Map&lt;String, Object&gt; which contains the keys and values
	 *             representing a Jail's data.
	 * @return true when the Jail is saved to the database successfully.
	 */
	abstract public boolean saveJail(Jail jail, Map<String, Object> data);

	/**
	 * @param group PlotGroup to save.
	 * @param data  Map&lt;String, Object&gt; which contains the keys and values
	 *              representing a PlotGroup's data.
	 * @return true when the PlotGroup is saved to the database successfully.
	 */
	abstract public boolean savePlotGroup(PlotGroup group, Map<String, Object> data);

	/**
	 * @param district District to save.
	 * @param data     Map&lt;String, Object&gt; which contains the keys and values
	 *                 representing a District's data.
	 * @return true when the District is saved to the database successfully.
	 */
	abstract public boolean saveDistrict(District district, Map<String, Object> data);

	/**
	 * @param resident Resident to save.
	 * @param data     Map&lt;String, Object&gt; which contains the keys and
	 *                 values representing a Resident's data.
	 * @return true when the Resident is saved to the database successfully.
	 */
	abstract public boolean saveResident(Resident resident, Map<String, Object> data);

	/**
	 * @param uuid UUID to save.
	 * @param data Map&lt;String, Object&gt; which contains the keys and values
	 *             representing a HibernatedResident's data.
	 * @return true when the HibernatedResident is saved to the database
	 *         successfully.
	 */
	abstract public boolean saveHibernatedResident(UUID uuid, Map<String, Object> data);

	/**
	 * @param town Town to save.
	 * @param data Map&lt;String, Object&gt; which contains the keys and values
	 *             representing a Town's data.
	 * @return true when the Town is saved to the database successfully.
	 */
	abstract public boolean saveTown(Town town, Map<String, Object> data);

	/**
	 * @param nation Nation to save.
	 * @param data   Map&lt;String, Object&gt; which contains the keys and
	 *               values representing a Nation's data.
	 * @return true when the Nation is saved to the database successfully.
	 */
	abstract public boolean saveNation(Nation nation, Map<String, Object> data);

	/**
	 * @param world TownyWorld to save.
	 * @param data  Map&lt;String, Object&gt; which contains the keys and values
	 *              representing a TownyWorld's data.
	 * @return true when the TownyWorld is saved to the database successfully.
	 */
	abstract public boolean saveWorld(TownyWorld world, Map<String, Object> data);

	/**
	 * @param townBlock TownBlock to save.
	 * @param data      Map&lt;String, Object&gt; which contains the keys and
	 *                  values representing a TownBlock's data.
	 * @return true when the TownBlock is saved to the database successfully.
	 */
	abstract public boolean saveTownBlock(TownBlock townBlock, Map<String, Object> data);

	/*
	 * Individual objects saving methods. Methods are found in TownyDataBaseHandler.
	 */

	abstract public boolean saveJail(Jail jail);

	abstract public boolean savePlotGroup(PlotGroup group);

	abstract public boolean saveDistrict(District district);

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveHibernatedResident(UUID uuid, long registered);
	
	abstract public boolean saveTown(Town town);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveTownBlock(TownBlock townBlock);

	abstract public boolean saveCooldowns();

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

	public boolean saveDistricts() {
		TownyMessaging.sendDebugMsg("Saving all Districts");
		universe.getDistricts().stream().forEach(d -> saveDistrict(d));
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
	 * @param district District to delete from the Database.
	 */
	abstract public void deleteDistrict(District district);

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

	abstract public void removeTownBlock(TownBlock townBlock) throws TownyException;

	abstract public void removeTownBlock(TownBlock townBlock, Cause cause) throws TownyException;

	abstract public void removeTownBlocks(Town town);

	public boolean removeTown(Town town, @NotNull DeleteTownEvent.Cause cause) {
		return removeTown(town, cause, null);
	}

	public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender) {
		return removeTown(town, cause, sender, TownySettings.getTownRuinsEnabled() && !town.isRuined());
	}

	abstract public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender, boolean delayFullRemoval);

	public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause) {
		return removeNation(nation, cause, null);
	}

	abstract public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause, @Nullable CommandSender sender);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public void removeJail(Jail jail);

	abstract public void removePlotGroup(PlotGroup group);

	abstract public void removeDistrict(District district);
	
	/*
	 * Rename Object methods found in TownyDatabaseHandler
	 */
	
	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;

	abstract public void renameDistrict(District district, String newName) throws AlreadyRegisteredException;

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	/*
	 * Misc
	 */

	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation);

	abstract public void mergeTown(Town mergeInto, Town mergeFrom);

	/**
	 * @param uuid UUID of the HibernatedResident
	 * @return a CompletableFuture that should result in the Long value representing
	 *         the resident's registered time.
	 */
	abstract public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid);

	public boolean cleanup() {

		return true;

	}

//	/**
//	 * OLD STUFF
//	 */

//
//	public boolean loadResidents() {
//
//		TownyMessaging.sendDebugMsg("Loading Residents");
//
//		for (Resident resident : universe.getResidents()) {
//			if (!loadResident(resident)) {
//				plugin.getLogger().severe("Loading Error: Could not read resident data '" + resident.getName() + "'.");
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public boolean loadTowns() {
//
//		TownyMessaging.sendDebugMsg("Loading Towns");
//		for (Town town : universe.getTowns())
//			if (!loadTown(town)) {
//				plugin.getLogger().severe("Loading Error: Could not read town data '" + town.getName() + "'.");
//				return false;
//			}
//		return true;
//	}
//
//	public boolean loadNations() {
//
//		TownyMessaging.sendDebugMsg("Loading Nations");
//		for (Nation nation : universe.getNations())
//			if (!loadNation(nation)) {
//				plugin.getLogger().severe("Loading Error: Could not read nation data '" + nation.getName() + "'.");
//				return false;
//			}
//		return true;
//	}
//
//	public boolean loadWorlds() {
//
//		TownyMessaging.sendDebugMsg("Loading Worlds");
//		for (TownyWorld world : universe.getTownyWorlds())
//			if (!loadWorld(world)) {
//				plugin.getLogger().severe("Loading Error: Could not read world data '" + world.getName() + "'.");
//				return false;
//			}
//		return true;
//	}
//	
//	public boolean loadJails() {
//		TownyMessaging.sendDebugMsg("Loading Jails");
//		for (Jail jail : universe.getJails()) {
//			if (!loadJail(jail)) {
//				plugin.getLogger().severe("Loading Error: Could not read jail data '" + jail.getUUID() + "'.");
//				return false;
//			}
//		}
//		return true;
//	}
//	
//	public boolean loadPlotGroups() {
//		TownyMessaging.sendDebugMsg("Loading PlotGroups");
//		for (PlotGroup group : universe.getGroups()) {
//			if (!loadPlotGroup(group)) {
//				plugin.getLogger().severe("Loading Error: Could not read PlotGroup data: '" + group.getUUID() + "'.");
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public boolean loadDistricts() {
//		TownyMessaging.sendDebugMsg("Loading Districts");
//		for (District district : universe.getDistricts()) {
//			if (!loadDistrict(district)) {
//				plugin.getLogger().severe("Loading Error: Could not read District data: '" + district.getUUID() + "'.");
//				return false;
//			}
//		}
//		return true;
//	}
//
//	abstract public boolean loadCooldowns();
//
//	/*
//	 * Save all of category
//	 */
//
//	public boolean saveResidents() {
//
//		TownyMessaging.sendDebugMsg("Saving Residents");
//		for (Resident resident : universe.getResidents())
//			saveResident(resident);
//		return true;
//	}
//	
//	public boolean savePlotGroups() {
//		TownyMessaging.sendDebugMsg("Saving PlotGroups");
//		for (PlotGroup plotGroup : universe.getGroups())
//			/*
//			 * Only save plotgroups which actually have townblocks associated with them.
//			 */
//			if (plotGroup.hasTownBlocks())
//				savePlotGroup(plotGroup);
//			else
//				deletePlotGroup(plotGroup); 
//		return true;
//	}
//
//	public boolean saveDistricts() {
//		TownyMessaging.sendDebugMsg("Saving Districts");
//		for (District district : universe.getDistricts())
//			/*
//			 * Only save districts which actually have townblocks associated with them.
//			 */
//			if (district.hasTownBlocks())
//				saveDistrict(district);
//			else
//				deleteDistrict(district); 
//		return true;
//	}
//
//	public boolean saveJails() {
//		TownyMessaging.sendDebugMsg("Saving Jails");
//		for (Jail jail : universe.getJails())
//			saveJail(jail);
//		return true;
//	}
//	
//	public boolean saveTowns() {
//
//		TownyMessaging.sendDebugMsg("Saving Towns");
//		for (Town town : universe.getTowns())
//			saveTown(town);
//		return true;
//	}
//
//	public boolean saveNations() {
//
//		TownyMessaging.sendDebugMsg("Saving Nations");
//		for (Nation nation : universe.getNations())
//			saveNation(nation);
//		return true;
//	}
//
//	public boolean saveWorlds() {
//
//		TownyMessaging.sendDebugMsg("Saving Worlds");
//		for (TownyWorld world : universe.getTownyWorlds())
//			saveWorld(world);
//		return true;
//	}
//	
//	public boolean saveTownBlocks() {
//		TownyMessaging.sendDebugMsg("Saving Townblocks");
//		for (Town town : universe.getTowns()) {
//			for (TownBlock townBlock : town.getTownBlocks())
//				saveTownBlock(townBlock);
//		}
//		return true;
//	}
//	
//	abstract public boolean saveCooldowns();
//
//	// Database functions
//
//	abstract public void removeResident(Resident resident);
//
//	abstract public void removeTownBlock(TownBlock townBlock) throws TownyException;
//
//	abstract public void removeTownBlock(TownBlock townBlock, Cause cause) throws TownyException;
//
//	abstract public void removeTownBlocks(Town town);
//
//	public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause) {
//		return removeNation(nation, cause, null);
//	}
//
//	abstract public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause, @Nullable CommandSender sender);
//
//	/**
//	 * @deprecated Use {@link #newResident(String, UUID)} instead.
//	 */
//	@Deprecated(since = "0.102.0.4")
//	abstract public @NotNull Resident newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;
//
//	abstract public @NotNull Resident newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;
//	
//	/**
//	 * @deprecated Use {@link #newNation(String, UUID)} instead.
//	 */
//	@Deprecated(since = "0.102.0.4")
//	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;
//
//	abstract public void newNation(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;
//
//	abstract public void newWorld(String name) throws AlreadyRegisteredException;
//
//	public boolean removeTown(Town town, @NotNull DeleteTownEvent.Cause cause) {
//		return removeTown(town, cause, null);
//	}
//
//	public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender) {
//		return removeTown(town, cause, sender, TownySettings.getTownRuinsEnabled() && !town.isRuined());
//	}
//
//	abstract public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender, boolean delayFullRemoval);
//
//	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;
//
//	abstract public void removeJail(Jail jail);
//	
//	abstract public void removePlotGroup(PlotGroup group);
//	
//	abstract public void removeDistrict(District district);
//
//	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;
//
//	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;
//	
//	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation);
//
//	abstract public void mergeTown(Town mergeInto, Town mergeFrom);
//
//	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;
//
//	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;
//	
//	abstract public void renameDistrict(District district, String newName) throws AlreadyRegisteredException;
//	
//	/**
//	 * @deprecated since 0.100.2.9 use {@link #removeTown(Town, com.palmergames.bukkit.towny.event.DeleteTownEvent.Cause)} instead.
//	 * @param town
//	 */
//	@Deprecated
//	public void removeTown(Town town) {
//		removeTown(town, DeleteTownEvent.Cause.UNKNOWN);
//	}
//	
//	@SuppressWarnings("unused")
//	private void removeTown$$bridge$$public(Town town, boolean delayFullRemoval) {
//		removeTown(town, DeleteTownEvent.Cause.UNKNOWN, null, delayFullRemoval);
//	}
//
//	/**
//	 * @deprecated since 0.100.2.96 use {@link #removeNation(Nation, com.palmergames.bukkit.towny.event.DeleteNationEvent.Cause)} instead.
//	 * @param nation
//	 */
//	@Deprecated
//	public void removeNation(Nation nation) {
//		removeNation(nation, DeleteNationEvent.Cause.UNKNOWN, null);
//	}
}
