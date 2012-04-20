package com.palmergames.bukkit.towny.db;

import static com.palmergames.bukkit.towny.object.TownyObservableType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.NameValidation;

/**
 * @author ElgarL
 * 
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {

	@Override
	public synchronized boolean hasResident(String name) {

		try {
			return universe.getResidentMap().containsKey(NameValidation.checkAndFilterPlayerName(name).toLowerCase());
		} catch (InvalidNameException e) {
			return false;
		}
	}

	@Override
	public synchronized boolean hasTown(String name) {

		return universe.getTownsMap().containsKey(name.toLowerCase());
	}

	@Override
	public synchronized boolean hasNation(String name) {

		return universe.getNationsMap().containsKey(name.toLowerCase());
	}

	@Override
	public synchronized List<Resident> getResidents(Player player, String[] names) {

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
	public synchronized List<Resident> getResidents(String[] names) {

		List<Resident> matches = new ArrayList<Resident>();
		for (String name : names)
			try {
				matches.add(getResident(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}

	@Override
	public synchronized List<Resident> getResidents() {

		return new ArrayList<Resident>(universe.getResidentMap().values());
	}

	@Override
	public synchronized Resident getResident(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterPlayerName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasResident(name))
			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));

		return universe.getResidentMap().get(name);

	}

	@Override
	public synchronized List<Town> getTowns(String[] names) {

		List<Town> matches = new ArrayList<Town>();
		for (String name : names)
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}

	@Override
	public synchronized List<Town> getTowns() {

		return new ArrayList<Town>(universe.getTownsMap().values());
	}

	@Override
	public synchronized Town getTown(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasTown(name))
			throw new NotRegisteredException(String.format("The town '%s' is not registered.", name));

		return universe.getTownsMap().get(name);
	}

	@Override
	public synchronized List<Nation> getNations(String[] names) {

		List<Nation> matches = new ArrayList<Nation>();
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}

	@Override
	public synchronized List<Nation> getNations() {

		return new ArrayList<Nation>(universe.getNationsMap().values());
	}

	@Override
	public synchronized Nation getNation(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasNation(name))
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return universe.getNationsMap().get(name.toLowerCase());
	}

	@Override
	public synchronized TownyWorld getWorld(String name) throws NotRegisteredException {

		TownyWorld world = universe.getWorldMap().get(name.toLowerCase());

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}

	@Override
	public synchronized List<TownyWorld> getWorlds() {

		return new ArrayList<TownyWorld>(universe.getWorldMap().values());
	}

	/**
	 * Returns the world a town belongs to
	 * 
	 * @param townName
	 * @return TownyWorld for this town.
	 */
	@Override
	public synchronized TownyWorld getTownWorld(String townName) {

		for (TownyWorld world : universe.getWorldMap().values()) {
			if (world.hasTown(townName))
				return world;
		}

		return null;
	}

	@Override
	public synchronized void removeResident(Resident resident) {

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
	public synchronized void removeTownBlock(TownBlock townBlock) {

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
	public synchronized void removeTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<TownBlock>(town.getTownBlocks()))
			removeTownBlock(townBlock);
	}

	@Override
	public synchronized List<TownBlock> getAllTownBlocks() {

		List<TownBlock> townBlocks = new ArrayList<TownBlock>();
		for (TownyWorld world : getWorlds())
			townBlocks.addAll(world.getTownBlocks());
		return townBlocks;
	}

	@Override
	public synchronized void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getResidentMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("A resident with the name " + filteredName + " is already in use.");

		universe.getResidentMap().put(filteredName.toLowerCase(), new Resident(filteredName));

		universe.setChangedNotify(NEW_RESIDENT);
	}

	@Override
	public synchronized void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getTownsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		universe.getTownsMap().put(filteredName.toLowerCase(), new Town(filteredName));

		universe.setChangedNotify(NEW_TOWN);
	}

	@Override
	public synchronized void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getNationsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		universe.getNationsMap().put(filteredName.toLowerCase(), new Nation(filteredName));

		universe.setChangedNotify(NEW_NATION);
	}

	@Override
	public synchronized void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName = name;
		/*
		 * try {
		 * filteredName = checkAndFilterName(name);
		 * } catch (InvalidNameException e) {
		 * throw new NotRegisteredException(e.getMessage());
		 * }
		 */
		if (universe.getWorldMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + filteredName + " is already in use.");

		universe.getWorldMap().put(filteredName.toLowerCase(), new TownyWorld(filteredName));

		universe.setChangedNotify(NEW_WORLD);
	}

	@Override
	public synchronized void removeResidentList(Resident resident) {

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
		} catch (EmptyTownException ex) {
			removeTown(ex.getTown());
		}
		// Delete the residents file.
		deleteResident(resident);
		// Remove the residents record from memory.
		universe.getResidentMap().remove(name.toLowerCase());
		
		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
			resident.removeAccount();
		
		plugin.deleteCache(name);
		saveResidentList();

	}

	@Override
	public synchronized void removeTown(Town town) {

		removeTownBlocks(town);

		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
		TownyWorld townyWorld = town.getWorld();

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

		for (Resident resident : toSave) {
			removeResident(resident);
			saveResident(resident);
		}

		if (TownyEconomyHandler.isActive())
			try {
				town.payTo(town.getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.removeAccount();
			} catch (EconomyException e) {
			}

		universe.getTownsMap().remove(town.getName().toLowerCase());

		plugin.updateCache();

		deleteTown(town);
		saveTownList();
		try {
			townyWorld.removeTown(town);
		} catch (NotRegisteredException e) {
			// Must already be removed
		}
		saveWorld(townyWorld);

		universe.setChangedNotify(REMOVE_TOWN);
	}

	@Override
	public synchronized void removeNation(Nation nation) {

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

		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive())
			try {
				nation.payTo(nation.getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.removeAccount();
			} catch (EconomyException e) {
			}

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<Town>(nation.getTowns());
		nation.clear();

		universe.getNationsMap().remove(nation.getName().toLowerCase());

		for (Town town : toSave) {
			saveTown(town);
		}

		plugin.updateCache();
		saveNationList();

		universe.setChangedNotify(REMOVE_NATION);
	}

	@Override
	public synchronized void removeWorld(TownyWorld world) throws UnsupportedOperationException {

		deleteWorld(world);
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Set<String> getResidentKeys() {

		return universe.getResidentMap().keySet();
	}

	@Override
	public synchronized Set<String> getTownsKeys() {

		return universe.getTownsMap().keySet();
	}

	@Override
	public synchronized Set<String> getNationsKeys() {

		return universe.getNationsMap().keySet();
	}

	@Override
	public synchronized List<Town> getTownsWithoutNation() {

		List<Town> townFilter = new ArrayList<Town>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}

	@Override
	public synchronized List<Resident> getResidentsWithoutTown() {

		List<Resident> residentFilter = new ArrayList<Resident>();
		for (Resident resident : universe.getResidentMap().values())
			if (!resident.hasTown())
				residentFilter.add(resident);
		return residentFilter;
	}

	@Override
	public synchronized void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (hasTown(filteredName))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		// TODO: Delete/rename any invites.

		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
		Boolean isCapital = false;
		Nation nation = null;
		Double townBalance = 0.0;

		// Save the towns bank balance to set in the new account.
		// Clear accounts
		if (TownySettings.isUsingEconomy())
			try {
				townBalance = town.getHoldingBalance();
				town.removeAccount();
			} catch (EconomyException e) {
			}

		// Store the nation in case we have to update the capitol
		if (town.hasNation()) {
			nation = town.getNation();
			isCapital = town.isCapital();
		}

		/*
		 * Tidy up old files.
		 * Has to be done here else the town no longer exists
		 * and the file move command may fail.
		 */
		deleteTown(town);

		/*
		 * Remove the old town from the townsMap
		 * and rename to the new name
		 */
		universe.getTownsMap().remove(town.getName().toLowerCase());
		town.setName(filteredName);
		universe.getTownsMap().put(filteredName.toLowerCase(), town);

		// If this was a nation capitol
		if (isCapital) {
			nation.setCapital(town);
			saveNation(nation);
		}

		if (TownySettings.isUsingEconomy())
			town.setBalance(townBalance, "Rename Town - Transfer to new account");

		for (Resident resident : toSave) {
			saveResident(resident);
		}

		saveTown(town);
		saveTownList();
		saveWorld(town.getWorld());

		universe.setChangedNotify(RENAME_TOWN);
	}

	@Override
	public synchronized void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		String filteredName;

		try {
			filteredName = NameValidation.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (hasNation(filteredName))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		// TODO: Delete/rename any invites.

		List<Town> toSave = new ArrayList<Town>(nation.getTowns());
		Double nationBalance = 0.0;

		// Save the nations bank balance to set in the new account.
		// Clear accounts
		if (TownySettings.isUsingEconomy())
			try {
				nationBalance = nation.getHoldingBalance();
				nation.removeAccount();
			} catch (EconomyException e) {
			}

		//Tidy up old files
		deleteNation(nation);

		/*
		 * Remove the old nation from the nationsMap
		 * and rename to the new name
		 */
		String oldName = nation.getName();
		universe.getNationsMap().remove(oldName.toLowerCase());
		nation.setName(filteredName);
		universe.getNationsMap().put(filteredName.toLowerCase(), nation);

		if (TownyEconomyHandler.isActive())
			nation.setBalance(nationBalance, "Rename Nation - Transfer to new account");

		for (Town town : toSave) {
			saveTown(town);
		}

		saveNation(nation);
		saveNationList();

		//search and update all ally/enemy lists
		Nation oldNation = new Nation(oldName);
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