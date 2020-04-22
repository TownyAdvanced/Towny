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
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarRuinsUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.io.File;
import java.util.ArrayList;
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
					saveTown(townOutlaw);
				}
			}
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}

		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident.getName()));
	}

	public void removeOneOfManyTownBlocks(TownBlock townBlock, Town town) {
		
		TownPreUnclaimEvent event = new TownPreUnclaimEvent(townBlock);
		BukkitTools.getPluginManager().callEvent(event);
		
		if (event.isCancelled())
			return;

		Resident resident = null;
		try {
			resident = townBlock.getResident();
		} catch (NotRegisteredException ignored) {
		}
		
		TownyWorld world = townBlock.getWorld();
		WorldCoord coord = townBlock.getWorldCoord(); 

		if (world.isUsingPlotManagementDelete())
			TownyRegenAPI.addDeleteTownBlockIdQueue(coord);

		// Move the plot to be restored
		if (world.isUsingPlotManagementRevert()) {
			PlotBlockData plotData = TownyRegenAPI.getPlotChunkSnapshot(townBlock);
			if (plotData != null && !plotData.getBlockList().isEmpty()) {
				TownyRegenAPI.addPlotChunk(plotData, true);
			}
		}

		if (resident != null)
			saveResident(resident);

		world.removeTownBlock(townBlock);

		deleteTownBlock(townBlock);
		// Raise an event to signal the unclaim
		BukkitTools.getPluginManager().callEvent(new TownUnclaimEvent(town, coord));	
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

		TownyWorld world = townBlock.getWorld();
		world.removeTownBlock(townBlock);

		saveWorld(world);
		deleteTownBlock(townBlock);

		saveTownBlockList();

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
	
	public void removeManyTownBlocks(Town town) {

		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks()))
			removeOneOfManyTownBlocks(townBlock, town);
		saveTownBlockList();
	}

	@Override
	public List<TownBlock> getAllTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<>();
		for (TownyWorld world : getWorlds())
			townBlocks.addAll(world.getTownBlocks());
		return townBlocks;
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
			universe.getTownsTrie().addKey(filteredName);

		} finally {
			lock.unlock();
		}
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
			universe.getNationsTrie().addKey(filteredName);

		} finally {
			lock.unlock();
		}
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
		boolean delayFullRemoval = TownySettings.getWarSiegeEnabled() && TownySettings.getWarSiegeDelayFullTownRemoval();
		removeTown(town, delayFullRemoval);
	}

	@Override
	public void removeTown(Town town, boolean delayFullRemoval) {
		if (delayFullRemoval) {
			SiegeWarRuinsUtil.putTownIntoRuinedState(town, plugin);
			return;
		}

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);

		BukkitTools.getPluginManager().callEvent(preEvent);
		if (preEvent.isCancelled())
			return;

		removeManyTownBlocks(town);
		//removeTownBlocks(town);

		if (town.hasSiege())
			removeSiege(town.getSiege());

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

		Resident king = nation.getKing();

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

		//Delete all sieges
		List<Siege> siegesToDelete = new ArrayList<>(nation.getSieges());
		for (Siege siege : siegesToDelete) {
			toSave.add(siege.getDefendingTown());  //Prepare to save town
			siege.getDefendingTown().setSiege(null); //Remove siege from town

			//If the siege was in progress, initiate siege immunity for the town
			if (siege.getStatus() == SiegeStatus.IN_PROGRESS) {
				siege.setActualEndTime(System.currentTimeMillis());
				SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);
			}

			//Delete siege
			deleteSiege(siege);
		}

		nation.clear();

		universe.getNationsTrie().removeKey(nation.getName().toLowerCase());
		universe.getNationsMap().remove(nation.getName().toLowerCase());
		for (Siege siege : siegesToDelete) {
			universe.getSiegesMap().remove(siege.getName().toLowerCase());
		}

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
		if (siegesToDelete.size() > 0)
			saveSiegeList();

		//Refund some of the initial setup cost to the king
		if (TownySettings.getWarSiegeEnabled()
			&& TownySettings.isUsingEconomy()
			&& TownySettings.getWarSiegeRefundInitialNationCostOnDelete()) {
			try {
				//Refund the king with some of the initial nation setup cost
				double amountToRefund = Math.round(TownySettings.getNewNationPrice() * 0.01 * TownySettings.getWarSiegeNationCostRefundPercentageOnDelete());
				king.getAccount().collect(amountToRefund, "Refund of Some of the Initial Nation Cost");
			} catch (Exception e) {
				e.printStackTrace();
			}
			TownyMessaging.sendGlobalMessage(
				String.format(
					TownySettings.getLangString("msg_siege_war_refund_initial_cost_on_nation_delete"),
					king.getFormattedName(),
					TownySettings.getWarSiegeNationCostRefundPercentageOnDelete() + "%"));
		}

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
			if(town.hasSiege()) {
				deleteSiege(town.getSiege());
			}

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

			//Move/rename siege
			if(town.hasSiege()) {
				Siege siege = town.getSiege();
				String oldSiegeName = siege.getName();
				String newSiegeName = siege.getAttackingNation().getName() + "#vs#" + town.getName();
				//Update siege
				siege.setName(newSiegeName);
				//Update universe
				universe.getSiegesMap().remove(oldSiegeName.toLowerCase());
				universe.getSiegesMap().put(newSiegeName.toLowerCase(), siege);
			}

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

			//Save siege data
			if(town.hasSiege()) {
				saveSiege(town.getSiege());
				saveNation(town.getSiege().getAttackingNation());
			}

			saveTownList();
			saveSiegeList();
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
			for(Siege siege: new ArrayList<>(nation.getSieges())) {
				deleteSiege(siege);
			}
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

			//Move/rename sieges
			String oldSiegeName;
			String newSiegeName;
			for(Siege siege: nation.getSieges()) {
				oldSiegeName = siege.getName();
				newSiegeName = siege.getAttackingNation().getName() + "#vs#" + siege.getDefendingTown().getName();
				//Update siege
				siege.setName(newSiegeName);
				//Update universe
				universe.getSiegesMap().remove(oldSiegeName.toLowerCase());
				universe.getSiegesMap().put(newSiegeName.toLowerCase(), siege);
			}

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

			//Save sieges
			for(Siege siege: nation.getSieges()) {
				saveSiege(siege);
				saveTown(siege.getDefendingTown());
			}

			saveNationList();
			saveSiegeList();

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
			resident.setTownRanks(townRanks);
			resident.setRegistered(registered);
			resident.setLastOnline(lastOnline);
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
			saveResidentList();
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

	@Override
	public List<Siege> getSieges() {
		return new ArrayList<>(universe.getSiegesMap().values());
	}

	@Override
	public void newSiege(String siegeName) throws AlreadyRegisteredException {

		lock.lock();

		try {
			if(universe.getSiegesMap().containsKey(siegeName.toLowerCase()))
				throw new AlreadyRegisteredException("Siege is already registered");

			Siege siege = new Siege(siegeName);

			universe.getSiegesMap().put(siegeName.toLowerCase(), siege);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public Siege getSiege(String siegeName) throws NotRegisteredException {
		if(!universe.getSiegesMap().containsKey(siegeName.toLowerCase())) {
			throw new NotRegisteredException("Siege not found");
		}
		return universe.getSiegesMap().get(siegeName.toLowerCase());
	}

	//Remove a particular siege, and all associated data
	@Override
	public void removeSiege(Siege siege) {
		//Remove siege from town
		siege.getDefendingTown().setSiege(null);
		//Remove siege from nation
		siege.getAttackingNation().removeSiege(siege);
		//Remove siege from universe
		universe.getSiegesMap().remove(siege.getName().toLowerCase());

		//Save town
		saveTown(siege.getDefendingTown());
		//Save attacking nation
		saveNation(siege.getAttackingNation());
		//Delete siege file
		deleteSiege(siege);
		//Save siege list
		saveSiegeList();
	}

	@Override
	public Set<String> getSiegeKeys() {

		return universe.getSiegesMap().keySet();
	}

	@Override
	public void removeTownFromNation(Towny plugin, Town town, Nation nation) {
		boolean removeNation = false;

		try {
			nation.removeTown(town);
		} catch(NotRegisteredException x) {
			return;  //Town was already removed
		} catch(EmptyNationException x) {
			removeNation = true;  //Set flag to remove nation at end of this method
		}

		if(removeNation) {
			removeNation(nation);
			saveNationList();
		} else {
			saveNation(nation);
			saveNationList();
			plugin.resetCache();
		}

		saveTown(town);
	}

	@Override
	public void addTownToNation(Towny plugin, Town town,Nation nation) {
		try {
			nation.addTown(town);
			saveTown(town);
			plugin.resetCache();
			saveNation(nation);
		} catch (AlreadyRegisteredException x) {
			return;   //Town already in nation
		}
	}
}
