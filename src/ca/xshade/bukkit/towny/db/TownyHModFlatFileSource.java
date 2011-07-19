package ca.xshade.bukkit.towny.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.util.FileMgmt;
import ca.xshade.util.KeyValueFile;


// TODO: Make sure the lack of a particular value doesn't error out the entire file

public class TownyHModFlatFileSource extends TownyFlatFileSource {

	@Override
	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
		this.rootFolder = universe.getRootFolder();
		this.dataFolder = "/data-hmod";

		// Create files and folders if non-existent
		try {
			FileMgmt.checkFolders(new String[]{ rootFolder,
					rootFolder + dataFolder,
					rootFolder + dataFolder + "/residents",
					rootFolder + dataFolder + "/towns",
					rootFolder + dataFolder + "/nations"});
			FileMgmt.checkFiles(new String[]{
					rootFolder + dataFolder + "/residents.txt",
					rootFolder + dataFolder + "/towns.txt",
					rootFolder + dataFolder + "/nations.txt",
					rootFolder + dataFolder + "/townblocks.txt"});
		} catch (IOException e) {
			System.out.println("[Towny] Error: Could not create hmod-flatfile default files and folders.");
		}
	}
	
	@Override
	public void backup() throws IOException {
	}

	/*
	 * Load keys
	 */
	
	
	@Override
	public boolean loadWorldList() {
		plugin.sendDebugMsg("Loading World List");
		if (plugin != null)
			return loadServerWorldsList();
		return false;
	}

	
	@Override
	public boolean loadWorlds() {
		System.out.println("[Towny] [hMod Conversion] Town Blocks");
		String line;
		String[] tokens;
		
		//Default world is the first one loaded
		TownyWorld world = universe.getWorlds().toArray(new TownyWorld[0])[0];
		
		try {
			BufferedReader fin = new BufferedReader(new FileReader(rootFolder + dataFolder + "/townblocks.csv"));
			while ( (line = fin.readLine()) != null) {
				tokens = line.split(",");
				if (tokens.length >= 4)
					try {
						Town town = universe.getTown(tokens[2]);
					
					
						int x = Integer.parseInt(tokens[0]);
						int z = Integer.parseInt(tokens[1]);

						try {
							world.newTownBlock(x, z);
						} catch (AlreadyRegisteredException e) {
						}
						TownBlock townblock = world.getTownBlock(x, z);

						if (town != null)
							townblock.setTown(town);

						try {
							townblock.setResident(universe.getResident(tokens[3]));
						} catch (NotRegisteredException e) {
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (NotRegisteredException e) {
						e.printStackTrace();
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
	 * Load individual towny object
	 */

	@Override
	public boolean loadResident(Resident resident) {
		System.out.println("[Towny] [hMod Conversion] Resident: " + resident.getName());
		String line;
		String path = rootFolder + dataFolder + "/residents/" + resident.getName() + ".txt";
		File fileResident = new File(path);
		if (fileResident.exists() && fileResident.isFile()) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				resident.setLastOnline(Long.parseLong(kvFile.get("lastLogin")));
				
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
		System.out.println("[Towny] [hMod Conversion] Town: " + town.getName());
		String line;
		String[] tokens;
		String path = rootFolder + dataFolder + "/towns/" + town.getName() + ".txt";
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
		System.out.println("[Towny] [hMod Conversion] Nation: " + nation.getName());
		String line = "";
		String[] tokens;
		String path = rootFolder + dataFolder + "/nations/" + nation.getName() + ".txt";
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
		return false;
	}

	/*
	 * Save keys
	 */

	@Override
	public boolean saveResidentList() {
		return false;
	}

	@Override
	public boolean saveTownList() {
		return false;
	}

	@Override
	public boolean saveNationList() {
		return false;
	}

	@Override
	public boolean saveWorldList() {
		return false;
	}

	/*
	 * Save individual towny objects
	 */

	@Override
	public boolean saveResident(Resident resident) {
		return false;
	}

	@Override
	public boolean saveTown(Town town) {
		return false;
	}

	@Override
	public boolean saveNation(Nation nation) {
		return false;
	}

	@Override
	public boolean saveWorld(TownyWorld world) {
		return false;
	}
}