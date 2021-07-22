package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource.elements;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeletePlayerEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.RenameResidentEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.town.TownUnclaimEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.DataFieldIO;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author ElgarL
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {
	final String rootFolderPath;
	final String dataFolderPath;
	final String settingsFolderPath;
	final String logFolderPath;
	final String backupFolderPath;

	Logger logger = LogManager.getLogger(TownyDatabaseHandler.class);
	protected final Queue<Runnable> queryQueue = new ConcurrentLinkedQueue<>();
	private final BukkitTask task;
	
	protected TownyDatabaseHandler(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		this.rootFolderPath = universe.getRootFolder();
		this.dataFolderPath = rootFolderPath + File.separator + "data";
		this.settingsFolderPath = rootFolderPath + File.separator + "settings";
		this.logFolderPath = rootFolderPath + File.separator + "logs";
		this.backupFolderPath = rootFolderPath + File.separator + "backup";

		if (!FileMgmt.checkOrCreateFolders(
				rootFolderPath,
				rootFolderPath + File.separator + "logs",
				dataFolderPath,
				dataFolderPath + File.separator + "plot-block-data"
			) || !FileMgmt.checkOrCreateFiles(
				dataFolderPath + File.separator + "regen.txt",
				dataFolderPath + File.separator + "snapshot_queue.txt"
			)) {
				TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");
			}
		
		/*
		 * Start our Async queue for pushing data to the flatfile database.
		 */
		task = BukkitTools.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			while (!this.queryQueue.isEmpty()) {
				Runnable operation = this.queryQueue.poll();
				operation.run();
			}
		}, 5L, 5L);
	}
	
	@Override
	public void finishTasks() {
		
		// Cancel the repeating task as its not needed anymore.
		task.cancel();
		
		// Make sure that *all* tasks are saved before shutting down.
		while (!queryQueue.isEmpty()) {
			Runnable operation = this.queryQueue.poll();
			operation.run();
		}
	}
	
	@Override
	public boolean backup() throws IOException {

		if (!TownySettings.getSaveDatabase().equalsIgnoreCase("flatfile")) {
			System.out.println("***** Warning *****");
			System.out.println("***** Only Snapshots & Regen files in towny\\data\\ will be backed up!");
			System.out.println("***** This does not include your residents/towns/nations.");
			System.out.println("***** Make sure you have scheduled a backup in MySQL too!!!");
		}
		String backupType = TownySettings.getFlatFileBackupType();
		long t = System.currentTimeMillis();
		String newBackupFolder = backupFolderPath + File.separator + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + t;
		FileMgmt.checkOrCreateFolders(rootFolderPath, rootFolderPath + File.separator + "backup");
		switch (backupType.toLowerCase()) {
		case "folder": {
			FileMgmt.checkOrCreateFolder(newBackupFolder);
			FileMgmt.copyDirectory(new File(dataFolderPath), new File(newBackupFolder));
			FileMgmt.copyDirectory(new File(logFolderPath), new File(newBackupFolder));
			FileMgmt.copyDirectory(new File(settingsFolderPath), new File(newBackupFolder));
			return true;
		}
		case "zip": {
			FileMgmt.zipDirectories(new File(newBackupFolder + ".zip"), new File(dataFolderPath),
					new File(logFolderPath), new File(settingsFolderPath));
			return true;
		}
		case "tar.gz":
		case "tar": {
			FileMgmt.tar(new File(newBackupFolder.concat(".tar.gz")),
				new File(dataFolderPath),
				new File(logFolderPath),
				new File(settingsFolderPath));
			return true;
		}
		default:
		case "none": {
			return false;
		}
		}
	}

	/*
	 * Add new objects to the TownyUniverse maps.
	 */
	
	@Override
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		newResident(name, null);
	}

	@Override
	public void newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}
		
		if (universe.hasResident(name))
			throw new AlreadyRegisteredException("A resident with the name " + filteredName + " is already in use.");
		
		Resident resident = new Resident(filteredName);
		
		if (uuid != null)
			resident.setUUID(uuid);
		
		universe.registerResident(resident);
	}

	/**
	 * @deprecated as of 0.96.4.0, use {@link TownyUniverse#newTown(String)} instead.
	 * Create a new town from a name
	 * 
	 * @param name town name
	 * @throws AlreadyRegisteredException thrown if town already exists.
	 * @throws NotRegisteredException thrown if town has an invalid name.
	 */
	@Deprecated
	@Override
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		try {
			universe.newTown(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}
	}

	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		newNation(name, null);
	}

	@Override
	public void newNation(String name, @Nullable UUID uuid) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.hasNation(filteredName))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		Nation nation = new Nation(filteredName);
		
		if (uuid != null)
			nation.setUUID(uuid);
		
		universe.registerNation(nation);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException {
		
		if (universe.getWorldMap().containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		universe.getWorldMap().put(name.toLowerCase(), new TownyWorld(name));
	}

	/*
	 * Are these objects in the TownyUniverse maps?
	 */

	/**
	 * @deprecated as of 0.96.6.0, use {@link TownyUniverse#hasResident(String)} instead.
	 * 
	 * @param name Name to check for.
	 * @return whether Towny has a resident by the name.
	 */
	@Override
	@Deprecated
	public boolean hasResident(String name) {
		return universe.hasResident(name);
	}

	/**
	 * @deprecated as of 0.96.4.0, use {@link TownyUniverse#hasTown(String)} instead.
	 * 
	 * Checks if a town with the name exists.
	 * 
	 * @param name Name of the town to check.
	 * @return whether the town exists.
	 */
	@Deprecated
	@Override
	public boolean hasTown(String name) {
		return universe.hasTown(name);
	}

	/**
	 * @deprecated as of 0.96.4.0, use {@link TownyUniverse#hasNation(String)} instead.
	 * 
	 * Check if a nation with the given name exists.
	 * 
	 * @param name Name of the nation to check.
	 * @return whether the nation with the given name exists.
	 */
	@Deprecated
	@Override
	public boolean hasNation(String name) {
		return universe.hasNation(name);
	}

	/**
	 * @deprecated as of 0.96.4.0, No longer used by Towny. Messing with the Resident map is ill advised.
	 * 
	 * Gets the names of all residents.
	 * 
	 * @return Returns a set of all resident names.
	 */
	@Override
	@Deprecated
	public Set<String> getResidentKeys() {

		return universe.getResidents().stream().map(TownyObject::getName).collect(Collectors.toSet());
	}

	/**
	 * @deprecated as of 0.96.4.0, No longer used by Towny. Messing with the Towns map is ill advised.
	 * 
	 * Gets the keys of TownyUniverse's Towns Map.
	 * 
	 * @return Returns {@link Map#keySet()} of {@link TownyUniverse#getTownsMap()}
	 */
	@Override
	@Deprecated
	public Set<String> getTownsKeys() {

		return universe.getTownsMap().keySet();
	}

	/**
	 * @deprecated as of 0.96.4.0, No longer used by Towny. Messing with the Nations map is ill advised. Also this method is inefficient.
	 * 
	 * Gets the names of all nations.
	 * 
	 * @return Returns a set of all nation names from all registered nations.
	 */
	@Override
	@Deprecated
	public Set<String> getNationsKeys() {

		return universe.getNations().stream().map(TownyObject::getName).collect(Collectors.toSet());
	}
	
	/*
	 * getResident methods.
	 */
	
	@Override
	public List<Resident> getResidents(Player player, String[] names) {

		List<Resident> invited = new ArrayList<>();
		for (String name : names) {
			Resident target = universe.getResident(name);
			if (target == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered_1", name));
			}
			else {
				invited.add(target);
			}
		}
		return invited;
	}

	@Override
	public List<Resident> getResidents(String[] names) {

		List<Resident> matches = new ArrayList<>();
		for (String name : names) {
			Resident matchRes = universe.getResident(name);
			
			if (matchRes != null)
				matches.add(matchRes);
		}
		return matches;
	}
	
	@Override
	public List<Resident> getResidents(UUID[] uuids) {
		return Arrays.stream(uuids).filter(Objects::nonNull).map(universe::getResident).filter(Objects::nonNull).collect(Collectors.toList());
	}

	/**
	 * @deprecated as of 0.96.6.0, Use {@link TownyUniverse#getResidents()} instead.
	 * 
	 * Gets a list of all Towny residents.
	 * @return list of all towny residents
	 */
	@Override
	@Deprecated
	public List<Resident> getResidents() {
		return new ArrayList<>(universe.getResidents());
	}

	/**
	 * @deprecated as of 0.96.6.0, Use {@link TownyUniverse#getResident(String)} instead.
	 * 
	 * Get a resident matching a specific name.
	 * @param name Name of the resident to find.
	 * @return the resident matching the name.
	 * @throws NotRegisteredException if no resident matching the name is found.
	 */
	@Override
	@Deprecated
	public Resident getResident(String name) throws NotRegisteredException {
		Resident res = universe.getResident(name);
		
		if (res == null)
			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));
		
		return res;
	}

	@Override
	public List<Resident> getResidentsWithoutTown() {

		List<Resident> residentFilter = new ArrayList<>();
		for (Resident resident : universe.getResidents())
			if (!resident.hasTown())
				residentFilter.add(resident);
		return residentFilter;
	}
	
	/*
	 * getTowns methods.
	 */	
	
	@Override
	public List<Town> getTowns(String[] names) {

		List<Town> matches = new ArrayList<>();
		for (String name : names) {
			Town t = universe.getTown(name);
			
			if (t != null) {
				matches.add(t);
			}
		}
		
		return matches;
	}

	/**
	 * @deprecated as of 0.96.4.0, Use {@link TownyUniverse#getTowns()} instead.
	 * 
	 * @return a list of all towns.
	 */
	@Deprecated
	@Override
	public List<Town> getTowns() {

		return new ArrayList<>(universe.getTowns());
	}

	/**
	 * @deprecated as of 0.96.4.0, Use {@link TownyUniverse#getTown(String)} instead.
	 * 
	 * Gets a town from the passed-in name.
	 * @param name Town Name
	 * @return town associated with the name.
	 * @throws NotRegisteredException Town does not exist.
	 */
	@Deprecated
	@Override
	public Town getTown(String name) throws NotRegisteredException {
		Town town = universe.getTown(name);
		
		if (town == null)
			throw new NotRegisteredException(String.format("The town with name '%s' is not registered!", name));
		
		return town;
	}

	/**
	 * @deprecated as of 0.96.4.0, Use {@link TownyUniverse#getTown(UUID)} instead.
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
	@Override
	public Town getTown(UUID uuid) throws NotRegisteredException {
		Town town = universe.getTown(uuid);	
		
		if (town == null)
			throw new NotRegisteredException(String.format("The town with uuid '%s' is not registered.", uuid));
		
		return town;
	}

	@Override
	public List<Town> getTownsWithoutNation() {

		List<Town> townFilter = new ArrayList<>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}
	
	/*
	 * getNations methods.
	 */
	
	@Override
	public List<Nation> getNations(String[] names) {

		List<Nation> matches = new ArrayList<>();
		for (String name : names) {
			Nation nation = universe.getNation(name);
			
			if (nation != null)
				matches.add(nation);
		}
		return matches;
	}

	/**
	 * @deprecated as of 0.96.6.0, Use {@link TownyUniverse#getNations()} instead.
	 * 
	 * Get all nations.
	 * 
	 * @return all nations.
	 */
	@Deprecated
	@Override
	public List<Nation> getNations() {

		return new ArrayList<>(universe.getNations());
	}

	/**
	 * @deprecated as of 0.96.6.0, Please use {@link TownyUniverse#getNation(String)} instead.
	 * 
	 * Get the nation matching the passed-in name.
	 * 
	 * @param name Name of the nation to get.
	 * @return the nation that matches the name
	 * @throws NotRegisteredException if no nation is found matching the given name.
	 */
	@Deprecated
	@Override
	public Nation getNation(String name) throws NotRegisteredException {
		Nation nation = universe.getNation(name);

		if (nation == null)
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return nation;
	}

	/**
	 * @deprecated as of 0.96.6.0, Use {@link TownyUniverse#getNation(UUID)} instead.
	 * 
	 * Get the nation matching the given UUID.
	 * 
	 * @param uuid UUID of nation to get.
	 * @return the nation matching the given UUID.
	 * @throws NotRegisteredException if no nation is found matching the given UUID.
	 */
	@Deprecated
	@Override
	public Nation getNation(UUID uuid) throws NotRegisteredException {
		Nation nation = universe.getNation(uuid);
		
		if (nation == null)
			throw new NotRegisteredException(String.format("The nation with uuid '%s' is not registered.", uuid.toString()));
		
		return nation;
	}

	/*
	 * getWorlds methods.
	 */

	@Override
	public TownyWorld getWorld(String name) throws NotRegisteredException {

		TownyWorld world = universe.getWorldMap().get(name.toLowerCase());

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}

	@Override
	public List<TownyWorld> getWorlds() {

		return new ArrayList<>(universe.getWorldMap().values());
	}
	
	/*
	 * getTownblocks methods.
	 */

	@Override
	public Collection<TownBlock> getAllTownBlocks() {
		return TownyUniverse.getInstance().getTownBlocks().values();
	}
	
	/*
	 * getPlotGroups methods.
	 */

	public PlotGroup getPlotObjectGroup(UUID groupID) {
		return universe.getGroup(groupID);
	}

	public List<PlotGroup> getAllPlotGroups() {
		return new ArrayList<>(universe.getGroups());
	}
	
	/*
	 * get Jails method.
	 */
	public List<Jail> getAllJails() {
		return new ArrayList<>(universe.getJailUUIDMap().values());
	}

	/*
	 * Remove Object Methods
	 */
	
	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlawlists.
		for (Town townOutlaw : TownyUniverse.getInstance().getTowns()) {
			if (townOutlaw.hasOutlaw(resident)) {
				townOutlaw.removeOutlaw(resident);
				saveTown(townOutlaw);
			}
		}

		// Remove resident from residents' friendslists.
		List<Resident> toSave = new ArrayList<>();
		for (Resident toCheck : universe.getResidents()) {		
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
			if (toCheck.hasFriend(resident)) {
				TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
				toCheck.removeFriend(resident);
				toSave.add(toCheck);
			}
		}
		for (Resident toCheck : toSave)
			saveResident(toCheck);
		
		Town town = null;

		if (resident.hasTown())
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e1) {
				e1.printStackTrace();
			}

		if (town != null) {
			resident.removeTown();
			
			// Delete the town if there are no more residents
			if (town.getNumResidents() == 0) {
				TownyUniverse.getInstance().getDataSource().removeTown(town);
			}
		}

		// Delete the residents file.
		deleteResident(resident);
		// Remove the residents record from memory.
		try {
			universe.unregisterResident(resident);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		// Clear accounts
		if (TownySettings.isDeleteEcoAccount() && TownyEconomyHandler.isActive())
			resident.getAccount().removeAccount();

		plugin.deleteCache(resident.getName());
		
		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident));
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {
		Town town = townBlock.getTownOrNull();
		if (town == null)
			// Log as error because TownBlocks *must* have a town.
			logger.error(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", townBlock.getWorld().getName(), townBlock.getX(), townBlock.getZ()));

		TownPreUnclaimEvent event = new TownPreUnclaimEvent(town, townBlock);
		BukkitTools.getPluginManager().callEvent(event);
		
		if (event.isCancelled()) {
			// Log as Warn because the event has been processed
			logger.warn(event.getCancelMessage());
			return;
		}
		
		if (townBlock.isJail())
			removeJail(townBlock.getJail());

		TownyUniverse.getInstance().removeTownBlock(townBlock);
		deleteTownBlock(townBlock);


		if (townBlock.getWorld().isUsingPlotManagementDelete())
			TownyRegenAPI.addDeleteTownBlockIdQueue(townBlock.getWorldCoord());

		// Move the plot to be restored
		if (townBlock.getWorld().isUsingPlotManagementRevert()) {
			PlotBlockData plotData = TownyRegenAPI.getPlotChunkSnapshot(townBlock);
			if (plotData != null && !plotData.getBlockList().isEmpty()) {
				TownyRegenAPI.addPlotChunk(plotData, true);
			}
		}
		// Raise an event to signal the unclaim
		BukkitTools.getPluginManager().callEvent(new TownUnclaimEvent(town, townBlock.getWorldCoord()));
	}

	@Override
	public void removeTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks()))
			removeTownBlock(townBlock);
	}

	@Override
	public void removeTown(Town town) {
		
		/*
		 * If Town Ruining is enabled set the town into a ruined state
		 * rather than deleting.
		 */
		removeTown(town, TownRuinSettings.getTownRuinsEnabled() && !town.isRuined());
	}

	@Override
	public void removeTown(Town town, boolean delayFullRemoval) {
		if (delayFullRemoval) {
			/*
			 * When Town ruining is active, send the Town into a ruined state, prior to real removal.
			 */
			TownRuinUtil.putTownIntoRuinedState(town, plugin);
			return;
		}

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);
		BukkitTools.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled())
			return;
		
		Resident mayor = town.getMayor();
		TownyWorld townyWorld = town.getHomeblockWorld();
		
		// Remove the Town's spawn particle.
		if (town.hasSpawn()) {
			try {
				TownyUniverse.getInstance().removeSpawnPoint(town.getSpawn());
			} catch (TownyException ignored) {}
		}
		
		removeTownBlocks(town);

		List<Resident> toSave = new ArrayList<>(town.getResidents());

		if (town.hasNation()) {
			town.removeNation();
		}

		for (Resident resident : toSave) {
			resident.clearModes();
			resident.removeTown();
		}
		
		// Look for residents inside of this town's jail(s) and free them, more than 
		// likely the above removeTownBlocks(town) will have already set them free. 
		new ArrayList<>(TownyUniverse.getInstance().getJailedResidentMap()).stream()
			.filter(resident -> resident.hasJailTown(town.getName()))
			.forEach(resident -> JailUtil.unJailResident(resident, UnJailReason.JAIL_DELETED));

		if (TownyEconomyHandler.isActive())
			try {
				town.getAccount().payTo(town.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.getAccount().removeAccount();
			} catch (Exception ignored) {
			}

		if (townyWorld != null) {
			try {
				townyWorld.removeTown(town);
			} catch (NotRegisteredException e) {
				// Must already be removed
			}
			saveWorld(townyWorld);
		}

		try {
			universe.unregisterTown(town);
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		
		plugin.resetCache();
		deleteTown(town);
		
		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town, mayor.getUUID()));
		
		TownyMessaging.sendGlobalMessage(Translation.of("msg_del_town2", town.getName()));
	}

	@Override
	public void removeNation(Nation nation) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation);
		BukkitTools.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled())
			return;

		Resident king = null;
		if (nation.hasKing())
			king = nation.getKing();
		
		// Remove the Nation's spawn particle.
		if (nation.hasSpawn()) {
			try {
				TownyUniverse.getInstance().removeSpawnPoint(nation.getSpawn());
			} catch (TownyException ignored) {}
		}
		
		//search and remove from all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<>();
		for (Nation toCheck : new ArrayList<>(universe.getNations()))
			if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
				try {
					if (toCheck.hasAlly(nation))
						toCheck.removeAlly(nation);
					else
						toCheck.removeEnemy(nation);

					toSaveNation.add(toCheck);
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
			}

		for (Nation toCheck : toSaveNation)
			saveNation(toCheck);

		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive())
			try {
				nation.getAccount().payTo(nation.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.getAccount().removeAccount();
			} catch (Exception ignored) {
			}

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<>(nation.getTowns());
		nation.clear();

		try {
			universe.unregisterNation(nation);
		} catch (NotRegisteredException e) {
			// Just print out the exception. Very unlikely to happen.
			e.printStackTrace();
		}

		for (Town town : toSave) {

			for (Resident res : town.getResidents()) {
				if (res.hasTitle() || res.hasSurname()) {
					res.setTitle("");
					res.setSurname("");
				}
				res.updatePermsForNationRemoval();
				res.save();
			}
			try {
				town.setNation(null);
			} catch (AlreadyRegisteredException ignored) {
				// Cannot reach AlreadyRegisteredException
			}
			town.save();
			BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(town, nation));			
		}

		plugin.resetCache();
		
		UUID kingUUID = null;
		if (king != null)
			kingUUID = king.getUUID();

		BukkitTools.getPluginManager().callEvent(new DeleteNationEvent(nation, kingUUID));
	}

	@Override
	public void removeWorld(TownyWorld world) throws UnsupportedOperationException {

		deleteWorld(world);
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeJail(Jail jail) {
		// Unjail residents jailed here.
		new ArrayList<>(TownyUniverse.getInstance().getJailedResidentMap()).stream()
			.filter(resident -> resident.getJail().getUUID().equals(jail.getUUID()))
			.forEach(resident -> JailUtil.unJailResident(resident, UnJailReason.JAIL_DELETED));
		
		// Delete cells and spawnparticles.
		if (jail.hasCells())
			jail.removeAllCells();
		
		// Remove Town's record of the jail.
		if (jail.getTown() != null)
			jail.getTown().removeJail(jail);
		
		// Unregister the jail from the Universe.
		TownyUniverse.getInstance().unregisterJail(jail);
		
		deleteJail(jail);
	}

	@Override
	public void removePlotGroup(PlotGroup group) {
		TownyUniverse.getInstance().unregisterGroup(group);
		deletePlotGroup(group);
	}
	
	/*
	 * Rename Object Methods
	 */

	@Override
	public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();
		
		String oldName;

		try {

			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(newName);
			} catch (InvalidNameException e) {
				throw new NotRegisteredException(e.getMessage());
			}

			if (TownyUniverse.getInstance().hasTown(filteredName))
				throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

			List<Resident> toSave = new ArrayList<>(town.getResidents());
			boolean isCapital = false;
			Nation nation = null;
			double townBalance = 0.0;
			oldName = town.getName();

			// Save the towns bank balance to set in the new account.
			// Clear accounts
			if (TownyEconomyHandler.isActive())
				try {
					townBalance = town.getAccount().getHoldingBalance();					
					if (TownySettings.isEcoClosedEconomyEnabled()){
						town.getAccount().deposit(townBalance, "Town Rename");
					} 
					town.getAccount().removeAccount();
					
				} catch (Exception ignored) {
					TownyMessaging.sendErrorMsg("The bank balance for the town " + oldName + ", could not be received from the economy plugin and will not be able to be converted.");
				}
				
			UUID oldUUID = town.getUUID();
			long oldregistration = town.getRegistered();

			// Store the nation in case we have to update the capitol
			if (town.hasNation()) {
				nation = town.getNation();
				isCapital = town.isCapital();
			}

			TownyWorld world = town.getHomeblockWorld(); // TODO: This was added because renaming was throwing an NRE
			if (world.hasTown(town))                     // At some point worlds storing Towns will have to be re-evaluated.
				world.removeTown(town);                  // Worlds holding Towns is only useful when it comes to checking 
			                                             // distances between townblocks.

			/*                         
			 * Tidy up old files.
			 * Has to be done here else the town no longer exists
			 * and the file move command may fail.
			 */
			deleteTown(town);

			/*
			 * Remove the old town from the townsMap
			 * and rename to the new name
			 */
			// Re-register the town in the unvierse maps
			universe.unregisterTown(town);
			town.setName(filteredName);
			universe.registerTown(town);
			world.addTown(town);

			// If this was a nation capitol
			if (isCapital) {
				nation.setCapital(town);
			}
			town.setUUID(oldUUID);
			town.setRegistered(oldregistration);
			if (TownyEconomyHandler.isActive()) {
				town.getAccount().setName(TownySettings.getTownAccountPrefix() + town.getName());
				town.getAccount().setBalance(townBalance, "Rename Town - Transfer to new account");
			}

			for (Resident resident : toSave) {
				saveResident(resident);
			}

			// Update all townBlocks with the new name

			for (TownBlock townBlock : town.getTownBlocks()) {
				//townBlock.setTown(town);
				saveTownBlock(townBlock);
			}
			
			if (town.hasPlotGroups())
				for (PlotGroup pg : town.getPlotGroups()) {
					pg.setTown(town);
					savePlotGroup(pg);
				}

			saveTown(town);
			saveWorld(town.getHomeblockWorld());

			if (nation != null) {
				saveNation(nation);
			}

		} finally {
			lock.unlock();
		}

		BukkitTools.getPluginManager().callEvent(new RenameTownEvent(oldName, town));
	}
		
	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();

		String oldName;

		try {

			String filteredName;

			try {
				filteredName = NameValidation.checkAndFilterName(newName);
			} catch (InvalidNameException e) {
				throw new NotRegisteredException(e.getMessage());
			}

			if (universe.hasNation(filteredName))
				throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

			List<Town> toSave = new ArrayList<>(nation.getTowns());
			double nationBalance = 0.0;

			// Save the nations bank balance to set in the new account.
			// Clear accounts
			if (TownyEconomyHandler.isActive())
				try {
					nationBalance = nation.getAccount().getHoldingBalance();
					if (TownySettings.isEcoClosedEconomyEnabled()){
						nation.getAccount().withdraw(nationBalance, "Nation Rename");
					}
					nation.getAccount().removeAccount();
					
				} catch (Exception ignored) {
					TownyMessaging.sendErrorMsg("The bank balance for the nation " + nation.getName() + ", could not be received from the economy plugin and will not be able to be converted.");
				}

			//Tidy up old files
			deleteNation(nation);

			/*
			 * Remove the old nation from the nationsMap
			 * and rename to the new name
			 */
			oldName = nation.getName();
			universe.unregisterNation(nation);
			nation.setName(filteredName);
			universe.registerNation(nation);

			if (TownyEconomyHandler.isActive()) {
				nation.getAccount().setName(TownySettings.getNationAccountPrefix() + nation.getName());
				nation.getAccount().setBalance(nationBalance, "Rename Nation - Transfer to new account");
			}

			for (Town town : toSave) {
				saveTown(town);
			}

			saveNation(nation);

			//search and update all ally/enemy lists
			Nation oldNation = new Nation(oldName);
			List<Nation> toSaveNation = new ArrayList<>(universe.getNations());
			for (Nation toCheck : toSaveNation)
				if (toCheck.hasAlly(oldNation) || toCheck.hasEnemy(oldNation)) {
					try {
						if (toCheck.hasAlly(oldNation)) {
							toCheck.removeAlly(oldNation);
							toCheck.addAlly(nation);
						} else {
							toCheck.removeEnemy(oldNation);
							toCheck.addEnemy(nation);
						}
					} catch (NotRegisteredException e) {
						e.printStackTrace();
					}
				} else
					toSave.remove(toCheck);

			for (Nation toCheck : toSaveNation)
				saveNation(toCheck);

		} finally {
			lock.unlock();
		}

		BukkitTools.getPluginManager().callEvent(new RenameNationEvent(oldName, nation));
	}

	@Override
	public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException {
		// Create new one
		group.setName(newName);
		
		// Save
		savePlotGroup(group);
	}

	@Override
	public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException {
		
		lock.lock();
		
		String oldName = resident.getName();
		
		try {
			double balance = 0.0D;

			// Get balance in case this a server using ico5.  
			if (TownyEconomyHandler.isActive() && TownyEconomyHandler.getVersion().startsWith("iConomy 5")) {
				balance = resident.getAccount().getHoldingBalance();
				resident.getAccount().removeAccount();				
			}
			// Change account name over.
			if (TownyEconomyHandler.isActive())
				resident.getAccount().setName(newName);
			
			// Remove the resident from the universe name storage.
			universe.unregisterResident(resident);
			//rename the resident
			resident.setName(newName);
			// Re-register the resident with the new name.
			universe.registerResident(resident);
			// Set the economy account balance in ico5 (because it doesn't use UUIDs.)
			if (TownyEconomyHandler.isActive() && TownyEconomyHandler.getVersion().startsWith("iConomy 5")) {
				resident.getAccount().setName(resident.getName());
				resident.getAccount().setBalance(balance, "Rename Player - Transfer to new account");				
			}
			
			// Save resident with new name.
			saveResident(resident);

			// Save townblocks resident owned personally with new name.
			for(TownBlock tb: resident.getTownBlocks()){
				saveTownBlock(tb);				
			}
			
			// Save the town if the player was the mayor.
			if (resident.isMayor())
				saveTown(resident.getTown());
			
			// Make an oldResident with the previous name for use in searching friends/outlawlists/deleting the old resident file.
			Resident oldResident = new Resident(oldName);
			
			// Search and update all friends lists
			List<Resident> toSaveResident = new ArrayList<>(universe.getResidents());
			for (Resident toCheck : toSaveResident){
				if (toCheck.hasFriend(oldResident)) {
					toCheck.removeFriend(oldResident);
					toCheck.addFriend(resident);
				}
			}
			for (Resident toCheck : toSaveResident)
				saveResident(toCheck);
			
			// Search and update all outlaw lists.
			List<Town> toSaveTown = new ArrayList<>(TownyUniverse.getInstance().getTowns());
			for (Town toCheckTown : toSaveTown) {
				if (toCheckTown.hasOutlaw(oldResident)) {
					toCheckTown.removeOutlaw(oldResident);
					toCheckTown.addOutlaw(resident);
				}
			}
			for (Town toCheckTown : toSaveTown)
				saveTown(toCheckTown);	

			//delete the old resident and tidy up files
			deleteResident(oldResident);

		} finally {
			lock.unlock();			
		}
		
		BukkitTools.getPluginManager().callEvent(new RenameResidentEvent(oldName, resident));
	}
	
	/*
	 * PlotBlockData methods
	 */
	
	/**
	 * Save PlotBlockData
	 *
	 * @param plotChunk - Plot for data to be saved for.
	 * @return true if saved
	 */
	@Override
	public boolean savePlotData(PlotBlockData plotChunk) {
        String path = getPlotFilename(plotChunk);
        
        queryQueue.add(() -> {
			File file = new File(dataFolderPath + File.separator + "plot-block-data" + File.separator + plotChunk.getWorldName());
			FileMgmt.savePlotData(plotChunk, file, path);
		});
		
		return true;
	}

	/**
	 * Load PlotBlockData
	 *
	 * @param worldName - World in which to load PlotBlockData for.
	 * @param x - Coordinate for X.
	 * @param z - Coordinate for Z.
	 * @return PlotBlockData or null
	 */
	@Override
	public PlotBlockData loadPlotData(String worldName, int x, int z) {

		try {
			TownyWorld world = getWorld(worldName);
			TownBlock townBlock = new TownBlock(x, z, world);

			return loadPlotData(townBlock);
		} catch (NotRegisteredException e) {
			// Failed to get world
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Load PlotBlockData for regen at unclaim
	 * 
	 * First attempts to load a .zip file containing the data file.
	 * Fallback attempts to load old .data files instead.
	 * 
	 * Once it finds a zip or data file it will send it to be unpacked 
	 * by {@link #loadDataStream(PlotBlockData, InputStream)}
	 * which will return the populated PlotBlockData object. 
	 *
	 * @param townBlock - townBlock being reverted
	 * @return PlotBlockData or null
	 */
    @Override
    public PlotBlockData loadPlotData(TownBlock townBlock) {

    	PlotBlockData plotBlockData = null;
		try {
			plotBlockData = new PlotBlockData(townBlock);
		} catch (NullPointerException e1) {
			TownyMessaging.sendErrorMsg("Unable to load plotblockdata for townblock: " + townBlock.getWorldCoord().toString() + ". Skipping regeneration for this townBlock.");
			return null;
		}
        
        String fileName = getPlotFilename(townBlock);
        if (isFile(fileName)) {
        	/*
        	 * Attempt to load .zip file's inner .data file.
        	 */
        	try (ZipFile zipFile = new ZipFile(fileName)) {
				InputStream stream = zipFile.getInputStream(zipFile.entries().nextElement());
				return loadDataStream(plotBlockData, stream);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			

        } else if (isFile(getLegacyPlotFilename(townBlock))) {
        	/*
        	 * Attempt to load legacy .data files.
        	 */
        	try {
    			return loadDataStream(plotBlockData, new FileInputStream(getLegacyPlotFilename(townBlock)));
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    			return null;
    		}
        }
        
        return null;
    }

    /**
     * Loads PlotBlockData from an InputStream provided by 
     * {@link #loadPlotData(TownBlock)}
     * 
     * @param plotBlockData - plotBlockData object to populate with block array.
     * @param stream - InputStream used to populate the plotBlockData.
     * @return PlotBlockData object populated with blocks.
     */
    private PlotBlockData loadDataStream(PlotBlockData plotBlockData, InputStream stream) {
    	int version = 0;
    	List<String> blockArr = new ArrayList<>();
    	String value;
        try (DataInputStream fin = new DataInputStream(stream)) {
            
            //read the first 3 characters to test for version info
            fin.mark(3);
            byte[] key = new byte[3];
            fin.read(key, 0, 3);
            String test = new String(key);
            
            if (elements.fromString(test) == elements.VER) {// Read the file version
                version = fin.read();
                plotBlockData.setVersion(version);
                
                // next entry is the plot height
                plotBlockData.setHeight(fin.readInt());
            } else {
                /*
                 * no version field so set height
                 * and push rest to queue
                 */
                plotBlockData.setVersion(version);
                // First entry is the plot height
                fin.reset();
                plotBlockData.setHeight(fin.readInt());
                blockArr.add(fin.readUTF());
                blockArr.add(fin.readUTF());
            }
            
            /*
             * Load plot block data based upon the stored version number.
             */
            switch (version) {
                
                default:
                case 4:
                case 3:
                case 1:
                    
                    // load remainder of file
                    while ((value = fin.readUTF()) != null) {
                        blockArr.add(value);
                    }
                    
                    break;
                
                case 2: {
                    
                    // load remainder of file
                    int temp = 0;
                    while ((temp = fin.readInt()) >= 0) {
                        blockArr.add(temp + "");
                    }
                    
                    break;
                }
            }
            
            
        } catch (EOFException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        plotBlockData.setBlockList(blockArr);
        plotBlockData.resetBlockListRestored();
        return plotBlockData;
    }
    
    @Override
	public void deletePlotData(PlotBlockData plotChunk) {
		File file = new File(getPlotFilename(plotChunk));
		queryQueue.add(new DeleteFileTask(file, true));
	}

	private String getPlotFilename(PlotBlockData plotChunk) {

		return dataFolderPath + File.separator + "plot-block-data" + File.separator + plotChunk.getWorldName() + File.separator + plotChunk.getX() + "_" + plotChunk.getZ() + "_" + plotChunk.getSize() + ".zip";
	}

	private String getPlotFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "plot-block-data" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".zip";
	}

	public String getLegacyPlotFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "plot-block-data" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}
	
	private boolean isFile(String fileName) {
		File file = new File(fileName);
		return file.exists() && file.isFile();
	}
	
	/*
	 * RegenList and SnapshotList methods
	 */
	
	@Override
	public boolean loadRegenList() {
		
		TownyMessaging.sendDebugMsg("Loading Regen List");
		
		String line = null;
		
		String[] split;
		PlotBlockData plotData;
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "regen.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {
					split = line.split(",");
					plotData = loadPlotData(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]));
					if (plotData != null) {
						TownyRegenAPI.addPlotChunk(plotData, false);
					}
				}
			
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Regen List at " + line + ", in towny\\data\\regen.txt");
			e.printStackTrace();
			return false;
			
		}
		
	}
	
	@Override
	public boolean loadSnapshotList() {
		
		TownyMessaging.sendDebugMsg("Loading Snapshot Queue");
		
		String line = null;
		
		String[] split;
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "snapshot_queue.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {
					split = line.split(",");
					WorldCoord worldCoord = new WorldCoord(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]));
					TownyRegenAPI.addWorldCoord(worldCoord);
				}
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Snapshot Queue List at " + line + ", in towny\\data\\snapshot_queue.txt");
			e.printStackTrace();
			return false;
			
		}
		
	}
	
	protected final String serializeMetadata(TownyObject obj) {
		return DataFieldIO.serializeCDFs(obj.getMetadata());
	}
	
	@Override
	public boolean saveRegenList() {
        queryQueue.add(() -> {
        	File file = new File(dataFolderPath + File.separator + "regen.txt");
        	
			Collection<String> lines = TownyRegenAPI.getPlotChunks().values().stream()
				.map(data -> data.getWorldName() + "," + data.getX() + "," + data.getZ())
				.collect(Collectors.toList());
			
			FileMgmt.listToFile(lines, file.getPath());
		});

		return true;
	}

	@Override
	public boolean saveSnapshotList() {
       queryQueue.add(() -> {
       		List<String> coords = new ArrayList<>();
       		while (TownyRegenAPI.hasWorldCoords()) {
			   	WorldCoord worldCoord = TownyRegenAPI.getWorldCoord();
			   	coords.add(worldCoord.getWorldName() + "," + worldCoord.getX() + "," + worldCoord.getZ());
		    }
       		
       		FileMgmt.listToFile(coords, dataFolderPath + File.separator + "snapshot_queue.txt");
	   });
       
       return true;
	}

	/*
	 * Misc methods follow below
	 */
	
	@Override
	public void deleteFile(String fileName) {
		File file = new File(fileName);
		queryQueue.add(new DeleteFileTask(file, true));
	}

	/**
	 * @param town - Town to validate outpost spawns of
	 * @author - Articdive | Author note is only for people to know who wrote it and
	 *         who to ask, not to creditize
	 */
	public static void validateTownOutposts(Town town) {
		List<Location> validoutpostspawns = new ArrayList<>();
		if (town != null && town.hasOutpostSpawn()) {
			for (Location outpostSpawn : town.getAllOutpostSpawns()) {
				TownBlock outpostSpawnTB = TownyAPI.getInstance().getTownBlock(outpostSpawn);
				if (outpostSpawnTB != null) {
					validoutpostspawns.add(outpostSpawn);
				}
			}
			town.setOutpostSpawns(validoutpostspawns);
		}
	}

	/** 
	 * Merges the succumbingNation into the prevailingNation.
	 * 
	 * @param succumbingNation - Nation to be removed, towns put into prevailingNation.
	 * @param prevailingNation - Nation which survives, absorbs other nation's towns.
	 * 
	 * @author LlmDl
	 */
	public void mergeNation(Nation succumbingNation, Nation prevailingNation) {

		if (TownyEconomyHandler.isActive())
			succumbingNation.getAccount().payTo(succumbingNation.getAccount().getHoldingBalance(), prevailingNation, "Nation merge bank accounts.");

		
		lock.lock();
		List<Town> towns = new ArrayList<>(succumbingNation.getTowns());
		for (Town town : towns) {			
			town.removeNation();
			try {
				town.setNation(prevailingNation);
			} catch (AlreadyRegisteredException ignored) {
			}
			saveTown(town);
		}
		lock.unlock();
	}

	/**
	 * Merges the mergeFrom town into the mergeInto town.
	 * @param mergeInto The town that the other town merges into.
	 * @param mergeFrom The town that will be deleted.
	 */
	public void mergeTown(Town mergeInto, Town mergeFrom) {
		if (TownyEconomyHandler.isActive() && mergeFrom.getAccount().getHoldingBalance() > 0)
			mergeFrom.getAccount().payTo(mergeFrom.getAccount().getHoldingBalance(), mergeInto, Translation.of("msg_town_merge_transaction_reason"));

		lock.lock();
		boolean isSameNation = false;
		if (mergeInto.hasNation() && mergeFrom.hasNation()) {
			try {
				isSameNation = mergeInto.getNation().hasTown(mergeFrom);
			} catch (NotRegisteredException ignored) {}
		}
		String mayorName = mergeFrom.getMayor().getName();
		List<Jail> jails = TownyUniverse.getInstance().getJailUUIDMap().values().stream()
				.filter(jail -> jail.getTown().equals(mergeFrom))
				.collect(Collectors.toList());
		List<Location> outposts = new ArrayList<Location>(mergeFrom.getAllOutpostSpawns());

		mergeInto.addPurchasedBlocks(mergeFrom.getPurchasedBlocks());
		mergeInto.addBonusBlocks(mergeFrom.getBonusBlocks());

		for (TownBlock tb : mergeFrom.getTownBlocks()) {
			tb.setTown(mergeInto);
			tb.save();
		}
		
		List<Resident> residents = new ArrayList<Resident>(mergeFrom.getResidents());
		for (Resident resident : residents) {
			try {
				if (mergeInto.hasOutlaw(resident)) {
					resident.removeTown();
					continue;
				}
				
				List<String> nationRanks = new ArrayList<String>(resident.getNationRanks());
				
				resident.removeTown();
				resident.setTown(mergeInto);

				if (isSameNation) {
					for (String rank : nationRanks)
						resident.addNationRank(rank);
				}
				resident.save();
			} catch (TownyException ignored) {}
		}

		for (Resident outlaw : mergeFrom.getOutlaws()) {
			if (!mergeInto.hasOutlaw(outlaw) && !mergeInto.hasResident(outlaw)) {
				try {
					mergeInto.addOutlaw(outlaw);
				} catch (AlreadyRegisteredException ignored) {}
			}
		}

		for (Jail jail : jails) {
			TownBlock jailPlot = jail.getTownBlock();
			if (jailPlot.getType() != TownBlockType.JAIL)
				jailPlot.setType(TownBlockType.JAIL);
			
			jail.setTown(mergeInto);
		}

		for (Location outpost : outposts)
			mergeInto.addOutpostSpawn(outpost);

		lock.unlock();
		removeTown(mergeFrom, false);

		mergeInto.save();
		TownyMessaging.sendGlobalMessage(Translation.of("msg_town_merge_success", mergeFrom.getName(), mayorName, mergeInto.getName()));
	}
	
	public List<UUID> toUUIDList(Collection<Resident> residents) {
		return residents.stream().filter(Resident::hasUUID).map(Resident::getUUID).collect(Collectors.toList());
	}
	
	public UUID[] toUUIDArray(String[] uuidArray) {
		UUID[] uuids = new UUID[uuidArray.length];
		
		for (int i = 0; i < uuidArray.length; i++) {
			try {
				uuids[i] = UUID.fromString(uuidArray[i]);
			} catch (IllegalArgumentException ignored) {}
		}
		
		return uuids;
	}
}
