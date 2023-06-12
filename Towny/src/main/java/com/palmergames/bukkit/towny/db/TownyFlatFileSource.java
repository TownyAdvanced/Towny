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
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectCouldNotBeLoadedException;
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
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TownyFlatFileSource extends TownyDatabaseHandler {

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
			dataFolderPath + File.separator + "jails",
			dataFolderPath + File.separator + "jails" + File.separator + "deleted"
		)) {
			TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_cannot_create_defaults"));
		}

		if (TownySettings.getDatabaseVersion().equals("1")) {
			TownyLegacyFlatFileConverter converter = new TownyLegacyFlatFileConverter(plugin, this);
			plugin.getLogger().info("** Legacy Database Detected! Attempting UUID update! **");
			long startTime = System.currentTimeMillis();
			if (converter.updateLegacyFlatFileDB()) {
				TownySettings.setDatabaseVersion("2");
				long time = System.currentTimeMillis() - startTime;
				plugin.getLogger().info("** Database converted in " + time + "ms. **");
				plugin.getLogger().info("** Legacy Database Update Complete! **");
			} else {
				plugin.getLogger().warning("** Legacy Database Update Unsuccessful! **");
			}
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
		ALLIANCE("alliances", ".txt"), NATION("nations", ".txt"), TOWN("towns", ".txt"), RESIDENT("residents", ".txt"),
		HIBERNATED_RESIDENT("residents" + File.separator + "hibernated", ".txt"), JAIL("jails", ".txt"),
		WORLD("worlds", ".txt"), TOWNBLOCK("townblocks", ".data"), PLOTGROUP("plotgroups", ".data");

		String folderName;
		String fileExtension;

		TownyDBFileType(String folderName, String fileExtension) {
			this.folderName = folderName;
			this.fileExtension = fileExtension;
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
	}

	private String getFileOfTypeWithUUID(TownyDBFileType type, UUID uuid) {
		return dataFolderPath + File.separator + type.folderName + File.separator + uuid + type.fileExtension;
	}

	private String getFileOfTypeWithName(TownyDBFileType type, String name) {
		return dataFolderPath + File.separator + type.folderName + File.separator + name + type.fileExtension;
	}

	private boolean loadFlatFileListOfType(TownyDBFileType type, Consumer<UUID> consumer) {
		TownyMessaging.sendDebugMsg("Searching for " + type.folderName + "...");
		File[] files = new File(dataFolderPath + File.separator + type.folderName)
				.listFiles(file -> file.getName().toLowerCase().endsWith(type.fileExtension));

		if (files.length != 0)
			TownyMessaging.sendDebugMsg("Loading " + files.length + " entries from the " + type.folderName + " folder...");

		for (File file : files)
			try {
				// Send our UUID to the consumer.
				consumer.accept(UUID.fromString(file.getName().replace(type.fileExtension, "")));
			} catch (IllegalArgumentException ignored) {
				// TownyLegacyFlatFileConverter should have taken care of things before we get here.
				plugin.getLogger().warning("The file: " + file.getName() + " in the " + type.folderName + " folder could not be read!");
				plugin.getLogger().warning("If your database did not convert to UUIDs in full you may open your database.yml and set the version back to 1 in order to try again.");
			}
		return true;
	}

	private boolean loadFlatFilesOfType(TownyDBFileType type, Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		for (UUID uuid : uuids)
			if (!loadFile(type, uuid))
				throw new ObjectCouldNotBeLoadedException(type.getLoadErrorMsg(uuid));
		return true;
	}

	private boolean loadFile(TownyDBFileType type, UUID uuid) {
		return switch (type) {
		case JAIL -> loadJailData(uuid);
		case PLOTGROUP -> loadPlotGroupData(uuid);
		case RESIDENT -> loadResidentData(uuid);
		case TOWN -> loadTownData(uuid);
		case NATION -> loadNationData(uuid);
		case WORLD -> loadWorldData(uuid);
		case TOWNBLOCK -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	private String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getUUID()
				+ File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize()
				+ ".data";
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadJailList() {
		return loadFlatFileListOfType(TownyDBFileType.JAIL, uuid -> universe.newJailInternal(uuid));
	}

	@Override
	public boolean loadPlotGroupList() {
		return loadFlatFileListOfType(TownyDBFileType.PLOTGROUP, uuid -> universe.newPlotGroupInternal(uuid));
	}

	@Override
	public boolean loadResidentList() {
		return loadFlatFileListOfType(TownyDBFileType.RESIDENT, uuid -> universe.newResidentInternal(uuid));
	}

	@Override
	public boolean loadTownList() {
		return loadFlatFileListOfType(TownyDBFileType.TOWN, uuid -> universe.newTownInternal(uuid));
	}

	@Override
	public boolean loadNationList() {
		return loadFlatFileListOfType(TownyDBFileType.NATION, uuid -> universe.newNationInternal(uuid));
	}

	@Override
	public boolean loadWorldList() {
		TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_server_world_list"));
		loadFlatFileListOfType(TownyDBFileType.WORLD, uuid -> universe.newWorldInternal(uuid));
		for (World world : Bukkit.getServer().getWorlds()) {
			if (universe.getWorldIDMap().containsKey(world.getUID()))
				continue;

			// Register and create files for any worlds which did not have files yet.
			TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
			universe.registerTownyWorld(townyWorld);
			File worldFile = new File(getFileOfTypeWithUUID(TownyDBFileType.WORLD, world.getUID()));
			if (!worldFile.exists())
				try {
					FileMgmt.mapToFile(ObjectSaveUtil.getWorldMap(townyWorld), Paths.get(getFileOfTypeWithUUID(TownyDBFileType.WORLD, townyWorld.getUUID())));
				} catch (Exception e) {
					logger.warn("Could not save new world file for TownyWorld: " + townyWorld.getUUID());
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
				String worldUUIDAsString = worldfolder.getName();
				UUID worldUUID; 
				try {
					worldUUID = UUID.fromString(worldUUIDAsString);
				} catch (IllegalArgumentException e) {
					plugin.getLogger().warning("World folder " + worldfolder + " in TownBlocks folder not readable...");
					continue;
				}
				if (BukkitTools.getWorld(worldUUID) == null) {
					TownyMessaging.sendErrorMsg("Your towny\\data\\townblocks\\ folder contains a folder named '"
							+ worldUUIDAsString + "' which doesn't correspond to a World UID on your Bukkit server!");
					TownyMessaging.sendErrorMsg("Towny is going to skip loading the townblocks found in this folder.");
					continue;
				}

				TownyWorld world = universe.getWorld(worldUUID);

				if (world == null) {
					World bukkitWorld = Bukkit.getWorld(worldUUID);
					if (bukkitWorld == null)
						continue;
					universe.newWorld(bukkitWorld);
					world = universe.getWorld(worldUUID);
				}
				File worldFolder = new File(dataFolderPath + File.separator + "townblocks" + File.separator + worldUUIDAsString);
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
				TownyMessaging
						.sendDebugMsg(Translation.of("flatfile_dbg_world_loaded_townblocks", worldUUIDAsString, total));
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
	public boolean loadJailUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.JAIL, uuids);
	}

	@Override
	public boolean loadPlotGroupUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.PLOTGROUP, uuids);
	}

	@Override
	public boolean loadResidentUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.RESIDENT, uuids);
	}

	@Override
	public boolean loadTownUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.TOWN, uuids);
	}

	@Override
	public boolean loadNationUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.NATION, uuids);
	}

	@Override
	public boolean loadWorldUUIDs(Set<UUID> uuids) throws ObjectCouldNotBeLoadedException {
		return loadFlatFilesOfType(TownyDBFileType.WORLD, uuids);
	}

	@Override
	public boolean loadTownBlocks(Collection<TownBlock> townBlocks) throws ObjectCouldNotBeLoadedException {
		for (TownBlock townBlock : townBlocks)
			if (!loadTownBlock(townBlock))
				throw new ObjectCouldNotBeLoadedException("The Townblock: '" + townBlock.toString() + "' could not be read from the database!");
		return true;
	}

	/*
	 * Return Loadable Objects as Maps for TownyDataBaseHandler to load. 
	 */

	@Override
	public Map<String, String> getJailMap(UUID uuid) {
		File jailFile = new File(getFileOfTypeWithUUID(TownyDBFileType.JAIL, uuid));
		if (jailFile.exists() && jailFile.isFile())
			return FileMgmt.loadFileIntoMap(jailFile);
		TownyMessaging.sendErrorMsg("Cannot find a jail file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getPlotGroupMap(UUID uuid) {
		File groupFile = new File(getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, uuid));
		if (groupFile.exists() && groupFile.isFile())
			return FileMgmt.loadFileIntoMap(groupFile);
		TownyMessaging.sendErrorMsg("Cannot find a plotgroup file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getResidentMap(UUID uuid) {
		File residentFile = new File(getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, uuid));
		if (residentFile.exists() && residentFile.isFile())
			return FileMgmt.loadFileIntoMap(residentFile);
		TownyMessaging.sendErrorMsg("Cannot find a resident file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getTownMap(UUID uuid) {
		File townFile = new File(getFileOfTypeWithUUID(TownyDBFileType.TOWN, uuid));
		if (townFile.exists() && townFile.isFile())
			return FileMgmt.loadFileIntoMap(townFile);
		TownyMessaging.sendErrorMsg("Cannot find a town file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getNationMap(UUID uuid) {
		File nationFile = new File(getFileOfTypeWithUUID(TownyDBFileType.NATION, uuid));
		if (nationFile.exists() && nationFile.isFile())
			return FileMgmt.loadFileIntoMap(nationFile);
		TownyMessaging.sendErrorMsg("Cannot find a nation file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getWorldMap(UUID uuid) {
		File worldFile = new File(getFileOfTypeWithUUID(TownyDBFileType.WORLD, uuid));
		if (worldFile.exists() && worldFile.isFile())
			return FileMgmt.loadFileIntoMap(worldFile);
		TownyMessaging.sendErrorMsg("Cannot find a world file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public Map<String, String> getTownBlockMap(TownBlock townBlock) {
		File fileTownBlock = new File(getTownBlockFilename(townBlock));
		if (fileTownBlock.exists() && fileTownBlock.isFile())
			return FileMgmt.loadFileIntoMap(fileTownBlock);	
		TownyMessaging.sendErrorMsg("Cannot find a townBlock file for " + townBlock.toString() + "!");
		return null;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveJail(Jail jail, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Jail unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean savePlotGroup(PlotGroup group, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Town unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveResident(Resident resident, Map<String, Object> data) {
		/*
		 * Make sure we only save in async
		 */
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, resident.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveHibernatedResident(UUID uuid, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid)));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Hibernated Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveTown(Town town, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.TOWN, town.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Town unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveNation(Nation nation, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.NATION, nation.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Nation unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveWorld(TownyWorld world, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.WORLD, world.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save World unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock, Map<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getTownBlockFilename(townBlock)));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save TownBlock unknown error " + e.getMessage());
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
	public void deleteJail(Jail jail) {
		deleteFileByTypeAndUUID(TownyDBFileType.JAIL, jail.getUUID());
	}


	/*
	 * Misc Methods
	 */
	
	@Override
	public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			File hibernatedFile = new File(getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid));

			if (!hibernatedFile.exists())
				return Optional.empty();

			Map<String, String> keys = FileMgmt.loadFileIntoMap(hibernatedFile);
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

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param resident Resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getResidentFilename(Resident resident) {
		return getFileOfTypeWithName(TownyDBFileType.RESIDENT, resident.getName());
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param uuid UUID of the hibernated resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getHibernatedResidentFilename(UUID uuid) {
		return getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid);
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param town Town whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getTownFilename(Town town) {
		return getFileOfTypeWithName(TownyDBFileType.TOWN, town.getName());
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param nation Nation whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getNationFilename(Nation nation) {
		return getFileOfTypeWithName(TownyDBFileType.NATION, nation.getName());
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param world TownyWorld whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getWorldFilename(TownyWorld world) {
		return getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName());
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param group PlotGroup whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getPlotGroupFilename(PlotGroup group) {
		return getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getID());
	}

	/**
	 * @deprecated as of 0.99.2.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param jail Jail whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getJailFilename(Jail jail) {
		return getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID());
	}

	/*
	 * EVERYTHING BELOW HERE DOESN'T EXIST IN UUID BRANCH
	 */

	@SuppressWarnings("ReadWriteStringCanBeUsed")
	@Override
	public boolean loadCooldowns() {
		final Path cooldownsFile = Paths.get(dataFolderPath).resolve("cooldowns.json");
		if (!Files.exists(cooldownsFile))
			return true;

		final String data;
		try {
			data = new String(Files.readAllBytes(cooldownsFile), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("An exception occurred when reading cooldowns.json", e);
			return true;
		}
		
		try {
			CooldownTimerTask.getCooldowns().putAll(new Gson().fromJson(data, new TypeToken<Map<String, Long>>(){}.getType()));
		} catch (JsonSyntaxException e) {
			logger.warn("Could not load saved cooldowns due to a json syntax exception", e);
		}
		
		return true;
	}

	@SuppressWarnings("ReadWriteStringCanBeUsed")
	@Override
	public boolean saveCooldowns() {
		final JsonObject object = new JsonObject();
		
		for (Map.Entry<String, Long> cooldown : CooldownTimerTask.getCooldowns().entrySet())
			object.addProperty(cooldown.getKey(), cooldown.getValue());
		
		this.queryQueue.add(() -> {
			try {
				Files.write(Paths.get(dataFolderPath).resolve("cooldowns.json"), new GsonBuilder().setPrettyPrinting().create().toJson(object).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				logger.warn("An exception occurred when writing cooldowns.json", e);
			}
		});
		
		return true;
	}
}
