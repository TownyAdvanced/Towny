package com.palmergames.bukkit.towny;

import com.earth2me.essentials.Essentials;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.metrics.Metrics;
import com.palmergames.bukkit.towny.chat.TNCRegister;
import com.palmergames.bukkit.towny.command.InviteCommand;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.command.TownyWorldCommand;
import com.palmergames.bukkit.towny.command.commandobjects.AcceptCommand;
import com.palmergames.bukkit.towny.command.commandobjects.CancelCommand;
import com.palmergames.bukkit.towny.command.commandobjects.ConfirmCommand;
import com.palmergames.bukkit.towny.command.commandobjects.DenyCommand;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.TownyStartException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.listeners.TownyBlockListener;
import com.palmergames.bukkit.towny.listeners.TownyCustomListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityMonitorListener;
import com.palmergames.bukkit.towny.listeners.TownyLoginListener;
import com.palmergames.bukkit.towny.listeners.TownyPlayerListener;
import com.palmergames.bukkit.towny.listeners.TownyVehicleListener;
import com.palmergames.bukkit.towny.listeners.TownyWeatherListener;
import com.palmergames.bukkit.towny.listeners.TownyWorldListener;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.BukkitPermSource;
import com.palmergames.bukkit.towny.permissions.GroupManagerSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.permissions.VaultPermSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarBlockListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarCustomListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.FlagWarEntityListener;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import net.milkbowl.vault.permission.Permission;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Towny Plugin for Bukkit
 * 
 * Website &amp; Source: https://github.com/TownyAdvanced/Towny
 * 
 * @author Shade, ElgarL, LlmDl
 */

public class Towny extends JavaPlugin {
	public static final boolean isSpigot = BukkitTools.isSpigot();
	public static final boolean isCitizens = Bukkit.getPluginManager().isPluginEnabled("Citizens");
	private static final Logger LOGGER = LogManager.getLogger(Towny.class);
	public static boolean safeMode = false;
	private static Towny plugin;
	
	private final String version;
	private final TownyUniverse townyUniverse;
	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyVehicleListener vehicleListener = new TownyVehicleListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyCustomListener customListener = new TownyCustomListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private final TownyWeatherListener weatherListener = new TownyWeatherListener(this);
	private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
	private final TownyWorldListener worldListener = new TownyWorldListener(this);
	private final FlagWarBlockListener flagWarBlockListener = new FlagWarBlockListener(this);
	private final FlagWarCustomListener flagWarCustomListener = new FlagWarCustomListener(this);
	private final FlagWarEntityListener flagWarEntityListener = new FlagWarEntityListener();
	private final TownyLoginListener loginListener = new TownyLoginListener();
	private final HUDManager HUDManager = new HUDManager(this);

	private Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<>());

	private Essentials essentials = null;
	
	public Towny() {
		plugin = this;
		version = this.getDescription().getVersion();
		townyUniverse = TownyUniverse.getInstance();
	}

	@Override
	public void onEnable() {
		LOGGER.log(Level.INFO, "====================      Towny      ========================");

		// Setup static classes
		BukkitTools.initialize(this);
		TownyTimerHandler.initialize(this);
		TownyEconomyHandler.initialize(this);
		TownyFormatter.initialize();
		TownyRegenAPI.initialize(this);
		PlayerCacheUtil.initialize(this);
		SpawnUtil.initialize(this);
		TownyPerms.initialize(this);
		InviteHandler.initialize(this);
		ConfirmationHandler.initialize(this);
		try {
			load();

			// Setup bukkit command interfaces
			registerSpecialCommands();
			PluginCommand command;
			if ((command = getCommand("townyadmin")) != null) {
				command.setExecutor(new TownyAdminCommand(this));
			}
			if ((command = getCommand("townyworld")) != null) {
				command.setExecutor(new TownyWorldCommand(this));
			}
			if ((command = getCommand("resident")) != null) {
				command.setExecutor(new ResidentCommand(this));
			}
			if ((command = getCommand("towny")) != null) {
				command.setExecutor(new TownyCommand(this));
			}
			CommandExecutor townCommandExecutor = new TownCommand(this);
			if ((command = getCommand("town")) != null) {
				command.setExecutor(townCommandExecutor);
			}
			// This is needed because the vanilla "/t" tab completer needs to be overridden.
			if ((command = getCommand("t")) != null) {
				command.setTabCompleter((TabCompleter)townCommandExecutor);
			}
			if ((command = getCommand("nation")) != null) {
				command.setExecutor(new NationCommand(this));
			}
			if ((command = getCommand("plot")) != null) {
				command.setExecutor(new PlotCommand(this));
			}
			if ((command = getCommand("invite")) != null) {
				command.setExecutor(new InviteCommand(this));
			}

			addMetricsCharts();

			FlagWar.onEnable();

			// If there are significant changes from versions to versions they can go here.
			if (!version.equalsIgnoreCase(TownySettings.getLastRunVersion(version))) {
				try {
					LOGGER.log(Level.INFO, "------------------------------------");
					LOGGER.log(Level.INFO, "[Towny] ChangeLog up until v" + getVersion());
					boolean display = false;
					String lastVersion = TownySettings.getLastRunVersion(getVersion()).split("_")[0];
					BufferedReader reader = new BufferedReader(new InputStreamReader(Towny.class.getResourceAsStream("/ChangeLog.txt")));
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith(lastVersion)) {
							display = true;
						}
						if (display && line.replaceAll(" ", "").replaceAll("\t", "").length() > 0) {
							LOGGER.log(Level.INFO, line);
						}
						
					}
					LOGGER.log(Level.INFO, "------------------------------------");
				} catch (IOException e) {
					TownyMessaging.sendDebugMsg("Could not read ChangeLog.txt");
				}
				TownySettings.setLastRunVersion(getVersion());
				TownyUniverse townyUniverse = TownyUniverse.getInstance();
				townyUniverse.getDataSource().saveAll();
				townyUniverse.getDataSource().cleanup();
			}

			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
		} catch (TownyStartException startException) {
			safeMode = true;
		}

		registerEvents();

		LOGGER.log(Level.INFO, "=============================================================");
		if (isError()) {
			LOGGER.log(Level.ERROR, "[WARNING] - ***** SAFE MODE ***** " + version);
		} else {
			LOGGER.log(Level.INFO, "[Towny] Version: " + version + " - Mod Enabled");
		}
		LOGGER.log(Level.INFO, "=============================================================");

		if (!isError()) {
			// Re login anyone online. (In case of plugin reloading)
			for (Player player : BukkitTools.getOnlinePlayers())
				if (player != null) {
					
					// Test and kick any players with invalid names.
					if (player.getName().contains(" ")) {
						player.kickPlayer("Invalid name!");
						return;
					}

					// Perform login code in it's own thread to update Towny data.
					if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(this, player), 0L) == -1) {
						TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
					}
				}
		}
	}

	@Override
	public void onDisable() {
		LOGGER.log(Level.INFO, "==============================================================");
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getDataSource() != null && !safeMode) {
			townyUniverse.getDataSource().saveQueues();
		}

		if (!safeMode) {
			FlagWar.onDisable();
		}

		if (TownyAPI.getInstance().isWarTime()) {
			TownyUniverse.getInstance().getWarEvent().toggleEnd();
		}

		stopTimedTasks();

		TownyRegenAPI.cancelProtectionRegenTasks();

		playerCache.clear();
		
		try {
			// Shut down our saving task.
			townyUniverse.getDataSource().cancelTask();
		} catch (NullPointerException ignored) {
			// The saving task will not have started if this disable was fired by onEnable failing.			
		}

		LOGGER.log(Level.INFO, "[Towny] Version: " + version + " - Mod Disabled");
		LOGGER.log(Level.INFO, "=============================================================");
	}
	
	public void load() {
		try {
			TownySettings.loadConfig(getDataFolder() + File.separator + "settings" + File.separator + "config.yml", version);
		} catch (IOException e) {
			e.printStackTrace();
			throw new TownyStartException("Failed to load Town's main configuration.");
		}
		try {
			TownySettings.loadLanguage(getDataFolder() + File.separator + "settings", "english.yml");
		} catch (IOException e) {
			e.printStackTrace();
			throw new TownyStartException("Failed to load Towny's language configuration.");
		}
		TownyPerms.loadPerms(getDataFolder() + File.separator + "settings", "townyperms.yml");
		
		// Init logger
		TownyLogger.getInstance();
		
		//// Database
		String saveDbType = TownySettings.getSaveDatabase();
		String loadDbType = TownySettings.getLoadDatabase();

		// Setup any defaults before we load the dataSource.
		Coord.setCellSize(TownySettings.getTownBlockSize());

		LOGGER.log(Level.INFO, "[Towny] Database: [Load] " + loadDbType + " [Save] " + saveDbType);

		townyUniverse.clearAll();

		long startTime = System.currentTimeMillis();
		switch (loadDbType.toLowerCase()) {
			case "ff":
			case "flatfile": {
				townyUniverse.dataSource = new TownyFlatFileSource(this);
				break;
			}
			case "h2":
			case "sqlite":
			case "mysql": {
				townyUniverse.dataSource = new TownySQLSource(this, loadDbType.toLowerCase());
			}
			default: {
				throw new TownyStartException("Failed to find load-database type: " + loadDbType);
			}
		}
		if (!townyUniverse.dataSource.loadAll()) {
			throw new TownyStartException("Failed to load the load-database.");
		}
		long time = System.currentTimeMillis() - startTime;
		LOGGER.log(Level.INFO, "[Towny] Database loaded in " + time + "ms.");

			townyUniverse.dataSource.cleanupBackups();
			// Set the new class for saving.
			switch (saveDbType.toLowerCase()) {
				case "ff":
				case "flatfile": {
					townyUniverse.dataSource = new TownyFlatFileSource(this);
					break;
				}
				case "h2":
				case "sqlite":
				case "mysql": {
					townyUniverse.dataSource = new TownySQLSource(this, saveDbType.toLowerCase());
					break;
				}
				default: {
					throw new TownyStartException("Failed to find save-database type: " + saveDbType);
				}
			}
			FileMgmt.checkOrCreateFolder(getDataFolder() + File.separator + "logs"); // Setup the logs folder here as the logger will not yet be enabled.
			try {
				townyUniverse.dataSource.backup();

				if (loadDbType.equalsIgnoreCase("flatfile") || saveDbType.equalsIgnoreCase("flatfile")) {
					townyUniverse.dataSource.deleteUnusedResidents();
				}

			} catch (IOException e) {
				System.out.println("[Towny] Error: Could not create backup.");
				e.printStackTrace();
			}

			if (loadDbType.equalsIgnoreCase(saveDbType)) {
				// Update all Worlds data files
				townyUniverse.dataSource.saveAllWorlds();
			} else {
				//Formats are different so save ALL data.
				townyUniverse.dataSource.saveAll();
			}

		if (!(new File(getDataFolder(), "outpostschecked.txt").exists())) {
			for (Town town : townyUniverse.dataSource.getTowns()) {
				TownySQLSource.validateTownOutposts(town);
			}
			saveResource("outpostschecked.txt", false);
		}
		//// End Database
		
		// Check for dependencies
		checkPlugins();

		// make sure the timers are stopped for a reset
		// They may still be started from a reload.
		stopTimedTasks();

		// Start timers
		startTimedTasks();

		// Reset the cache
		resetCache();
	}
	
	private void checkPlugins() {
		List<String> using = new ArrayList<>();
		Plugin test;

		test = getServer().getPluginManager().getPlugin("GroupManager");
		if (test != null) {
			// groupManager = (GroupManager)test;
			TownyUniverse.getInstance().permissionSource = new GroupManagerSource(this, test);
			using.add(String.format("%s v%s", "GroupManager", test.getDescription().getVersion()));
		} else {
			// Try Vault
			test = getServer().getPluginManager().getPlugin("Vault");
			if (test != null) {
				net.milkbowl.vault.chat.Chat chat = getServer().getServicesManager().load(net.milkbowl.vault.chat.Chat.class);
				if (chat == null) {
					// No Chat implementation
					test = null;
					// Fall back to BukkitPermissions below
				} else {
					TownyUniverse.getInstance().permissionSource = new VaultPermSource(this, chat);
					RegisteredServiceProvider<Permission> vaultPermProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
					if (vaultPermProvider != null) {
						using.add(vaultPermProvider.getPlugin().getName() + " " + vaultPermProvider.getPlugin().getDescription().getVersion() + " via Vault " + test.getDescription().getVersion());
					} else {
						using.add(String.format("%s v%s", "Vault", test.getDescription().getVersion()));
					}
				}
			}

			if (test == null) {
				TownyUniverse.getInstance().permissionSource = new BukkitPermSource(this);
				using.add("BukkitPermissions");
			}
		}

		if (TownySettings.isUsingEconomy()) {

			if (TownyEconomyHandler.setupEconomy()) {
				using.add(TownyEconomyHandler.getVersion());
				if (TownyEconomyHandler.getVersion().startsWith("Essentials Economy")) {
					System.out.println("[Towny] Warning: Essentials Economy has been known to reset town and nation bank accounts to their default amount. The authors of Essentials recommend using another economy plugin until they have fixed this bug.");
				}
					
			} else {
				TownyMessaging.sendErrorMsg("No compatible Economy plugins found. Install Vault.jar with any of the supported eco systems.");
				TownyMessaging.sendErrorMsg("If you do not want an economy to be used, set using_economy: false in your Towny config.yml.");
			}
		}

		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null) {
			TownySettings.setUsingEssentials(false);
		} else if (TownySettings.isUsingEssentials()) {
			this.essentials = (Essentials) test;
			using.add(String.format("%s v%s", "Essentials", test.getDescription().getVersion()));
		}
		
		if (Bukkit.getPluginManager().isPluginEnabled("Questioner")) {
			TownyMessaging.sendErrorMsg("Questioner.jar present on server, Towny no longer requires Questioner for invites/confirmations.");
			TownyMessaging.sendErrorMsg("You may safely remove Questioner.jar from your plugins folder.");
		}

		test = getServer().getPluginManager().getPlugin("PlaceholderAPI");
		if(test != null){
            new TownyPlaceholderExpansion(this).register();
            using.add(String.format("%s v%s", "PlaceholderAPI", test.getDescription().getVersion()));
		}

		if (using.size() > 0) {
			System.out.println("[Towny] Using: " + StringMgmt.join(using, ", "));
		}


		//Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}
	}

	private void stopTimedTasks() {
		TownyTimerHandler.toggleTownyRepeatingTimer(false);
		TownyTimerHandler.toggleDailyTimer(false);
		TownyTimerHandler.toggleMobRemoval(false);
		TownyTimerHandler.toggleHealthRegen(false);
		TownyTimerHandler.toggleTeleportWarmup(false);
		TownyTimerHandler.toggleCooldownTimer(false);
		TownyTimerHandler.toggleDrawSmokeTask(false);
	}

	private void startTimedTasks() {
		TownyTimerHandler.toggleTownyRepeatingTimer(true);
		TownyTimerHandler.toggleDailyTimer(true);
		TownyTimerHandler.toggleMobRemoval(true);
		TownyTimerHandler.toggleHealthRegen(TownySettings.hasHealthRegen());
		TownyTimerHandler.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0);
		TownyTimerHandler.toggleCooldownTimer(TownySettings.getPVPCoolDownTime() > 0 || TownySettings.getSpawnCooldownTime() > 0);
		TownyTimerHandler.toggleDrawSmokeTask(true);
	}

	private void registerEvents() {

		final PluginManager pluginManager = getServer().getPluginManager();

		if (!isError()) {
			// Have War Events get launched before regular events.
			pluginManager.registerEvents(flagWarBlockListener, this);
			pluginManager.registerEvents(flagWarEntityListener, this);
			
			// Huds
			pluginManager.registerEvents(HUDManager, this);

			// Manage player deaths and death payments
			pluginManager.registerEvents(entityMonitorListener, this);
			pluginManager.registerEvents(vehicleListener, this);
			pluginManager.registerEvents(weatherListener, this);
			pluginManager.registerEvents(flagWarCustomListener, this);
			pluginManager.registerEvents(customListener, this);
			pluginManager.registerEvents(worldListener, this);
			pluginManager.registerEvents(loginListener, this);
		}

		// Always register these events.
		pluginManager.registerEvents(playerListener, this);
		pluginManager.registerEvents(blockListener, this);
		pluginManager.registerEvents(entityListener, this);

	}
	/**
	 * Fetch the TownyUniverse instance.
	 * 
	 * @return TownyUniverse
	 * @deprecated use {@link com.palmergames.bukkit.towny.TownyUniverse#getInstance()}
	 */
	public com.palmergames.bukkit.towny.TownyUniverse getTownyUniverse() {

		return townyUniverse;
	}

	public String getVersion() {

		return version;
	}

	/**
	 * @deprecated {Use {@link #safeMode}}
	 * @return the error
	 */
	@Deprecated
	public boolean isError() {
		return safeMode;
	}

	// is Essentials active
	public boolean isEssentials() {

		return (TownySettings.isUsingEssentials() && (this.essentials != null));
	}


	/**
	 * @deprecated {Use {@link #isCitizens}}
	 * @return if we are using citizens2
	 */
	@Deprecated
	public boolean isCitizens2() {

		return isCitizens;
	}

	/**
	 * @return Essentials object
	 * @throws TownyException - If Towny can't find Essentials.
	 */
	public Essentials getEssentials() throws TownyException {

		if (essentials == null)
			throw new TownyException("Essentials is not installed, or not enabled!");
		else
			return essentials;
	}

	public World getServerWorld(String name) throws NotRegisteredException {

		for (World world : BukkitTools.getWorlds())
			if (world.getName().equals(name))
				return world;

		throw new NotRegisteredException(String.format("A world called '$%s' has not been registered.", name));
	}

	public boolean hasCache(Player player) {

		return playerCache.containsKey(player.getName().toLowerCase());
	}

	public void newCache(Player player) {

		try {
			playerCache.put(player.getName().toLowerCase(), new PlayerCache(TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()), player));
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Could not create permission cache for this world (" + player.getWorld().getName() + ".");
		}

	}

	public void deleteCache(Player player) {

		deleteCache(player.getName());
	}

	public void deleteCache(String name) {

		playerCache.remove(name.toLowerCase());
	}

	/**
	 * Fetch the current players cache
	 * Creates a new one, if one doesn't exist.
	 * 
	 * @param player - Player to get the current cache from.
	 * @return the current (or new) cache for this player.
	 */
	public PlayerCache getCache(Player player) {

		if (!hasCache(player)) {
			newCache(player);
			getCache(player).setLastTownBlock(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)));
		}

		return playerCache.get(player.getName().toLowerCase());
	}

	/**
	 * Resets all Online player caches, retaining their location info.
	 */
	public void resetCache() {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				getCache(player).resetAndUpdate(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player))); // Automatically
																														// resets
																														// permissions.
	}

	/**
	 * Resets all Online player caches if their location equals this one
	 * 
	 * @param worldCoord - the location to check for
	 */
	public void updateCache(WorldCoord worldCoord) {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (Coord.parseCoord(player).equals(worldCoord))
					getCache(player).resetAndUpdate(worldCoord); // Automatically
																	// resets
																	// permissions.
	}

	/**
	 * Resets all Online player caches if their location has changed
	 */
	public void updateCache() {

		WorldCoord worldCoord = null;

		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player));
				PlayerCache cache = getCache(player);
				if (cache.getLastTownBlock() != worldCoord)
					cache.resetAndUpdate(worldCoord);
			}
		}
	}

	/**
	 * Resets a specific players cache if their location has changed
	 * 
	 * @param player - Player, whose cache is to be updated.
	 */
	public void updateCache(Player player) {

		WorldCoord worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player));
		PlayerCache cache = getCache(player);

		if (cache.getLastTownBlock() != worldCoord)
			cache.resetAndUpdate(worldCoord);
	}

	/**
	 * Resets a specific players cache
	 * 
	 * @param player - Player, whose cache is to be reset.
	 */
	public void resetCache(Player player) {

		getCache(player).resetAndUpdate(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)));
	}

	public void setPlayerMode(Player player, String[] modes, boolean notify) {

		if (player == null)
			return;

		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			resident.setModes(modes, notify);

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
		}
	}

	/**
	 * Remove ALL current modes (and set the defaults)
	 * 
	 * @param player - player, whose modes are to be reset (all removed).
	 */
	public void removePlayerMode(Player player) {

		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			resident.clearModes();

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
		}

	}

	/**
	 * Fetch a list of all the players current modes.
	 * 
	 * @param player - player, whose modes are to be listed, taken.
	 * @return list of modes
	 */
	public List<String> getPlayerMode(Player player) {

		return getPlayerMode(player.getName());
	}

	public List<String> getPlayerMode(String name) {

		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(name);
			return resident.getModes();

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
			return null;
		}
	}

	/**
	 * Check if the player has a specific mode.
	 * 
	 * @param player - Player to be checked
	 * @param mode - Mode to be checked for within player.
	 * @return true if the mode is present.
	 */
	public boolean hasPlayerMode(Player player, String mode) {

		return hasPlayerMode(player.getName(), mode);
	}

	public boolean hasPlayerMode(String name, String mode) {

		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(name);
			return resident.hasMode(mode);

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
			return false;
		}
	}

	public String getConfigPath() {

		return getDataFolder().getPath() + File.separator + "settings" + File.separator + "config.yml";
	}

	public Object getSetting(String root) {

		return TownySettings.getProperty(root);
	}

	public void log(String msg) {

		if (TownySettings.isLogging()) {
			LOGGER.info(ChatColor.stripColor(msg));
		}
	}
	

	public boolean parseOnOff(String s) throws Exception {

		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception(String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
	}

	/**
	 * @return the Towny instance
	 */
	public static Towny getPlugin() {
		return plugin;
	}

	/**
	 * @return the playerListener
	 */
	public TownyPlayerListener getPlayerListener() {
	
		return playerListener;
	}

	
	/**
	 * @return the vehicleListener
	 */
	public TownyVehicleListener getVehicleListener() {
	
		return vehicleListener;
	}

	
	/**
	 * @return the entityListener
	 */
	public TownyEntityListener getEntityListener() {
	
		return entityListener;
	}

	
	/**
	 * @return the weatherListener
	 */
	public TownyWeatherListener getWeatherListener() {
	
		return weatherListener;
	}

	
	/**
	 * @return the entityMonitorListener
	 */
	public TownyEntityMonitorListener getEntityMonitorListener() {
	
		return entityMonitorListener;
	}

	
	/**
	 * @return the worldListener
	 */
	public TownyWorldListener getWorldListener() {
	
		return worldListener;
	}

	
	/**
	 * @return the flagWarBlockListener
	 */
	public FlagWarBlockListener getFlagWarBlockListener() {
	
		return flagWarBlockListener;
	}

	
	/**
	 * @return the flagWarCustomListener
	 */
	public FlagWarCustomListener getFlagWarCustomListener() {
	
		return flagWarCustomListener;
	}

	
	/**
	 * @return the flagWarEntityListener
	 */
	public FlagWarEntityListener getFlagWarEntityListener() {
	
		return flagWarEntityListener;
	}
	
	/**
	 * @return the HUDManager
	 */
	public HUDManager getHUDManager() {
		
		return HUDManager;
	}

	// https://www.spigotmc.org/threads/small-easy-register-command-without-plugin-yml.38036/
	private void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>();
		commands.add(new AcceptCommand(TownySettings.getAcceptCommand()));
		commands.add(new DenyCommand(TownySettings.getDenyCommand()));
		commands.add(new ConfirmCommand(TownySettings.getConfirmCommand()));
		commands.add(new CancelCommand(TownySettings.getCancelCommand()));
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			commandMap.registerAll("towny", commands);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private void addMetricsCharts() {
		/*
		 * Register bStats Metrics
		 */
		Metrics metrics = new Metrics(this);
		
		metrics.addCustomChart(new Metrics.SimplePie("language", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return TownySettings.getString(ConfigNodes.LANGUAGE);
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("server_type", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (Bukkit.getServer().getName().equalsIgnoreCase("paper"))
					return "Paper";
				else if (Bukkit.getServer().getName().equalsIgnoreCase("craftbukkit")) {
					if (isSpigot)
						return "Spigot";
					else 
						return "CraftBukkit";
				}
				return "Unknown";
			}
		}));

		metrics.addCustomChart(new Metrics.SimplePie("nation_zones_enabled", new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (TownySettings.getNationZonesEnabled())
					return "true";
				else
					return "false";
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("database_type", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return TownySettings.getSaveDatabase().toLowerCase();
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("town_block_size", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return String.valueOf(TownySettings.getTownBlockSize());
			}
		}));
	}
}
