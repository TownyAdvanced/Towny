package com.palmergames.bukkit.towny.object;

import static com.palmergames.bukkit.towny.object.TownyObservableType.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownyHModFlatFileSource;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.tasks.DailyTimerTask;
import com.palmergames.bukkit.towny.tasks.HealthRegenTimerTask;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.towny.tasks.RepeatingTimerTask;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.war.War;
import com.palmergames.bukkit.util.MinecraftTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.TimeMgmt;


public class TownyUniverse extends TownyObject {
	
	private static Towny plugin;
	
	protected Hashtable<String, Resident> residents = new Hashtable<String, Resident>();
	protected Hashtable<String, Town> towns = new Hashtable<String, Town>();
	protected Hashtable<String, Nation> nations = new Hashtable<String, Nation>();
	protected Hashtable<String, TownyWorld> worlds = new Hashtable<String, TownyWorld>();
	
	private Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<BlockLocation, ProtectionRegenTask>();
	private Set<Block> protectionPlaceholders = new HashSet<Block>();
	
	
	//private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<String, PlotBlockData>();

	// private List<Election> elections;
	private static TownyDataSource dataSource;
	private static CachePermissions cachePermissions = new CachePermissions();
	private static TownyPermissionSource permissionSource;

	private int townyRepeatingTask = -1;
	private int dailyTask = -1;
	private int mobRemoveTask = -1;
	private int healthRegenTask = -1;
	private int teleportWarmupTask = -1;
	private War warEvent;
	private String rootFolder;

	public TownyUniverse() {
		setName("");
		rootFolder = "";
	}

	public TownyUniverse(String rootFolder) {
		setName("");
		this.rootFolder = rootFolder;
	}

	public TownyUniverse(Towny plugin) {
		setName("");
		TownyUniverse.plugin = plugin;
	}

	public void newDay() {
		if (!isDailyTimerRunning())
			toggleDailyTimer(true);
		//dailyTimer.schedule(new DailyTimerTask(this), 0);
		if (getPlugin().getServer().getScheduler().scheduleAsyncDelayedTask(getPlugin(), new DailyTimerTask(this)) == -1)
			TownyMessaging.sendErrorMsg("Could not schedule newDay.");
		setChangedNotify(NEW_DAY);
	}

	public void toggleTownyRepeatingTimer(boolean on) {
		if (on && !isTownyRepeatingTaskRunning()) {
			townyRepeatingTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new RepeatingTimerTask(this), 0, MinecraftTools.convertToTicks(TownySettings.getPlotManagementSpeed()));
			if (townyRepeatingTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule Towny Timer Task.");
		} else if (!on && isTownyRepeatingTaskRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(townyRepeatingTask);
			townyRepeatingTask = -1;
		}
		setChanged();
	}

	public void toggleMobRemoval(boolean on) {
		if (on && !isMobRemovalRunning()) {
			mobRemoveTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new MobRemovalTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
			if (mobRemoveTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule mob removal loop.");
		} else if (!on && isMobRemovalRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(mobRemoveTask);
			mobRemoveTask = -1;
		}
		setChangedNotify(TOGGLE_MOB_REMOVAL);
	}

	public void toggleDailyTimer(boolean on) {
		if (on && !isDailyTimerRunning()) {
			long timeTillNextDay = TownyUtil.townyTime();
			TownyMessaging.sendMsg("Time until a New Day: " + TimeMgmt.formatCountdownTime(timeTillNextDay));
			dailyTask = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new DailyTimerTask(this), MinecraftTools.convertToTicks(timeTillNextDay), MinecraftTools.convertToTicks(TownySettings.getDayInterval()));
			if (dailyTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule new day loop.");
		} else if (!on && isDailyTimerRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(dailyTask);
			dailyTask = -1;
		}
		setChangedNotify(TOGGLE_DAILY_TIMER);
	}

	public void toggleHealthRegen(boolean on) {
		if (on && !isHealthRegenRunning()) {
			healthRegenTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new HealthRegenTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
			if (healthRegenTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule health regen loop.");
		} else if (!on && isHealthRegenRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(healthRegenTask);
			healthRegenTask = -1;
		}
		setChangedNotify(TOGGLE_HEALTH_REGEN);
	}

	public void toggleTeleportWarmup(boolean on) {
		if (on && !isTeleportWarmupRunning()) {
			teleportWarmupTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new TeleportWarmupTimerTask(this), 0, 20);
			if (teleportWarmupTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule teleport warmup loop.");
		} else if (!on && isTeleportWarmupRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(teleportWarmupTask);
			teleportWarmupTask = -1;
		}
		setChangedNotify(TOGGLE_TELEPORT_WARMUP);
	}

	public boolean isTownyRepeatingTaskRunning() {
		return townyRepeatingTask != -1;

	}

	public boolean isMobRemovalRunning() {
		return mobRemoveTask != -1;
	}

	public boolean isDailyTimerRunning() {
		return dailyTask != -1;
	}

	public boolean isHealthRegenRunning() {
		return healthRegenTask != -1;
	}

	public boolean isTeleportWarmupRunning() {
		return teleportWarmupTask != -1;
	}

	public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {

		if (!player.isOnline())
			return;

		// Test and kick any players with invalid names.
		if ((player.getName().trim() == null) || (player.getName().contains(" "))) {
			player.kickPlayer("Invalid name!");
			return;
		}

		Resident resident;

		if (!getDataSource().hasResident(player.getName())) {
			getDataSource().newResident(player.getName());
			resident = getDataSource().getResident(player.getName());

			TownyMessaging.sendMessage(player, TownySettings.getRegistrationMsg(player.getName()));
			resident.setRegistered(System.currentTimeMillis());
			if (!TownySettings.getDefaultTownName().equals(""))
				try {
					Town town = getDataSource().getTown(TownySettings.getDefaultTownName());
					town.addResident(resident);
					getDataSource().saveTown(town);
				} catch (NotRegisteredException e) {
				} catch (AlreadyRegisteredException e) {
				}

			getDataSource().saveResident(resident);
			getDataSource().saveResidentList();

		} else {
			resident = getDataSource().getResident(player.getName());
			resident.setLastOnline(System.currentTimeMillis());

			getDataSource().saveResident(resident);
		}

		try {
			TownyMessaging.sendTownBoard(player, resident.getTown());
		} catch (NotRegisteredException e) {
		}

		if (isWarTime())
			getWarEvent().sendScores(player, 3);

		//Schedule to setup default modes when the player has finished loading
		if (getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new SetDefaultModes(this, player, false), 1) == -1)
			TownyMessaging.sendErrorMsg("Could not set default modes for " + player.getName() + ".");

		setChangedNotify(PLAYER_LOGIN);
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
	
	public String checkAndFilterName(String name) throws InvalidNameException {
		String out = TownySettings.filterName(name);

		if (!TownySettings.isValidName(out))
			throw new InvalidNameException(out + " is an invalid name.");

		return out;
	}

	public String[] checkAndFilterArray(String[] arr) {
		String[] out = arr;
		int count = 0;

		for (String word : arr) {
			out[count] = TownySettings.filterName(word);
			count++;
		}

		return out;
	}	

	public static Player getPlayer(Resident resident) throws TownyException {
		for (Player player : getOnlinePlayers())
			if (player.getName().equals(resident.getName()))
				return player;
		throw new TownyException(String.format("%s is not online", resident.getName()));
	}

	public static Player[] getOnlinePlayers() {
		return Bukkit.getOnlinePlayers();
	}

	public static List<Player> getOnlinePlayers(ResidentList residents) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : getOnlinePlayers())
			if (residents.hasResident(player.getName()))
				players.add(player);
		return players;
	}

	public static List<Player> getOnlinePlayers(Town town) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player player : getOnlinePlayers())
			if (town.hasResident(player.getName()))
				players.add(player);
		return players;
	}

	public static List<Player> getOnlinePlayers(Nation nation) {
		ArrayList<Player> players = new ArrayList<Player>();
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
	public boolean isWilderness(Block block) {

		WorldCoord worldCoord;

		try {
			worldCoord = new WorldCoord(getDataSource().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
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
	public String getTownName(Location loc) {

		try {
			WorldCoord worldCoord = new WorldCoord(getDataSource().getWorld(loc.getWorld().getName()), Coord.parseCoord(loc));
			return worldCoord.getTownBlock().getTown().getName();
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
	public TownBlock getTownBlock(Location loc) {

		TownyMessaging.sendDebugMsg("Fetching TownBlock");

		try {
			WorldCoord worldCoord = new WorldCoord(getDataSource().getWorld(loc.getWorld().getName()), Coord.parseCoord(loc));
			return worldCoord.getTownBlock();
		} catch (NotRegisteredException e) {
			// No data so return null
			return null;
		}
	}
	
	public List<Resident> getActiveResidents() {
		List<Resident> activeResidents = new ArrayList<Resident>();
		for (Resident resident : getDataSource().getResidents())
			if (isActiveResident(resident))
				activeResidents.add(resident);
		return activeResidents;
	}

	public boolean isActiveResident(Resident resident) {
		return ((System.currentTimeMillis() - resident.getLastOnline() < (20 * TownySettings.getInactiveAfter())) || (plugin.isOnline(resident.getName())));
	}

	/*
	public List<String> getStatus(TownBlock townBlock) {
		return TownyFormatter.getStatus(townBlock);
	}

	public List<String> getStatus(Resident resident) {
		return TownyFormatter.getStatus(resident);
	}

	public List<String> getStatus(Town town) {
		return TownyFormatter.getStatus(town);
	}

	public List<String> getStatus(Nation nation) {
		return TownyFormatter.getStatus(nation);
	}

	public List<String> getStatus(TownyWorld world) {
		return TownyFormatter.getStatus(world);
	}

	public List<String> getTaxStatus(Resident resident) {
		return TownyFormatter.getTaxStatus(resident);
	}
	*/
	
	public String getRootFolder() {
		if (plugin != null)
			return plugin.getDataFolder().getPath();
		else
			return rootFolder;
	}

	public boolean loadSettings() {
		try {
			FileMgmt.checkFolders(new String[] { getRootFolder(), getRootFolder() + FileMgmt.fileSeparator() + "settings", getRootFolder() + FileMgmt.fileSeparator() + "logs" }); // Setup the logs folder here as the logger will not yet be enabled.

			TownySettings.loadConfig(getRootFolder() + FileMgmt.fileSeparator() + "settings" + FileMgmt.fileSeparator() + "config.yml", plugin.getVersion());
			TownySettings.loadLanguage(getRootFolder() + FileMgmt.fileSeparator() + "settings", "english.yml");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// Setup any defaults before we load the database.
		Coord.setCellSize(TownySettings.getTownBlockSize());

		System.out.println("[Towny] Database: [Load] " + TownySettings.getLoadDatabase() + " [Save] " + TownySettings.getSaveDatabase());
		
		worlds.clear();
		nations.clear();
		towns.clear();
		residents.clear();
		
		if (!loadDatabase(TownySettings.getLoadDatabase())) {
			System.out.println("[Towny] Error: Failed to load!");
			return false;
		}

		try {
			getDataSource().cleanupBackups();
			// Set the new class for saving.
			//setDataSource(TownySettings.getSaveDatabase());
			//getDataSource().initialize(plugin, this);
			try {
				getDataSource().backup();
				getDataSource().deleteUnusedResidentFiles();
			} catch (IOException e) {
				System.out.println("[Towny] Error: Could not create backup.");
				e.printStackTrace();
				return false;
			}

			//if (TownySettings.isSavingOnLoad())
			//      townyUniverse.getDataSource().saveAll();
		} catch (UnsupportedOperationException e) {
			System.out.println("[Towny] Error: Unsupported save format!");
			return false;
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
	
	public void setDataSource(String databaseType) throws UnsupportedOperationException {
		if (databaseType.equalsIgnoreCase("flatfile"))
			setDataSource(new TownyFlatFileSource());
		else if (databaseType.equalsIgnoreCase("flatfile-hmod"))
			setDataSource(new TownyHModFlatFileSource());
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

	public static CachePermissions getCachePermissions() {
		return cachePermissions;
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

	public boolean isWarTime() {
		return warEvent != null ? warEvent.isWarTime() : false;
	}

	public void startWarEvent() {
		this.warEvent = new War(plugin, TownySettings.getWarTimeWarningDelay());
		setChangedNotify(WAR_START);
	}

	public void endWarEvent() {
		if (isWarTime())
			warEvent.toggleEnd();
		// Automatically makes warEvent null
		setChangedNotify(WAR_END);
	}

	public void clearWarEvent() {
		getWarEvent().cancelTasks(getPlugin().getServer().getScheduler());
		setWarEvent(null);
		setChangedNotify(WAR_CLEARED);
	}

	//TODO: throw error if null
	public War getWarEvent() {
		return warEvent;
	}

	public void setWarEvent(War warEvent) {
		this.warEvent = warEvent;
		setChangedNotify(WAR_SET);
	}

	public static Towny getPlugin() {
		return plugin;
	}

	public void setPlugin(Towny plugin) {
		TownyUniverse.plugin = plugin;
	}	

	public void sendUniverseTree(CommandSender sender) {
		for (String line : getTreeString(0))
			sender.sendMessage(line);
	}

	@Override
	public List<String> getTreeString(int depth) {
		List<String> out = new ArrayList<String>();
		out.add(getTreeDepth(depth) + "Universe (" + getName() + ")");
		if (plugin != null) {
			out.add(getTreeDepth(depth + 1) + "Server (" + plugin.getServer().getName() + ")");
			out.add(getTreeDepth(depth + 2) + "Version: " + plugin.getServer().getVersion());
			out.add(getTreeDepth(depth + 2) + "Players: " + plugin.getServer().getOnlinePlayers().length + "/" + plugin.getServer().getMaxPlayers());
			out.add(getTreeDepth(depth + 2) + "Worlds (" + plugin.getServer().getWorlds().size() + "): " + Arrays.toString(plugin.getServer().getWorlds().toArray(new World[0])));
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

	public void sendMessageTo(ResidentList residents, String msg, String modeRequired) {
		for (Player player : getOnlinePlayers(residents))
			if (plugin.hasPlayerMode(player, modeRequired))
				player.sendMessage(msg);
	}

	public List<Resident> getValidatedResidents(Object sender, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names) {
			List<Player> matches = plugin.getServer().matchPlayer(name);
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

	public List<Resident> getOnlineResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names) {
			List<Player> matches = plugin.getServer().matchPlayer(name);
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

	public List<Resident> getOnlineResidents(ResidentList residentList) {
		List<Resident> onlineResidents = new ArrayList<Resident>();
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			for (Resident resident : residentList.getResidents()) {
				if (resident.getName().equalsIgnoreCase(player.getName()))
					onlineResidents.add(resident);
			}
		}

		return onlineResidents;
	}

	public void requestTeleport(Player player, Town town, double cost) {
		try {
			TeleportWarmupTimerTask.requestTeleport(getDataSource().getResident(player.getName().toLowerCase()), town, cost);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

		setChangedNotify(TELEPORT_REQUEST);
	}

	public void abortTeleportRequest(Resident resident) {
		TeleportWarmupTimerTask.abortTeleportRequest(resident);
	}

	public void addWarZone(WorldCoord worldCoord) {
		worldCoord.getWorld().addWarZone(worldCoord);
		plugin.updateCache(worldCoord);
	}

	public void removeWarZone(WorldCoord worldCoord) {
		worldCoord.getWorld().removeWarZone(worldCoord);
		plugin.updateCache(worldCoord);
	}

	public boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {
		try {
			return isEnemy(getDataSource().getResident(player.getName()).getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException e) {
			return false;
		}
	}

	public boolean hasProtectionRegenTask(BlockLocation blockLocation) {
		for (BlockLocation location : protectionRegenTasks.keySet()) {
			if (location.isLocation(blockLocation)) {
				return true;
			}
		}
		return false;
	}

	public ProtectionRegenTask GetProtectionRegenTask(BlockLocation blockLocation) {
		for (BlockLocation location : protectionRegenTasks.keySet()) {
			if (location.isLocation(blockLocation)) {
				return protectionRegenTasks.get(location);
			}
		}
		return null;
	}

	public void addProtectionRegenTask(ProtectionRegenTask task) {
		protectionRegenTasks.put(task.getBlockLocation(), task);
	}

	public void removeProtectionRegenTask(ProtectionRegenTask task) {
		protectionRegenTasks.remove(task.getBlockLocation());
		if (protectionRegenTasks.isEmpty())
			protectionPlaceholders.clear();
	}

	public void cancelProtectionRegenTasks() {
		for (ProtectionRegenTask task : protectionRegenTasks.values()) {
			plugin.getServer().getScheduler().cancelTask(task.getTaskId());
			task.replaceProtections();
		}
		protectionRegenTasks.clear();
		protectionPlaceholders.clear();
	}

	public boolean isPlaceholder(Block block) {
		return protectionPlaceholders.contains(block);
	}

	public void addPlaceholder(Block block) {
		protectionPlaceholders.add(block);
	}

	public void removePlaceholder(Block block) {
		protectionPlaceholders.remove(block);
	}
	
	public void setChangedNotify(TownyObservableType type) {
		setChanged();
		notifyObservers(type);
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
	
	/*
	@Deprecated
	public void sendMessage(Player player, List<String> lines) {
	        sendMessage(player, lines.toArray(new String[0]));
	}
	@Deprecated
	public void sendTownMessage(Town town, List<String> lines) {
	        sendTownMessage(town, lines.toArray(new String[0]));
	}
	@Deprecated
	public void sendNationMessage(Nation nation, List<String> lines) {
	        sendNationMessage(nation, lines.toArray(new String[0]));
	}
	@Deprecated
	public void sendGlobalMessage(List<String> lines) {
	        sendGlobalMessage(lines.toArray(new String[0]));
	}
	@Deprecated
	public void sendGlobalMessage(String line) {
	        for (Player player : getOnlinePlayers()) {
	                player.sendMessage(line);
	                plugin.log("[Global Message] " + player.getName() + ": " + line);
	        }
	}
	@Deprecated
	public void sendMessage(Player player, String[] lines) {
	        for (String line : lines) {
	                player.sendMessage(line);
	                //plugin.log("[send Message] " + player.getName() + ": " + line);
	        }
	}
	@Deprecated
	public void sendResidentMessage(Resident resident, String[] lines) throws TownyException {
	        for (String line : lines)
	                plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
	        Player player = getPlayer(resident);
	        for (String line : lines)
	                player.sendMessage(line);
	        
	}
	@Deprecated
	public void sendTownMessage(Town town, String[] lines) {
	        for (String line : lines)
	                plugin.log("[Town Msg] " + town.getName() + ": " + line);
	        for (Player player : getOnlinePlayers(town)){
	                for (String line : lines)
	                        player.sendMessage(line);
	        }
	}
	@Deprecated
	public void sendNationMessage(Nation nation, String[] lines) {
	        for (String line : lines)
	                plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
	        for (Player player : getOnlinePlayers(nation))
	                for (String line : lines)
	                        player.sendMessage(line);
	}
	@Deprecated
	public void sendGlobalMessage(String[] lines) {
	        for (String line : lines)
	                plugin.log("[Global Msg] " + line);
	        for (Player player : getOnlinePlayers())
	                for (String line : lines)
	                        player.sendMessage(line);
	}
	@Deprecated
	public void sendResidentMessage(Resident resident, String line) throws TownyException {
	        plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
	        Player player = getPlayer(resident);
	        player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}
	@Deprecated
	public void sendTownMessage(Town town, String line) {
	        plugin.log("[Town Msg] " + town.getName() + ": " + line);
	        for (Player player : getOnlinePlayers(town))
	                player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}
	@Deprecated
	public void sendNationMessage(Nation nation, String line) {
	        plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
	        for (Player player : getOnlinePlayers(nation))
	                player.sendMessage(line);
	}
	@Deprecated
	public void sendTownBoard(Player player, Town town) {
	        for (String line : ChatTools.color(Colors.Gold + "[" + town.getName() + "] " + Colors.Yellow + town.getTownBoard()))
	                player.sendMessage(line);
	}
	*/
}
