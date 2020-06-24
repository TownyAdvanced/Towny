package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import javax.naming.InvalidNameException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashSet;

public final class TownyFlatFileSource extends TownyDatabaseHandler {

	private final Queue<FlatFile_Task> queryQueue = new ConcurrentLinkedQueue<>();
	private final BukkitTask task;

	private final String newLine = System.getProperty("line.separator");
	
	public TownyFlatFileSource(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		// Create files and folders if non-existent
		if (!FileMgmt.checkOrCreateFolders(
			rootFolderPath,
			dataFolderPath,
			dataFolderPath + File.separator + "residents",
			dataFolderPath + File.separator + "towns",
			dataFolderPath + File.separator + "towns" + File.separator + "deleted",
			dataFolderPath + File.separator + "nations",
			dataFolderPath + File.separator + "nations" + File.separator + "deleted",
			dataFolderPath + File.separator + "worlds",
			dataFolderPath + File.separator + "worlds" + File.separator + "deleted",
			dataFolderPath + File.separator + "plot-block-data",
			dataFolderPath + File.separator + "townblocks",
			dataFolderPath + File.separator + "plotgroups"
		) || !FileMgmt.checkOrCreateFiles(
			dataFolderPath + File.separator + "residents.txt",
			dataFolderPath + File.separator + "towns.txt",
			dataFolderPath + File.separator + "nations.txt",
			dataFolderPath + File.separator + "worlds.txt",
			dataFolderPath + File.separator + "regen.txt",
			dataFolderPath + File.separator + "snapshot_queue.txt",
			dataFolderPath + File.separator + "plotgroups.txt"
		)) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");
		}
		/*
		 * Start our Async queue for pushing data to the database.
		 */
		task = BukkitTools.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			
			while (!TownyFlatFileSource.this.queryQueue.isEmpty()) {
				
				FlatFile_Task query = TownyFlatFileSource.this.queryQueue.poll();
				
				try {
					
					FileMgmt.listToFile(query.list, query.path);
					
				} catch (NullPointerException ex) {
					
					if (query != null)
						TownyMessaging.sendErrorMsg("Null Error saving to file - " + query.path);
					
				}
				
			}
			
		}, 5L, 5L);
	}
	
	public enum elements {
		VER, novalue;

		public static elements fromString(String Str) {

			try {
				return valueOf(Str);
			} catch (Exception ex) {
				return novalue;
			}
		}
	}

	@Override
	public void finishTasks() {
		
		// Cancel the repeating task as its not needed anymore.
		task.cancel();
		
		// Make sure that *all* tasks are saved before shutting down.
		while (!queryQueue.isEmpty()) {
			FlatFile_Task query = TownyFlatFileSource.this.queryQueue.poll();

			try {
				FileMgmt.listToFile(query.list, query.path);
			} catch (NullPointerException ex) {
				if (query != null)
					TownyMessaging.sendErrorMsg("Null Error saving to file - " + query.path);
			}
		}
	}

	@Override
	public synchronized boolean backup() throws IOException {
		String backupType = TownySettings.getFlatFileBackupType();
		long t = System.currentTimeMillis();
		String newBackupFolder = backupFolderPath + File.separator + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + t;
		FileMgmt.checkOrCreateFolders(
				rootFolderPath,
				rootFolderPath + File.separator + "backup");
		switch (backupType.toLowerCase()) {
			case "folder": {
				FileMgmt.checkOrCreateFolder(newBackupFolder);
				FileMgmt.copyDirectory(new File(dataFolderPath), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(logFolderPath), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(settingsFolderPath), new File(newBackupFolder));
				return true;
			}
			case "zip": {
				FileMgmt.zipDirectories(new File(newBackupFolder + ".zip"),
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
    
    @Override
    public void cleanupBackups() {
        long deleteAfter = TownySettings.getBackupLifeLength();
        if (deleteAfter >= 0)
            FileMgmt.deleteOldBackups(new File(universe.getRootFolder() + File.separator + "backup"), deleteAfter);
    }
    
    @Override
	public synchronized void deleteUnusedResidents() {

		String path;
		Set<String> names;

		path = dataFolderPath + File.separator + "residents";
		names = getResidentKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);

		path = dataFolderPath + File.separator + "towns";
		names = getTownsKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);

		path = dataFolderPath + File.separator + "nations";
		names = getNationsKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);
	}

	public String getResidentFilename(Resident resident) {

		return dataFolderPath + File.separator + "residents" + File.separator + resident.getName() + ".txt";
	}

	public String getTownFilename(Town town) {

		return dataFolderPath + File.separator + "towns" + File.separator + town.getName() + ".txt";
	}

	public String getNationFilename(Nation nation) {

		return dataFolderPath + File.separator + "nations" + File.separator + nation.getName() + ".txt";
	}

	public String getWorldFilename(TownyWorld world) {

		return dataFolderPath + File.separator + "worlds" + File.separator + world.getName() + ".txt";
	}

	public String getPlotFilename(PlotBlockData plotChunk) {

		return dataFolderPath + File.separator + "plot-block-data" + File.separator + plotChunk.getWorldName() + File.separator + plotChunk.getX() + "_" + plotChunk.getZ() + "_" + plotChunk.getSize() + ".data";
	}

	public String getPlotFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "plot-block-data" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}

	public String getTownBlockFilename(TownBlock townBlock) {

		return dataFolderPath + File.separator + "townblocks" + File.separator + townBlock.getWorld().getName() + File.separator + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}
	
	public String getPlotGroupFilename(PlotGroup group) {
		return dataFolderPath + File.separator + "plotgroups" + File.separator + group.getID() + ".data";
	}

	/*
	 * Load keys
	 */
	
	@Override
	public boolean loadTownBlockList() {
		
		TownyMessaging.sendDebugMsg("Loading TownBlock List");

		File townblocksFolder = new File(dataFolderPath + File.separator + "townblocks");
		File[] worldFolders = townblocksFolder.listFiles();
		TownyMessaging.sendDebugMsg("Folders found " + worldFolders.length);
		boolean mismatched = false;
		int mismatchedCount = 0;
		try {
			for (File worldfolder : worldFolders) {
				String worldName = worldfolder.getName();
				TownyWorld world;
				try {
					world = getWorld(worldName);
				} catch (NotRegisteredException e) {
					newWorld(worldName);
					world = getWorld(worldName);
				}
				File worldFolder = new File(dataFolderPath + File.separator + "townblocks" + File.separator + worldName);
				File[] townBlockFiles = worldFolder.listFiles();
				int total = 0;
				for (File townBlockFile : townBlockFiles) {
					if (!townBlockFile.getName().endsWith(".data") || townBlockFile.getName().equalsIgnoreCase("deleted")) {
						// File is not a townblock.data file or is the deleted folder.
						continue;
					}
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
	                TownyUniverse.getInstance().addTownBlock(townBlock);
					total++;
				}
				TownyMessaging.sendDebugMsg("World: " + worldName + " loaded " + total + " townblocks.");
			}
			if (mismatched)
				TownyMessaging.sendDebugMsg(String.format("%s townblocks were found with a town_block_size that does not match your config, they were not loaded into memory.", mismatchedCount));

			return true;
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean loadPlotGroupList() {
		TownyMessaging.sendDebugMsg("Loading Group List");
		String line = null;

		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "plotgroups.txt"), StandardCharsets.UTF_8))) {

			while ((line = fin.readLine()) != null) {
				if (!line.equals("")) {
					String[] tokens = line.split(",");
					String townName = null;
					UUID groupID;
					String groupName;
					
					// While in development the PlotGroupList stored a 4th element, a worldname. This was scrapped pre-release. 
					if (tokens.length == 4) {
						townName = tokens[1];
						groupID = UUID.fromString(tokens[2]);
						groupName = tokens[3];
					} else {
						townName = tokens[0];
						groupID = UUID.fromString(tokens[1]);
						groupName = tokens[2];
					}
					Town town = null;
					try {
						town = getTown(townName);
					} catch (NotRegisteredException e) {
						continue;
					}
					if (town != null)
						universe.newGroup(town, groupName, groupID);
				}
			}
			
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Group List at " + line + ", in towny\\data\\groups.txt");
			e.printStackTrace();
			return false;
		}
	}
	
	
	@Override
	public boolean loadResidentList() {
		
		TownyMessaging.sendDebugMsg("Loading Resident List");
		String line = null;

		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "residents.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null) {
				if (!line.equals("")) {
					try {
						newResident(line);
					} catch (AlreadyRegisteredException e) {
						TownyMessaging.sendDebugMsg("Duplicate resident '" + line + "' found in residents.txt, ignoring.");
						continue;
					}
				}
			}

			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Resident List at " + line + ", in towny\\data\\residents.txt");
			e.printStackTrace();
			return false;
			
		}
	}
	
	@Override
	public boolean loadTownList() {
		
		TownyMessaging.sendDebugMsg("Loading Town List");
		String line = null;
		BufferedReader fin;
		
		try {
			fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "towns.txt"), StandardCharsets.UTF_8));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					newTown(line);
			
			return true;
			
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			return false;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Town List at " + line + ", in towny\\data\\towns.txt");
			e.printStackTrace();
			return false;
			
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException ignore) {
				}
			}
		}
		
	}
	
	
	
	@Override
	public boolean loadNationList() {
		
		TownyMessaging.sendDebugMsg("Loading Nation List");
		String line = null;
		BufferedReader fin;
		
		try {
			fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "nations.txt"),StandardCharsets.UTF_8));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					newNation(line);
			
			return true;
			
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			return false;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Nation List at " + line + ", in towny\\data\\nations.txt");
			e.printStackTrace();
			return false;
			
		} finally {
			try {
				fin.close();
			} catch (IOException ignore) {
			}
		}
	}
	
	@Override
	public boolean loadWorldList() {
		
		if (plugin != null) {
			TownyMessaging.sendDebugMsg("Loading Server World List");
			for (World world : plugin.getServer().getWorlds()) {
				try {
					newWorld(world.getName());
				} catch (AlreadyRegisteredException e) {
					//e.printStackTrace();
				}
			}
		}
		
		// Can no longer reply on Bukkit to report ALL available worlds.
		
		TownyMessaging.sendDebugMsg("Loading World List");
		
		String line = null;
		
		try (BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFolderPath + File.separator + "worlds.txt"), StandardCharsets.UTF_8))) {
			
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					newWorld(line);
			
			return true;
			
		} catch (AlreadyRegisteredException e) {
			// Ignore this as the world may have been passed to us by bukkit
			return true;
			
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading World List at " + line + ", in towny\\data\\worlds.txt");
			e.printStackTrace();
			return false;
			
		}
		
	}
	
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

	/**
	 * Function which reads from a resident, town, nation, townyobject file, returning a hashmap. 
	 * 
	 * @param file - File from which the HashMap will be made.
	 * @return HashMap - Used for loading keys and values from object files. 
	 */
	public HashMap<String, String> loadFileIntoHashMap(File file) {
		HashMap<String, String> keys = new HashMap<>();
		try (FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {					
					Properties properties = new Properties();
					properties.load(isr);		
					for (String key : properties.stringPropertyNames()) {
						String value = properties.getProperty(key);
						keys.put(key, String.valueOf(value));
					}
			} catch (IOException e) {
				e.printStackTrace();
			}		
		return keys;
	}
	
	/*
	 * Load individual towny objects
	 */
	
	@Override
	public boolean loadResident(Resident resident) {
		
		String line = null;
		String path = getResidentFilename(resident);
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Resident: " + resident.getName());
			try {
				HashMap<String, String> keys = loadFileIntoHashMap(fileResident);
				
				resident.setLastOnline(Long.parseLong(keys.get("lastOnline")));
				
				line = keys.get("uuid");
				if (line != null)
					resident.setUUID(UUID.fromString(line));
				
				line = keys.get("registered");
				if (line != null)
					resident.setRegistered(Long.parseLong(line));
				else
					resident.setRegistered(resident.getLastOnline());
				
				line = keys.get("isNPC");
				if (line != null)
					resident.setNPC(Boolean.parseBoolean(line));
				
				line = keys.get("isJailed");
				if (line != null)
					resident.setJailed(Boolean.parseBoolean(line));
				
				line = keys.get("JailSpawn");
				if (line != null)
					resident.setJailSpawn(Integer.valueOf(line));
				
				line = keys.get("JailDays");
				if (line != null)
					resident.setJailDays(Integer.valueOf(line));
				
				line = keys.get("JailTown");
				if (line != null)
					resident.setJailTown(line);
				
				line = keys.get("title");
				if (line != null)
					resident.setTitle(line);
				
				line = keys.get("surname");
				if (line != null)
					resident.setSurname(line);
				
				line = keys.get("town");
				if (line != null)
					resident.setTown(getTown(line));

				try {
					line = keys.get("town-ranks");
					if (line != null)
						resident.setTownRanks(Arrays.asList((line.split(","))));
				} catch (Exception e) {}

				try {
					line = keys.get("nation-ranks");
					if (line != null)
						resident.setNationRanks(Arrays.asList((line.split(","))));
				} catch (Exception e) {}

				line = keys.get("friends");
				if (line != null) {
					String[] tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Resident friend = getResident(token);
							if (friend != null)
								resident.addFriend(friend);
						}
					}
				}
				
				line = keys.get("protectionStatus");
				if (line != null)
					resident.setPermissions(line);

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					resident.setMetadata(line.trim());
				
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading resident file " + resident.getName() + " at line: " + line + ", in towny\\data\\residents\\" + resident.getName() + ".txt");
				return false;
			} finally {
				saveResident(resident);
			}
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public boolean loadTown(Town town) {
		
		String line = null;
		String[] tokens;
		String path = getTownFilename(town);
		File fileTown = new File(path);		
		if (fileTown.exists() && fileTown.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Town: " + town.getName());
			try {
				HashMap<String, String> keys = loadFileIntoHashMap(fileTown);

				line = keys.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Town (" + town.getName() + ") Fetching Resident: " + token);							
							try {
								Resident resident = getResident(token);
								if (resident != null) {
									try {
										town.addResident(resident);
									} catch (AlreadyRegisteredException e) {
										TownyMessaging.sendErrorMsg("Loading Error: " + resident.getName() + " is already a member of a town (" + resident.getTown().getName() + ").");
									}
								}
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("Loading Error: Exception while reading a resident in the town file of " + town.getName() + ".txt. The resident " + token + " does not exist, removing them from town... (Will require manual editing of the town file if they are the mayor)");
							}
						}
					}
				}
				
				line = keys.get("outlaws");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Town Fetching Outlaw: " + token);
							try {
								Resident outlaw = getResident(token);
								if (outlaw != null)
									town.addOutlaw(outlaw);
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("Loading Error: Exception while reading an outlaw of town file " + town.getName() + ".txt. The outlaw " + token + " does not exist, removing from list...");
							}
						}
					}
				}
				
				line = keys.get("mayor");
				if (line != null)
					town.setMayor(getResident(line));
				
				town.setTownBoard(keys.get("townBoard"));
				
				line = keys.get("tag");
				if (line != null)
					try {
						town.setTag(line);
					} catch (TownyException e) {
						town.setTag("");
					}
				
				line = keys.get("protectionStatus");
				if (line != null)
					town.setPermissions(line);
				
				line = keys.get("bonusBlocks");
				if (line != null)
					try {
						town.setBonusBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setBonusBlocks(0);
					}
				
				line = keys.get("purchasedBlocks");
				if (line != null)
					try {
						town.setPurchasedBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPurchasedBlocks(0);
					}
				
				line = keys.get("plotPrice");
				if (line != null)
					try {
						town.setPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotPrice(0);
					}
				
				line = keys.get("hasUpkeep");
				if (line != null)
					try {
						town.setHasUpkeep(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("taxpercent");
				if (line != null)
					try {
						town.setTaxPercentage(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("taxes");
				if (line != null)
					try {
						town.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						town.setTaxes(0);
					}
				
				line = keys.get("plotTax");
				if (line != null)
					try {
						town.setPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotTax(0);
					}
				
				line = keys.get("commercialPlotPrice");
				if (line != null)
					try {
						town.setCommercialPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotPrice(0);
					}
				
				line = keys.get("commercialPlotTax");
				if (line != null)
					try {
						town.setCommercialPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotTax(0);
					}
				
				line = keys.get("embassyPlotPrice");
				if (line != null)
					try {
						town.setEmbassyPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotPrice(0);
					}
				
				line = keys.get("embassyPlotTax");
				if (line != null)
					try {
						town.setEmbassyPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotTax(0);
					}
				
				line = keys.get("spawnCost");
				if (line != null)
					try {
						town.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						town.setSpawnCost(TownySettings.getSpawnTravelCost());
					}
				
				line = keys.get("adminDisabledPvP");
				if (line != null)
					try {
						town.setAdminDisabledPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("adminEnabledPvP");
				if (line != null)
					try {
						town.setAdminEnabledPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("open");
				if (line != null)
					try {
						town.setOpen(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("public");
				if (line != null)
					try {
						town.setPublic(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("conquered");
				if (line != null)
					try {
						town.setConquered(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("conqueredDays");
				if (line != null)
					town.setConqueredDays(Integer.valueOf(line));

				line = keys.get("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = getWorld(tokens[0]);
							try {
								int x = Integer.parseInt(tokens[1]);
								int z = Integer.parseInt(tokens[2]);
								TownBlock homeBlock = TownyUniverse.getInstance().getTownBlock(new WorldCoord(world.getName(), x, z));
								town.forceSetHomeBlock(homeBlock);
							} catch (NumberFormatException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid location.");
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid TownBlock.");
							} catch (TownyException e) {
								TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " does not have a home block.");
							}
                        } catch (NotRegisteredException e) {
                            TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " homeBlock tried to load invalid world.");
                        }
				}
				
				line = keys.get("spawn");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);
							
							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							town.forceSetSpawn(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
				
				// Load outpost spawns
				line = keys.get("outpostspawns");
				if (line != null) {
					String[] outposts = line.split(";");
					for (String spawn : outposts) {
						tokens = spawn.split(",");
						if (tokens.length >= 4)
							try {
								World world = plugin.getServerWorld(tokens[0]);
								double x = Double.parseDouble(tokens[1]);
								double y = Double.parseDouble(tokens[2]);
								double z = Double.parseDouble(tokens[3]);
								
								Location loc = new Location(world, x, y, z);
								if (tokens.length == 6) {
									loc.setPitch(Float.parseFloat(tokens[4]));
									loc.setYaw(Float.parseFloat(tokens[5]));
								}
								town.forceAddOutpostSpawn(loc);
							} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
							}
					}
				}
				
				// Load jail spawns
				line = keys.get("jailspawns");
				if (line != null) {
					String[] jails = line.split(";");
					for (String spawn : jails) {
						tokens = spawn.split(",");
						if (tokens.length >= 4)
							try {
								World world = plugin.getServerWorld(tokens[0]);
								double x = Double.parseDouble(tokens[1]);
								double y = Double.parseDouble(tokens[2]);
								double z = Double.parseDouble(tokens[3]);
								
								Location loc = new Location(world, x, y, z);
								if (tokens.length == 6) {
									loc.setPitch(Float.parseFloat(tokens[4]));
									loc.setYaw(Float.parseFloat(tokens[5]));
								}
								town.forceAddJailSpawn(loc);
							} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
							}
					}
				}
				
				line = keys.get("uuid");
				if (line != null) {
					try {
						town.setUuid(UUID.fromString(line));
					} catch (IllegalArgumentException ee) {
						town.setUuid(UUID.randomUUID());
					}
				}
				line = keys.get("registered");
				if (line != null) {
					try {
						town.setRegistered(Long.valueOf(line));
					} catch (Exception ee) {
						town.setRegistered(0);
					}
				}

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					town.setMetadata(line.trim());

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading town file " + town.getName() + " at line: " + line + ", in towny\\data\\towns\\" + town.getName() + ".txt");
				return false;
			} finally {
				saveTown(town);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean loadNation(Nation nation) {
		
		String line = "";
		String[] tokens;
		String path = getNationFilename(nation);
		File fileNation = new File(path);
		
		if (fileNation.exists() && fileNation.isFile()) {
			TownyMessaging.sendDebugMsg("Loading Nation: " + nation.getName());
			try {
				HashMap<String, String> keys = loadFileIntoHashMap(fileNation);
				
				line = keys.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							try {
								TownyMessaging.sendDebugMsg("Nation Fetching Town: " + token);
								Town town = getTown(token);
								if (town != null) {
									nation.addTown(town);
								}
							} catch (NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("Loading Error: Exception while reading a town in the nation file of " + nation.getName() + ".txt. The town " + token + " does not exist, removing it from nation... (Will require editing of the nation file if it is the capital)");
							}
						}
					}
				}
				
				line = keys.get("capital");
				if (line != null)
					nation.setCapital(getTown(line));
				
				line = keys.get("nationBoard");
				if (line != null)
					try {
						nation.setNationBoard(line);
					} catch (Exception e) {
						nation.setNationBoard("");
					}

				line = keys.get("mapColorHexCode");
				if (line != null) {
					try {
						nation.setMapColorHexCode(line);
					} catch (Exception e) {
						nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
					}
				} else {
					nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
				}

				line = keys.get("tag");
				if (line != null)
					try {
						nation.setTag(line);
					} catch (TownyException e) {
						nation.setTag("");
					}
				
				line = keys.get("allies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Nation friend = getNation(token);
							if (friend != null)
								nation.addAlly(friend); //("ally", friend);
						}
					}
				}
				
				line = keys.get("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							Nation enemy = getNation(token);
							if (enemy != null)
								nation.addEnemy(enemy); //("enemy", enemy);
						}
					}
				}
				
				line = keys.get("taxes");
				if (line != null)
					try {
						nation.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setTaxes(0.0);
					}
				
				line = keys.get("spawnCost");
				if (line != null)
					try {
						nation.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setSpawnCost(TownySettings.getSpawnTravelCost());
					}
				
				line = keys.get("neutral");
				if (line != null)
					try {
						nation.setNeutral(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("uuid");
				if (line != null) {
					try {
						nation.setUuid(UUID.fromString(line));
					} catch (IllegalArgumentException ee) {
						nation.setUuid(UUID.randomUUID());
					}
				}
				line = keys.get("registered");
				if (line != null) {
					try {
						nation.setRegistered(Long.valueOf(line));
					} catch (Exception ee) {
						nation.setRegistered(0);
					}
				}
				
				line = keys.get("nationSpawn");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length >= 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);
							
							Location loc = new Location(world, x, y, z);
							if (tokens.length == 6) {
								loc.setPitch(Float.parseFloat(tokens[4]));
								loc.setYaw(Float.parseFloat(tokens[5]));
							}
							nation.forceSetNationSpawn(loc);
						} catch (NumberFormatException | NullPointerException | NotRegisteredException ignored) {
						}
				}
				
				line = keys.get("isPublic");
				if (line != null)
					try {
						nation.setPublic(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("isOpen");
				if (line != null)
					try {
						nation.setOpen(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					nation.setMetadata(line.trim());

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading nation file " + nation.getName() + " at line: " + line + ", in towny\\data\\nations\\" + nation.getName() + ".txt");
				e.printStackTrace();
				return false;
			} finally {
				saveNation(nation);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean loadWorld(TownyWorld world) {
		
		String line = "";
		String path = getWorldFilename(world);
		
		// create the world file if it doesn't exist
		if (!FileMgmt.checkOrCreateFile(path)) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading file " + path);
		}
		
		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			TownyMessaging.sendDebugMsg("Loading World: " + world.getName());
			try {
				HashMap<String, String> keys = loadFileIntoHashMap(fileWorld);
				
				line = keys.get("claimable");
				if (line != null)
					try {
						world.setClaimable(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("pvp");
				if (line != null)
					try {
						world.setPVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcepvp");
				if (line != null)
					try {
						world.setForcePVP(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcetownmobs");
				if (line != null)
					try {
						world.setForceTownMobs(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("worldmobs");
				if (line != null)
					try {
						world.setWorldMobs(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("firespread");
				if (line != null)
					try {
						world.setFire(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forcefirespread");
				if (line != null)
					try {
						world.setForceFire(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("explosions");
				if (line != null)
					try {
						world.setExpl(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("forceexplosions");
				if (line != null)
					try {
						world.setForceExpl(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("endermanprotect");
				if (line != null)
					try {
						world.setEndermanProtect(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("disableplayertrample");
				if (line != null)
					try {
						world.setDisablePlayerTrample(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("disablecreaturetrample");
				if (line != null)
					try {
						world.setDisableCreatureTrample(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("unclaimedZoneBuild");
				if (line != null)
					try {
						world.setUnclaimedZoneBuild(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneDestroy");
				if (line != null)
					try {
						world.setUnclaimedZoneDestroy(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneSwitch");
				if (line != null)
					try {
						world.setUnclaimedZoneSwitch(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneItemUse");
				if (line != null)
					try {
						world.setUnclaimedZoneItemUse(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneName");
				if (line != null)
					try {
						world.setUnclaimedZoneName(line);
					} catch (Exception ignored) {
					}
				line = keys.get("unclaimedZoneIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setUnclaimedZoneIgnore(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementDelete(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("plotManagementDeleteIds");
				if (line != null)
					try {
						//List<Integer> nums = new ArrayList<Integer>();
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setPlotManagementDeleteIds(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementMayorDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementMayorDelete(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				line = keys.get("plotManagementMayorDelete");
				if (line != null)
					try {
						List<String> materials = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									materials.add(s.toUpperCase().trim());
								} catch (NumberFormatException ignored) {
								}
						world.setPlotManagementMayorDelete(materials);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementRevert");
				if (line != null)
					try {
						world.setUsingPlotManagementRevert(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}

				line = keys.get("plotManagementIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								mats.add(s);
						
						world.setPlotManagementIgnoreIds(mats);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementWildRegen");
				if (line != null)
					try {
						world.setUsingPlotManagementWildRevert(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("PlotManagementWildRegenEntities");
				if (line != null)
					try {
						List<String> entities = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									entities.add(s.trim());
								} catch (NumberFormatException ignored) {
								}
						world.setPlotManagementWildRevertEntities(entities);
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingPlotManagementWildRegenDelay");
				if (line != null)
					try {
						world.setPlotManagementWildRevertDelay(Long.parseLong(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("usingTowny");
				if (line != null)
					try {
						world.setUsingTowny(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}
				
				line = keys.get("warAllowed");
				if (line != null)
					try {
						world.setWarAllowed(Boolean.parseBoolean(line));
					} catch (Exception ignored) {
					}

				line = keys.get("metadata");
				if (line != null && !line.isEmpty())
					world.setMetadata(line.trim());
				
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading world file " + path + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
				return false;
			} finally {
				saveWorld(world);
			}
			return true;
		} else {
			TownyMessaging.sendErrorMsg("Loading Error: File error while reading " + world.getName() + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
			return false;
		}
	}
	
	@Override
	public boolean loadPlotGroups() {
		String line = "";
		String path;
		
		for (PlotGroup group : getAllPlotGroups()) {
			path = getPlotGroupFilename(group);
			
			File groupFile = new File(path);
			if (groupFile.exists() && groupFile.isFile()) {
				String test = null;
				try {
					HashMap<String, String> keys = loadFileIntoHashMap(groupFile);

					line = keys.get("groupName");
					if (line != null)
						group.setName(line.trim());
					
					line = keys.get("groupID");
					if (line != null)
						group.setID(UUID.fromString(line.trim()));
					
					test = "town";
					line = keys.get("town");
					if (line != null && !line.isEmpty()) {
						Town town = getTown(line.trim());
						group.setTown(town);
					}
					else {
						TownyMessaging.sendErrorMsg("Could not add to town!");
						deletePlotGroup(group);
					}
					
					line = keys.get("groupPrice");
					if (line != null && !line.isEmpty())
						group.setPrice(Double.parseDouble(line.trim()));

				} catch (Exception e) {
					if (test.equals("town")) {
						TownyMessaging.sendDebugMsg("Group file missing Town, deleting " + path);
						deletePlotGroup(group);
						TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in group.txt");
						continue;
					}
					TownyMessaging.sendErrorMsg("Loading Error: Exception while reading Group file " + path + " at line: " + line);
					return false;
				}
			} else {
				TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in groups.txt");
			}
		}
		
		savePlotGroupList();
		
		return true;
	}
	
	@Override
	public boolean loadTownBlocks() {
		
		String line = "";
		String path;
		

		for (TownBlock townBlock : getAllTownBlocks()) {
			path = getTownBlockFilename(townBlock);
			
			File fileTownBlock = new File(path);
			if (fileTownBlock.exists() && fileTownBlock.isFile()) {

				try {
					HashMap<String, String> keys = loadFileIntoHashMap(fileTownBlock);			

					line = keys.get("town");
					if (line != null) {
						if (line.isEmpty()) {
							TownyMessaging.sendErrorMsg("TownBlock file missing Town, deleting " + path);
							TownyUniverse.getInstance().removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}
						Town town = null;
						try {
							town = getTown(line.trim());
						} catch (NotRegisteredException e) {
							TownyMessaging.sendErrorMsg("TownBlock file contains unregistered Town: " + line + ", deleting " + path);
							TownyUniverse.getInstance().removeTownBlock(townBlock);
							deleteTownBlock(townBlock);
							continue;
						}
						townBlock.setTown(town);
						try {
							town.addTownBlock(townBlock);
							TownyWorld townyWorld = townBlock.getWorld();
							if (townyWorld != null && !townyWorld.hasTown(town))
								townyWorld.addTown(town);
						} catch (AlreadyRegisteredException ignored) {
						}
					} else {
						// Town line is null, townblock is invalid.
						TownyMessaging.sendErrorMsg("TownBlock file missing Town, deleting " + path);
						TownyUniverse.getInstance().removeTownBlock(townBlock);
						deleteTownBlock(townBlock);
						continue;
					}

					line = keys.get("name");
					if (line != null)
						try {
							townBlock.setName(line.trim());
						} catch (Exception ignored) {
						}
					
					line = keys.get("price");
					if (line != null)
						try {
							townBlock.setPlotPrice(Double.parseDouble(line.trim()));
						} catch (Exception ignored) {
						}

					line = keys.get("resident");
					if (line != null && !line.isEmpty())
						try {
							Resident res = getResident(line.trim());
							townBlock.setResident(res);
						} catch (Exception ignored) {
						}
					
					line = keys.get("type");
					if (line != null)
						try {
							townBlock.setType(Integer.parseInt(line));
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

					line = keys.get("metadata");
					if (line != null && !line.isEmpty())
						townBlock.setMetadata(line.trim());

					line = keys.get("groupID");
					UUID groupID = null;
					if (line != null && !line.isEmpty()) {
						groupID = UUID.fromString(line.trim());
					}
					
					if (groupID != null) {
						PlotGroup group = getPlotObjectGroup(townBlock.getTown().toString(), groupID);
						townBlock.setPlotObjectGroup(group);
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock file " + path + " at line: " + line);
					return false;
				}

			} else {
				TownyMessaging.sendErrorMsg("TownBlock file contains unknown error, deleting " + path);
				TownyUniverse.getInstance().removeTownBlock(townBlock);
				deleteTownBlock(townBlock);
			}
		}
		
		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean savePlotGroupList() {
		List<String> list = new ArrayList<>();
		
		for (PlotGroup group : getAllPlotGroups()) {
			list.add(group.getTown().getName() + "," + group.getID() + "," + group.getName());
		}
		
		this.queryQueue.add(new FlatFile_Task(list, dataFolderPath + File.separator + "plotgroups.txt"));
		
		return true;
	}

	@Override
	public boolean saveResidentList() {

		List<String> list = new ArrayList<>();

		for (Resident resident : getResidents()) {

			try {

				if (!list.contains(resident.getName()))
					list.add(NameValidation.checkAndFilterPlayerName(resident.getName()));

			} catch (InvalidNameException e) {

				TownyMessaging.sendErrorMsg("Saving Error: Exception while saving town list file:" + resident.getName());
			}
		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, dataFolderPath + File.separator + "residents.txt"));

		return true;

	}

	@Override
	public boolean saveTownList() {

		List<String> list = new ArrayList<>();

		for (Town town : getTowns()) {

			list.add(town.getName());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, dataFolderPath + File.separator + "towns.txt"));

		return true;

	}

	@Override
	public boolean saveNationList() {

		List<String> list = new ArrayList<>();

		for (Nation nation : getNations()) {

			list.add(nation.getName());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, dataFolderPath + File.separator + "nations.txt"));

		return true;

	}

	@Override
	public boolean saveWorldList() {

		List<String> list = new ArrayList<>();

		for (TownyWorld world : getWorlds()) {

			list.add(world.getName());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, dataFolderPath + File.separator + "worlds.txt"));

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
		// isNPC
		list.add("isNPC=" + resident.isNPC());
		// isJailed
		list.add("isJailed=" + resident.isJailed());
		// JailSpawn
		list.add("JailSpawn=" + resident.getJailSpawn());
		// JailDays
		list.add("JailDays=" + resident.getJailDays());
		// JailTown
		list.add("JailTown=" + resident.getJailTown());

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
		StringBuilder md = new StringBuilder();
		if (resident.hasMeta()) {
			HashSet<CustomDataField<?>> tdata = resident.getMetadata();
			for (CustomDataField<?> cdf : tdata) {
				md.append(cdf.toString()).append(";");
			}
		}
		list.add("metadata=" + md.toString());
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getResidentFilename(resident)));

		return true;

	}

	@Override
	public boolean saveTown(Town town) {

		List<String> list = new ArrayList<>();

		// Name
		list.add("name=" + town.getName());
		// Residents
		list.add("residents=" + StringMgmt.join(town.getResidents(), ","));
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
		list.add("assistants=" + StringMgmt.join(town.getAssistants(), ","));

		list.add(newLine);
		// Town Board
		list.add("townBoard=" + town.getTownBoard());
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
		// Open
		list.add("open=" + town.isOpen());
		// PVP
		list.add("adminDisabledPvP=" + town.isAdminDisabledPVP());
		list.add("adminEnabledPvP=" + town.isAdminEnabledPVP());
		// Public
		list.add("public=" + town.isPublic());
		// Conquered towns setting + date
		list.add("conquered=" + town.isConquered());
		list.add("conqueredDays " + town.getConqueredDays());
		if (town.hasValidUUID()){
			list.add("uuid=" + town.getUuid());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + town.getRegistered());
        
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

		// Jail Spawns
		StringBuilder jailArray = new StringBuilder("jailspawns=");
		if (town.hasJailSpawn())
			for (Location spawn : new ArrayList<>(town.getAllJailSpawns())) {
				jailArray.append(spawn.getWorld().getName()).append(",").append(spawn.getX()).append(",").append(spawn.getY()).append(",").append(spawn.getZ()).append(",").append(spawn.getPitch()).append(",").append(spawn.getYaw()).append(";");
			}
		list.add(jailArray.toString());

		// Outlaws
		list.add("outlaws=" + StringMgmt.join(town.getOutlaws(), ","));

		// Metadata
		StringBuilder md = new StringBuilder();
		if (town.hasMeta()) {
			HashSet<CustomDataField<?>> tdata = town.getMetadata();
			for (CustomDataField<?> cdf : tdata) {
				md.append(cdf.toString()).append(";");
			}
		}
		list.add("metadata=" + md.toString());
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getTownFilename(town)));

		return true;

	}
	
	@Override
	public boolean savePlotGroup(PlotGroup group) {
		
		List<String> list = new ArrayList<>();
		
		// Group ID
		list.add("groupID=" + group.getID().toString());
		
		// Group Name
		list.add("groupName=" + group.getName());
		
		// Group Price
		list.add("groupPrice=" + group.getPrice());
		
		// Town
		list.add("town=" + group.getTown().toString());
		
		// Save file
		this.queryQueue.add(new FlatFile_Task(list, getPlotGroupFilename(group)));
		
		return true;
	}

	@Override
	public boolean saveNation(Nation nation) {

		List<String> list = new ArrayList<>();

		list.add("towns=" + StringMgmt.join(nation.getTowns(), ","));

		if (nation.hasCapital())
			list.add("capital=" + nation.getCapital().getName());

		list.add("nationBoard=" + nation.getNationBoard());

		list.add("mapColorHexCode=" + nation.getMapColorHexCode());

		if (nation.hasTag())
			list.add("tag=" + nation.getTag());

		list.add("assistants=" + StringMgmt.join(nation.getAssistants(), ","));

		list.add("allies=" + StringMgmt.join(nation.getAllies(), ","));

		list.add("enemies=" + StringMgmt.join(nation.getEnemies(), ","));

		// Taxes
		list.add("taxes=" + nation.getTaxes());
		// Nation Spawn Cost
		list.add("spawnCost=" + nation.getSpawnCost());
		// Peaceful
		list.add("neutral=" + nation.isNeutral());
		if (nation.hasValidUUID()){
			list.add("uuid=" + nation.getUuid());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
        list.add("registered=" + nation.getRegistered());
        
        // Spawn
		if (nation.hasNationSpawn()) {
			try {
				list.add("nationSpawn=" + nation.getNationSpawn().getWorld().getName() + "," + nation.getNationSpawn().getX() + "," + nation.getNationSpawn().getY() + "," + nation.getNationSpawn().getZ() + "," + nation.getNationSpawn().getPitch() + "," + nation.getNationSpawn().getYaw());
			} catch (TownyException ignored) { }
		}

		list.add("isPublic=" + nation.isPublic());
		
		list.add("isOpen=" + nation.isOpen());

		// Metadata
		StringBuilder md = new StringBuilder();
		if (nation.hasMeta()) {
			HashSet<CustomDataField<?>> tdata = nation.getMetadata();
			for (CustomDataField<?> cdf : tdata) {
				md.append(cdf.toString()).append(";");
			}
		}
		list.add("metadata=" + md.toString());
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getNationFilename(nation)));

		return true;

	}

	@Override
	public boolean saveWorld(TownyWorld world) {

		List<String> list = new ArrayList<>();

		// PvP
		list.add("pvp=" + world.isPVP());
		// Force PvP
		list.add("forcepvp=" + world.isForcePVP());
		// Claimable
		list.add("# Can players found towns and claim plots in this world?");
		list.add("claimable=" + world.isClaimable());
		// has monster spawns
		list.add("worldmobs=" + world.hasWorldMobs());
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
		// PlayerTrample
		list.add("disableplayertrample=" + world.isDisablePlayerTrample());
		// CreatureTrample
		list.add("disablecreaturetrample=" + world.isDisableCreatureTrample());

		// Unclaimed
		list.add(newLine);
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
		list.add("# The following settings are only used if you are not using any permissions provider plugin");

		// Unclaimed Zone Ignore Ids
		if (world.getUnclaimedZoneIgnoreMaterials() != null)
			list.add("unclaimedZoneIgnoreIds=" + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ","));

		// PlotManagement Delete
		list.add(newLine);
		list.add("# The following settings control what blocks are deleted upon a townblock being unclaimed");

		// Using PlotManagement Delete
		list.add("usingPlotManagementDelete=" + world.isUsingPlotManagementDelete());
		// Plot Management Delete Ids
		if (world.getPlotManagementDeleteIds() != null)
			list.add("plotManagementDeleteIds=" + StringMgmt.join(world.getPlotManagementDeleteIds(), ","));

		// PlotManagement
		list.add(newLine);
		list.add("# The following settings control what blocks are deleted upon a mayor issuing a '/plot clear' command");

		// Using PlotManagement Mayor Delete
		list.add("usingPlotManagementMayorDelete=" + world.isUsingPlotManagementMayorDelete());
		// Plot Management Mayor Delete
		if (world.getPlotManagementMayorDelete() != null)
			list.add("plotManagementMayorDelete=" + StringMgmt.join(world.getPlotManagementMayorDelete(), ","));

		// PlotManagement Revert
		list.add(newLine + "# If enabled when a town claims a townblock a snapshot will be taken at the time it is claimed.");
		list.add("# When the townblock is unclaimded its blocks will begin to revert to the original snapshot.");

		// Using PlotManagement Revert
		list.add("usingPlotManagementRevert=" + world.isUsingPlotManagementRevert());
		// Using PlotManagement Revert Speed
		//list.add("usingPlotManagementRevertSpeed=" + Long.toString(world.getPlotManagementRevertSpeed()));

		list.add("# Any block Id's listed here will not be respawned. Instead it will revert to air.");

		// Plot Management Ignore Ids
		if (world.getPlotManagementIgnoreIds() != null)
			list.add("plotManagementIgnoreIds=" + StringMgmt.join(world.getPlotManagementIgnoreIds(), ","));

		// PlotManagement Wild Regen
		list.add("");
		list.add("# If enabled any damage caused by explosions will repair itself.");

		// Using PlotManagement Wild Regen
		list.add("usingPlotManagementWildRegen=" + world.isUsingPlotManagementWildRevert());

		// Wilderness Explosion Protection entities
		if (world.getPlotManagementWildRevertEntities() != null)
			list.add("PlotManagementWildRegenEntities=" + StringMgmt.join(world.getPlotManagementWildRevertEntities(), ","));

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
		StringBuilder md = new StringBuilder();
		if (world.hasMeta()) {
			HashSet<CustomDataField<?>> tdata = world.getMetadata();
			for (CustomDataField<?> cdf : tdata) {
				md.append(cdf.toString()).append(";");
			}
		}
		list.add("metadata=" + md.toString());
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getWorldFilename(world)));

		return true;

	}

	@Override
	public boolean saveAllTownBlocks() {
		for (Town town : getTowns()) {
			for (TownBlock townBlock : town.getTownBlocks())
				saveTownBlock(townBlock);
		}
		
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
		if (townBlock.hasResident()) {

			try {
				list.add("resident=" + townBlock.getResident().getName());
			} catch (NotRegisteredException ignored) {
			}
		}

		// type
		list.add("type=" + townBlock.getType().getId());

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
		
		// Metadata
		StringBuilder md = new StringBuilder();
		if (townBlock.hasMeta()) {
			HashSet<CustomDataField<?>> tdata = townBlock.getMetadata();
			for (CustomDataField<?> cdf : tdata) {
				md.append(cdf.toString()).append(";");
			}
		}
		
		list.add("metadata=" + md.toString());
		
		// Group ID
		StringBuilder groupID = new StringBuilder();
		StringBuilder groupName = new StringBuilder();
		if (townBlock.hasPlotObjectGroup()) {
			groupID.append(townBlock.getPlotObjectGroup().getID());
			groupName.append(townBlock.getPlotObjectGroup().getName());
		}
		
		list.add("groupID=" + groupID.toString());
		
		
		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getTownBlockFilename(townBlock)));

		return true;

	}

	@Override
	public boolean saveRegenList() {
        
        try (BufferedWriter fout = new BufferedWriter(new FileWriter(dataFolderPath + File.separator + "regen.txt"))) {
            for (PlotBlockData plot : new ArrayList<>(TownyRegenAPI.getPlotChunks().values()))
                fout.write(plot.getWorldName() + "," + plot.getX() + "," + plot.getZ() + newLine);
            
        } catch (Exception e) {
            TownyMessaging.sendErrorMsg("Saving Error: Exception while saving regen file");
            e.printStackTrace();
            return false;
            
        }

		return true;

	}

	@Override
	public boolean saveSnapshotList() {
        
        try (BufferedWriter fout = new BufferedWriter(new FileWriter(dataFolderPath + File.separator + "snapshot_queue.txt"))) {
            while (TownyRegenAPI.hasWorldCoords()) {
                WorldCoord worldCoord = TownyRegenAPI.getWorldCoord();
                fout.write(worldCoord.getWorldName() + "," + worldCoord.getX() + "," + worldCoord.getZ() + newLine);
            }
            
        } catch (Exception e) {
            TownyMessaging.sendErrorMsg("Saving Error: Exception while saving snapshot_queue file");
            e.printStackTrace();
            return false;
            
        }

		return true;

	}

	/**
	 * Save PlotBlockData
	 *
	 * @param plotChunk - Plot for data to be saved for.
	 * @return true if saved
	 */
	@Override
	public boolean savePlotData(PlotBlockData plotChunk) {

		FileMgmt.checkOrCreateFolder(dataFolderPath + File.separator + "plot-block-data" + File.separator + plotChunk.getWorldName());
        
        String path = getPlotFilename(plotChunk);
        try (DataOutputStream fout = new DataOutputStream(new FileOutputStream(path))) {
            
            switch (plotChunk.getVersion()) {
                
                case 1:
                case 2:
                case 3:
                case 4:
                    /*
                     * New system requires pushing
                     * version data first
                     */
                    fout.write("VER".getBytes(StandardCharsets.UTF_8));
                    fout.write(plotChunk.getVersion());
                    
                    break;
                
                default:
                
            }
            
            // Push the plot height, then the plot block data types.
            fout.writeInt(plotChunk.getHeight());
            for (String block : new ArrayList<>(plotChunk.getBlockList())) {
                fout.writeUTF(block);
            }
            
        } catch (Exception e) {
            TownyMessaging.sendErrorMsg("Saving Error: Exception while saving PlotBlockData file (" + path + ")");
            e.printStackTrace();
            return false;
        }
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
	 * @param townBlock - townBlock being reverted
	 * @return PlotBlockData or null
	 */
    @Override
    public PlotBlockData loadPlotData(TownBlock townBlock) {
        
        String fileName = getPlotFilename(townBlock);
        
        String value;
        
        if (isFile(fileName)) {
            PlotBlockData plotBlockData = null;
			try {
				plotBlockData = new PlotBlockData(townBlock);
			} catch (NullPointerException e1) {
				TownyMessaging.sendErrorMsg("Unable to load plotblockdata for townblock: " + townBlock.getWorldCoord().toString() + ". Skipping regeneration for this townBlock.");
				return null;
			}
            List<String> blockArr = new ArrayList<>();
            int version = 0;
            
            try (DataInputStream fin = new DataInputStream(new FileInputStream(fileName))) {
                
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
        return null;
    }
    
    @Override
	public void deletePlotData(PlotBlockData plotChunk) {

		File file = new File(getPlotFilename(plotChunk));
		if (file.exists())
			file.delete();
	}

	private boolean isFile(String fileName) {

		File file = new File(fileName);
		return file.exists() && file.isFile();

	}

	@Override
	public void deleteFile(String fileName) {

		File file = new File(fileName);
		if (file.exists())
			file.delete();
	}

	@Override
	public void deleteResident(Resident resident) {

		File file = new File(getResidentFilename(resident));
		if (file.exists())
			file.delete();
	}

	@Override
	public void deleteTown(Town town) {

		File file = new File(getTownFilename(town));
		if (file.exists()) {
			FileMgmt.moveFile(file, ("deleted"));
		}
	}

	@Override
	public void deleteNation(Nation nation) {

		File file = new File(getNationFilename(nation));
		if (file.exists()) {
			FileMgmt.moveFile(file, ("deleted"));
		}
	}

	@Override
	public void deleteWorld(TownyWorld world) {

		File file = new File(getWorldFilename(world));
		if (file.exists()) {
			FileMgmt.moveFile(file, ("deleted"));
		}
	}

	@Override
	public void deleteTownBlock(TownBlock townBlock) {

		File file = new File(getTownBlockFilename(townBlock));
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
	}
	
	@Override
	public void deletePlotGroup(PlotGroup group) {
    	File file = new File(getPlotGroupFilename(group));
    	if (file.exists())
    		file.delete();
    	else
    		TownyMessaging.sendErrorMsg("That file doesn't exist!");
	}
}
