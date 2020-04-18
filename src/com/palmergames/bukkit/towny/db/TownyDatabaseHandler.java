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
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author ElgarL
 * 
 */
@Deprecated
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
		return universe.hasResident(name);
	}

	@Override
	public boolean hasTown(String name) {
		return universe.hasTown(name);
	}

	@Override
	public boolean hasNation(String name) {
		return universe.hasNation(name);
	}

	@Override
	public List<Resident> getResidents(Player player, String[] names) {
		return universe.getResidents(player, names);
	}

	@Override
	public List<Resident> getResidents(String[] names) {
		return universe.getResidents(names);
	}

	@Override
	public List<Resident> getResidents() {
		return universe.getResidents();
	}

	@Override
	public Resident getResident(String name) throws NotRegisteredException {
		return universe.getResident(name);
	}

	@Override
	public List<Town> getTowns(String[] names) {
		return universe.getTowns(names);
	}

	@Override
	public List<Town> getTowns() {
		return universe.getTowns();
	}

	@Override
	public Town getTown(String name) throws NotRegisteredException {
		return universe.getTown(name);
	}

	@Override
	public Town getTown(UUID uuid) throws NotRegisteredException {
		return universe.getTown(uuid);
	}
	
	public PlotGroup getPlotObjectGroup(String townName, UUID groupID) {
		return universe.getGroup(townName, groupID);
	}

	@Override
	public List<Nation> getNations(String[] names) {
		return universe.getNations(names);
	}

	@Override
	public List<Nation> getNations() {
		return universe.getNations();
	}

	@Override
	public Nation getNation(String name) throws NotRegisteredException {
		return universe.getNation(name);
	}

	@Override
	public Nation getNation(UUID uuid) throws NotRegisteredException {
		return universe.getNation(uuid);
	}

	@Override
	public TownyWorld getWorld(String name) throws NotRegisteredException {
		return universe.getWorld(name);
	}

	@Override
	public List<TownyWorld> getWorlds() {
		return universe.getWorlds();
	}

	/**
	 * Returns the world a town belongs to
	 * 
	 * @param townName Town to check world of
	 * @return TownyWorld for this town.
	 */
	@Override
	public TownyWorld getTownWorld(String townName) {
		return universe.getTownWorld(townName);
	}

	@Override
	public void removeResident(Resident resident) {

		Town town = null;

		if (resident.hasTown())
			try {
				town = resident.getTown();
			} catch (NotRegisteredException e1) {
				e1.printStackTrace();
			}

		try {
			if (town != null) {
				town.removeResident(resident);
				if (town.hasNation())
					saveNation(town.getNation());

				//saveTown(town);
			}
			resident.clear();
			
		} catch (EmptyTownException e) {
			removeTown(town);

		} catch (NotRegisteredException e) {
			// town not registered
			e.printStackTrace();
		}
		
		try {
			for (Town townOutlaw : getTowns()) {
				if (townOutlaw.hasOutlaw(resident)) {
					townOutlaw.removeOutlaw(resident);
					//saveTown(townOutlaw);
				}
			}
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

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
		return TownyUniverse.getInstance().getTownBlocks();
	}
	
	public List<PlotGroup> getAllPlotGroups() {
		return new ArrayList<>(universe.getGroups());
	}
	
	public void newPlotGroup(PlotGroup group) {
		universe.getGroups().add(group);
	}

	@Override
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
		// TODO Figure out how to fetch UUID from name
		throw new UnsupportedOperationException("Cannot fetch resident UUID!");
	}
	
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		universe.newTown(name);
	}

	@Override
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
		universe.newNation(name);
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException {
		World world = Bukkit.getWorld(name);
		
		if (world != null) {
			try {
				universe.newWorld(world.getUID(), world.getName());
			} catch (NotRegisteredException ignore) {
			}
		}
	}

	@Override
	public void removeResidentList(Resident resident) {

		String name = resident.getName();

		//search and remove from all friends lists
		List<Resident> toSave = new ArrayList<>();

		for (Resident toCheck : new ArrayList<>(universe.getResidentMap().values())) {
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
			if (toCheck.hasFriend(resident)) {
				try {
					TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
					toCheck.removeFriend(resident);
					toSave.add(toCheck);
				} catch (NotRegisteredException e) {
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
		universe.getResidentsTrie().removeKey(name);

		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
			resident.getAccount().removeAccount();

		plugin.deleteCache(name);
		saveResidentList();

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
					saveNation(nation);
				}
				town.setNation(null);
			}
			town.clear();
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		} catch (AlreadyRegisteredException ignored) {
			// This should only be happening when a town thinks it is in the nation, while the nation doesn't consider the town a member.
		}

		for (Resident resident : toSave) {
			resident.clearModes();
			try {
				town.removeResident(resident);
			} catch (NotRegisteredException | EmptyTownException ignored) {
			}
			saveResident(resident);
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

		//Delete nation and getString towns
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

			//saveTown(town);
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
		// Perform legacy name checks because they were previously handled by this method.
		String newFilteredName;
		try {
			newFilteredName = NameValidation.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (TownyUniverse.getInstance().hasTown(newFilteredName))
			throw new AlreadyRegisteredException("The town " + newFilteredName + " is already in use.");
		
		town.rename(newFilteredName);
	}
		

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {
		// Perform legacy name validation check
		String filteredNewName;

		try {
			filteredNewName = NameValidation.checkAndFilterName(newName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (hasNation(filteredNewName))
			throw new AlreadyRegisteredException("The nation " + filteredNewName + " is already in use.");
		
		nation.rename(filteredNewName);
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
		resident.rename(newName);
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
				//saveTown(town);
			}
		} catch (EconomyException ignored) {
		} catch (EmptyNationException en) {
			// This is the intended end-result of the merge.
			prevailingNation.addTown(lastTown);
			//saveTown(lastTown);
			String name = en.getNation().getName();
			universe.getDataSource().removeNation(en.getNation());
			saveNation(prevailingNation);
			universe.getDataSource().saveNationList();
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), name));
			lock.unlock();
		}
	}
}
