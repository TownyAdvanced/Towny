package com.palmergames.bukkit.towny;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.db.migration.SQLDatabaseMigrator;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Position;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnPoint;
import com.palmergames.bukkit.towny.object.SpawnPointLocation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.map.TownyMapData;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.CleanupTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.Trie;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Towny's class for internal API Methods
 * If you don't want to change the dataSource, war, permissions or similar behavior
 * and only for example want to get Resident objects you should use {@link TownyAPI}
 *
 * @author Articdive
 */
public class TownyUniverse {
    private static TownyUniverse instance;
    private final Towny towny;
    
    private final Map<UUID, Resident> residentUUIDMap = new ConcurrentHashMap<>();
    private final Map<String, Resident> residentNameMap = new ConcurrentHashMap<>();
    private final Trie residentsTrie = new Trie();
    
    private final Map<String, Town> townNameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Town> townUUIDMap = new ConcurrentHashMap<>();
    private final Trie townsTrie = new Trie();
    
    private final Map<String, Nation> nationNameMap = new ConcurrentHashMap<>();
	private final Map<UUID, Nation> nationUUIDMap = new ConcurrentHashMap<>();
    private final Trie nationsTrie = new Trie();
    
    private final Map<String, TownyWorld> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, TownyWorld> worldUUIDMap = new ConcurrentHashMap<>();
    
    private final Map<String, CustomDataField<?>> registeredMetadata = new HashMap<>();
	private final Map<WorldCoord, TownBlock> townBlocks = new ConcurrentHashMap<>();
	private CompletableFuture<Void> backupFuture;
    
	private final Map<SpawnPointLocation, SpawnPoint> spawnPoints = new ConcurrentHashMap<>(); 
    private final List<Resident> jailedResidents = new ArrayList<>();
    private final Map<UUID, Jail> jailUUIDMap = new ConcurrentHashMap<>();
    private final Map<String, String> replacementNamesMap = new ConcurrentHashMap<>();
    private final Map<UUID, PlotGroup> plotGroupUUIDMap = new ConcurrentHashMap<>();
    private final Map<UUID, District> districtUUIDMap = new ConcurrentHashMap<>();
    
    private final Map<WorldCoord, TownyMapData> wildernessMapDataMap = new ConcurrentHashMap<WorldCoord, TownyMapData>();
    private final String rootFolder;
	private TownyDataSource loadDataSource;
	private TownyDataSource saveDataSource;
    private TownyPermissionSource permissionSource;

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
     * Performs CleanupTask and BackupTask in async,
     */
    public void performCleanupAndBackup() {
		backupFuture = CompletableFuture
			.runAsync(new CleanupTask())
			.thenRunAsync(new BackupTask());
	}
    
    /**
     * Clears the object maps.
     */
    public void clearAllObjects() {
    	worlds.clear();
        nationNameMap.clear();
        nationUUIDMap.clear();
        townNameMap.clear();
        townUUIDMap.clear();
        residentNameMap.clear();
        residentUUIDMap.clear();
        townBlocks.clear();
        spawnPoints.clear();
        jailUUIDMap.clear();
        plotGroupUUIDMap.clear();
        wildernessMapDataMap.clear();
        replacementNamesMap.clear();
    }
    
    /**
     * Test loading and saving the database.
     * 
     * @param loadDbType - load setting from the config.
     * @param saveDbType - save setting from the config.
     */
	void loadAndSaveDatabase(String loadDbType, String saveDbType) throws TownyInitException {
    	towny.getLogger().info("Database: [Load] " + loadDbType + " [Save] " + saveDbType);
		
		loadDatabase(loadDbType);
		saveDatabase(saveDbType);
		
		// Dispose of the load data source if it's no longer needed
		if (this.loadDataSource != this.saveDataSource) {
			this.loadDataSource.finishTasks();
			this.loadDataSource = null;
		}
    }
    
    /**
     * Loads the database into memory.
     *  
     * @param loadDbType - load setting from the config.
     * @return true when the database will load.
     */
    private boolean loadDatabase(String loadDbType) {
        
        long startTime = System.currentTimeMillis();

        /*
         * Select the datasource.
         */
        switch (loadDbType.toLowerCase(Locale.ROOT)) {
            case "ff":
            case "flatfile": {
                this.loadDataSource = new TownyFlatFileSource(towny, this);
                break;
            }
            case "mysql": {
                this.loadDataSource = new TownySQLSource(towny, this);
                break;
            }
            default: {
            	throw new TownyInitException("Database: Database.yml unsupported load format: " + loadDbType, TownyInitException.TownyError.DATABASE_CONFIG);
            }
        }
		
		// Loading the database can cause saving, so save that back to the load source
		this.saveDataSource = this.loadDataSource;
        
        /*
         * Load the actual database.
         */
        if (!loadDataSource.loadAll())
        	throw new TownyInitException("Database: Failed to load database.", TownyInitException.TownyError.DATABASE);

        long time = System.currentTimeMillis() - startTime;
        towny.getLogger().info("Database: Loaded in " + time + "ms.");

        // Throw Event.
        BukkitTools.fireEvent(new TownyLoadedDatabaseEvent());

		// The migrator does nothing on flatfile except for bumping the database version
		final SQLDatabaseMigrator migrator = new SQLDatabaseMigrator(towny);
		migrator.migrateIfNeeded();
        
        // Congratulations the Database loaded.
       	return true;
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
            switch (saveDbType.toLowerCase(Locale.ROOT)) {
                case "ff":
                case "flatfile": {
                    this.saveDataSource = this.loadDataSource instanceof TownyFlatFileSource ? this.loadDataSource : new TownyFlatFileSource(towny, this);
                    break;
                }
                case "mysql": {
                    this.saveDataSource = this.loadDataSource instanceof TownySQLSource ? this.loadDataSource : new TownySQLSource(towny, this);
                    break;
                }
                default: {
                	throw new TownyInitException("Database.yml contains unsupported save format: " + saveDbType, TownyInitException.TownyError.DATABASE);
                }
            }

            if (TownySettings.getLoadDatabase().equalsIgnoreCase(saveDbType)) {
                // Update all Worlds data files
                saveDataSource.saveAllWorlds();                
            } else {
                //Formats are different so save ALL data.
                saveDataSource.saveAll();
            }
            return true;
        } catch (UnsupportedOperationException e) {
        	throw new TownyInitException("Database: Failed to save database!", TownyInitException.TownyError.DATABASE);
        }
    }

    /**
     * Run during onDisable() to finish cleanup and backup.
     */
    public void finishTasks() {
    	if (backupFuture != null && !backupFuture.isDone()) {
			// Join into main thread for proper termination.
			try {
				backupFuture.get(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				towny.getLogger().warning("Timed out waiting for backup task to finish.");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException ignored) {}
		}
	}

    /*
     * DataSource, PermissionSource and RootFolder.
     */

    public TownyDataSource getDataSource() {
        return saveDataSource;
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

	// =========== Resident Methods ===========

	/**
	 * Check if a resident exists with the passed in name.
	 * Will return true for fake residents and registered NPCs.
	 * 
	 * @param residentName Resident name to check for.
	 * @return whether Towny has a resident matching that name.
	 */
	public boolean hasResident(@NotNull String residentName) {
		Preconditions.checkNotNull(residentName, "Resident name cannot be null!");
		
		if (residentName.isEmpty())
			return false;
		
		if (TownySettings.isFakeResident(residentName))
			return true;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(residentName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException ignored) {
			return false;
		}
		
		return residentNameMap.containsKey(filteredName);
	}

	/**
	 * Check if a resident exists matching the passed in UUID.
	 * 
	 * @param residentUUID UUID of the resident to check.
	 * @return whether the resident matching the UUID exists.
	 */
	public boolean hasResident(@NotNull UUID residentUUID) {
		Preconditions.checkNotNull(residentUUID, "Resident uuid cannot be null!");
		
		return residentUUIDMap.containsKey(residentUUID);
	}

	/**
	 * Get the resident matching the passed in name.
	 * 
	 * Any fake residents (not registered NPCs) will return a new instance of a resident on method call.
	 * 
	 * @param residentName Name of the resident to fetch.
	 * @return the resident matching the given name or {@code null} if no resident is found.
	 */
	@Nullable
	public Resident getResident(@NotNull String residentName) {
		Preconditions.checkNotNull(residentName, "Resident name cannot be null!");

		if (residentName.isEmpty())
			return null;

		String filteredName = residentName;
		try {
			filteredName = NameValidation.checkAndFilterPlayerName(residentName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException ignored) {
		}
		
		Resident res = residentNameMap.get(filteredName);

		if (res == null && TownySettings.isFakeResident(residentName)) {
			Resident npc = new Resident(residentName, JavaUtil.changeUUIDVersion(UUID.randomUUID(), 2));
			npc.setNPC(true);
			return npc;
		}
		
		return res;
	}

	/**
	 * Get an optional instance of the resident matching the passed in name.
	 * 
	 * @param residentName Name of the resident to fetch.
	 * @return Optional object that may contain the resident matching the given name.
	 */
	@NotNull
	public Optional<Resident> getResidentOpt(@NotNull String residentName) {
		return Optional.ofNullable(getResident(residentName));
	}

	/**
	 * Get the resident with the passed-in UUID.
	 *
	 * @param residentUUID UUID of the resident to get.
	 * @return the resident with the passed-in UUID or {@code null} if no resident is found.
	 */
	@Nullable
	public Resident getResident(@NotNull UUID residentUUID) {
		Preconditions.checkNotNull(residentUUID, "Resident uuid cannot be null!");
		
		return residentUUIDMap.get(residentUUID);
	}
	
	/**
	 * Get an optional object that may contain the resident with the passed-in UUID.
	 *
	 * @param residentUUID UUID of the resident to get.
	 * @return an optional object that may contain the resident with the passed-in UUID. 
	 */
	@NotNull
	public Optional<Resident> getResidentOpt(@NotNull UUID residentUUID) {
		return Optional.ofNullable(getResident(residentUUID));
	}
	
	// Internal Use Only
	public void registerResidentUUID(@NotNull Resident resident) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(resident, "Resident cannot be null!");
		
		if (resident.getUUID() != null) {
			if (residentUUIDMap.putIfAbsent(resident.getUUID(), resident) != null) {
				throw new AlreadyRegisteredException(
					String.format("UUID '%s' was already registered for resident '%s'!", resident.getUUID().toString(), resident.getName())
				);
			}
		}
	}

	/**
	 * Register a resident into the internal structures.
	 * This will allow the resident to be fetched by name and UUID, as well as autocomplete the resident name.
	 * 
	 * If a resident's name or UUID change, the resident must be re-registered into the maps. 
	 * 
	 * This does not modify the resident internally, nor saves the resident in the database.
	 * 
	 * @param resident Resident to register.
	 * @throws AlreadyRegisteredException if another resident has been registered with the same name or UUID.
	 */
	public void registerResident(@NotNull Resident resident) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(resident, "Resident cannot be null!");

		if (residentNameMap.putIfAbsent(resident.getName().toLowerCase(Locale.ROOT), resident) != null) {
			throw new AlreadyRegisteredException(String.format("The resident with name '%s' is already registered!", resident.getName()));
		}

		residentsTrie.addKey(resident.getName());
		registerResidentUUID(resident);
	}

	/**
	 * Unregister a resident from the internal structures.
	 * This does not modify the resident internally, nor performs any database operations using the resident.
	 * 
	 * @param resident Resident to unregister
	 * @throws NotRegisteredException if the resident's name or UUID was not registered.
	 */
	public void unregisterResident(@NotNull Resident resident) throws NotRegisteredException {
		Preconditions.checkNotNull(resident, "Resident cannot be null!");

		if (residentNameMap.remove(resident.getName().toLowerCase(Locale.ROOT)) == null) {
			throw new NotRegisteredException(String.format("The resident with the name '%s' is not registered!", resident.getName()));
		}

		residentsTrie.removeKey(resident.getName());

		if (resident.getUUID() != null) {
			if (residentUUIDMap.remove(resident.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The resident with the UUID '%s' is not registered!", resident.getUUID().toString()));
			}
		}
	}

    @Unmodifiable
    public Collection<Resident> getResidents() {
		return Collections.unmodifiableCollection(residentNameMap.values());
	}

	/**
	 * @return number of residents that Towny has.
	 */
	public int getNumResidents() {
		return residentNameMap.size();
	}

	public Trie getResidentsTrie() {
		return residentsTrie;
	}
	
    public List<Resident> getJailedResidentMap() {
        return jailedResidents;
    }

	// =========== Town Methods ===========
	
	public boolean hasTown(@NotNull String townName) {
		Preconditions.checkNotNull(townName, "Town Name cannot be null!");
		
		// Fast-fail
		if (townName.isEmpty())
			return false;
		
    	String formattedName;
		try {
			formattedName = NameValidation.checkAndFilterTownNameOrThrow(townName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException e) {
			return false;
		}
		
		return townNameMap.containsKey(formattedName);
	}
	
	public boolean hasTown(@NotNull UUID townUUID) {
		Preconditions.checkNotNull(townUUID, "Town UUID cannot be null!");
    	
    	return townUUIDMap.containsKey(townUUID);
	}
	
	@Nullable
	public Town getTown(@NotNull String townName) {
    	Preconditions.checkNotNull(townName, "Town Name cannot be null!");
    	
    	// Fast-fail empty names
    	if (townName.isEmpty())
    		return null;
    	
		String formattedName;
		try {
			formattedName = NameValidation.checkAndFilterTownNameOrThrow(townName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException e) {
			return null;
		}
		
		return townNameMap.get(formattedName);
	}
	
	@Nullable
	public Town getTown(UUID townUUID) {
    	return townUUIDMap.get(townUUID);
	}
	
	@Unmodifiable
	public Collection<Town> getTowns() {
    	return Collections.unmodifiableCollection(townNameMap.values());
	}

    public Trie getTownsTrie() {
    	return townsTrie;
	}

	@ApiStatus.Internal
	public void newTownInternal(String name, UUID uuid) throws AlreadyRegisteredException, com.palmergames.bukkit.towny.exceptions.InvalidNameException {
    	newTown(name, uuid);
	}

	/**
	 * Create a new town from the string name.
	 *
	 * @param name Town name
	 * @throws AlreadyRegisteredException Town name is already in use.
	 * @throws InvalidNameException Town name is invalid.
	 */
	public void newTown(@NotNull String name) throws AlreadyRegisteredException, InvalidNameException {
		Preconditions.checkNotNull(name, "Name cannot be null!");
		
		newTown(name, UUID.randomUUID());
	}

	private void newTown(String name, UUID uuid) throws AlreadyRegisteredException, InvalidNameException {
		Preconditions.checkArgument(uuid != null, "uuid may not be null");
		String filteredName = NameValidation.checkAndFilterTownNameOrThrow(name);

		Town town = new Town(filteredName, uuid);
		registerTown(town);
	}
	
	// This is used internally since UUIDs are assigned after town objects are created.
	public void registerTownUUID(@NotNull Town town) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(town, "Town cannot be null!");
		
		if (town.getUUID() != null) {
			
			if (townUUIDMap.containsKey(town.getUUID())) {
				throw new AlreadyRegisteredException("UUID of town " + town.getName() + " was already registered!");
			}
			
			townUUIDMap.put(town.getUUID(), town);
		}
	}

	/**
	 * Used to register a town into the TownyUniverse internal maps.
	 * 
	 * This does not create a new town, or save a new town.
	 * 
	 * @param town Town to register.
	 * @throws AlreadyRegisteredException Town is already in the universe maps.
	 */
	public void registerTown(@NotNull Town town) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(town, "Town cannot be null!");
		
		if (townNameMap.putIfAbsent(town.getName().toLowerCase(Locale.ROOT), town) != null) {
			throw new AlreadyRegisteredException(String.format("The town with name '%s' is already registered!", town.getName()));
		}
		
		townsTrie.addKey(town.getName());
		registerTownUUID(town);
	}

	/**
	 * Used to unregister a town from the TownyUniverse internal maps.
	 * 
	 * This does not delete a town, nor perform any actions that affect the town internally.
	 * 
	 * @param town Town to unregister
	 * @throws NotRegisteredException Town is not registered in the universe maps.
	 */
	public void unregisterTown(@NotNull Town town) throws NotRegisteredException {
		Preconditions.checkNotNull(town, "Town cannot be null!");
		
		if (townNameMap.remove(town.getName().toLowerCase(Locale.ROOT)) == null) {
			throw new NotRegisteredException(String.format("The town with the name '%s' is not registered!", town.getName()));
		}
		
		townsTrie.removeKey(town.getName());
		
		if (town.getUUID() != null) {
			if (townUUIDMap.remove(town.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The town with the UUID '%s' is not registered!", town.getUUID().toString()));
			}
		}
	}

	// =========== Nation Methods ===========

	/**
	 * Check if the nation matching the given name exists.
	 *
	 * @param nationName Name of the nation to check.
	 * @return whether the nation matching the name exists.
	 */
	public boolean hasNation(@NotNull String nationName) {
		Preconditions.checkNotNull(nationName, "Nation Name cannot be null!");

		// Fast-fail if empty
		if (nationName.isEmpty())
			return false;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterNationNameOrThrow(nationName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException ignored) {
			return false;
		}

		return nationNameMap.containsKey(filteredName);
	}

	/**
	 * Check if the nation matching the given UUID exists.
	 * 
	 * @param nationUUID UUID of the nation to check.
	 * @return whether the nation matching the UUID exists.
	 */
	public boolean hasNation(@NotNull UUID nationUUID) {
		Preconditions.checkNotNull(nationUUID, "Nation UUID cannot be null!");
		
		return nationUUIDMap.containsKey(nationUUID);
	}

	/**
	 * Get the nation with the passed-in nation name if it exists.
	 * 
	 * @param nationName Name of the nation to fetch.
	 * @return the nation matching the name or {@code null} if it doesn't exist.
	 */
	@Nullable
	public Nation getNation(@NotNull String nationName) {
		Preconditions.checkNotNull(nationName, "Nation Name cannot be null!");
		
		// Fast-fail if empty
		if (nationName.isEmpty())
			return null;

		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterNationNameOrThrow(nationName).toLowerCase(Locale.ROOT);
		} catch (InvalidNameException ignored) {
			return null;
		}
		
		return nationNameMap.get(filteredName);
	}

	/**
	 * Get the nation with the given UUID if it exists.
	 * 
	 * @param nationUUID UUID of the nation to get.
	 * @return the nation with the given UUID or {@code null} if it doesn't exist.
	 */
	@Nullable
	public Nation getNation(@NotNull UUID nationUUID) {
		Preconditions.checkNotNull(nationUUID, "Nation UUID cannot be null!");
		
		return nationUUIDMap.get(nationUUID);
	}
	
	@Unmodifiable
	public Collection<Nation> getNations() {
		return Collections.unmodifiableCollection(nationNameMap.values());
	}
	
	public int getNumNations() {
		return nationNameMap.size();
	}

	// This is used internally since UUIDs are assigned after nation objects are created.
	public void registerNationUUID(@NotNull Nation nation) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(nation, "Nation cannot be null!");

		if (nation.getUUID() != null) {

			if (nationUUIDMap.containsKey(nation.getUUID())) {
				throw new AlreadyRegisteredException("UUID of nation " + nation.getName() + " was already registered!");
			}

			nationUUIDMap.put(nation.getUUID(), nation);
		}
	}

	/**
	 * Used to register a nation into the TownyUniverse internal maps.
	 *
	 * This does not create a new nation, or save a new nation.
	 *
	 * @param nation Nation to register.
	 * @throws AlreadyRegisteredException Nation is already in the universe maps.
	 */
	public void registerNation(@NotNull Nation nation) throws AlreadyRegisteredException {
		Preconditions.checkNotNull(nation, "Nation cannot be null!");

		if (nationNameMap.putIfAbsent(nation.getName().toLowerCase(Locale.ROOT), nation) != null) {
			throw new AlreadyRegisteredException(String.format("The nation with name '%s' is already registered!", nation.getName()));
		}

		nationsTrie.addKey(nation.getName());
		registerNationUUID(nation);
	}

	/**
	 * Used to unregister a nation from the TownyUniverse internal maps.
	 *
	 * This does not delete a nation, nor perform any actions that affect the nation internally.
	 *
	 * @param nation Nation to unregister
	 * @throws NotRegisteredException Nation is not registered in the universe maps.
	 */
	public void unregisterNation(@NotNull Nation nation) throws NotRegisteredException {
		Preconditions.checkNotNull(nation, "Nation cannot be null!");

		if (nationNameMap.remove(nation.getName().toLowerCase(Locale.ROOT)) == null) {
			throw new NotRegisteredException(String.format("The nation with the name '%s' is not registered!", nation.getName()));
		}

		nationsTrie.removeKey(nation.getName());

		if (nation.getUUID() != null) {
			if (nationUUIDMap.remove(nation.getUUID()) == null) {
				throw new NotRegisteredException(String.format("The nation with the UUID '%s' is not registered!", nation.getUUID().toString()));
			}
		}
	}

	public Trie getNationsTrie() {
		return nationsTrie;
	}
	
	// =========== World Methods ===========

	/**
	 * Causes a new TownyWorld object to be made in the Universe, from a Bukkit World.
	 */
	public void newWorld(@NotNull World world) {
		Preconditions.checkNotNull(world, "World cannot be null!");
		if (getWorldIDMap().containsKey(world.getUID()))
			return;
		TownyWorld townyWorld = new TownyWorld(world.getName(), world.getUID());
		registerTownyWorld(townyWorld);
		townyWorld.save();
	}

	public void registerTownyWorld(@NotNull TownyWorld world) {
		Preconditions.checkNotNull(world, "World cannot be null!");
		worldUUIDMap.putIfAbsent(world.getUUID(), world);
		worlds.putIfAbsent(world.getName().toLowerCase(Locale.ROOT), world);
	}

	public Map<UUID, TownyWorld> getWorldIDMap() {
		return worldUUIDMap;
	}

	@Nullable
	public TownyWorld getWorld(UUID uuid) {
		return worldUUIDMap.get(uuid);
	}
	
	public Set<UUID> getWorldUUIDs() {
		return worldUUIDMap.keySet();
	}

	public Map<String, TownyWorld> getWorldMap() {
        return worlds;
    }
	
	@Nullable
	public TownyWorld getWorld(String name) {
		return worlds.get(name.toLowerCase(Locale.ROOT));
	}
	
	public List<TownyWorld> getTownyWorlds() {
		return new ArrayList<>(worlds.values());
	}

	public boolean hasTownyWorld(String worldName) {
		Preconditions.checkNotNull(worldName, "World Name cannot be null!");

		// Fast-fail if empty
		if (worldName.isEmpty())
			return false;

		return worlds.containsKey(worldName.toLowerCase(Locale.ROOT));
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
        
        out.add(getTreeDepth(depth + 1) + "Nations (" + nationNameMap.size() + "):");
        for (Nation nation : nationNameMap.values()) {
            out.addAll(nation.getTreeString(depth + 2));
        }
        
        Collection<Town> townsWithoutNation = TownyAPI.getInstance().getTownsWithoutNation();
        out.add(getTreeDepth(depth + 1) + "Towns (" + townsWithoutNation.size() + "):");
        for (Town town : townsWithoutNation) {
            out.addAll(town.getTreeString(depth + 2));
        }
        
        Collection<Resident> residentsWithoutTown = TownyAPI.getInstance().getResidentsWithoutTown();
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

    /**
     * Used in loading only.
     * @param uuid UUID to assign to the PlotGroup.
     */
    public void newPlotGroupInternal(UUID uuid) {
    	PlotGroup group = new PlotGroup(uuid, null, null);
    	registerGroup(group);
    }
    
	
	public void registerGroup(PlotGroup group) {
		plotGroupUUIDMap.put(group.getUUID(), group);
	}

	public void unregisterGroup(UUID uuid) {
		PlotGroup group = plotGroupUUIDMap.get(uuid);
		if (group == null)
			return;
		group.getTown().removePlotGroup(group);
		plotGroupUUIDMap.remove(uuid);
	}

	/**
	 * Get all the plot object groups from all towns
	 * Returns a collection that does not reflect any group additions/removals
	 * 
	 * @return collection of PlotObjectGroup
	 */
	public Collection<PlotGroup> getGroups() {
    	return new ArrayList<>(plotGroupUUIDMap.values());
	}

	public Set<UUID> getPlotGroupUUIDs() {
		return plotGroupUUIDMap.keySet();
	}

	/**
	 * Gets the plot group from the town name and the plot group UUID 
	 * 
	 * @param groupID UUID of the plot group
	 * @return PlotGroup if found, null if none found.
	 */
	@Nullable
	public PlotGroup getGroup(UUID groupID) {
		return plotGroupUUIDMap.get(groupID);
	}

    /*
     * District Stuff.
     */

    /**
     * Used in loading only.
     * @param uuid UUID to assign to the District.
     */
    public void newDistrictInternal(UUID uuid) {
    	District district = new District(uuid, null, null);
    	registerDistrict(district);
    }
    
	
	public void registerDistrict(District district) {
		districtUUIDMap.put(district.getUUID(), district);
	}

	public void unregisterDistrict(UUID uuid) {
		District district = districtUUIDMap.get(uuid);
		if (district == null)
			return;
		district.getTown().removeDistrict(district);
		districtUUIDMap.remove(uuid);
	}

	/**
	 * Get all the districts from all towns
	 * Returns a collection that does not reflect any district additions/removals
	 * 
	 * @return collection of District
	 */
	public Collection<District> getDistricts() {
    	return new ArrayList<>(districtUUIDMap.values());
	}

	public Set<UUID> getDistrictUUIDs() {
		return districtUUIDMap.keySet();
	}

	/**
	 * Gets the district from the town name and the district UUID 
	 * 
	 * @param districtID UUID of the district
	 * @return District if found, null if none found.
	 */
	@Nullable
	public District getDistrict(UUID districtID) {
		return districtUUIDMap.get(districtID);
	}

	/*
	 * Metadata Stuff
	 */

	public void addCustomCustomDataField(CustomDataField<?> cdf) throws KeyAlreadyRegisteredException {
    	
    	if (this.getRegisteredMetadataMap().containsKey(cdf.getKey()))
    		throw new KeyAlreadyRegisteredException();
    	
    	this.getRegisteredMetadataMap().put(cdf.getKey(), cdf);
	}

	public Map<String, CustomDataField<?>> getRegisteredMetadataMap() {
		return getRegisteredMetadata();
	}
	
	public Map<String, CustomDataField<?>> getRegisteredMetadata() {
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
	
	@Nullable
	public TownBlock getTownBlockOrNull(WorldCoord worldCoord) {
		return townBlocks.get(worldCoord);
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
			if (townBlock.hasResident())
				townBlock.getResidentOrNull().removeTownBlock(townBlock);
			if (townBlock.hasTown())
				townBlock.getTownOrNull().removeTownBlock(townBlock);
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
	 * SpawnPoint Stuff
	 */

	@ApiStatus.Internal
	public Map<SpawnPointLocation, SpawnPoint> getSpawnPoints() {
		return spawnPoints;
	}
	
	@Nullable
	public SpawnPoint getSpawnPoint(Location loc) {
		return spawnPoints.get(SpawnPointLocation.parseSpawnPointLocation(loc));
	}
	
	public boolean hasSpawnPoint(Location loc) {
		return spawnPoints.containsKey(SpawnPointLocation.parseSpawnPointLocation(loc)); 
	}
	
	public void addSpawnPoint(SpawnPoint spawn) {
		spawnPoints.put(spawn.getSpawnPointLocation(), spawn);
	}

	public void removeSpawnPoint(Location loc) {
		removeSpawnPoint(SpawnPointLocation.parseSpawnPointLocation(loc));
	}
	
	public void removeSpawnPoint(SpawnPointLocation point) {
		spawnPoints.remove(point);
	}
	
    /*
     * Jail Stuff
     */

	public List<Jail> getJails() {
		return new ArrayList<>(getJailUUIDMap().values());
	}
	
    public Map<UUID, Jail> getJailUUIDMap() {
    	return jailUUIDMap;
    }
    
    @Nullable
    public Jail getJail(UUID uuid) {
    	if (hasJail(uuid))
    		return jailUUIDMap.get(uuid);
    	
    	return null;
    }
    
    public boolean hasJail(UUID uuid) {
    	return jailUUIDMap.containsKey(uuid);
    }
    
    public void registerJail(Jail jail) {
    	jailUUIDMap.put(jail.getUUID(), jail);
    }
    
    public void unregisterJail(Jail jail) {
    	jailUUIDMap.remove(jail.getUUID());
    }
    
    /**
     * Used in loading only.
     * 
     * @param uuid UUID of the given jail, taken from the Jail filename.
     */
    public void newJailInternal(String uuid) {
    	// Remaining fields are set later on in the loading process.
    	Jail jail = new Jail(UUID.fromString(uuid), null, null, new ArrayList<Position>());
    	registerJail(jail);
    }

	public Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
		return wildernessMapDataMap;
	}
	
	public Map<String,String> getReplacementNameMap() {
		return replacementNamesMap;
	}
}
