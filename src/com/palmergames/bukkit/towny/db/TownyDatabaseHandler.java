package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.RenameResidentEvent;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.palmergames.bukkit.towny.object.TownyObservableType.*;

/**
 * @author ElgarL
 * 
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {

	@Override
	public boolean hasResident(String name) {

		try {
			return TownySettings.isFakeResident(name) || universe.getResidentMap().containsKey(NameValidation.checkAndFilterPlayerName(name).toLowerCase());
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

		try {
			name = NameValidation.checkAndFilterPlayerName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasResident(name)) {

			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));

		} else if (TownySettings.isFakeResident(name)) {

			Resident resident = new Resident(name);
			resident.setNPC(true);

			return resident;

		}

		return universe.getResidentMap().get(name);

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

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasTown(name))
			throw new NotRegisteredException(String.format("The town '%s' is not registered.", name));

		return universe.getTownsMap().get(name);
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

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException e) {
		}

		if (!hasNation(name))
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return universe.getNationsMap().get(name.toLowerCase());
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
				if (town.hasNation())
					saveNation(town.getNation());

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

		saveTownBlockList();

		if (resident != null)
			saveResident(resident);
//		if (town != null)         		- Removed in 0.91.1.2, possibly fixing SQL database corruption 
//		    saveTown(town);				  occuring when towns are deleted. 

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
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();

		try {

			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				throw new NotRegisteredException(e.getMessage());
			}

			if (universe.getTownsMap().containsKey(filteredName.toLowerCase()))
				throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

			universe.getTownsMap().put(filteredName.toLowerCase(), new Town(filteredName));

		} finally {
			lock.unlock();
		}

		universe.setChangedNotify(NEW_TOWN);
	}

	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();

		try {

			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				throw new NotRegisteredException(e.getMessage());
			}

			if (universe.getNationsMap().containsKey(filteredName.toLowerCase()))
				throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

			universe.getNationsMap().put(filteredName.toLowerCase(), new Nation(filteredName));

		} finally {
			lock.unlock();
		}
		universe.setChangedNotify(NEW_NATION);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException {

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
	public void removeTown(Town town) {

		removeTownBlocks(town);

		List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
		TownyWorld townyWorld = town.getWorld();

		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				nation.removeTown(town);

				saveNation(nation);
			}

			// Clear all town blocks so the sign removal triggers.
			removeTownBlocks(town);

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
			} catch (Exception e) {
			}

		universe.getTownsMap().remove(town.getName().toLowerCase());

		plugin.resetCache();

		deleteTown(town);
		saveTownList();
		try {
			townyWorld.removeTown(town);
		} catch (NotRegisteredException e) {
			// Must already be removed
		}
		saveWorld(townyWorld);

		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town.getName()));

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

		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive())
			try {
				nation.payTo(nation.getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.removeAccount();
			} catch (Exception e) {
			}

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<Town>(nation.getTowns());
		nation.clear();

		universe.getNationsMap().remove(nation.getName().toLowerCase());

		for (Town town : toSave) {

			/*
			 * Remove all resident titles before saving the town itself.
			 */
			List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

			for (Resident res : titleRemove) {
				if (res.hasTitle() || res.hasSurname()) {
					res.setTitle("");
					res.setSurname("");
					saveResident(res);
				}
			}

			saveTown(town);
		}

		plugin.resetCache();
		saveNationList();

		BukkitTools.getPluginManager().callEvent(new DeleteNationEvent(nation.getName()));

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

		lock.lock();

		String oldName;

		try {

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
			oldName = town.getName();

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
			}

			if (TownySettings.isUsingEconomy()) {
				//TODO
				try {
					town.setBalance(townBalance, "Rename Town - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

			for (Resident resident : toSave) {
				saveResident(resident);
			}

			//search and update all resident's jailTown with new name.

            for (Resident toCheck : getResidents()){
                    if (toCheck.hasJailTown(oldName)) {
                        toCheck.setJailTown(newName);
                        
                        saveResident(toCheck);
                    }
            }
            
			// Update all townBlocks with the new name

			for (TownBlock townBlock : town.getTownBlocks()) {
				//townBlock.setTown(town);
				saveTownBlock(townBlock);
			}

			saveTown(town);
			saveTownList();
			saveWorld(town.getWorld());

			if (nation != null) {
				saveNation(nation);
			}

		} finally {
			lock.unlock();
		}

		BukkitTools.getPluginManager().callEvent(new RenameTownEvent(oldName, town));

		universe.setChangedNotify(RENAME_TOWN);
	}

	@Override
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		lock.lock();

		String oldName;

		try {

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
			oldName = nation.getName();
			universe.getNationsMap().remove(oldName.toLowerCase());
			nation.setName(filteredName);
			universe.getNationsMap().put(filteredName.toLowerCase(), nation);

			if (TownyEconomyHandler.isActive()) {
				//TODO
				try {
					nation.setBalance(nationBalance, "Rename Nation - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

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

		} finally {
			lock.unlock();
		}

		BukkitTools.getPluginManager().callEvent(new RenameNationEvent(oldName, nation));

		universe.setChangedNotify(RENAME_NATION);
	}

	@Override
	public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException {
		
		lock.lock();
		
		String oldName = resident.getName();
		
		try {
			
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(newName);				
			} catch (InvalidNameException e) {
				throw new NotRegisteredException(e.getMessage());
			}
			
			//data needed for a new resident
			double balance = 0.0D;
			Town town = null;
			long registered = 0L;		
			long lastOnline = 0L;
			boolean isMayor = false;
			boolean isJailed = false;
			int JailSpawn = 0;
			
			boolean transferBalance = !TownyEconomyHandler.hasEconomyAccount(filteredName);
			
			//get data needed for resident
			if(transferBalance && TownySettings.isUsingEconomy()){
				try {
					balance = resident.getHoldingBalance();
					resident.removeAccount();
				} catch (EconomyException e) {
				}				
			}
			List<Resident> friends = resident.getFriends();
			List<String> nationRanks = resident.getNationRanks();
			TownyPermission permissions = resident.getPermissions();
			String surname = resident.getSurname();
			String title = resident.getTitle();
			if (resident.hasTown()) {
				town = resident.getTown();
			}
			List<TownBlock> townBlocks = resident.getTownBlocks();
			List<String> townRanks = resident.getTownRanks();
			registered = resident.getRegistered();			
			lastOnline = resident.getLastOnline();
			isMayor = resident.isMayor();
			isJailed = resident.isJailed();
			JailSpawn = resident.getJailSpawn();
			
			//delete the resident and tidy up files
			deleteResident(resident);
		
			//remove old resident from residentsMap
			//rename the resident
			universe.getResidentMap().remove(oldName.toLowerCase());
			resident.setName(filteredName);
			universe.getResidentMap().put(filteredName.toLowerCase(), resident);
			
			//add everything back to the resident
			if (transferBalance && TownySettings.isUsingEconomy()) {
				//TODO
				try {
					resident.setBalance(balance, "Rename Player - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}				
			}
			resident.setFriends(friends);
			resident.setNationRanks(nationRanks);
			resident.setPermissions(permissions.toString()); //not sure if this will work
			resident.setSurname(surname);
			resident.setTitle(title);
			resident.setTown(town);
			resident.setTownblocks(townBlocks);
			resident.setTownRanks(townRanks);
			resident.setRegistered(registered);
			resident.setLastOnline(lastOnline);
			if(isMayor){
				try {
					town.setMayor(resident);
				} catch (TownyException e) {					
				}
			}
			resident.setJailed(isJailed);
			resident.setJailSpawn(JailSpawn);
			
			//save stuff
			saveResidentList();
			saveResident(resident);
			if(town !=null){
			    saveTown(town);
		    }
			for(TownBlock tb: townBlocks){
				saveTownBlock(tb);				
			}
			
			//search and update all friends lists
			Resident oldResident = new Resident(oldName);
			List<Resident> toSaveResident = new ArrayList<Resident>(getResidents());
			for (Resident toCheck : toSaveResident){
				if (toCheck.hasFriend(oldResident)) {
					try {
						toCheck.removeFriend(oldResident);
						toCheck.addFriend(resident);
					} catch (NotRegisteredException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			for (Resident toCheck : toSaveResident)
				saveResident(toCheck);
		
		} finally {
			lock.unlock();			
		}
		
		BukkitTools.getPluginManager().callEvent(new RenameResidentEvent(oldName, resident));
		
		universe.setChangedNotify(RENAME_RESIDENT);
		
	}
	
}
