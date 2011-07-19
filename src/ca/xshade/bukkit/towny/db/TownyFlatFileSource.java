package ca.xshade.bukkit.towny.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.util.FileMgmt;
import ca.xshade.util.KeyValueFile;
import ca.xshade.util.StringMgmt;


// TODO: Make sure the lack of a particular value doesn't error out the entire file

public class TownyFlatFileSource extends TownyDataSource {
	protected final String newLine = System.getProperty("line.separator");
	protected String rootFolder = "";
	protected String dataFolder = FileMgmt.fileSeparator() + "data";
	protected String settingsFolder = FileMgmt.fileSeparator() + "settings";

	@Override
	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
		this.rootFolder = universe.getRootFolder();

		// Create files and folders if non-existent
		try {
			FileMgmt.checkFolders(new String[]{ rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds" + FileMgmt.fileSeparator() + "deleted",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks"});
			FileMgmt.checkFiles(new String[]{
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt",
					rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"});
		} catch (IOException e) {
			System.out.println("[Towny] Error: Could not create flatfile default files and folders.");
		}
	}
	
	@Override
	public void backup() throws IOException {
		String backupType = TownySettings.getFlatFileBackupType();
		if (!backupType.equalsIgnoreCase("none")) {
			long t = System.currentTimeMillis();
			String newBackupFolder = rootFolder + FileMgmt.fileSeparator() + "backup" + FileMgmt.fileSeparator() + new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t) + " - " + Long.toString(t);
			FileMgmt.checkFolders(new String[]{ rootFolder, rootFolder + FileMgmt.fileSeparator() + "backup" });
			if (backupType.equalsIgnoreCase("folder")) {
				FileMgmt.checkFolders(new String[]{newBackupFolder});
				FileMgmt.copyDirectory(new File(rootFolder + dataFolder), new File(newBackupFolder));
				FileMgmt.copyDirectory(new File(rootFolder + settingsFolder), new File(newBackupFolder));
			} else if (backupType.equalsIgnoreCase("zip"))
				FileMgmt.zipDirectories(new File[]{new File(rootFolder + dataFolder), new File(rootFolder + settingsFolder)}, new File(newBackupFolder + ".zip"));
			else
				throw new IOException("Unsupported flatfile backup type (" + backupType + ")");
		}
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
		return rootFolder + dataFolder + FileMgmt.fileSeparator() +  "worlds" + FileMgmt.fileSeparator() + world.getName() + ".txt";
	}
	
	

	/*
	 * Load keys
	 */

	@Override
	public boolean loadResidentList() {
		sendDebugMsg("Loading Resident List");
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newResident(line);
			fin.close();
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			confirmContinuation(e.getMessage() + " | Continuing will delete it's data.");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean loadTownList() {
		sendDebugMsg("Loading Town List");
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newTown(line);
			fin.close();
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			confirmContinuation(e.getMessage() + " | Continuing will delete it's data.");
		
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean loadNationList() {
		sendDebugMsg("Loading Nation List");
		String line;
		BufferedReader fin;

		try {
			fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		try {
			while ((line = fin.readLine()) != null)
				if (!line.equals(""))
					universe.newNation(line);
			fin.close();
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			confirmContinuation(e.getMessage() + " | Continuing will delete it's data.");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public boolean loadWorldList() {
		sendDebugMsg("Loading World List");
		if (plugin != null)
			return loadServerWorldsList();
		else {
			String line;
			BufferedReader fin;

			try {
				fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			}
			try {
				while ((line = fin.readLine()) != null)
					if (!line.equals(""))
						universe.newWorld(line);
				fin.close();

			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
				confirmContinuation(e.getMessage() + " | Continuing will delete it's data.");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	/*
	 * Load individual towny object
	 */

	@Override
	public boolean loadResident(Resident resident) {
		String line;
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
				
				
				line = kvFile.get("town");
				if (line != null)
					resident.setTown(universe.getTown(line));

				line = kvFile.get("friends");
				if (line != null) {
					String[] tokens = line.split(",");
					for (String token : tokens) {
						Resident friend = universe.getResident(token);
						if (friend != null)
							resident.addFriend(friend);
					}
				}
				
				line = kvFile.get("protectionStatus");
				if (line != null)
					resident.setPermissions(line);

				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, null, resident);

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading resident file " + resident.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	@Override
	public boolean loadTown(Town town) {
		String line;
		String[] tokens;
		String path = getTownFilename(town);
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);

				line = kvFile.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident resident = universe.getResident(token);
						if (resident != null)
							town.addResident(resident);
					}
				}

				line = kvFile.get("mayor");
				if (line != null)
					town.setMayor(universe.getResident(line));

				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null)
							town.addAssistant(assistant);
					}
				}

				town.setTownBoard(kvFile.get("townBoard"));

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

				line = kvFile.get("plotPrice");
				if (line != null)
					try {
						town.setPlotPrice(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPlotPrice(0);
					}
				
				line = kvFile.get("hasUpkeep");
				if (line != null)
					try {
						town.setHasUpkeep(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("taxes");
				if (line != null)
					try {
						town.setTaxes(Integer.parseInt(line));
					} catch (Exception e) {
						town.setTaxes(0);
					}
				
				line = kvFile.get("plotTax");
				if (line != null)
					try {
						town.setPlotTax(Integer.parseInt(line));
					} catch (Exception e) {
						town.setPlotTax(0);
					}

				line = kvFile.get("pvp");
				if (line != null)
					try {
						town.setPVP(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("mobs");
				if (line != null)
					try {
						town.setHasMobs(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}
				
				line = kvFile.get("public");
				if (line != null)
					try {
						town.setPublic(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("explosion");
				if (line != null)
					try {
						town.setBANG(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

                line = kvFile.get("taxpercent");
				if (line != null)
					try {
						town.setTaxPercentage(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("fire");
				if (line != null)
					try {
						town.setFire(Boolean.parseBoolean(line));
					} catch (NumberFormatException nfe) {
					} catch (Exception e) {
					}

				line = kvFile.get("townBlocks");
				if (line != null)
					utilLoadTownBlocks(line, town, null);

				line = kvFile.get("homeBlock");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 3)
						try {
							TownyWorld world = universe.getWorld(tokens[0]);
							int x = Integer.parseInt(tokens[1]);
							int z = Integer.parseInt(tokens[2]);
							TownBlock homeBlock = world.getTownBlock(x, z);
							town.setHomeBlock(homeBlock);
						} catch (NumberFormatException e) {
							System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid location.");
						} catch (NotRegisteredException e) {
							System.out.println("[Towny] [Warning] " + town.getName() + " homeBlock tried to load invalid world.");
						} catch (TownyException e) {
							System.out.println("[Towny] [Warning] " + town.getName() + " does not have a home block.");
						}
				}

				line = kvFile.get("spawn");
				if (line != null) {
					tokens = line.split(",");
					if (tokens.length == 4)
						try {
							World world = plugin.getServerWorld(tokens[0]);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							double z = Double.parseDouble(tokens[3]);
							town.setSpawn(new Location(world, x, y, z));
						} catch (NumberFormatException e) {
						} catch (NotRegisteredException e) {
						} catch (NullPointerException e) {
						} catch (TownyException e) {
							System.out.println("[Towny] [Warning] " + town.getName() + " does not have a spawn point.");
						}
				}

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading town file " + town.getName());
				e.printStackTrace();
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
						Town town = universe.getTown(token);
						if (town != null)
							nation.addTown(town);
					}
				}

				line = kvFile.get("capital");
				if (line != null)
					nation.setCapital(universe.getTown(line));

				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = universe.getResident(token);
						if (assistant != null)
							nation.addAssistant(assistant);
					}
				}

				line = kvFile.get("allies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation friend = universe.getNation(token);
						if (friend != null)
							nation.setAliegeance("ally", friend);
					}
				}

				line = kvFile.get("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation enemy = universe.getNation(token);
						if (enemy != null)
							nation.setAliegeance("enemy", enemy);
					}
				}

				line = kvFile.get("taxes");
				if (line != null)
					try {
						nation.setTaxes(Integer.parseInt(line));
					} catch (Exception e) {
						nation.setTaxes(0);
					}
					
				line = kvFile.get("neutral");
				if (line != null)
					try {
						nation.setNeutral(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading nation file " + nation.getName());
				e.printStackTrace();
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
			FileMgmt.checkFiles(new String[]{path});
		} catch (IOException e1) {
			System.out.println("[Towny] Loading Error: Exception while reading file " + path);
			e1.printStackTrace();
		}
		
		File fileWorld = new File(path);
		if (fileWorld.exists() && fileWorld.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = universe.getTown(token);
						if (town != null)
							world.addTown(town);
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
				
				line = kvFile.get("townmobs");
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
						world.setForceFire(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				line = kvFile.get("explosions");
				if (line != null)
					try {
						world.setForceExpl(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
				
				
				line = kvFile.get("usingDefault");
				if (line != null)
					try {
						world.setUsingDefault(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}
					
				if (!world.isUsingDefault()) {
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
							List<Integer> nums = new ArrayList<Integer>();
							for (String s: line.split(","))
								try {
									nums.add(Integer.parseInt(s));
								} catch (NumberFormatException e) {
								}
							world.setUnclaimedZoneIgnore(nums);
						} catch (Exception e) {
						}
				}
				
				line = kvFile.get("usingTowny");
				if (line != null)
					try {
						world.setUsingTowny(Boolean.parseBoolean(line));
					} catch (Exception e) {
					}

				// loadTownBlocks(world);

			} catch (Exception e) {
				System.out.println("[Towny] Loading Error: Exception while reading world file " + world.getName());
				e.printStackTrace();
				return false;
			}

			return true;
		} else
			return false;
	}

	public boolean loadTownBlocks(TownyWorld world) {
		String line;
		String[] tokens;

		try {
			BufferedReader fin = new BufferedReader(new FileReader(rootFolder + dataFolder + FileMgmt.fileSeparator() + "townblocks" + FileMgmt.fileSeparator() + world.getName() + ".csv"));
			while ((line = fin.readLine()) != null) {
				tokens = line.split(",");
				if (tokens.length >= 3) {
					Town town;
					try {
						town = universe.getTown(tokens[2]);

						// Towns can't control blocks in more than one world.
						if (town.getWorld() != world)
							continue;

					} catch (TownyException e) {
						// Town can be null
						// since we also check admin only toggle
						town = null;
					}

					int x = Integer.parseInt(tokens[0]);
					int z = Integer.parseInt(tokens[1]);

					world.newTownBlock(x, z);
					TownBlock townblock = world.getTownBlock(x, z);
					townblock.setTown(town);

					if (tokens.length >= 4)
						try {
							Resident resident = universe.getResident(tokens[3]);
							townblock.setResident(resident);
						} catch (TownyException e) {
						}
					if (tokens.length >= 5)
						try {
							townblock.setForSale(Boolean
									.parseBoolean(tokens[4]));
						} catch (Exception e) {
						}
				}
			}
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveResidentList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "residents.txt"));
			for (Resident resident : universe.getResidents())
				fout.write(resident.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving residents list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveTownList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "towns.txt"));
			for (Town town : universe.getTowns())
				fout.write(town.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving town list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveNationList() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "nations.txt"));
			for (Nation nation : universe.getNations())
				fout.write(nation.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving nation list file");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveWorldList() {
		try {
			
			System.out.print("[Towny] saveWorldList");
			
			BufferedWriter fout = new BufferedWriter(new FileWriter(rootFolder + dataFolder + FileMgmt.fileSeparator() + "worlds.txt"));
			for (TownyWorld world : universe.getWorlds())
				fout.write(world.getName() + newLine);
			fout.close();
			return true;
		} catch (Exception e) {
			System.out.println("[Towny] Loading Error: Exception while saving world list file");
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {
		try {
			String path = getResidentFilename(resident);
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			// Last Online
			fout.write("lastOnline=" + Long.toString(resident.getLastOnline()) + newLine);
			// Registered
			fout.write("registered=" + Long.toString(resident.getRegistered()) + newLine);
			if (resident.hasTown())
				fout.write("town=" + resident.getTown().getName() + newLine);
			// Friends
			fout.write("friends=");
			for (Resident friend : resident.getFriends())
				fout.write(friend.getName() + ",");
			fout.write(newLine);
			// TownBlocks
			fout.write("townBlocks=" + utilSaveTownBlocks(resident.getTownBlocks()) + newLine);
			// Plot Protection
			fout.write("protectionStatus=" + resident.getPermissions().toString() + newLine);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean saveTown(Town town) {
		BufferedWriter fout;
		String path = getTownFilename(town);
		try {
			fout = new BufferedWriter(new FileWriter(path));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			// Residents
			fout.write("residents=" + StringMgmt.join(town.getResidents(), ",") + newLine);
			// Mayor
			if (town.hasMayor())
				fout.write("mayor=" + town.getMayor().getName() + newLine);
			// Nation
			if (town.hasNation())
				fout.write("nation=" + town.getNation().getName() + newLine);
			// Assistants
			fout.write("assistants=");
			for (Resident assistant : town.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			// Town Board
			fout.write("townBoard=" + town.getTownBoard() + newLine);
			// Town Protection
			fout.write("protectionStatus=" + town.getPermissions().toString() + newLine);
			// Bonus Blocks
			fout.write("bonusBlocks=" + Integer.toString(town.getBonusBlocks()) + newLine);
			// Taxes
			fout.write("taxes=" + Integer.toString(town.getTaxes()) + newLine);
			// Plot Price
			fout.write("plotPrice=" + Integer.toString(town.getPlotPrice()) + newLine);
			// Plot Tax
			fout.write("plotTax=" + Integer.toString(town.getPlotTax()) + newLine);
			// Upkeep
			fout.write("hasUpkeep=" + Boolean.toString(town.hasUpkeep()) + newLine);
			// PVP
			fout.write("pvp=" + Boolean.toString(town.isPVP()) + newLine);
			// Mobs
			fout.write("mobs=" + Boolean.toString(town.hasMobs()) + newLine);
			// Public
			fout.write("public=" + Boolean.toString(town.isPublic()) + newLine);
			// Explosions
			fout.write("explosion=" + Boolean.toString(town.isBANG()) + newLine);
			// Firespread
			fout.write("fire=" + Boolean.toString(town.isFire()) + newLine);
            // Taxpercent
			fout.write("taxpercent=" + Boolean.toString(town.isTaxPercentage()) + newLine);
			// TownBlocks
			fout.write("townBlocks=" + utilSaveTownBlocks(town.getTownBlocks()) + newLine);
			// Home Block
			if (town.hasHomeBlock())
				fout.write("homeBlock=" + town.getHomeBlock().getWorld().getName() + ","
						+ Integer.toString(town.getHomeBlock().getX()) + ","
						+ Integer.toString(town.getHomeBlock().getZ()) + newLine);
			// Spawn
			if (town.hasSpawn())
				fout.write("spawn=" + town.getSpawn().getWorld().getName() + ","
						+ Double.toString(town.getSpawn().getX()) + ","
						+ Double.toString(town.getSpawn().getY()) + ","
						+ Double.toString(town.getSpawn().getZ()) + newLine);

			fout.close();
		} catch (Exception e) {
			try {
				fout.close();
			} catch (IOException ioe) {
			}
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean saveNation(Nation nation) {
		try {
			String path = getNationFilename(nation);
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : nation.getTowns())
				fout.write(town.getName() + ",");
			fout.write(newLine);
			if (nation.hasCapital())
				fout.write("capital=" + nation.getCapital().getName());
			fout.write(newLine);
			fout.write("assistants=");
			for (Resident assistant : nation.getAssistants())
				fout.write(assistant.getName() + ",");
			fout.write(newLine);
			fout.write("friends=");
			for (Nation allyNation : nation.getAllies())
				fout.write(allyNation.getName() + ",");
			fout.write(newLine);
			fout.write("enemies=");
			for (Nation enemyNation : nation.getEnemies())
				fout.write(enemyNation.getName() + ",");
			fout.write(newLine);
			// Taxes
			fout.write("taxes=" + Integer.toString(nation.getTaxes()) + newLine);
			// Neutral
			fout.write("neutral=" + Boolean.toString(nation.isNeutral()) + newLine);

			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean saveWorld(TownyWorld world) {
		try {
			sendDebugMsg("Saving world - " + getWorldFilename(world));
			
			String path = getWorldFilename(world);
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			
			// Towns
			fout.write("towns=");
			for (Town town : world.getTowns()){
				sendDebugMsg("   Town - " + town.getName());
				fout.write(town.getName() + ",");
			}
				
			fout.write(newLine);

			// PvP
			fout.write("pvp=" + Boolean.toString(world.isPVP()) + newLine);
			// Force PvP
			fout.write("forcepvp=" + Boolean.toString(world.isForcePVP()) + newLine);
			// Claimable
			fout.write("claimable=" + Boolean.toString(world.isClaimable()) + newLine);
			// has monster spawns			
			fout.write("worldmobs=" + Boolean.toString(world.hasWorldMobs()) + newLine);
			// force town mob spawns			
			fout.write("forcetownmobs=" + Boolean.toString(world.isForceTownMobs()) + newLine);
			// has firespread enabled
			fout.write("forcefirespread=" + Boolean.toString(world.isForceFire()) + newLine);
			// has explosions enabled
			fout.write("forceexplosions=" + Boolean.toString(world.isForceExpl()) + newLine);
			// Using Default
			fout.write("usingDefault=" + Boolean.toString(world.isUsingDefault()) + newLine);
			// Unclaimed Zone Build
			if (world.getUnclaimedZoneBuild() != null)
				fout.write("unclaimedZoneBuild=" + Boolean.toString(world.getUnclaimedZoneBuild()) + newLine);
			// Unclaimed Zone Destroy
			if (world.getUnclaimedZoneDestroy() != null)
				fout.write("unclaimedZoneDestroy=" + Boolean.toString(world.getUnclaimedZoneDestroy()) + newLine);
			// Unclaimed Zone Switch
			if (world.getUnclaimedZoneSwitch() != null)
				fout.write("unclaimedZoneSwitch=" + Boolean.toString(world.getUnclaimedZoneSwitch()) + newLine);
			// Unclaimed Zone Item Use
			if (world.getUnclaimedZoneItemUse() != null)
				fout.write("unclaimedZoneItemUse=" + Boolean.toString(world.getUnclaimedZoneItemUse()) + newLine);
			// Unclaimed Zone Name
			if (world.getUnclaimedZoneName() != null)
				fout.write("unclaimedZoneName=" + world.getUnclaimedZoneName() + newLine);
			// Unclaimed Zone Name
			if (world.getUnclaimedZoneIgnoreIds() != null)
				fout.write("unclaimedZoneIgnoreIds=" + StringMgmt.join(world.getUnclaimedZoneIgnoreIds(), ",") + newLine);
			// Using Towny
			fout.write("usingTowny=" + Boolean.toString(world.isUsingTowny()) + newLine);
			
			fout.close();

			// saveTownBlocks(world);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * public boolean saveTownBlocks(TownyWorld world) { try { BufferedWriter
	 * fout = new BufferedWriter(new FileWriter(rootFolder +
	 * dataFolder + "/townblocks/"+world.getName()+".csv")); for (TownBlock townblock :
	 * world.getTownBlocks()) { String line = townblock.getX() + "," +
	 * Long.toString(townblock.getZ()); line += ","; if (townblock.hasTown())
	 * line += townblock.getTown().getName(); line += ","; if
	 * (townblock.hasResident()) line += townblock.getResident().getName(); line
	 * += "," + Boolean.toString(townblock.isForSale()); fout.write(line +
	 * newLine); } fout.close(); return true; } catch (Exception e) {
	 * System.out.println("[Towny] Loading Error: Exception while saving town blocks list file");
	 * e.printStackTrace();
	 * return false; } }
	 */

	/**
	 * Load townblocks according to the given line Townblock: x,y,forSale Eg:
	 * townBlocks=world:10,11;10,12,true;|nether:1,1|
	 * 
	 * @param line
	 * @param town
	 * @param resident
	 */

	public void utilLoadTownBlocks(String line, Town town, Resident resident) {
		String[] worlds = line.split("\\|");
		for (String w : worlds) {
			String[] split = w.split(":");
			if (split.length != 2)
				continue;
			try {
				TownyWorld world = universe.getWorld(split[0]);
				for (String s : split[1].split(";")) {
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

						if (tokens.length >= 3)
							townblock.setForSale(true); //Automatically assume the townblock is for sale
					} catch (NumberFormatException e) {
					} catch (NotRegisteredException e) {
					}
				}
			} catch (NotRegisteredException e) {
				continue;
			}
		}
	}

	public String utilSaveTownBlocks(List<TownBlock> townBlocks) {
		HashMap<TownyWorld, ArrayList<TownBlock>> worlds = new HashMap<TownyWorld, ArrayList<TownBlock>>();
		String out = "";

		// Sort all town blocks according to what world its in
		for (TownBlock townBlock : townBlocks) {
			TownyWorld world = townBlock.getWorld();
			if (!worlds.containsKey(world))
				worlds.put(world, new ArrayList<TownBlock>());
			worlds.get(world).add(townBlock);
		}

		for (TownyWorld world : worlds.keySet()) {
			out += world.getName() + ":";
			for (TownBlock townBlock : worlds.get(world))
				out += townBlock.getX() + "," + townBlock.getZ() + (townBlock.isForSale() ? ",true" : "") + ";";
			out += "|";
		}

		return out;
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
		if (file.exists()){
			try {
				FileMgmt.moveFile(file, ("deleted"));
			} catch (IOException e) {
				System.out.println("[Towny] Error moving Town txt file.");
			}
		}
	}

	@Override
	public void deleteNation(Nation nation) {
		File file = new File(getNationFilename(nation));
		if (file.exists()){
			try {
				FileMgmt.moveFile(file, ("deleted"));
			} catch (IOException e) {
				System.out.println("[Towny] Error moving Nation txt file.");
			}
		}
	}

	@Override
	public void deleteWorld(TownyWorld world) {
		File file = new File(getWorldFilename(world));
		if (file.exists()){
			try {
				FileMgmt.moveFile(file, ("deleted"));
			} catch (IOException e) {
				System.out.println("[Towny] Error moving World txt file.");
			}
		}
	}
}