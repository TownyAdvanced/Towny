package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;

public class WarDataBase {

	private static Towny plugin;
	private static Map<UUID, List<Town>> warTownsMap = new ConcurrentHashMap<>();
	private static Map<UUID, List<Resident>> warResidentsMap = new ConcurrentHashMap<>();
	private static Map<UUID, List<TownBlock>> warTownblocksMap = new ConcurrentHashMap<>();
	private static Map<UUID, WarData> wardatas = new ConcurrentHashMap<UUID, WarData>();

	public static void initialize(Towny instance) {
		plugin = instance;
	}

	public static boolean loadAll() {
		/*
		 * Clear out the maps, because this might be a reload.
		 */
		clearAll();

		plugin.getLogger().info("Loading EventWar Database");
		plugin.getLogger().info("TownyUniverse pre-loaded with " + TownyUniverse.getInstance().getWars().size() + " wars.");
		
		/*
		 * Scrape metadata for WarData.
		 */
		loadWarsFromTowns();
		loadWarsFromResidents();
		loadWarsFromTownBlocks();
		
		plugin.getLogger().info("Found " + warTownsMap.size() + " wars.");
		plugin.getLogger().info("Found " + warTownsMap.values().size() + " town involved in " + warTownsMap.size() + " war(s).");
		plugin.getLogger().info("Found " + warResidentsMap.values().size() + " residents involved in " + warResidentsMap.size() + " war(s).");
		plugin.getLogger().info("Found " + warTownblocksMap.values().size() + " townblocks involved in " + warTownblocksMap.size() + " war(s).");

		/*
		 * Use WarData to finish loading Wars pre-loaded from the flatfile War files.
		 */
		finalizeWarData();
		
		return true;
	}

	/**
	 * Finalize populating pre-loaded Wars with the scraped metadata.
	 */
	private static void finalizeWarData() {
		// We could end up in a situation where a war still has a file but no metadata exists any more.
		if (wardatas.isEmpty())
			removeAllWars();

		for (War war : new ArrayList<>(TownyUniverse.getInstance().getWars())) {
			/*
			 * Get the relevant WarData for this UUID.
			 */
			UUID uuid = war.getWarUUID();
			WarData data = wardatas.get(uuid);

			/*
			 * After parsing the metadata, no Town had this war, remove it.
			 */
			if (data == null) {
				TownyUniverse.getInstance().getDataSource().removeWar(war);
				continue;
			}

			/*
			 * Populate the future participants into the WarData.
			 */
			switch (war.getWarType()) {
				case CIVILWAR:
				case NATIONWAR:
				case WORLDWAR:
					List<Nation> nations = new ArrayList<>();
					for (Town town : warTownsMap.get(uuid))
						if (town.hasNation() && !nations.contains(town.getNationOrNull()))
							nations.add(town.getNationOrNull());
					data.setNations(nations);
				case TOWNWAR:
				case RIOT:
					data.setTowns(warTownsMap.get(uuid));
					data.setResidents(warResidentsMap.get(uuid));
					data.setTownblocks(warTownblocksMap.get(uuid));
			}
			war.loadWar(data.getNations(), data.getTowns(), data.getResidents(), data.getTownblocks());
			plugin.getLogger().info("Loaded war: " + war.getWarName());
		}
	}

	/**
	 * Scrape warUUID meta from Towns to create the initial WarData and populate the warTownsMap.
	 */
	public static void loadWarsFromTowns() {
		for (Town town : TownyUniverse.getInstance().getTowns()) {
			// Cycle through towns looking for a eventwar UUID
			String string = WarMetaDataController.getWarUUID(town);
			if (string == null)
				continue;
			UUID warUUID = UUID.fromString(string);
			
			// If this UUID is pre-registered in TownyUniverse (via the towny\data\wars\UUID.txt files) 
			// then add the Town to the warTownsMap and create a WarData for this UUID. 
			if (TownyUniverse.getInstance().getWarEvent(warUUID) != null) {
				addWarUuidForTown(warUUID, town);
				if (!wardatas.containsKey(warUUID))
					wardatas.put(warUUID, WarData.of(warUUID));
			// Else, this UUID is not backed by any data, delete the metadata.
			} else {
				plugin.getLogger().info(town.getName() + " tried to load an invalid war, this War UUID " + warUUID + " will be purged from any further metadata discovered.");
				cleanTownMetaData(town);
			}
		}
	}

	private static void addWarUuidForTown(UUID uuid, Town town) {
		List<Town> townList = new ArrayList<>();
		if (warTownsMap.containsKey(uuid))
			townList = warTownsMap.get(uuid);
		townList.add(town);
		warTownsMap.put(uuid, townList);
		plugin.getLogger().info("Loaded town " + town.getName() + " into war " + uuid);
	}

	/**
	 * Scrape warUUID meta from Residents to populate the warResidentsMap.
	 */
	public static void loadWarsFromResidents() {
		for (Resident resident : TownyUniverse.getInstance().getResidents()) {
			String slug = WarMetaDataController.getWarUUID(resident);
			if (slug == null)
				continue;
			UUID warUUID = UUID.fromString(slug);
			if (warTownsMap.containsKey(warUUID))
				addWarUuidForResident(warUUID, resident);
			else
				cleanResidentMetaData(resident);
		}
	}

	private static void addWarUuidForResident(UUID uuid, Resident resident) {
		List<Resident> resList = new ArrayList<>();
		if (warResidentsMap.containsKey(uuid))
			resList = warResidentsMap.get(uuid);
		resList.add(resident);
		warResidentsMap.put(uuid, resList);
	}

	/**
	 * Scrape warUUID meta from TownBlocks to populate the warTownblocksMap.
	 */
	public static void loadWarsFromTownBlocks() {
		for (TownBlock tb : TownyUniverse.getInstance().getTownBlocks().values()) {
			String slug = WarMetaDataController.getWarUUID(tb);
			if (slug == null)
				continue;
			UUID warUUID = UUID.fromString(slug);
			if (warTownsMap.containsKey(warUUID))
				addWarUuidForTownBlock(warUUID, tb);
			else
				cleanTownBlockMetaData(tb);
		}
	}

	private static void addWarUuidForTownBlock(UUID uuid, TownBlock tb) {
		List<TownBlock> townblockList = new ArrayList<>();
		if (warTownblocksMap.containsKey(uuid))
			townblockList = warTownblocksMap.get(uuid);
		townblockList.add(tb);
		warTownblocksMap.put(uuid, townblockList);
	}
	
	private static void clearAll() {
		warTownsMap.clear();
		warResidentsMap.clear();
		warTownblocksMap.clear();
		wardatas.clear();
	}
	
	/**
	 * Something went wrong, we will be removing all traces of wars.
	 */
	public static void removeAllWars() {
		for (War war : new ArrayList<>(TownyUniverse.getInstance().getWars()))
			TownyUniverse.getInstance().getDataSource().removeWar(war);
		
		for (Nation nation : TownyUniverse.getInstance().getNations()) 
			nation.setActiveWar(false);
		
		for (Town town : TownyUniverse.getInstance().getTowns()) {
			town.setActiveWar(false);
			cleanTownMetaData(town);
		}
		
		for (Resident resident : TownyUniverse.getInstance().getResidents())
			cleanResidentMetaData(resident);
		
		for (TownBlock tb : TownyUniverse.getInstance().getTownBlocks().values())
			cleanTownBlockMetaData(tb);
	}
	
	public static void removeWar(War war) {
		TownyUniverse.getInstance().getDataSource().removeWar(war);
		
		for (Nation nation : war.getWarParticipants().getNations()) {
			nation.setActiveWar(false);
			// Give the town a lastWarEndTime metadata
			WarMetaDataController.setLastWarTime(nation, System.currentTimeMillis());
		}

		
		for (Town town : war.getWarParticipants().getTowns()) { 
			cleanTownMetaData(town);
			town.setActiveWar(false);
			// Give the town a lastWarEndTime metadata
			WarMetaDataController.setLastWarTime(town, System.currentTimeMillis());
			for (TownBlock tb : town.getTownBlocks())
				cleanTownBlockMetaData(tb);
		}
		
		for (Resident res : war.getWarParticipants().getResidents()) 
			cleanResidentMetaData(res);
	}
	
	public static void cleanTownMetaData(Town town) {
		WarMetaDataController.removeWarUUID(town);
		WarMetaDataController.removeWarSide(town);
		WarMetaDataController.removeScore(town);
	}
	
	public static void cleanResidentMetaData(Resident resident) {
		WarMetaDataController.removeWarUUID(resident);
		WarMetaDataController.removeResidentLivesMeta(resident);
		WarMetaDataController.removeWarSide(resident);
		WarMetaDataController.removeScore(resident);
	}

	public static void cleanTownBlockMetaData(TownBlock tb) {
		WarMetaDataController.removeWarUUID(tb);
	}

	/**
	 * A class that helps transition war metadata loaded 
	 * from towns, residents and townblocks into something 
	 * that can be loaded into a War. 
	 */
	private static class WarData {
		private UUID uuid;
		private List<Nation> nations;
		private List<Town> towns;
		private List<Resident> residents;
		private List<TownBlock> townblocks;
		
		private static WarData of(UUID uuid) {
			return new WarData(uuid);
		}
		
		private WarData(UUID uuid) {
			this.setUuid(uuid);
		}
		
		@SuppressWarnings("unused")
		public UUID getUuid() {
			return uuid;
		}

		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}

		public List<Nation> getNations() {
			return nations;
		}

		public void setNations(List<Nation> nations) {
			this.nations = nations;
		}

		public List<Town> getTowns() {
			return towns;
		}

		public void setTowns(List<Town> towns) {
			this.towns = towns;
		}

		public List<Resident> getResidents() {
			return residents;
		}

		public void setResidents(List<Resident> residents) {
			this.residents = residents;
		}

		public List<TownBlock> getTownblocks() {
			return townblocks;
		}

		public void setTownblocks(List<TownBlock> townblocks) {
			this.townblocks = townblocks;
		}
	}
 
}
