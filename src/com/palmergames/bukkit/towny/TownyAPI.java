package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.townblockstatus.NationZoneTownBlockStatusEvent;
import com.palmergames.bukkit.towny.exceptions.KeyAlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.MathUtil;

import io.papermc.lib.PaperLib;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Towny's class for external API Methods
 * For more dynamic/controlled changing of Towny's behavior, for example Database, War, Permissions
 * The {@link TownyUniverse} class should be used. It contains the map of all objects
 * aswell as serving as an internal API, that Towny uses.
 * @author Articdive
 */
public class TownyAPI {
    private static TownyAPI instance;
    private final Towny towny;
    private final TownyUniverse townyUniverse;
    
    private TownyAPI() {
        towny = Towny.getPlugin();
        townyUniverse = TownyUniverse.getInstance();
    }
    
    /**
     * Gets the town spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the town spawn.
     * @return {@link Location} of the town spawn or if it is not obtainable null.
     */
    @Nullable
    public Location getTownSpawnLocation(Player player) {
    	Resident resident = townyUniverse.getResident(player.getUniqueId());
    	
    	if (resident == null)
    		return null;
    	
        if (resident.hasTown())
			return resident.getTownOrNull().getSpawnOrNull();

		return null;
    }
    
    /**
     * Gets the nation spawn {@link Location} of a {@link Player}.
     *
     * @param player {@link Player} of which you want the nation spawn.
     * @return {@link Location} of the nation spawn or if it is not obtainable null.
     */
    @Nullable
    public Location getNationSpawnLocation(Player player) {
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		if (resident == null || !resident.hasNation())
			return null;

		return resident.getNationOrNull().getSpawnOrNull();
	}
 
    /**
     * Gets the resident's town if they have one.
     * 
     * @param resident Resident to get the town from.
     * @return The resident's Town or null if they have none.
     */
    @Nullable
    public Town getResidentTownOrNull(Resident resident) {
    	return resident.getTownOrNull();
    }
    
    /**
     * Gets the resident's nation if they have one.
     * 
     * @param resident Resident to get the nation from.
     * @return The resident's Nation or null if they have none.
     */
    @Nullable
    public Nation getResidentNationOrNull(Resident resident) {
    	if (resident.hasNation())
    		return resident.getTownOrNull().getNationOrNull();
    	return null;
    }
    
    /**
     * Gets the town's nation if they have one.
     * 
     * @param town Town to get the nation from.
     * @return The town's Nation or null if they have none.
     */
    @Nullable
    public Nation getTownNationOrNull(Town town) {
    	return town.getNationOrNull();
    }
    
    /**
     * Gets the nation from the given name.
     * @param name String name of the nation.
     * @return nation or null if it doesn't exist.
     */
    @Nullable
    public Nation getNation(String name) {
    	return TownyUniverse.getInstance().getNation(name);
    }
    
    /**
     * Gets the nation from the given UUID.
     * @param uuid UUID of the nation.
     * @return nation or null if it doesn't exist.
     */
    @Nullable
    public Nation getNation(UUID uuid) {
    	return TownyUniverse.getInstance().getNation(uuid);
    }

	/**
	 * Gets a List of Nations using an array of names.
	 * 
	 * @param names Array of Strings representing possible Nation names.
	 * @return List of Nations for which a name was matched.
	 */
	public List<Nation> getNations(String[] names) {
		return Arrays.stream(names).filter(Objects::nonNull).map(townyUniverse::getNation).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Gets a List of Nations using a List of UUIDs
	 * 
	 * @param uuids List of UUIDs representing possible Nations.
	 * @return List of Nations for which a UUID was matched.
	 */
	public List<Nation> getNations(List<UUID> uuids) {
		List<Nation> matches = new ArrayList<>();
		for (UUID uuid : uuids) {
			Nation n = townyUniverse.getNation(uuid);
			if (n != null) {
				matches.add(n);
			}
		}
		return matches;
	}

	public List<Nation> getNations(UUID[] uuids) {
		return getNations(Stream.of(uuids).collect(Collectors.toList()));
	}
	
    /**
     * Gets the resident from the given name.
     * @param name String name of the resident.
     * @return resident or null if it doesn't exist.
     */
    @Nullable
    public Resident getResident(String name) {
    	return TownyUniverse.getInstance().getResident(name);
    }
    
    /**
     * Gets the resident from the given UUID.
     * @param uuid UUID name of the resident.
     * @return resident or null if it doesn't exist.
     */
    @Nullable
    public Resident getResident(UUID uuid) {
    	return TownyUniverse.getInstance().getResident(uuid);
    }  
    
    /**
     * Gets the resident from the given Player.
     * 
     * @param player Player to get the resident from.
     * @return resident or null if it doesn't exist.
     */
    @Nullable
    public Resident getResident(Player player) {
    	return getResident(player.getUniqueId());
    }

	/**
	 * Get a List of Resident from an array of names.
	 * 
	 * @param names Array of Strings representing resident names.
	 * @return List of Residents which matched to a name.
	 */
	public List<Resident> getResidents(String[] names) {
		return Arrays.stream(names).filter(Objects::nonNull).map(townyUniverse::getResident).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Get a List of Resident from an array of UUIDs.
	 * 
	 * @param uuids Array of UUIDs representing resident uuids.
	 * @return List of Residents which matched to a UUID.
	 */
	public List<Resident> getResidents(UUID[] uuids) {
		return Arrays.stream(uuids).filter(Objects::nonNull).map(townyUniverse::getResident).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Gets a List of all of the Residents on the server.
	 * 
	 * @return a List of Residents
	 */
	public List<Resident> getResidents() {
		return new ArrayList<>(townyUniverse.getResidents());
	}

	/**
	 * Gets a List of all the Residents which don't belong to a town.
	 * 
	 * @return A List of all townless Residents.
	 */
	public List<Resident> getResidentsWithoutTown() {

		List<Resident> residentFilter = new ArrayList<>();
		for (Resident resident : getResidents())
			if (!resident.hasTown())
				residentFilter.add(resident);
		return residentFilter;
	}
    
    /**
     * Gets the town from the given name.
     * @param name String name of the town.
     * @return town or null if it doesn't exist.
     */
    @Nullable
    public Town getTown(String name) {
    	return TownyUniverse.getInstance().getTown(name);
    }

    /**
     * Gets the town from the given UUID.
     * @param uuid UUID name of the town.
     * @return town or null if it doesn't exist.
     */
    @Nullable
    public Town getTown(UUID uuid) {
    	return TownyUniverse.getInstance().getTown(uuid);
    }

	/**
	 * Gets a List of Towns using an array of names.
	 * 
	 * @param names Array of Strings representing possible Town names.
	 * @return List of Towns for which a name was matched.
	 */
	public List<Town> getTowns(String[] names) {
		return Arrays.stream(names).filter(Objects::nonNull).map(townyUniverse::getTown).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Gets a List of Towns using a List of UUIDs
	 * 
	 * @param uuids List of UUIDs representing possible Towns.
	 * @return List of Towns for which a UUID was matched.
	 */
	public List<Town> getTowns(List<UUID> uuids) {
		List<Town> matches = new ArrayList<>();
		for (UUID uuid : uuids) {
			Town t = townyUniverse.getTown(uuid);
			if (t != null) {
				matches.add(t);
			}
		}
		return matches;
	}
	
	public List<Town> getTowns(UUID[] uuids) {
		return getTowns(Stream.of(uuids).collect(Collectors.toList()));
	}
    
    /**
     * Find the the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the matching {@link Player}.
     * @return an online {@link Player} or if it's not obtainable.
     */
    public Player getPlayer(Resident resident) {
    	// NPCs are not players
    	if (resident.isNPC())
    		return null;
    	
    	Player player = null;
    	
    	if (resident.hasUUID())
    		player = BukkitTools.getPlayer(resident.getUUID());
    	
    	// Some servers use cross-platform proxies / offline mode where UUIDs may not be accurate. 
    	if (player == null)
    		player = BukkitTools.getPlayerExact(resident.getName());
    	
        return player;
    }
    
    /**
     * Find the {@link UUID} for the matching {@link Player} of the specified {@link Resident}.
     *
     * @param resident {@link Resident} of which you want the {@link UUID}.
     * @return an online {@link Player}'s {@link UUID} or null if it's not obtainable.
     */
    @Nullable
    public UUID getPlayerUUID(Resident resident) {
    	// NPCs are not players
    	if (resident.isNPC())
    		return null;
    	
    	// Use stored UUID if it exists
    	if (resident.hasUUID())
    		return resident.getUUID();
    	
    	Player player = BukkitTools.getPlayerExact(resident.getName());
    	
    	if (player != null)
    		return player.getUniqueId();
        
        return null;
    }
    
    /**
     * Gets all online {@link Player}s for a specific {@link ResidentList}.
     *
     * @param owner {@link ResidentList} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link ResidentList}.
     */
    public List<Player> getOnlinePlayers(ResidentList owner) {
		final List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		
		players.removeIf(player -> !owner.hasResident(player.getName()));
		return players;
	}
    
    /**
     * Gets all online {@link Player}s for a specific {@link Town}.
     * 
     * @param town {@link Town} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Town}.
     */
    public List<Player> getOnlinePlayersInTown(Town town){

    	return getOnlinePlayers(town);
    }

    /**
     * Gets all online {@link Player}s for a specific {@link Nation}.
     * 
     * @param nation {@link Nation} of which you want all the online {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Nation}.
     */
    public List<Player> getOnlinePlayersInNation(Nation nation){
    	return getOnlinePlayers(nation);
    }
    
    /** 
     * Gets all online {@link Player}s for a specific {@link Nation}s alliance.
     * 
     * @param nation {@link Nation} of which you want all the online allied {@link Player}s.
     * @return {@link List} of all online {@link Player}s in the specified {@link Nation}s allies.
     */
    public List<Player> getOnlinePlayersAlliance(Nation nation) {
		ArrayList<Player> players = new ArrayList<>(getOnlinePlayers(nation));
        if (!nation.getAllies().isEmpty()) {
			for (Nation nations : nation.getAllies()) {
				players.addAll(getOnlinePlayers(nations));
			}
        }
        return players;
    }
    
    /**
     * Check if the specified {@link Block} is in the wilderness.
     *
     * @param block {@link Block} to test for.
     * @return true if the {@link Block} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(Block block) {
        return isWilderness(block.getLocation());
    }
    
    /**
     * Check if the specified {@link Location} is in the wilderness.
     *
     * @param location {@link Location} to test widlerness for.
     * @return true if the {@link Location} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(Location location) {
        return isWilderness(WorldCoord.parseWorldCoord(location));
    }
    
    /**
     * Check if the specified {@link WorldCoord} is in the wilderness.
     *
     * @param worldCoord {@link WorldCoord} to test widlerness for.
     * @return true if the {@link WorldCoord} is in the wilderness, false otherwise.
     */
    public boolean isWilderness(WorldCoord worldCoord) {
        
		if (worldCoord.hasTownBlock() && worldCoord.getTownBlockOrNull().hasTown())
			return false;

		// Must be wilderness
		return true;
    }    
    
	/**
	 * Answers whether Towny considers PVP enabled at a location.
	 * 
	 * @param location Location to check for PVP.
	 * @return true if PVP is enabled or this isn't a world with Towny enabled.
	 */
	public boolean isPVP(Location location) {
		return !isTownyWorld(location.getWorld()) || !CombatUtil.preventPvP(getTownyWorld(location.getWorld()), getTownBlock(location));
	}

	/**
	 * Answers whether Towny has mobs enabled at a location.
	 * 
	 * @param location Location to check for mobs status.
	 * @return true if Towny would let mobs spawn/exist at the given Location, or if
	 *         Towny is disabled in the Location's world.
	 * @since 0.98.2.4.
	 */
	public boolean areMobsEnabled(Location location) {
		TownyWorld townyWorld = getTownyWorld(location.getWorld());
		return !townyWorld.isUsingTowny() || isWilderness(location)
			? townyWorld.hasWildernessMobs()
			: townyWorld.isForceTownMobs() || getTownBlock(location).getPermissions().mobs;
	}
	
    /**
     * Returns value of usingTowny for the given world.
     * 
     * @param world - the world to check
     * @return true or false
     */
    public boolean isTownyWorld(World world) {
    	TownyWorld townyWorld = getTownyWorld(world); 
    	return townyWorld != null && townyWorld.isUsingTowny();

    }
    
    /**
     * Returns {@link TownyWorld} unless it is null.
     * 
     * @param worldName - the name of the world to get.
     * @return TownyWorld or {@code null}.
     */
    @Nullable
    public TownyWorld getTownyWorld(String worldName) {
    	return townyUniverse.getWorld(worldName);
    }
    
    /**
     * Returns {@link TownyWorld} unless it is null.
     * 
     * @param worldUUID - the uuid of the world to get.
     * @return TownyWorld or {@code null}.
     */
    @Nullable
    public TownyWorld getTownyWorld(UUID worldUUID) {
    	return townyUniverse.getWorld(worldUUID);
    }
    
    /**
     * Returns {@link TownyWorld} unless it is null.
     * 
     * @param world - the world to get.
     * @return TownyWorld or {@code null}.
     */
    @Nullable
    public TownyWorld getTownyWorld(World world) {
    	return getTownyWorld(world.getUID());
    }
    
    /**
     * Get the {@link Town} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} for.
     * @return {@link Town} at this location, or {@code null} for none.
     */
    @Nullable
    public Town getTown(Location location) {
        WorldCoord worldCoord = WorldCoord.parseWorldCoord(location);
		return worldCoord.getTownOrNull();
    }
    
    /**
     * Get the {@link Town} of a {@link TownBlock} or null.
     * Should be used after testing TownBlock.hasTown().
     * 
     * @param townBlock {@link TownBlock} from which to get a {@link Town}.
     * @return {@link Town} or {@code null}. 
     */
    @Nullable
    public Town getTownOrNull(TownBlock townBlock) {
    	return townBlock.getTownOrNull();
    }
    
    /**
     * Get the {@link Resident} who owns the {@link TownBlock} or null.
     * Resident will be returned if the TownBlock is owned by a player.
     * Should be used after testing TownBlock.hasResident().
     * 
     * @param townBlock {@link TownBlock} from which to get a {@link Resident}.
     * @return {@link Resident} or {@code null}. 
     */
    @Nullable
    public Resident getResidentOrNull(TownBlock townBlock) {
    	return townBlock.getResidentOrNull();
    }
    
    /**
     * Get the name of a {@link Town} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} name for.
     * @return {@link String} containg the name of the {@link Town} at this location, or {@code null} for none.
     */
    @Nullable
    public String getTownName(Location location) {
    	Town town = getTown(location);
    	return town != null ? town.getName() : null;
    }
    
    /**
     * Get the {@link UUID} of a {@link Town} at the specified {@link Location}.
     *
     * @param location {@link Location} to get {@link Town} {@link UUID} for.
     * @return {@link UUID} of any {@link Town} at this {@link Location}, or {@code null} for none.
     */
    @Nullable
    public UUID getTownUUID(Location location) {
    	Town town = getTown(location);
    	return town != null ? town.getUUID() : null;
    }

	/**
	 * Get a List of all the Towns.
	 * 
	 * @return a List of all the Towns.
	 */
	public List<Town> getTowns() {
		return new ArrayList<>(townyUniverse.getTowns());
	}

	/**
	 * Get a List of all the Nations.
	 * 
	 * @return a List of all the Nations.
	 * @since 0.98.4.1.
	 */
	public List<Nation> getNations() {
		return new ArrayList<>(townyUniverse.getNations());
	}

	/**
	 * Get a List of all the Towns that aren't a part of a Nation.
	 * 
	 * @return a List of all the nationless Towns.
	 */
	public List<Town> getTownsWithoutNation() {
		List<Town> townFilter = new ArrayList<>();
		for (Town town : getTowns())
			if (!town.hasNation())
				townFilter.add(town);
		return townFilter;
	}
    
    /**
     * Get the {@link TownBlock} at a specific {@link Location}.
     *
     * @param location {@link Location} to get {@link TownBlock} of.
     * @return {@link TownBlock} at this {@link Location}, or {@code null} for none.
     */
    @Nullable
    public TownBlock getTownBlock(Location location) {
        WorldCoord worldCoord = WorldCoord.parseWorldCoord(location);
		return worldCoord.getTownBlockOrNull();
    }
    
    /**
     * Get the {@link TownBlock} in which a {@link Player} is located.
     *
     * @param player {@link Player} to get {@link TownBlock} of.
     * @return {@link TownBlock} at the location of this {@link Player}, or {@code null} when the player is in the wilderness.
     */
    @Nullable
    public TownBlock getTownBlock(@NotNull Player player) {
		return WorldCoord.parseWorldCoord(player.getLocation()).getTownBlockOrNull();
    }
    
    /** 
     * Get the {@link TownBlock} at a specific {@link WorldCoord}.
     * 
     * @param wc {@link WorldCoord} to get the {@link TownBlock} of (if it claimed by a town.)
     * @return {@link TownBlock} at this {@link WorldCoord}, or {@code null} if this isn't claimed.
     */
    @Nullable
    public TownBlock getTownBlock(WorldCoord wc) {
    	return wc.getTownBlockOrNull();
    }

    /**
     * Get a Collection of all of the TownBlocks.
     * 
     * @return Collection of TownBlocks.
     */
    public Collection<TownBlock> getTownBlocks() {
    	return townyUniverse.getTownBlocks().values();
    }
    
    /**
     * Gets Towny's saving Database
     *
     * @return the {@link TownyDataSource}
     */
    public TownyDataSource getDataSource() {
        return townyUniverse.getDataSource();
    }
    
    /**
     * Check which {@link Resident}s are online in a {@link ResidentList}
     *
     * @param owner {@link ResidentList} to check for online {@link Resident}s.
     * @return {@link List} of {@link Resident}s that are online.
     */
    public List<Resident> getOnlineResidents(ResidentList owner) {
        
        List<Resident> onlineResidents = new ArrayList<>();
        for (Player player : BukkitTools.getOnlinePlayers()) {
            if (player != null)
                for (Resident resident : owner.getResidents()) {
                    if (resident.getName().equalsIgnoreCase(player.getName()))
                        onlineResidents.add(resident);
                }
        }
        return onlineResidents;
    }
    
	public void requestTeleport(Player player, Location spawnLoc) {
		requestTeleport(player, spawnLoc, 0);
	}

    public void requestTeleport(Player player, Location spawnLoc, int cooldown) {
    	
    	Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
    	
    	if (resident != null) {
			TeleportWarmupTimerTask.requestTeleport(resident, spawnLoc, cooldown);
		}
    }
    
    public void abortTeleportRequest(Resident resident) {
        
        TeleportWarmupTimerTask.abortTeleportRequest(resident);
    }
    
    public void registerCustomDataField(CustomDataField<?> field) throws KeyAlreadyRegisteredException {
    	townyUniverse.addCustomCustomDataField(field);
	}

    /**
     * Method to figure out if a location is in a NationZone.
     * 
     * @param location - Location to test.
     * @return true if the location is in a NationZone.
     */
    public boolean isNationZone(Location location) {
    	if (!isWilderness(location))
    		return false;
    	TownBlockStatus status = hasNationZone(location);
    	if (status.equals(TownBlockStatus.NATION_ZONE))
    		return true;
    	
    	return false;
    }
    /**
     * Method to figure out if a location in the wilderness is normal wilderness of nation zone.
     * Recommended to use {@link TownyAPI#isWilderness(Location)} prior to using this, to confirm the location is not in a town.  
     * 
     * @param location - Location to test whether it is a nation zone or normal wilderness.
     * @return returns either UNCLAIMED_ZONE or NATION_ZONE
     */
    public TownBlockStatus hasNationZone(Location location) {
    	
    	return hasNationZone(WorldCoord.parseWorldCoord(location));
    }

    /**
     * Method to figure out if a worldcoord in the wilderness is normal wilderness of nation zone.
     * Recommended to use {@link TownyAPI#isWilderness(WorldCoord)} prior to using this, to confirm the location is not in a town.  
     * 
     * @param worldCoord - WorldCoord to test whether it is a nation zone or normal wilderness.
     * @return returns either UNCLAIMED_ZONE or NATION_ZONE
     */
    public TownBlockStatus hasNationZone(WorldCoord worldCoord) {
    	
		final TownBlock nearestTownblock = Optional.ofNullable(worldCoord.getTownyWorld()).map(world -> world.getClosestTownblockWithNationFromCoord(worldCoord)).orElse(null);
		
		if (nearestTownblock == null)
			return TownBlockStatus.UNCLAIMED_ZONE;
		
		Town nearestTown = nearestTownblock.getTownOrNull();
		
		// Safety validation, both these cases should never occur.
		if (nearestTown == null || !nearestTown.hasNation())
			return TownBlockStatus.UNCLAIMED_ZONE;
		
		// This nation zone system can be disabled during wartime.
		if (TownySettings.getNationZonesWarDisables() && nearestTown.getNationOrNull().hasActiveWar())
			return TownBlockStatus.UNCLAIMED_ZONE;

		// It is possible to only have nation zones surrounding nation capitals. If this is true, we treat this like a normal wilderness.
		if (!nearestTown.isCapital() && TownySettings.getNationZonesCapitalsOnly())
			return TownBlockStatus.UNCLAIMED_ZONE;
		
		// Even after checking for having a nation, and whether it might need to be a capital,
		// towns can disable their nation zone manually.
		if (!nearestTown.isNationZoneEnabled())
			return TownBlockStatus.UNCLAIMED_ZONE;
		
		int distance = (int) MathUtil.distance(worldCoord.getX(), nearestTownblock.getX(), worldCoord.getZ(), nearestTownblock.getZ());
		int nationZoneRadius = nearestTown.getNationZoneSize();

		if (distance <= nationZoneRadius) {
			if (BukkitTools.isEventCancelled(new NationZoneTownBlockStatusEvent(nearestTown)))
				return TownBlockStatus.UNCLAIMED_ZONE;
			
			return TownBlockStatus.NATION_ZONE;
		}
		
		return TownBlockStatus.UNCLAIMED_ZONE;
	}

	/**
	 * See TranslationLoader for an easy-to-use way of creating the Map needed for
	 * this method.
	 * 
	 * @param plugin       Plugin your plugin.
	 * @param translations Map&lt;String, Map&lt;String, String&gt;&gt; A hashmap
	 *                     keyed by the locale name, with a value of secondary
	 *                     hashmap of the locale's language string keys and their
	 *                     corresponding values.
	 */
	public void addTranslations(Plugin plugin, Map<String, Map<String, String>> translations) {
		Translation.addTranslations(translations);
		Towny.getPlugin().getLogger().info("Loaded additional language files for plugin: " + plugin.getName());
	}
    
    public static TownyAPI getInstance() {
        if (instance == null) {
            instance = new TownyAPI();
        }
        return instance;
    }
    
	/**
	 * Returns a List&lt;String&gt; containing strings of resident, town, and/or
	 * nation names that match with arg. Can check for multiple types, for example
	 * "rt" would check for residents and towns but not nations or worlds. Useful
	 * for tab completion systems calling for Towny Objects.
	 *
	 * @param arg  the string to match with the chosen type
	 * @param type the type of Towny object to check for, can be r(esident), t(own),
	 *             n(ation), w(orld), or any combination of those to check.
	 * @return Matches for the arg with the chosen type
	 */
	public static List<String> getTownyObjectStartingWith(String arg, String type) {
		return BaseCommand.getTownyStartingWith(arg, type);
	}

	/**
	 * Checks if arg starts with filters, if not returns matches from
	 * {@link #getTownyObjectStartingWith(String, String)}. Add a "+" to the type to
	 * return both cases. Useful for tab completion systems.
	 *
	 * @param filters the strings to filter arg with
	 * @param arg     the string to check with filters and possibly match with Towny
	 *                objects if no filters are found
	 * @param type    the type of check to use, see
	 *                {@link #getTownyObjectStartingWith(String, String)} for possible
	 *                types. Add "+" to check for both filters and
	 *                {@link #getTownyObjectStartingWith(String, String)}
	 * @return Matches for the arg filtered by filters or checked with type
	 */
	public static List<String> filterByStartOrGetTownyObjectStartingWith(List<String> filters, String arg, String type) {
		return BaseCommand.filterByStartOrGetTownyStartingWith(filters, arg, type);
	}

	/**
	 * Returns the names a player's town's residents that start with a string.
	 * Useful for tab completion systems.
	 *
	 * @param player the player to get the town's residents of
	 * @param str the string to check if the town's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getTownResidentNamesOfPlayerStartingWith(Player player, String str){
		return BaseCommand.getTownResidentNamesOfPlayerStartingWith(player, str);
	}

	/**
	 * Returns the names a town's residents that start with a string.
	 * Useful for tab completion systems.
	 *
	 * @param townName the town to get the residents of
	 * @param str the string to check if the town's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getResidentsOfTownStartingWith(String townName, String str) {
		return BaseCommand.getResidentsOfTownStartingWith(townName, str);
	}
	
	/**
	 * Returns a list of residents which are online and have no town.
	 * Useful for tab completion systems.
	 * 
	 * @param str the string to check if the resident's name starts with.
	 * @return the residents name or an empty list.
	 */
	public static List<String> getResidentsWithoutTownStartingWith(String str) {
		return BaseCommand.getResidentsWithoutTownStartingWith(str);
	}

	
	/**
	 * @deprecated as of 0.98.3.7, use {@link TownyAPI#getNations(UUID[])} instead.
	 * @param uuids List of UUIDs representing possible Nations.
	 * @return {@link TownyAPI#getNations(UUID[])}
	 */
	@Deprecated
	public List<Nation> getNation(List<UUID> uuids) {
		return getNations(uuids);
	}

	/**
     * @deprecated since 0.97.3.0 use {@link Town#hasActiveWar()} or {@link Nation#hasActiveWar()} instead.
     * @return false.
     */
    @Deprecated
    public boolean isWarTime() {
    	return false;
    }

    /**
     * Teleports the Player to the specified jail {@link Location}.
     *
     * @param player   {@link Player} to be teleported to jail.
     * @param location {@link Location} of the jail to be teleported to.
	 * @deprecated Since 0.97.3.0 use {@link com.palmergames.bukkit.towny.utils.SpawnUtil#jailTeleport(Resident)} or {@link com.palmergames.bukkit.towny.utils.SpawnUtil#jailAwayTeleport(Resident)} instead.
     */
	@Deprecated
    public void jailTeleport(final Player player, final Location location) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(towny, () -> PaperLib.teleportAsync(player, location, PlayerTeleportEvent.TeleportCause.PLUGIN),
			(long) TownySettings.getTeleportWarmupTime() * 20);
	}

	/**
	 * Get a list of active {@link Resident}s.
	 *
	 * @return {@link List} of active {@link Resident}s.
	 * @deprecated This is deprecated as of 0.97.2.6, and will be removed in a future release.
	 */
	@Deprecated
	public List<Resident> getActiveResidents() {
		return new ArrayList<>(townyUniverse.getResidents());
	}

	/**
	 * Check if the specified {@link Resident} is an active Resident.
	 *
	 * @param resident {@link Resident} to test for activity.
	 * @return true if the player is active, false otherwise.
	 * @deprecated This is deprecated as of 0.97.2.6, and will be removed in a future release.
	 */
	@Deprecated
	public boolean isActiveResident(Resident resident) {
		return resident.isOnline();
	}
	
	@Nullable
	public Town getTown(@NotNull Player player) {
		Resident resident = getResident(player);
		
		return resident == null ? null : resident.getTownOrNull();
	}
	
	@Nullable
	public Nation getNation(@NotNull Player player) {
		Resident resident = getResident(player);
		
		return resident == null ? null : resident.getNationOrNull();
	}
}
