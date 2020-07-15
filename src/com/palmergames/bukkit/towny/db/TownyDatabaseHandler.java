package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeletePlayerEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.event.RenameResidentEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author ElgarL
 * 
 */
public abstract class TownyDatabaseHandler extends TownyDataSource {
	final String rootFolderPath;
	final String dataFolderPath;
	final String settingsFolderPath;
	final String logFolderPath;
	final String backupFolderPath;
	
	public TownyDatabaseHandler(Towny plugin, TownyUniverse universe) {
		super(plugin, universe);
		this.rootFolderPath = universe.getRootFolder();
		this.dataFolderPath = rootFolderPath + File.separator + "data";
		this.settingsFolderPath = rootFolderPath + File.separator + "settings";
		this.logFolderPath = rootFolderPath + File.separator + "logs";
		this.backupFolderPath = rootFolderPath + File.separator + "backup";
	}
	
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

		List<Resident> invited = new ArrayList<>();
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

		List<Resident> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getResident(name));
			} catch (NotRegisteredException ignored) {
			}
		return matches;
	}

	@Override
	public List<Resident> getResidents() {

		return new ArrayList<>(universe.getResidentMap().values());
	}

	@Override
	public Resident getResident(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterPlayerName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
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

		List<Town> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException ignored) {
			}
		return matches;
	}

	@Override
	public List<Town> getTowns() {

		return new ArrayList<>(universe.getTownsMap().values());
	}

	@Override
	public Town getTown(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}

		if (!hasTown(name))
			throw new NotRegisteredException(String.format("The town '%s' is not registered.", name));

		return universe.getTownsMap().get(name);
	}

	@Override
	public Town getTown(UUID uuid) throws NotRegisteredException {
		String name = null;
		for (Town town : this.getTowns()) {
			if (uuid.equals(town.getUuid())) {
				name = town.getName();
			}
		}

		if (name == null) {
			throw new NotRegisteredException(String.format("The town with uuid '%s' is not registered.", uuid));
		}
		
		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}

		return universe.getTownsMap().get(name);
	}
	
	public PlotGroup getPlotObjectGroup(String townName, UUID groupID) {
		return universe.getGroup(townName, groupID);
	}

	@Override
	public List<Nation> getNations(String[] names) {

		List<Nation> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException ignored) {
			}
		return matches;
	}

	@Override
	public List<Nation> getNations() {

		return new ArrayList<>(universe.getNationsMap().values());
	}

	@Override
	public Nation getNation(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}

		if (!hasNation(name))
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return universe.getNationsMap().get(name.toLowerCase());
	}

	@Override
	public Nation getNation(UUID uuid) throws NotRegisteredException {
		String name = null;
		for (Nation nation : this.getNations()) {
			if (uuid.equals(nation.getUuid())) {
				name = nation.getName();
			}
		}

		if (name == null) {
			throw new NotRegisteredException(String.format("The town with uuid '%s' is not registered.", uuid));
		}
		
		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}
		
		return universe.getNationsMap().get(name);
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

		return new ArrayList<>(universe.getWorldMap().values());
	}

	/**
	 * Returns the world a town belongs to
	 * 
	 * @param townName Town to check world of
	 * @return TownyWorld for this town.
	 */
	@Override
	public TownyWorld getTownWorld(String townName) {

		for (TownyWorld world : universe.getWorldMap().values()) {
			if (world.hasTown(townName))
				return world;
		}

		// If this has failed the Town has no land claimed at all but should be given a world regardless.
		return universe.getDataSource().getWorlds().get(0);
	}

	@Override
	public void removeResident(Resident resident) {

		// Remove resident from towns' outlawlists.
		for (Town townOutlaw : getTowns()) {
			if (townOutlaw.hasOutlaw(resident)) {
				townOutlaw.removeOutlaw(resident);
				saveTown(townOutlaw);
			}
		}

		// Remove resident from residents' friendslists.
		List<Resident> toSave = new ArrayList<>();
		for (Resident toCheck : universe.getResidentMap().values()) {		
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
			if (toCheck.hasFriend(resident)) {
				TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
				toCheck.removeFriend(resident);
				toSave.add(toCheck);
			}
		}
		for (Resident toCheck : toSave)
			saveResident(toCheck);
		
		Town town = null;

		if (resident.hasTown())
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e1) {
				e1.printStackTrace();
			}

		if (town != null) {
			resident.removeTown();
		}

		// Delete the residents file.
		deleteResident(resident);
		// Remove the residents record from memory.
		universe.getResidentMap().remove(resident.getName().toLowerCase());
		universe.getResidentsTrie().removeKey(resident.getName());

		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
			resident.getAccount().removeAccount();

		plugin.deleteCache(resident.getName());
		
		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident.getName()));
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {

		TownPreUnclaimEvent event = new TownPreUnclaimEvent(townBlock);
		BukkitTools.getPluginManager().callEvent(event);
		
		if (event.isCancelled())
			return;
		
		Town town = null;
//		Resident resident = null;                   - Removed in 0.95.2.5
//		try {
//			resident = townBlock.getResident();
//		} catch (NotRegisteredException ignored) {
//		}
		try {
			town = townBlock.getTown();
		} catch (NotRegisteredException ignored) {
		}

		TownyUniverse.getInstance().removeTownBlock(townBlock);
		deleteTownBlock(townBlock);

//		if (resident != null)           - Removed in 0.95.2.5, residents don't store townblocks in them.
//			saveResident(resident);

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
		// Raise an event to signal the unclaim
		BukkitTools.getPluginManager().callEvent(new TownUnclaimEvent(town, townBlock.getWorldCoord()));
	}

	@Override
	public void removeTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks()))
			removeTownBlock(townBlock);
	}

	@Override
	public Collection<TownBlock> getAllTownBlocks() {
		return TownyUniverse.getInstance().getTownBlocks().values();
	}
	
	public List<PlotGroup> getAllPlotGroups() {
		return new ArrayList<>(universe.getGroups());
	}
	
	public void newPlotGroup(PlotGroup group) {
		universe.getGroups().add(group);
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
		universe.getResidentsTrie().addKey(filteredName);
	}

	@Override
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getTownsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		universe.getTownsMap().put(filteredName.toLowerCase(), new Town(filteredName));
		universe.getTownsTrie().addKey(filteredName);
	}

	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (universe.getNationsMap().containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		universe.getNationsMap().put(filteredName.toLowerCase(), new Nation(filteredName));
		universe.getNationsTrie().addKey(filteredName);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException {
		
		/*
		 * try {
		 * filteredName = checkAndFilterName(name);
		 * } catch (InvalidNameException e) {
		 * throw new NotRegisteredException(e.getMessage());
		 * }
		 */
		if (universe.getWorldMap().containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		universe.getWorldMap().put(name.toLowerCase(), new TownyWorld(name));
	}

	@Override
	public void removeTown(Town town) {
		
		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);
		BukkitTools.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled())
			return;
		
		removeTownBlocks(town);

		List<Resident> toSave = new ArrayList<>(town.getResidents());
		TownyWorld townyWorld = town.getHomeblockWorld();

		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				// Although the town might believe it is in the nation, it doesn't mean the nation thinks so.
				if (nation.hasTown(town)) {
					nation.removeTown(town);
				}
			}
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		for (Resident resident : toSave) {
			resident.clearModes();
			resident.removeTown();
		}
		
		// Look for residents inside of this town's jail and free them
		for (Resident jailedRes : TownyUniverse.getInstance().getJailedResidentMap()) {
			if (jailedRes.hasJailTown(town.getName())) {
                jailedRes.setJailed(jailedRes, 0, town);
                saveResident(jailedRes);
            }
		}

		if (TownyEconomyHandler.isActive())
			try {
				town.getAccount().payTo(town.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.getAccount().removeAccount();
			} catch (Exception ignored) {
			}

		try {
			townyWorld.removeTown(town);
		} catch (NotRegisteredException e) {
			// Must already be removed
		}
		saveWorld(townyWorld);
		
		universe.getTownsTrie().removeKey(town.getName());
		universe.getTownsMap().remove(town.getName().toLowerCase());
		plugin.resetCache();
		deleteTown(town);
		saveTownList();
		
		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town.getName()));
	}

	@Override
	public void removeNation(Nation nation) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation.getName());
		BukkitTools.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled())
			return;

		//search and remove from all ally/enemy lists
		List<Nation> toSaveNation = new ArrayList<>();
		for (Nation toCheck : new ArrayList<>(universe.getNationsMap().values()))
			if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
				try {
					if (toCheck.hasAlly(nation))
						toCheck.removeAlly(nation);
					else
						toCheck.removeEnemy(nation);

					toSaveNation.add(toCheck);
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
			}

		for (Nation toCheck : toSaveNation)
			saveNation(toCheck);

		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive())
			try {
				nation.getAccount().payTo(nation.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.getAccount().removeAccount();
			} catch (Exception ignored) {
			}

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<>(nation.getTowns());
		nation.clear();

		universe.getNationsTrie().removeKey(nation.getName().toLowerCase());
		universe.getNationsMap().remove(nation.getName().toLowerCase());

		for (Town town : toSave) {

			/*
			 * Remove all resident titles before saving the town itself.
			 */
			List<Resident> titleRemove = new ArrayList<>(town.getResidents());

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

		List<Town> townFilter = new ArrayList<>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}

	@Override
	public List<Resident> getResidentsWithoutTown() {

		List<Resident> residentFilter = new ArrayList<>();
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

			List<Resident> toSave = new ArrayList<>(town.getResidents());
			boolean isCapital = false;
			Nation nation = null;
			double townBalance = 0.0;
			oldName = town.getName();

			// Save the towns bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					townBalance = town.getAccount().getHoldingBalance();					
					if (TownySettings.isEcoClosedEconomyEnabled()){
						town.getAccount().pay(townBalance, "Town Rename");
					} 
					town.getAccount().removeAccount();
					
				} catch (EconomyException ignored) {
				}
			UUID oldUUID = town.getUuid();
			long oldregistration = town.getRegistered();

			// Store the nation in case we have to update the capitol
			if (town.hasNation()) {
				nation = town.getNation();
				isCapital = town.isCapital();
			}

			TownyWorld world = town.getHomeblockWorld();
			world.removeTown(town);
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
			universe.getTownsTrie().removeKey(town.getName());
			universe.getTownsMap().remove(town.getName().toLowerCase());
			town.setName(filteredName);
			universe.getTownsMap().put(filteredName.toLowerCase(), town);
			universe.getTownsTrie().addKey(filteredName);
			world.addTown(town);

			// If this was a nation capitol
			if (isCapital) {
				nation.setCapital(town);
			}
			town.setUuid(oldUUID);
			town.setRegistered(oldregistration);
			if (TownySettings.isUsingEconomy()) {
				try {
					town.getAccount().setName(TownySettings.getTownAccountPrefix() + town.getName());
					town.getAccount().setBalance(townBalance, "Rename Town - Transfer to new account");
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
			
			if (town.hasObjectGroups())
				for (PlotGroup pg : town.getPlotObjectGroups()) {
					pg.setTown(town);
					savePlotGroup(pg);
				}

			saveTown(town);
			saveTownList();
			savePlotGroupList();
			saveWorld(town.getHomeblockWorld());

			if (nation != null) {
				saveNation(nation);
			}

		} finally {
			lock.unlock();
		}

		BukkitTools.getPluginManager().callEvent(new RenameTownEvent(oldName, town));
	}
		

	@SuppressWarnings("unlikely-arg-type")
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

			List<Town> toSave = new ArrayList<>(nation.getTowns());
			double nationBalance = 0.0;

			// Save the nations bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					nationBalance = nation.getAccount().getHoldingBalance();
					if (TownySettings.isEcoClosedEconomyEnabled()){
						nation.getAccount().pay(nationBalance, "Nation Rename");
					}
					nation.getAccount().removeAccount();
					
				} catch (EconomyException ignored) {
				}

			UUID oldUUID = nation.getUuid();
			long oldregistration = nation.getRegistered();

			//Tidy up old files
			deleteNation(nation);

			/*
			 * Remove the old nation from the nationsMap
			 * and rename to the new name
			 */
			oldName = nation.getName();
			universe.getNationsMap().remove(oldName.toLowerCase());
			universe.getNationsTrie().removeKey(oldName);
			nation.setName(filteredName);
			universe.getNationsMap().put(filteredName.toLowerCase(), nation);
			universe.getNationsTrie().addKey(filteredName);

			if (TownyEconomyHandler.isActive()) {
				try {
					nation.getAccount().setName(TownySettings.getNationAccountPrefix() + nation.getName());
					nation.getAccount().setBalance(nationBalance, "Rename Nation - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

			nation.setUuid(oldUUID);
			nation.setRegistered(oldregistration);

			for (Town town : toSave) {
				saveTown(town);
			}

			saveNation(nation);
			saveNationList();

			//search and update all ally/enemy lists
			Nation oldNation = new Nation(oldName);
			List<Nation> toSaveNation = new ArrayList<>(getNations());
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
	}

	@Override
	public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException {
		// Create new one
		group.setName(newName);
		
		// Save
		savePlotGroup(group);
		savePlotGroupList();

		// Delete the old group file.
		deletePlotGroup(group);
	}

	@Override
	public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException {
		
		lock.lock();
		
		String oldName = resident.getName();
		
		try {
			
			//data needed for a new resident
			double balance = 0.0D;
			Town town = null;
			long registered;
			long lastOnline;
			UUID uuid = null;
			boolean isMayor;
			boolean isJailed;
			boolean isNPC;
			int JailSpawn;
			
			if(TownyEconomyHandler.getVersion().startsWith("iConomy 5") && TownySettings.isUsingEconomy()){
				try {
					balance = resident.getAccount().getHoldingBalance();
					resident.getAccount().removeAccount();
				} catch (EconomyException ignored) {
				}				
			} else {
				resident.getAccount().setName(newName);
			}
			
			//get data needed for resident
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
			if (resident.hasUUID())
				uuid = resident.getUUID();
			isMayor = resident.isMayor();
			isNPC = resident.isNPC();
			isJailed = resident.isJailed();			
			JailSpawn = resident.getJailSpawn();
			
			if (resident.isJailed()) {
				try {
					universe.getJailedResidentMap().remove(universe.getDataSource().getResident(oldName));
					universe.getJailedResidentMap().add(universe.getDataSource().getResident(newName));
				} catch (Exception ignored) {
				}
			}
				
			
			//delete the resident and tidy up files
			deleteResident(resident);
		
			//remove old resident from residentsMap
			//rename the resident
			universe.getResidentMap().remove(oldName.toLowerCase());
			universe.getResidentsTrie().removeKey(oldName);
			resident.setName(newName);
			universe.getResidentMap().put(newName.toLowerCase(), resident);
			universe.getResidentsTrie().addKey(newName);
			
			//add everything back to the resident
			if (TownyEconomyHandler.getVersion().startsWith("iConomy 5") && TownySettings.isUsingEconomy()) {
				try {
					resident.getAccount().setName(resident.getName());
					resident.getAccount().setBalance(balance, "Rename Player - Transfer to new account");
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
			try {
				resident.setTownRanks(townRanks);
			} catch (ConcurrentModificationException ignored) {
				// If this gets tripped by TownyNameUpdater in the future we will at least not be deleting anyone, they just won't have their townranks.
			}
			resident.setRegistered(registered);
			resident.setLastOnline(lastOnline);
			if (uuid != null)
				resident.setUUID(uuid);
			if(isMayor){
				try {
					town.setMayor(resident);
				} catch (TownyException ignored) {
				}
			}
			if (isNPC)
				resident.setNPC(true);
			resident.setJailed(isJailed);
			resident.setJailSpawn(JailSpawn);
			
			//save stuff
			saveResident(resident);
			if(town != null){
			    saveTown(town);
		    }
			for(TownBlock tb: townBlocks){
				saveTownBlock(tb);				
			}
			
			//search and update all friends lists
			//followed by outlaw lists
			Resident oldResident = new Resident(oldName);
			List<Resident> toSaveResident = new ArrayList<>(getResidents());
			for (Resident toCheck : toSaveResident){
				if (toCheck.hasFriend(oldResident)) {
					toCheck.removeFriend(oldResident);
					toCheck.addFriend(resident);
				}
			}
			for (Resident toCheck : toSaveResident)
				saveResident(toCheck);
			
			List<Town> toSaveTown = new ArrayList<>(getTowns());
			for (Town toCheckTown : toSaveTown) {
				if (toCheckTown.hasOutlaw(oldResident)) {
					toCheckTown.removeOutlaw(oldResident);
					toCheckTown.addOutlaw(resident);
				}
			}
			for (Town toCheckTown : toSaveTown)
				saveTown(toCheckTown);	
		
		} finally {
			lock.unlock();			
		}
		
		BukkitTools.getPluginManager().callEvent(new RenameResidentEvent(oldName, resident));
	}
	
	/** 
	 * Merges the succumbingNation into the prevailingNation.
	 * 
	 * @param succumbingNation - Nation to be removed, towns put into prevailingNation.
	 * @param prevailingNation - Nation which survives, absorbs other nation's towns.
	 * @throws NotRegisteredException - Shouldn't happen.
	 * @throws AlreadyRegisteredException - Shouldn't happen. 
	 * 
	 * @author LlmDl
	 */
	public void mergeNation(Nation succumbingNation, Nation prevailingNation) throws NotRegisteredException, AlreadyRegisteredException {
		
		lock.lock();
		List<Town> towns = new ArrayList<>(succumbingNation.getTowns());
		Town lastTown = null;
		try {
			succumbingNation.getAccount().payTo(succumbingNation.getAccount().getHoldingBalance(), prevailingNation, "Nation merge bank accounts.");
			for (Town town : towns) {			
				lastTown = town;
				for (Resident res : town.getResidents()) {
					if (res.hasTitle() || res.hasSurname()) {
						res.setTitle("");
						res.setSurname("");
					}
					res.updatePermsForNationRemoval();
					saveResident(res);
				}
				succumbingNation.removeTown(town);
				prevailingNation.addTown(town);
				saveTown(town);
			}
		} catch (EconomyException ignored) {
		} catch (EmptyNationException en) {
			// This is the intended end-result of the merge.
			prevailingNation.addTown(lastTown);
			saveTown(lastTown);
			String name = en.getNation().getName();
			universe.getDataSource().removeNation(en.getNation());
			saveNation(prevailingNation);
			universe.getDataSource().saveNationList();
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), name));
			lock.unlock();
		}
	}
}
