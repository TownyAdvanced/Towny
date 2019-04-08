package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.KeyValueFile;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: Make sure the lack of a particular value doesn't error out the entire file

public class TownyFlatFileSource extends TownyDatabaseHandler {

	private Queue<FlatFile_Task> queryQueue = new ConcurrentLinkedQueue<>();
	private BukkitTask task = null;

	protected final String newLine = System.getProperty("line.separator");
	protected String rootFolder = "";
	protected String dataFolder = FileMgmt.fileSeparator() + "data";
	protected String settingsFolder = FileMgmt.fileSeparator() + "settings";
	protected String logFolder = FileMgmt.fileSeparator() + "logs";

	private enum elements {
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
	public void initialize(Towny plugin, TownyUniverse universe) {

		this.universe = universe;
		this.plugin = plugin;
		this.rootFolder = universe.getRootFolder();

		// Create files and folders if non-existent
		try {
			FileMgmt.checkFolders(new String[] {
					rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks" });
			FileMgmt.checkFiles(new String[] {
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt" });
		} catch (IOException e) {
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

					TownyMessaging.sendErrorMsg("Null Error saving to file - " + query.path);

				}

			}

		}, 5L, 5L);
	}

	@Override
	public void cancelTask() {

		task.cancel();

	}

	@Override
	public synchronized void backup() throws IOException {

		String backupType = TownySettings.getFlatFileBackupType();
		if (!backupType.equalsIgnoreCase("none")) {

			TownyLogger.shutDown();

			long t = System.currentTimeMillis();
			String newBackupFolder = rootFolder + FileMgmt.fileSeparator() + "backup" + FileMgmt.fileSeparator() + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + Long.toString(t);
			FileMgmt.checkFolders(new String[] {
					rootFolder,
					rootFolder + FileMgmt.fileSeparator() + "backup" });
			if (backupType.equalsIgnoreCase("folder")) {
				FileMgmt.checkFolders(new String[] { newBackupFolder });
				FileMgmt.copyDirectory(new File(rootFolder + dataFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + logFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + settingsFolder), new File(newBackupFolder));
			} else if (backupType.equalsIgnoreCase("zip"))
				FileMgmt.zipDirectories(new File[] {
						new File(rootFolder + dataFolder),
						new File(rootFolder + logFolder),
						new File(rootFolder + settingsFolder) }, new File(newBackupFolder + ".zip"));
			else {
				plugin.setupLogger();
				throw new IOException("Unsupported flatfile backup type (" + backupType + ")");
			}
			plugin.setupLogger();
		}
	}

	@Override
	public synchronized void deleteUnusedResidentFiles() {

		String path;
		Set<String> names;

		path = rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents";
		names = getResidentKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);

		path = rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns";
		names = getTownsKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);

		path = rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations";
		names = getNationsKeys();

		FileMgmt.deleteUnusedFiles(new File(path), names);
	}

	public String getResidentFilename(Resident resident) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents" + FileMgmt.fileSeparator() + resident.getName() + ".txt";
	}

	public String getTownFilename(Town town) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns" + FileMgmt.fileSeparator() + town.getName() + ".txt";
	}

	public String getNationFilename(Nation nation) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations" + FileMgmt.fileSeparator() + nation.getName() + ".txt";
	}

	public String getWorldFilename(TownyWorld world) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds" + FileMgmt.fileSeparator() + world.getName() + ".txt";
	}

	public String getPlotFilename(PlotBlockData plotChunk) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() + plotChunk.getWorldName() + FileMgmt.fileSeparator() + plotChunk.getX() + "_" + plotChunk.getZ() + "_" + plotChunk.getSize() + ".data";
	}

	public String getPlotFilename(TownBlock townBlock) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() + townBlock.getWorld().getName() + FileMgmt.fileSeparator() + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}

	public String getTownBlockFilename(TownBlock townBlock) {

		return rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks" + FileMgmt.fileSeparator() + townBlock.getWorld().getName() + FileMgmt.fileSeparator() + townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data";
	}

	/*
	 * Load keys
	 */

	@Override
	public boolean loadTownBlockList() {

		TownyMessaging.sendDebugMsg("Loading TownBlock List");
		String line = null;
		BufferedReader fin = null;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks.txt"));

			while ((line = fin.readLine()) != null)
				if (!line.equals("")) {

					String[] tokens = line.split(",");

					if (tokens.length < 3)
						continue;

					TownyWorld world;

					try {

						world = getWorld(tokens[0]);

					} catch (NotRegisteredException ex) {

						/*
						 * The world is not listed.
						 * Allow the creation of new worlds here to account
						 * for mod worlds which are not reported at startup.
						 */
						newWorld(tokens[0]);
						world = getWorld(tokens[0]);

					}

					int x = Integer.parseInt(tokens[1]);
					int z = Integer.parseInt(tokens[2]);

					try {
						world.newTownBlock(x, z);
					} catch (AlreadyRegisteredException e) {
					}

				}

			return true;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Townblock List at " + line + ", in towny\\data\\townblocks.txt");
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
	public boolean loadResidentList() {

		TownyMessaging.sendDebugMsg("Loading Resident List");
		String line = null;
		BufferedReader fin = null;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt"));

			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					newResident(line);

			return true;

		} catch (AlreadyRegisteredException e) {
			TownyMessaging.sendErrorMsg("Error Loading Resident List at " + line + ", resident is possibly listed twice.");
			e.printStackTrace();
			return false;

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Error Loading Resident List at " + line + ", in towny\\data\\residents.txt");
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
	public boolean loadTownList() {

		TownyMessaging.sendDebugMsg("Loading Town List");
		String line = null;
		BufferedReader fin = null;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt"));
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
		BufferedReader fin = null;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt"));
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
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

	@Override
	public boolean loadWorldList() {

		if (plugin != null) {
			TownyMessaging.sendDebugMsg("Loading Server World List");
			for (World world : plugin.getServer().getWorlds())
				try {
					newWorld(world.getName());
				} catch (AlreadyRegisteredException | NotRegisteredException e) {
					//e.printStackTrace();
				}
		}

		// Can no longer reply on Bukkit to report ALL available worlds.

		TownyMessaging.sendDebugMsg("Loading World List");

		String line = null;
		BufferedReader fin = null;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"));

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
	public boolean loadRegenList() {

		TownyMessaging.sendDebugMsg("Loading Regen List");

		String line = null;
		BufferedReader fin = null;
		String[] split;
		PlotBlockData plotData;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt"));

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
	public boolean loadSnapshotList() {

		TownyMessaging.sendDebugMsg("Loading Snapshot Queue");

		String line = null;
		BufferedReader fin = null;
		String[] split;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt"));

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

		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException ignore) {
				}
			}
		}

	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResident(Resident resident) {

		String line = null;
		String path = getResidentFilename(resident);
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				resident.setLastOnline(Long.parseLong(kvFile.get("lastOnline")));

				line = kvFile.get("registered");
				if (line != null)
					resident.setRegistered(Long.parseLong(line));
				else
					resident.setRegistered(resident.getLastOnline());

				line = kvFile.get("isNPC");
				if (line != null)
					resident.setNPC(Boolean.parseBoolean(line));

				line = kvFile.get("isJailed");
				if (line != null)
					resident.setJailed(Boolean.parseBoolean(line));

				line = kvFile.get("JailSpawn");
				if (line != null)
					resident.setJailSpawn(Integer.valueOf(line));

				line = kvFile.get("JailTown");
				if (line != null)
					resident.setJailTown(line);

				line = kvFile.get("title");
				if (line != null)
					resident.setTitle(line);

				line = kvFile.get("surname");
				if (line != null)
					resident.setSurname(line);

				line = kvFile.get("town");
				if (line != null)
					resident.setTown(getTown(line));

				line = kvFile.get("town-ranks");
				if (line != null)
					resident.setTownRanks(new ArrayList<>(Arrays.asList((line.split(",")))));

				line = kvFile.get("nation-ranks");
				if (line != null)
					resident.setNationRanks(new ArrayList<>(Arrays.asList((line.split(",")))));

				line = kvFile.get("friends");
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

				line = kvFile.get("protectionStatus");
				if (line != null)
					resident.setPermissions(line);

				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, null, resident);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading resident file " + resident.getName() + " at line: " + line + ", in towny\\data\\residents\\" + resident.getName() + ".txt");
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadTown(Town town) {

		String line = null;
		String[] tokens;
		String path = getTownFilename(town);
		File fileTown = new File(path);
		if (fileTown.exists() && fileTown.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Town Fetching Resident: " + token);
							Resident resident = getResident(token);
							if (resident != null) {
								try {
									town.addResident(resident);
								} catch (AlreadyRegisteredException e) {
									TownyMessaging.sendErrorMsg("Loading Error: " + resident.getName() + " is already a member of a town (" + resident.getTown().getName() + ").");
								}

							}
						}
					}
				}

				line = kvFile.get("outlaws");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Town Fetching Outlaw: " + token);
							try {
								Resident outlaw = getResident(token);
								if (outlaw != null)
									town.addOutlaw(outlaw);
							}
							catch(NotRegisteredException e) {
								TownyMessaging.sendErrorMsg("Loading Error: Exception while reading an outlaw of town file " + town.getName() + ".txt. The outlaw " + token + " does not exist, skipping...");
							}
						}
					}
				}

				line = kvFile.get("mayor");
				if (line != null)
					town.setMayor(getResident(line));

				//				line = kvFile.get("assistants");
				//				if (line != null) {
				//					tokens = line.split(",");
				//					for (String token : tokens) {
				//						if (!token.isEmpty()) {
				//							Resident assistant = getResident(token);
				//							if ((assistant != null) && (town.hasResident(assistant)))
				//								town.addAssistant(assistant);
				//						}
				//					}
				//				}

				town.setTownBoard(kvFile.get("townBoard"));

				line = kvFile.get("tag");
				if (line != null)
					try {
						town.setTag(line);
					} catch (TownyException e) {
						town.setTag("");
					}

				line = kvFile.get("protectionStatus");
				if (line != null)
					town.setPermissions(line);

				line = kvFile.get("bonusBlocks");
				if (line != null)
					try {
						town.setBonusBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setBonusBlocks(0);
					}

				line = kvFile.get("purchasedBlocks");
				if (line != null)
					try {
						town.setPurchasedBlocks(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPurchasedBlocks(0);
					}

				line = kvFile.get("plotPrice");
				if (line != null)
					try {
						town.setPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotPrice(0);
					}

				line = kvFile.get("hasUpkeep");
				if (line != null)
					try {
						town.setHasUpkeep(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("taxpercent");
				if (line != null)
					try {
						town.setTaxPercentage(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("taxes");
				if (line != null)
					try {
						town.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						town.setTaxes(0);
					}

				line = kvFile.get("plotTax");
				if (line != null)
					try {
						town.setPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setPlotTax(0);
					}

				line = kvFile.get("commercialPlotPrice");
				if (line != null)
					try {
						town.setCommercialPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotPrice(0);
					}

				line = kvFile.get("commercialPlotTax");
				if (line != null)
					try {
						town.setCommercialPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setCommercialPlotTax(0);
					}

				line = kvFile.get("embassyPlotPrice");
				if (line != null)
					try {
						town.setEmbassyPlotPrice(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotPrice(0);
					}

				line = kvFile.get("embassyPlotTax");
				if (line != null)
					try {
						town.setEmbassyPlotTax(Double.parseDouble(line));
					} catch (Exception e) {
						town.setEmbassyPlotTax(0);
					}

				line = kvFile.get("spawnCost");
				if (line != null)
					try {
						town.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						town.setSpawnCost(TownySettings.getSpawnTravelCost());
					}

				line = kvFile.get("adminDisabledPvP");
				if (line != null)
					try {
						town.setAdminDisabledPVP(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				/*
				 * line = kvFile.get("mobs");
				 * if (line != null)
				 * try {
				 * town.setHasMobs(Boolean.parseBoolean(line));
				 * } catch (NumberFormatException nfe) {
				 * } catch (Exception e) {
				 * }
				 */
				line = kvFile.get("open");
				if (line != null)
					try {
						town.setOpen(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("public");
				if (line != null)
					try {
						town.setPublic(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				/*
				 * line = kvFile.get("explosion");
				 * if (line != null)
				 * try {
				 * town.setBANG(Boolean.parseBoolean(line));
				 * } catch (NumberFormatException nfe) {
				 * } catch (Exception e) {
				 * }
				 *
				 * line = kvFile.get("fire");
				 * if (line != null)
				 * try {
				 * town.setFire(Boolean.parseBoolean(line));
				 * } catch (NumberFormatException nfe) {
				 * } catch (Exception e) {
				 * }
				 */
				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, town, null);

				line = kvFile.get("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = getWorld(tokens[0]);

							try {
								int x = Integer.parseInt(tokens[1]);
								int z = Integer.parseInt(tokens[2]);
								TownBlock homeBlock = world.getTownBlock(x, z);
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

				line = kvFile.get("spawn");
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
						} catch (NumberFormatException | NullPointerException | NotRegisteredException e) {
						}
				}

				// Load outpost spawns
				line = kvFile.get("outpostspawns");
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
							} catch (NumberFormatException | NullPointerException | NotRegisteredException e) {
							}
					}
				}

				// Load jail spawns
				line = kvFile.get("jailspawns");
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
							} catch (NumberFormatException | NullPointerException | NotRegisteredException e) {
							}
					}
				}

				line = kvFile.get("uuid");
				if (line != null) {
					try {
						town.setUuid(UUID.fromString(line));
					} catch (IllegalArgumentException ee) {
						town.setUuid(UUID.randomUUID());
					}
				}
				line = kvFile.get("registered");
				if (line != null){
					try {
						town.setRegistered(Long.valueOf(line));
					} catch (Exception ee){
						town.setRegistered(0);
					}
				}

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading town file " + town.getName() + " at line: " + line + ", in towny\\data\\towns\\" + town.getName() + ".txt");
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadNation(Nation nation) {

		String line = "";
		String[] tokens;
		String path = getNationFilename(nation);
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("Nation Fetching Town: " + token);
							Town town = getTown(token);
							if (town != null)
								nation.addTown(town);
						}
					}
				}

				line = kvFile.get("capital");
				if (line != null)
					nation.setCapital(getTown(line));

				//				line = kvFile.get("assistants");
				//				if (line != null) {
				//					tokens = line.split(",");
				//					for (String token : tokens) {
				//						if (!token.isEmpty()) {
				//							Resident assistant = getResident(token);
				//							if (assistant != null)
				//								nation.addAssistant(assistant);
				//						}
				//					}
				//				}

				line = kvFile.get("nationBoard");
				if (line != null)
					try {
						nation.setNationBoard(line);
					} catch (Exception e) {
						nation.setNationBoard("");
					}
				

				line = kvFile.get("tag");
				if (line != null)
					try {
						nation.setTag(line);
					} catch (TownyException e) {
						nation.setTag("");
					}

				line = kvFile.get("allies");
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

				line = kvFile.get("enemies");
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

				line = kvFile.get("taxes");
				if (line != null)
					try {
						nation.setTaxes(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setTaxes(0.0);
					}

				line = kvFile.get("spawnCost");
				if (line != null)
					try {
						nation.setSpawnCost(Double.parseDouble(line));
					} catch (Exception e) {
						nation.setSpawnCost(TownySettings.getSpawnTravelCost());
					}

				line = kvFile.get("neutral");
				if (line != null)
					try {
						nation.setNeutral(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("uuid");
				if (line != null) {
					try {
						nation.setUuid(UUID.fromString(line));
					} catch (IllegalArgumentException ee) {
						nation.setUuid(UUID.randomUUID());
					}
				}
				line = kvFile.get("registered");
				if (line != null){
					try {
						nation.setRegistered(Long.valueOf(line));
					} catch (Exception ee){
						nation.setRegistered(0);
					}
				}

				line = kvFile.get("nationSpawn");
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
						} catch (NumberFormatException | NullPointerException | NotRegisteredException e) {
						}
				}

				line = kvFile.get("isPublic");
				if (line != null)
					try {
						nation.setPublic(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading nation file " + nation.getName() + " at line: " + line + ", in towny\\data\\nations\\" + nation.getName() + ".txt");
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadWorld(TownyWorld world) {

		String line = "";
		String[] tokens;
		String path = getWorldFilename(world);

		// create the world file if it doesn't exist
		try {
			FileMgmt.checkFiles(new String[] { path });
		} catch (IOException e1) {
			TownyMessaging.sendErrorMsg("Loading Error: Exception while reading file " + path);
		}

		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						if (!token.isEmpty()) {
							TownyMessaging.sendDebugMsg("World Fetching Town: " + token);
							Town town = getTown(token);
							if (town != null) {
								town.setWorld(world);
								//world.addTown(town); not needed as it's handled in the Town object
							}
						}
					}
				}

				line = kvFile.get("claimable");
				if (line != null)
					try {
						world.setClaimable(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("pvp");
				if (line != null)
					try {
						world.setPVP(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("forcepvp");
				if (line != null)
					try {
						world.setForcePVP(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("forcetownmobs");
				if (line != null)
					try {
						world.setForceTownMobs(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("worldmobs");
				if (line != null)
					try {
						world.setWorldMobs(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("firespread");
				if (line != null)
					try {
						world.setFire(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("forcefirespread");
				if (line != null)
					try {
						world.setForceFire(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("explosions");
				if (line != null)
					try {
						world.setExpl(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("forceexplosions");
				if (line != null)
					try {
						world.setForceExpl(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("endermanprotect");
				if (line != null)
					try {
						world.setEndermanProtect(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("disableplayertrample");
				if (line != null)
					try {
						world.setDisablePlayerTrample(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("disablecreaturetrample");
				if (line != null)
					try {
						world.setDisableCreatureTrample(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("unclaimedZoneBuild");
				if (line != null)
					try {
						world.setUnclaimedZoneBuild(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneDestroy");
				if (line != null)
					try {
						world.setUnclaimedZoneDestroy(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneSwitch");
				if (line != null)
					try {
						world.setUnclaimedZoneSwitch(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneItemUse");
				if (line != null)
					try {
						world.setUnclaimedZoneItemUse(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneName");
				if (line != null)
					try {
						world.setUnclaimedZoneName(line);
					} catch (Exception e) {
					}
				line = kvFile.get("unclaimedZoneIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									int id = Integer.parseInt(s);

									mats.add(BukkitTools.getMaterial(id).name());

								} catch (NumberFormatException e) {
									mats.add(s);
								}
						world.setUnclaimedZoneIgnore(mats);
					} catch (Exception e) {
					}

				line = kvFile.get("usingPlotManagementDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementDelete(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("plotManagementDeleteIds");
				if (line != null)
					try {
						//List<Integer> nums = new ArrayList<Integer>();
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									int id = Integer.parseInt(s);

									mats.add(BukkitTools.getMaterial(id).name());

								} catch (NumberFormatException e) {
									mats.add(s);
								}
						world.setPlotManagementDeleteIds(mats);
					} catch (Exception e) {
					}

				line = kvFile.get("usingPlotManagementMayorDelete");
				if (line != null)
					try {
						world.setUsingPlotManagementMayorDelete(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				line = kvFile.get("plotManagementMayorDelete");
				if (line != null)
					try {
						List<String> materials = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									materials.add(s.toUpperCase().trim());
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementMayorDelete(materials);
					} catch (Exception e) {
					}

				line = kvFile.get("usingPlotManagementRevert");
				if (line != null)
					try {
						world.setUsingPlotManagementRevert(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				/*
				 * No longer used - Never was used. Sadly not configurable per-world based on how the timer runs.
				 */
//				line = kvFile.get("usingPlotManagementRevertSpeed");
//				if (line != null)
//					try {
//						world.setPlotManagementRevertSpeed(Long.parseLong(line));
//					} catch (Exception e) {
//					}
				line = kvFile.get("plotManagementIgnoreIds");
				if (line != null)
					try {
						List<String> mats = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									int id = Integer.parseInt(s);

									mats.add(BukkitTools.getMaterial(id).name());

								} catch (NumberFormatException e) {
									mats.add(s);
								}
						world.setPlotManagementIgnoreIds(mats);
					} catch (Exception e) {
					}

				line = kvFile.get("usingPlotManagementWildRegen");
				if (line != null)
					try {
						world.setUsingPlotManagementWildRevert(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				line = kvFile.get("PlotManagementWildRegenEntities");
				if (line != null)
					try {
						List<String> entities = new ArrayList<>();
						for (String s : line.split(","))
							if (!s.isEmpty())
								try {
									entities.add(s.trim());
								} catch (NumberFormatException e) {
								}
						world.setPlotManagementWildRevertEntities(entities);
					} catch (Exception e) {
					}

				line = kvFile.get("usingPlotManagementWildRegenDelay");
				if (line != null)
					try {
						world.setPlotManagementWildRevertDelay(Long.parseLong(line));
					} catch (Exception e) {
					}

				line = kvFile.get("usingTowny");
				if (line != null)
					try {
						world.setUsingTowny(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				// loadTownBlocks(world);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Loading Error: Exception while reading world file " + path + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
				return false;
			}

			return true;
		} else {
			TownyMessaging.sendErrorMsg("Loading Error: File error while reading " + world.getName() + " at line: " + line + ", in towny\\data\\worlds\\" + world.getName() + ".txt");
			return false;
		}
	}

	@Override
	public boolean loadTownBlocks() {

		String line = "";
		String path;

		for (TownBlock townBlock : getAllTownBlocks()) {
			path = getTownBlockFilename(townBlock);
			//boolean set = false;

			File fileTownBlock = new File(path);
			if (fileTownBlock.exists() && fileTownBlock.isFile()) {
				try {
					KeyValueFile kvFile = new KeyValueFile(path);

					line = kvFile.get("name");
					if (line != null)
						try {
							townBlock.setName(line.trim());
						} catch (Exception e) {
						}

					line = kvFile.getString("price");
					if (line != null)
						try {
							townBlock.setPlotPrice(Double.parseDouble(line.trim()));
						} catch (Exception e) {
						}

					line = kvFile.getString("town");
					if (line != null)
						try {
							Town town = getTown(line.trim());
							townBlock.setTown(town);
						} catch (Exception e) {
						}

					line = kvFile.getString("resident");
					if (line != null && !line.isEmpty())
						try {
							Resident res = getResident(line.trim());
							townBlock.setResident(res);
						} catch (Exception e) {
						}

					line = kvFile.getString("type");
					if (line != null)
						try {
							townBlock.setType(Integer.parseInt(line));
						} catch (Exception e) {
						}

					line = kvFile.getString("outpost");
					if (line != null)
						try {
							townBlock.setOutpost(Boolean.parseBoolean(line));
						} catch (Exception e) {
						}

					line = kvFile.get("permissions");
					if ((line != null) && !line.isEmpty())
						try {
							townBlock.setPermissions(line.trim());
							//set = true;
						} catch (Exception e) {
						}

					line = kvFile.get("changed");
					if (line != null)
						try {
							townBlock.setChanged(Boolean.parseBoolean(line.trim()));
						} catch (Exception e) {
						}

					line = kvFile.get("locked");
					if (line != null)
						try {
							townBlock.setLocked(Boolean.parseBoolean(line.trim()));
						} catch (Exception e) {
						}

					line = kvFile.getString("town");
					if (line.isEmpty()) {
						TownyMessaging.sendDebugMsg("TownBlock file missing Town, deleting " + path);
						deleteTownBlock(townBlock);
						TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in townblocks.txt");
						TownyWorld world = townBlock.getWorld();
						world.removeTownBlock(townBlock);
					}

				} catch (Exception e) {
					TownyMessaging.sendErrorMsg("Loading Error: Exception while reading TownBlock file " + path + " at line: " + line);
					return false;
				}

				//				if (!set) {
				//					// no permissions found so set in relation to it's owners perms.
				//					try {
				//						if (townBlock.hasResident()) {
				//							townBlock.setPermissions(townBlock.getResident().getPermissions().toString());
				//						} else {
				//							townBlock.setPermissions(townBlock.getTown().getPermissions().toString());
				//						}
				//					} catch (NotRegisteredException e) {
				//						// Will never reach here
				//					}
				//				}
			} else {
				TownyMessaging.sendDebugMsg("Missing file: " + path + " deleting entry in townblocks.txt");
				TownyWorld world = townBlock.getWorld();
				world.removeTownBlock(townBlock);
			}
		}
		saveTownBlockList();

		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveTownBlockList() {

		List<String> list = new ArrayList<>();

		for (TownBlock townBlock : getAllTownBlocks()) {

			list.add(townBlock.getWorld().getName() + "," + townBlock.getX() + "," + townBlock.getZ());

		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks.txt"));

		return true;

	}

	@Override
	public boolean saveResidentList() {

		List<String> list = new ArrayList<>();

		for (Resident resident : getResidents()) {

			try {

				list.add(NameValidation.checkAndFilterPlayerName(resident.getName()));

			} catch (InvalidNameException e) {

				TownyMessaging.sendErrorMsg("Saving Error: Exception while saving town list file:" + resident.getName());
			}
		}

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt"));

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
		this.queryQueue.add(new FlatFile_Task(list, rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt"));

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
		this.queryQueue.add(new FlatFile_Task(list, rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt"));

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
		this.queryQueue.add(new FlatFile_Task(list, rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"));

		return true;

	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {

		List<String> list = new ArrayList<>();

		// Last Online
		list.add("lastOnline=" + Long.toString(resident.getLastOnline()));
		// Registered
		list.add("registered=" + Long.toString(resident.getRegistered()));
		// isNPC
		list.add("isNPC=" + Boolean.toString(resident.isNPC()));
		// isJailed
		list.add("isJailed=" + Boolean.toString(resident.isJailed()));
		// JailSpawn
		list.add("JailSpawn=" + Integer.toString(resident.getJailSpawn()));
		// JailTown
		list.add("JailTown=" + resident.getJailTown());

		// title		
		list.add("title=" + resident.getTitle());
		// surname
		list.add("surname=" + resident.getSurname());

		if (resident.hasTown()) {
			try {
				list.add("town=" + resident.getTown().getName());
			} catch (NotRegisteredException e) {
			}
			list.add("town-ranks=" + StringMgmt.join(resident.getTownRanks(), ","));
			list.add("nation-ranks=" + StringMgmt.join(resident.getNationRanks(), ","));
		}

		// Friends
		list.add("friends=" + StringMgmt.join(resident.getFriends(), ","));
		list.add("");

		// Plot Protection
		list.add("protectionStatus=" + resident.getPermissions().toString());

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
			} catch (NotRegisteredException e) {
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
		list.add("bonusBlocks=" + Integer.toString(town.getBonusBlocks()));
		// Purchased Blocks
		list.add("purchasedBlocks=" + Integer.toString(town.getPurchasedBlocks()));
		// Taxpercent
		list.add("taxpercent=" + Boolean.toString(town.isTaxPercentage()));
		// Taxes
		list.add("taxes=" + Double.toString(town.getTaxes()));
		// Plot Price
		list.add("plotPrice=" + Double.toString(town.getPlotPrice()));
		// Plot Tax
		list.add("plotTax=" + Double.toString(town.getPlotTax()));
		// Commercial Plot Price
		list.add("commercialPlotPrice=" + Double.toString(town.getCommercialPlotPrice()));
		// Commercial Tax
		list.add("commercialPlotTax=" + Double.toString(town.getCommercialPlotTax()));
		// Embassy Plot Price
		list.add("embassyPlotPrice=" + Double.toString(town.getEmbassyPlotPrice()));
		// Embassy Tax
		list.add("embassyPlotTax=" + Double.toString(town.getEmbassyPlotTax()));
		// Town Spawn Cost
		list.add("spawnCost=" + Double.toString(town.getSpawnCost()));
		// Upkeep
		list.add("hasUpkeep=" + Boolean.toString(town.hasUpkeep()));
		// Open
		list.add("open=" + Boolean.toString(town.isOpen()));
		// PVP
		list.add("adminDisabledPvP=" + Boolean.toString(town.isAdminDisabledPVP()));
		/* // Mobs
		* fout.write("mobs=" + Boolean.toString(town.hasMobs()) + newLine);
		*/
		// Public
		list.add("public=" + Boolean.toString(town.isPublic()));
		if (town.hasValidUUID()){
			list.add("uuid=" + town.getUuid());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
		Long value = town.getRegistered();
		if (value != null){
			list.add("registered=" + town.getRegistered());
		} else {
			list.add("registered=" + 0);
		}

		// Home Block
		if (town.hasHomeBlock())
			try {
				list.add("homeBlock=" + town.getHomeBlock().getWorld().getName() + "," + Integer.toString(town.getHomeBlock().getX()) + "," + Integer.toString(town.getHomeBlock().getZ()));
			} catch (TownyException e) {
			}

		// Spawn
		if (town.hasSpawn())
			try {
				list.add("spawn=" + town.getSpawn().getWorld().getName() + "," + Double.toString(town.getSpawn().getX()) + "," + Double.toString(town.getSpawn().getY()) + "," + Double.toString(town.getSpawn().getZ()) + "," + Float.toString(town.getSpawn().getPitch()) + "," + Float.toString(town.getSpawn().getYaw()));
			} catch (TownyException e) {
			}

		// Outpost Spawns
		String outpostArray = "outpostspawns=";
		if (town.hasOutpostSpawn())
			for (Location spawn : new ArrayList<>(town.getAllOutpostSpawns())) {
				outpostArray += (spawn.getWorld().getName() + "," + Double.toString(spawn.getX()) + "," + Double.toString(spawn.getY()) + "," + Double.toString(spawn.getZ()) + "," + Float.toString(spawn.getPitch()) + "," + Float.toString(spawn.getYaw()) + ";");
			}
		list.add(outpostArray);

		// Jail Spawns
		String jailArray = "jailspawns=";
		if (town.hasJailSpawn())
			for (Location spawn : new ArrayList<>(town.getAllJailSpawns())) {
				jailArray += (spawn.getWorld().getName() + "," + Double.toString(spawn.getX()) + "," + Double.toString(spawn.getY()) + "," + Double.toString(spawn.getZ()) + "," + Float.toString(spawn.getPitch()) + "," + Float.toString(spawn.getYaw()) + ";");
			}
		list.add(jailArray);

		// Outlaws
		list.add("outlaws=" + StringMgmt.join(town.getOutlaws(), ","));

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getTownFilename(town)));

		return true;

	}

	@Override
	public boolean saveNation(Nation nation) {

		List<String> list = new ArrayList<>();

		list.add("towns=" + StringMgmt.join(nation.getTowns(), ","));

		if (nation.hasCapital())
			list.add("capital=" + nation.getCapital().getName());

		list.add("nationBoard=" + nation.getNationBoard());

		if (nation.hasTag())
			list.add("tag=" + nation.getTag());

		list.add("assistants=" + StringMgmt.join(nation.getAssistants(), ","));

		list.add("allies=" + StringMgmt.join(nation.getAllies(), ","));

		list.add("enemies=" + StringMgmt.join(nation.getEnemies(), ","));

		// Taxes
		list.add("taxes=" + Double.toString(nation.getTaxes()));
		// Nation Spawn Cost
		list.add("spawnCost=" + Double.toString(nation.getSpawnCost()));
		// Peaceful
		list.add("neutral=" + Boolean.toString(nation.isNeutral()));
		if (nation.hasValidUUID()){
			list.add("uuid=" + nation.getUuid());
		} else {
			list.add("uuid=" + UUID.randomUUID());
		}
		Long value = nation.getRegistered();
		if (value != null){
			list.add("registered=" + nation.getRegistered());
		} else {
			list.add("registered=" + 0);
		}

		// Spawn
		if (nation.hasNationSpawn()) {
			try {
				list.add("nationSpawn=" + nation.getNationSpawn().getWorld().getName() + "," + Double.toString(nation.getNationSpawn().getX()) + "," + Double.toString(nation.getNationSpawn().getY()) + "," + Double.toString(nation.getNationSpawn().getZ()) + "," + Float.toString(nation.getNationSpawn().getPitch()) + "," + Float.toString(nation.getNationSpawn().getYaw()));
			} catch (TownyException e) { }
		}

		list.add("isPublic=" + Boolean.toString(nation.isPublic()));

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getNationFilename(nation)));

		return true;

	}

	@Override
	public boolean saveWorld(TownyWorld world) {

		List<String> list = new ArrayList<>();

		// Towns
		list.add("towns=" + StringMgmt.join(world.getTowns(), ","));

		list.add("");

		// PvP
		list.add("pvp=" + Boolean.toString(world.isPVP()));
		// Force PvP
		list.add("forcepvp=" + Boolean.toString(world.isForcePVP()));
		// Claimable
		list.add("# Can players found towns and claim plots in this world?");
		list.add("claimable=" + Boolean.toString(world.isClaimable()));
		// has monster spawns			
		list.add("worldmobs=" + Boolean.toString(world.hasWorldMobs()));
		// force town mob spawns			
		list.add("forcetownmobs=" + Boolean.toString(world.isForceTownMobs()));
		// has firespread enabled
		list.add("firespread=" + Boolean.toString(world.isFire()));
		list.add("forcefirespread=" + Boolean.toString(world.isForceFire()));
		// has explosions enabled
		list.add("explosions=" + Boolean.toString(world.isExpl()));
		list.add("forceexplosions=" + Boolean.toString(world.isForceExpl()));
		// Enderman block protection
		list.add("endermanprotect=" + Boolean.toString(world.isEndermanProtect()));
		// PlayerTrample
		list.add("disableplayertrample=" + Boolean.toString(world.isDisablePlayerTrample()));
		// CreatureTrample
		list.add("disablecreaturetrample=" + Boolean.toString(world.isDisableCreatureTrample()));

		// Unclaimed
		list.add(newLine);
		list.add("# Unclaimed Zone settings.");

		// Unclaimed Zone Build
		if (world.getUnclaimedZoneBuild() != null)
			list.add("unclaimedZoneBuild=" + Boolean.toString(world.getUnclaimedZoneBuild()));
		// Unclaimed Zone Destroy
		if (world.getUnclaimedZoneDestroy() != null)
			list.add("unclaimedZoneDestroy=" + Boolean.toString(world.getUnclaimedZoneDestroy()));
		// Unclaimed Zone Switch
		if (world.getUnclaimedZoneSwitch() != null)
			list.add("unclaimedZoneSwitch=" + Boolean.toString(world.getUnclaimedZoneSwitch()));
		// Unclaimed Zone Item Use
		if (world.getUnclaimedZoneItemUse() != null)
			list.add("unclaimedZoneItemUse=" + Boolean.toString(world.getUnclaimedZoneItemUse()));
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
		list.add("usingPlotManagementDelete=" + Boolean.toString(world.isUsingPlotManagementDelete()));
		// Plot Management Delete Ids
		if (world.getPlotManagementDeleteIds() != null)
			list.add("plotManagementDeleteIds=" + StringMgmt.join(world.getPlotManagementDeleteIds(), ","));

		// PlotManagement
		list.add(newLine);
		list.add("# The following settings control what blocks are deleted upon a mayor issuing a '/plot clear' command");

		// Using PlotManagement Mayor Delete
		list.add("usingPlotManagementMayorDelete=" + Boolean.toString(world.isUsingPlotManagementMayorDelete()));
		// Plot Management Mayor Delete
		if (world.getPlotManagementMayorDelete() != null)
			list.add("plotManagementMayorDelete=" + StringMgmt.join(world.getPlotManagementMayorDelete(), ","));

		// PlotManagement Revert
		list.add(newLine + "# If enabled when a town claims a townblock a snapshot will be taken at the time it is claimed.");
		list.add("# When the townblock is unclaimded its blocks will begin to revert to the original snapshot.");

		// Using PlotManagement Revert
		list.add("usingPlotManagementRevert=" + Boolean.toString(world.isUsingPlotManagementRevert()));
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
		list.add("usingPlotManagementWildRegen=" + Boolean.toString(world.isUsingPlotManagementWildRevert()));

		// Wilderness Explosion Protection entities
		if (world.getPlotManagementWildRevertEntities() != null)
			list.add("PlotManagementWildRegenEntities=" + StringMgmt.join(world.getPlotManagementWildRevertEntities(), ","));

		// Using PlotManagement Wild Regen Delay
		list.add("usingPlotManagementWildRegenDelay=" + Long.toString(world.getPlotManagementWildRevertDelay()));

		// Using Towny
		list.add("");
		list.add("# This setting is used to enable or disable Towny in this world.");

		// Using Towny
		list.add("usingTowny=" + Boolean.toString(world.isUsingTowny()));

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getWorldFilename(world)));

		return true;

	}

	@Override
	public boolean saveAllTownBlocks() {

		for (TownyWorld world : getWorlds()) {
			for (TownBlock townBlock : world.getTownBlocks())
				saveTownBlock(townBlock);
		}

		return true;
	}

	@Override
	public boolean saveTownBlock(TownBlock townBlock) {

		FileMgmt.checkFolders(new String[] { rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks" + FileMgmt.fileSeparator() + townBlock.getWorld().getName() });

		List<String> list = new ArrayList<>();

		// name
		list.add("name=" + townBlock.getName());

		// price
		list.add("price=" + townBlock.getPlotPrice());

		// town
		try {
			list.add("town=" + townBlock.getTown().getName());
		} catch (NotRegisteredException e) {
		}

		// resident
		if (townBlock.hasResident()) {

			try {
				list.add("resident=" + townBlock.getResident().getName());
			} catch (NotRegisteredException e) {
			}
		}

		// type
		list.add("type=" + townBlock.getType().getId());

		// outpost
		list.add("outpost=" + Boolean.toString(townBlock.isOutpost()));

		/*
		 * Only include a permissions line IF the plot perms are custom.
		 */
		if (townBlock.isChanged()) {
			// permissions
			list.add("permissions=" + townBlock.getPermissions().toString());
		}

		// Have permissions been manually changed
		list.add("changed=" + Boolean.toString(townBlock.isChanged()));

		list.add("locked=" + Boolean.toString(townBlock.isLocked()));

		/*
		 *  Make sure we only save in async
		 */
		this.queryQueue.add(new FlatFile_Task(list, getTownBlockFilename(townBlock)));

		return true;

	}

	@Override
	public boolean saveRegenList() {

		BufferedWriter fout = null;

		try {
			fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "regen.txt"));
			for (PlotBlockData plot : new ArrayList<>(TownyRegenAPI.getPlotChunks().values()))
				fout.write(plot.getWorldName() + "," + plot.getX() + "," + plot.getZ() + newLine);

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving Error: Exception while saving regen file");
			e.printStackTrace();
			return false;

		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException ignore) {
				}
			}
		}

		return true;

	}

	@Override
	public boolean saveSnapshotList() {

		BufferedWriter fout = null;

		try {
			fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "snapshot_queue.txt"));
			while (TownyRegenAPI.hasWorldCoords()) {
				WorldCoord worldCoord = TownyRegenAPI.getWorldCoord();
				fout.write(worldCoord.getWorldName() + "," + worldCoord.getX() + "," + worldCoord.getZ() + newLine);
			}

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving Error: Exception while saving snapshot_queue file");
			e.printStackTrace();
			return false;

		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException ignore) {
				}
			}
		}

		return true;

	}

	/**
	 * Load townblocks according to the given line Townblock: x,y,forSale Eg:
	 * townBlocks=world:10,11;10,12,true;|nether:1,1|
	 *
	 * @param line
	 * @param town
	 * @param resident
	 */
	@Deprecated
	public void utilLoadTownBlocks(String line, Town town, Resident resident) {

		String[] worlds = line.split("\\|");
		for (String w : worlds) {
			String[] split = w.split(":");
			if (split.length != 2) {
				TownyMessaging.sendErrorMsg("[Warning] " + town.getName() + " BlockList does not have a World or data.");
				continue;
			}
			try {
				TownyWorld world = getWorld(split[0]);
				for (String s : split[1].split(";")) {
					String blockTypeData = null;
					int indexOfType = s.indexOf("[");
					if (indexOfType != -1) { //is found
						int endIndexOfType = s.indexOf("]");
						if (endIndexOfType != -1) {
							blockTypeData = s.substring(indexOfType + 1, endIndexOfType);
						}
						s = s.substring(endIndexOfType + 1);
					}
					String[] tokens = s.split(",");
					if (tokens.length < 2)
						continue;
					try {
						int x = Integer.parseInt(tokens[0]);
						int z = Integer.parseInt(tokens[1]);

						try {
							world.newTownBlock(x, z);
						} catch (AlreadyRegisteredException e) {
						}
						TownBlock townblock = world.getTownBlock(x, z);

						if (town != null)
							townblock.setTown(town);

						if (resident != null && townblock.hasTown())
							townblock.setResident(resident);

						if (blockTypeData != null) {
							utilLoadTownBlockTypeData(townblock, blockTypeData);
						}

						//if present set the plot price
						if (tokens.length >= 3) {
							if (tokens[2].equals("true"))
								townblock.setPlotPrice(town.getPlotPrice());
							else
								townblock.setPlotPrice(Double.parseDouble(tokens[2]));
						}

					} catch (NumberFormatException | NotRegisteredException e) {
					}
				}
			} catch (NotRegisteredException e) {
				// Continue; No longer necessary it's last statement!
			}
		}
	}

	@Deprecated
	public void utilLoadTownBlockTypeData(TownBlock townBlock, String data) {

		String[] tokens = data.split(",");

		// Plot Type
		if (tokens.length >= 1)
			townBlock.setType(Integer.valueOf(tokens[0]));

		// Outpost or normal plot.
		if (tokens.length >= 2)
			townBlock.setOutpost(tokens[1].equalsIgnoreCase("1") ? true : false);
	}

	@Deprecated
	public String utilSaveTownBlocks(List<TownBlock> townBlocks) {

		HashMap<TownyWorld, ArrayList<TownBlock>> worlds = new HashMap<>();
		String out = "";

		// Sort all town blocks according to what world its in
		for (TownBlock townBlock : townBlocks) {
			TownyWorld world = townBlock.getWorld();
			if (!worlds.containsKey(world))
				worlds.put(world, new ArrayList<>());
			worlds.get(world).add(townBlock);
		}

		for (TownyWorld world : worlds.keySet()) {
			out += world.getName() + ":";
			for (TownBlock townBlock : worlds.get(world)) {
				out += "[" + townBlock.getType().getId();
				out += "," + (townBlock.isOutpost() ? "1" : "0");
				out += "]" + townBlock.getX() + "," + townBlock.getZ() + "," + townBlock.getPlotPrice() + ";";
			}
			out += "|";

		}

		return out;
	}

	/**
	 * Save PlotBlockData
	 *
	 * @param plotChunk
	 * @return true if saved
	 */
	@Override
	public boolean savePlotData(PlotBlockData plotChunk) {

		FileMgmt.checkFolders(new String[] { rootFolder + dataFolder + FileMgmt.fileSeparator() + "plot-block-data" + FileMgmt.fileSeparator() + plotChunk.getWorldName() });

		DataOutputStream fout = null;
		String path = getPlotFilename(plotChunk);

		try {
			fout = new DataOutputStream(new FileOutputStream(path));

			switch (plotChunk.getVersion()) {

			case 1:
			case 2:
				/*
				 * New system requires pushing
				 * version data first
				 */
				fout.write("VER".getBytes(Charset.forName("UTF-8")));
				fout.write(plotChunk.getVersion());

				break;

			default:

			}

			// Push the plot height, then the plot block data types.
			fout.writeInt(plotChunk.getHeight());
			for (int block : new ArrayList<>(plotChunk.getBlockList())) {
				fout.writeInt(block);
			}

		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Saving Error: Exception while saving PlotBlockData file (" + path + ")");
			e.printStackTrace();
			return false;
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException ignore) {
				}
			}
		}
		return true;

	}

	/**
	 * Load PlotBlockData
	 *
	 * @param worldName
	 * @param x
	 * @param z
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
	 * @param townBlock
	 * @return PlotBlockData or null
	 */
	@Override
	public PlotBlockData loadPlotData(TownBlock townBlock) {

		String fileName = getPlotFilename(townBlock);

		int value;

		if (isFile(fileName)) {
			PlotBlockData plotBlockData = new PlotBlockData(townBlock);
			List<Integer> IntArr = new ArrayList<>();
			int version = 0;

			DataInputStream fin = null;

			try {
				fin = new DataInputStream(new FileInputStream(fileName));

				//read the first 3 characters to test for version info
				byte[] key = new byte[3];
				fin.read(key, 0, 3);
				String test = new String(key);

				switch (elements.fromString(test)) {

				case VER:
					// Read the file version
					version = fin.read();
					plotBlockData.setVersion(version);

					// next entry is the plot height
					plotBlockData.setHeight(fin.readInt());
					break;

				default:
					/*
					 * no version field so set height
					 * and push rest to queue
					 */
					plotBlockData.setVersion(version);
					// First entry is the plot height
					plotBlockData.setHeight(key[0]);
					IntArr.add((int) key[1]);
					IntArr.add((int) key[2]);
				}

				/*
				 * Load plot block data based upon the stored version number.
				 */
				switch (version) {

				default:
				case 1:

					// load remainder of file
					while ((value = fin.read()) >= 0) {
						IntArr.add(value);
					}

					break;

				case 2:

					// load remainder of file
					while ((value = fin.readInt()) >= 0) {
						IntArr.add(value);
					}

					break;

				}


			} catch (EOFException e) {
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fin != null) {
					try {
						fin.close();
					} catch (IOException ignore) {
					}
				}
			}

			plotBlockData.setBlockList(IntArr);
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
		if (file.exists())
			file.delete();
	}
}
