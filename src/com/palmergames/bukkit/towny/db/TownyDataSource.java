package com.palmergames.bukkit.towny.db;

import java.io.IOException;
import java.util.Scanner;

import org.bukkit.World;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
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
	protected TownySettings settings;
	protected Towny plugin;
	protected boolean firstRun = false;

	public void initialize(Towny plugin, TownyUniverse universe) {
		this.universe = universe;
		this.plugin = plugin;
	}
	
	public void backup() throws IOException {
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
			plugin.sendDebugMsg(msg);
		else
			System.out.println("[Towny] Debug: " + msg);
	}

	public boolean loadAll() {
		return loadWorldList() && loadNationList() && loadTownList() && loadResidentList()
			&& loadWorlds() && loadNations() && loadTowns() && loadResidents();
	}

	public boolean saveAll() {
		return saveWorldList() && saveNationList() && saveTownList() && saveResidentList()
			&& saveWorlds() && saveNations() && saveTowns() && saveResidents();
	}

	abstract public boolean loadResidentList();
	abstract public boolean loadTownList();
	abstract public boolean loadNationList();
	
	abstract public boolean loadResident(Resident resident);
	abstract public boolean loadTown(Town town);
	abstract public boolean loadNation(Nation nation);
	abstract public boolean loadWorld(TownyWorld world);
	
	abstract public boolean saveResidentList();
	abstract public boolean saveTownList();
	abstract public boolean saveNationList();
	abstract public boolean saveWorldList();
	
	abstract public boolean saveResident(Resident resident);
	abstract public boolean saveTown(Town town);
	abstract public boolean saveNation(Nation nation);
	abstract public boolean saveWorld(TownyWorld world);

	abstract public void deleteResident(Resident resident);
	abstract public void deleteTown(Town town);
	abstract public void deleteNation(Nation nation);
	abstract public void deleteWorld(TownyWorld world);
	
	abstract public void deleteFile(String file);
	
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

	/*
	 * Load all of category
	 */

	public boolean loadResidents() {
		sendDebugMsg("Loading Residents");
		for (Resident resident : universe.getResidents())
			if (!loadResident(resident)) {
				System.out.println("[Towny] Loading Error: Could not read resident data '" + resident.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadTowns() {
		sendDebugMsg("Loading Towns");
		for (Town town : universe.getTowns())
			if (!loadTown(town)) {
				System.out.println("[Towny] Loading Error: Could not read town data " + town.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadNations() {
		sendDebugMsg("Loading Nations");
		for (Nation nation : universe.getNations())
			if (!loadNation(nation)) {
				System.out.println("[Towny] Loading Error: Could not read nation data '" + nation.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadWorlds() {
		sendDebugMsg("Loading Worlds");
		for (TownyWorld world : universe.getWorlds())
			if (!loadWorld(world)){
				System.out.println("[Towny] Loading Error: Could not read world data '" + world.getName() + "'.");
				return false;
			}
		return true;
	}

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {
		sendDebugMsg("Saving Residents");
		for (Resident resident : universe.getResidents())
			saveResident(resident);
		return true;
	}

	public boolean saveTowns() {
		sendDebugMsg("Saving Towns");
		for (Town town : universe.getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {
		sendDebugMsg("Saving Nations");
		for (Nation nation : universe.getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {
		sendDebugMsg("Saving Worlds");
		for (TownyWorld world : universe.getWorlds())
			saveWorld(world);
		return true;
	}
}