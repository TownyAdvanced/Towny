package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.HashMap;

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
    
    private final ConcurrentHashMap<String, Resident> residents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Town> towns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Nation> nations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SiegeZone> siegeZones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TownyWorld> worlds = new ConcurrentHashMap<>();
    private final HashMap<String, CustomDataField> registeredMetadata = new HashMap<>();
    private final List<Resident> jailedResidents = new ArrayList<>();
    private final String rootFolder;
    private TownyDataSource dataSource;
    private TownyPermissionSource permissionSource;
    private War warEvent;
    
    private TownyUniverse() {
        towny = Towny.getPlugin();
        rootFolder = towny.getDataFolder().getPath();
    }
    
    // TODO: Put loadSettings into the constructor, since it is 1-time-run code.
    boolean loadSettings() {
        
        try {
            TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", towny.getVersion());
            TownySettings.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
            TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
            
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
		// Init logger
		TownyLogger.getInstance();
        
        String saveDbType = TownySettings.getSaveDatabase();
        String loadDbType = TownySettings.getLoadDatabase();
        
        // Setup any defaults before we load the dataSource.
        Coord.setCellSize(TownySettings.getTownBlockSize());
        
        System.out.println("[Towny] Database: [Load] " + loadDbType + " [Save] " + saveDbType);
        
        clearAll();
                
        if (!loadDatabase(loadDbType)) {
            System.out.println("[Towny] Error: Failed to load!");
            return false;
        }
        
        try {
            dataSource.cleanupBackups();
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
                default: {
                
                }
            }
            FileMgmt.checkOrCreateFolder(rootFolder + File.separator + "logs"); // Setup the logs folder here as the logger will not yet be enabled.
            try {
                dataSource.backup();
                
                if (loadDbType.equalsIgnoreCase("flatfile") || saveDbType.equalsIgnoreCase("flatfile")) {
                    dataSource.deleteUnusedResidents();
                }
                
            } catch (IOException e) {
                System.out.println("[Towny] Error: Could not create backup.");
                e.printStackTrace();
                return false;
            }
            
            if (loadDbType.equalsIgnoreCase(saveDbType)) {
                // Update all Worlds data files
                dataSource.saveAllWorlds();
            } else {
                //Formats are different so save ALL data.
                dataSource.saveAll();
            }
            
        } catch (UnsupportedOperationException e) {
            System.out.println("[Towny] Error: Unsupported save format!");
            return false;
        }
        
        File f = new File(rootFolder, "outpostschecked.txt");
        if (!(f.exists())) {
            for (Town town : dataSource.getTowns()) {
                TownySQLSource.validateTownOutposts(town);
            }
            towny.saveResource("outpostschecked.txt", false);
        }
        return true;
    }
    
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
    
    public void onLogin(Player player) {
        
        if (!player.isOnline())
            return;
        
        // Test and kick any players with invalid names.
        player.getName();
        if (player.getName().contains(" ")) {
            player.kickPlayer("Invalid name!");
            return;
        }
        
        // Perform login code in it's own thread to update Towny data.
        //new OnPlayerLogin(plugin, player).start();
        if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(towny, player), 0L) == -1) {
            TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
        }
        
    }
    
    public void onLogout(Player player) {
        
        try {
            Resident resident = dataSource.getResident(player.getName());
            resident.setLastOnline(System.currentTimeMillis());
            dataSource.saveResident(resident);
        } catch (NotRegisteredException ignored) {
        }
    }
    
    public void startWarEvent() {
        warEvent = new War(towny, TownySettings.getWarTimeWarningDelay());
    }
    
    //TODO: This actually breaks the design pattern, so I might just redo warEvent to never be null.
    //TODO for: Articdive
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
    
    public TownyPermissionSource getPermissionSource() {
        return permissionSource;
    }
    
    public void setPermissionSource(TownyPermissionSource permissionSource) {
        this.permissionSource = permissionSource;
    }
    
    public War getWarEvent() {
        return warEvent;
    }
    
    public void setWarEvent(War warEvent) {
        this.warEvent = warEvent;
    }
    
    public String getRootFolder() {
        return rootFolder;
    }
    
    public ConcurrentHashMap<String, Nation> getNationsMap() {
        return nations;
    }
    
    public ConcurrentHashMap<String, SiegeZone> getSiegeZonesMap() {
    	return siegeZones;
	}
    public ConcurrentHashMap<String, Resident> getResidentMap() {
        return residents;
    }
    
    public List<Resident> getJailedResidentMap() {
        return jailedResidents;
    }
    
    public ConcurrentHashMap<String, Town> getTownsMap() {
        return towns;
    }
    
    public ConcurrentHashMap<String, TownyWorld> getWorldMap() {
        return worlds;
    }
    
    public TownyDataSource getDataSource() {
        return dataSource;
    }
    
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
    
    /**
     * Pretty much this method checks if a townblock is contained within a list of locations.
     *
     * @param minecraftcoordinates - List of minecraft coordinates you should probably parse town.getAllOutpostSpawns()
     * @param tb                   - TownBlock to check if its contained..
     * @return true if the TownBlock is considered an outpost by it's Town.
     * @author Lukas Mansour (Articdive)
     */
    public boolean isTownBlockLocContainedInTownOutposts(List<Location> minecraftcoordinates, TownBlock tb) {
        if (minecraftcoordinates != null && tb != null) {
            for (Location minecraftcoordinate : minecraftcoordinates) {
                if (Coord.parseCoord(minecraftcoordinate).equals(tb.getCoord())) {
                    return true; // Yes the TownBlock is considered an outpost by the Town
                }
            }
        }
        return false;
    }
    
    public void addCustomCustomDataField(CustomDataField cdf) throws KeyAlreadyRegisteredException {
    	
    	if (this.getRegisteredMetadataMap().containsKey(cdf.getKey()))
    		throw new KeyAlreadyRegisteredException();
    	
    	this.getRegisteredMetadataMap().put(cdf.getKey(), cdf);
	}
    
    public static TownyUniverse getInstance() {
        if (instance == null) {
            instance = new TownyUniverse();
        }
        return instance;
    }
    
    public void clearAll() {
    	worlds.clear();
        nations.clear();
        towns.clear();
        residents.clear();
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
			return t.hasObjectGroupName(groupName);
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
			if (town.hasObjectGroups()) {
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

	public HashMap<String, CustomDataField> getRegisteredMetadataMap() {
		return getRegisteredMetadata();
	}

	public PlotGroup newGroup(Town town, String name, UUID id) throws AlreadyRegisteredException {
    	
    	// Create new plot group.
		PlotGroup newGroup = new PlotGroup(id, name, town);
		
		// Check if there is a duplicate
		if (town.hasObjectGroupName(newGroup.getName())) {
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
	
	public HashMap<String, CustomDataField> getRegisteredMetadata() {
		return registeredMetadata;
	}
}
