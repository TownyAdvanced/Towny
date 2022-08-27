package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource.TownyDBFileType;
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
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PermissionData;
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
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author ElgarL, LlmDL
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
	
	public String getDataFolderPath() {
		return this.dataFolderPath;
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
	public void newWorld(World world) {
		if (universe.getWorldIDMap().containsKey(world.getUID()))
			return;
		TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
		universe.registerTownyWorld(townyWorld);
		townyWorld.save();
	}

	/*
	 * Load a single object, not used by Towny itself.
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
	 * New Load Object Methods
	 * 
	 * These are called from the FlatFileSource and SQLSource which present Towny
	 * with an object, UUID and the keys which are used to load an object.
	 */
	
	public boolean loadJail(Jail jail, HashMap<String, String> keys) {
		String line = "";
		line = keys.get("townblock");
		if (line != null) {
			try {
				TownBlock tb = parseTownBlockFromDB(line);
				jail.setTownBlock(tb);
				jail.setTown(tb.getTownOrNull());
				tb.setJail(jail);
				tb.getTown().addJail(jail);
			} catch (NumberFormatException | NotRegisteredException e) {
				TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " tried to load invalid townblock " + line + " deleting jail.");
				removeJail(jail);
				deleteJail(jail);
				return true;
			}
		}
		line = keys.get("spawns");
		if (line != null) {
			String[] jails = line.split(";");
			for (String spawn : jails) {
				Location loc = SpawnUtil.parseSpawnLocationFromDB(spawn);
				if (loc != null)
					jail.addJailCell(loc);
			}
			if (jail.getJailCellLocations().isEmpty()) {
				TownyMessaging.sendErrorMsg("Jail " + jail.getUUID() + " loaded with zero spawns " + line + " deleting jail.");
				removeJail(jail);
				deleteJail(jail);
				return true;
			}
		}
		return true;
	}
	
	public boolean loadPlotGroup(PlotGroup group, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("groupName");
			if (line != null)
				group.setName(line.trim());
			
			line = keys.get("town");
			if (line != null && !line.isEmpty()) {
				Town town = universe.getTown(line.trim());
				if (town != null) {
					group.setTown(town);
				} else {
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_group_file_missing_town_delete", group.getSaveLocation()));
					deletePlotGroup(group); 
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_missing_file_delete_group_entry", group.getSaveLocation()));
					return true;
				}
			} else {
				TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_could_not_add_to_town"));
				deletePlotGroup(group);
			}
			
			line = keys.get("groupPrice");
			if (line != null && !line.isEmpty())
				group.setPrice(Double.parseDouble(line.trim()));

			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_group_file_at_line", group.getSaveLocation(), line));
			return false;
		}
	}
	
	public boolean loadResident(Resident resident, HashMap<String, String> keys) {
		try {
			String line = "";
			// Name
			resident.setName(keys.getOrDefault("name", generateMissingName()));
			// Registered Date
			resident.setRegistered(getOrDefault(keys, "registered", 0l));
			// Last Online Date
			resident.setLastOnline(getOrDefault(keys, "lastOnline", 0l));
			// isNPC
			resident.setNPC(getOrDefault(keys, "isNPC", false));
			// jail
			line = keys.get("jail");
			if (line != null && universe.hasJail(UUID.fromString(line)))
				resident.setJail(universe.getJail(UUID.fromString(line)));
			if (resident.isJailed()) {
				line = keys.get("jailCell");
				if (line != null)
					resident.setJailCell(Integer.parseInt(line));
				
				line = keys.get("jailHours");
				if (line != null)
					resident.setJailHours(Integer.parseInt(line));
			}
			line = keys.get("friends");
			if (line != null) {
				List<Resident> residentFriends = new ArrayList<>();
				try {
					residentFriends = TownyAPI.getInstance().getResidents(toUUIDArray(line.split("#")));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					residentFriends = TownyAPI.getInstance().getResidents(line.split(","));
				}
				resident.loadFriends(residentFriends);
			}

			resident.setPermissions(keys.getOrDefault("protectionStatus", ""));
	
			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(resident, line.trim());
	
			line = keys.get("town");
			if (line != null) {
				Town town = null;
				try {
					town = universe.getTown(UUID.fromString(line));
				} catch (IllegalArgumentException e1) { // Legacy DB used Names instead of UUIDs.
					town = universe.getTown(line);
				}
	//			} else if (universe.getReplacementNameMap().containsKey(line)) {
	//				town = universe.getTown(universe.getReplacementNameMap().get(line));
				if (town == null)
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_resident_tried_load_invalid_town", resident.getName(), line));

				if (town != null) {
					resident.setTown(town, false);
					
					line = keys.get("title");
					if (line != null)
						resident.setTitle(line);
					
					line = keys.get("surname");
					if (line != null)
						resident.setSurname(line);
					
					try {
						line = keys.get("town-ranks");
						if (line != null)
							resident.setTownRanks(Arrays.asList((line.split("#"))));
					} catch (Exception e) {}
	
					try {
						line = keys.get("nation-ranks");
						if (line != null)
							resident.setNationRanks(Arrays.asList((line.split("#"))));
					} catch (Exception e) {}
	
					line = keys.get("joinedTownAt");
					if (line != null) {
						resident.setJoinedTownAt(Long.valueOf(line));
					}
				}
			}
			
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(e.getMessage());
			return false;
		} finally {
			saveResident(resident);
		}
	}
	
	public boolean loadTown(Town town, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("mayor");
			if (line != null)
				try {
					Resident res = null;
					try {
						res = universe.getResident(UUID.fromString(line));
					} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
						res = universe.getResident(line);
					}
					if (res == null)
						throw new TownyException();
					
					town.forceSetMayor(res);
				} catch (TownyException e1) {
					if (town.getResidents().isEmpty())
						deleteTown(town);
					else 
						town.findNewMayor();

					return true;
				}

			town.setName(keys.getOrDefault("name", generateMissingName()));
			town.setRegistered(getOrDefault(keys, "registered", 0l));
			town.setRuined(getOrDefault(keys, "ruined", false));
			town.setRuinedTime(getOrDefault(keys, "ruinedTime", 0l));
			town.setNeutral(getOrDefault(keys, "neutral", TownySettings.getTownDefaultNeutral()));
			town.setOpen(getOrDefault(keys, "open", TownySettings.getTownDefaultOpen()));
			town.setPublic(getOrDefault(keys, "public", TownySettings.getTownDefaultPublic()));
			town.setConquered(getOrDefault(keys, "conquered", false));
			town.setConqueredDays(getOrDefault(keys, "conqueredDays", 0));
			town.setDebtBalance(getOrDefault(keys, "debtBalance", 0.0));
			town.setNationZoneOverride(getOrDefault(keys, "nationZoneOverride", 0));
			town.setNationZoneEnabled(getOrDefault(keys, "nationZoneEnabled", false));
			town.setBoard(keys.getOrDefault("townBoard", TownySettings.getTownDefaultBoard()));
			town.setTag(keys.getOrDefault("tag", ""));
			town.setBonusBlocks(getOrDefault(keys, "bonusBlocks", 0));
			town.setPurchasedBlocks(getOrDefault(keys, "purchasedBlocks", 0));
			town.setHasUpkeep(getOrDefault(keys, "hasUpkeep", true));
			town.setHasUnlimitedClaims(getOrDefault(keys, "hasUnlimitedClaims", false));
			town.setTaxes(getOrDefault(keys, "taxes", TownySettings.getTownDefaultTax()));
			town.setTaxPercentage(getOrDefault(keys, "taxpercent", TownySettings.getTownDefaultTaxPercentage()));
			town.setPlotPrice(getOrDefault(keys, "plotPrice", 0.0));
			town.setPlotTax(getOrDefault(keys, "plotTax", TownySettings.getTownDefaultPlotTax()));
			town.setCommercialPlotTax(getOrDefault(keys, "commercialPlotTax", TownySettings.getTownDefaultShopTax()));
			town.setCommercialPlotPrice(getOrDefault(keys, "commercialPlotPrice", 0.0));
			town.setEmbassyPlotTax(getOrDefault(keys, "embassyPlotTax", TownySettings.getTownDefaultEmbassyTax()));
			town.setEmbassyPlotPrice(getOrDefault(keys, "embassyPlotPrice", 0.0));
			town.setMaxPercentTaxAmount(getOrDefault(keys, "maxPercentTaxAmount", TownySettings.getMaxTownTaxPercentAmount()));
			town.setSpawnCost(getOrDefault(keys, "spawnCost", TownySettings.getSpawnTravelCost()));
			town.setMapColorHexCode(keys.getOrDefault("mapColorHexCode", MapUtil.generateRandomTownColourAsHexCode()));
			town.setAdminDisabledPVP(getOrDefault(keys, "adminDisabledPvP", false));
			town.setAdminEnabledPVP(getOrDefault(keys, "adminEnabledPvP", false));
			town.setManualTownLevel(getOrDefault(keys, "manualTownLevel", -1));
			town.setPermissions(keys.getOrDefault("protectionStatus", ""));
			town.setJoinedNationAt(getOrDefault(keys, "joinedNationAt", 0l));
			town.setMovedHomeBlockAt(getOrDefault(keys, "movedHomeBlockAt", 0l));
			line = keys.get("homeBlock");
			if (line != null) {
				try {
					town.setHomeBlock(parseTownBlockFromDB(line));
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_location", town.getName()));
				} catch (NotRegisteredException e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_homeblock_load_invalid_townblock", town.getName()));
				}
			}

			line = keys.get("spawn");
			if (line != null) {
				Location loc = SpawnUtil.parseSpawnLocationFromDB(line);
				if (loc != null)
					town.setSpawn(loc);
			}
			
			// Load outpost spawns
			line = keys.get("outpostspawns");
			if (line != null) {
				String[] outposts = line.split(";");
				for (String spawn : outposts) {
					Location loc = SpawnUtil.parseSpawnLocationFromDB(spawn);
					if (loc != null)
						town.forceAddOutpostSpawn(loc);
				}
			}

			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(town, line.trim());
			
			line = keys.get("nation");
			if (line != null && !line.isEmpty()) {
				Nation nation = null;
				try {
					nation = universe.getNation(UUID.fromString(line));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					nation = universe.getNation(line);
				}
//				else if (universe.getReplacementNameMap().containsKey(line))
//					nation = universe.getNation(universe.getReplacementNameMap().get(line));

				// Only set the nation if it exists
				if (nation != null)
					town.setNation(nation, false);
			}

			line = keys.get("primaryJail");
			if (line != null) {
				UUID jailUUID = UUID.fromString(line);
				if (universe.hasJail(jailUUID))
					town.setPrimaryJail(universe.getJail(jailUUID));
			}
			
			line = keys.get("trustedResidents");
			if (line != null && !line.isEmpty())
				TownyAPI.getInstance().getResidents(toUUIDArray(line.split("#"))).stream().forEach(res -> town.addTrustedResident(res));

			line = keys.get("allies");
			if (line != null && !line.isEmpty()) {
				String search = line.contains("#") ? "#" : ","; // Legacy DB used , instead of #.
				town.loadAllies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(search))));
			}
			
			line = keys.get("enemies");
			if (line != null && !line.isEmpty()) {
				String search = line.contains("#") ? "#" : ","; // Legacy DB used , instead of #.
				town.loadEnemies(TownyAPI.getInstance().getTowns(toUUIDArray(line.split(search))));
			}
			
			line = keys.get("outlaws");
			if (line != null && !line.isEmpty()) {
				List<Resident> outlawResidents = new ArrayList<>();
				try {
					outlawResidents = TownyAPI.getInstance().getResidents(toUUIDArray(line.split("#")));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					outlawResidents = TownyAPI.getInstance().getResidents(line.split(","));
				}
				outlawResidents.stream().forEach(res -> {
					try {
						town.addOutlaw(res);
					} catch (AlreadyRegisteredException ignored) {}
				});
			}
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_reading_town_file_at_line", town.getName(), line, town.getUUID().toString()));
			e.printStackTrace();
			return false;
		} finally {
			saveTown(town);
		}
		return true;
	}
	
	public boolean loadNation(Nation nation, HashMap<String, String> keys) {
		String line = "";
		try {
			line = keys.get("capital");
			String cantLoadCapital = Translation.of("flatfile_err_nation_could_not_load_capital_disband", nation.getName());
			if (line != null) {
				Town town = universe.getTown(UUID.fromString(line));
				if (town != null) {
					try {
						nation.forceSetCapital(town);
					} catch (EmptyNationException e1) {
						plugin.getLogger().warning(cantLoadCapital);
						removeNation(nation);
						return true;
					}
				}
				else {
					TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_cannot_set_capital_try_next", nation.getName(), line));
					if (!nation.findNewCapital()) {
						plugin.getLogger().warning(cantLoadCapital);
						removeNation(nation);
						return true;
					}
				}
			} else {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_undefined_capital_select_new", nation.getName()));
				if (!nation.findNewCapital()) {
					plugin.getLogger().warning(cantLoadCapital);
					removeNation(nation);
					return true;
				}
			}

			nation.setName(keys.getOrDefault("name", generateMissingName()));
			nation.setTaxes(getOrDefault(keys, "taxes", 0.0));
			nation.setSpawnCost(getOrDefault(keys, "spawnCost", TownySettings.getSpawnTravelCost()));
			nation.setNeutral(getOrDefault(keys, "neutral", false));
			nation.setRegistered(getOrDefault(keys, "registered", 0l));
			nation.setPublic(getOrDefault(keys, "isPublic", false));
			nation.setOpen(getOrDefault(keys, "isOpen", TownySettings.getNationDefaultOpen()));
			nation.setBoard(keys.getOrDefault("nationBoard", TownySettings.getNationDefaultBoard()));
			nation.setMapColorHexCode(keys.getOrDefault("mapColorHexCode", MapUtil.generateRandomNationColourAsHexCode()));
			nation.setTag(keys.getOrDefault("tag", ""));

			
			line = keys.get("allies");
			if (line != null && !line.isEmpty()) {
				List<Nation> allyNations = new ArrayList<>();
				try {
					allyNations = TownyAPI.getInstance().getNations(toUUIDArray(line.split("#")));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					allyNations = TownyAPI.getInstance().getNations(line.split(","));
				}
				nation.loadAllies(allyNations);
			}
			
			line = keys.get("enemies");
			if (line != null && !line.isEmpty()) {
				List<Nation> enemyNations = new ArrayList<>();
				try {
					enemyNations = TownyAPI.getInstance().getNations(toUUIDArray(line.split("#"))); 
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					enemyNations = TownyAPI.getInstance().getNations(line.split(","));
				}
				nation.loadEnemies(enemyNations);
			}

			line = keys.get("nationSpawn");
			if (line != null) {
				Location loc = SpawnUtil.parseSpawnLocationFromDB(line);
				if (loc != null)
					nation.setSpawn(loc);
			}

			
			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(nation, line.trim());

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_reading_nation_file_at_line", nation.getName(), line, nation.getUUID().toString()));
			e.printStackTrace();
			return false;
		} finally {
			saveNation(nation);
		}
		return true;
	}
	
	public boolean loadWorld(TownyWorld world, HashMap<String, String> keys) {
		String line = "";
		try {
			world.setName(keys.getOrDefault("name", generateMissingName()));
			world.setClaimable(getOrDefault(keys,"claimable", true));
			world.setUsingTowny(getOrDefault(keys, "usingTowny", TownySettings.isUsingTowny()));
			world.setWarAllowed(getOrDefault(keys, "warAllowed", TownySettings.isWarAllowed()));
			world.setPVP(getOrDefault(keys, "pvp", TownySettings.isPvP()));
			world.setForcePVP(getOrDefault(keys, "forcepvp", TownySettings.isForcingPvP()));
			world.setFriendlyFire(getOrDefault(keys, "friendlyFire", TownySettings.isFriendlyFireEnabled()));
			world.setForceTownMobs(getOrDefault(keys, "forcetownmobs", TownySettings.isForcingMonsters()));
			world.setWildernessMobs(getOrDefault(keys, "wildernessmobs", TownySettings.isWildernessMonstersOn()));
			world.setWorldMobs(getOrDefault(keys, "worldmobs", TownySettings.isWorldMonstersOn()));
			world.setFire(getOrDefault(keys, "firespread", TownySettings.isFire()));
			world.setForceFire(getOrDefault(keys, "forcefirespread", TownySettings.isForcingFire()));
			world.setExpl(getOrDefault(keys, "explosions", TownySettings.isExplosions()));
			world.setForceExpl(getOrDefault(keys, "forceexplosions", TownySettings.isForcingExplosions()));
			world.setEndermanProtect(getOrDefault(keys, "endermanprotect", TownySettings.getEndermanProtect()));
			world.setDisableCreatureTrample(getOrDefault(keys, "disablecreaturetrample", TownySettings.isCreatureTramplingCropsDisabled()));
			world.setUnclaimedZoneBuild(getOrDefault(keys, "unclaimedZoneBuild", TownySettings.getUnclaimedZoneBuildRights()));
			world.setUnclaimedZoneDestroy(getOrDefault(keys, "unclaimedZoneDestroy", TownySettings.getUnclaimedZoneDestroyRights()));
			world.setUnclaimedZoneSwitch(getOrDefault(keys, "unclaimedZoneSwitch", TownySettings.getUnclaimedZoneSwitchRights()));
			world.setUnclaimedZoneItemUse(getOrDefault(keys, "unclaimedZoneItemUse", TownySettings.getUnclaimedZoneItemUseRights()));
			world.setUnclaimedZoneName(keys.getOrDefault("unclaimedZoneName", TownySettings.getUnclaimedZoneName()));
			world.setUnclaimedZoneIgnore(toList(keys.get("unclaimedZoneIgnoreIds")));
			world.setPlotManagementDeleteIds(toList(keys.get("plotManagementDeleteIds")));
			world.setUsingPlotManagementDelete(getOrDefault(keys, "usingPlotManagementDelete", TownySettings.isUsingPlotManagementDelete()));
			world.setDeletingEntitiesOnUnclaim(getOrDefault(keys, "isDeletingEntitiesOnUnclaim", TownySettings.isDeletingEntitiesOnUnclaim()));
			world.setUnclaimDeleteEntityTypes(toList(keys.get("unclaimDeleteEntityTypes")));
			world.setPlotManagementMayorDelete(toList(keys.get("plotManagementMayorDelete")));
			world.setUsingPlotManagementMayorDelete(getOrDefault(keys, "usingPlotManagementMayorDelete", TownySettings.isUsingPlotManagementMayorDelete()));
			world.setPlotManagementIgnoreIds(toList(keys.get("plotManagementIgnoreIds")));
			world.setUsingPlotManagementRevert(getOrDefault(keys, "usingPlotManagementRevert", TownySettings.isUsingPlotManagementRevert()));
			world.setPlotManagementWildRevertEntities(toList(keys.get("PlotManagementWildRegenEntities")));
			world.setUsingPlotManagementWildEntityRevert(getOrDefault(keys, "usingPlotManagementWildRegen", TownySettings.isUsingPlotManagementWildEntityRegen()));
			world.setPlotManagementWildRevertBlockWhitelist(toList(keys.get("PlotManagementWildRegenBlockWhitelist")));
			world.setPlotManagementWildRevertMaterials(toList(keys.get("PlotManagementWildRegenBlocks")));
			world.setUsingPlotManagementWildBlockRevert(getOrDefault(keys, "usingPlotManagementWildRegenBlocks", TownySettings.isUsingPlotManagementWildBlockRegen()));
			world.setPlotManagementWildRevertDelay(getOrDefault(keys, "usingPlotManagementWildRegenDelay", TownySettings.getPlotManagementWildRegenDelay()));
			line = keys.get("metadata");
			if (line != null && !line.isEmpty())
				MetadataLoader.getInstance().deserializeMetadata(world, line.trim());
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_world_file_at_line", world.getName(), line, world.getUUID().toString()));
			return false;
		} finally {
			saveWorld(world);
		}
		return true;
	}
	
	/*
	 * HashMap methods used to save objects in the TownyFlatFileSource and TownySQLSource
	 */

	public HashMap<String, Object> getJailHashMapForSaving(Jail jail) throws Exception {
		try {
			HashMap<String, Object> jail_hm = new HashMap<>();
			jail_hm.put("uuid", jail.getUUID());
			jail_hm.put("townBlock", getTownBlockForSaving(jail.getTownBlock()));
			
			StringBuilder jailCellArray = new StringBuilder();
			if (jail.hasCells())
				for (Location cell : new ArrayList<>(jail.getJailCellLocations()))
					jailCellArray.append(parseLocationForSaving(cell)).append(";");

			jail_hm.put("spawns", jailCellArray);
			
			return jail_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Jail HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getPlotGroupHashMapForSaving(PlotGroup group) throws Exception {
		try {
			HashMap<String, Object> pltgrp_hm = new HashMap<>();
			pltgrp_hm.put("groupID", group.getUUID());
			pltgrp_hm.put("groupName", group.getName());
			pltgrp_hm.put("groupPrice", group.getPrice());
			pltgrp_hm.put("town", group.getTown().toString());

			return pltgrp_hm;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: PlotGroup HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getResidentHashMapForSaving(Resident resident) throws Exception {
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("name", resident.getName());
			res_hm.put("uuid", resident.hasUUID() ? resident.getUUID().toString() : "");
			res_hm.put("lastOnline", resident.getLastOnline());
			res_hm.put("registered", resident.getRegistered());
			res_hm.put("joinedTownAt", resident.getJoinedTownAt());
			res_hm.put("isNPC", resident.isNPC());
			res_hm.put("jailUUID", resident.isJailed() ? resident.getJail().getUUID() : "");
			res_hm.put("jailCell", resident.getJailCell());
			res_hm.put("jailHours", resident.getJailHours());
			res_hm.put("title", resident.getTitle());
			res_hm.put("surname", resident.getSurname());
			res_hm.put("town", resident.hasTown() ? resident.getTown().getUUID() : "");
			res_hm.put("town-ranks", resident.hasTown() ? StringMgmt.join(resident.getTownRanks(), "#") : "");
			res_hm.put("nation-ranks", resident.hasTown() ? StringMgmt.join(resident.getNationRanks(), "#") : "");
			res_hm.put("friends", StringMgmt.join(resident.getFriendsUUIDs(), "#"));
			res_hm.put("protectionStatus", resident.getPermissions().toString().replaceAll(",", "#"));
			res_hm.put("metadata", resident.hasMeta() ? serializeMetadata(resident) : "");
			return res_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Town HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getHibernatedResidentHashMapForSaving(UUID uuid, long registered) throws Exception {
		try {
			HashMap<String, Object> res_hm = new HashMap<>();
			res_hm.put("uuid", uuid);
			res_hm.put("registered", registered);
			return res_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: HibernatedResident HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getTownBlockHashMapForSaving(TownBlock townBlock) throws Exception {
		try {
			HashMap<String, Object> tb_hm = new HashMap<>();
			tb_hm.put("world", townBlock.getWorld().getUUID());
			tb_hm.put("x", townBlock.getX());
			tb_hm.put("z", townBlock.getZ());
			tb_hm.put("name", townBlock.getName());
			tb_hm.put("price", townBlock.getPlotPrice());
			tb_hm.put("town", townBlock.getTown().getUUID());
			tb_hm.put("resident", (townBlock.hasResident()) ? townBlock.getResidentOrNull().getUUID() : "");
			tb_hm.put("typeName", townBlock.getTypeName());
			tb_hm.put("outpost", townBlock.isOutpost());
			tb_hm.put("permissions",
					townBlock.isChanged() ? townBlock.getPermissions().toString().replaceAll(",", "#") : "");
			tb_hm.put("locked", townBlock.isLocked());
			tb_hm.put("changed", townBlock.isChanged());
			tb_hm.put("claimedAt", townBlock.getClaimedAt());
			tb_hm.put("groupID", townBlock.hasPlotObjectGroup() ? townBlock.getPlotObjectGroup().getUUID().toString() : "");
			tb_hm.put("metadata", townBlock.hasMeta() ? serializeMetadata(townBlock) : "");
			tb_hm.put("trustedResidents", StringMgmt.join(toUUIDList(townBlock.getTrustedResidents()), "#"));

			Map<String, String> stringMap = new HashMap<>();
			for (Map.Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet())
				stringMap.put(entry.getKey().getUUID().toString(), entry.getValue().toString());
			tb_hm.put("customPermissionData", new Gson().toJson(stringMap));

			return tb_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: TownBlock HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getTownHashMapForSaving(Town town) throws Exception {
		try {
			HashMap<String, Object> twn_hm = new HashMap<>();
			twn_hm.put("name", town.getName());
			twn_hm.put("uuid", town.hasValidUUID() ? town.getUUID() : UUID.randomUUID()); //TODO: Do we really want this?
			twn_hm.put("outlaws", StringMgmt.join(town.getOutlaws(), "#"));
			twn_hm.put("mayor", town.hasMayor() ? town.getMayor().getUUID() : "");
			twn_hm.put("nation", town.hasNation() ? town.getNation().getUUID() : "");
			twn_hm.put("assistants", StringMgmt.join(town.getRank("assistant"), "#"));
			twn_hm.put("townBoard", town.getBoard());
			twn_hm.put("tag", town.getTag());
			twn_hm.put("protectionStatus", town.getPermissions().toString().replaceAll(",", "#"));
			twn_hm.put("bonus", town.getBonusBlocks());
			twn_hm.put("manualTownLevel", town.getManualTownLevel());
			twn_hm.put("purchased", town.getPurchasedBlocks());
			twn_hm.put("nationZoneOverride", town.getNationZoneOverride());
			twn_hm.put("nationZoneEnabled", town.isNationZoneEnabled());
			twn_hm.put("commercialPlotPrice", town.getCommercialPlotPrice());
			twn_hm.put("commercialPlotTax", town.getCommercialPlotTax());
			twn_hm.put("embassyPlotPrice", town.getEmbassyPlotPrice());
			twn_hm.put("embassyPlotTax", town.getEmbassyPlotTax());
			twn_hm.put("spawnCost", town.getSpawnCost());
			twn_hm.put("plotPrice", town.getPlotPrice());
			twn_hm.put("plotTax", town.getPlotTax());
			twn_hm.put("taxes", town.getTaxes());
			twn_hm.put("hasUpkeep", town.hasUpkeep());
			twn_hm.put("hasUnlimitedClaims", town.hasUnlimitedClaims());
			twn_hm.put("taxpercent", town.isTaxPercentage());
			twn_hm.put("maxPercentTaxAmount", town.getMaxPercentTaxAmount());
			twn_hm.put("open", town.isOpen());
			twn_hm.put("public", town.isPublic());
			twn_hm.put("conquered", town.isConquered());
			twn_hm.put("conqueredDays", town.getConqueredDays());
			twn_hm.put("admindisabledpvp", town.isAdminDisabledPVP());
			twn_hm.put("adminenabledpvp", town.isAdminEnabledPVP());
			twn_hm.put("joinedNationAt", town.getJoinedNationAt());
			twn_hm.put("mapColorHexCode", town.getMapColorHexCode());
			twn_hm.put("movedHomeBlockAt", town.getMovedHomeBlockAt());
			twn_hm.put("metadata", town.hasMeta() ? serializeMetadata(town) : "");
			twn_hm.put("homeblock", town.hasHomeBlock() ? getTownBlockForSaving(town.getHomeBlock()) : "");
			twn_hm.put("spawn", town.hasSpawn() ? parseLocationForSaving(town.getSpawn()) : "");
			StringBuilder outpostArray = new StringBuilder();
			if (town.hasOutpostSpawn())
				for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns()))
					outpostArray.append(parseLocationForSaving(spawn)).append(";");
			twn_hm.put("outpostSpawns", outpostArray.toString());
			twn_hm.put("registered", town.getRegistered());
			twn_hm.put("ruined", town.isRuined());
			twn_hm.put("ruinedTime", town.getRuinedTime());
			twn_hm.put("neutral", town.isNeutral());
			twn_hm.put("debtBalance", town.getDebtBalance());
			if (town.getPrimaryJail() != null)
				twn_hm.put("primaryJail", town.getPrimaryJail().getUUID());
			twn_hm.put("trustedResidents", StringMgmt.join(toUUIDList(town.getTrustedResidents()), "#"));
			twn_hm.put("allies", StringMgmt.join(town.getAlliesUUIDs(), "#"));
			twn_hm.put("enemies", StringMgmt.join(town.getEnemiesUUIDs(), "#"));
			return twn_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Town HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getNationHashMapForSaving(Nation nation) throws Exception {
		try {
			HashMap<String, Object> nat_hm = new HashMap<>();
			nat_hm.put("name", nation.getName());
			nat_hm.put("uuid", nation.hasValidUUID() ? nation.getUUID() : UUID.randomUUID()); //TODO: Do we really want this?
			nat_hm.put("capital", nation.hasCapital() ? nation.getCapital().getUUID() : "");
			nat_hm.put("nationBoard", nation.getBoard());
			nat_hm.put("mapColorHexCode", nation.getMapColorHexCode());
			nat_hm.put("tag", nation.hasTag() ? nation.getTag() : "");
			nat_hm.put("allies", StringMgmt.join(nation.getAlliesUUIDs(), "#"));
			nat_hm.put("enemies", StringMgmt.join(nation.getEnemiesUUIDs(), "#"));
			nat_hm.put("taxes", nation.getTaxes());
			nat_hm.put("spawnCost", nation.getSpawnCost());
			nat_hm.put("neutral", nation.isNeutral());
			nat_hm.put("nationSpawn", nation.hasSpawn() ? parseLocationForSaving(nation.getSpawn()) : "");
			nat_hm.put("registered", nation.getRegistered());
			nat_hm.put("isPublic", nation.isPublic());
			nat_hm.put("isOpen", nation.isOpen());
			nat_hm.put("metadata", nation.hasMeta() ? serializeMetadata(nation) : "");
			return nat_hm;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: Nation HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public HashMap<String, Object> getWorldHashMapForSaving(TownyWorld world) throws Exception {
		try {
			HashMap<String, Object> world_hm = new HashMap<>();

			world_hm.put("name", world.getName());
			world_hm.put("usingTowny", world.isUsingTowny());
			world_hm.put("warAllowed", world.isWarAllowed());
			world_hm.put("pvp", world.isPVP());
			world_hm.put("forcepvp", world.isForcePVP());
			world_hm.put("friendlyFire", world.isFriendlyFireEnabled());
			world_hm.put("claimable", world.isClaimable());
			world_hm.put("worldmobs", world.hasWorldMobs());
			world_hm.put("wildernessmobs", world.hasWildernessMobs());
			world_hm.put("forcetownmobs", world.isForceTownMobs());
			world_hm.put("firespread", world.isFire());
			world_hm.put("forcefirespread", world.isForceFire());
			world_hm.put("explosions", world.isExpl());
			world_hm.put("forceexplosions", world.isForceExpl());
			world_hm.put("endermanprotect", world.isEndermanProtect());
			world_hm.put("disablecreaturetrample", world.isDisableCreatureTrample());

			world_hm.put("unclaimedZoneBuild", world.getUnclaimedZoneBuild());
			world_hm.put("unclaimedZoneDestroy", world.getUnclaimedZoneDestroy());
			world_hm.put("unclaimedZoneSwitch", world.getUnclaimedZoneSwitch());
			world_hm.put("unclaimedZoneItemUse", world.getUnclaimedZoneItemUse());
			if (world.getUnclaimedZoneName() != null)
				world_hm.put("unclaimedZoneName", world.getUnclaimedZoneName());

			// Unclaimed Zone Ignore Ids
			if (world.getUnclaimedZoneIgnoreMaterials() != null)
				world_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), "#"));

			// Deleting EntityTypes from Townblocks on Unclaim.
			world_hm.put("isDeletingEntitiesOnUnclaim", world.isDeletingEntitiesOnUnclaim());
			if (world.getUnclaimDeleteEntityTypes() != null)
				world_hm.put("unclaimDeleteEntityTypes", StringMgmt.join(world.getUnclaimDeleteEntityTypes(), "#"));

			// Using PlotManagement Delete
			world_hm.put("usingPlotManagementDelete", world.isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (world.getPlotManagementDeleteIds() != null)
				world_hm.put("plotManagementDeleteIds", StringMgmt.join(world.getPlotManagementDeleteIds(), "#"));

			// Using PlotManagement Mayor Delete
			world_hm.put("usingPlotManagementMayorDelete", world.isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (world.getPlotManagementMayorDelete() != null)
				world_hm.put("plotManagementMayorDelete", StringMgmt.join(world.getPlotManagementMayorDelete(), "#"));

			// Using PlotManagement Revert
			world_hm.put("usingPlotManagementRevert", world.isUsingPlotManagementRevert());

			// Plot Management Ignore Ids
			if (world.getPlotManagementIgnoreIds() != null)
				world_hm.put("plotManagementIgnoreIds", StringMgmt.join(world.getPlotManagementIgnoreIds(), "#"));

			// Using PlotManagement Wild Regen
			world_hm.put("usingPlotManagementWildRegen", world.isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (world.getPlotManagementWildRevertEntities() != null)
				world_hm.put("PlotManagementWildRegenEntities", StringMgmt.join(world.getPlotManagementWildRevertEntities(), "#"));

			// Wilderness Explosion Protection Block Whitelist
			if (world.getPlotManagementWildRevertBlockWhitelist() != null)
				world_hm.put("PlotManagementWildRegenBlockWhitelist", StringMgmt.join(world.getPlotManagementWildRevertBlockWhitelist(), "#"));

			// Using PlotManagement Wild Regen Delay
			world_hm.put("plotManagementWildRegenSpeed", world.getPlotManagementWildRevertDelay());
			
			// Using PlotManagement Wild Block Regen
			world_hm.put("usingPlotManagementWildRegenBlocks", world.isUsingPlotManagementWildBlockRevert());

			// Wilderness Explosion Protection blocks
			if (world.getPlotManagementWildRevertBlocks() != null)
				world_hm.put("PlotManagementWildRegenBlocks", StringMgmt.join(world.getPlotManagementWildRevertBlocks(), "#"));

			world_hm.put("metadata", world.hasMeta() ? serializeMetadata(world) : "");

			return world_hm;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving: World HashMap could not be made.");
			e.printStackTrace();
			throw new Exception(e);
		}
	}
	
	/*
	 * Private methods used to read a key and set a default value from the config if it isn't present.
	 */
	
	private boolean getOrDefault(HashMap<String, String> keys, String key, boolean bool) {
		return Boolean.parseBoolean(keys.getOrDefault(key, String.valueOf(bool)));
	}
	
	private long getOrDefault(HashMap<String, String> keys, String key, long num) {
		return Long.parseLong(keys.getOrDefault(key, String.valueOf(num)));
	}

	private double getOrDefault(HashMap<String, String> keys, String key, double num) {
		return Double.parseDouble(keys.getOrDefault(key, String.valueOf(num)));
	}

	private int getOrDefault(HashMap<String, String> keys, String key, int num) {
		return Integer.parseInt(keys.getOrDefault(key, String.valueOf(num)));
	}
	
	private List<String> toList(String string) {
		List<String> mats = new ArrayList<>();
		if (string != null)
			try {
				for (String s : string.split("#"))
					if (!s.isEmpty())
						mats.add(s);
			} catch (Exception ignored) {
			}
		return mats;
	}

	private String getTownBlockForSaving(TownBlock tb) {
		return tb.getWorld().getUUID() + "#" + tb.getX() + "#" + tb.getZ();
	}

	private TownBlock parseTownBlockFromDB(String input) throws NumberFormatException, NotRegisteredException {
		String[] tokens = input.split("#");
		try {
			UUID uuid = UUID.fromString(tokens[0]);
			if (universe.getWorld(uuid) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(uuid, Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			if (universe.getWorld(tokens[0]) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(tokens[0], Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		}
	}
	
	private String parseLocationForSaving(Location loc) {
		return loc.getWorld().getUID().toString() + "#" 
				+ loc.getX() + "#"
				+ loc.getY() + "#"
				+ loc.getZ() + "#"
				+ loc.getPitch() + "#"
				+ loc.getYaw();
	}
	
	/*
	 * Remove Object Methods
	 */

	protected void removeFromUniverse(TownyDBFileType type, UUID uuid) {
		switch (type) {
		case JAIL -> universe.unregisterJail(uuid);
		case NATION -> universe.unregisterNation(uuid);
		case PLOTGROUP -> universe.unregisterGroup(uuid);
		case RESIDENT -> universe.unregisterResident(uuid);
		case TOWN -> universe.unregisterTown(uuid);
		case TOWNBLOCK -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		case WORLD -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}
	
	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlawlists.
		for (Town townOutlaw : universe.getTowns()) {
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

		// Clear accounts
		if (TownySettings.isDeleteEcoAccount() && TownyEconomyHandler.isActive())
			resident.getAccount().removeAccount();

		if (resident.hasUUID() && !resident.isNPC())
			saveHibernatedResident(resident.getUUID(), resident.getRegistered());

		// Remove the residents record from memory.
		removeFromUniverse(TownyDBFileType.RESIDENT, resident.getUUID());
		plugin.deleteCache(resident);
		// Delete the residents file.
		deleteResident(resident);
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
			if (!event.getCancelMessage().isEmpty())
				logger.warn(event.getCancelMessage());
			return;
		}
		
		if (townBlock.isJail() && townBlock.getJail() != null)
			removeJail(townBlock.getJail());

		universe.removeTownBlock(townBlock);
		deleteTownBlock(townBlock);

		if (townBlock.getWorld().isDeletingEntitiesOnUnclaim())
			TownyRegenAPI.addDeleteTownBlockEntityQueue(townBlock.getWorldCoord());

		if (townBlock.getWorld().isUsingPlotManagementDelete())
			TownyRegenAPI.addDeleteTownBlockIdQueue(townBlock.getWorldCoord());

		// Move the plot to be restored
		if (townBlock.getWorld().isUsingPlotManagementRevert())
			TownyRegenAPI.addToRegenQueueList(townBlock.getWorldCoord(), true);

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
			Bukkit.getPluginManager().callEvent(tpre);
			if (!tpre.isCancelled()) {
				TownRuinUtil.putTownIntoRuinedState(town);
				return;
			}
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
				universe.removeSpawnPoint(town.getSpawn());
			} catch (TownyException ignored) {}
		}
		
		removeTownBlocks(town);

		if (town.hasNation())
			town.removeNation();

		// Remove all of the Town's Residents.
		for (Resident resident : new ArrayList<>(town.getResidents())) {
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
		removeFromUniverse(TownyDBFileType.TOWN, town.getUUID());
		plugin.resetCache();
		deleteTown(town);
		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town, mayor));
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_town2", town.getName()));
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
				universe.removeSpawnPoint(nation.getSpawn());
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

		//Save towns
		List<Town> toSave = new ArrayList<>(nation.getTowns());
		nation.clear();

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
			BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(town, nation));
		}

		removeFromUniverse(TownyDBFileType.NATION, nation.getUUID());
		plugin.resetCache();
		deleteNation(nation);
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
		removeFromUniverse(TownyDBFileType.JAIL, jail.getUUID());
		
		deleteJail(jail);
	}

	@Override
	public void removePlotGroup(PlotGroup group) {
		removeFromUniverse(TownyDBFileType.PLOTGROUP, group.getUUID());
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
	
	private String generateMissingName() {
		// TODO: Make this a thing.
		return "bob";
	}

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
		boolean isSameNation = false;
		if (mergeInto.hasNation() && mergeFrom.hasNation()) {
			try {
				isSameNation = mergeInto.getNation().hasTown(mergeFrom);
			} catch (NotRegisteredException ignored) {}
		}
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
	
	public UUID[] toUUIDArray(String[] uuidArray) throws IllegalArgumentException {
		UUID[] uuids = new UUID[uuidArray.length];
		
		for (int i = 0; i < uuidArray.length; i++)
			uuids[i] = UUID.fromString(uuidArray[i]);
		
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
	
	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getNations(String[])} instead.
	 */
	@Deprecated
	@Override
	public List<Nation> getNations(String[] names) {
		return TownyAPI.getInstance().getNations(names);
	}

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

	/**
	 * @deprecated as of 0.97.5.18, use {@link TownyAPI#getTownBlocks} instead.
	 */
	@Deprecated
	@Override
	public Collection<TownBlock> getAllTownBlocks() {
		return universe.getTownBlocks().values();
	}

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
}