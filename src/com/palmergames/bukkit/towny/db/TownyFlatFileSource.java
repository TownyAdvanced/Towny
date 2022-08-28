package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.tasks.DeleteFileTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import org.bukkit.Bukkit;
import org.bukkit.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TownyFlatFileSource extends TownyDatabaseHandler {

	public TownyFlatFileSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		// Create files and folders if non-existent
		if (!FileMgmt.checkOrCreateFolders(rootFolderPath, dataFolderPath,
				dataFolderPath + File.separator + "residents",
				dataFolderPath + File.separator + "residents" + File.separator + "deleted",
				dataFolderPath + File.separator + "residents" + File.separator + "hibernated",
				dataFolderPath + File.separator + "towns",
				dataFolderPath + File.separator + "towns" + File.separator + "deleted",
				dataFolderPath + File.separator + "nations",
				dataFolderPath + File.separator + "nations" + File.separator + "deleted",
				dataFolderPath + File.separator + "worlds",
				dataFolderPath + File.separator + "worlds" + File.separator + "deleted",
				dataFolderPath + File.separator + "townblocks", dataFolderPath + File.separator + "plotgroups",
				dataFolderPath + File.separator + "plotgroups" + File.separator + "deleted",
				dataFolderPath + File.separator + "jails",
				dataFolderPath + File.separator + "jails" + File.separator + "deleted")) {
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
		ALLIANCE("alliances", ".txt"), NATION("nations", ".txt"), TOWN("towns", ".txt"), RESIDENT("residents", ".txt"),
		HIBERNATED_RESIDENT("residents" + File.separator + "hibernated", ".txt"), JAIL("jails", ".txt"),
		WORLD("worlds", ".txt"), TOWNBLOCK("townblocks", ".data"), PLOTGROUP("plotgroups", ".data");

		private String folderName;
		private String fileExtension;

		TownyDBFileType(String folderName, String fileExtension) {
			this.folderName = folderName;
			this.fileExtension = fileExtension;
		}

		private String getSingular() {
			// Hibernated Residents are never loaded so this method is never called on them.
			return folderName.substring(folderName.length() - 1);
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

		int convertedFiles = 0;
		for (File file : files)
			try {
				// Send our UUID to the consumer.
				consumer.accept(UUID.fromString(file.getName().replace(type.fileExtension, "")));
			} catch (IllegalArgumentException ignored) {
				/* A file which isn't a UUID was found, likely an old Database file. */
				UUID uuid = TownyLegacyFlatFileConverter.getUUID(file);
				if (uuid == null) {
					plugin.getLogger().warning("No UUID could be found in the " + type.folderName + "\\" + file.getName() 
						+ " file! This file will not be loaded into the TownyUniverse!");
					continue;
				}
				convertedFiles++;
				// Rename and delete the legacy file.
				renameLegacyFile(file, type, uuid);
				// Send our recovered UUID to the consumer.
				consumer.accept(uuid);

			}

		if (convertedFiles > 0)
			plugin.getLogger().info("Towny converted " + convertedFiles + " files from legacy to UUID format in the " + type.folderName + " folder.");
		return true;
	}

	private boolean loadFlatFilesOfType(TownyDBFileType type, Set<UUID> uuids) {
		for (UUID uuid : uuids) {
			if (!loadFile(type, uuid)) {
				plugin.getLogger().severe(type.getLoadErrorMsg(uuid));
				return false;
			}
		}
		return true;
	}

	private boolean loadFile(TownyDBFileType type, UUID uuid) {
		return switch (type) {
		case JAIL -> loadJailData(uuid);
		case NATION -> loadNationData(uuid);
		case PLOTGROUP -> loadPlotGroupData(uuid);
		case RESIDENT -> loadResidentData(uuid);
		case TOWN -> loadTownData(uuid);
		case TOWNBLOCK -> throw new UnsupportedOperationException("Unimplemented case: " + type);
		case WORLD -> loadWorldData(uuid);
		default -> throw new IllegalArgumentException("Unexpected value: " + type);
		};
	}

	public String getNameOfObject(String type, UUID uuid) {
		File file = new File(getFileOfTypeWithUUID(TownyDBFileType.valueOf(type.toUpperCase(Locale.ROOT)), uuid));
		if (file.exists() && file.isFile()) {
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
				Properties properties = new Properties();
				properties.load(isr);
				return properties.getProperty("name");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
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
		if (plugin != null) {
			TownyMessaging.sendDebugMsg(Translation.of("flatfile_dbg_loading_server_world_list"));
			for (World world : plugin.getServer().getWorlds()) {
				universe.newWorld(world);
			}
		}

		return loadFlatFileListOfType(TownyDBFileType.WORLD, uuid -> universe.newWorldInternal(uuid));
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
					World world = BukkitTools.getWorld(worldfolder.getName());
					worldUUID = world.getUID();
					renameLegacyFolder(worldfolder, TownyDBFileType.TOWNBLOCK, worldUUID);
					worldUUIDAsString = worldUUID.toString();
				}
				TownyWorld world = universe.getWorld(worldUUID);
				if (world == null) {
					World bukkitWorld = Bukkit.getWorld(worldUUID);
					if (bukkitWorld == null)
						continue;
					universe.newWorld(bukkitWorld);
					world = universe.getWorld(worldUUID);
				}
				File worldFolder = new File(
						dataFolderPath + File.separator + "townblocks" + File.separator + worldUUIDAsString);
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
	public boolean loadJails() {
		return loadFlatFilesOfType(TownyDBFileType.JAIL, universe.getJailUUIDs());
	}

	@Override
	public boolean loadPlotGroups() {
		return loadFlatFilesOfType(TownyDBFileType.PLOTGROUP, universe.getPlotGroupUUIDs());
	}

	@Override
	public boolean loadResidents() {
		return loadFlatFilesOfType(TownyDBFileType.RESIDENT, universe.getResidentUUIDs());
	}

	@Override
	public boolean loadTowns() {
		return loadFlatFilesOfType(TownyDBFileType.TOWN, universe.getTownUUIDs());
	}

	@Override
	public boolean loadNations() {
		return loadFlatFilesOfType(TownyDBFileType.NATION, universe.getNationUUIDs());
	}

	@Override
	public boolean loadWorlds() {
		return loadFlatFilesOfType(TownyDBFileType.WORLD, universe.getWorldUUIDs());
	}

	@Override
	public boolean loadTownBlocks() {

		String line = "";
		String path;

		for (TownBlock townBlock : universe.getTownBlocks().values()) {
			path = getTownBlockFilename(townBlock);

			File fileTownBlock = new File(path);
			if (fileTownBlock.exists() && fileTownBlock.isFile()) {

				try {
					HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(fileTownBlock);

					line = keys.get("town");
					if (line != null) {
						if (line.isEmpty()) {
							TownyMessaging.sendErrorMsg(
									Translation.of("flatfile_err_townblock_file_missing_town_delete", path));
							universe.removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}
						Town town = null;
						try {
							town = universe.getTown(UUID.fromString(line.trim()));
						} catch (IllegalArgumentException e) {
							// This is a legacy file opening a townblock that records the Town by the Town's name instead of UUID.
							town = universe.getTown(line.trim());
						}
//						else if (universe.getReplacementNameMap().containsKey(line.trim()))
//							town = universe.getTown(universe.getReplacementNameMap().get(line).trim());

						if (town == null) {
							TownyMessaging.sendErrorMsg(Translation
									.of("flatfile_err_townblock_file_contains_unregistered_town_delete", line, path));
							universe.removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}

						townBlock.setTown(town, false);
						try {
							town.addTownBlock(townBlock);
							TownyWorld townyWorld = townBlock.getWorld();
							if (townyWorld != null && !townyWorld.hasTown(town))
								townyWorld.addTown(town);
						} catch (AlreadyRegisteredException ignored) {
						}
					} else {
						// Town line is null, townblock is invalid.
						TownyMessaging
								.sendErrorMsg(Translation.of("flatfile_err_townblock_file_missing_town_delete", path));
						universe.removeTownBlock(townBlock);
						deleteTownBlock(townBlock);
						continue;
					}

					line = keys.get("name");
					if (line != null)
						try {
							townBlock.setName(line.trim());
						} catch (Exception ignored) {
						}

					line = keys.get("type");
					if (line != null)
						townBlock.setType(TownBlockTypeHandler.getTypeInternal(line));

					line = keys.get("resident");
					if (line != null && !line.isEmpty()) {
						Resident res = universe.getResident(line.trim());
						if (res != null) {
							townBlock.setResident(res, false);
						} else {
							TownyMessaging.sendErrorMsg(
									Translation.of("flatfile_err_invalid_townblock_resident", townBlock.toString()));
						}
					}

					line = keys.get("price");
					if (line != null)
						try {
							townBlock.setPlotPrice(Double.parseDouble(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("outpost");
					if (line != null)
						try {
							townBlock.setOutpost(Boolean.parseBoolean(line));
						} catch (Exception ignored) {
						}

					line = keys.get("permissions");
					if ((line != null) && !line.isEmpty())
						try {
							townBlock.setPermissions(line.trim());
						} catch (Exception ignored) {
						}

					line = keys.get("changed");
					if (line != null)
						try {
							townBlock.setChanged(Boolean.parseBoolean(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("locked");
					if (line != null)
						try {
							townBlock.setLocked(Boolean.parseBoolean(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("claimedAt");
					if (line != null)
						try {
							townBlock.setClaimedAt(Long.parseLong(line));
						} catch (Exception ignored) {
						}

					line = keys.get("metadata");
					if (line != null && !line.isEmpty())
						MetadataLoader.getInstance().deserializeMetadata(townBlock, line.trim());

					line = keys.get("groupID");
					UUID groupID = null;
					if (line != null && !line.isEmpty()) {
						groupID = UUID.fromString(line.trim());
					}

					if (groupID != null) {
						PlotGroup group = universe.getGroup(groupID);
						if (group != null) {
							townBlock.setPlotObjectGroup(group);
							if (group.getPermissions() == null && townBlock.getPermissions() != null)
								group.setPermissions(townBlock.getPermissions());
							if (townBlock.hasResident())
								group.setResident(townBlock.getResidentOrNull());
						} else {
							townBlock.removePlotObjectGroup();
						}
					}

					line = keys.get("trustedResidents");
					if (line != null && !line.isEmpty() && townBlock.getTrustedResidents().isEmpty()) {
						townBlock.addTrustedResidents(TownyAPI.getInstance().getResidents(ObjectLoadUtil.toUUIDArray(line.split(","))));

						if (townBlock.hasPlotObjectGroup()
								&& townBlock.getPlotObjectGroup().getTrustedResidents().isEmpty()
								&& townBlock.getTrustedResidents().size() > 0)
							townBlock.getPlotObjectGroup().setTrustedResidents(townBlock.getTrustedResidents());
					}

					line = keys.get("customPermissionData");
					if (line != null && !line.isEmpty() && townBlock.getPermissionOverrides().isEmpty()) {
						Map<String, String> map = new Gson().fromJson(line, Map.class);

						for (Map.Entry<String, String> entry : map.entrySet()) {
							Resident resident;
							try {
								resident = TownyAPI.getInstance().getResident(UUID.fromString(entry.getKey()));
							} catch (IllegalArgumentException e) {
								continue;
							}

							if (resident == null)
								continue;

							townBlock.getPermissionOverrides().put(resident, new PermissionData(entry.getValue()));
						}

						if (townBlock.hasPlotObjectGroup()
								&& townBlock.getPlotObjectGroup().getPermissionOverrides().isEmpty()
								&& townBlock.getPermissionOverrides().size() > 0)
							townBlock.getPlotObjectGroup().setPermissionOverrides(townBlock.getPermissionOverrides());
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(
							Translation.of("flatfile_err_exception_reading_townblock_file_at_line", path, line));
					return false;
				} finally {
					saveTownBlock(townBlock);
				}

			} else {
				TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_unknown_err", path));
				universe.removeTownBlock(townBlock);
				deleteTownBlock(townBlock);
			}
		}

		return true;
	}

	/*
	 * Return Loadable Objects as HashMaps for TownyDataBaseHandler to load. 
	 */

	@Override
	public HashMap<String, String> getJailMap(UUID uuid) {
		File jailFile = new File(getFileOfTypeWithUUID(TownyDBFileType.JAIL, uuid));
		if (jailFile.exists() && jailFile.isFile())
			return FileMgmt.loadFileIntoHashMap(jailFile);
		TownyMessaging.sendErrorMsg("Cannot find a jail file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public HashMap<String, String> getPlotGroupMap(UUID uuid) {
		File groupFile = new File(getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, uuid));
		if (groupFile.exists() && groupFile.isFile())
			return FileMgmt.loadFileIntoHashMap(groupFile);
		TownyMessaging.sendErrorMsg("Cannot find a plotgroup file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public HashMap<String, String> getResidentMap(UUID uuid) {
		File residentFile = new File(getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, uuid));
		if (residentFile.exists() && residentFile.isFile())
			return FileMgmt.loadFileIntoHashMap(residentFile);
		TownyMessaging.sendErrorMsg("Cannot find a resident file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public HashMap<String, String> getTownMap(UUID uuid) {
		File townFile = new File(getFileOfTypeWithUUID(TownyDBFileType.TOWN, uuid));
		if (townFile.exists() && townFile.isFile())
			return FileMgmt.loadFileIntoHashMap(townFile);
		TownyMessaging.sendErrorMsg("Cannot find a town file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public HashMap<String, String> getNationMap(UUID uuid) {
		File nationFile = new File(getFileOfTypeWithUUID(TownyDBFileType.NATION, uuid));
		if (nationFile.exists() && nationFile.isFile())
			return FileMgmt.loadFileIntoHashMap(nationFile);
		TownyMessaging.sendErrorMsg("Cannot find a nation file with the UUID " + uuid.toString() + "!");
		return null;
	}

	@Override
	public HashMap<String, String> getWorldMap(UUID uuid) {
		File worldFile = new File(getFileOfTypeWithUUID(TownyDBFileType.WORLD, uuid));
		if (worldFile.exists() && worldFile.isFile())
			return FileMgmt.loadFileIntoHashMap(worldFile);
		TownyMessaging.sendErrorMsg("Cannot find a world file with the UUID " + uuid.toString() + "!");
		return null;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveJail(Jail jail, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Jail unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean savePlotGroup(PlotGroup group, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Town unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveResident(Resident resident, HashMap<String, Object> data) {
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
	public boolean saveHibernatedResident(UUID uuid, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid)));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Hibernated Resident unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveTown(Town town, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.TOWN, town.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Town unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveNation(Nation nation, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.NATION, nation.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save Nation unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveWorld(TownyWorld world, HashMap<String, Object> data) {
		try {
			this.queryQueue.add(new FlatFileSaveTask(data, getFileOfTypeWithUUID(TownyDBFileType.WORLD, world.getUUID())));
			return true;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("FlatFile: Save World unknown error " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock, HashMap<String, Object> data) {
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
	private void deleteFileByTypeAndName(TownyDBFileType type, String name) {
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
	 * Legacy DB Methods
	 */

	private void renameLegacyFile(File file, TownyDBFileType type, UUID uuid) {
		File newFile = new File(dataFolderPath + File.separator + type.folderName + File.separator + uuid.toString() + type.fileExtension);
		boolean delete = false;
		String fileName = file.getName().replace(type.fileExtension, "");
		if (newFile.exists()) {
			plugin.getLogger().warning(type.folderName + "\\" +  file.getName() + " could not be saved in UUID format because a file with the UUID " + uuid.toString() + " already exists! The non-UUID formatted file will be removed.");
			delete = true;
		} else {
			delete = file.renameTo(newFile);
			TownyLegacyFlatFileConverter.applyName(newFile, fileName);
		}
		if (delete)
			deleteFileByTypeAndName(type, fileName);
	}
	
	private void renameLegacyFolder(File folder, TownyDBFileType type, UUID uuid) {
		File newFile = new File(dataFolderPath + File.separator + type.folderName + File.separator + uuid.toString());
		boolean delete = false;
		if (newFile.exists()) {
			plugin.getLogger().warning(type.folderName + "\\" +  folder.getName() + " folder could not be saved in UUID format because a folder with the UUID " + uuid.toString() + " already exists! The non-UUID formatted folder will be removed.");
			delete = true;
		} else {
			delete = folder.renameTo(newFile);
		}
		if (delete)
			folder.delete();
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

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param resident Resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getResidentFilename(Resident resident) {
		return getFileOfTypeWithName(TownyDBFileType.RESIDENT, resident.getName());
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param uuid UUID of the hibernated resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getHibernatedResidentFilename(UUID uuid) {
		return getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid);
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param town Town whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getTownFilename(Town town) {
		return getFileOfTypeWithName(TownyDBFileType.TOWN, town.getName());
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param nation Nation whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getNationFilename(Nation nation) {
		return getFileOfTypeWithName(TownyDBFileType.NATION, nation.getName());
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param world TownyWorld whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getWorldFilename(TownyWorld world) {
		return getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName());
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param group PlotGroup whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getPlotGroupFilename(PlotGroup group) {
		return getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getID());
	}

	/**
	 * @deprecated as of 0.98.4.0, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param jail Jail whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getJailFilename(Jail jail) {
		return getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID());
	}

}
