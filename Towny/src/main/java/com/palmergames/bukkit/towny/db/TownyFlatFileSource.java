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
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectLoadException;
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
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

	public enum TownyDBFileType {
		NATION("nations", ".txt", false), TOWN("towns", ".txt", false), RESIDENT("residents", ".txt", false),
		HIBERNATED_RESIDENT("residents" + File.separator + "hibernated", ".txt", false), JAIL("jails", ".txt", true),
		WORLD("worlds", ".txt", false), TOWNBLOCK("townblocks", ".data", true), PLOTGROUP("plotgroups", ".data", true),
		DISTRICT("districts", ".data", true);

		String folderName;
		String fileExtension;
		boolean hasOnlyUUID;

		TownyDBFileType(String folderName, String fileExtension, boolean hasOnlyUUID) {
			this.folderName = folderName;
			this.fileExtension = fileExtension;
			this.hasOnlyUUID = hasOnlyUUID;
		}

		private String getSingular() {
			// Hibernated Residents are never loaded so this method is never called on them.
			return folderName.substring(0, folderName.length() - 1);
		}

		public String getFolderName() {
			return folderName;
		}

		public String getSaveLocation(String fileName) {
			return Towny.getPlugin().getDataFolder().getPath() + File.separator + "data" + File.separator + folderName
					+ File.separator + fileName + fileExtension;
		}

		public String getLoadErrorMsg(UUID uuid) {
			return "Loading Error: Could not read the " + getSingular() + " with UUID '" + uuid + "' from the "
					+ folderName + " folder.";
		}

		public boolean hasOnlyUUID() {
			return hasOnlyUUID;
		}
	}

	private String getFileOfTypeWithUUID(TownyDBFileType type, UUID uuid) {
		return dataFolderPath + File.separator + type.folderName + File.separator + uuid + type.fileExtension;
	}

	private String getFileOfTypeWithName(TownyDBFileType type, String name) {
		return dataFolderPath + File.separator + type.folderName + File.separator + name + type.fileExtension;
	}

	private boolean loadFlatFileListOfType(TownyDBFileType type, Consumer<NameAndId> consumer) {
		TownyMessaging.sendDebugMsg("Searching for " + type.folderName + "...");
		File[] files = new File(dataFolderPath + File.separator + type.folderName)
				.listFiles(file -> file.getName().toLowerCase().endsWith(type.fileExtension));

		if (files.length != 0)
			TownyMessaging.sendDebugMsg("Loading " + files.length + " entries from the " + type.folderName + " folder...");

		for (File file : files) {
			String fileName = file.getName().replace(".txt", "").replace(".data", "");

			// Jails, Districts and PlotGroups have always been UUID-named files and don't require a name.
			if (type.hasOnlyUUID() && fileName.length() == UUID_LENGTH) {
				consumer.accept(new NameAndId("", UUID.fromString(fileName)));
				continue;
			}

			final NameAndId nameAndId = this.loadNameAndUUIDFromFile(file, fileName, type.name());

			if (fileName.length() != UUID_LENGTH && !type.name().equals("WORLD")) {
				final Path filePath = file.toPath();

				try {
					Files.move(filePath, filePath.resolveSibling(nameAndId.uuid() + ".txt"), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					plugin.getSLF4JLogger().warn("Failed to rename name-based file '{}' to uuid variant", fileName, e);
					return false;
				}
			}

			try {
				// Send our NameAndId to the consumer.
				consumer.accept(nameAndId);
			} catch (IllegalArgumentException ignored) {
				plugin.getLogger().warning("The file: " + file.getName() + " in the " + type.folderName + " folder could not be read!");
//				plugin.getLogger().warning("If your database did not convert to UUIDs in full you may open your database.yml and set the version back to 1 in order to try again.");
			}
		}
		return true;
	}

	private boolean loadFlatFilesOfType(TownyDBFileType type, Set<UUID> uuids) throws ObjectLoadException {
		for (UUID uuid : uuids)
			if (!loadFile(type, uuid))
				throw new ObjectLoadException(type.getLoadErrorMsg(uuid));
		return true;
	}

	private boolean loadFile(TownyDBFileType type, UUID uuid) {
		return switch (type) {
		case JAIL -> loadJailData(uuid);
		case PLOTGROUP -> loadPlotGroupData(uuid);
		case DISTRICT -> loadDistrictData(uuid);
		case RESIDENT -> loadResidentData(uuid);
		case TOWN -> loadTownData(uuid);
		case NATION -> loadNationData(uuid);
		case WORLD -> {
			// TODO: Make sure this isn't an issue when worlds load late.
			TownyWorld world = TownyUniverse.getInstance().getWorld(uuid);
			yield world != null && loadWorldData(world.getName());
		}
		case TOWNBLOCK -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	private String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName()
				+ File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize()
				+ ".data";
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadJailList() {
		return loadFlatFileListOfType(TownyDBFileType.JAIL, nameAndId -> universe.newJailInternal(nameAndId.uuid()));
	}

	@Override
	public boolean loadPlotGroupList() {
		return loadFlatFileListOfType(TownyDBFileType.PLOTGROUP, nameAndId -> universe.newPlotGroupInternal(nameAndId.uuid()));
	}

	@Override
	public boolean loadDistrictList() {
		return loadFlatFileListOfType(TownyDBFileType.DISTRICT, nameAndId -> universe.newDistrictInternal(nameAndId.uuid()));
	}

	@Override
	public boolean loadResidentList() {
		return loadFlatFileListOfType(TownyDBFileType.RESIDENT, nameAndId -> {
			if (!universe.hasResident(nameAndId.uuid()))
				universe.newResidentInternal(nameAndId.name(), nameAndId.uuid());
			else {
				final Resident otherResident = universe.getResident(nameAndId.uuid());
				if (otherResident != null && !otherResident.getName().equals(nameAndId.name())) {
					// UUID is already registered
					super.pendingDuplicateResidents.add(Pair.pair(nameAndId.name(), otherResident.getName()));
				}
			}
		});
	}

	@Override
	public boolean loadTownList() {
		return loadFlatFileListOfType(TownyDBFileType.TOWN, nameAndId -> {
			String name = nameAndId.name();
			try {
				NameValidation.checkAndFilterTownNameOrThrow(name);
			} catch (InvalidNameException e) {
				name = generateReplacementName(true);
				String nameAndReason = String.format("'%s', invalid because: %s", nameAndId.name(), e.getMessage());
				super.pendingRenamedTowns.add(Pair.pair(nameAndId.uuid().toString(), nameAndReason));
			}
			universe.newTownInternal(name, nameAndId.uuid());
		});
	}

	@Override
	public boolean loadNationList() {
		return loadFlatFileListOfType(TownyDBFileType.NATION, nameAndId -> {
			String name = nameAndId.name();
			try {
				NameValidation.checkAndFilterNationNameOrThrow(name);
			} catch (InvalidNameException e) {
				name = generateReplacementName(false);
				String nameAndReason = String.format("'%s', invalid because: %s", nameAndId.name(), e.getMessage());
				super.pendingRenamedNations.add(Pair.pair(nameAndId.uuid().toString(), nameAndReason));
			}
			universe.newNationInternal(name, nameAndId.uuid());
		});
	}

	@Override
	public boolean loadWorldList() {
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_server_world_list"));
		loadFlatFileListOfType(TownyDBFileType.WORLD, nameAndId -> universe.newWorldInternal(nameAndId.name(), nameAndId.uuid()));
		for (World world : Bukkit.getServer().getWorlds()) {
			if (universe.getWorldIDMap().containsKey(world.getUID()))
				continue;

			// Register and create files for any worlds which did not have files yet.
			TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
			universe.registerTownyWorld(townyWorld);
			File worldFile = new File(getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName()));
			if (!worldFile.exists())
				try {
					FileMgmt.mapToFile(townyWorld.getObjectDataMap(context), Paths.get(getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName())));
				} catch (Exception e) {
					plugin.getLogger().warning("Could not save new world file for TownyWorld: " + townyWorld.getUUID());
					e.printStackTrace();
				}
		}
		return true;
	}

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

				// Run a test for worlds that bukkit doesn't recognize. These worlds can sometimes be loaded after Towny.
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
				// Ordinarily, all worlds that have existed in the database will have loaded and
				// the TownyUniverse will know about them. Any world that returns null here have
				// some other underlying issue.
				if (world == null) {
					Towny.getPlugin().getLogger().warning("Your towny\\data\\townblocks\\ folder contains a folder named '"
							+ worldName + "' which doesn't appear to exist in your Towny database!");
					Towny.getPlugin().getLogger().warning("Towny will not be able to load the townblocks, if this world no longer exists please delete the folder.");
					continue;
				}

				File worldFolder = new File(dataFolderPath + File.separator + "townblocks" + File.separator + worldName);
				File[] townBlockFiles = worldFolder.listFiles(file -> file.getName().endsWith(".data"));
				int total = 0;
				for (File townBlockFile : townBlockFiles) {
					String[] coords = townBlockFile.getName().split("_");
					String[] size = coords[2].split("\\.");
					// Do not load a townBlockFile if it does not use teh currently set
					// town_block_size.
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
			e1.printStackTrace();
			return false;
		}
	}

	/*
	 * Load individual Towny object-callers
	 */

	@Override
	public boolean loadJailUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.JAIL, uuids);
	}

	@Override
	public boolean loadPlotGroupUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.PLOTGROUP, uuids);
	}

	@Override
	public boolean loadDistrictUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.DISTRICT, uuids);
	}

	@Override
	public boolean loadResidentUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.RESIDENT, uuids);
	}

	@Override
	public boolean loadTownUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.TOWN, uuids);
	}

	@Override
	public boolean loadNationUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.NATION, uuids);
	}

	@Override
	public boolean loadWorldUUIDs(Set<UUID> uuids) throws ObjectLoadException {
		return loadFlatFilesOfType(TownyDBFileType.WORLD, uuids);
	}

	@Override
	public boolean loadTownBlocks(Collection<TownBlock> townBlocks) throws ObjectLoadException {
		for (TownBlock townBlock : townBlocks)
			if (!loadTownBlock(townBlock))
				throw new ObjectLoadException("The Townblock: '" + townBlock.toString() + "' could not be read from the database!");
		return true;
	}

	/*
	 * Return Loadable Objects as Maps for TownyDataBaseHandler to load. 
	 */

	@Override
	public Map<String, String> getJailMap(UUID uuid) {
		File jailFile = new File(getFileOfTypeWithUUID(TownyDBFileType.JAIL, uuid));
		if (jailFile.exists() && jailFile.isFile())
			return FileMgmt.loadFileIntoHashMap(jailFile);
		TownyMessaging.sendErrorMsg("Cannot find a jail file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getPlotGroupMap(UUID uuid) {
		File groupFile = new File(getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, uuid));
		if (groupFile.exists() && groupFile.isFile())
			return FileMgmt.loadFileIntoHashMap(groupFile);
		TownyMessaging.sendErrorMsg("Cannot find a plotgroup file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getDistrictMap(UUID uuid) {
		File districtFile = new File(getFileOfTypeWithUUID(TownyDBFileType.DISTRICT, uuid));
		if (districtFile.exists() && districtFile.isFile())
			return FileMgmt.loadFileIntoHashMap(districtFile);
		TownyMessaging.sendErrorMsg("Cannot find a district file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getResidentMap(UUID uuid) {
		File residentFile = new File(getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, uuid));
		if (residentFile.exists() && residentFile.isFile())
			return FileMgmt.loadFileIntoHashMap(residentFile);
		TownyMessaging.sendErrorMsg("Cannot find a resident file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getTownMap(UUID uuid) {
		File townFile = new File(getFileOfTypeWithUUID(TownyDBFileType.TOWN, uuid));
		if (townFile.exists() && townFile.isFile())
			return FileMgmt.loadFileIntoHashMap(townFile);
		TownyMessaging.sendErrorMsg("Cannot find a town file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getNationMap(UUID uuid) {
		File nationFile = new File(getFileOfTypeWithUUID(TownyDBFileType.NATION, uuid));
		if (nationFile.exists() && nationFile.isFile())
			return FileMgmt.loadFileIntoHashMap(nationFile);
		TownyMessaging.sendErrorMsg("Cannot find a nation file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getWorldMap(String name) {
		File worldFile = new File(getFileOfTypeWithName(TownyDBFileType.WORLD, name));
		if (worldFile.exists() && worldFile.isFile())
			return FileMgmt.loadFileIntoHashMap(worldFile);
		TownyMessaging.sendErrorMsg("Cannot find a world file with the name " + name + "!");
		return null;
	}

	@Override
	public Map<String, String> getTownBlockMap(TownBlock townBlock) {
		File fileTownBlock = new File(getTownBlockFilename(townBlock));
		if (fileTownBlock.exists() && fileTownBlock.isFile())
			return FileMgmt.loadFileIntoHashMap(fileTownBlock);	
		TownyMessaging.sendErrorMsg("Cannot find a townBlock file for " + townBlock.toString() + "!");
		return null;
	}

	/*
	 * Save individual towny objects
	 */

	public boolean saveJail(Jail jail) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(jail.getObjectDataMap(context), getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID())));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean savePlotGroup(PlotGroup group) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(group.getObjectDataMap(context),  getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getUUID())));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveDistrict(District district) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(district.getObjectDataMap(context),  getFileOfTypeWithUUID(TownyDBFileType.DISTRICT, district.getUUID())));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveResident(Resident resident) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(resident.getObjectDataMap(context),  getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, resident.getUUID())));
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
		this.queryQueue.add(new FlatFileSaveTask(res_hm, getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid)));
		return true;
	}

	@Override
	public boolean saveTown(Town town) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(town.getObjectDataMap(context), getFileOfTypeWithUUID(TownyDBFileType.TOWN, town.getUUID())));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveNation(Nation nation) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(nation.getObjectDataMap(context), getFileOfTypeWithUUID(TownyDBFileType.NATION, nation.getUUID())));
			return true;
		} catch (ObjectSaveException e) {
			plugin.getLogger().log(Level.WARNING, e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveWorld(TownyWorld world) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(world.getObjectDataMap(context), getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName())));
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

	/*
	 * Delete objects
	 */

	// Private FlatFile method for deleting database objects.
	private void deleteFileByTypeAndUUID(TownyDBFileType type, UUID uuid) {
		File file = new File(getFileOfTypeWithUUID(type, uuid));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	// Private FlatFile method for deleting legacy database objects keyed by names.
	void deleteFileByTypeAndName(TownyDBFileType type, String name) {
		File file = new File(getFileOfTypeWithName(type, name));
		queryQueue.add(new DeleteFileTask(file, false));
	}

	@Override
	public void deleteResident(Resident resident) {
		deleteFileByTypeAndUUID(TownyDBFileType.RESIDENT, resident.getUUID());
	}

	@Override
	public void deleteHibernatedResident(UUID uuid) {
		deleteFileByTypeAndUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid);
	}

	@Override
	public void deleteTown(Town town) {
		deleteFileByTypeAndUUID(TownyDBFileType.TOWN, town.getUUID());
	}

	@Override
	public void deleteNation(Nation nation) {
		deleteFileByTypeAndUUID(TownyDBFileType.NATION, nation.getUUID());
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		deleteFileByTypeAndUUID(TownyDBFileType.WORLD, world.getUUID());
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {

		File file = new File(getTownBlockFilename(townBlock));

		queryQueue.add(() -> {
			if (file.exists()) {
				// TownBlocks can end up being deleted because they do not contain valid towns.
				// This will move a deleted townblock to either:
				// towny\townblocks\worldname\deleted\townname folder, or the
				// towny\townblocks\worldname\deleted\ folder if there is not valid townname.
				String name = null;
				try {
					name = townBlock.getTown().getUUID().toString();
				} catch (NotRegisteredException ignored) {
				}
				if (name != null)
					FileMgmt.moveTownBlockFile(file, "deleted", name);
				else
					FileMgmt.moveTownBlockFile(file, "deleted", "");
			}
		});
	}

	@Override
	public void deletePlotGroup(PlotGroup group) {
		deleteFileByTypeAndUUID(TownyDBFileType.PLOTGROUP, group.getUUID());
	}

	@Override
	public void deleteDistrict(District district) {
		deleteFileByTypeAndUUID(TownyDBFileType.DISTRICT, district.getUUID());
	}

	@Override
	public void deleteJail(Jail jail) {
		deleteFileByTypeAndUUID(TownyDBFileType.JAIL, jail.getUUID());
	}

	@Override
	public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			File hibernatedFile = new File(getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid));
			
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
