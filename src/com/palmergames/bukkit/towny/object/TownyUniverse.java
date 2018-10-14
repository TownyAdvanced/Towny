package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.FileMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.naming.InvalidNameException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.palmergames.bukkit.towny.object.TownyObservableType.PLAYER_LOGOUT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TELEPORT_REQUEST;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_CLEARED;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_END;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_SET;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_START;

public class TownyUniverse extends TownyObject {

	public TownyUniverse(Towny plugin) {

		// Initialize the object.
		setName("");
		TownyUniverse.plugin = plugin;
	}
	
	private static Towny plugin;

	protected Hashtable<String, Resident> residents = new Hashtable<>();
	protected Hashtable<String, Town> towns = new Hashtable<>();
	protected Hashtable<String, Nation> nations = new Hashtable<>();
	protected Hashtable<String, TownyWorld> worlds = new Hashtable<>();

	private static TownyDataSource dataSource;
	private static TownyPermissionSource permissionSource;
	
	private static War warEvent;
	private String rootFolder;

	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {

		if (!player.isOnline())
			return;

		// Test and kick any players with invalid names.
		if ((player.getName().trim() == null) || (player.getName().contains(" "))) {
			player.kickPlayer("Invalid name!");
			return;
		}

		// Perform login code in it's own thread to update Towny data.
		//new OnPlayerLogin(plugin, player).start();
		if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(plugin,player),0L) == -1)
			TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");

	}

	public void onLogout(Player player) {

		try {
			Resident resident = getDataSource().getResident(player.getName());
			resident.setLastOnline(System.currentTimeMillis());
			getDataSource().saveResident(resident);
		} catch (NotRegisteredException e) {
		}
		setChangedNotify(PLAYER_LOGOUT);
	}

	public Location getTownSpawnLocation(Player player) throws TownyException {

		try {
			Resident resident = getDataSource().getResident(player.getName());
			Town town = resident.getTown();
			return town.getSpawn();
		} catch (TownyException x) {
			throw new TownyException("Unable to get spawn location");
		}
	}

	public Location getNationSpawnLocation(Player player) throws TownyException {

		try {
			Resident resident = getDataSource().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			return nation.getNationSpawn();
		} catch (TownyException x) {
			throw new TownyException("Unable to get nation spawn location");
		}
	}

	/**
	 * Find a matching online player for this resident.
	 * 
	 * @param resident
	 * @return an online player object
	 * @throws TownyException
	 */
	public static Player getPlayer(Resident resident) throws TownyException {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (player.getName().equals(resident.getName()))
					return player;
		throw new TownyException(String.format("%s is not online", resident.getName()));
	}
	
	/**
	 * Find a matching online player for this resident.
	 * 
	 * @param resident
	 * @return an online player UUID
	 * @throws TownyException
	 */
	public static UUID getPlayerUUID(Resident resident) throws TownyException {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (player.getName().equals(resident.getName()))
					return player.getUniqueId();
		throw new TownyException(String.format("%s is not online", resident.getName()));
	}

	/**
	 * Get a list of all online players matching the residents supplied.
	 * 
	 * @param residents
	 * @return list of all matching players
	 */
	public static List<Player> getOnlinePlayers(ResidentList residents) {

		ArrayList<Player> players = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (residents.hasResident(player.getName()))
					players.add(player);
		return players;
	}

	/**
	 * Get a list of all online players for a specific town
	 * 
	 * @param town
	 * @return list of all matching players
	 */
	public static List<Player> getOnlinePlayers(Town town) {

		ArrayList<Player> players = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (town.hasResident(player.getName()))
					players.add(player);
		return players;
	}

	/**
	 * Get a list of all online players for a specific nation
	 * 
	 * @param nation
	 * @return list of all matching players
	 */
	public static List<Player> getOnlinePlayers(Nation nation) {

		ArrayList<Player> players = new ArrayList<>();
		for (Town town : nation.getTowns())
			players.addAll(getOnlinePlayers(town));
		return players;
	}

	/**
	 * isWilderness
	 * 
	 * returns true if this block is in the wilderness
	 * 
	 * @param block
	 * @return true is in wilderness
	 */
	public static boolean isWilderness(Block block) {

		WorldCoord worldCoord;

		try {
			worldCoord = new WorldCoord(getDataSource().getWorld(block.getWorld().getName()).getName(), Coord.parseCoord(block));
		} catch (NotRegisteredException e) {
			// No record so must be Wilderness
			return true;
		}

		try {
			return worldCoord.getTownBlock().getTown() == null;
		} catch (NotRegisteredException e) {
			// Must be wilderness
			return true;
		}
	}

	/**
	 * getTownName
	 * 
	 * returns the name of the Town this location lies within if no town is
	 * registered it returns null
	 * 
	 * @param loc
	 * @return name of any town at this location, or null for none.
	 */
	public static String getTownName(Location loc) {

		try {
			WorldCoord worldCoord = new WorldCoord(getDataSource().getWorld(loc.getWorld().getName()).getName(), Coord.parseCoord(loc));
			return worldCoord.getTownBlock().getTown().getName();
		} catch (NotRegisteredException e) {
			// No data so return null
			return null;
		}

	}
	/**
	 * getTownUUID
	 *
	 * returns the uuid of the Town this location lies within if no town is
	 * registered it returns null
	 *
	 * @param loc
	 * @return name of any town at this location, or null for none.
	 */
	public static UUID getTownUUID(Location loc) {

		try {
			WorldCoord worldCoord = new WorldCoord(getDataSource().getWorld(loc.getWorld().getName()).getName(), Coord.parseCoord(loc));
			return worldCoord.getTownBlock().getTown().getUuid();
		} catch (NotRegisteredException e) {
			// No data so return null
			return null;
		}

	}

	/**
	 * getTownBlock
	 * 
	 * returns TownBlock this location lies within if no block is registered it
	 * returns null
	 * 
	 * @param loc
	 * @return TownBlock at this location, or null for none.
	 */
	public static TownBlock getTownBlock(Location loc) {

		try {
			WorldCoord worldCoord = new WorldCoord(getDataSource().getWorld(loc.getWorld().getName()).getName(), Coord.parseCoord(loc));
			return worldCoord.getTownBlock();
		} catch (NotRegisteredException e) {
			// No data so return null
			return null;
		}
	}

	public List<Resident> getActiveResidents() {

		List<Resident> activeResidents = new ArrayList<>();
		for (Resident resident : getDataSource().getResidents())
			if (isActiveResident(resident))
				activeResidents.add(resident);
		return activeResidents;
	}

	public boolean isActiveResident(Resident resident) {

		return ((System.currentTimeMillis() - resident.getLastOnline() < (20 * TownySettings.getInactiveAfter())) || (BukkitTools.isOnline(resident.getName())));
	}

	public boolean loadSettings() {

		try {
			TownySettings.loadConfig(getRootFolder() + FileMgmt.fileSeparator() + "settings" + FileMgmt.fileSeparator() + "config.yml", plugin.getVersion());
			TownySettings.loadLanguage(getRootFolder() + FileMgmt.fileSeparator() + "settings", "english.yml");
			TownyPerms.loadPerms(getRootFolder() + FileMgmt.fileSeparator() + "settings", "townyperms.yml");

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		String save = TownySettings.getSaveDatabase(), load = TownySettings.getLoadDatabase();

		// Setup any defaults before we load the database.
		Coord.setCellSize(TownySettings.getTownBlockSize());

		System.out.println("[Towny] Database: [Load] " + load + " [Save] " + save);

		worlds.clear();
		nations.clear();
		towns.clear();
		residents.clear();

		if (!loadDatabase(load)) {
			System.out.println("[Towny] Error: Failed to load!");
			return false;
		}

		try {
			getDataSource().cleanupBackups();
			// Set the new class for saving.
			setDataSource(save);
			getDataSource().initialize(plugin, this);
			FileMgmt.checkFolders(new String[] {getRootFolder() + FileMgmt.fileSeparator() + "logs" }); // Setup the logs folder here as the logger will not yet be enabled.
			try {
				getDataSource().backup();
				
				if (load.equalsIgnoreCase("flatfile") || save.equalsIgnoreCase("flatfile"))
					getDataSource().deleteUnusedResidentFiles();
				
			} catch (IOException e) {
				System.out.println("[Towny] Error: Could not create backup.");
				e.printStackTrace();
				return false;
			}

			if (load.equalsIgnoreCase(save)) {
				// Update all Worlds data files
				getDataSource().saveAllWorlds();
			} else {
				//Formats are different so save ALL data.
				getDataSource().saveAll();
			}

			//if (TownySettings.isSavingOnLoad())
			//      townyUniverse.getDataSource().saveAll();
		} catch (UnsupportedOperationException e) {
			System.out.println("[Towny] Error: Unsupported save format!");
			return false;
		}

		File f = new File(plugin.getDataFolder(), "outpostschecked.txt");
		if (!(f.exists())) {
			for (Town town : getDataSource().getTowns()) {
				TownySQLSource.validateTownOutposts(town);
			}
			plugin.saveResource("outpostschecked.txt", false);
		}
		return true;
	}

	public boolean loadDatabase(String databaseType) {

		try {
			setDataSource(databaseType);
		} catch (UnsupportedOperationException e) {
			return false;
		}
		getDataSource().initialize(plugin, this);

		return getDataSource().loadAll();
	}
	
	
	public String getRootFolder() {

		if (plugin != null)
			return plugin.getDataFolder().getPath();
		else
			return rootFolder;
	}

	public void setDataSource(String databaseType) throws UnsupportedOperationException {

		if (databaseType.equalsIgnoreCase("flatfile"))
			setDataSource(new TownyFlatFileSource());
		// HMOD has been moved to legacy
		else if ((databaseType.equalsIgnoreCase("mysql")) || (databaseType.equalsIgnoreCase("sqlite")) || (databaseType.equalsIgnoreCase("h2")))
			setDataSource(new TownySQLSource(databaseType));
		else
			throw new UnsupportedOperationException();
	}

	public void setDataSource(TownyDataSource dataSource) {

		TownyUniverse.dataSource = dataSource;
	}

	public static TownyDataSource getDataSource() {

		return dataSource;
	}

	public void setPermissionSource(TownyPermissionSource permissionSource) {

		TownyUniverse.permissionSource = permissionSource;
	}

	public static TownyPermissionSource getPermissionSource() {

		return permissionSource;
	}

	/**
	 * @return Hashtable of residents
	 */
	public Hashtable<String, Resident> getResidentMap() {

		return this.residents;
	}

	/**
	 * @return HashTable of Towns
	 */
	public Hashtable<String, Town> getTownsMap() {

		return this.towns;
	}

	/**
	 * @return Hashtable of all nations
	 */
	public Hashtable<String, Nation> getNationsMap() {

		return this.nations;
	}

	/**
	 * @return Map of TownyWorlds
	 */
	public Hashtable<String, TownyWorld> getWorldMap() {

		return this.worlds;
	}

	public static boolean isWarTime() {

		return warEvent != null ? warEvent.isWarTime() : false;
	}

	public void startWarEvent() {

		warEvent = new War(plugin, TownySettings.getWarTimeWarningDelay());
		setChangedNotify(WAR_START);
	}

	public void endWarEvent() {

		if (isWarTime())
			warEvent.toggleEnd();
		// Automatically makes warEvent null
		setChangedNotify(WAR_END);
	}

	public void clearWarEvent() {

		getWarEvent().cancelTasks(BukkitTools.getScheduler());
		setWarEvent(null);
		setChangedNotify(WAR_CLEARED);
	}

	//TODO: throw error if null
	public War getWarEvent() {

		return warEvent;
	}

	public void setWarEvent(War event) {

		warEvent = event;
		setChangedNotify(WAR_SET);
	}

	public void sendUniverseTree(CommandSender sender) {

		for (String line : getTreeString(0))
			sender.sendMessage(line);
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "Universe (" + getName() + ")");
		if (plugin != null) {
			out.add(getTreeDepth(depth + 1) + "Server (" + BukkitTools.getServer().getName() + ")");
			out.add(getTreeDepth(depth + 2) + "Version: " + BukkitTools.getServer().getVersion());
			//out.add(getTreeDepth(depth + 2) + "Players: " + BukkitTools.getOnlinePlayers().length + "/" + BukkitTools.getServer().getMaxPlayers());
			out.add(getTreeDepth(depth + 2) + "Worlds (" + BukkitTools.getWorlds().size() + "): " + Arrays.toString(BukkitTools.getWorlds().toArray(new World[0])));
		}
		out.add(getTreeDepth(depth + 1) + "Worlds (" + getDataSource().getWorlds().size() + "):");
		for (TownyWorld world : getDataSource().getWorlds())
			out.addAll(world.getTreeString(depth + 2));

		out.add(getTreeDepth(depth + 1) + "Nations (" + getDataSource().getNations().size() + "):");
		for (Nation nation : getDataSource().getNations())
			out.addAll(nation.getTreeString(depth + 2));

		Collection<Town> townsWithoutNation = getDataSource().getTownsWithoutNation();
		out.add(getTreeDepth(depth + 1) + "Towns (" + townsWithoutNation.size() + "):");
		for (Town town : townsWithoutNation)
			out.addAll(town.getTreeString(depth + 2));

		Collection<Resident> residentsWithoutTown = getDataSource().getResidentsWithoutTown();
		out.add(getTreeDepth(depth + 1) + "Residents (" + residentsWithoutTown.size() + "):");
		for (Resident resident : residentsWithoutTown)
			out.addAll(resident.getTreeString(depth + 2));
		return out;
	}

	public static List<Resident> getValidatedResidents(Object sender, String[] names) {

		List<Resident> invited = new ArrayList<>();
		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				String line = "Multiple players selected";
				for (Player p : matches)
					line += ", " + p.getName();
				TownyMessaging.sendErrorMsg(sender, line);
			} else if (matches.size() == 1) {
				// Match found online
				try {
					Resident target = getDataSource().getResident(matches.get(0).getName());
					invited.add(target);
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(sender, x.getMessage());
				}
			} else {
				// No online matches so test for offline.
				Resident target;
				try {
					target = getDataSource().getResident(name);
					invited.add(target);
				} catch (NotRegisteredException x) {
					TownyMessaging.sendErrorMsg(sender, x.getMessage());
				}
			}
		}
		return invited;
	}

	public static List<Resident> getOnlineResidents(Player player, String[] names) {

		List<Resident> invited = new ArrayList<>();
		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				String line = "Multiple players selected";
				for (Player p : matches)
					line += ", " + p.getName();
				TownyMessaging.sendErrorMsg(player, line);
			} else if (matches.size() == 1)
				try {
					Resident target = getDataSource().getResident(matches.get(0).getName());
					invited.add(target);
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}
		}
		return invited;
	}

	public static List<Resident> getOnlineResidents(ResidentList residentList) {

		List<Resident> onlineResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null)
				for (Resident resident : residentList.getResidents()) {
					if (resident.getName().equalsIgnoreCase(player.getName()))
						onlineResidents.add(resident);
				}
		}

		return onlineResidents;
	}
	
	public static List<Resident> getOnlineResidentsViewable(Player viewer, ResidentList residentList) {

		List<Resident> onlineResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				/*
				 * Loop town/nation resident list
				 */
				for (Resident resident : residentList.getResidents()) {
					if (resident.getName().equalsIgnoreCase(player.getName()))
						if ((viewer == null) || (viewer.canSee(BukkitTools.getPlayerExact(resident.getName())))) {
							onlineResidents.add(resident);
						}
				}
			}
		}

		return onlineResidents;
	}

	public void requestTeleport(Player player, Location spawnLoc, double cost) {

		try {
			TeleportWarmupTimerTask.requestTeleport(getDataSource().getResident(player.getName().toLowerCase()), spawnLoc);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

		setChangedNotify(TELEPORT_REQUEST);
	}

	public void abortTeleportRequest(Resident resident) {

		TeleportWarmupTimerTask.abortTeleportRequest(resident);
	}

	public static void jailTeleport(final Player player, final Location loc) {
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.teleport(loc, TeleportCause.PLUGIN),
				TownySettings.getTeleportWarmupTime() * 20);
	}
	
	public void addWarZone(WorldCoord worldCoord) {

		try {
			worldCoord.getTownyWorld().addWarZone(worldCoord);
		} catch (NotRegisteredException e) {
			// Not a registered world
		}
		plugin.updateCache(worldCoord);
	}

	public void removeWarZone(WorldCoord worldCoord) {

		try {
			worldCoord.getTownyWorld().removeWarZone(worldCoord);
		} catch (NotRegisteredException e) {
			// Not a registered world
		}
		plugin.updateCache(worldCoord);
	}

	

	public void setChangedNotify(TownyObservableType type) {

		setChanged();
		notifyObservers(type);
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param possibleAllies
	 * @return true if all are allies
	 */
	@Deprecated
	public boolean areAllAllies(List<Nation> possibleAllies) {

		if (possibleAllies.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleAllies.size() - 1; i++)
				if (!possibleAllies.get(i).hasAlly(possibleAllies.get(i + 1)))
					return false;
			return true;
		}
	}
	
	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param player
	 * @param worldCoord
	 * @return true if this is an enemy townblock
	 */
	@Deprecated
	public boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {

		try {
			return CombatUtil.isEnemy(getDataSource().getResident(player.getName()).getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException e) {
			return false;
		}
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param a
	 * @param b
	 * @return true if these residents are allies
	 */
	@Deprecated
	public boolean isAlly(String a, String b) {

		try {
			Resident residentA = getDataSource().getResident(a);
			Resident residentB = getDataSource().getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return true;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return true;
			if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param a
	 * @param b
	 * @return true if these towns are allies
	 */
	@Deprecated
	public boolean isAlly(Town a, Town b) {

		try {
			if (a == b)
				return true;
			if (a.getNation() == b.getNation())
				return true;
			if (a.getNation().hasAlly(b.getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param a
	 * @param b
	 * @return true if resident a can attack resident b
	 */
	@Deprecated
	public boolean canAttackEnemy(String a, String b) {

		try {
			Resident residentA = getDataSource().getResident(a);
			Resident residentB = getDataSource().getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return false;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return false;
			Nation nationA = residentA.getTown().getNation();
			Nation nationB = residentB.getTown().getNation();
			if (nationA.isNeutral() || nationB.isNeutral())
				return false;
			if (nationA.hasEnemy(nationB))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param a
	 * @param b
	 * @return true if resident b is an enemy of resident a
	 */
	@Deprecated
	public boolean isEnemy(String a, String b) {

		try {
			Resident residentA = getDataSource().getResident(a);
			Resident residentB = getDataSource().getResident(b);
			if (residentA.getTown() == residentB.getTown())
				return false;
			if (residentA.getTown().getNation() == residentB.getTown().getNation())
				return false;
			if (residentA.getTown().getNation().hasEnemy(residentB.getTown().getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Deprecated - Use CombatUtil
	 * 
	 * @param a
	 * @param b
	 * @return true if town b is an enemy of town a
	 */
	@Deprecated
	public boolean isEnemy(Town a, Town b) {

		try {
			if (a == b)
				return false;
			if (a.getNation() == b.getNation())
				return false;
			if (a.getNation().hasEnemy(b.getNation()))
				return true;
		} catch (NotRegisteredException e) {
			return false;
		}
		return false;
	}

	/**
	 * Deprecated - Use TownyUniverse.getDataSource().newResident(name)
	 * 
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().newResident(name);
	}

	/**
	 * Deprecated - Use TownyUniverse.getDataSource().newTown(name)
	 * 
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().newTown(name);
	}

	/**
	 * Deprecated - Use TownyUniverse.getDataSource().getTownWorld(name)
	 * 
	 * @param townName
	 * @return TownyWorld
	 */
	@Deprecated
	public static TownyWorld getTownWorld(String townName) {

		return getDataSource().getTownWorld(townName);
	}

	/**
	 * Deprecated - Use TownyUniverse.getDataSource().newNation(name)
	 * 
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().newNation(name);
	}

	/**
	 * Deprecated - Use TownyUniverse.getDataSource().newWorld(name)
	 * 
	 * @param name
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().newWorld(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().hasResident(name)
	 * 
	 * @param name
	 * @return true if resident exists
	 */
	@Deprecated
	public boolean hasResident(String name) {

		return getDataSource().hasResident(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().hasTown(name)
	 * 
	 * @param name
	 * @return true if town exists
	 */
	@Deprecated
	public boolean hasTown(String name) {

		return getDataSource().hasTown(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().hasNation(name)
	 * 
	 * @param name
	 * @return true if nation exists
	 */
	@Deprecated
	public boolean hasNation(String name) {

		return getDataSource().hasNation(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().renameTown()
	 * 
	 * @param town
	 * @param newName
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().renameTown(town, newName);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().renameNation()
	 * 
	 * @param nation
	 * @param newName
	 * @throws AlreadyRegisteredException
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {

		getDataSource().renameNation(nation, newName);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResident(name)
	 * 
	 * @param name
	 * @return Resident with this name
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public Resident getResident(String name) throws NotRegisteredException {

		return getDataSource().getResident(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResidents()
	 * 
	 * @return List of all residents
	 */
	@Deprecated
	public List<Resident> getResidents() {

		return getDataSource().getResidents();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResidentKeys()
	 * 
	 * @return Set of all residents
	 */
	@Deprecated
	public Set<String> getResidentKeys() {

		return getDataSource().getResidentKeys();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getTownsKeys()
	 * 
	 * @return Set of Town names
	 */
	@Deprecated
	public Set<String> getTownsKeys() {

		return getDataSource().getTownsKeys();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getNationsKeys()
	 * 
	 * @return Set of all nation names
	 */
	@Deprecated
	public Set<String> getNationsKeys() {

		return getDataSource().getNationsKeys();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getTowns()
	 * 
	 * @return List of all towns
	 */
	@Deprecated
	public List<Town> getTowns() {

		return getDataSource().getTowns();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getNations()
	 * 
	 * @return List of all nations
	 */
	@Deprecated
	public List<Nation> getNations() {

		return getDataSource().getNations();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getWorlds()
	 * 
	 * @return List of TownyWorlds
	 */
	@Deprecated
	public List<TownyWorld> getWorlds() {

		return getDataSource().getWorlds();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getTownsWithoutNation()
	 * 
	 * @return List of towns without a nation
	 */
	@Deprecated
	public List<Town> getTownsWithoutNation() {

		return getDataSource().getTownsWithoutNation();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResidentsWithoutTown()
	 * 
	 * @return List of residents with no town.
	 */
	@Deprecated
	public List<Resident> getResidentsWithoutTown() {

		return getDataSource().getResidentsWithoutTown();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResidents()
	 * 
	 * @param names
	 * @return List of residents matching these names
	 */
	@Deprecated
	public List<Resident> getResidents(String[] names) {

		return getDataSource().getResidents(names);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getTowns()
	 * 
	 * @param names
	 * @return List of names matching towns
	 */
	@Deprecated
	public List<Town> getTowns(String[] names) {

		return getDataSource().getTowns(names);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getNations()
	 * 
	 * @param names
	 * @return List of nations matching these names
	 */
	@Deprecated
	public List<Nation> getNations(String[] names) {

		return getDataSource().getNations(names);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getTown()
	 * 
	 * @param name
	 * @return Town with this name
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public Town getTown(String name) throws NotRegisteredException {

		return getDataSource().getTown(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getNation()
	 * 
	 * @param name
	 * @return Nation matching this name
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public Nation getNation(String name) throws NotRegisteredException {

		return getDataSource().getNation(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getWorld()
	 * 
	 * @param name
	 * @return TownyWorld
	 * @throws NotRegisteredException
	 */
	@Deprecated
	public static TownyWorld getWorld(String name) throws NotRegisteredException {

		return getDataSource().getWorld(name);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeWorld()
	 * 
	 * @param world
	 * @throws UnsupportedOperationException
	 */
	@Deprecated
	public void removeWorld(TownyWorld world) throws UnsupportedOperationException {

		getDataSource().removeWorld(world);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeNation()
	 * 
	 * @param nation
	 */
	@Deprecated
	public void removeNation(Nation nation) {

		getDataSource().removeNation(nation);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeTown()
	 * 
	 * @param town
	 */
	@Deprecated
	public void removeTown(Town town) {

		getDataSource().removeTown(town);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeResident()
	 * 
	 * @param resident
	 */
	@Deprecated
	public void removeResident(Resident resident) {

		getDataSource().removeResident(resident);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeResidentList()
	 * 
	 * @param resident
	 */
	@Deprecated
	public void removeResidentList(Resident resident) {

		getDataSource().removeResidentList(resident);

	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeTownBlock()
	 * 
	 * @param townBlock
	 */
	@Deprecated
	public void removeTownBlock(TownBlock townBlock) {

		getDataSource().removeTownBlock(townBlock);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().removeTownBlocks()
	 * 
	 * @param town
	 */
	@Deprecated
	public void removeTownBlocks(Town town) {

		getDataSource().removeTownBlocks(town);
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getAllTownBlocks()
	 * 
	 * @return List all townblocks
	 */
	@Deprecated
	public List<TownBlock> getAllTownBlocks() {

		return getDataSource().getAllTownBlocks();
	}

	/**
	 * Deprecated - use TownyUniverse.getDataSource().getResidents()
	 * 
	 * @param player
	 * @param names
	 * @return List
	 */
	@Deprecated
	public List<Resident> getResidents(Player player, String[] names) {

		return getDataSource().getResidents(player, names);
	}

	/**
	 * (Please use addDeleteTownBlockIdQueue in TownyRegenAPI)
	 * 
	 * @param townBlock
	 */
	@Deprecated
	public void deleteTownBlockIds(TownBlock townBlock) {

		WorldCoord worldCoord = townBlock.getWorldCoord();
		TownyRegenAPI.addDeleteTownBlockIdQueue(worldCoord);
	}

	@Deprecated
	public String checkAndFilterName(String name) throws InvalidNameException {

		return NameValidation.checkAndFilterPlayerName(name);
	}

	@Deprecated
	public String[] checkAndFilterArray(String[] arr) {

		return NameValidation.checkAndFilterArray(arr);
	}

	/**
	 * @author - Articdive
	 * @param minecraftcoordinates - List of minecraft coordinates you should probably parse town.getAllOutpostSpawns()
	 * @param tb - TownBlock to check if its contained..
	 * @note - Pretty much this method checks if a townblock is contained within a list of locations.
	 */
	public static boolean isTownBlockLocContainedInTownOutposts(List<Location> minecraftcoordinates, TownBlock tb) {
		if (minecraftcoordinates != null && tb != null) {
			for (Location minecraftcoordinate : minecraftcoordinates) {
				if (Coord.parseCoord(minecraftcoordinate).equals(tb.getCoord())) {
					return true; // Yes the TownBlock is considered an outpost by the Town
				}
			}
		}
		return false;
	}

}
