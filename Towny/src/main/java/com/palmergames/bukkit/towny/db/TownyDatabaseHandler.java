package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
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
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Identifiable;
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
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.WorldCoordEntityRemover;
import com.palmergames.bukkit.towny.regen.WorldCoordMaterialRemover;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;

import com.palmergames.util.JavaUtil;
import com.palmergames.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author ElgarL
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {
	public static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ssZ");
	final String rootFolderPath;
	final String dataFolderPath;
	final String settingsFolderPath;
	final String logFolderPath;
	final String backupFolderPath;
	protected final Queue<Runnable> queryQueue = new ConcurrentLinkedQueue<>();
	private final ScheduledTask task;
	protected List<Pair<String, String>> pendingDuplicateResidents = new ArrayList<>();
	
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
		 * Start our async queue for pushing data to the database.
		 */
		task = plugin.getScheduler().runAsyncRepeating(() -> {
			synchronized(queryQueue) {
				while (!this.queryQueue.isEmpty()) {
					Runnable operation = this.queryQueue.poll();
					operation.run();
				}
			}
		}, 5L, 5L);
	}
	
	@Override
	public void finishTasks() {
		
		// Cancel the repeating task as its not needed anymore.
		synchronized (this.queryQueue) {
			if (task != null)
				task.cancel();

			// Make sure that *all* tasks are saved before shutting down.
			while (!this.queryQueue.isEmpty()) {
				Runnable operation = this.queryQueue.poll();
				operation.run();
			}
		}
	}
	
	@Override
	public boolean backup() throws IOException {

		if (!TownySettings.getSaveDatabase().equalsIgnoreCase("flatfile") && !TownySettings.disableMySQLBackupWarning()) {
			plugin.getLogger().info("***** Warning *****");
			plugin.getLogger().info("***** Only Snapshots & Regen files in plugins/Towny/data/ will be backed up!");
			plugin.getLogger().info("***** This does not include your residents/towns/nations.");
			plugin.getLogger().info("***** Make sure you have scheduled a backup in MySQL too!!!");
			plugin.getLogger().info("***** If you already have backups or accept the risk, this message can be disabled in the database config.");
		}
		
		String backupType = TownySettings.getFlatFileBackupType();
		String newBackupFolder = backupFolderPath + File.separator + BACKUP_DATE_FORMAT.format(System.currentTimeMillis());
		FileMgmt.checkOrCreateFolders(rootFolderPath, rootFolderPath + File.separator + "backup");
        return switch (backupType.toLowerCase(Locale.ROOT)) {
            case "folder" -> {
                FileMgmt.checkOrCreateFolder(newBackupFolder);
                FileMgmt.copyDirectory(new File(dataFolderPath), new File(newBackupFolder));
                FileMgmt.copyDirectory(new File(logFolderPath), new File(newBackupFolder));
                FileMgmt.copyDirectory(new File(settingsFolderPath), new File(newBackupFolder));
                yield true;
            }
            case "zip" -> {
                FileMgmt.zipDirectories(new File(newBackupFolder + ".zip"), new File(dataFolderPath),
                        new File(logFolderPath), new File(settingsFolderPath));
                yield true;
            }
            case "tar.gz", "tar" -> {
                FileMgmt.tar(new File(newBackupFolder.concat(".tar.gz")),
                        new File(dataFolderPath),
                        new File(logFolderPath),
                        new File(settingsFolderPath));
                yield true;
            }
            default -> false;
        };
	}

	@Override
	public void postLoad() {
		deleteDuplicateResidents();
	}

	private void deleteDuplicateResidents() {
		for (final Pair<String, String> residentPair : this.pendingDuplicateResidents) {
			Resident firstRes = universe.getResident(residentPair.left());
			Resident secondRes = universe.getResident(residentPair.right());

			// Check if both uuids are actually equal
			if (firstRes == null || secondRes == null || firstRes.getUUID() == null || !firstRes.getUUID().equals(secondRes.getUUID())) {
				continue;
			}

			if (firstRes.getLastOnline() > secondRes.getLastOnline()) {
				// firstRes was online most recently, so delete secondRes
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_deleting_duplicate", secondRes.getName(), firstRes.getName()));
				try {
					universe.unregisterResident(secondRes);
				} catch (NotRegisteredException ignored) {}
				// Check if the older resident is a part of a town
				Town olderResTown = secondRes.getTownOrNull();
				if (olderResTown != null) {
					try {
						// Resident#removeTown saves the resident, so we can't use it.
						olderResTown.removeResident(secondRes);
					} catch (EmptyTownException e) {
						try {
							universe.unregisterTown(olderResTown);
						} catch (NotRegisteredException ignored) {}
						deleteTown(olderResTown);
					}
				}
				deleteResident(secondRes);
			} else {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_deleting_duplicate", firstRes.getName(), secondRes.getName()));
				try {
					universe.unregisterResident(firstRes);
				} catch (NotRegisteredException ignored) {}
				deleteResident(firstRes);
			}
		}

		this.pendingDuplicateResidents.clear();
	}

	/*
	 * Add new objects to the TownyUniverse maps.
	 */
	
	@Override
	public @NotNull Resident newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		return newResident(name, null);
	}

	@Override
	public @NotNull Resident newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException {
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
		return resident;
	}

	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		newNation(name, null);
	}

	@Override
	public void newNation(String name, @Nullable UUID uuid) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterNationNameOrThrow(name);
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
	 * Remove Object Methods
	 */
	
	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlaw & trusted lists.
		for (Town town : universe.getTowns()) {
			if (!town.exists())
				continue;

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

		for (TownBlock townBlock : new ArrayList<>(resident.getTownBlocks())) {
			townBlock.setResident(null, false);
			resident.removeTownBlock(townBlock);
			// Embassy plots are not put back up for sale, because the town would have no control over who buys them/griefs them.
			if (townBlock.getType() != TownBlockType.EMBASSY)
				townBlock.setPlotPrice(townBlock.getTownOrNull().getPlotPrice());

			// Set the plot permissions to mirror the towns.
			townBlock.setType(townBlock.getType());
			townBlock.save();
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
		
		if (resident.hasTown() && resident.getTownOrNull() != null)
			resident.removeTown();

		if (resident.hasUUID() && !resident.isNPC())
			saveHibernatedResident(resident.getUUID(), resident.getRegistered());

		// Delete the residents file.
		deleteResident(resident);
		// Remove the residents record from memory.
		try {
			universe.unregisterResident(resident);
		} catch (NotRegisteredException e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred while unregistering resident " + resident.getName(), e);
		}

		// Clear accounts
		if (TownySettings.isDeleteEcoAccount() && TownyEconomyHandler.isActive())
			resident.getAccount().removeAccount();

		plugin.deleteCache(resident);
		
		BukkitTools.fireEvent(new DeletePlayerEvent(resident));
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) throws TownyException {
		removeTownBlock(townBlock, TownPreUnclaimEvent.Cause.UNKNOWN);
	}

	@Override
	public void removeTownBlock(TownBlock townBlock, TownPreUnclaimEvent.Cause cause) throws TownyException {
		Town town = townBlock.getTownOrNull();
		if (town == null)
			// Log as error because TownBlocks *must* have a town.
			plugin.getLogger().severe(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", townBlock.getWorld().getName(), townBlock.getX(), townBlock.getZ()));

		if (!cause.ignoresPreEvent())
			BukkitTools.ifCancelledThenThrow(new TownPreUnclaimEvent(town, townBlock, cause));

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
		BukkitTools.fireEvent(new TownUnclaimEvent(town, townBlock.getWorldCoord(), false));
	}

	@Override
	public void removeTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks()))
			try {
				removeTownBlock(townBlock, TownPreUnclaimEvent.Cause.DELETE);
			} catch (TownyException ignored) {}
	}

	@Override
	public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender, boolean delayFullRemoval) {
		if (delayFullRemoval) {
			/*
			 * When Town ruining is active, send the Town into a ruined state, prior to real
			 * removal, if the TownPreRuinedEvent is not cancelled.
			 */
			TownPreRuinedEvent tpre = new TownPreRuinedEvent(town, cause, sender);
			if (!BukkitTools.isEventCancelled(tpre)) {
				TownRuinUtil.putTownIntoRuinedState(town);
				return false;
			} else if (sender != null && !tpre.getCancelMessage().isEmpty()) {
				TownyMessaging.sendErrorMsg(tpre.getCancelMessage());
			}
		}

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town, cause, sender);
		if (!cause.ignoresPreEvent() && BukkitTools.isEventCancelled(preEvent)) {
			if (sender != null && !preEvent.getCancelMessage().isEmpty())
				TownyMessaging.sendErrorMsg(sender, preEvent.getCancelMessage());
			
			return false;
		}
		
		Resident mayor = town.getMayor();
		TownyWorld townyWorld = town.getHomeblockWorld();
		int numTownBlocks = town.getNumTownBlocks();
		
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

		if (TownyEconomyHandler.isActive())
			town.getAccount().removeAccount();

		for (Resident resident : toSave) {
			ResidentModeHandler.resetModes(resident, false);
			resident.removeTown(true);
		}
		
		// Look for residents inside of this town's jail(s) and free them, more than 
		// likely the above removeTownBlocks(town) will have already set them free. 
		new ArrayList<>(universe.getJailedResidentMap()).stream()
			.filter(resident -> resident.hasJailTown(town.getName()))
			.forEach(resident -> JailUtil.unJailResident(resident, UnJailReason.JAIL_DELETED));

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
		
		BukkitTools.fireEvent(new DeleteTownEvent(town, mayor, numTownBlocks, cause, sender));
		
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_town2", town.getName()));
		return true;
	}

	@Override
	public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause, @Nullable CommandSender sender) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation, cause, sender);
		if (sender != null)
			preEvent.setCancelMessage(Translatable.of("msg_err_you_cannot_delete_this_nation").forLocale(sender));
		
		if (!cause.ignoresPreEvent() && BukkitTools.isEventCancelled(preEvent)) {
			if (sender != null && !preEvent.getCancelMessage().isEmpty())
				TownyMessaging.sendErrorMsg(preEvent.getCancelMessage());
			
			return false;
		}

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
			invite.getReceiver().deleteReceivedInvite(invite);

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
		} catch (NotRegisteredException ignored) {
			// Just ignore the exception. Very unlikely to happen.
		}

		for (Town town : toSave) {
			if (!town.exists())
				continue;

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

		BukkitTools.fireEvent(new DeleteNationEvent(nation, king, cause, sender));
		return true;
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

	@Override
	public void removeDistrict(District district) {
		universe.unregisterDistrict(district.getUUID());
		deleteDistrict(district);
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
				filteredName = NameValidation.checkAndFilterTownNameOrThrow(newName);
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
				if (TownyEconomyHandler.canRenameAccounts()) {
					TownyEconomyHandler.rename(town, TownySettings.getTownAccountPrefix() + filteredName);
				} else {
					try {
						townBalance = town.getAccount().getHoldingBalance();
						town.getAccount().withdraw(townBalance, "Rename Town - Transfer from old account");
					} catch (Exception ignored) {
						TownyMessaging.sendErrorMsg("The bank balance for the town " + oldName + " could not be received from the economy plugin and will not be able to be converted.");
					}
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
				if (!TownyEconomyHandler.canRenameAccounts())
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
		
	@Override
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();

		String oldName;

		try {

			String filteredName;

			try {
				filteredName = NameValidation.checkAndFilterNationNameOrThrow(newName);
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
				if (TownyEconomyHandler.canRenameAccounts()) {
					TownyEconomyHandler.rename(nation, TownySettings.getNationAccountPrefix() + filteredName);
				} else {
					try {
						nationBalance = nation.getAccount().getHoldingBalance();
						nation.getAccount().setBalance(0, "Rename Nation - Transfer from old account");
					} catch (Exception ignored) {
						TownyMessaging.sendErrorMsg("The bank balance for the nation " + nation.getName() + ", could not be received from the economy plugin and will not be able to be converted.");
					}
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
				if (!TownyEconomyHandler.canRenameAccounts())
					nation.getAccount().setBalance(nationBalance, "Rename Nation - Transfer to new account");
			}

			for (Town town : toSave) {
				saveTown(town);
			}

			saveNation(nation);

			//search and update all ally/enemy lists
			Nation oldNation = new Nation(oldName);
			List<Nation> toSaveNations = new ArrayList<>();
			universe.getNations().stream()
				.filter(n -> n.hasAlly(oldNation) || n.hasEnemy(oldNation))
				.forEach(n -> {
					if (n.hasAlly(oldNation)) {
						n.removeAlly(oldNation);
						n.addAlly(nation);
					} else {
						n.removeEnemy(oldNation);
						n.addEnemy(nation);
					}
					toSaveNations.add(n);
				});
			toSaveNations.forEach(Nation::save);

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
	public void renameDistrict(District district, String newName) throws AlreadyRegisteredException {
		// Create new one
		district.setName(newName);
		
		// Save
		saveDistrict(district);
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
			oldResident.setUUID(resident.getUUID());
			
			// Search and update all friends lists
			Set<Resident> residentsToSave = new HashSet<>();
			for (Resident toCheck : new ArrayList<>(universe.getResidents())){
				if (toCheck.hasFriend(oldResident)) {
					toCheck.removeFriend(oldResident);
					toCheck.addFriend(resident);
					residentsToSave.add(toCheck);
				}
			}
			residentsToSave.forEach(Resident::save);

			// Search and update all town outlaw, trustedresidents lists.
			Set<Town> townsToSave = new HashSet<>();
			for (Town toCheckTown : new ArrayList<>(universe.getTowns())) {
				if (toCheckTown.hasOutlaw(oldResident)) {
					toCheckTown.removeOutlaw(oldResident);
					toCheckTown.addOutlaw(resident);
					townsToSave.add(toCheckTown);
				}
				if (toCheckTown.hasTrustedResident(oldResident)) {
					toCheckTown.removeTrustedResident(oldResident);
					toCheckTown.addTrustedResident(resident);
					townsToSave.add(toCheckTown);
				}
			}
			townsToSave.forEach(Town::save);

			new ArrayList<>(universe.getTownBlocks().values()).stream()
				.filter(tb -> tb.hasTrustedResident(oldResident))
				.forEach(tb -> {
					tb.removeTrustedResident(oldResident);
					tb.addTrustedResident(resident);
				});

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
				plugin.getLogger().log(Level.WARNING, "An exception occurred while loading plot block data from file " + fileName, e);
				return null;
			}
			

        } else if (isFile(getLegacyPlotFilename(townBlock))) {
        	/*
        	 * Attempt to load legacy .data files.
        	 */
        	try {
    			return loadDataStream(plotBlockData, new FileInputStream(getLegacyPlotFilename(townBlock)));
    		} catch (FileNotFoundException e) {
				plugin.getLogger().log(Level.WARNING, "Could not find file for legacy plot block data file for townblock " + townBlock, e);
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
            plugin.getLogger().log(Level.WARNING, "An exception occurred while loading plot block data stream", e);
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
			plugin.getLogger().log(Level.WARNING, "Error Loading Regen List at " + line + ", in towny\\data\\regen.txt", e);
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

		int mergeFromBonus = mergeFrom.getBonusBlocks();
		int newTownBonus = TownySettings.getNewTownBonusBlocks();
		if (newTownBonus > 0 && mergeFromBonus >= newTownBonus)
			mergeFromBonus = mergeFromBonus - newTownBonus;
		mergeInto.addBonusBlocks(mergeFromBonus);

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
		removeTown(mergeFrom, DeleteTownEvent.Cause.MERGED, null, false);

		mergeInto.save();
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_town_merge_success", mergeFrom.getName(), mayorName, mergeInto.getName()));
	}
	
	protected List<UUID> toUUIDList(Collection<? extends Identifiable> objects) {
		final List<UUID> list = new ArrayList<>();

		for (final Identifiable object : objects) {
			final UUID uuid = object.getUUID();

			if (uuid != null) {
				list.add(uuid);
			}
		}

		return list;
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
		} catch (TownyException ignored) {
			// fallback to replacement name
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

	/**
	 * Attempts to parse the given UUID string into a UUID, or generates a new one if it was invalid.
	 * <p>
	 * Intended for Towny objects such as towns/nations, not players. Use {@link #parsePlayerUUID(String, String)} for those.
	 *
	 * @param uuidString The uuid string as retrieved from the database, or {@code null}.
	 * @param describedAs The type and name of the Towny object, such as {@code "town '" + townName + "'"}
	 * @return The parsed uuid, or a brand new uuid.
	 */
	protected UUID parseUUIDOrNew(@Nullable String uuidString, String describedAs) {
		if (uuidString != null) {
			try {
				return UUID.fromString(uuidString);
			} catch (IllegalArgumentException ignored) {}
		}

		plugin.getLogger().warning(describedAs + " did not have a uuid or had an invalid one (got '" + uuidString + "'), generating a new one.");
		return UUID.randomUUID();
	}

	/**
	 * Attempts to parse the given player UUID string into a UUID.
	 *
	 * @param playerUUID The player's uuid as retrieved from the database, or {@code null}.
	 * @param playerName The player's name.
	 * @return The player's uuid, or {@code null} if it was unable to be parsed/found.
	 */
	protected @Nullable UUID parsePlayerUUID(@Nullable String playerUUID, String playerName) {
		if (playerUUID != null) {
			try {
				return UUID.fromString(playerUUID);
			} catch (IllegalArgumentException ignored) {}
		}

		if (playerName.startsWith(TownySettings.getNPCPrefix())) {
			// Create a random uuid and set the version byte to 2 for NPCs
			return JavaUtil.changeUUIDVersion(UUID.randomUUID(), 2);
		}

		if (!Bukkit.getServer().getOnlineMode()) {
			return BukkitTools.getOfflinePlayerUUID(playerName);
		}

		final OfflinePlayer cached = BukkitTools.getOfflinePlayerIfCached(playerName);
		if (cached != null) {
			return cached.getUniqueId();
		}

		plugin.getLogger().warning("Could not find a previous UUID for player '" + playerName + "', looking it up using the Mojang API...");
		return plugin.getServer().getPlayerUniqueId(playerName);
	}
}
