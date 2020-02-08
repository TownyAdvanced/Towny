package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyFormatter;
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
import com.palmergames.bukkit.towny.object.PlotObjectGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Player;

import javax.naming.InvalidNameException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

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
	
	public PlotObjectGroup getPlotObjectGroup(String townName, UUID groupID) {
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
	
	public List<PlotObjectGroup> getAllPlotGroups() {
		List<PlotObjectGroup> groups = new ArrayList<>();
		groups.addAll(universe.getGroups());
		
		return groups;
	}
	
	public void newPlotGroup(PlotObjectGroup group) {
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
		/*
		 * If removal is delayed:
		 * 1. Town will be set into a special 'ruined' state
		 * 2. All perms will be enabled
		 * 3. All plots will be unowned
		 * 4. The town leadership cannot run /t commands
		 * 5. Town will be deleted after 2 upkeep cycles
		 */
		if(delayFullRemoval) {
			town.setRecentlyRuinedEndTime(888);
			town.setPublic(false);
			town.setOpen(false);
			for (String element : new String[] { "residentBuild",
				"residentDestroy", "residentSwitch",
				"residentItemUse", "outsiderBuild",
				"outsiderDestroy", "outsiderSwitch",
				"outsiderItemUse", "allyBuild", "allyDestroy",
				"allySwitch", "allyItemUse", "nationBuild", "nationDestroy",
				"nationSwitch", "nationItemUse",
				"pvp", "fire", "explosion", "mobs"})
			{
				town.getPermissions().set(element, true);
			}
			//Reset and save town blocks
			for(TownBlock townBlock: town.getTownBlocks()) {
				townBlock.setType(townBlock.getType());
				townBlock.setResident(null);
				saveTownBlock(townBlock);
			}
			saveTown(town);
			plugin.resetCache();
			return;
		}

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);

		BukkitTools.getPluginManager().callEvent(preEvent);
		if (preEvent.isCancelled())
			return;

		removeManyTownBlocks(town);
		//removeTownBlocks(town);	

		if(town.hasSiege())
			removeSiege(town.getSiege());

		List<Resident> toSave = new ArrayList<>(town.getResidents());
		TownyWorld townyWorld = town.getWorld();

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

		//Refund some of the initial setup cost to the king
		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.isUsingEconomy()
			&& TownySettings.getWarSiegeRefundInitialNationCostOnDelete()) {
			try {
				//Refund the king with some of the initial nation setup cost
				double amountToRefund = Math.round(TownySettings.getNewNationPrice() * 0.01 * TownySettings.getWarSiegeNationCostRefundPercentageOnDelete());
				nation.getKing().getAccount().collect(amountToRefund, "Refund of Some of the Initial Nation Cost");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
			TownyMessaging.sendGlobalMessage(
				String.format(
				TownySettings.getLangString("msg_siege_war_refund_initial_cost_on_nation_delete"),
				TownyFormatter.getFormattedResidentName(nation.getKing()),
				TownySettings.getWarSiegeNationCostRefundPercentageOnDelete() + "%"));
		}

		//Delete nation and save towns
		deleteNation(nation);
		List<Town> toSave = new ArrayList<>(nation.getTowns());

		//Delete siegezones & save affected towns
		List<SiegeZone> siegeZonesToDelete = new ArrayList<>(nation.getSiegeZones());
		Siege siege;
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			siege = siegeZone.getSiege();
			siege.getSiegeZones().remove(nation); //Remove nation from siege
			toSave.add(siegeZone.getDefendingTown());  //Prepare to save town
			if(siege.getSiegeZones().size() == 0) {
				siege.getDefendingTown().setSiege(null);
				siege.setActualEndTime(System.currentTimeMillis());
				SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);
			}
			deleteSiegeZone(siegeZone);
		}

		nation.clear();

		universe.getNationsMap().remove(nation.getName().toLowerCase());
		for(SiegeZone siegeZone: siegeZonesToDelete) {
			universe.getSiegeZonesMap().remove(siegeZone.getName().toLowerCase());
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
		if(siegeZonesToDelete.size() >0)
			saveSiegeZoneList();

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

			TownyWorld world = town.getWorld();
			world.removeTown(town);
			/*
			 * Tidy up old files.
			 * Has to be done here else the town no longer exists
			 * and the file move command may fail.
			 */
			deleteTown(town);
			
			if(town.hasSiege()) {
				for(SiegeZone siegeZone: new ArrayList<>(town.getSiege().getSiegeZones().values())) {
					deleteSiegeZone(siegeZone);
				}
			}
		
			/*
			 * Remove the old town from the townsMap
			 * and rename to the new name
			 */
			universe.getTownsMap().remove(town.getName().toLowerCase());
			town.setName(filteredName);
			universe.getTownsMap().put(filteredName.toLowerCase(), town);
			world.addTown(town);

			//Similarly move/rename siegezones
			if(town.hasSiege()) {
				String oldSiegeZoneName;
				String newSiegeZoneName;
				for (SiegeZone siegeZone : town.getSiege().getSiegeZones().values()) {
					oldSiegeZoneName = SiegeZone.generateName(siegeZone.getAttackingNation().getName(), oldName);
					newSiegeZoneName = siegeZone.getName();
					universe.getSiegeZonesMap().remove(oldSiegeZoneName);
					universe.getSiegeZonesMap().put(newSiegeZoneName.toLowerCase(), siegeZone);
				}
			}
			
			// If this was a nation capitol
			if (isCapital) {
				nation.setCapital(town);
			}
			town.setUuid(oldUUID);
			town.setRegistered(oldregistration);
			if (TownySettings.isUsingEconomy()) {
				try {
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
				for (PlotObjectGroup pg : town.getPlotObjectGroups()) {
					pg.setTown(town);
					savePlotGroup(pg);
				}

			saveTown(town);
			for(SiegeZone siegeZone: town.getSiege().getSiegeZones().values()) {
				saveSiegeZone(siegeZone);
				saveNation(siegeZone.getAttackingNation());
			}

			saveTownList();
			saveSiegeZoneList();
			savePlotGroupList();
			saveWorld(town.getWorld());

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

			for(SiegeZone siegeZone: new ArrayList<>(nation.getSiegeZones())) {
				deleteSiegeZone(siegeZone);
			}
			/*
			 * Remove the old nation from the nationsMap
			 * and rename to the new name
			 */
			oldName = nation.getName();
			universe.getNationsMap().remove(oldName.toLowerCase());
			nation.setName(filteredName);
			universe.getNationsMap().put(filteredName.toLowerCase(), nation);

			//Similarly move/rename siegezones
			String oldSiegeZoneName;
			String newSiegeZoneName;
			for(SiegeZone siegeZone: nation.getSiegeZones()) {
				oldSiegeZoneName = SiegeZone.generateName(oldName, siegeZone.getDefendingTown().getName());
				newSiegeZoneName = siegeZone.getName();
				universe.getSiegeZonesMap().remove(oldSiegeZoneName);
				universe.getSiegeZonesMap().put(newSiegeZoneName.toLowerCase(), siegeZone);
			}

			if (TownyEconomyHandler.isActive()) {
				try {
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
			for(SiegeZone siegeZone: nation.getSiegeZones()) {
				saveSiegeZone(siegeZone);
				saveTown(siegeZone.getDefendingTown());
			}

			saveNationList();
			saveSiegeZoneList();

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
	public void renameGroup(PlotObjectGroup group, String newName) throws AlreadyRegisteredException {
		// Create new one
		group.setGroupName(newName);
		
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
			resident.setName(newName);
			universe.getResidentMap().put(newName.toLowerCase(), resident);
			
			//add everything back to the resident
			if (TownyEconomyHandler.getVersion().startsWith("iConomy 5") && TownySettings.isUsingEconomy()) {
				try {
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
	public List<SiegeZone> getSiegeZones() {
		return new ArrayList<>(universe.getSiegeZonesMap().values());
	}

	public void newSiegeZone(String siegeZoneName) throws AlreadyRegisteredException {
		String[] townAndNationArray = SiegeZone.generateTownAndNationName(siegeZoneName);
		newSiegeZone(townAndNationArray[0],townAndNationArray[1]);
	}

	@Override
	public void newSiegeZone(String attackingNationName,String defendingTownName) throws AlreadyRegisteredException {

		lock.lock();

		try {
			String siegeZoneName = SiegeZone.generateName(attackingNationName, defendingTownName);

			if(universe.getSiegeZonesMap().containsKey(siegeZoneName.toLowerCase()))
				throw new AlreadyRegisteredException("Siege Zone is already registered");

			Town town = universe.getTownsMap().get(defendingTownName.toLowerCase());
			Nation nation = universe.getNationsMap().get(attackingNationName.toLowerCase());
			SiegeZone siegeZone = new SiegeZone(nation, town);

			universe.getSiegeZonesMap().put(siegeZoneName.toLowerCase(), siegeZone);

		} finally {
			lock.unlock();
		}
	}

	@Override
	public SiegeZone getSiegeZone(String siegeZoneName) throws NotRegisteredException {
		if(!universe.getSiegeZonesMap().containsKey(siegeZoneName.toLowerCase())) {
			throw new NotRegisteredException("Siege Zone not found");
		}
		return universe.getSiegeZonesMap().get(siegeZoneName.toLowerCase());
	}
	
	//Remove a particular siege, and all associated data
	@Override
	public void removeSiege(Siege siege) {
		//Remove siege from town
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
	}


	//Remove a particular siege, and all associated data
	@Override
	public void removeSiegeZone(SiegeZone siegeZone) {
		//Remove siege zone from town
		siegeZone.getDefendingTown().getSiege().getSiegeZones().remove(siegeZone.getAttackingNation());
		//Remove siege zone from nation
		siegeZone.getAttackingNation().removeSiegeZone(siegeZone);
		//Remove siege zone from universe
		universe.getSiegeZonesMap().remove(siegeZone.getName().toLowerCase());

		//Save town
		saveTown(siegeZone.getDefendingTown());
		//SaveNation
		saveNation(siegeZone.getAttackingNation());
		//Delete siege zone file
		deleteSiegeZone(siegeZone);

		//Save siege zone list
		saveSiegeZoneList();
	}


	@Override
	public Set<String> getSiegeZonesKeys() {

		return universe.getSiegeZonesMap().keySet();
	}
}
