package com.palmergames.bukkit.towny.db;

import static com.palmergames.bukkit.towny.object.TownyObservableType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.EmptyNationException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.WarSpoils;



public abstract class TownyDatabaseHandler extends TownyDataSource {

	@Override
	public boolean hasResident(String name) {
		try {
			return universe.getResidentMap().containsKey(universe.checkAndFilterName(name).toLowerCase());
		} catch (InvalidNameException e) {
			return false;
		}
	}
	
	@Override
	public boolean hasTown(String name) {
		return universe.getTownsMap().containsKey(name.toLowerCase());
	}

	@Override
	public boolean hasNation(String name) {
		return universe.getNationsMap().containsKey(name.toLowerCase());
	}	
	
	@Override
	public List<Resident> getResidents(Player player, String[] names) {
	    List<Resident> invited = new ArrayList<Resident>();
	    for (String name : names)
	        try {
	            Resident target = getResident(name);
	            invited.add(target);
	        } catch (TownyException x) {
	            TownyMessaging.sendErrorMsg(player, x.getMessage());
	        }
	    return invited;
	}
	
	@Override
	public List<Resident> getResidents(String[] names) {
		List<Resident> matches = new ArrayList<Resident>();
		for (String name : names)
			try {
				matches.add(getResident(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	@Override
	public List<Resident> getResidents() {
		return new ArrayList<Resident>(universe.getResidentMap().values());
	}
	
	@Override
	public Resident getResident(String name) throws NotRegisteredException {
		Resident resident = null;
		try {
			resident = universe.getResidentMap().get(universe.checkAndFilterName(name).toLowerCase());
		} catch (InvalidNameException e) {
		}
		if (resident == null)
			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));

		return resident;
	}

	@Override
	public List<Town> getTowns(String[] names) {
		List<Town> matches = new ArrayList<Town>();
		for (String name : names)
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	@Override
	public List<Town> getTowns() {
		return new ArrayList<Town>(universe.getTownsMap().values());
	}
	
	@Override
	public Town getTown(String name) throws NotRegisteredException {
		Town town = universe.getTownsMap().get(name.toLowerCase());
		if (town == null)
			throw new NotRegisteredException(String.format("The town '%s' is not registered.", name));
		return town;
	}

	@Override
	public List<Nation> getNations(String[] names) {
		List<Nation> matches = new ArrayList<Nation>();
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}
	
	@Override
	public List<Nation> getNations() {
		return new ArrayList<Nation>(universe.getNationsMap().values());
	}

	@Override
	public Nation getNation(String name) throws NotRegisteredException {
		Nation nation = universe.getNationsMap().get(name.toLowerCase());
		if (nation == null)
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));
		return nation;
	}
	
	@Override
	public TownyWorld getWorld(String name) throws NotRegisteredException {
		TownyWorld world = universe.getWorldMap().get(name.toLowerCase());

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}
	
	@Override
	public List<TownyWorld> getWorlds() {
		return new ArrayList<TownyWorld>(universe.getWorldMap().values());
	}
	
	/**
	 * Returns the world a town belongs to
	 * 
	 * @param townName
	 * @return TownyWorld for this town.
	 */
	@Override
	public TownyWorld getTownWorld(String townName) {

		for (TownyWorld world : universe.getWorldMap().values()) {
			if (world.hasTown(townName))
				return world;
		}

		return null;
	}
	
	@Override
	public void removeResident(Resident resident) {

		Town town = null;

		if (resident.hasTown())
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		try {
			if (town != null) {
				town.removeResident(resident);
				saveTown(town);
			}
			resident.clear();
		} catch (EmptyTownException e) {
			removeTown(town);

		} catch (NotRegisteredException e) {
			// town not registered
			e.printStackTrace();
		}


		universe.setChangedNotify(REMOVE_RESIDENT);
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {
		Resident resident = null;
		Town town = null;
		try {
			resident = townBlock.getResident();
		} catch (NotRegisteredException e) {
		}
		try {
			town = townBlock.getTown();
		} catch (NotRegisteredException e) {
		}
		TownyWorld world = townBlock.getWorld();
		world.removeTownBlock(townBlock);

		saveWorld(world);
		deleteTownBlock(townBlock);

		if (resident != null)
			saveResident(resident);
		if (town != null)
			saveTown(town);

		if (townBlock.getWorld().isUsingPlotManagementDelete())
			TownyRegenAPI.addDeleteTownBlockIdQueue(townBlock.getWorldCoord());

		// Move the plot to be restored
		if (townBlock.getWorld().isUsingPlotManagementRevert()) {
			PlotBlockData plotData = TownyRegenAPI.getPlotChunkSnapshot(townBlock);
			if (plotData != null && !plotData.getBlockList().isEmpty()) {
				TownyRegenAPI.addPlotChunk(plotData, true);
			}
		}

		universe.setChangedNotify(REMOVE_TOWN_BLOCK);
	}
	
	@Override
	public void removeTownBlocks(Town town) {
		for (TownBlock townBlock : new ArrayList<TownBlock>(town.getTownBlocks()))
			removeTownBlock(townBlock);
	}

	@Override
	public List<TownBlock> getAllTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<TownBlock>();
		for (TownyWorld world : getWorlds())
			townBlocks.addAll(world.getTownBlocks());
		return townBlocks;
	}
	
	@Override
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = universe.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getResidentMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("A resident with the name " + filteredName + " is already in use.");

		universe.getResidentMap().put(filteredName.toLowerCase(), new Resident(filteredName));

		universe.setChangedNotify(NEW_RESIDENT);
	}

	@Override
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = universe.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getTownsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		universe.getTownsMap().put(filteredName.toLowerCase(), new Town(filteredName));

		universe.setChangedNotify(NEW_TOWN);
	}
	
	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = universe.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getNationsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		universe.getNationsMap().put(filteredName.toLowerCase(), new Nation(filteredName));

		universe.setChangedNotify(NEW_NATION);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName = name;
		/*
		try {
		        filteredName = checkAndFilterName(name);
		} catch (InvalidNameException e) {
		        throw new NotRegisteredException(e.getMessage());
		}
		*/
		if (universe.getWorldMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + filteredName + " is already in use.");

		universe.getWorldMap().put(filteredName.toLowerCase(), new TownyWorld(filteredName));

		universe.setChangedNotify(NEW_WORLD);
	}
	
	@Override
	public void removeResidentList(Resident resident) {
		String name = resident.getName();

		//search and remove from all friends lists
		List<Resident> toSave = new ArrayList<Resident>();

		for (Resident toCheck : new ArrayList<Resident>(universe.getResidentMap().values())) {
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
			if (toCheck.hasFriend(resident)) {
				try {
					TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
					toCheck.removeFriend(resident);
					toSave.add(toCheck);
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		for (Resident toCheck : toSave)
			saveResident(toCheck);

		//Wipe and delete resident
		try {
			resident.clear();
		} catch (EmptyTownException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		deleteResident(resident);

		universe.getResidentMap().remove(name.toLowerCase());
		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
			resident.removeAccount();
		plugin.deleteCache(name);
		saveResidentList();

	}
	
	@Override
	public void removeTown(Town town) {

		removeTownBlocks(town);

		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
		TownyWorld world = town.getWorld();

		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				nation.removeTown(town);

				saveNation(nation);
			}
			town.clear();
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (plugin.isEcoActive())
			try {
				town.payTo(town.getHoldingBalance(), new WarSpoils(), "Remove Town");
			} catch (EconomyException e) {
			}

		for (Resident resident : toSave) {
			removeResident(resident);
			saveResident(resident);
		}

		universe.getTownsMap().remove(town.getName().toLowerCase());
		// Clear accounts
		if (TownySettings.isUsingEconomy())
			town.removeAccount();
		plugin.updateCache();

		deleteTown(town);
		saveTownList();
		saveWorld(world);

		universe.setChangedNotify(REMOVE_TOWN);
	}
	
	@Override
	public void removeNation(Nation nation) {

		//search and remove from all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<Nation>();
		for (Nation toCheck : new ArrayList<Nation>(universe.getNationsMap().values()))
			if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
				try {
					if (toCheck.hasAlly(nation))
						toCheck.removeAlly(nation);
					else
						toCheck.removeEnemy(nation);

					toSaveNation.add(toCheck);
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		for (Nation toCheck : toSaveNation)
			saveNation(toCheck);

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<Town>(nation.getTowns());
		nation.clear();
		if (plugin.isEcoActive())
			try {
				nation.payTo(nation.getHoldingBalance(), new WarSpoils(), "Remove Nation");
			} catch (EconomyException e) {
			}
		universe.getNationsMap().remove(nation.getName().toLowerCase());
		// Clear accounts
		if (TownySettings.isUsingEconomy())
			nation.removeAccount();

		plugin.updateCache();
		for (Town town : toSave)
			saveTown(town);
		saveNationList();

			universe.setChangedNotify(REMOVE_NATION);
	}
	
	@Override
	public void removeWorld(TownyWorld world) throws UnsupportedOperationException {
		deleteWorld(world);
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getResidentKeys() {
		return universe.getResidentMap().keySet();
	}

	@Override
	public Set<String> getTownsKeys() {
		return universe.getTownsMap().keySet();
	}

	@Override
	public Set<String> getNationsKeys() {
		return universe.getNationsMap().keySet();
	}

	@Override
	public List<Town> getTownsWithoutNation() {
		List<Town> townFilter = new ArrayList<Town>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}

	@Override
	public List<Resident> getResidentsWithoutTown() {
		List<Resident> residentFilter = new ArrayList<Resident>();
		for (Resident resident : universe.getResidentMap().values())
			if (!resident.hasTown())
				residentFilter.add(resident);
		return residentFilter;
	}
	
	@Override
	public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = universe.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (hasTown(filteredName))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		// TODO: Delete/rename any invites.

		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());

		//Tidy up old files
		// Has to be done here else the town no longer exists and the move command may fail.
		deleteTown(town);

		String oldName = town.getName();
		universe.getTownsMap().remove(oldName.toLowerCase());
		town.setName(filteredName);

		universe.getTownsMap().put(filteredName.toLowerCase(), town);

		//Check if this is a nation capitol
		if (town.isCapital()) {
			Nation nation = town.getNation();
			nation.setCapital(town);
			saveNation(nation);
		}

		Town oldTown = new Town(oldName);

		try {
			town.pay(town.getHoldingBalance(), "Rename Town - Empty account of new town name.");
			oldTown.payTo(oldTown.getHoldingBalance(), town, "Rename Town - Transfer to new account");
		} catch (EconomyException e) {
		}

		for (Resident resident : toSave) {
			saveResident(resident);
		}

		saveTown(town);
		saveTownList();
		saveWorld(town.getWorld());

		universe.setChangedNotify(RENAME_TOWN);
	}

	@Override
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = universe.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (hasNation(filteredName))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		// TODO: Delete/rename any invites.

		List<Town> toSave = new ArrayList<Town>(nation.getTowns());

		String oldName = nation.getName();
		universe.getNationsMap().put(filteredName.toLowerCase(), nation);
		//Tidy up old files
		deleteNation(nation);

		universe.getNationsMap().remove(oldName.toLowerCase());
		nation.setName(filteredName);
		Nation oldNation = new Nation(oldName);

		if (plugin.isEcoActive())
			try {
				nation.pay(nation.getHoldingBalance(), "Rename Nation - Empty account of new nation name.");
				oldNation.payTo(oldNation.getHoldingBalance(), nation, "Rename Nation - Transfer to new account");
			} catch (EconomyException e) {
			}

		for (Town town : toSave) {
			saveTown(town);
		}

		saveNation(nation);
		saveNationList();

		//search and update all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<Nation>(getNations());
		for (Nation toCheck : toSaveNation)
			if (toCheck.hasAlly(oldNation) || toCheck.hasEnemy(oldNation)) {
				try {
					if (toCheck.hasAlly(oldNation)) {
						toCheck.removeAlly(oldNation);
						toCheck.addAlly(nation);
					} else {
						toCheck.removeEnemy(oldNation);
						toCheck.addEnemy(nation);
					}
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				toSave.remove(toCheck);

		for (Nation toCheck : toSaveNation)
			saveNation(toCheck);

		universe.setChangedNotify(RENAME_NATION);
	}
	
}