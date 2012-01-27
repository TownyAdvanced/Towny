package com.palmergames.bukkit.towny.db;

import java.io.IOException;
import java.util.ArrayList;
//import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
//import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;

/*
 * --- : Loading process : ---
 * 
 * Load all the names/keys for each world, nation, town, and resident.
 * Load each world, which loads it's town blocks.
 * Load nations, towns, and residents.
 */

/*
 * Loading Towns:
 * Make sure to load TownBlocks, then HomeBlock, then Spawn.
 */

public abstract class TownyDataSource {
	
	protected TownyUniverse universe;
	protected Towny plugin;
	protected boolean firstRun = true;

	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
	}

	public void backup() throws IOException {
	}

	public void cleanupBackups() {
	}

	public void deleteUnusedResidentFiles() {

	}

	public boolean confirmContinuation(String msg) {
		Boolean choice = null;
		String input = null;
		while (choice == null) {
			System.out.println(msg);
			System.out.print("    Continue (y/n): ");
			Scanner in = new Scanner(System.in);
			input = in.next();
			input = input.toLowerCase();
			if (input.equals("y") || input.equals("yes")) {
				in.close();
				return true;
			} else if (input.equals("n") || input.equals("no")) {
				in.close();
				return false;
			}
		}
		System.out.println("[Towny] Error recieving input, exiting.");
		return false;
	}

	public void sendDebugMsg(String msg) {
		if (plugin != null)
			TownyMessaging.sendDebugMsg(msg);
		else
			System.out.println("[Towny] Debug: " + msg);
	}

	public boolean loadAll() {
		return loadWorldList() && loadNationList() && loadTownList() && loadResidentList()
			&& loadWorlds() && loadNations() && loadTowns() && loadResidents()
			&& loadRegenList() && loadSnapshotList() && loadTownBlocks();
	}

	public boolean saveAll() {
		return saveWorldList() && saveNationList() && saveTownList() && saveResidentList()
			&& saveWorlds() && saveNations() && saveTowns() && saveResidents()
			&& saveRegenList() && saveSnapshotList();
	}

	abstract public boolean loadResidentList();
	abstract public boolean loadTownList();
	abstract public boolean loadNationList();
	abstract public boolean loadWorldList();
	abstract public boolean loadRegenList();
	abstract public boolean loadSnapshotList();
	abstract public boolean loadTownBlocks();
	
	abstract public boolean loadResident(Resident resident);
	abstract public boolean loadTown(Town town);
	abstract public boolean loadNation(Nation nation);
	abstract public boolean loadWorld(TownyWorld world);
	
	abstract public boolean saveResidentList();
	abstract public boolean saveTownList();
	abstract public boolean saveNationList();
	abstract public boolean saveWorldList();
	abstract public boolean saveRegenList();
	abstract public boolean saveSnapshotList();
	
	abstract public boolean saveResident(Resident resident);
	abstract public boolean saveTown(Town town);
	abstract public boolean saveNation(Nation nation);
	abstract public boolean saveWorld(TownyWorld world);
	
	abstract public boolean saveTownBlock(TownBlock townBlock);
	abstract public boolean savePlotData(PlotBlockData plotChunk);
	
	abstract public PlotBlockData loadPlotData(String worldName, int x, int z);
	abstract public PlotBlockData loadPlotData(TownBlock townBlock);
	
	abstract public void deletePlotData(PlotBlockData plotChunk);
	abstract public void deleteResident(Resident resident);
	abstract public void deleteTown(Town town);
	abstract public void deleteNation(Nation nation);
	abstract public void deleteWorld(TownyWorld world);
	abstract public void deleteTownBlock(TownBlock townBlock);
	abstract public void deleteFile(String file);

	/*
	public boolean loadWorldList() {
		return loadServerWorldsList();
	}
	
	public boolean loadServerWorldsList() {
		sendDebugMsg("Loading Server World List");
		for (World world : plugin.getServer().getWorlds())
			try {
				//String[] split = world.getName().split("/");
				//String worldName = split[split.length-1];
				//universe.newWorld(worldName);
				universe.newWorld(world.getName());
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
		return true;
	}
	*/

	/*
	 * Load all of category
	 */

	public boolean loadResidents() {
		sendDebugMsg("Loading Residents");

		List<Resident> toRemove = new ArrayList<Resident>();

		for (Resident resident : new ArrayList<Resident>(getResidents()))
			if (!loadResident(resident)) {
				System.out.println("[Towny] Loading Error: Could not read resident data '" + resident.getName() + "'.");
				toRemove.add(resident);
				//return false;
			}

		// Remove any resident which failed to load.
		for (Resident resident : toRemove) {
			System.out.println("[Towny] Loading Error: Removing resident data for '" + resident.getName() + "'.");
			removeResidentList(resident);
		}

		return true;
	}

	public boolean loadTowns() {
		sendDebugMsg("Loading Towns");
		for (Town town : getTowns())
			if (!loadTown(town)) {
				System.out.println("[Towny] Loading Error: Could not read town data " + town.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadNations() {
		sendDebugMsg("Loading Nations");
		for (Nation nation : getNations())
			if (!loadNation(nation)) {
				System.out.println("[Towny] Loading Error: Could not read nation data '" + nation.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadWorlds() {
		sendDebugMsg("Loading Worlds");
		for (TownyWorld world : getWorlds())
			if (!loadWorld(world)) {
				System.out.println("[Towny] Loading Error: Could not read world data '" + world.getName() + "'.");
				return false;
			} else {
				// Push all Towns belonging to this world
			}
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {
		sendDebugMsg("Saving Residents");
		for (Resident resident : getResidents())
			saveResident(resident);
		return true;
	}

	public boolean saveTowns() {
		sendDebugMsg("Saving Towns");
		for (Town town : getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {
		sendDebugMsg("Saving Nations");
		for (Nation nation : getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {
		sendDebugMsg("Saving Worlds");
		for (TownyWorld world : getWorlds())
			saveWorld(world);
		return true;
	}

	
	// Database functions
	abstract public List<Resident> getResidents(Player player, String[] names);
	abstract public List<Resident> getResidents();
	abstract public List<Resident> getResidents(String[] names);
	abstract public Resident getResident(String name) throws NotRegisteredException;
	
	abstract public void removeResidentList(Resident resident);
	abstract public void removeNation(Nation nation);
	abstract public boolean hasResident(String name);
	abstract public boolean hasTown(String name);
	abstract public boolean hasNation(String name);
	
	abstract public List<Town> getTowns(String[] names);
	abstract public List<Town> getTowns();
	abstract public Town getTown(String name) throws NotRegisteredException;
	abstract public List<Nation> getNations(String[] names);
	abstract public List<Nation> getNations();
	abstract public Nation getNation(String name) throws NotRegisteredException;
	abstract public TownyWorld getWorld(String name) throws NotRegisteredException;
	abstract public List<TownyWorld> getWorlds();
	abstract public TownyWorld getTownWorld(String townName);
	abstract public void removeResident(Resident resident);
	abstract public void removeTownBlock(TownBlock townBlock);
	abstract public void removeTownBlocks(Town town);
	abstract public List<TownBlock> getAllTownBlocks();
	abstract public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;
	abstract public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException;
	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;
	abstract public void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException;
	abstract public void removeTown(Town town);
	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public Set<String> getResidentKeys();
	abstract public Set<String> getTownsKeys();
	abstract public Set<String> getNationsKeys();
	
	abstract public List<Town> getTownsWithoutNation();
	abstract public List<Resident> getResidentsWithoutTown();
	
	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;
	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	
}