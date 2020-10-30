package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.migration.ConfigMigrator;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyDatabaseHandler;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.CleanupTask;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.Trie;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Towny's class for internal API Methods
 * If you don't want to change the dataSource, war, permissions or similiar behavior
 * and only for example want to get Resident objects you should use {@link TownyAPI}
 *
 * @author Lukas Mansour (Articdive)
 */
public class TownyUniverse {
    private static TownyUniverse instance;
    private final Towny towny;
    
    private final Map<String, Resident> residents = new ConcurrentHashMap<>();
    private final Trie residentsTrie = new Trie();
    private final Map<String, Town> towns = new ConcurrentHashMap<>();
    private final Trie townsTrie = new Trie();
    private final Map<String, Nation> nations = new ConcurrentHashMap<>();
    private final Trie nationsTrie = new Trie();
    private final Map<String, TownyWorld> worlds = new ConcurrentHashMap<>();
    private final Map<String, CustomDataField> registeredMetadata = new HashMap<>();
	private final Map<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private CompletableFuture<Void> backupFuture;
    
    private final List<Resident> jailedResidents = new ArrayList<>();
    private final String rootFolder;
    private TownyDataSource dataSource;
    private TownyPermissionSource permissionSource;
    private War warEvent;
    private List<War> wars = new ArrayList<>();

    private TownyUniverse() {
        towny = Towny.getPlugin();
        rootFolder = towny.getDataFolder().getPath();
    }
    
    public static TownyUniverse getInstance() {
        if (instance == null) {
            instance = new TownyUniverse();
        }
        return instance;
    }

    /**
     * Loads Towny's files/database en masse. Will end up in safemode if things do not go well. 
     * 
     * Loads config/language/townyperms files.
     * Initiates the logger.
     * Flushes object maps.
     * Saves and loads the database.
     * Will migrate the config if needed.
     * Loads the town and nation levels.
     * Legacy outpost test.
     * Schedule cleanup and backup.
     * 
     * @return true if things go well.
     */
    boolean loadSettings() {
        
    	// Load config, language and townyperms files.
    	if (!loadFiles())
    		return false;

    	// Init logger
		TownyLogger.getInstance();

        // Clears the object maps from memory.
        clearAllObjects();
                
        // Try to load and save the database.
        if (!loadAndSaveDatabase(TownySettings.getLoadDatabase(), TownySettings.getSaveDatabase()))
        	return false;

        // Try migrating the config and world files if the version has changed.
        if (!Version.fromString(TownySettings.getLastRunVersion()).equals(towny.getVersion())) {
			ConfigMigrator migrator = new ConfigMigrator(TownySettings.getConfig(), "config-migration.json");
			migrator.migrate();
		}
        
        // Loads Town and Nation Levels after migration has occured.
        if (!loadTownAndNationLevels())
        	return false;

        File f = new File(rootFolder, "outpostschecked.txt");                                        // Old towny didn't keep as good track of outpost spawn points,
        if (!f.exists()) {                                                                           // some of them ending up outside of claimed plots. If the file 
            for (Town town : dataSource.getTowns()) TownyDatabaseHandler.validateTownOutposts(town); // does not exist we will test all outpostspawns and create the
            towny.saveResource("outpostschecked.txt", false);                                        // file. Sometimes still useful on servers who've manually
        }                                                                                            // altered data manually and want to re-check.

		// Run both the cleanup and backup async.
		performCleanupAndBackup();

		// Things would appear to have gone well.
        return true;
    }
 
    /*
     * loadSettings() functions.
     */

    /**
    * Performs CleanupTask and BackupTask in async,
     */
    public void performCleanupAndBackup() {
		backupFuture = CompletableFuture
			.runAsync(new CleanupTask())
			.thenRunAsync(new BackupTask());
	}
    
    /**
     * Load config, language and townyperms files.
     * 
     * @return true if no exceptions are found.
     */
    private boolean loadFiles() {
        try {
            TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", towny.getVersion());
            Translation.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
            TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
        } catch (IOException | TownyException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Clears the object maps.
     */
    public void clearAllObjects() {
    	worlds.clear();
        nations.clear();
        towns.clear();
        residents.clear();
        townBlocks.clear();
    }
    
    /**
     * Test loading and saving the database.
     * 
     * @param loadDbType - load setting from the config.
     * @param saveDbType - save setting from the config.
     * @return true when the databse will load and save.
     */
    private boolean loadAndSaveDatabase(String loadDbType, String saveDbType) {
    	System.out.println("[Towny] Database: [Load] " + loadDbType + " [Save] " + saveDbType);
        // Try loading the database.
        long startTime = System.currentTimeMillis();
        if (!loadDatabase(loadDbType)) {
            System.out.println("[Towny] Error: Failed to load!");
            return false;
        }
        
        long time = System.currentTimeMillis() - startTime;
        System.out.println("[Towny] Database: Loaded in " + time + "ms.");
        System.out.println("[Towny] Database: " + TownySettings.getUUIDPercent() + " of residents have stored UUIDs."); // TODO: remove this when we're using UUIDs directly in the database.

        // Try saving the database.
        if (!saveDatabase(saveDbType)) {
        	System.out.println("[Towny] Error: Unsupported save format!");
        	return false;
        }
		return true;
    }
    
    /**
     * Loads the database into memory.
     *  
     * @param loadDbType - load setting from the config.
     * @return true when the database will load.
     */
    private boolean loadDatabase(String loadDbType) {
        
        switch (loadDbType.toLowerCase()) {
            case "ff":
            case "flatfile": {
                this.dataSource = new TownyFlatFileSource(towny, this);
                break;
            }
            case "h2":
            case "sqlite":
            case "mysql": {
                this.dataSource = new TownySQLSource(towny, this, loadDbType.toLowerCase());
                break;
            }
            default: {
                return false;
            }
        }
        
        return dataSource.loadAll();
    }
    
    /**
     * Saves the database into memory.
     * 
     * @param saveDbType - save setting from the config.
     * @return true when the database will save.
     */
    private boolean saveDatabase(String saveDbType) {
        try {
            // Set the new class for saving.
            switch (saveDbType.toLowerCase()) {
                case "ff":
                case "flatfile": {
                    this.dataSource = new TownyFlatFileSource(towny, this);
                    break;
                }
                case "h2":
                case "sqlite":
                case "mysql": {
                    this.dataSource = new TownySQLSource(towny, this, saveDbType.toLowerCase());
                    break;
                }
                default: {}
            }

            if (TownySettings.getLoadDatabase().equalsIgnoreCase(saveDbType)) {
                // Update all Worlds data files
                dataSource.saveAllWorlds();                
            } else {
                //Formats are different so save ALL data.
                dataSource.saveAll();
            }
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * Loads the Town and Nation Levels from the config.yml
     * 
     * @return true if they have the required elements.
     */
    private boolean loadTownAndNationLevels() {
		// Load Nation & Town level data into maps.
		try {
			TownySettings.loadTownLevelConfig();
			TownySettings.loadNationLevelConfig();
			return true;
		} catch (IOException e) {
			return false;
		}
    }

    /**
     * Run during onDisable() to finish cleanup and backup.
     */
    public void finishTasks() {
    	if (backupFuture != null) {
			// Join into main thread for proper termination.
			backupFuture.join();
		}
	}

    /*
     * DataSource, PermissionSource and RootFolder.
     */

    public TownyDataSource getDataSource() {
        return dataSource;
    }
    
    public TownyPermissionSource getPermissionSource() {
        return permissionSource;
    }
    
    public void setPermissionSource(TownyPermissionSource permissionSource) {
        this.permissionSource = permissionSource;
    }
    
    public String getRootFolder() {
        return rootFolder;
    }
    
    /*
     * Maps and Tries
     */

    public Map<String, Nation> getNationsMap() {
        return nations;
    }
    
    public Trie getNationsTrie() {
    	return nationsTrie;
	}
	
    public Map<String, Resident> getResidentMap() {
        return residents;
    }

	public Trie getResidentsTrie() {
		return residentsTrie;
	}
	
    public List<Resident> getJailedResidentMap() {
        return jailedResidents;
    }
    
    public Map<String, Town> getTownsMap() {
        return towns;
    }
    
    public Trie getTownsTrie() {
    	return townsTrie;
	}
	
    public Map<String, TownyWorld> getWorldMap() {
        return worlds;
    }
    
    /*
     * Towny Tree command output.
     */

    public List<String> getTreeString(int depth) {
        
        List<String> out = new ArrayList<>();
        out.add(getTreeDepth(depth) + "Universe (1)");
        if (towny != null) {
            out.add(getTreeDepth(depth + 1) + "Server (" + BukkitTools.getServer().getName() + ")");
            out.add(getTreeDepth(depth + 2) + "Version: " + BukkitTools.getServer().getVersion());
            //out.add(getTreeDepth(depth + 2) + "Players: " + BukkitTools.getOnlinePlayers().length + "/" + BukkitTools.getServer().getMaxPlayers());
            out.add(getTreeDepth(depth + 2) + "Worlds (" + BukkitTools.getWorlds().size() + "): " + Arrays.toString(BukkitTools.getWorlds().toArray(new World[0])));
        }
        out.add(getTreeDepth(depth + 1) + "Worlds (" + worlds.size() + "):");
        for (TownyWorld world : worlds.values()) {
            out.addAll(world.getTreeString(depth + 2));
        }
        
        out.add(getTreeDepth(depth + 1) + "Nations (" + nations.size() + "):");
        for (Nation nation : nations.values()) {
            out.addAll(nation.getTreeString(depth + 2));
        }
        
        Collection<Town> townsWithoutNation = dataSource.getTownsWithoutNation();
        out.add(getTreeDepth(depth + 1) + "Towns (" + townsWithoutNation.size() + "):");
        for (Town town : townsWithoutNation) {
            out.addAll(town.getTreeString(depth + 2));
        }
        
        Collection<Resident> residentsWithoutTown = dataSource.getResidentsWithoutTown();
        out.add(getTreeDepth(depth + 1) + "Residents (" + residentsWithoutTown.size() + "):");
        for (Resident resident : residentsWithoutTown) {
            out.addAll(resident.getTreeString(depth + 2));
        }
        return out;
    }
    
    private String getTreeDepth(int depth) {
        
        char[] fill = new char[depth * 4];
        Arrays.fill(fill, ' ');
        if (depth > 0) {
            fill[0] = '|';
            int offset = (depth - 1) * 4;
            fill[offset] = '+';
            fill[offset + 1] = '-';
            fill[offset + 2] = '-';
        }
        return new String(fill);
    }

    /*
     * PlotGroup Stuff.
     */

	public PlotGroup newGroup(Town town, String name, UUID id) throws AlreadyRegisteredException {
    	
    	// Create new plot group.
		PlotGroup newGroup = new PlotGroup(id, name, town);
		
		// Check if there is a duplicate
		if (town.hasPlotGroupName(newGroup.getName())) {
			TownyMessaging.sendErrorMsg("group " + town.getName() + ":" + id + " already exists"); // FIXME Debug message
			throw new AlreadyRegisteredException();
		}
		
		// Create key and store group globally.
		town.addPlotGroup(newGroup);
		
		return newGroup;
	}

	public UUID generatePlotGroupID() {
		return UUID.randomUUID();
	}

	public void removeGroup(PlotGroup group) {
		group.getTown().removePlotGroup(group);
		
	}

    public boolean hasGroup(String townName, UUID groupID) {
		Town t = towns.get(townName);
		
		if (t != null) {
			return t.getObjectGroupFromID(groupID) != null;
		}
		
		return false;
	}

	public boolean hasGroup(String townName, String groupName) {
		Town t = towns.get(townName);

		if (t != null) {
			return t.hasPlotGroupName(groupName);
		}

		return false;
	}

	/**
	 * Get all the plot object groups from all towns
	 * Returns a collection that does not reflect any group additions/removals
	 * 
	 * @return collection of PlotObjectGroup
	 */
	public Collection<PlotGroup> getGroups() {
    	List<PlotGroup> groups = new ArrayList<>();
    	
		for (Town town : towns.values()) {
			if (town.hasPlotGroups()) {
				groups.addAll(town.getPlotObjectGroups());
			}
		}
		
		return groups;
	}

	/**
	 * Gets the plot group from the town name and the plot group UUID 
	 * 
	 * @param townName Town name
	 * @param groupID UUID of the plot group
	 * @return PlotGroup if found, null if none found.
	 */
	public PlotGroup getGroup(String townName, UUID groupID) {
		Town t = null;
		try {
			t = TownyUniverse.getInstance().getDataSource().getTown(townName);
		} catch (NotRegisteredException e) {
			return null;
		}
		if (t != null) {
			return t.getObjectGroupFromID(groupID);
		}
		
		return null;
	}

	/**
	 * Gets the plot group from the town name and the plot group name
	 * 
	 * @param townName Town Name
	 * @param groupName Plot Group Name
	 * @return the plot group if found, otherwise null
	 */
	public PlotGroup getGroup(String townName, String groupName) {
		Town t = towns.get(townName);

		if (t != null) {
			return t.getPlotObjectGroupFromName(groupName);
		}

		return null;
	}

	/*
	 * Metadata Stuff
	 */

	public void addCustomCustomDataField(CustomDataField cdf) throws KeyAlreadyRegisteredException {
    	
    	if (this.getRegisteredMetadataMap().containsKey(cdf.getKey()))
    		throw new KeyAlreadyRegisteredException();
    	
    	this.getRegisteredMetadataMap().put(cdf.getKey(), cdf);
	}

	public Map<String, CustomDataField> getRegisteredMetadataMap() {
		return getRegisteredMetadata();
	}
	
	public Map<String, CustomDataField> getRegisteredMetadata() {
		return registeredMetadata;
	}

	/*
	 * Townblock Stuff
	 */

	/**
	 * How to get a TownBlock for now.
	 * 
	 * @param worldCoord we are testing for a townblock.
	 * @return townblock if it exists, otherwise null.
	 * @throws NotRegisteredException if there is no homeblock to get.
	 */
	public TownBlock getTownBlock(WorldCoord worldCoord) throws NotRegisteredException {
		if (hasTownBlock(worldCoord))
			return townBlocks.get(worldCoord);
		else 
			throw new NotRegisteredException();
	}

	/**
	 * Get Universe-wide ConcurrentHashMap of WorldCoords and their TownBlocks.
	 * Populated at load time from townblocks folder's files.
	 * 
	 * 
	 * @return townblocks hashmap read from townblock files.
	 */	
	public Map<WorldCoord, TownBlock> getTownBlocks() {
		return townBlocks;
	}
	
	public void addTownBlock(TownBlock townBlock) {
		if (hasTownBlock(townBlock.getWorldCoord()))
			return;
		townBlocks.put(townBlock.getWorldCoord(), townBlock);
	}

	/**
	 * Does this WorldCoord have a TownBlock?
	 * @param worldCoord - the coord for which we want to know if there is a townblock.
	 * @return true if Coord is a townblock
	 */	
	public boolean hasTownBlock(WorldCoord worldCoord) {
		return townBlocks.containsKey(worldCoord);
	}

	/**
	 * Remove one townblock from the TownyUniverse townblock map.
	 * @param townBlock to remove.
	 */
	public void removeTownBlock(TownBlock townBlock) {
		
		if (removeTownBlock(townBlock.getWorldCoord())) {
			try {
				if (townBlock.hasResident())
					townBlock.getResident().removeTownBlock(townBlock);
			} catch (NotRegisteredException e) {
			}
			try {
				if (townBlock.hasTown())
					townBlock.getTown().removeTownBlock(townBlock);
			} catch (NotRegisteredException e) {
			}
		}
	}
	
	/**
	 * Remove a list of TownBlocks from the TownyUniverse townblock map.
	 * @param townBlocks to remove.
	 */
	public void removeTownBlocks(List<TownBlock> townBlocks) {

		for (TownBlock townBlock : new ArrayList<>(townBlocks))
			removeTownBlock(townBlock);
	}

	/** 
	 * Removes a townblock at the given worldCoord from the TownyUniverse townblock map.
	 * @param worldCoord to remove.
	 * @return whether the townblock was successfully removed   
	 */
	private boolean removeTownBlock(WorldCoord worldCoord) {

		return townBlocks.remove(worldCoord) != null;
	}

	/*
	 * War Stuff
	 */

//    public void startWarEvent() {
//        warEvent = new War(towny, TownySettings.getWarTimeWarningDelay());
//    }
    
    public void endWarEvent() {
        if (warEvent != null && warEvent.isWarTime()) {
            warEvent.toggleEnd();
        }
    }	

    public void addWarZone(WorldCoord worldCoord) {
        try {
        	if (worldCoord.getTownyWorld().isWarAllowed())
            	worldCoord.getTownyWorld().addWarZone(worldCoord);
        } catch (NotRegisteredException e) {
            // Not a registered world
        }
        towny.updateCache(worldCoord);
    }
    
    public void removeWarZone(WorldCoord worldCoord) {
        try {
            worldCoord.getTownyWorld().removeWarZone(worldCoord);
        } catch (NotRegisteredException e) {
            // Not a registered world
        }
        towny.updateCache(worldCoord);
    }

    public War getWarEvent() {
        return warEvent;
    }
    
    public War getWarEvent(Player player) {
    	Resident resident = null;
		try {
			resident = getDataSource().getResident(player.getName());
		} catch (NotRegisteredException ignored) {}
		if (resident == null)
			return null;
        for (War war : getWars()) {
        	if (war.isWarringResident(resident))
        		return war;
        }
        return null;
    }
    
    public War getWarEvent(TownBlock townBlock) {
    	for (War war : getWars()) {
    		if (war.isWarZone(townBlock.getWorldCoord()))
    			return war;
    	}
    	return null;
    }

    public boolean hasWarEvent(TownBlock townBlock) {
    	for (War war : getWars()) {
    		if (war.isWarZone(townBlock.getWorldCoord()))
    			return true;
    	}
    	return false;
    }
    
    public boolean hasWarEvent(Town town) {
    	for (War war : getWars()) {
    		if (war.isWarringTown(town))
    			return true;
    	}
    	return false;
    }
    
    
    public void setWarEvent(War warEvent) {
        this.warEvent = warEvent;
    }
    
    public List<War> getWars() {
    	return wars;
    }
    
    public void addWar(War war) {
    	wars.add(war);
    }
    
    public void removeWar(War war) {
    	wars.remove(war);
    }
    
    @Deprecated
	public String getSaveDbType() {
		return TownySettings.getSaveDatabase();
	}
	
	@Deprecated
	public String getLoadDbType() {
		return TownySettings.getLoadDatabase();
	}
}
