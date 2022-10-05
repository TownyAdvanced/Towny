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
import com.palmergames.bukkit.towny.event.town.TownPreRuinedEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.town.TownUnclaimEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.DataFieldIO;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.WorldCoordEntityRemover;
import com.palmergames.bukkit.towny.regen.WorldCoordMaterialRemover;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
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
			plugin.getLogger().info("***** Warning *****");
			plugin.getLogger().info("***** Only Snapshots & Regen files in towny\\data\\ will be backed up!");
			plugin.getLogger().info("***** This does not include your residents/towns/nations.");
			plugin.getLogger().info("***** Make sure you have scheduled a backup in MySQL too!!!");
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
		
		if (universe.getWorldMap().containsKey(name.toLowerCase(Locale.ROOT)))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		universe.getWorldMap().put(name.toLowerCase(Locale.ROOT), new TownyWorld(name));
	}

	/*
	 * getResident methods.
	 */
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidents(String[])} instead.
	 */
	@Deprecated
	@Override
	public List<Resident> getResidents(String[] names) {
		return TownyAPI.getInstance().getResidents(names);
	}
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidents(UUID[])} instead.
	 */
	@Deprecated
	@Override
	public List<Resident> getResidents(UUID[] uuids) {
		return TownyAPI.getInstance().getResidents(uuids);
	}

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getResidentsWithoutTown()} instead.
	 */
	@Deprecated
	@Override
	public List<Resident> getResidentsWithoutTown() {
		return TownyAPI.getInstance().getResidentsWithoutTown();
	}
	
	/*
	 * getTowns methods.
	 */	
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTowns(String[])} instead.
	 */
	@Deprecated
	@Override
	public List<Town> getTowns(String[] names) {
		return TownyAPI.getInstance().getTowns(names);
	}

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTowns(List)} instead.
	 */
	@Deprecated
	@Override
	public List<Town> getTowns(List<UUID> uuids) {
		return TownyAPI.getInstance().getTowns(uuids);
	}

	/**
	 * @deprecated as of 0.97.5.18 use {@link TownyAPI#getTownsWithoutNation} instead.
	 */
	@Deprecated
	@Override
	public List<Town> getTownsWithoutNation() {
		return TownyAPI.getInstance().getTownsWithoutNation();
	}
	
	/*
	 * getNations methods.
	 */
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getNations(String[])} instead.
	 */
	@Deprecated
	@Override
	public List<Nation> getNations(String[] names) {
		return TownyAPI.getInstance().getNations(names);
	}

	/*
	 * getWorlds methods.
	 */

	/**
	 * @deprecated as of 0.97.5.18, Use {@link TownyUniverse#getWorld(String)} instead.
	 *  
	 * @param name Name of TownyWorld
	 * @return TownyWorld matching the name or Null.
	 */
	@Deprecated
	@Nullable
	@Override
	public TownyWorld getWorld(String name){
		return universe.getWorld(name);
	}

	/**
	 * @deprecated as of 0.97.5.18, Use {@link TownyUniverse#getTownyWorlds()} instead.
	 * 
	 * @return List of TownyWorlds.
	 */
	@Deprecated
	@Override
	public List<TownyWorld> getWorlds() {
		return universe.getTownyWorlds();
	}
	
	/*
	 * getTownblocks methods.
	 */

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTownBlocks} instead.
	 */
	@Deprecated
	@Override
	public Collection<TownBlock> getAllTownBlocks() {
		return universe.getTownBlocks().values();
	}
	
	/*
	 * getPlotGroups methods.
	 */

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyUniverse#getGroup(UUID)} instead.
	 */
	@Deprecated
	public PlotGroup getPlotObjectGroup(UUID groupID) {
		return universe.getGroup(groupID);
	}

	/**
	 * @deprecated since 0.97.5.18 use {@link TownyUniverse#getGroups()} instead.
	 * @return List of PlotGroups. 
	 */
	@Deprecated
	public List<PlotGroup> getAllPlotGroups() {
		return new ArrayList<>(universe.getGroups());
	}
	
	/**
	 * @deprecated since 0.97.5.18 use {@link TownyUniverse#getJails()} instead.
	 * @return List of jails. 
	 */
	@Deprecated
	public List<Jail> getAllJails() {
		return new ArrayList<>(universe.getJailUUIDMap().values());
	}

	/*
	 * Remove Object Methods
	 */
	
	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlaw & trusted lists.
		for (Town town : universe.getTowns()) {
			boolean save = false;
			
			if (town.hasOutlaw(resident)) {
				town.removeOutlaw(resident);
				save = true;
			}
			
			if (town.hasTrustedResident(resident)) {
				town.removeTrustedResident(resident);
				save = true;
			}
			
			if (save)
				town.save();
		}
		
		for (PlotGroup group : universe.getGroups()) {
			if (group.hasTrustedResident(resident)) {
				group.removeTrustedResident(resident);
				group.save();
			}
		}
		
		for (TownBlock townBlock : universe.getTownBlocks().values()) {
			if (townBlock.hasTrustedResident(resident)) {
				townBlock.removeTrustedResident(resident);
				townBlock.save();
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
		
		if (resident.hasTown()) {
			Town town = resident.getTownOrNull();

			if (town != null) {
				// Delete the town if there are no more residents
				if (town.getNumResidents() <= 1) {
					universe.getDataSource().removeTown(town);
				}

				resident.removeTown();
			}
		}

		if (resident.hasUUID() && !resident.isNPC())
			saveHibernatedResident(resident.getUUID(), resident.getRegistered());

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

		plugin.deleteCache(resident);
		
		BukkitTools.fireEvent(new DeletePlayerEvent(resident));
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {
		Town town = townBlock.getTownOrNull();
		if (town == null)
			// Log as error because TownBlocks *must* have a town.
			logger.error(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", townBlock.getWorld().getName(), townBlock.getX(), townBlock.getZ()));

		TownPreUnclaimEvent event = new TownPreUnclaimEvent(town, townBlock);
		if (BukkitTools.isEventCancelled(event)) {
			// Log as Warn because the event has been processed
			if (!event.getCancelMessage().isEmpty())
				logger.warn(event.getCancelMessage());
			return;
		}
		
		if (townBlock.isJail() && townBlock.getJail() != null)
			removeJail(townBlock.getJail());

		if (TownySettings.getTownUnclaimCoolDownTime() > 0)
			CooldownTimerTask.addCooldownTimer(townBlock.getWorldCoord().toString(), CooldownType.TOWNBLOCK_UNCLAIM);

		universe.removeTownBlock(townBlock);
		deleteTownBlock(townBlock);

		if (townBlock.getWorld().isDeletingEntitiesOnUnclaim())
			WorldCoordEntityRemover.addToQueue(townBlock.getWorldCoord());

		if (townBlock.getWorld().isUsingPlotManagementDelete())
			WorldCoordMaterialRemover.addToQueue(townBlock.getWorldCoord());

		// Move the plot to be restored
		if (townBlock.getWorld().isUsingPlotManagementRevert())
			TownyRegenAPI.addToRegenQueueList(townBlock.getWorldCoord(), true);

		// Raise an event to signal the unclaim
		BukkitTools.fireEvent(new TownUnclaimEvent(town, townBlock.getWorldCoord()));
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
		removeTown(town, TownySettings.getTownRuinsEnabled() && !town.isRuined());
	}

	@Override
	public void removeTown(Town town, boolean delayFullRemoval) {
		if (delayFullRemoval) {
			/*
			 * When Town ruining is active, send the Town into a ruined state, prior to real
			 * removal, if the TownPreRuinedEvent is not cancelled.
			 */
			TownPreRuinedEvent tpre = new TownPreRuinedEvent(town);
			if (!BukkitTools.isEventCancelled(tpre)) {
				TownRuinUtil.putTownIntoRuinedState(town);
				return;
			}
		}

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);
		if (BukkitTools.isEventCancelled(preEvent))
			return;
		
		Resident mayor = town.getMayor();
		TownyWorld townyWorld = town.getHomeblockWorld();
		
		// Remove the Town's spawn particle.
		if (town.hasSpawn()) {
			try {
				universe.removeSpawnPoint(town.getSpawn());
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
		new ArrayList<>(universe.getJailedResidentMap()).stream()
			.filter(resident -> resident.hasJailTown(town.getName()))
			.forEach(resident -> JailUtil.unJailResident(resident, UnJailReason.JAIL_DELETED));

		if (TownyEconomyHandler.isActive())
			town.getAccount().removeAccount();

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
		
		BukkitTools.fireEvent(new DeleteTownEvent(town, mayor));
		
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_town2", town.getName()));
	}

	@Override
	public void removeNation(Nation nation) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation);
		if (BukkitTools.isEventCancelled(preEvent))
			return;

		Resident king = null;
		if (nation.hasKing())
			king = nation.getKing();
		
		// Remove the Nation's spawn particle.
		if (nation.hasSpawn()) {
			try {
				universe.removeSpawnPoint(nation.getSpawn());
			} catch (TownyException ignored) {}
		}
		
		//search and remove from all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<>();
		for (Nation toCheck : new ArrayList<>(universe.getNations()))
			if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
				if (toCheck.hasAlly(nation))
					toCheck.removeAlly(nation);
				else
					toCheck.removeEnemy(nation);

				toSaveNation.add(toCheck);
			}

		for (Nation toCheck : toSaveNation)
			saveNation(toCheck);
		
		// Search and remove any ally invites sent to this nation.
		for (Nation toCheck : new ArrayList<>(universe.getNations()))
			for (Invite invite : new ArrayList<>(toCheck.getSentAllyInvites())) 
				if (invite.getReceiver().getName().equalsIgnoreCase(nation.getName())) {
					toCheck.deleteSentAllyInvite(invite);
					InviteHandler.removeInvite(invite);
				}
		// Search and remove any sent ally invites sent by this nation.
		for (Invite invite : new ArrayList<>(nation.getSentAllyInvites())) {
			nation.deleteSentAllyInvite(invite);
			InviteHandler.removeInvite(invite);
		}
		
		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive())
			nation.getAccount().removeAccount();

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
				res.updatePermsForNationRemoval();
				res.save();
			}
			try {
				town.setNation(null);
			} catch (AlreadyRegisteredException ignored) {
				// Cannot reach AlreadyRegisteredException
			}
			town.save();
			BukkitTools.fireEvent(new NationRemoveTownEvent(town, nation));
		}

		plugin.resetCache();

		BukkitTools.fireEvent(new DeleteNationEvent(nation, king));
	}

	@Override
	public void removeWorld(TownyWorld world) throws UnsupportedOperationException {

		deleteWorld(world);
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeJail(Jail jail) {
		// Unjail residents jailed here.
		new ArrayList<>(universe.getJailedResidentMap()).stream()
			.filter(resident -> resident.getJail().getUUID().equals(jail.getUUID()))
			.forEach(resident -> JailUtil.unJailResident(resident, UnJailReason.JAIL_DELETED));
		
		// Delete cells and spawnparticles.
		if (jail.hasCells())
			jail.removeAllCells();
		
		// Remove Town's record of the jail.
		if (jail.getTown() != null)
			jail.getTown().removeJail(jail);
		
		// Unregister the jail from the Universe.
		universe.unregisterJail(jail);
		
		deleteJail(jail);
	}

	@Override
	public void removePlotGroup(PlotGroup group) {
		universe.unregisterGroup(group.getUUID());
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

			if (universe.hasTown(filteredName))
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
				nation = town.getNationOrNull();
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

			town.saveTownBlocks();
			
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

		BukkitTools.fireEvent(new RenameTownEvent(oldName, town));
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
					if (toCheck.hasAlly(oldNation)) {
						toCheck.removeAlly(oldNation);
						toCheck.addAlly(nation);
					} else {
						toCheck.removeEnemy(oldNation);
						toCheck.addEnemy(nation);
					}
				} else
					toSave.remove(toCheck);

			for (Nation toCheck : toSaveNation)
				saveNation(toCheck);

		} finally {
			lock.unlock();
		}

		BukkitTools.fireEvent(new RenameNationEvent(oldName, nation));
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
			if (TownyEconomyHandler.isActive() && resident.getAccountOrNull() != null)
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
			List<Town> toSaveTown = new ArrayList<>(universe.getTowns());
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
		
		BukkitTools.fireEvent(new RenameResidentEvent(oldName, resident));
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

		TownyWorld world = universe.getWorld(worldName); 
		if (world == null)
			return null;
		
		return loadPlotData(new TownBlock(x, z, world));
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

			if (elements.fromString(test) != elements.VER)
				// This is too old to be used by modern Towny.
				// Return null so that we do not regenerate this plot, or, we queue up a new
				// plotsnapshot to be made.
				return null;

			version = fin.read();
			if (version < 4)
				// This is too old to be used by modern Towny.
				// Return null so that we do not regenerate this plot, or, we queue up a new
				// plotsnapshot to be made.
				return null;

			// set the version
			plotBlockData.setVersion(version);

			// next entry is the plot height
			plotBlockData.setHeight(fin.readInt());

			// Snapshots taken before 0.98.4.11 did not account for Mojang's lowered World
			// Height, and there will be no blocks stored below y=0.
			// Versions 5 and newer store the world's min-height as an int here.
			plotBlockData.setMinHeight(version == 4 ? 0 : fin.readInt());

			/*
			 * Load plot block data off of the remaining file.
			 */
			while ((value = fin.readUTF()) != null)
				blockArr.add(value);

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

	@Override
	public boolean hasPlotData(TownBlock townBlock) {
		return isFile(getPlotFilename(townBlock));
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
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "regen.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {
					split = line.split(",");
					WorldCoord wc = new WorldCoord(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]));
					TownyRegenAPI.addToRegenQueueList(wc, false);
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
			Collection<String> lines = TownyRegenAPI.getRegenQueueList().stream()
				.map(wc -> wc.getWorldName() + "," + wc.getX() + "," + wc.getZ())
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
		boolean isSameNation = mergeInto.hasNation() && mergeInto.getNationOrNull().hasTown(mergeFrom);
		String mayorName = mergeFrom.getMayor().getName();
		List<Jail> jails = universe.getJailUUIDMap().values().stream()
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
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_town_merge_success", mergeFrom.getName(), mayorName, mergeInto.getName()));
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

	/**
	 * Generates a town or nation replacementname.
	 * i.e.: Town1 or Nation2
	 * 
	 * @param town Boolean for whether it's a town or a nation we're creating a name for.
	 * @return replacementName String.
	 */
	public String generateReplacementName(boolean town) {
		Random r = new Random();
		String replacementName = "replacementname" + r.nextInt(99) + 1;
		try {
			replacementName = getNextName(town);
		} catch (TownyException e) {
			e.printStackTrace();
		}
		return replacementName;
	}
	
	
	private String getNextName(boolean town) throws TownyException  {
		String name = town ? "Town" : "Nation";
		
		int i = 0;
		do {
			String newName = name + ++i;
			if (town) {
				if (!universe.hasTown(newName))
					return newName;
		    } else { 
				if (!universe.hasNation(newName))
					return newName;
		    }
			if (i > 100000)
				throw new TownyException("Too many replacement names.");
		} while (true);
	}
}
