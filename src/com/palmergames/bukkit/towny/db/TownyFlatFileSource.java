package com.palmergames.bukkit.towny.db;

import com.google.gson.Gson;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
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
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TownyFlatFileSource extends TownyDatabaseHandler {

	private final String newLine = System.getProperty("line.separator");
	
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
		) || !FileMgmt.checkOrCreateFiles(
			dataFolderPath + File.separator + "worlds.txt"
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
		ALLIANCE("alliances", ".txt"),
		NATION("nations", ".txt"),
		TOWN("towns", ".txt"),
		RESIDENT("residents", ".txt"),
		HIBERNATED_RESIDENT("residents" + File.separator + "hibernated", ".txt"),
		JAIL("jails", ".txt"),
		WORLD("worlds", ".txt"),
		TOWNBLOCK("townblocks", ".data"),
		PLOTGROUP("plotgroups", ".data");
		
		private String folderName;
		private String fileExtension;

		TownyDBFileType(String folderName, String fileExtension) {
			this.folderName = folderName;
			this.fileExtension = fileExtension;
		}
		
		private String getSingular() {
			// Hibernated Residents are never loaded so this method is never called on them.
			return folderName.substring(folderName.length()-1);
		}
		
		public String getSaveLocation(String fileName) {
			return Towny.getPlugin().getDataFolder().getPath() + File.separator + "data" + File.separator + folderName + File.separator + fileName + fileExtension;
		}
		
		public String getLoadErrorMsg(UUID uuid) {
			return "Loading Error: Could not read the " + getSingular() + " with UUID '" + uuid + "' from the " + folderName + " folder.";
		}
	}

	public String getFileOfTypeWithUUID(TownyDBFileType type, UUID uuid) {
		return dataFolderPath + File.separator + type.folderName + File.separator + uuid + type.fileExtension;
	}
	
	public String getFileOfTypeWithName(TownyDBFileType type, String name) {
		return dataFolderPath + File.separator + type.folderName + File.separator + name + type.fileExtension;
	}
	
	public boolean loadFlatFileListOfType(TownyDBFileType type, Consumer<UUID> consumer) {
		TownyMessaging.sendDebugMsg("Searching for " + type.folderName + "...");
		File[] files = new File(dataFolderPath + File.separator + type.folderName)
				.listFiles(file -> file.getName().toLowerCase().endsWith(type.fileExtension));

		if (files.length != 0)
			TownyMessaging.sendDebugMsg("Loading " + files.length + " entries from the " + type.folderName + " folder...");

		for (File file : files)
			consumer.accept(UUID.fromString(file.getName().replace(type.fileExtension, "")));

		return true;
	}
	
	public boolean loadFlatFilesOfType(TownyDBFileType type, Set<UUID> uuids) {
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
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
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
				String worldName = worldfolder.getName();
				UUID worldUUID = UUID.fromString(worldName);
				TownyWorld world = universe.getWorld(worldUUID);
				if (world == null) {
					World bukkitWorld = Bukkit.getWorld(worldUUID);
					if (bukkitWorld == null)
						continue;
					newWorld(bukkitWorld);
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
							TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_missing_town_delete", path));
							universe.removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}
						Town town = null;
						if (universe.hasTown(line.trim()))
							town = universe.getTown(line.trim());
//						else if (universe.getReplacementNameMap().containsKey(line.trim()))
//							town = universe.getTown(universe.getReplacementNameMap().get(line).trim());
						
						if (town == null) {
							TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_contains_unregistered_town_delete", line, path));
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
						TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_missing_town_delete", path));
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
						}
						else {
							TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_invalid_townblock_resident", townBlock.toString()));
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
						} catch (Exception ignored) {}
					
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
						for (Resident resident : TownyAPI.getInstance().getResidents(toUUIDArray(line.split(","))))
							townBlock.addTrustedResident(resident);
						
						if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getTrustedResidents().isEmpty() && townBlock.getTrustedResidents().size() > 0)
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
						
						if (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPermissionOverrides().isEmpty() && townBlock.getPermissionOverrides().size() > 0)
							townBlock.getPlotObjectGroup().setPermissionOverrides(townBlock.getPermissionOverrides());
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_exception_reading_townblock_file_at_line", path, line));
					return false;
				}

			} else {
				TownyMessaging.sendErrorMsg(Translation.of("flatfile_err_townblock_file_unknown_err", path));
				universe.removeTownBlock(townBlock);
				deleteTownBlock(townBlock);
			}
		}
		
		return true;
	}

	// TODO: bring the loadObject methods from TownyDataSource and into the FlatFile and SQL sources.

	/*
	 * Load individual towny objects
	 */

	@Override
	public boolean loadJailData(UUID uuid) {
		File jailFile = new File(getFileOfTypeWithUUID(TownyDBFileType.JAIL, uuid));
		if (jailFile.exists() && jailFile.isFile()) {
			Jail jail = TownyUniverse.getInstance().getJail(uuid);
			if (jail == null) {
				TownyMessaging.sendErrorMsg("Cannot find a jail with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(jailFile);
			
			return loadJail(jail, keys);
		}
		return true;
	}
	
	@Override
	public boolean loadPlotGroupData(UUID uuid) {
		File plotGroupFile = new File(getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, uuid));
		if (plotGroupFile.exists() && plotGroupFile.isFile()) {
			PlotGroup plotGroup = TownyUniverse.getInstance().getGroup(uuid);
			if (plotGroup  == null) {
				TownyMessaging.sendErrorMsg("Cannot find a plotgroup with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(plotGroupFile);
			
			return loadPlotGroup(plotGroup , keys);
		}
		return true;
	}
	
	@Override
	public boolean loadResidentData(UUID uuid) {
		File residentFile = new File(getFileOfTypeWithUUID(TownyDBFileType.RESIDENT, uuid));
		if (residentFile.exists() && residentFile.isFile()) {
			Resident resident = TownyUniverse.getInstance().getResident(uuid);
			if (resident == null) {
				TownyMessaging.sendErrorMsg("Cannot find a resident with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(residentFile);
			
			return loadResident(resident, keys); 
		}
		return true;
	}
	
	@Override
	public boolean loadTownData(UUID uuid) {
		File townFile = new File(getFileOfTypeWithUUID(TownyDBFileType.TOWN, uuid));
		if (townFile.exists() && townFile.isFile()) {
			Town town = TownyUniverse.getInstance().getTown(uuid);
			if (town == null) {
				TownyMessaging.sendErrorMsg("Cannot find a town with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(townFile);
			
			return loadTown(town, keys); 
		}
		return true;
	}
	
	@Override
	public boolean loadNationData(UUID uuid) {
		File nationFile = new File(getFileOfTypeWithUUID(TownyDBFileType.NATION, uuid));
		if (nationFile.exists() && nationFile.isFile()) {
			Nation nation = TownyUniverse.getInstance().getNation(uuid);
			if (nation == null) {
				TownyMessaging.sendErrorMsg("Cannot find a nation with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(nationFile);
			
			return loadNation(nation, keys); 
		}
		return true;
	}

	@Override
	public boolean loadWorldData(UUID uuid) {
		File worldFile = new File(getFileOfTypeWithUUID(TownyDBFileType.WORLD, uuid));
		if (worldFile.exists() && worldFile.isFile()) {
			TownyWorld world = TownyUniverse.getInstance().getWorld(uuid);
			if (world == null) {
				TownyMessaging.sendErrorMsg("Cannot find a world with the UUID " + uuid.toString() + " in the TownyUniverse.");
				return false; 
			}
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(worldFile);
			
			return loadWorld(world, keys); 
		}
		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveWorldList() {

		List<String> list = new ArrayList<>();

		for (TownyWorld world : universe.getTownyWorlds()) {

			list.add(world.getName());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, dataFolderPath + File.separator + "worlds.txt"));

		return true;

	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {

		List<String> list = new ArrayList<>();

		if (resident.hasUUID()) {
			list.add("uuid=" + resident.getUUID());
		}
		// Last Online
		list.add("lastOnline=" + resident.getLastOnline());
		// Registered
		list.add("registered=" + resident.getRegistered());
		// Joined Town At
		list.add("joinedTownAt=" + resident.getJoinedTownAt());
		// isNPC
		list.add("isNPC=" + resident.isNPC());
		
		// if they are jailed:
		if (resident.isJailed()) {
			// jail uuid
			list.add("jail=" + resident.getJail().getUUID());
			// jailCell
			list.add("jailCell=" + resident.getJailCell());
			// jailHours
			list.add("jailHours=" + resident.getJailHours());
		}
		
		// title
		list.add("title=" + resident.getTitle());
		// surname
		list.add("surname=" + resident.getSurname());

		if (resident.hasTown()) {
			try {
				list.add("town=" + resident.getTown().getName());
			} catch (NotRegisteredException ignored) {
			}
			list.add("town-ranks=" + StringMgmt.join(resident.getTownRanks(), ","));
			list.add("nation-ranks=" + StringMgmt.join(resident.getNationRanks(), ","));
		}

		// Friends
		list.add("friends=" + StringMgmt.join(resident.getFriends(), ","));
		list.add("");

		// Plot Protection
		list.add("protectionStatus=" + resident.getPermissions().toString());

		// Metadata
		list.add("metadata=" + serializeMetadata(resident));
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getResidentFilename(resident)));

		return true;

	}
	
	@Override
	public boolean saveHibernatedResident(UUID uuid, long registered) {
		List<String> list = new ArrayList<>();
		list.add("registered=" + registered);
		this.queryQueue.add(new FlatFileSaveTask(list, getHibernatedResidentFilename(uuid)));
		return true;
	}

	@Override
	public boolean saveTown(Town town) {

		List<String> list = new ArrayList<>();

		// Name
		list.add("name=" + town.getName());
		// Mayor
		if (town.hasMayor())
			list.add("mayor=" + town.getMayor().getName());
		// Nation
		if (town.hasNation())
			try {
				list.add("nation=" + town.getNation().getName());
			} catch (NotRegisteredException ignored) {
			}

		// Assistants
		list.add("assistants=" + StringMgmt.join(town.getRank("assistant"), ","));

		list.add(newLine);
		// Town Board
		list.add("townBoard=" + town.getBoard());
		// tag
		list.add("tag=" + town.getTag());
		// Town Protection
		list.add("protectionStatus=" + town.getPermissions().toString());
		// Bonus Blocks
		list.add("bonusBlocks=" + town.getBonusBlocks());
		// Purchased Blocks
		list.add("purchasedBlocks=" + town.getPurchasedBlocks());
		// Taxpercent
		list.add("taxpercent=" + town.isTaxPercentage());
		// Taxpercent Cap
		list.add("maxPercentTaxAmount=" + town.getMaxPercentTaxAmount());
		// Taxes
		list.add("taxes=" + town.getTaxes());
		// Plot Price
		list.add("plotPrice=" + town.getPlotPrice());
		// Plot Tax
		list.add("plotTax=" + town.getPlotTax());
		// Commercial Plot Price
		list.add("commercialPlotPrice=" + town.getCommercialPlotPrice());
		// Commercial Tax
		list.add("commercialPlotTax=" + town.getCommercialPlotTax());
		// Embassy Plot Price
		list.add("embassyPlotPrice=" + town.getEmbassyPlotPrice());
		// Embassy Tax
		list.add("embassyPlotTax=" + town.getEmbassyPlotTax());
		// Town Spawn Cost
		list.add("spawnCost=" + town.getSpawnCost());
		// Upkeep
		list.add("hasUpkeep=" + town.hasUpkeep());
		// UnlimitedClaims
		list.add("hasUnlimitedClaims=" + town.hasUnlimitedClaims());
		// Open
		list.add("open=" + town.isOpen());
		// PVP
		list.add("adminDisabledPvP=" + town.isAdminDisabledPVP());
		list.add("adminEnabledPvP=" + town.isAdminEnabledPVP());
		// Public
		list.add("public=" + town.isPublic());
		// Conquered towns setting + date
		list.add("conquered=" + town.isConquered());
		list.add("conqueredDays=" + town.getConqueredDays());
		if (town.hasValidUUID()){
			list.add("uuid=" + town.getUUID());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + town.getRegistered());
		list.add("joinedNationAt=" + town.getJoinedNationAt());
		list.add("movedHomeBlockAt=" + town.getMovedHomeBlockAt());
        
        // Home Block
		if (town.hasHomeBlock())
			try {
				list.add("homeBlock=" + town.getHomeBlock().getWorld().getName() + "," + town.getHomeBlock().getX() + "," + town.getHomeBlock().getZ());
			} catch (TownyException ignored) {
			}

		// Spawn
		if (town.hasSpawn())
			try {
				list.add("spawn=" + town.getSpawn().getWorld().getName() + "," + town.getSpawn().getX() + "," + town.getSpawn().getY() + "," + town.getSpawn().getZ() + "," + town.getSpawn().getPitch() + "," + town.getSpawn().getYaw());
			} catch (TownyException ignored) {
			}

		// Outpost Spawns
		StringBuilder outpostArray = new StringBuilder("outpostspawns=");
		if (town.hasOutpostSpawn())
			for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns())) {
				outpostArray.append(spawn.getWorld().getName()).append(",").append(spawn.getX()).append(",").append(spawn.getY()).append(",").append(spawn.getZ()).append(",").append(spawn.getPitch()).append(",").append(spawn.getYaw()).append(";");
			}
		list.add(outpostArray.toString());

		// Outlaws
		list.add("outlaws=" + StringMgmt.join(town.getOutlaws(), ","));

		// Metadata
		list.add("metadata=" + serializeMetadata(town));
		
		// ManualTownLevel
		list.add("manualTownLevel=" + town.getManualTownLevel());
		
		list.add("ruined=" + town.isRuined());
		list.add("ruinedTime=" + town.getRuinedTime());
		// Peaceful
		list.add("neutral=" + town.isNeutral());
		
		// Debt balance
		list.add("debtBalance=" + town.getDebtBalance());

		// Primary Jail
		if (town.getPrimaryJail() != null)
			list.add("primaryJail=" + town.getPrimaryJail().getUUID());
		
		list.add("trustedResidents=" + StringMgmt.join(toUUIDList(town.getTrustedResidents()), ","));
		
		list.add("mapColorHexCode=" + town.getMapColorHexCode());
		list.add("nationZoneOverride=" + town.getNationZoneOverride());
		list.add("nationZoneEnabled=" + town.isNationZoneEnabled());
		list.add("allies=" + StringMgmt.join(town.getAlliesUUIDs(), ","));
		list.add("enemies=" + StringMgmt.join(town.getEnemiesUUIDs(), ","));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getTownFilename(town)));

		return true;

	}
	
	@Override
	public boolean savePlotGroup(PlotGroup group) {
		
		List<String> list = new ArrayList<>();
		
		// Group Name
		list.add("groupName=" + group.getName());
		
		// Group Price
		list.add("groupPrice=" + group.getPrice());
		
		// Town
		list.add("town=" + group.getTown().toString());
		
		// Save file
		this.queryQueue.add(new FlatFileSaveTask(list, getPlotGroupFilename(group)));
		
		return true;
	}

	@Override
	public boolean saveNation(Nation nation) {

		List<String> list = new ArrayList<>();

		if (nation.hasCapital())
			list.add("capital=" + nation.getCapital().getName());

		list.add("nationBoard=" + nation.getBoard());

		list.add("mapColorHexCode=" + nation.getMapColorHexCode());

		if (nation.hasTag())
			list.add("tag=" + nation.getTag());

		list.add("allies=" + StringMgmt.join(nation.getAllies(), ","));

		list.add("enemies=" + StringMgmt.join(nation.getEnemies(), ","));

		// Taxes
		list.add("taxes=" + nation.getTaxes());
		// Nation Spawn Cost
		list.add("spawnCost=" + nation.getSpawnCost());
		// Peaceful
		list.add("neutral=" + nation.isNeutral());
		if (nation.hasValidUUID()){
			list.add("uuid=" + nation.getUUID());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + nation.getRegistered());
        
        // Spawn
		if (nation.hasSpawn()) {
			try {
				list.add("nationSpawn=" + nation.getSpawn().getWorld().getName() + "," + nation.getSpawn().getX() + "," + nation.getSpawn().getY() + "," + nation.getSpawn().getZ() + "," + nation.getSpawn().getPitch() + "," + nation.getSpawn().getYaw());
			} catch (TownyException ignored) { }
		}

		list.add("isPublic=" + nation.isPublic());
		
		list.add("isOpen=" + nation.isOpen());

		// Metadata
		list.add("metadata=" + serializeMetadata(nation));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getNationFilename(nation)));

		return true;

	}

	@Override
	public boolean saveWorld(TownyWorld world) {

		List<String> list = new ArrayList<>();

		// PvP
		list.add("pvp=" + world.isPVP());
		// Force PvP
		list.add("forcepvp=" + world.isForcePVP());
		// FriendlyFire 
		list.add("friendlyFire=" + world.isFriendlyFireEnabled());		
		// Claimable
		list.add("# Can players found towns and claim plots in this world?");
		list.add("claimable=" + world.isClaimable());
		// has monster spawns
		list.add("worldmobs=" + world.hasWorldMobs());
		// has wilderness spawns
		list.add("wildernessmobs=" + world.hasWildernessMobs());
		// force town mob spawns
		list.add("forcetownmobs=" + world.isForceTownMobs());
		// has firespread enabled
		list.add("firespread=" + world.isFire());
		list.add("forcefirespread=" + world.isForceFire());
		// has explosions enabled
		list.add("explosions=" + world.isExpl());
		list.add("forceexplosions=" + world.isForceExpl());
		// Enderman block protection
		list.add("endermanprotect=" + world.isEndermanProtect());
		// CreatureTrample
		list.add("disablecreaturetrample=" + world.isDisableCreatureTrample());

		// Unclaimed
		list.add("");
		list.add("# Unclaimed Zone settings.");

		// Unclaimed Zone Build
		if (world.getUnclaimedZoneBuild() != null)
			list.add("unclaimedZoneBuild=" + world.getUnclaimedZoneBuild());
		// Unclaimed Zone Destroy
		if (world.getUnclaimedZoneDestroy() != null)
			list.add("unclaimedZoneDestroy=" + world.getUnclaimedZoneDestroy());
		// Unclaimed Zone Switch
		if (world.getUnclaimedZoneSwitch() != null)
			list.add("unclaimedZoneSwitch=" + world.getUnclaimedZoneSwitch());
		// Unclaimed Zone Item Use
		if (world.getUnclaimedZoneItemUse() != null)
			list.add("unclaimedZoneItemUse=" + world.getUnclaimedZoneItemUse());
		// Unclaimed Zone Name
		if (world.getUnclaimedZoneName() != null)
			list.add("unclaimedZoneName=" + world.getUnclaimedZoneName());

		list.add("");
		list.add("# The following are blocks that will bypass the above build, destroy, switch and itemuse settings.");

		// Unclaimed Zone Ignore Ids
		if (world.getUnclaimedZoneIgnoreMaterials() != null)
			list.add("unclaimedZoneIgnoreIds=" + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ","));

		// PlotManagement Delete
		list.add("");
		list.add("# The following settings control what blocks are deleted upon a townblock being unclaimed");
		// Using PlotManagement Delete
		list.add("usingPlotManagementDelete=" + world.isUsingPlotManagementDelete());
		// Plot Management Delete Ids
		if (world.getPlotManagementDeleteIds() != null)
			list.add("plotManagementDeleteIds=" + StringMgmt.join(world.getPlotManagementDeleteIds(), ","));

		// EntityType removal on unclaim.
		list.add("");
		list.add("# The following settings control what EntityTypes are deleted upon a townblock being unclaimed");
		list.add("# Valid EntityTypes are listed here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html");
		list.add("isDeletingEntitiesOnUnclaim=" + world.isDeletingEntitiesOnUnclaim());
		if (world.getUnclaimDeleteEntityTypes() != null)
			list.add("unclaimDeleteEntityTypes=" + StringMgmt.join(world.getUnclaimDeleteEntityTypes(), ","));

		// PlotManagement
		list.add("");
		list.add("# The following settings control what blocks are deleted upon a mayor issuing a '/plot clear' command");
		// Using PlotManagement Mayor Delete
		list.add("usingPlotManagementMayorDelete=" + world.isUsingPlotManagementMayorDelete());
		// Plot Management Mayor Delete
		if (world.getPlotManagementMayorDelete() != null)
			list.add("plotManagementMayorDelete=" + StringMgmt.join(world.getPlotManagementMayorDelete(), ","));

		// PlotManagement Revert
		list.add("");
		list.add("# If enabled when a town claims a townblock a snapshot will be taken at the time it is claimed.");
		list.add("# When the townblock is unclaimed its blocks will begin to revert to the original snapshot.");
		// Using PlotManagement Revert
		list.add("usingPlotManagementRevert=" + world.isUsingPlotManagementRevert());

		list.add("# Any block Id's listed here will not be respawned. Instead it will revert to air. This list also world on the WildRegen settings below.");
		// Plot Management Ignore Ids
		if (world.getPlotManagementIgnoreIds() != null)
			list.add("plotManagementIgnoreIds=" + StringMgmt.join(world.getPlotManagementIgnoreIds(), ","));

		// PlotManagement Wild Regen
		list.add("");
		list.add("# The following settings control which entities/blocks' explosions are reverted in the wilderness.");
		list.add("# If enabled any damage caused by entity explosions will repair itself.");
		// Using PlotManagement Wild Regen
		list.add("usingPlotManagementWildRegen=" + world.isUsingPlotManagementWildEntityRevert());

		list.add("# The list of entities whose explosions would be reverted.");
		// Wilderness Explosion Protection entities
		if (world.getPlotManagementWildRevertEntities() != null)
			list.add("PlotManagementWildRegenEntities=" + StringMgmt.join(world.getPlotManagementWildRevertEntities(), ","));

		list.add("# If enabled any damage caused by block explosions will repair itself.");
		// Using PlotManagement Wild Block Regen
		list.add("usingPlotManagementWildRegenBlocks=" + world.isUsingPlotManagementWildBlockRevert());

		list.add("# The list of blocks whose explosions would be reverted.");
		// Wilderness Explosion Protection blocks
		if (world.getPlotManagementWildRevertBlocks() != null)
			list.add("PlotManagementWildRegenBlocks=" + StringMgmt.join(world.getPlotManagementWildRevertBlocks(), ","));

		list.add("# The list of blocks to regenerate. (if empty all blocks will regenerate)");
		// Wilderness Explosion Protection entities
		if (world.getPlotManagementWildRevertBlockWhitelist() != null)
			list.add("PlotManagementWildRegenBlockWhitelist=" + StringMgmt.join(world.getPlotManagementWildRevertBlockWhitelist(), ","));

		list.add("# The delay after which the explosion reverts will begin.");
		// Using PlotManagement Wild Regen Delay
		list.add("usingPlotManagementWildRegenDelay=" + world.getPlotManagementWildRevertDelay());

		
		// Using Towny
		list.add("");
		list.add("# This setting is used to enable or disable Towny in this world.");
		// Using Towny
		list.add("usingTowny=" + world.isUsingTowny());

		// is War allowed
		list.add("");
		list.add("# This setting is used to enable or disable Event war in this world.");
		list.add("warAllowed=" + world.isWarAllowed());

		// Metadata
		list.add("");
		list.add("metadata=" + serializeMetadata(world));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getWorldFilename(world)));

		return true;

	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock) {

		FileMgmt.checkOrCreateFolder(dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName());

		List<String> list = new ArrayList<>();

		// name
		list.add("name=" + townBlock.getName());

		// price
		list.add("price=" + townBlock.getPlotPrice());

		// town
		try {
			list.add("town=" + townBlock.getTown().getName());
		} catch (NotRegisteredException ignored) {
		}

		// resident
		if (townBlock.hasResident())
			list.add("resident=" + townBlock.getResidentOrNull().getName());

		// type
		list.add("type=" + townBlock.getTypeName());

		// outpost
		list.add("outpost=" + townBlock.isOutpost());

		/*
		 * Only include a permissions line IF the plot perms are custom.
		 */
		if (townBlock.isChanged()) {
			// permissions
			list.add("permissions=" + townBlock.getPermissions().toString());
		}

		// Have permissions been manually changed
		list.add("changed=" + townBlock.isChanged());

		list.add("locked=" + townBlock.isLocked());

		list.add("claimedAt=" + townBlock.getClaimedAt());
		
		// Metadata
		list.add("metadata=" + serializeMetadata(townBlock));
		
		// Group ID
		StringBuilder groupID = new StringBuilder();
		if (townBlock.hasPlotObjectGroup()) {
			groupID.append(townBlock.getPlotObjectGroup().getUUID());
		}
		
		list.add("groupID=" + groupID);
		
		list.add("trustedResidents=" + StringMgmt.join(toUUIDList(townBlock.getTrustedResidents()), ","));
		
		Map<String, String> stringMap = new HashMap<>();
		for (Map.Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
			stringMap.put(entry.getKey().getUUID().toString(), entry.getValue().toString());
		}
		
		list.add("customPermissionData=" + new Gson().toJson(stringMap));
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFileSaveTask(list, getTownBlockFilename(townBlock)));

		return true;

	}

	public boolean saveJail(Jail jail) {
		
		List<String> list = new ArrayList<>();
		
		list.add("townblock=" + jail.getTownBlock().getWorldCoord().toString());
		StringBuilder jailArray = new StringBuilder("spawns=");
		for (Location spawn : new ArrayList<>(jail.getJailCellLocations())) {
			jailArray.append(spawn.getWorld().getName()).append(",")
					.append(spawn.getX()).append(",")
					.append(spawn.getY()).append(",")
					.append(spawn.getZ()).append(",")
					.append(spawn.getPitch()).append(",")
					.append(spawn.getYaw()).append(";");
		}
		list.add(jailArray.toString());

		this.queryQueue.add(new FlatFileSaveTask(list, getJailFilename(jail)));
		return true;
	}
	
	/*
	 * Delete objects
	 */

	@Override
	public void deleteObject(String type, UUID uuid) {
		deleteFileByTypeAndUUID(TownyDBFileType.valueOf(type), uuid);
	}
	
	@Override
	public void deleteObject(String type, String name) {
		deleteFileByTypeAndName(TownyDBFileType.valueOf(type), name);
	}
	
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
		deleteFileByTypeAndName(TownyDBFileType.RESIDENT, resident.getName());
	}

	@Override 
	public void deleteHibernatedResident(UUID uuid) {
		deleteFileByTypeAndUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid);
	}
	
	@Override
	public void deleteTown(Town town) {
		deleteFileByTypeAndName(TownyDBFileType.TOWN, town.getName());
	}

	@Override
	public void deleteNation(Nation nation) {
		deleteFileByTypeAndName(TownyDBFileType.NATION, nation.getName());
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		deleteFileByTypeAndName(TownyDBFileType.WORLD, world.getName());
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
					name = townBlock.getTown().getName();
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
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param resident Resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getResidentFilename(Resident resident) {
		return getFileOfTypeWithName(TownyDBFileType.RESIDENT, resident.getName());
	}
	
	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param uuid UUID of the hibernated resident whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getHibernatedResidentFilename(UUID uuid) {
		return getFileOfTypeWithUUID(TownyDBFileType.HIBERNATED_RESIDENT, uuid);
	}

	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param town Town whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getTownFilename(Town town) {
		return getFileOfTypeWithName(TownyDBFileType.TOWN, town.getName());
	}

	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param nation Nation whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getNationFilename(Nation nation) {
		return getFileOfTypeWithName(TownyDBFileType.NATION, nation.getName());
	}

	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithName(TownyDBFileType, String)} instead.
	 * @param world TownyWorld whose file you want to get.
	 * @return {@link #getFileOfTypeWithName(TownyDBFileType, String)}
	 */
	@Deprecated
	public String getWorldFilename(TownyWorld world) {
		return getFileOfTypeWithName(TownyDBFileType.WORLD, world.getName());
	}
	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param group PlotGroup whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getPlotGroupFilename(PlotGroup group) {
		return getFileOfTypeWithUUID(TownyDBFileType.PLOTGROUP, group.getID());
	}

	/**
	 * @deprecated as of 0.98.1.13, use {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)} instead.
	 * @param jail Jail whose file you want to get.
	 * @return {@link #getFileOfTypeWithUUID(TownyDBFileType, UUID)}
	 */
	@Deprecated
	public String getJailFilename(Jail jail) {
		return getFileOfTypeWithUUID(TownyDBFileType.JAIL, jail.getUUID());
	}


}
