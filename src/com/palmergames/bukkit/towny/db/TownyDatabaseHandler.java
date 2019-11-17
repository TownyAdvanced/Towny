package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.*;
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
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.towny.war.siegewar.Siege;
import com.palmergames.bukkit.towny.war.siegewar.SiegeZone;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;

import java.util.*;

import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_WORLD;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_TOWN_BLOCK;
import static com.palmergames.bukkit.towny.object.TownyObservableType.RENAME_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.RENAME_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.RENAME_TOWN;

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
	public boolean hasSiegeZone(String name) {

		return universe.getSiegeZonesMap().containsKey(name.toLowerCase());
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
			} catch (NotRegisteredException e) {
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

		List<Town> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException e) {
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
		} catch (InvalidNameException e) {
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
		} catch (InvalidNameException e) {
		}

		return universe.getTownsMap().get(name);
	}

	@Override
	public List<Nation> getNations(String[] names) {

		List<Nation> matches = new ArrayList<>();
		for (String name : names)
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException e) {
			}
		return matches;
	}

	@Override
	public List<Nation> getNations() {

		return new ArrayList<>(universe.getNationsMap().values());
	}

	@Override
	public List<SiegeZone> getSiegeZones() {
		return new ArrayList<>(universe.getSiegeZonesMap().values());
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
		} catch (InvalidNameException e) {
		}
		
		return universe.getNationsMap().get(name);
	}


	public void newSiegeZone(String siegeZoneName) throws AlreadyRegisteredException {
		System.out.println("About to create new siege zone");
		String[] townAndNationArray = SiegeZone.generateTownAndNationName(siegeZoneName);
		newSiegeZone(townAndNationArray[0],townAndNationArray[1]);
	}

	@Override
	public void newSiegeZone(String attackingNationName,String defendingTownName) throws AlreadyRegisteredException {

		lock.lock();

		System.out.println("About to create new siege zone 2");

		try {
			String siegeZoneName = SiegeZone.generateName(attackingNationName, defendingTownName);

			System.out.println("About to create new siege zone with name: " +siegeZoneName);

			if(universe.getSiegeZonesMap().containsKey(siegeZoneName.toLowerCase()))
				throw new AlreadyRegisteredException("Siege Zone is already registered");

			Town town = universe.getTownsMap().get(defendingTownName.toLowerCase());
			Nation nation = universe.getNationsMap().get(attackingNationName.toLowerCase());
			SiegeZone siegeZone = new SiegeZone(nation, town);

			universe.getSiegeZonesMap().put(siegeZoneName.toLowerCase(), siegeZone);

		} finally {
			lock.unlock();
		}
		//universe.setChangedNotify(NEW_SIEGE);  //Todo - what is this - should I add it?
	}

	@Override
	public SiegeZone getSiegeZone(String nationName, String townName) throws NotRegisteredException  {
		String siegeZoneName = SiegeZone.generateName(nationName,townName);

		System.out.println("ZONENAME: " + siegeZoneName);
		System.out.println("ZONES IN UNIVERSE " + universe.getSiegeZonesMap().size());


		if(!universe.getSiegeZonesMap().containsKey(siegeZoneName.toLowerCase())) {
			throw new NotRegisteredException("Siege Zone not found");
		}
		return universe.getSiegeZonesMap().get(siegeZoneName.toLowerCase());
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

		return null;
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

				saveTown(town);
			}
			resident.clear();
			
			
			for (Town townOutlaw : TownyUniverse.getDataSource().getTowns()) {
				if (townOutlaw.hasOutlaw(resident)) {
					townOutlaw.removeOutlaw(resident);
					saveTown(townOutlaw);
				}
			}
			
		} catch (EmptyTownException e) {
			removeTown(town);

		} catch (NotRegisteredException e) {
			// town not registered
			e.printStackTrace();
		}
		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident.getName()));

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
		// Raise an event to signal the unclaim
		BukkitTools.getPluginManager().callEvent(new TownUnclaimEvent(town, townBlock.getWorldCoord()));
	}

	@Override
	public void removeTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks()))
			removeTownBlock(townBlock);
	}

	@Override
	public List<TownBlock> getAllTownBlocks() {

		List<TownBlock> townBlocks = new ArrayList<>();
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

		// Clear accounts
		if (TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
			resident.removeAccount();

		plugin.deleteCache(name);
		saveResidentList();

	}




	//@Override //todo - make me override
	public void ruinTown(Town town) {
		town.setRecentlyRuined(true);
		town.setRecentlyRuinedEndTime(System.currentTimeMillis() + 3000);

		List<SiegeZone> siegeZonesToDelete = new ArrayList<>();
		List<Nation> nationsToSave = new ArrayList<>();
		if(town.getSiege() != null) {
			for(Map.Entry<Nation,SiegeZone> entry: town.getSiege().getSiegeZones().entrySet()) {
				siegeZonesToDelete.add(entry.getValue());
				nationsToSave.add(entry.getKey());
			}
		}

		List<Resident> residentsToSave = new ArrayList<>(town.getResidents());
		TownyWorld townyWorld = town.getWorld();

		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				nation.removeTown(town);
				nationsToSave.add(nation);
			}

			// Clear all town blocks so the sign removal triggers.
			removeTownBlocks(town);

			town.clear();
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		for (Resident resident : residentsToSave) {
			resident.clearModes();
			removeResident(resident);
			saveResident(resident);
		}

		// Look for residents inside of this town's jail and free them
		// TODO: Perhaps in the future a new object JailedResidents can be used to make this searching much quicker.
		for (Resident jailedRes : getResidents()) {
			if (jailedRes.hasJailTown(town.getName())) {
				jailedRes.setJailed(BukkitTools.getPlayer(jailedRes.getName()), 0, town);
				saveResident(jailedRes);
			}
		}

		if (TownyEconomyHandler.isActive())
			try {
				town.payTo(town.getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.removeAccount();
			} catch (Exception e) {
			}

		//Remove items from universe
		universe.getTownsMap().remove(town.getName().toLowerCase());
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			universe.getSiegeZonesMap().remove(siegeZone.getName());
		}

		//Remove siege zones from DB
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			deleteSiegeZone(siegeZone);
		}
		//Save nations
		for(Nation nationToSave: nationsToSave) {
			saveNation(nationToSave);
		}

		plugin.resetCache();

		deleteTown(town);

		if(siegeZonesToDelete.size() >0){
			saveSiegeZoneList();
		}

		saveTownList();
		try {
			townyWorld.removeTown(town);
		} catch (NotRegisteredException e) {
			// Must already be removed
		}

		if(town.hasSiege()) {
			saveSiegeZoneList();
		}

		saveWorld(townyWorld);
	}

	public void removeRuinedTown(Town town) {
		//Calledby siege timer only
	}

	@Override
	public void removeTown(Town town) {
		BukkitTools.getPluginManager().callEvent(new PreDeleteTownEvent(town));

		boolean putTownIntoRuinedState = false;
		if(TownySettings.getWarSiegeEnabled()) {
			//Todo - add a config here
			putTownIntoRuinedState = true;
		}

		List<TownBlock> townBlocksToKeep = null;
		if(putTownIntoRuinedState) {
			townBlocksToKeep = town.getTownBlocks();
			town.setRecentlyRuined(true);
			town.setRecentlyRuinedEndTime(System.currentTimeMillis() + 3000);
			town.setPVP(true);
			town.setPermissions("");
		}

		List<SiegeZone> siegeZonesToDelete = new ArrayList<>();
		List<Nation> nationsToSave = new ArrayList<>();
		if(town.getSiege() != null) {
			for(Map.Entry<Nation,SiegeZone> entry: town.getSiege().getSiegeZones().entrySet()) {
				siegeZonesToDelete.add(entry.getValue());
				nationsToSave.add(entry.getKey());
			}
		}

		List<Resident> residentsToSave = new ArrayList<>(town.getResidents());
		TownyWorld townyWorld = town.getWorld();

		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				nation.removeTown(town);
				nationsToSave.add(nation);
			}

			// Clear all town blocks so the sign removal triggers.
			removeTownBlocks(town);
			town.clear();
		} catch (EmptyNationException e) {
			removeNation(e.getNation());
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		for (Resident resident : residentsToSave) {
			resident.clearModes();
			removeResident(resident);
			saveResident(resident);
		}
		
		// Look for residents inside of this town's jail and free them
		// TODO: Perhaps in the future a new object JailedResidents can be used to make this searching much quicker.
		for (Resident jailedRes : getResidents()) {
			if (jailedRes.hasJailTown(town.getName())) {
                jailedRes.setJailed(BukkitTools.getPlayer(jailedRes.getName()), 0, town);
                saveResident(jailedRes);
            }
		}

		if (TownyEconomyHandler.isActive())
			try {
				town.payTo(town.getHoldingBalance(), new WarSpoils(), "Remove Town");
				town.removeAccount();
			} catch (Exception e) {
			}

		//Remove items from universe
		universe.getTownsMap().remove(town.getName().toLowerCase());
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			universe.getSiegeZonesMap().remove(siegeZone.getName());
		}

		//Remove siege zones and nations from DB
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			deleteSiegeZone(siegeZone);
		}
		for(Nation nationToSave: nationsToSave) {
			saveNation(nationToSave);
		}

		plugin.resetCache();

		if(siegeZonesToDelete.size() >0){
			saveSiegeZoneList();
		}

		if(putTownIntoRuinedState) {
			//Re-add the zones back in
			town.setTownblocks(townBlocksToKeep);
		} else {
			deleteTown(town);
			saveTownList();
			try {
				townyWorld.removeTown(town);
			} catch (NotRegisteredException e) {
				// Must already be removed
			}
			saveWorld(townyWorld);
		}

		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town.getName()));

		universe.setChangedNotify(REMOVE_TOWN);
	}

	//Remove a particular siege, and all associated data
	@Override
	public void removeSiege(Siege siege) {

		//todo ????????? do we need this?
		//BukkitTools.getPluginManager().callEvent(new PreDeleteTownEvent(town));

		//Remove siege from siege
		siege.getDefendingTown().setSiege(null);

		List<SiegeZone> siegeZonesToRemove = new ArrayList<>();
		List<Nation> nationsToSave = new ArrayList<>();

		//Calculate zones to remove and nations to save
		for(Map.Entry<Nation,SiegeZone> entry: siege.getSiegeZones().entrySet()) {
			siegeZonesToRemove.add(entry.getValue());
			nationsToSave.add(entry.getKey());
		}

		//Remove siege zones from nations
		for(SiegeZone siegeZone: siegeZonesToRemove) {
			siegeZone.getAttackingNation().removeSiegeZone(siegeZone);
		}

		//Remove siege zones from universe
		for(SiegeZone siegeZone: siegeZonesToRemove) {
			universe.getSiegeZonesMap().remove(siegeZone.getName().toLowerCase());
		}

		//Save town
		saveTown(siege.getDefendingTown());

		//SaveNations
		for(Nation nation: nationsToSave) {
			saveNation(nation);
		}

		//Delete siege zone files
		for(SiegeZone siegeZone: siegeZonesToRemove) {
			deleteSiegeZone(siegeZone);
		}
		saveSiegeZoneList();

		//Todo - do we need something like this?
		//BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town.getName()));

		//Todo - do we need something like this?
		//universe.setChangedNotify(REMOVE_TOWN);
	}

	@Override
	public void removeNation(Nation nation) {

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
				nation.payTo(nation.getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.removeAccount();
			} catch (Exception e) {
			}

		//Delete values from memory (nation, siegezones, & sieges)
		List<Town> townsToSave = new ArrayList<>(nation.getTowns());
		List<SiegeZone> siegeZonesToDelete = new ArrayList<>(nation.getSiegeZones());
		deleteNation(nation);
		Siege siege;
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			siege = siegeZone.getSiege();
			siegeZone.getSiege().getSiegeZones().remove(nation); //Remove nation from siege
			townsToSave.add(siegeZone.getDefendingTown());  //Prepare to save town
			if(siege.getSiegeZones().size() == 0) {
				siege.getDefendingTown().setSiege(null);
				siege.getDefendingTown().setSiegeImmunityEndTime(0);
			}
		}
		nation.clear();

		//Remove data from universe
		universe.getNationsMap().remove(nation.getName().toLowerCase());
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			universe.getSiegeZonesMap().remove(siegeZone.getName().toLowerCase());
		}

		//Remove data from database
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			deleteSiegeZone(siegeZone);
		}

		//Save affected towns
		for (Town town : townsToSave) {

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

		//Save lists
		saveNationList();
		saveSiegeZoneList();

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
	public Set<String> getSiegeZonesKeys() {

		return universe.getSiegeZonesMap().keySet();
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
			Boolean isCapital = false;
			Nation nation = null;
			Double townBalance = 0.0;
			oldName = town.getName();

			// Save the towns bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					townBalance = town.getHoldingBalance();					
					if (TownySettings.isEcoClosedEconomyEnabled()){
						town.pay(townBalance, "Town Rename");
					} 
					town.removeAccount();
					
				} catch (EconomyException e) {
				}
			UUID oldUUID = town.getUuid();
			long oldregistration = town.getRegistered();

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
			//deleteSiege(town.getSiege());

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
			town.setUuid(oldUUID);
			town.setRegistered(oldregistration);
			if (TownySettings.isUsingEconomy()) {
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

			//Save Siege zones and associated nations
			for(Map.Entry<Nation,SiegeZone> entry: town.getSiege().getSiegeZones().entrySet()) {
				saveSiegeZone(entry.getValue());
				saveNation(entry.getKey());
			}

			saveTown(town);
			saveTownList();
			saveSiegeZoneList();
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

			List<Town> townsToSave = new ArrayList<>(nation.getTowns());

			List<SiegeZone> siegeZonesToSave = new ArrayList<>(nation.getSiegeZones());
			for(SiegeZone siegeZone: siegeZonesToSave) {
				townsToSave.add(siegeZone.getSiege().getDefendingTown());
			}

			Double nationBalance = 0.0;

			// Save the nations bank balance to set in the new account.
			// Clear accounts
			if (TownySettings.isUsingEconomy())
				try {
					nationBalance = nation.getHoldingBalance();
					if (TownySettings.isEcoClosedEconomyEnabled()){
						nation.pay(nationBalance, "Nation Rename");
					}
					nation.removeAccount();
					
				} catch (EconomyException e) {
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
			nation.setName(filteredName);
			universe.getNationsMap().put(filteredName.toLowerCase(), nation);

			//Update siege zones map
			for(SiegeZone siegeZone: nation.getSiegeZones()) {
				universe.getSiegeZonesMap().remove(SiegeZone.generateName(
						oldName.toLowerCase(),
						siegeZone.getSiege().getDefendingTown().getName().toLowerCase()));
				universe.getSiegeZonesMap().put(siegeZone.getName(), siegeZone);
			}

			if (TownyEconomyHandler.isActive()) {
				try {
					nation.setBalance(nationBalance, "Rename Nation - Transfer to new account");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

			nation.setUuid(oldUUID);
			nation.setRegistered(oldregistration);

			//Save siege zones, towns, nation
			for (SiegeZone siegeZone : siegeZonesToSave) {
				saveSiegeZone(siegeZone);
			}
			for (Town town : townsToSave) {
				saveTown(town);
			}
			saveNation(nation);

			//Save lists
			saveSiegeZoneList();
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
					townsToSave.remove(toCheck);

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
			//followed by outlaw lists
			Resident oldResident = new Resident(oldName);
			List<Resident> toSaveResident = new ArrayList<>(getResidents());
			for (Resident toCheck : toSaveResident){
				if (toCheck.hasFriend(oldResident)) {
					try {
						toCheck.removeFriend(oldResident);
						toCheck.addFriend(resident);
					} catch (NotRegisteredException e) {
						e.printStackTrace();
					}
				}
			}
			for (Resident toCheck : toSaveResident)
				saveResident(toCheck);
			
			List<Town> toSaveTown = new ArrayList<>(getTowns());
			for (Town toCheckTown : toSaveTown) {
				if (toCheckTown.hasOutlaw(oldResident)) {
					try {
						toCheckTown.removeOutlaw(oldResident);
						toCheckTown.addOutlaw(resident);
					} catch (NotRegisteredException e) {
						e.printStackTrace();
					}					
				}
			}
			for (Town toCheckTown : toSaveTown)
				saveTown(toCheckTown);	
		
		} finally {
			lock.unlock();			
		}
		
		BukkitTools.getPluginManager().callEvent(new RenameResidentEvent(oldName, resident, !Bukkit.getServer().isPrimaryThread()));
		
		universe.setChangedNotify(RENAME_RESIDENT);
		
	}
	
}
