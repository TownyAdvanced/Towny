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
import com.palmergames.bukkit.towny.event.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author ElgarL
 * 
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {
	final String rootFolderPath;
	final String dataFolderPath;
	final String settingsFolderPath;
	final String logFolderPath;
	final String backupFolderPath;
	
	protected final Queue<Runnable> queryQueue = new ConcurrentLinkedQueue<>();
	private final BukkitTask task;
	
	public TownyDatabaseHandler(Towny plugin, TownyUniverse universe) {
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

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getResidentMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("A resident with the name " + filteredName + " is already in use.");

		universe.getResidentMap().put(filteredName.toLowerCase(), new Resident(filteredName));
		universe.getResidentsTrie().addKey(filteredName);
	}

	/**
	 * Create a new town from a name
	 * 
	 * @param name town name
	 * @throws AlreadyRegisteredException thrown if town already exists.
	 * @throws NotRegisteredException thrown if town has an invalid name.
	 * 
	 * @deprecated Use {@link TownyUniverse#newTown(String)} instead.
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
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getNationsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		universe.getNationsMap().put(filteredName.toLowerCase(), new Nation(filteredName));
		universe.getNationsTrie().addKey(filteredName);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException {
		
		if (universe.getWorldMap().containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		universe.getWorldMap().put(name.toLowerCase(), new TownyWorld(name));
	}

	public void newPlotGroup(PlotGroup group) {
		universe.getGroups().add(group);
	}

	/*
	 * Are these objects in the TownyUniverse maps?
	 */
	
	@Override
	public boolean hasResident(String name) {
		try {
			return TownySettings.isFakeResident(name) || universe.getResidentMap().containsKey(NameValidation.checkAndFilterPlayerName(name).toLowerCase());
		} catch (InvalidNameException e) {
			return false;
		}
	}

	/**
	 * Checks if a town with the name exists.
	 * 
	 * @param name Name of the town to check.
	 * @return whether the town exists.
	 * 
	 * @deprecated Use {@link TownyUniverse#hasTown(String)} instead.
	 */
	@Deprecated
	@Override
	public boolean hasTown(String name) {
		return universe.hasTown(name);
	}

	@Override
	public boolean hasNation(String name) {

		return universe.getNationsMap().containsKey(name.toLowerCase());
	}

	/**
	 * Gets the keys of TownyUniverse's Resident Map
	 * 
	 * @return Returns the {@link Map#keySet()} of {@link TownyUniverse#getResidentMap()}
	 * 
	 * @deprecated No longer used by Towny. Messing with the Resident map is ill advised.
	 */
	@Override
	@Deprecated
	public Set<String> getResidentKeys() {

		return universe.getResidentMap().keySet();
	}

	/**
	 * Gets the keys of TownyUniverse's Towns Map.
	 * 
	 * @return Returns {@link Map#keySet()} of {@link TownyUniverse#getTownsMap()}
	 * 
	 * @deprecated No longer used by Towny. Messing with the Towns map is ill advised.
	 */
	@Override
	@Deprecated
	public Set<String> getTownsKeys() {

		return universe.getTownsMap().keySet();
	}

	/**
	 * Gets the keys of TownyUniverse's Nations Map
	 * 
	 * @return Returns {@link Map#keySet()} of {@link TownyUniverse#getNationsMap()}
	 * 
	 * @deprecated No longer used by Towny. Messing with the Nations map is ill advised.
	 */
	@Override
	@Deprecated
	public Set<String> getNationsKeys() {

		return universe.getNationsMap().keySet();
	}
	
	/*
	 * getResident methods.
	 */
	
	@Override
	public List<Resident> getResidents(Player player, String[] names) {

		List<Resident> invited = new ArrayList<>();
		for (String name : names)
			try {
				Resident target = getResident(name);
				invited.add(target);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		return invited;
	}

	@Override
	public List<Resident> getResidents(String[] names) {

		List<Resident> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getResident(name));
			} catch (NotRegisteredException ignored) {
			}
		return matches;
	}

	@Override
	public List<Resident> getResidents() {

		return new ArrayList<>(universe.getResidentMap().values());
	}

	@Override
	public Resident getResident(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterPlayerName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}

		if (!hasResident(name)) {

			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));

		} else if (TownySettings.isFakeResident(name)) {

			Resident resident = new Resident(name);
			resident.setNPC(true);

			return resident;

		}

		return universe.getResidentMap().get(name);

	}

	@Override
	public List<Resident> getResidentsWithoutTown() {

		List<Resident> residentFilter = new ArrayList<>();
		for (Resident resident : universe.getResidentMap().values())
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
	 * @return a list of all towns.
	 * 
	 * @deprecated Use {@link TownyUniverse#getTowns()} instead.
	 */
	@Deprecated
	@Override
	public List<Town> getTowns() {

		return new ArrayList<>(universe.getTowns());
	}

	/**
	 * Gets a town from the passed-in name.
	 * @param name Town Name
	 * @return town associated with the name.
	 * @throws NotRegisteredException Town does not exist.
	 * 
	 * @deprecated Use {@link TownyUniverse#getTown(String)} instead.
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
	 * Returns the associated town with the passed-in uuid.
	 * 
	 * @param uuid UUID of the town to fetch.
	 *                
	 * @return town associated with the uuid.
	 * 
	 * @throws NotRegisteredException Thrown if town doesn't exist.
	 * 
	 * @deprecated Use {@link TownyUniverse#getTown(UUID)} instead.
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
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException ignored) {
			}
		return matches;
	}

	@Override
	public List<Nation> getNations() {

		return new ArrayList<>(universe.getNationsMap().values());
	}

	@Override
	public Nation getNation(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}

		if (!hasNation(name))
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return universe.getNationsMap().get(name.toLowerCase());
	}

	@Override
	public Nation getNation(UUID uuid) throws NotRegisteredException {
		String name = null;
		for (Nation nation : this.getNations()) {
			if (uuid.equals(nation.getUuid())) {
				name = nation.getName();
			}
		}

		if (name == null) {
			throw new NotRegisteredException(String.format("The town with uuid '%s' is not registered.", uuid));
		}
		
		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}
		
		return universe.getNationsMap().get(name);
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

	public PlotGroup getPlotObjectGroup(String townName, UUID groupID) {
		return universe.getGroup(townName, groupID);
	}

	public List<PlotGroup> getAllPlotGroups() {
		return new ArrayList<>(universe.getGroups());
	}

	/*
	 * Remove Object Methods
	 */
	
	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlawlists.
		for (Town townOutlaw : getTowns()) {
			if (townOutlaw.hasOutlaw(resident)) {
				townOutlaw.removeOutlaw(resident);
				saveTown(townOutlaw);
			}
		}

		// Remove resident from residents' friendslists.
		List<Resident> toSave = new ArrayList<>();
		for (Resident toCheck : universe.getResidentMap().values()) {		
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
		universe.getResidentMap().remove(resident.getName().toLowerCase());
		universe.getResidentsTrie().removeKey(resident.getName());

		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount() && TownyEconomyHandler.isActive())
			resident.getAccount().removeAccount();

		plugin.deleteCache(resident.getName());
		
		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident));
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {

		TownPreUnclaimEvent event = new TownPreUnclaimEvent(townBlock);
		BukkitTools.getPluginManager().callEvent(event);
		
		if (event.isCancelled())
			return;
		
		Town town = null;
//		Resident resident = null;                   - Removed in 0.95.2.5
//		try {
//			resident = townBlock.getResident();
//		} catch (NotRegisteredException ignored) {
//		}
		try {
			town = townBlock.getTown();
		} catch (NotRegisteredException ignored) {
		}

		TownyUniverse.getInstance().removeTownBlock(townBlock);
		deleteTownBlock(townBlock);

//		if (resident != null)           - Removed in 0.95.2.5, residents don't store townblocks in them.
//			saveResident(resident);

//		if (town != null)         		- Removed in 0.91.1.2, possibly fixing SQL database corruption 
//		    saveTown(town);				  occuring when towns are deleted. 

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
		removeTown(town, TownRuinSettings.getTownRuinsEnabled());
	}

	@Override
	public void removeTown(Town town, boolean delayFullRemoval) {
		
		TownyMessaging.sendGlobalMessage(Translation.of("msg_del_town", town.getName()));
		
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
		
		removeTownBlocks(town);

		if (town.hasSiege())
			removeSiege(town.getSiege(), SiegeSide.ATTACKERS);

		List<Resident> toSave = new ArrayList<>(town.getResidents());
		TownyWorld townyWorld = town.getHomeblockWorld();

		if (town.hasNation()) {
			town.removeNation();
		}

		for (Resident resident : toSave) {
			resident.clearModes();
			resident.removeTown();
		}
		
		// Look for residents inside of this town's jail and free them
		for (Resident jailedRes : TownyUniverse.getInstance().getJailedResidentMap()) {
			if (jailedRes.hasJailTown(town.getName())) {
                jailedRes.setJailed(0, town);
                saveResident(jailedRes);
            }
		}

		if (TownyEconomyHandler.isActive())
			try {
				town.getAccount().payTo(town.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.getAccount().removeAccount();
			} catch (Exception ignored) {
			}

		try {
			townyWorld.removeTown(town);
		} catch (NotRegisteredException e) {
			// Must already be removed
		}
		saveWorld(townyWorld);

		try {
			universe.unregisterTown(town);
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
		}
		
		plugin.resetCache();
		deleteTown(town);
		
		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town));
	}

	@Override
	public void removeNation(Nation nation) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation);
		BukkitTools.getPluginManager().callEvent(preEvent);
		
		Resident king = nation.getKing();

		if (preEvent.isCancelled())
			return;

		//search and remove from all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<>();
		for (Nation toCheck : new ArrayList<>(universe.getNationsMap().values()))
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

		//Remove all sieges
		for (Siege siege : new ArrayList<>(nation.getSieges()))
			removeSiege(siege, SiegeSide.DEFENDERS);

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<>(nation.getTowns());
		nation.clear();

		universe.getNationsTrie().removeKey(nation.getName().toLowerCase());
		universe.getNationsMap().remove(nation.getName().toLowerCase());

		for (Town town : toSave) {

			for (Resident res : getResidents()) {
				if (res.hasTitle() || res.hasSurname()) {
					res.setTitle("");
					res.setSurname("");
				}
				res.updatePermsForNationRemoval();
				TownyUniverse.getInstance().getDataSource().saveResident(res);
			}
			try {
				town.setNation(null);
			} catch (AlreadyRegisteredException ignored) {
				// Cannot reach AlreadyRegisteredException
			}
			TownyUniverse.getInstance().getDataSource().saveTown(town);
			BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(town, nation));			
		}

		plugin.resetCache();

		SiegeWarMoneyUtil.makeNationRefundAvailable(king);

		BukkitTools.getPluginManager().callEvent(new DeleteNationEvent(nation));
	}

	@Override
	public void removeWorld(TownyWorld world) throws UnsupportedOperationException {

		deleteWorld(world);
		throw new UnsupportedOperationException();
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

			if (hasTown(filteredName))
				throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

			// TODO: Delete/rename any invites.

			List<Resident> toSave = new ArrayList<>(town.getResidents());
			boolean isCapital = false;
			Nation nation = null;
			double townBalance = 0.0;
			oldName = town.getName();

			// Save the towns bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					townBalance = town.getAccount().getHoldingBalance();					
					if (TownySettings.isEcoClosedEconomyEnabled()){
						town.getAccount().deposit(townBalance, "Town Rename");
					} 
					town.getAccount().removeAccount();
					
				} catch (EconomyException ignored) {
				}
			UUID oldUUID = town.getUUID();
			long oldregistration = town.getRegistered();

			// Store the nation in case we have to update the capitol
			if (town.hasNation()) {
				nation = town.getNation();
				isCapital = town.isCapital();
			}

			TownyWorld world = town.getHomeblockWorld();
			world.removeTown(town);
			/*
			 * Tidy up old files.
			 * Has to be done here else the town no longer exists
			 * and the file move command may fail.
			 */
			deleteTown(town);
			if(town.hasSiege()) {
				deleteSiege(town.getSiege());
			}

			/*
			 * Remove the old town from the townsMap
			 * and rename to the new name
			 */
			// Re-register the town in the unvierse maps
			universe.unregisterTown(town);
			town.setName(filteredName);
			universe.registerTown(town);
			world.addTown(town);

			//Move/rename siege
			if(town.hasSiege()) {
				Siege siege = town.getSiege();
				String oldSiegeName = siege.getName();
				String newSiegeName = siege.getAttackingNation().getName() + "#vs#" + town.getName();
				//Update siege
				siege.setName(newSiegeName);
				//Update universe
				universe.getSiegesMap().remove(oldSiegeName.toLowerCase());
				universe.getSiegesMap().put(newSiegeName.toLowerCase(), siege);
			}

			// If this was a nation capitol
			if (isCapital) {
				nation.setCapital(town);
			}
			town.setUUID(oldUUID);
			town.setRegistered(oldregistration);
			if (TownySettings.isUsingEconomy()) {
				try {
					town.getAccount().setName(TownySettings.getTownAccountPrefix() + town.getName());
					town.getAccount().setBalance(townBalance, "Rename Town - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

			for (Resident resident : toSave) {
				saveResident(resident);
			}

			//search and update all resident's jailTown with new name.

            for (Resident toCheck : getResidents()){
                    if (toCheck.hasJailTown(oldName)) {
                        toCheck.setJailTown(newName);
                        
                        saveResident(toCheck);
                    }
            }
            
			// Update all townBlocks with the new name

			for (TownBlock townBlock : town.getTownBlocks()) {
				//townBlock.setTown(town);
				saveTownBlock(townBlock);
			}
			
			if (town.hasPlotGroups())
				for (PlotGroup pg : town.getPlotObjectGroups()) {
					pg.setTown(town);
					savePlotGroup(pg);
				}

			saveTown(town);
			//Save siege data
			if(town.hasSiege()) {
				saveSiege(town.getSiege());
				saveNation(town.getSiege().getAttackingNation());
			}
			saveSiegeList();
			savePlotGroupList();
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

			if (hasNation(filteredName))
				throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

			// TODO: Delete/rename any invites.

			List<Town> toSave = new ArrayList<>(nation.getTowns());
			double nationBalance = 0.0;

			// Save the nations bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					nationBalance = nation.getAccount().getHoldingBalance();
					if (TownySettings.isEcoClosedEconomyEnabled()){
						nation.getAccount().withdraw(nationBalance, "Nation Rename");
					}
					nation.getAccount().removeAccount();
					
				} catch (EconomyException ignored) {
				}

			UUID oldUUID = nation.getUuid();
			long oldregistration = nation.getRegistered();

			//Tidy up old files
			deleteNation(nation);
			for(Siege siege: new ArrayList<>(nation.getSieges())) {
				deleteSiege(siege);
			}

			/*
			 * Remove the old nation from the nationsMap
			 * and rename to the new name
			 */
			oldName = nation.getName();
			universe.getNationsMap().remove(oldName.toLowerCase());
			universe.getNationsTrie().removeKey(oldName);
			nation.setName(filteredName);
			universe.getNationsMap().put(filteredName.toLowerCase(), nation);
			universe.getNationsTrie().addKey(filteredName);

			//Move/rename sieges
			String oldSiegeName;
			String newSiegeName;
			for(Siege siege: nation.getSieges()) {
				oldSiegeName = siege.getName();
				newSiegeName = siege.getAttackingNation().getName() + "#vs#" + siege.getDefendingTown().getName();
				//Update siege
				siege.setName(newSiegeName);
				//Update universe
				universe.getSiegesMap().remove(oldSiegeName.toLowerCase());
				universe.getSiegesMap().put(newSiegeName.toLowerCase(), siege);
			}

			if (TownyEconomyHandler.isActive()) {
				try {
					nation.getAccount().setName(TownySettings.getNationAccountPrefix() + nation.getName());
					nation.getAccount().setBalance(nationBalance, "Rename Nation - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

			nation.setUuid(oldUUID);
			nation.setRegistered(oldregistration);

			for (Town town : toSave) {
				saveTown(town);
			}

			saveNation(nation);

			//Save sieges
			for(Siege siege: nation.getSieges()) {
				saveSiege(siege);
				saveTown(siege.getDefendingTown());
			}

			saveSiegeList();

			//search and update all ally/enemy lists
			Nation oldNation = new Nation(oldName);
			List<Nation> toSaveNation = new ArrayList<>(getNations());
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
		savePlotGroupList();

		// Delete the old group file.
		deletePlotGroup(group);
	}

	@Override
	public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException {
		
		lock.lock();
		
		String oldName = resident.getName();
		
		try {
			
			//data needed for a new resident
			double balance = 0.0D;
			Town town = null;
			long registered;
			long lastOnline;
			UUID uuid = null;
			boolean isMayor;
			boolean isJailed;
			boolean isNPC;
			int JailSpawn;
			
			if(TownyEconomyHandler.getVersion().startsWith("iConomy 5") && TownySettings.isUsingEconomy()){
				try {
					balance = resident.getAccount().getHoldingBalance();
					resident.getAccount().removeAccount();
				} catch (EconomyException ignored) {
				}				
			} else {
				resident.getAccount().setName(newName);
			}
			
			//get data needed for resident
			List<Resident> friends = resident.getFriends();
			List<String> nationRanks = resident.getNationRanks();
			TownyPermission permissions = resident.getPermissions();
			String surname = resident.getSurname();
			String title = resident.getTitle();
			if (resident.hasTown()) {
				town = resident.getTown();
			}
			Collection<TownBlock> townBlocks = resident.getTownBlocks();
			List<String> townRanks = resident.getTownRanks();
			registered = resident.getRegistered();			
			lastOnline = resident.getLastOnline();
			if (resident.hasUUID())
				uuid = resident.getUUID();
			isMayor = resident.isMayor();
			isNPC = resident.isNPC();
			isJailed = resident.isJailed();			
			JailSpawn = resident.getJailSpawn();
			
			if (resident.isJailed()) {
				try {
					universe.getJailedResidentMap().remove(universe.getDataSource().getResident(oldName));
					universe.getJailedResidentMap().add(universe.getDataSource().getResident(newName));
				} catch (Exception ignored) {
				}
			}
				
			
			//delete the resident and tidy up files
			deleteResident(resident);
		
			//remove old resident from residentsMap
			//rename the resident
			universe.getResidentMap().remove(oldName.toLowerCase());
			universe.getResidentsTrie().removeKey(oldName);
			resident.setName(newName);
			universe.getResidentMap().put(newName.toLowerCase(), resident);
			universe.getResidentsTrie().addKey(newName);
			
			//add everything back to the resident
			if (TownyEconomyHandler.getVersion().startsWith("iConomy 5") && TownySettings.isUsingEconomy()) {
				try {
					resident.getAccount().setName(resident.getName());
					resident.getAccount().setBalance(balance, "Rename Player - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}				
			}
			resident.setFriends(friends);
			resident.setNationRanks(nationRanks);
			resident.setPermissions(permissions.toString()); //not sure if this will work
			resident.setSurname(surname);
			resident.setTitle(title);
			resident.setTown(town);
			resident.setTownblocks(townBlocks);
			try {
				resident.setTownRanks(townRanks);
			} catch (ConcurrentModificationException ignored) {
				// If this gets tripped by TownyNameUpdater in the future we will at least not be deleting anyone, they just won't have their townranks.
			}
			resident.setRegistered(registered);
			resident.setLastOnline(lastOnline);
			if (uuid != null)
				resident.setUUID(uuid);
			if(isMayor)
				town.setMayor(resident);
			if (isNPC)
				resident.setNPC(true);
			resident.setJailed(isJailed);
			resident.setJailSpawn(JailSpawn);
			
			//save stuff
			saveResident(resident);
			if(town != null){
			    saveTown(town);
		    }
			for(TownBlock tb: townBlocks){
				saveTownBlock(tb);				
			}
			
			//search and update all friends lists
			//followed by outlaw lists
			Resident oldResident = new Resident(oldName);
			List<Resident> toSaveResident = new ArrayList<>(getResidents());
			for (Resident toCheck : toSaveResident){
				if (toCheck.hasFriend(oldResident)) {
					toCheck.removeFriend(oldResident);
					toCheck.addFriend(resident);
				}
			}
			for (Resident toCheck : toSaveResident)
				saveResident(toCheck);
			
			List<Town> toSaveTown = new ArrayList<>(getTowns());
			for (Town toCheckTown : toSaveTown) {
				if (toCheckTown.hasOutlaw(oldResident)) {
					toCheckTown.removeOutlaw(oldResident);
					toCheckTown.addOutlaw(resident);
				}
			}
			for (Town toCheckTown : toSaveTown)
				saveTown(toCheckTown);	
		
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
		
		lock.lock();
		Iterator<Town> towns = succumbingNation.getTowns().iterator();
		while (towns.hasNext()) {
			try {
				if (TownySettings.isUsingEconomy())
					succumbingNation.getAccount().payTo(succumbingNation.getAccount().getHoldingBalance(), prevailingNation, "Nation merge bank accounts.");
				Town town = towns.next();
				town.removeNation();
				try {
					town.setNation(prevailingNation);
				} catch (AlreadyRegisteredException ignored) {
				}
				saveTown(town);
			} catch (EconomyException ignored) {			
			}
			towns.remove();
		}
		lock.unlock();
	}

	@Override
	public List<Siege> getSieges() {
		return new ArrayList<>(universe.getSiegesMap().values());
	}

	@Override
	public void newSiege(String siegeName) throws AlreadyRegisteredException {

		lock.lock();

		try {
			if(universe.getSiegesMap().containsKey(siegeName.toLowerCase()))
				throw new AlreadyRegisteredException("Siege is already registered");

			Siege siege = new Siege(siegeName);

			universe.getSiegesMap().put(siegeName.toLowerCase(), siege);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public Siege getSiege(String siegeName) throws NotRegisteredException {
		if(!universe.getSiegesMap().containsKey(siegeName.toLowerCase())) {
			throw new NotRegisteredException("Siege not found");
		}
		return universe.getSiegesMap().get(siegeName.toLowerCase());
	}

	//Remove a particular siege, and all associated data
	@Override
	public void removeSiege(Siege siege, SiegeSide refundSideIfSiegeIsActive) {
		//If siege is active, initiate siege immunity for town, and return war chest
		if(siege.getStatus().isActive()) {
			siege.setActualEndTime(System.currentTimeMillis());
			SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);

			if(refundSideIfSiegeIsActive == SiegeSide.ATTACKERS)
				SiegeWarMoneyUtil.giveWarChestToAttackingNation(siege);
			else if (refundSideIfSiegeIsActive == SiegeSide.DEFENDERS)
				SiegeWarMoneyUtil.giveWarChestToDefendingTown(siege);
		}

		//Remove siege from town
		siege.getDefendingTown().setSiege(null);
		//Remove siege from nation
		siege.getAttackingNation().removeSiege(siege);
		//Remove siege from universe
		universe.getSiegesMap().remove(siege.getName().toLowerCase());

		//Save town
		saveTown(siege.getDefendingTown());
		//Save attacking nation
		saveNation(siege.getAttackingNation());
		//Delete siege file
		deleteSiege(siege);
		//Save siege list
		saveSiegeList();
	}

	@Override
	public Set<String> getSiegeKeys() {

		return universe.getSiegesMap().keySet();
	}

	// TODO: See if this is actually needed - LlmDl
	@Override
	public void removeTownFromNation(Towny plugin, Town town, Nation nation) {
		boolean removeNation = false;

		town.removeNation();

		removeNation = town.hasNation();
		if(removeNation) {
			removeNation(nation);
		} else {
			saveNation(nation);
			plugin.resetCache();
		}

		saveTown(town);
	}

	// TODO: See if this is actually needed - LlmDl
	@Override
	public void addTownToNation(Towny plugin, Town town,Nation nation) {
		try {
			town.setNation(nation);
			saveTown(town);
			plugin.resetCache();
			saveNation(nation); // Likely an unneeded save.
		} catch (AlreadyRegisteredException x) {
			return;   //Town already in nation
		}
	}
}
