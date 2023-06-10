package com.palmergames.bukkit.towny.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource.TownyDBFileType;
import com.palmergames.bukkit.util.BukkitTools;

public class TownyLegacyFlatFileConverter {
	
	private Towny plugin;
	private Logger logger;
	private String databasePath;
	private TownyFlatFileSource source;
	private HashMap<String, UUID> residentNameMap = new HashMap<String, UUID>(0);
	private HashMap<String, UUID> townNameMap = new HashMap<String, UUID>(0);
	private HashMap<String, UUID> nationNameMap = new HashMap<String, UUID>(0);

	/**
	 * The goal of the {@link TownyLegacyFlatFileConverter} is to pre-edit Towny
	 * database files to the point that the {@link ObjectLoadUtil} is able to finish
	 * the job. This is required because of the order that Towny's database objects
	 * are loaded, leading to unknowns when converting.
	 * <p>
	 * This class edits:
	 * <p>
	 * - the mentions of mayors, towns and nations in the Resident, Town and Nation files;
	 * - the names are stored in the Resident, Town and Nation files;
	 * - the World files are given names and UUIDs;
	 * - Resident, Town, Nation and World files are renamed to their UUID;
	 * - the TownBlocks' world folders are renamed;
	 * all before they are loaded into memory.
	 * 
	 * @param plugin {@link Towny} plugin instance.
	 * @param source {@link TownyFlatFileSource} instance.
	 */
	public TownyLegacyFlatFileConverter(Towny plugin, TownyFlatFileSource source) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.source = source;
		this.databasePath = TownyUniverse.getInstance().getRootFolder() + File.separator + "data";
	}

	public boolean updateLegacyFlatFileDB() {
		return updateResidents() && updateTowns() && updateNations() && updateWorlds() && updateTownBlocks();
	}

	/*
	 * Updaters
	 */

	private boolean updateResidents() {
		return updateObjectType(TownyDBFileType.RESIDENT);
	}

	private boolean updateTowns() {
		return updateObjectType(TownyDBFileType.TOWN);
	}

	private boolean updateNations() {
		return updateObjectType(TownyDBFileType.NATION);
	}

	private boolean updateObjectType(TownyDBFileType type) {
		String folderName = type.getFolderName();
		logger.info("Updating legacy " + folderName + " files...");
		int numConvertedFiles = 0;
		int numAlreadyConvertedFiles = 0;
		File[] files = getFiles(type);

		List<File> toDelete = new ArrayList<>();
		for (File file : files) {
			String fileName = file.getName().replace(".txt", "");
			if (alreadyUUIDFile(fileName)) {
				numAlreadyConvertedFiles++;
				continue;
			}
			UUID uuid = getUUID(file);
			if (uuid == null) {
				logger.warning("No UUID could be found in the " + folderName + "\\" 
				+ file.getName() + " file! This file will not be converted!");
				toDelete.add(file);
				continue;
			}
			switch (type) {
			case RESIDENT:
				residentNameMap.put(fileName, uuid);
				updateTownIn(file, "town");
				break;
			case TOWN:
				townNameMap.put(fileName, uuid);
				updateMayorIn(file);
				updateNationIn(file);
				break;
			case NATION:
				nationNameMap.put(fileName, uuid);
				updateTownIn(file, "capital");
				break;
			default:
				break;
			}
			renameLegacyFile(file, type, uuid.toString());
			numConvertedFiles++;
		}
		int numDeletedFiles = toDelete.size();
		for (File file : new ArrayList<>(toDelete))
			source.deleteFileByTypeAndName(type, file.getName().replace(".txt", ""));

		if (numAlreadyConvertedFiles > 0)
			plugin.getLogger().info("Towny found " + numAlreadyConvertedFiles + " files that were already converted in the " + folderName + " folder.");
		if (numConvertedFiles > 0)
			plugin.getLogger().info("Towny converted " + numConvertedFiles + " files from legacy to UUID format in the " + folderName + " folder.");
		if (numDeletedFiles > 0)
			plugin.getLogger().info("Towny could not convert " + numDeletedFiles + " files from legacy to UUID format in the " + folderName + "folder.");
		return true;
	}

	private boolean updateWorlds() {
		logger.info("Updating legacy World files...");
		int convertedFiles = 0;
		File[] files = getFiles(TownyDBFileType.WORLD);
		if (files.length != 0)
			logger.info("Found " + files.length + " files in the worlds folder...");
		for (File file : files) {
			String fileName = file.getName().replace(".txt", "");
			if (alreadyUUIDFile(fileName))
				continue;

			updateWorldFile(file);
			convertedFiles++;
		}
		if (convertedFiles > 0)
			plugin.getLogger().info("Towny converted " + convertedFiles + " files from legacy to UUID format in the worlds folder.");
		return true;
	}

	private void updateWorldFile(File file) {
		String worldName = file.getName().replace(".txt", "");
		String uuid = BukkitTools.getWorld(worldName).getUID().toString();
		if (worldName == null || worldName.isEmpty() || uuid == null || uuid.isEmpty())
			return;
		applyUUIDAndName(file, worldName, uuid);
		renameLegacyFile(file, TownyDBFileType.WORLD, uuid);

	}

	private boolean updateTownBlocks() {
		logger.info("Updating legacy TownBlocks files...");
		File townblocksFolder = new File(databasePath + File.separator + "townblocks");
		File[] worldFolders = townblocksFolder.listFiles(File::isDirectory);
		if (worldFolders.length != 0)
			logger.info("Found " + worldFolders.length + " folders in the townblocks folder...");
		for (File worldfolder : worldFolders) {
			if (alreadyUUIDFile(worldfolder.getName()))
				continue;
			UUID uuid = BukkitTools.getWorld(worldfolder.getName()).getUID();
			File newFolder = new File(databasePath + File.separator + "townblocks" + File.separator + uuid.toString());
			logger.info("Renaming TownBlock world folder " + worldfolder.getName() + "...");
			worldfolder.renameTo(newFolder);
			worldfolder.delete();
		}
		return true;
	}

	/*
	 * Getters
	 */

	private File[] getFiles(TownyDBFileType type) {
		File[] files = new File(databasePath + File.separator + type.folderName)
				.listFiles(file -> file.getName().toLowerCase().endsWith(type.fileExtension));

		if (files.length != 0)
			logger.info("Found " + files.length + " files in the " + type.folderName + " folder...");
		return files;
	}

	private static UUID getUUID(File file) {
		if (file.exists() && file.isFile()) {
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
				Properties properties = new Properties();
				properties.load(isr);
				String uuidAsString = properties.getProperty("uuid");
				return UUID.fromString(uuidAsString); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static String getValue(File file, String key) {
		if (file.exists() && file.isFile()) {
			String search = key + "=";
			try (Scanner sc = new Scanner(file)) {
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					if (line.contains(search))
						return line.replace(search , ""); 
				}
			} catch (FileNotFoundException ignored) {}
		}
		return null;
	}

	private File getResidentFile(String mayorName) {
		return new File(databasePath + File.separator + "residents" + File.separator + mayorName + ".txt");
	}

	private File getNationFile(String nationName) {
		return new File(databasePath + File.separator + "nations" + File.separator + nationName + ".txt");
	}

	private File getTownFile(String townName) {
		return new File(databasePath + File.separator + "towns" + File.separator  + townName + ".txt");
	}

	/*
	 * Setters
	 */

	private void renameLegacyFile(File file, TownyDBFileType type, String uuid) {
		File newFile = new File(databasePath + File.separator + type.folderName + File.separator + uuid + type.fileExtension);
		boolean delete = false;
		String fileName = file.getName().replace(type.fileExtension, "");
		if (fileName == null) {
			plugin.getLogger().warning("While converting a file, Towny was passed a null fileName! Guilty file: " + file.getAbsolutePath());
			return;
		}
		if (newFile.exists()) {
			plugin.getLogger().warning(type.folderName + "\\" +  file.getName() + " could not be saved in UUID format because a file with the UUID " + uuid.toString() + " already exists! The non-UUID formatted file will be removed.");
			delete = true;
		} else {
			delete = file.renameTo(newFile);
			if (!type.equals(TownyDBFileType.WORLD))
				applyName(newFile, fileName);
		}
		if (delete)
			source.deleteFileByTypeAndName(type, fileName);
	}

	private void updateMayorIn(File file) {
		String mayorName = getValue(file, "mayor");
		if (mayorName == null || mayorName.isEmpty())
			return;

		UUID uuid = null;
		if (residentNameMap.containsKey(mayorName)) {
			uuid = residentNameMap.get(mayorName);
		} else {
			uuid = getUUID(getResidentFile(mayorName));
		}
		if (uuid == null) {
			logger.warning("The mayor named " + mayorName + " did not store a UUID!");
		} else {
			if (!residentNameMap.containsKey(mayorName))
				residentNameMap.put(mayorName, uuid);
			setKeyValueInFile("mayor", mayorName, uuid.toString(), file);
		}
	}

	private void updateNationIn(File file) {
		String nationName = getValue(file, "nation");
		if (nationName == null || nationName.isEmpty())
			return;

		UUID uuid = null;
		if (nationNameMap.containsKey(nationName)) {
			uuid = nationNameMap.get(nationName);
		} else {
			uuid = getUUID(getNationFile(nationName));
		}
		if (uuid == null) {
			logger.warning("The nation named " + nationName + " did not store a UUID!");
		} else {
			if (!nationNameMap.containsKey(nationName))
				nationNameMap.put(nationName, uuid);
			setKeyValueInFile("nation", nationName, uuid.toString(), file);
		}
	}

	private void updateTownIn(File file, String townOrCapital) {
		String townName = getValue(file, townOrCapital);
		if (townName == null || townName.isEmpty())
			return;

		UUID uuid = null;
		if (townNameMap.containsKey(townName)) {
			uuid = townNameMap.get(townName);
		} else {
			uuid = getUUID(getTownFile(townName)); 
		}
		if (uuid == null) {
			logger.warning("The " + townOrCapital + " named " + townName + " did not store a UUID!");
		} else {
			if (!townNameMap.containsKey(townName))
				townNameMap.put(townName, uuid);
			setKeyValueInFile(townOrCapital, townName, uuid.toString(), file);
		}
	}

	private static void applyName(File file, String name) {
		if (file.exists() && file.isFile()) {
			if (hasKey(file, "name"))
				return;
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) { 
				bw.append("name=" + name);
			} catch (IOException ignored) {}
		}
	}

	private static void applyUUIDAndName(File file, String name, String uuid) {
		if (file.exists() && file.isFile()) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
				if (!hasKey(file, "uuid"))
					bw.append("uuid=" + uuid + "\n");
				if (!hasKey(file, "name"))
					bw.append("name=" + name);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void setKeyValueInFile(String key, String oldValue, String newValue, File file) {
		String oldLine = key + "=" + oldValue;
		String newLine = key + "=" + newValue;

		try (Scanner sc = new Scanner(file)) {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while (sc.hasNextLine()) {
				line = sc.nextLine();
				if (line.contains(oldLine))
					line = newLine;
				sb.append(line + System.lineSeparator());
			}
			sc.close();
			String fileContents = sb.toString();
			try (FileWriter writer = new FileWriter(file)) {
				writer.append(fileContents);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
		}
	}

	/*
	 * Is-ers
	 */

	private static boolean hasKey(File file, String key) {
		try (FileInputStream fis = new FileInputStream(file);
				InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(isr);
			if (!properties.containsKey(key))
				return false;
			String value = (String) properties.get(key);
			return value != null && !value.isEmpty();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean alreadyUUIDFile(String fileName) {
		try {
			UUID.fromString(fileName);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static void convertLegacySQLDBToFlatfile() {
		
		
		
	}
}