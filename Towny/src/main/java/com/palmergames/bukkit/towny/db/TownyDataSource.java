package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent.Cause;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	final Lock lock = new ReentrantLock();
	protected final Towny plugin;
	protected final TownyUniverse universe;
	protected final TownyAPI api;

	TownyDataSource(Towny plugin, TownyUniverse universe) {
		this.plugin = plugin;
		this.universe = universe;
		this.api = TownyAPI.getInstance();
	}

	public abstract boolean backup() throws IOException;

	public boolean loadAll() {

		final boolean loaded = loadWorldList() && loadNationList() && loadTownList() && loadPlotGroupList() && loadDistrictList() && loadJailList() && loadResidentList() && loadTownBlockList() && loadWorlds() && loadResidents() && loadTowns() && loadNations() && loadTownBlocks() && loadPlotGroups() && loadDistricts() && loadJails() && loadRegenList() && loadCooldowns();

		if (loaded) {
			postLoad();
		}

		return loaded;
	}
	
	public void postLoad() {}

	public boolean saveAll() {

		return saveWorlds() && saveNations() && saveTowns() && saveResidents() && savePlotGroups() && saveDistricts() && saveTownBlocks() && saveJails() && saveRegenList() && saveCooldowns();
	}

	public boolean saveAllWorlds() {

		return saveWorlds();
	}

	public boolean saveQueues() {

		return saveRegenList();
	}

	abstract public void finishTasks();

	abstract public boolean loadTownBlockList();

	abstract public boolean loadResidentList();

	abstract public boolean loadTownList();

	abstract public boolean loadNationList();

	abstract public boolean loadWorldList();

	abstract public boolean loadRegenList();

	abstract public boolean loadTownBlocks();

	abstract public boolean loadJailList();
	
	abstract public boolean loadResident(Resident resident);

	abstract public boolean loadTown(Town town);

	abstract public boolean loadNation(Nation nation);

	abstract public boolean loadWorld(TownyWorld world);
	
	abstract public boolean loadJail(Jail jail);

	abstract public boolean loadPlotGroupList();

	abstract public boolean loadPlotGroup(PlotGroup group);

	abstract public boolean loadDistrictList();

	abstract public boolean loadDistrict(District district);

	abstract public boolean saveRegenList();

	abstract public boolean saveResident(Resident resident);

	abstract public boolean saveHibernatedResident(UUID uuid, long registered);
	
	abstract public boolean saveTown(Town town);
	
	abstract public boolean savePlotGroup(PlotGroup group);
	
	abstract public boolean saveDistrict(District district);
	
	abstract public boolean saveJail(Jail jail);

	abstract public boolean saveNation(Nation nation);

	abstract public boolean saveWorld(TownyWorld world);

	abstract public boolean saveTownBlock(TownBlock townBlock);

	abstract public boolean savePlotData(PlotBlockData plotChunk);

	abstract public PlotBlockData loadPlotData(String worldName, int x, int z);

	abstract public PlotBlockData loadPlotData(TownBlock townBlock);
	
	abstract public boolean hasPlotData(TownBlock townBlock);

	abstract public void deletePlotData(PlotBlockData plotChunk);

	abstract public void deleteResident(Resident resident);

	abstract public void deleteHibernatedResident(UUID uuid);
	
	abstract public void deleteTown(Town town);

	abstract public void deleteNation(Nation nation);

	abstract public void deleteWorld(TownyWorld world);

	abstract public void deleteTownBlock(TownBlock townBlock);

	abstract public void deleteFile(String file);
	
	abstract public void deletePlotGroup(PlotGroup group);
	
	abstract public void deleteDistrict(District district);
	
	abstract public void deleteJail(Jail jail);
	
	abstract public CompletableFuture<Optional<Long>> getHibernatedResidentRegistered(UUID uuid);

	public boolean cleanup() {

		return true;

	}

	public boolean loadResidents() {

		TownyMessaging.sendDebugMsg("Loading Residents");

		for (Resident resident : universe.getResidents()) {
			if (!loadResident(resident)) {
				plugin.getLogger().severe("Loading Error: Could not read resident data '" + resident.getName() + "'.");
				return false;
			}
		}
		return true;
	}

	public boolean loadTowns() {

		TownyMessaging.sendDebugMsg("Loading Towns");
		for (Town town : universe.getTowns())
			if (!loadTown(town)) {
				plugin.getLogger().severe("Loading Error: Could not read town data '" + town.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadNations() {

		TownyMessaging.sendDebugMsg("Loading Nations");
		for (Nation nation : universe.getNations())
			if (!loadNation(nation)) {
				plugin.getLogger().severe("Loading Error: Could not read nation data '" + nation.getName() + "'.");
				return false;
			}
		return true;
	}

	public boolean loadWorlds() {

		TownyMessaging.sendDebugMsg("Loading Worlds");
		for (TownyWorld world : universe.getTownyWorlds())
			if (!loadWorld(world)) {
				plugin.getLogger().severe("Loading Error: Could not read world data '" + world.getName() + "'.");
				return false;
			}
		return true;
	}
	
	public boolean loadJails() {
		TownyMessaging.sendDebugMsg("Loading Jails");
		for (Jail jail : universe.getJails()) {
			if (!loadJail(jail)) {
				plugin.getLogger().severe("Loading Error: Could not read jail data '" + jail.getUUID() + "'.");
				return false;
			}
		}
		return true;
	}
	
	public boolean loadPlotGroups() {
		TownyMessaging.sendDebugMsg("Loading PlotGroups");
		for (PlotGroup group : universe.getGroups()) {
			if (!loadPlotGroup(group)) {
				plugin.getLogger().severe("Loading Error: Could not read PlotGroup data: '" + group.getUUID() + "'.");
				return false;
			}
		}
		return true;
	}

	public boolean loadDistricts() {
		TownyMessaging.sendDebugMsg("Loading Districts");
		for (District district : universe.getDistricts()) {
			if (!loadDistrict(district)) {
				plugin.getLogger().severe("Loading Error: Could not read District data: '" + district.getUUID() + "'.");
				return false;
			}
		}
		return true;
	}

	abstract public boolean loadCooldowns();

	/*
	 * Save all of category
	 */

	public boolean saveResidents() {

		TownyMessaging.sendDebugMsg("Saving Residents");
		for (Resident resident : universe.getResidents())
			saveResident(resident);
		return true;
	}
	
	public boolean savePlotGroups() {
		TownyMessaging.sendDebugMsg("Saving PlotGroups");
		for (PlotGroup plotGroup : universe.getGroups())
			/*
			 * Only save plotgroups which actually have townblocks associated with them.
			 */
			if (plotGroup.hasTownBlocks())
				savePlotGroup(plotGroup);
			else
				deletePlotGroup(plotGroup); 
		return true;
	}

	public boolean saveDistricts() {
		TownyMessaging.sendDebugMsg("Saving Districts");
		for (District district : universe.getDistricts())
			/*
			 * Only save districts which actually have townblocks associated with them.
			 */
			if (district.hasTownBlocks())
				saveDistrict(district);
			else
				deleteDistrict(district); 
		return true;
	}

	public boolean saveJails() {
		TownyMessaging.sendDebugMsg("Saving Jails");
		for (Jail jail : universe.getJails())
			saveJail(jail);
		return true;
	}
	
	public boolean saveTowns() {

		TownyMessaging.sendDebugMsg("Saving Towns");
		for (Town town : universe.getTowns())
			saveTown(town);
		return true;
	}

	public boolean saveNations() {

		TownyMessaging.sendDebugMsg("Saving Nations");
		for (Nation nation : universe.getNations())
			saveNation(nation);
		return true;
	}

	public boolean saveWorlds() {

		TownyMessaging.sendDebugMsg("Saving Worlds");
		for (TownyWorld world : universe.getTownyWorlds())
			saveWorld(world);
		return true;
	}
	
	public boolean saveTownBlocks() {
		TownyMessaging.sendDebugMsg("Saving Townblocks");
		for (Town town : universe.getTowns()) {
			for (TownBlock townBlock : town.getTownBlocks())
				saveTownBlock(townBlock);
		}
		return true;
	}
	
	abstract public boolean saveCooldowns();

	// Database functions

	abstract public void removeResident(Resident resident);

	abstract public void removeTownBlock(TownBlock townBlock) throws TownyException;

	abstract public void removeTownBlock(TownBlock townBlock, Cause cause) throws TownyException;

	abstract public void removeTownBlocks(Town town);

	public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause) {
		return removeNation(nation, cause, null);
	}

	abstract public boolean removeNation(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause, @Nullable CommandSender sender);

	abstract public @NotNull Resident newResident(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public @NotNull Resident newResident(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newNation(String name, UUID uuid) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void newWorld(String name) throws AlreadyRegisteredException;

	public boolean removeTown(Town town, @NotNull DeleteTownEvent.Cause cause) {
		return removeTown(town, cause, null);
	}

	public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender) {
		return removeTown(town, cause, sender, TownySettings.getTownRuinsEnabled() && !town.isRuined());
	}

	abstract public boolean removeTown(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender, boolean delayFullRemoval);

	abstract public void removeWorld(TownyWorld world) throws UnsupportedOperationException;

	abstract public void removeJail(Jail jail);
	
	abstract public void removePlotGroup(PlotGroup group);
	
	abstract public void removeDistrict(District district);

	abstract public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException;
	
	abstract public void mergeNation(Nation succumbingNation, Nation prevailingNation);

	abstract public void mergeTown(Town mergeInto, Town mergeFrom);

	abstract public void renamePlayer(Resident resident, String newName) throws AlreadyRegisteredException, NotRegisteredException;

	abstract public void renameGroup(PlotGroup group, String newName) throws AlreadyRegisteredException;
	
	abstract public void renameDistrict(District district, String newName) throws AlreadyRegisteredException;
	
	/**
	 * @deprecated since 0.100.2.9 use {@link #removeTown(Town, com.palmergames.bukkit.towny.event.DeleteTownEvent.Cause)} instead.
	 * @param town
	 */
	@Deprecated
	public void removeTown(Town town) {
		removeTown(town, DeleteTownEvent.Cause.UNKNOWN);
	}
	
	@SuppressWarnings("unused")
	private void removeTown$$bridge$$public(Town town, boolean delayFullRemoval) {
		removeTown(town, DeleteTownEvent.Cause.UNKNOWN, null, delayFullRemoval);
	}

	/**
	 * @deprecated since 0.100.2.96 use {@link #removeNation(Nation, com.palmergames.bukkit.towny.event.DeleteNationEvent.Cause)} instead.
	 * @param nation
	 */
	@Deprecated
	public void removeNation(Nation nation) {
		removeNation(nation, DeleteNationEvent.Cause.UNKNOWN, null);
	}
}
