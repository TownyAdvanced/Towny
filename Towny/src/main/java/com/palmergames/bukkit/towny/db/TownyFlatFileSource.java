package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectSaveException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.NameAndId;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class TownyFlatFileSource extends TownyDatabaseHandler {
	private static final int UUID_LENGTH = 36;
	private static final SerializationContext context = new SerializationContext(true);
	
	public TownyFlatFileSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		// Create files and folders if non-existent
		if (!FileMgmt.checkOrCreateFolders(
			rootFolderPath,
			dataFolderPath,
			dataFolderPath + File.separator + "residents",
			dataFolderPath + File.separator + "residents" + File.separator + "deleted",
			dataFolderPath + File.separator + "residents" + File.separator + "hibernated",
			dataFolderPath + File.separator + "towns",
			dataFolderPath + File.separator + "towns" + File.separator + "deleted",
			dataFolderPath + File.separator + "nations",
			dataFolderPath + File.separator + "nations" + File.separator + "deleted",
			dataFolderPath + File.separator + "worlds",
			dataFolderPath + File.separator + "worlds" + File.separator + "deleted",
			dataFolderPath + File.separator + "townblocks",
			dataFolderPath + File.separator + "plotgroups",
			dataFolderPath + File.separator + "plotgroups" + File.separator + "deleted",
			dataFolderPath + File.separator + "districts",
			dataFolderPath + File.separator + "districts" + File.separator + "deleted",
			dataFolderPath + File.separator + "jails",
			dataFolderPath + File.separator + "jails" + File.separator + "deleted"
		)) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_cannot_create_defaults"));
		}
	}
	
	public enum elements {
		VER, NOVALUE;

		public static elements fromString(String str) {

			try {
				return valueOf(str);
			} catch (Exception ex) {
				return NOVALUE;
			}
		}
	}

	public String getResidentFilename(Resident resident) {

		return dataFolderPath + File.separator + "residents" + File.separator + resident.getUUID() + ".txt";
	}
	
	public String getHibernatedResidentFilename(UUID uuid) {

		return dataFolderPath + File.separator + "residents" + File.separator + "hibernated" + File.separator + uuid + ".txt";
	}

	public String getTownFilename(Town town) {

		return dataFolderPath + File.separator + "towns" + File.separator + town.getUUID() + ".txt";
	}

	public String getNationFilename(Nation nation) {

		return dataFolderPath + File.separator + "nations" + File.separator + nation.getUUID() + ".txt";
	}

	public String getWorldFilename(TownyWorld world) {

		return dataFolderPath + File.separator + "worlds" + File.separator + world.getName() + ".txt";
	}

	public String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}
	
	public String getPlotGroupFilename(PlotGroup group) {
		return dataFolderPath + File.separator + "plotgroups" + File.separator + group.getUUID() + ".data";
	}

	public String getDistrictFilename(District district) {
		return dataFolderPath + File.separator + "districts" + File.separator + district.getUUID() + ".data";
	}

	public String getJailFilename(Jail jail) {
		return dataFolderPath + File.separator + "jails" + File.separator + jail.getUUID() + ".txt";
	}
	
	/*
	 * Load keys
	 */
	
	@Override
	public boolean loadTownBlockList() {
		
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_townblock_list"));

		File townblocksFolder = new File(dataFolderPath + File.separator + "townblocks");
		File[] worldFolders = townblocksFolder.listFiles(File::isDirectory);
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_folders_found", worldFolders.length));
		boolean mismatched = false;
		int mismatchedCount = 0;
		try {
			for (File worldfolder : worldFolders) {
				String worldName = worldfolder.getName();
				if (BukkitTools.getWorld(worldName) == null) {
					Towny.getPlugin().getScheduler().runAsyncLater(() -> {
						// Check if the World is still null in Bukkit and warn the admin.
						if (BukkitTools.getWorld(worldName) == null) {
							Towny.getPlugin().getLogger().warning("Your towny\\data\\townblocks\\ folder contains a folder named '"
									+ worldName + "' which doesn't appear to exist on your Bukkit server!");
							Towny.getPlugin().getLogger().warning("Towny will load the townblocks regardless, but if this world no longer exists please delete the folder.");
						}
					}, 20L);
				}

				TownyWorld world = universe.getWorld(worldName);
				if (world == null) {
					newWorld(worldName);
					world = universe.getWorld(worldName);
				}
				File worldFolder = new File(dataFolderPath + File.separator + "townblocks" + File.separator + worldName);
				File[] townBlockFiles = worldFolder.listFiles(file->file.getName().endsWith(".data"));
				int total = 0;
				for (File townBlockFile : townBlockFiles) {
					String[] coords = townBlockFile.getName().split("_");
					String[] size = coords[2].split("\\.");
					// Do not load a townBlockFile if it does not use teh currently set town_block_size.
					if (Integer.parseInt(size[0]) != TownySettings.getTownBlockSize()) {
						mismatched = true;
						mismatchedCount++;
						continue;
					}
					int x = Integer.parseInt(coords[0]);
					int z = Integer.parseInt(coords[1]);
	                TownBlock townBlock = new TownBlock(x, z, world);
	                universe.addTownBlock(townBlock);
					total++;
				}
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_world_loaded_townblocks", worldName, total));
			}
			if (mismatched)
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_mismatched_townblock_size", mismatchedCount));

			return true;
		} catch (Exception e1) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred while loading the flatfile townblock list", e1);
			return false;
		}
	}
	
	@Override
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_group_list"));
		File[] plotGroupFiles = receiveObjectFiles("plotgroups", ".data");
		
		if (plotGroupFiles == null)
			return true; 
		
		for (File plotGroup : plotGroupFiles)
			universe.newPlotGroupInternal(UUID.fromString(plotGroup.getName().replace(".data", "")));
		
		return true;
	}

	@Override
	public boolean loadDistrictList() {
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_district_list"));
		File[] districtFiles = receiveObjectFiles("districts", ".data");
		
		if (districtFiles == null)
			return true; 
		
		for (File districtFile : districtFiles)
			universe.newDistrictInternal(UUID.fromString(districtFile.getName().replace(".data", "")));
		
		return true;
	}
	
	@Override
	public boolean loadResidentList() {
		
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_resident_list"));
		List<String> residents = receiveListFromLegacyFile("residents.txt");
		File[] residentFiles = receiveObjectFiles("residents", ".txt");

		for (File residentFile : residentFiles) {
			String fileName = residentFile.getName().replace(".txt", "");

			// Don't load resident files if they weren't in the residents.txt file.
			if (!residents.isEmpty() && !residents.contains(fileName)) {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_removing_resident_not_found", residentFile.getName()));
				deleteFile(residentFile.getAbsolutePath());
				continue;
			}
			
			String name;
			String uuidString;

			if (fileName.length() == UUID_LENGTH) {
				name = loadKeyFromFile(residentFile, "name");
				uuidString = fileName;
			} else {
				uuidString = this.loadKeyFromFile(residentFile, "uuid");
				name = fileName;
			}

			final @Nullable UUID uuid = super.parsePlayerUUID(uuidString, fileName);

			if (uuid == null) {
				plugin.getLogger().warning("Resident '" + name + "' does not have a valid uuid and cannot be loaded.");
				continue;
			}
			
			if (fileName.length() != UUID_LENGTH) {
				final Path residentFilePath = residentFile.toPath();

				try {
					Files.move(residentFilePath, residentFilePath.resolveSibling(uuid + ".txt"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					plugin.getSLF4JLogger().warn("Failed to rename name-based resident file '{}' to uuid variant", fileName, e);
					return false;
				}
			}
				
			try {
				newResident(name, uuid);
			} catch (NotRegisteredException e) {
				// Thrown if the resident name does not pass the filters.
				plugin.getLogger().log(Level.WARNING, "Resident " + name + " has an invalid name", e);
				return false;
			} catch (AlreadyRegisteredException e) {
				final Resident otherResident = universe.getResident(uuid);
				if (otherResident != null && !otherResident.getName().equals(name)) {
					// UUID is already registered
					super.pendingDuplicateResidents.add(Pair.pair(name, otherResident.getName()));
				}
			}			
		}

		if (!residents.isEmpty())
			deleteFile(dataFolderPath + File.separator + "residents.txt");

		return true;
			
	}

	@Override
	public boolean loadTownList() {
		
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_town_list"));
		List<String> towns = receiveListFromLegacyFile("towns.txt");
		File[] townFiles = receiveObjectFiles("towns", ".txt");

		List<NameAndId> rejectedTowns = new ArrayList<>();
		
		for (File townFile : townFiles) {
			String fileName = townFile.getName().replace(".txt", "");

			// Don't load town files if they weren't in the towns.txt file.
			if (!towns.isEmpty() && !towns.contains(fileName)) {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_removing_town_not_found", townFile.getName()));
				deleteFile(townFile.getAbsolutePath());
				continue;
			}
			
			final NameAndId nameAndId = this.loadNameAndUUIDFromFile(townFile, fileName, "town");
			
			if (fileName.length() != UUID_LENGTH) {
				final Path townFilePath = townFile.toPath();

				try {
					Files.move(townFilePath, townFilePath.resolveSibling(nameAndId.uuid() + ".txt"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					plugin.getSLF4JLogger().warn("Failed to rename name-based town file '{}' to uuid variant", fileName, e);
					return false;
				}
			}
			
			try {
				universe.newTownInternal(nameAndId.name(), nameAndId.uuid());
			} catch (AlreadyRegisteredException | InvalidNameException e) {
				// Thrown if the town name does not pass the filters.
				rejectedTowns.add(nameAndId);
			}
		}
		
		// Delete legacy file towns.txt if it was present.
		if (!towns.isEmpty())
			deleteFile(dataFolderPath + File.separator + "towns.txt");

		// Handle rejected town names after all the rest are loaded.
		for (NameAndId town : rejectedTowns) {
			String name = town.name();
			String newName = generateReplacementName(true);
			universe.getReplacementNameMap().put(name, newName);
			TownyMessaging.sendErrorMsg(String.format("The town %s (%s) tried to load an invalid name, attempting to rename it to %s.", name, town.uuid(), newName));
			try {
				universe.newTownInternal(newName, town.uuid());
			} catch (AlreadyRegisteredException | InvalidNameException e1) {
				// We really hope this doesn't fail again.
				plugin.getSLF4JLogger().warn("exception occurred while registering town '{}' ({}) internally", newName, town.uuid(), e1);
				return false;
			}
		}

		return true;

	}

	@Override
	public boolean loadNationList() {
		
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_nation_list"));
		List<String> nations = receiveListFromLegacyFile("nations.txt");
		File[] nationFiles = receiveObjectFiles("nations", ".txt");

		List<NameAndId> rejectedNations = new ArrayList<>();
		
		for (File nationFile : nationFiles) {
			String fileName = nationFile.getName().replace(".txt", "");

			// Don't load nation files if they weren't in the nations.txt file.
			if (!nations.isEmpty() && !nations.contains(fileName)) {
				TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_removing_nation_not_found", nationFile.getName()));
				deleteFile(nationFile.getAbsolutePath());
				continue;
			}
			
			final NameAndId nameAndId = this.loadNameAndUUIDFromFile(nationFile, fileName, "nation");

			if (fileName.length() != UUID_LENGTH) {
				final Path nationFilePath = nationFile.toPath();

				try {
					Files.move(nationFilePath, nationFilePath.resolveSibling(nameAndId.uuid() + ".txt"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					plugin.getSLF4JLogger().warn("Failed to rename name-based nation file '{}' to uuid variant", fileName, e);
					return false;
				}
			}
		
			try {
				newNation(nameAndId.name(), nameAndId.uuid());
			} catch (AlreadyRegisteredException | NotRegisteredException e) {
				// Thrown if the town name does not pass the filters.
				rejectedNations.add(nameAndId);
			}
		}
		
		// Delete legacy file towns.txt if it was present.
		if (!nations.isEmpty())
			deleteFile(dataFolderPath + File.separator + "nations.txt");
			
		// Handle rejected nation names after all the rest are loaded.
		for (NameAndId nation : rejectedNations) {
			String name = nation.name();
			String newName = generateReplacementName(false);
			universe.getReplacementNameMap().put(name, newName);
			TownyMessaging.sendErrorMsg(String.format("The nation %s (%s) tried to load an invalid name, attempting to rename it to %s.", name, nation.uuid(), newName));
			try {
				newNation(newName, nation.uuid());
			} catch (AlreadyRegisteredException | NotRegisteredException e1) {
				// we really hope this doesn't fail a second time.
				plugin.getLogger().log(Level.WARNING, "exception occurred while registering nation '" + newName + "' internally", e1);
				return false;
			}
		}
		return true;

	}
	
	@Override
	public boolean loadWorldList() {
		
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_server_world_list"));
		for (World world : Bukkit.getServer().getWorlds())
			universe.registerTownyWorld(new TownyWorld(world.getName(), world.getUID()));

		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_world_list"));
		
		for (File worldFile : receiveObjectFiles("worlds", ".txt")) {
			final String name = worldFile.getName().replace(".txt", "");
			
			// World is already loaded by the newWorld above
			if (universe.getWorld(name) != null)
				continue;
			
			// Attempt to get the uuid from the world file
			UUID uuid = null;
			try {
				uuid = UUID.fromString(Optional.ofNullable(this.loadKeyFromFile(worldFile, "uuid")).orElse(""));
			} catch (IllegalArgumentException ignored) {}

			if (uuid != null) {
				universe.registerTownyWorld(new TownyWorld(name, uuid));
			} else {
				try {
					newWorld(name);
				} catch (AlreadyRegisteredException ignored) {}
			}
		}
		
		return true;
	}

	public boolean loadJailList() {
		TownyMessaging.sendDebugMsg("Loading Jail List");
		File[] jailFiles = receiveObjectFiles("jails", ".txt");
		if (jailFiles == null)
			return true;
		
		for (File jail : jailFiles) {
			String uuid = jail.getName().replace(".txt", "");
			universe.newJailInternal(uuid);
		}
		
		return true;
	}
	
	/**
	 * Util method to procur a list of Towny Objects that will no longer be saved.
	 * ex: residents.txt, towns.txt, nations.txt, etc.
	 * 
	 * @param listFile - string representing residents.txt/towns.txt/nations.txt.
	 * @return list - List<String> of names of towny objects which used to be saved to the database. 
	 */
	private List<String> receiveListFromLegacyFile(String listFile) {
		String line;
		List<String> list = new ArrayList<>();
		// Build up a list of objects from any existing legacy objects.txt files.
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + listFile), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null && !line.equals(""))
				list.add(line);
		} catch (Exception ignored) {
			// No towns/residents/nations.txt any more.
		}
		return list;
	}

	/**
	 * Util method for gathering towny object .txt files from their parent folder.
	 * ex: "residents" 
	 * @param folder - Towny object folder
	 * @param extension - Extension of the filetype to receive objects from.
	 * @return files - Files from inside the residents\towns\nations folder.
	 */
	private File[] receiveObjectFiles(String folder, String extension) {
		return new File(dataFolderPath + File.separator + folder).listFiles(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
	}
	
	/*
	 * Load individual towny objects
	 */
	
	@Override
	public boolean loadResident(Resident resident) {
		File fileResident = new File(getResidentFilename(resident));
		if (fileResident.exists() && fileResident.isFile()) {
			TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_resident", resident.getName()));
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileResident);
				return resident.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		return false;
	}
	
	@Override
	public boolean loadTown(Town town) {
		File fileTown = new File(getTownFilename(town));
		if (fileTown.exists() && fileTown.isFile()) {
			TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_town", town.getName()));
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileTown);
				return town.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		return false;
	}
	
	@Override
	public boolean loadNation(Nation nation) {
		File fileNation = new File(getNationFilename(nation));
		if (fileNation.exists() && fileNation.isFile()) {
			TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_nation", nation.getName()));
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileNation);
				return nation.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		return false;
	}
	
	@Override
	public boolean loadWorld(TownyWorld world) {
		String path = getWorldFilename(world);
		// create the world file if it doesn't exist
		if (!FileMgmt.checkOrCreateFile(path)) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_file", path));
		}

		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_world", world.getName()));
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileWorld);
				return world.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		String line = "";
		// TODO: This error message isn't correct, has nothing to do with the line in a file.
		TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_file_error_reading_world_file_at_line", world.getName(), line, world.getName()));
		return false;
	}
	
	public boolean loadPlotGroup(PlotGroup group) {
		File groupFile = new File(getPlotGroupFilename(group));
		if (groupFile.exists() && groupFile.isFile()) {
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(groupFile);
				group.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_missing_file_delete_groups_entry", getPlotGroupFilename(group)));
		return false;
	}

	public boolean loadDistrict(District district) {
		File districtFile = new File(getDistrictFilename(district));
		if (districtFile.exists() && districtFile.isFile()) {
			try {
				HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(districtFile);
				return district.load(keys);
			} catch (Exception e) {
				plugin.getLogger().log(Level.WARNING, e.getMessage());
			}
		}
		return false;
	}
	
	@Override
	public boolean loadTownBlocks() {
		String path;
		for (TownBlock townBlock : universe.getTownBlocks().values()) {
			path = getTownBlockFilename(townBlock);
			
			File fileTownBlock = new File(path);
			if (fileTownBlock.exists() && fileTownBlock.isFile()) {
				try {
					HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileTownBlock);
					townBlock.load(keys);
				} catch (Exception e) {
					plugin.getLogger().log(Level.WARNING, e.getMessage());
				}
			} else {
				TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_unknown_err", path));
				universe.removeTownBlock(townBlock);
				deleteTownBlock(townBlock);
			}
		}

		return true;
	}

	public boolean loadJail(Jail jail) {
		File jailFile = new File(getJailFilename(jail));
		if (jailFile.exists() && jailFile.isFile()) {
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(jailFile);
			return jail.load(keys);
		}
		return false;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(resident.getObjectDataMap(context), getResidentFilename(resident)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}
	
	@Override
	public boolean saveHibernatedResident(UUID uuid, long registered) {
		HashMap<String, Object> res_hm = new HashMap<>();
		res_hm.put("registered", registered);
		this.queryQueue.add(new FlatFileSaveTask(res_hm, getHibernatedResidentFilename(uuid)));
		return true;
	}

	@Override
	public boolean saveTown(Town town) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(town.getObjectDataMap(context), getTownFilename(town)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}
	
	@Override
	public boolean savePlotGroup(PlotGroup group) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(group.getObjectDataMap(context), getPlotGroupFilename(group)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveDistrict(District district) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(district.getObjectDataMap(context), getDistrictFilename(district)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveNation(Nation nation) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(nation.getObjectDataMap(context), getNationFilename(nation)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveWorld(TownyWorld world) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(world.getObjectDataMap(context), getWorldFilename(world)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock) {

		if (!townBlock.hasTown())
			return false;

		FileMgmt.checkOrCreateFolder(dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName());
		try {
			this.queryQueue.add(new FlatFileSaveTask(townBlock.getObjectDataMap(context), getTownBlockFilename(townBlock)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	public boolean saveJail(Jail jail) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(jail.getObjectDataMap(context), getJailFilename(jail)));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	/*
	 * Delete objects
	 */
	
	@Override
	public void deleteResident(Resident resident) {
		File file = new File(getResidentFilename(resident));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override 
	public void deleteHibernatedResident(UUID uuid) {
		File file = new File(getHibernatedResidentFilename(uuid));
		queryQueue.add(new DeleteFileTask(file, true));
	}
	
	@Override
	public void deleteTown(Town town) {
		File file = new File(getTownFilename(town));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteNation(Nation nation) {
		File file = new File(getNationFilename(nation));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		File file = new File(getWorldFilename(world));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {

		File file = new File(getTownBlockFilename(townBlock));
		if (!file.exists())
			return;

		// TownBlocks can end up being deleted because they do not contain valid towns.
		// This will move a deleted townblock to either: 
		// towny\townblocks\worldname\deleted\townname folder, or the
		// towny\townblocks\worldname\deleted\ folder if there is not valid townname.
		queryQueue.add(() -> FileMgmt.moveTownBlockFile(file, "deleted", townBlock.hasTown() ? townBlock.getTownOrNull().getName() : ""));
	}
	
	@Override
	public void deletePlotGroup(PlotGroup group) {
    	File file = new File(getPlotGroupFilename(group));
    	queryQueue.add(new DeleteFileTask(file, false));
	}
	
	@Override
	public void deleteDistrict(District district) {
		File file = new File(getDistrictFilename(district));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteJail(Jail jail) {
		File file = new File(getJailFilename(jail));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			File hibernatedFile = new File(getHibernatedResidentFilename(uuid));
			
			if (!hibernatedFile.exists())
				return Optional.empty();
			
			Map<String, String> keys = FileMgmt.loadFileIntoHashMap(hibernatedFile);
			String registered = keys.get("registered");
			if (registered == null || registered.isEmpty())
				return Optional.empty();
			
			try {
				return Optional.of(Long.parseLong(registered));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		});
	}

	@Override
	public boolean loadCooldowns() {
		final Path cooldownsFile = Paths.get(dataFolderPath).resolve("cooldowns.json");
		if (!Files.exists(cooldownsFile))
			return true;

		final String data;
		try {
			data = Files.readString(cooldownsFile);
		} catch (IOException e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred when reading cooldowns.json", e);
			return true;
		}
		
		try {
			CooldownTimerTask.getCooldowns().putAll(new Gson().fromJson(data, new TypeToken<Map<String, Long>>(){}.getType()));
		} catch (JsonSyntaxException e) {
			plugin.getLogger().log(Level.WARNING, "Could not load saved cooldowns due to a json syntax exception", e);
		} catch (NullPointerException ignored) {}
		
		return true;
	}

	@Override
	public boolean saveCooldowns() {
		final JsonObject object = new JsonObject();
		
		for (Map.Entry<String, Long> cooldown : CooldownTimerTask.getCooldowns().entrySet())
			object.addProperty(cooldown.getKey(), cooldown.getValue());
		
		this.queryQueue.add(() -> {
			try {
				Files.writeString(Paths.get(dataFolderPath).resolve("cooldowns.json"), new GsonBuilder().setPrettyPrinting().create().toJson(object), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred when writing cooldowns.json", e);
			}
		});
		
		return true;
	}

	private String loadKeyFromFile(File file, String key) {
		final String searchingForLine = key + "=";
		
		try (final Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
			return lines.filter(line -> line.startsWith(searchingForLine))
				.findFirst()
				.map(uuidLine -> uuidLine.substring(searchingForLine.length()).trim())
				.orElse(null);
		} catch (IOException e) {
			plugin.getSLF4JLogger().error("An IO exception occurred while attempting to read key '{}' from file {}", key, file.getName(), e);
			return null;
		}
	}
	
	private NameAndId loadNameAndUUIDFromFile(File file, String fileName, String describedAs) {
		String possibleUUID;
		String name;

		if (fileName.length() == UUID_LENGTH) {
			// new behavior, file name is a UUID
			possibleUUID = fileName;
			name = loadKeyFromFile(file, "name");
		} else {
			// old file, file name is already a name
			possibleUUID = loadKeyFromFile(file, "uuid");
			name = fileName;
		}

		final UUID uuid = super.parseUUIDOrNew(possibleUUID, describedAs + " '" + name + "'");
		return new NameAndId(name, uuid);
	}
}
