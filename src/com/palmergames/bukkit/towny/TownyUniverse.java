package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.migration.ConfigMigrator;
import com.palmergames.bukkit.towny.database.handler.DatabaseHandler;
import com.palmergames.bukkit.towny.database.handler.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.database.handler.SQLDatabaseHandler;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.DeletePlayerEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteNationEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
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
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.CleanupBackupTask;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Version;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.Trie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Towny's class for internal API Methods
 * If you don't want to change the dataSource, war, permissions or similar behavior
 * and only for example want to get Resident objects you should use {@link TownyAPI}
 *
 * @author Lukas Mansour (Articdive)
 */
public class TownyUniverse {
    private static TownyUniverse instance;
    private final Towny towny;
    
    private final Map<UUID, Resident> residents = new ConcurrentHashMap<>();
	private final Map<String, Resident> residentNamesMap = new ConcurrentHashMap<>();
    private final Trie residentsTrie = new Trie();
    
    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();
	private final Map<String, Town> townNamesMap = new ConcurrentHashMap<>();
    private final Trie townsTrie = new Trie();
    
    private final Map<UUID, Nation> nations = new ConcurrentHashMap<>();
	private final Map<String, Nation> nationNamesMap = new ConcurrentHashMap<>();
    private final Trie nationsTrie = new Trie();
    
    // Legacy Maps
	@Deprecated
	private final Map<String, Nation> nationMap = new ConcurrentHashMap<>();
	@Deprecated
	private final Map<String, Town> townMap = new ConcurrentHashMap<>();
	@Deprecated
	private final Map<String, Resident> residentMap = new ConcurrentHashMap<>();
	@Deprecated
	private final Map<WorldCoord, TownBlock> townBlockMap = new ConcurrentHashMap<>();
	@Deprecated
	private final Map<String, TownyWorld> worldMap = new ConcurrentHashMap<>();
	
    
    private final Map<UUID, TownyWorld> worlds = new ConcurrentHashMap<>();
	private final Map<String, TownyWorld> worldNameMap = new ConcurrentHashMap<>();
    private final Map<String, CustomDataField> registeredMetadata = new HashMap<>();
	private final Map<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private CompletableFuture<Void> backupFuture;
    
    private final List<Resident> jailedResidents = new ArrayList<>();
    private final String rootFolder;
    private TownyDataSource dataSource;
    private TownyPermissionSource permissionSource;
    private War warEvent;
    private DatabaseHandler databaseHandler;
    private String saveDbType;
    private String loadDbType;
    
    private TownyUniverse() {
        towny = Towny.getPlugin();
        rootFolder = towny.getDataFolder().getPath();
    }
    
    // TODO: Put loadSettings into the constructor, since it is 1-time-run code.
    boolean loadSettings() {
        
        try {
            TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", towny.getVersion());
            Translation.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
            TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
            
        } catch (IOException | TownyException e) {
            e.printStackTrace();
            return false;
        }
		// Init logger
		TownyLogger.getInstance();

        saveDbType = TownySettings.getSaveDatabase();
        loadDbType = TownySettings.getLoadDatabase();
        
        System.out.println("[Towny] Database: [Load] " + loadDbType + " [Save] " + saveDbType);
        
        clearAll();
                
//        long startTime = System.currentTimeMillis();
//        if (!loadDatabase(loadDbType)) {
//            System.out.println("[Towny] Error: Failed to loadString!");
//            return false;
//        }
//        long time = System.currentTimeMillis() - startTime;
//        System.out.println("[Towny] Database loaded in " + time + "ms.");

		// Figure out if legacy or not.
		boolean isLegacy = false;
		if (new File(getRootFolder() + "/data/towns.txt").exists()) {
			isLegacy = true;
		}
        
        try {
            // Set the new class for saving.
            switch (saveDbType.toLowerCase()) {
                case "ff":
                case "flatfile": {
                    this.dataSource = new TownyFlatFileSource(towny, this);
                    this.databaseHandler = new FlatFileDatabaseHandler();
                    break;
                }
                case "h2":
					this.databaseHandler = new SQLDatabaseHandler("h2");
					this.dataSource = new TownySQLSource(towny, this, saveDbType.toLowerCase());
					break;
                case "sqlite":
					this.databaseHandler = new SQLDatabaseHandler("sqlite");
					this.dataSource = new TownySQLSource(towny, this, saveDbType.toLowerCase());
					break;
                case "mysql": {
                	this.databaseHandler = new SQLDatabaseHandler("mysql");
                    this.dataSource = new TownySQLSource(towny, this, saveDbType.toLowerCase());
                    break;
                }
                default: {
                
                }
            }
            FileMgmt.checkOrCreateFolder(rootFolder + File.separator + "logs"); // Setup the logs folder here as the logger will not yet be enabled.
			
            if (loadDbType.equalsIgnoreCase(saveDbType)) {
                // Update all Worlds data files
                //dataSource.saveAllWorlds(); // TODO: Replacement or not?
            } else {
                //Formats are different so getString ALL data.
                //dataSource.saveAll(); // TODO: Replacement or not?
            }
            
            // Load all the world files in.
			
			if (isLegacy) {
				databaseHandler.upgrade();
			} else {
				databaseHandler.loadAll();
			}
			
            
        } catch (UnsupportedOperationException e) {
            System.out.println("[Towny] Error: Unsupported getString format!");
            e.printStackTrace();
            return false;
        }
        
        Version lastRunVersion = new Version(TownySettings.getLastRunVersion(Towny.getPlugin().getVersion()));
        Version curVersion = new Version(Towny.getPlugin().getVersion());
        
        // Only migrate if the user just updated.
        if (!lastRunVersion.equals(curVersion)) {
			System.out.println("[Towny] Performing Config Migrations...");
			ConfigMigrator migrator = new ConfigMigrator(TownySettings.getConfig(), "config-migration.json");
			migrator.migrate();
		}
        
        File f = new File(rootFolder, "outpostschecked.txt");
        if (!(f.exists())) {
            for (Town town : dataSource.getTowns()) {
                TownySQLSource.validateTownOutposts(town);
            }
            towny.saveResource("outpostschecked.txt", false);
        }

		// Run both the backup cleanup and backup async.
		performBackup();
        
        return true;
    }

	public void performBackup() {
		backupFuture = CompletableFuture
			.runAsync(new CleanupBackupTask())
			.thenRunAsync(new BackupTask());
	}
    
    public boolean loadLegacyDatabase(String loadDbType) {
        
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
    
    public void finishTasks() {
    	if (backupFuture != null) {
			// Join into main thread for proper termination.
			backupFuture.join();
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

	// ---------- Resident Methods ----------
    
    public boolean hasResident(@NotNull UUID uuid) {
    	return residents.containsKey(uuid);
	}
	
	public Resident getResident(@NotNull UUID uuid) throws NotRegisteredException {
    	Resident r = residents.get(uuid);
    	
    	if (r == null)
			throw new NotRegisteredException(String.format("The resident with UUID '%s' is not registered.", uuid));
    	
    	return r;
	}
	
	public boolean hasResident(String name) {
    	try {
    		return residentNamesMap.containsKey(NameValidation.checkAndFilterPlayerName(name).toLowerCase());
		} catch (InvalidNameException ignored) {
    		return false;
		}
	}
	
	public Resident getResident(@NotNull String name) throws NotRegisteredException {
		try {
			name = NameValidation.checkAndFilterPlayerName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}
		
		Resident resident = residentNamesMap.get(name);
		
		if (resident == null)
			throw new NotRegisteredException(String.format("The resident '%s' is not registered.", name));
		else if (TownySettings.isFakeResident(name)) {
			resident = new Resident(UUID.randomUUID(), name);
			resident.setNPC(true);
		}
		
		return resident;
	}
	
	@NotNull
	public Collection<Resident> getResidents(@NotNull String... residents) {
    	return getResidents(null, residents);
	}
	
	@NotNull
	public Collection<Resident> getResidents(@Nullable Player errorReceiver, @NotNull String... residents) {
    	Objects.requireNonNull(residents);
    	
    	List<Resident> residentList = new ArrayList<>(residents.length);
    	
		for (String residentName : residents) {
			try {
				residentList.add(getResident(residentName));
			} catch (NotRegisteredException ex) {
				if (errorReceiver != null)
					TownyMessaging.sendErrorMsg(errorReceiver, ex.getMessage());
			}
		}
    	return Collections.unmodifiableList(residentList);
	}

	@Deprecated
	public final @NotNull Resident newResident(Player player) throws AlreadyRegisteredException, NotRegisteredException {
		Objects.requireNonNull(player);

		UUID playerID = player.getUniqueId();
		String playerName = player.getName();
		
		if (residents.containsKey(playerID)) {
			throw new AlreadyRegisteredException("The resident id " + player + " is already in use.");
		}

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(playerName);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		Resident newResident = new Resident(playerID, filteredName);
		newResident.save();

		residents.put(playerID, newResident);
		residentNamesMap.put(filteredName.toLowerCase(), newResident);
		residentsTrie.addKey(filteredName);

		return newResident;
	}
	
	public final @NotNull Resident newNPC(String name) throws AlreadyRegisteredException {
    	Objects.requireNonNull(name);
    	
    	UUID npcID = UUID.randomUUID();
    	
    	if (residents.containsKey(npcID)) {
			throw new AlreadyRegisteredException("The resident id " + npcID + " is already in use.");
		}
    	
    	if (residentNamesMap.containsKey(name)) {
			throw new AlreadyRegisteredException("The npc name " + name + " is already in use.");
		}
    	
    	Resident npc = new Resident(npcID, name);
    	npc.save();
    	
    	residents.put(npcID, npc);
    	residentNamesMap.put(name.toLowerCase(), npc);
    	
    	return npc;
	}

	public final void addResident(Resident resident) throws AlreadyRegisteredException {
		Objects.requireNonNull(resident);
		UUID residentID = resident.getUniqueIdentifier();
		String residentName = resident.getName();

		if (residents.containsKey(residentID)) {
			throw new AlreadyRegisteredException("The resident id " + resident.getUniqueIdentifier() + " is already in use.");
		}

		residents.put(residentID, resident);
		residentNamesMap.put(residentName.toLowerCase(), resident);
		residentsTrie.addKey(residentName);
		
		// Attach connections.
		try {
			Town town = resident.getTown();
			town.addResident(resident);

			if (resident.isMayor() && town.getMayor() != resident) {
				town.setMayor(resident);
			}

		} catch (TownyException ignored) {}
	}

	public void removeResident(Resident resident) {
    	String residentName = resident.getName();
    	UUID residentUUID = resident.getUniqueIdentifier();
    	
    	// Check if resident was in a town
		if (resident.hasTown()) {
			try {
				Town town = resident.getTown();
			} catch (NotRegisteredException e1) {
				// Should never get thrown
				e1.printStackTrace();
			}
		}

		// Remove the resident from outlaws in different towns
		for (Town townOutlaw : towns.values()) {
			if (townOutlaw.hasOutlaw(resident)) {
				townOutlaw.removeOutlaw(resident);
			}
		}

		// This should be called at the end, but to keep legacy behavior call it here.
		BukkitTools.getPluginManager().callEvent(new DeletePlayerEvent(resident.getName()));
		
		// Remove the resident from resident friends
		for (Resident toCheck : residents.values()) {
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
			if (toCheck.hasFriend(resident)) {
				TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
				toCheck.removeFriend(resident);
				toCheck.save();
			}
		}
		// Delete the residents file.
		databaseHandler.delete(resident);
		
		// Remove the residents record from memory.
		residents.remove(residentUUID);
		residentNamesMap.remove(residentName.toLowerCase());
		residentsTrie.removeKey(residentName);

		Towny.getPlugin().deleteCache(residentName);
	}

	public void updateResidentName(String oldName, String newName) {
		Resident resident = residentNamesMap.remove(oldName);
		if (resident != null) {
			residentsTrie.removeKey(oldName);
			residentNamesMap.put(newName.toLowerCase(), resident);
			residentsTrie.addKey(newName);
		}
	}
	
    @Deprecated
    public Map<String, Resident> getResidentMap() {
        return residentMap;
    }
    
    @NotNull
    public Collection<Resident> getResidents() {
    	return Collections.unmodifiableCollection(residents.values()); 
	}

	public Trie getResidentsTrie() {
		return residentsTrie;
	}
	
    public List<Resident> getJailedResidentMap() {
        return jailedResidents;
    }

	// ---------- Town Methods ----------
    
    public boolean hasTown(UUID town) {
    	return towns.containsKey(town);
	}
    
    public Town getTown(UUID townID) throws NotRegisteredException {
    	
    	if (townID == null) {
			return null;
		}
    	
    	Town t = towns.get(townID);
    	
    	if (t == null) 
			throw new NotRegisteredException(String.format("The townID with UUID '%s' is not registered.", townID));;
			
    	return t;
	}

	public boolean hasTown(String name) {
    	return townNamesMap.containsKey(name);
	}

	/**
	 * Fetches the {@link Town} from the memory cache.
	 *
	 * Note: This is fetch is not the most accurate, and 
	 * should only be used when in the context of a command.
	 *
	 * @param name The name of the town.
	 * @return The town with the name, null otherwise.
	 */
	public Town getTown(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
		}
		
		Town t = townNamesMap.get(name);
		
		if (t == null)
			throw new NotRegisteredException(String.format("The town '%s' is not registered.", name));
		
		return t;
	}
	
	@NotNull
	public Collection<Town> getTowns(@NotNull String... names) {
		Objects.requireNonNull(names);
		
		List<Town> matches = new ArrayList<>(names.length);
		for (String name : names) {
			try {
				matches.add(getTown(name));
			} catch (NotRegisteredException ignored) {
			}
		}
		
		return Collections.unmodifiableList(matches);
	}

	/**
	 * Creates a new {@link Town} and saves it into the database.
	 *
	 * @param name The name of the town.
	 * @throws AlreadyRegisteredException When the name is taken.
	 * @throws NotRegisteredException When the name is invalid.
	 */
	public final Town newTown(@NotNull String name) throws AlreadyRegisteredException, NotRegisteredException {
		Objects.requireNonNull(name);
		
		// Check if name is valid.
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}
		
		// Check if name already exists.
		if (townNamesMap.containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

		// Create new town and save it.
		Town newTown = new Town(UUID.randomUUID(), filteredName);

		// Save town
		newTown.save();

		// Add town to memory.
		towns.put(newTown.getUniqueIdentifier(), newTown);
		townNamesMap.put(filteredName.toLowerCase(), newTown);
		townsTrie.addKey(filteredName);
		
		return newTown;
	}
	
	public final @NotNull Town addTown(@NotNull Town town) throws AlreadyRegisteredException {
		Objects.requireNonNull(town);
		
		if (towns.containsKey(town.getUniqueIdentifier())) {
			throw new AlreadyRegisteredException("The town " + town.getName() + " is already in use.");
		}
		
		// Attach connections
		if (town.hasNation()) {
			try {
				Nation nation = town.getNation();
				nation.addTown(town);
			} catch (NotRegisteredException e) {
				e.printStackTrace();
			}
		}
		
		// Store into memory.
		towns.put(town.getUniqueIdentifier(), town);
		townNamesMap.put(town.getName().toLowerCase(), town);
		townsTrie.addKey(town.getName());
		
		return town;
	}

	public void updateTownName(String oldName, String newName) {
		Town town = townNamesMap.remove(oldName);
		if (town != null) {
			townsTrie.removeKey(oldName);
			townNamesMap.put(newName.toLowerCase(), town);
			townsTrie.addKey(newName);
		}
	}

	public void removeTown(Town town) {

		PreDeleteTownEvent preEvent = new PreDeleteTownEvent(town);
		BukkitTools.getPluginManager().callEvent(preEvent);

		if (preEvent.isCancelled())
			return;
		
		String townName = town.getName();
		UUID townUUID = town.getUniqueIdentifier();

		unclaimAllTownBlocks(town);
		
		TownyWorld townyWorld = town.getHomeblockWorld();

		// Remove town from nation
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
		
		// Remove residents
		for (Resident res : town.getResidents()) {
			try {
				res.setTown(null);
				res.save();
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
		}

		// Look for residents inside of this town's jail and free them
		for (Resident jailedRes : TownyUniverse.getInstance().getJailedResidentMap()) {
			if (jailedRes.hasJailTown(town.getName())) {
				jailedRes.setJailed(jailedRes, 0, town);
				jailedRes.save();
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
		townyWorld.save();

		// Delete town from DB
		databaseHandler.delete(town);
		
		towns.remove(townUUID);
		townsTrie.removeKey(townName);
		townNamesMap.remove(townName);
		
		Towny.getPlugin().resetCache();

		BukkitTools.getPluginManager().callEvent(new DeleteTownEvent(town.getName()));
	}

	public List<Town> getTowns() {
		return new ArrayList<>(towns.values());
	}
    
    @Deprecated
    public Map<String, Town> getTownsMap() {
        return townMap;
    }
    
    public Trie getTownsTrie() {
    	return townsTrie;
	}

	// ---------- Nation Methods ----------

	public boolean hasNation(UUID uuid) {
    	return nations.containsKey(uuid);
	}
	
	public Nation getNation(UUID uuid) throws NotRegisteredException {
    	Nation n = nations.get(uuid);
    	
    	if (n == null)
			throw new NotRegisteredException(String.format("The nation with UUID '%s' is not registered.", uuid));
    	
    	return n;
	}

	public boolean hasNation(String name) {
		return nationNamesMap.containsKey(name.toLowerCase());
	}
	
	public Nation getNation(String name) throws NotRegisteredException {

		try {
			name = NameValidation.checkAndFilterName(name).toLowerCase();
		} catch (InvalidNameException ignored) {
			return null;
		}
		
		Nation n = nationNamesMap.get(name.toLowerCase());
		
		if (n == null)
			throw new NotRegisteredException(String.format("The nation '%s' is not registered.", name));

		return n;
	}

	public Collection<Nation> getNations(@NotNull String... names) { 
		Objects.requireNonNull(names); 
		
		List<Nation> matches = new ArrayList<>(names.length);
		for (String name : names) {
			try {
				matches.add(getNation(name));
			} catch (NotRegisteredException ignored) {
			}
		}
		return Collections.unmodifiableCollection(matches);
	}

	public Nation newNation(@NotNull String name) throws AlreadyRegisteredException, NotRegisteredException {
		Objects.requireNonNull(name);
		
		// Validate name
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			throw new NotRegisteredException(e.getMessage());
		}

		if (nationNamesMap.containsKey(filteredName.toLowerCase()))
			throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

		Nation newNation = new Nation(UUID.randomUUID(), filteredName);
		
		// Save nation
		newNation.save();
		
		// Add nation to memory
		nations.put(newNation.getUniqueIdentifier(), newNation);
		nationNamesMap.put(filteredName.toLowerCase(), newNation);
		nationsTrie.addKey(filteredName);
		
		return newNation;
	}

	public final @NotNull Nation addNation(@NotNull Nation nation) throws AlreadyRegisteredException {
		Objects.requireNonNull(nation);

		if (nations.containsKey(nation.getUniqueIdentifier())) {
			throw new AlreadyRegisteredException("The nation " + nation.getName() + " is already in use.");
		}

		// Store into memory.
		nations.put(nation.getUniqueIdentifier(), nation);
		nationNamesMap.put(nation.getName().toLowerCase(), nation);
		townsTrie.addKey(nation.getName());

		return nation;
	}

	public void updateNationName(String oldName, String newName) {
		Nation nation = nationNamesMap.remove(oldName);
		if (nation != null) {
			nationsTrie.removeKey(oldName);
			nationNamesMap.put(newName.toLowerCase(), nation);
			nationsTrie.addKey(newName);
		}
	}

	public void removeNation(Nation nation) {

		PreDeleteNationEvent preEvent = new PreDeleteNationEvent(nation.getName());
		BukkitTools.getPluginManager().callEvent(preEvent);

		if (preEvent.isCancelled())
			return;
		
		String nationName = nation.getName();
		UUID nationUUID = nation.getUniqueIdentifier();

		// Search and remove from all ally/enemy lists
		for (Nation toCheck : nations.values()) {
			if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
				try {
					if (toCheck.hasAlly(nation))
						toCheck.removeAlly(nation);
					else
						toCheck.removeEnemy(nation);

					toCheck.save();
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
			}
		}

		// Transfer any money to the warchest.
		if (TownyEconomyHandler.isActive()) {
			try {
				nation.getAccount().payTo(nation.getAccount().getHoldingBalance(), new WarSpoils(), "Remove Nation");
				nation.getAccount().removeAccount();
			} catch (Exception ignored) {
			}
		}

		// The clear method will also remove surnames/titles from residents as well
		nation.clear();
		
		// Remove nation from DB
		databaseHandler.delete(nation);

		// Remove from memory
		nations.remove(nationUUID);
		nationsTrie.removeKey(nationName);
		nationNamesMap.remove(nationName.toLowerCase());
		

		Towny.getPlugin().resetCache();

		BukkitTools.getPluginManager().callEvent(new DeleteNationEvent(nation.getName()));
	}
	
	@NotNull
	public Collection<Nation> getNations() {
		return Collections.unmodifiableCollection(nations.values());
	}
	
	@Deprecated
	public Map<String, Nation> getNationsMap() {
		return nationMap;
	}

	public Trie getNationsTrie() {
		return nationsTrie;
	}

	// ---------- World Methods ----------
	
	public TownyWorld getWorld(String name) throws NotRegisteredException {
		TownyWorld world = worldNameMap.get(name);
    	if (world == null) {
    		throw new NotRegisteredException(String.format("The world '%s' is not registered.", name));
		}
    	
    	return world;
	}
	
	@Deprecated
	public TownyWorld _getWorld(String name) throws NotRegisteredException {
		TownyWorld world = worldMap.get(name);
		if (world == null) {
			throw new NotRegisteredException(String.format("The world '%s' is not registered.", name));
		}

		return world;
	}
	
	public TownyWorld getWorld(UUID uuid) throws NotRegisteredException {
    	TownyWorld world = worlds.get(uuid);
    	if (world == null) {
			throw new NotRegisteredException(String.format("The world with UUID '%s' is not registered.", uuid));
		}
    	
    	return world;
	}

	public TownyWorld getTownWorld(Town town) {
		TownyWorld firstWorld = null;
		for (TownyWorld world : worlds.values()) {
			if (firstWorld == null)
				firstWorld = world;
			
			if (world.hasTown(town))
				return world;
		}

		// If this has failed the Town has no land claimed at all but should be given a world regardless.
		return firstWorld;
	}
	
	public TownyWorld getTownWorld(String townName) {
		try {
			getTownWorld(getTown(townName));
		} catch (NotRegisteredException ignore) {
		}

		// If this has failed the Town has no land claimed at all but should be given a world regardless.
		for (TownyWorld value : worlds.values()) {
			return value;
		}
		return null;
	}
	
	public final TownyWorld addWorld(TownyWorld world) throws AlreadyRegisteredException {
		Objects.requireNonNull(world);

		if (worlds.containsKey(world.getUniqueIdentifier())) {
			throw new AlreadyRegisteredException("The world " + world.getName() + " is already in use.");
		}

		// Store into memory.
		worlds.put(world.getUniqueIdentifier(), world);
		worldNameMap.put(world.getName().toLowerCase(), world);

		return world;
	}

	public final void newWorld(UUID id, String name) throws AlreadyRegisteredException, NotRegisteredException {
		// Get bukkit world.
		World world = Bukkit.getWorld(id);

		if (world == null) {
			throw new NotRegisteredException("World doesn't exist");
		}

		if (worlds.containsKey(id)) {
			throw new AlreadyRegisteredException("The world with uuid " + id + " is already in use.");
		}

		UUID uuid = world.getUID();
		TownyWorld newWorld = new TownyWorld(uuid, name);
		newWorld.save();

		worlds.put(uuid, newWorld);
		worldNameMap.put(name.toLowerCase(), newWorld);
	}
	
	@NotNull
	public Collection<TownyWorld> getWorlds() { return Collections.unmodifiableCollection(worlds.values()); }
	
	@Deprecated
    public Map<String, TownyWorld> getWorldMap() {
        return worldMap;
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
        townBlocks.clear();
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
	 * @param townID Town name
	 * @param groupID UUID of the plot group
	 * @return PlotGroup if found, null if none found.
	 */
	public PlotGroup getGroup(UUID townID, UUID groupID) {
		try {
			return getTown(townID).getObjectGroupFromID(groupID);
		} catch (NotRegisteredException ignore) {
		}
		
		return null;
	}

	/**
	 * @deprecated Use {@link TownyUniverse#getGroup(UUID, UUID)}
	 * Gets the plot group from the town name and the plot group UUID 
	 * 
	 * @param townName Town name
	 * @param groupID UUID of the plot group
	 * @return PlotGroup if found, null if none found.
	 */
	@Deprecated
	public PlotGroup getGroup(String townName, UUID groupID) {
		try {
			return getTown(townName).getObjectGroupFromID(groupID);
		} catch (NotRegisteredException ignore) {
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
		try {
			return getTown(townName).getPlotObjectGroupFromName(groupName);
		} catch (NotRegisteredException ignore) {
		}

		return null;
	}

	public Map<String, CustomDataField> getRegisteredMetadataMap() {
		return getRegisteredMetadata();
	}

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
	
	public Map<String, CustomDataField> getRegisteredMetadata() {
		return registeredMetadata;
	}

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

	@Deprecated
	public TownBlock _getTownBlock(WorldCoord worldCoord) throws NotRegisteredException {
		if (hasTownBlock(worldCoord))
			return townBlockMap.get(worldCoord);
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
	@Deprecated
	public Map<WorldCoord, TownBlock> _getTownBlocks() {
		return townBlockMap;
	}
	
	@NotNull
	public Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks.values());
	}
	
	public void addTownBlock(@NotNull TownBlock townBlock) throws AlreadyRegisteredException {
		
		if (townBlocks.containsKey(townBlock.getWorldCoord())) {
			throw new AlreadyRegisteredException("Town block " + townBlock + " already exists");
		}

		townBlocks.put(townBlock.getWorldCoord(), townBlock);

		Town town;
		Resident resident;
		try {
			// Attach town.
			if (townBlock.hasTown()) {
				town = townBlock.getTown();
				town.addTownBlock(townBlock);

				// Check if homeblock
				if (townBlock.isHomeBlock()) {
					town.forceSetHomeBlock(townBlock);
				}
			}
			
			// Attach Resident
			if (townBlock.hasResident()) {
				resident = townBlock.getResident();
				resident.addTownBlock(townBlock);
			}
		} catch (TownyException e) {
			e.printStackTrace();
		}
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

				if (townBlock.hasTown())
					townBlock.getTown().removeTownBlock(townBlock);
			} catch (NotRegisteredException ignored) {}
		}
	}
	
	public void unclaimAllTownBlocks(Town town) {
		for (TownBlock townBlock : new ArrayList<>(town.getTownBlocks())) {
			unclaimTownBlock(townBlock);
		}
	}

	public void unclaimTownBlock(TownBlock townBlock) {
		TownPreUnclaimEvent event = new TownPreUnclaimEvent(townBlock);
		BukkitTools.getPluginManager().callEvent(event);

		if (event.isCancelled())
			return;

		Town town = null;
		
		try {
			town = townBlock.getTown();
		} catch (NotRegisteredException ignored) {
		}

		removeTownBlock(townBlock);
		
		// Delete townblock from DB
		databaseHandler.delete(townBlock);

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


	public String getSaveDbType() {
		return saveDbType;
	}

	public String getLoadDbType() {
		return loadDbType;
	}
	
	public DatabaseHandler getDatabaseHandler() {
		return databaseHandler;
	}

	public Map<String, TownyWorld> getWorldNameMap() {
		return worldNameMap;
	}
}

