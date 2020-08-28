package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
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
	public final String dataFolderPath;
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
	public Collection<Resident> getResidents(Player player, String[] names) {
		return universe.getResidents(player, names);
	}

	@Override
	public Collection<Resident> getResidents(String[] names) {
		return universe.getResidents(names);
	}

	@Override
	public Collection<Resident> getResidents() {
		return universe.getResidents();
	}

	@Override
	public Resident getResident(String name) throws NotRegisteredException {
		return universe.getResident(name);
	}

	@Override
	public Collection<Town> getTowns(String[] names) {
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
	public Collection<Nation> getNations(String[] names) {
		return universe.getNations(names);
	}

	@Override
	public Collection<Nation> getNations() {
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
	public Collection<TownyWorld> getWorlds() {
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
		universe.removeResident(resident);
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {
		universe.unclaimTownBlock(townBlock);
	}

	@Override
	public void removeTownBlocks(Town town) {
		universe.unclaimAllTownBlocks(town);
	}

	@Override
	public Collection<TownBlock> getAllTownBlocks() {
		return TownyUniverse.getInstance()._getTownBlocks().values();
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

		Resident resident = new Resident(UUID.randomUUID());
		resident.setName(filteredName);
		
		universe.getResidentMap().put(filteredName.toLowerCase(), resident);
		universe.getResidentsTrie().addKey(filteredName);
		
		// Start New DB - Add to new datastructures.
		universe.addResident(resident);
		// End New DB
	}
	
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
		if (universe.hasTown(name))
			throw new AlreadyRegisteredException("The town " + name + " is already in use.");
		
		// Start New DB - Add to new datastructures.
		universe.newTown(name);
		// End New DB
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

		// Start New DB - Add to new datastructures.
		universe.newNation(name);
		// End New DB
	}

	@Override
	public void newWorld(String name) throws AlreadyRegisteredException {
		World world = Bukkit.getWorld(name);
		
		if (world == null) {
			return;
		}
		
		if (universe.getWorldMap().containsKey(name.toLowerCase()))
			throw new AlreadyRegisteredException("The world " + name + " is already in use.");

		TownyWorld townyWorld = new TownyWorld(world.getUID(), name);
		
		universe.getWorldMap().put(name.toLowerCase(), townyWorld);

		try {
			universe.newWorld(world.getUID(), world.getName());
		} catch (NotRegisteredException ignored) {}
	}

	@Override
	public void removeTown(Town town) {
		universe.removeTown(town);
	}

	@Override
	public void removeNation(Nation nation) {
		universe.removeNation(nation);
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
		try {
			succumbingNation.getAccount().payTo(succumbingNation.getAccount().getHoldingBalance(), prevailingNation, "Nation merge bank accounts.");
			for (Town town : towns) {
				for (Resident res : town.getResidents()) {
					if (res.hasTitle() || res.hasSurname()) {
						res.setTitle("");
						res.setSurname("");
					}
					res.updatePermsForNationRemoval();
					saveResident(res);
				}
				town.removeNation();
				town.setNation(prevailingNation);
			}
		} catch (EconomyException ignored) {
		}
	}
}
