 package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.config.migration.ConfigMigrator;
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
import com.palmergames.bukkit.towny.db.DatabaseConfig;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.listeners.TownyPaperEvents;
import com.palmergames.bukkit.towny.listeners.TownyBlockListener;
import com.palmergames.bukkit.towny.listeners.TownyCustomListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityListener;
import com.palmergames.bukkit.towny.listeners.TownyEntityMonitorListener;
import com.palmergames.bukkit.towny.listeners.TownyInventoryListener;
import com.palmergames.bukkit.towny.listeners.TownyLoginListener;
import com.palmergames.bukkit.towny.listeners.TownyPlayerListener;
import com.palmergames.bukkit.towny.listeners.TownyServerListener;
import com.palmergames.bukkit.towny.listeners.TownyVehicleListener;
import com.palmergames.bukkit.towny.listeners.TownyWorldListener;
import com.palmergames.bukkit.towny.object.ChangelogResult;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import com.palmergames.bukkit.towny.scheduling.impl.BukkitTaskScheduler;
import com.palmergames.bukkit.towny.scheduling.impl.PaperTaskScheduler;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.utils.ChangelogReader;
import com.palmergames.bukkit.towny.utils.ChunkNotificationUtil;
import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.JavaUtil;

import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Main class for <a href="https://github.com/TownyAdvanced/Towny">Towny</a>
 * @author Shade, ElgarL, LlmDl
 */
public class Towny extends JavaPlugin {
	private static Towny plugin;
	private final String version = this.getPluginMeta().getVersion();

	private TownyUniverse townyUniverse;

	private final boolean isFolia = JavaUtil.classExists("io.papermc.paper.threadedregions.RegionizedServer");
	private final TaskScheduler scheduler;

	private final Map<UUID, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<>());
	private final Set<TownyInitException.TownyError> errors = new HashSet<>();
	
	public Towny() {
		plugin = this;
		this.scheduler = isFolia() ? new FoliaTaskScheduler(this) : expandedSchedulingAvailable() ? new PaperTaskScheduler(this) : new BukkitTaskScheduler(this);
	}

	@Override
	public void onEnable() {

		Bukkit.getLogger().info("====================      Towny      ========================");

		if (!isFolia && !JavaUtil.classExists("io.papermc.paper.configuration.Configuration")) {
			getLogger().severe("Towny 0.101.2.5 and up no longer supports Spigot/CraftBukkit, and now requires Paper to run. See https://papermc.io for more information about Paper.");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		townyUniverse = TownyUniverse.getInstance();
		
		// Setup static classes
		BukkitTools.initialize(this);
		TownyTimerHandler.initialize(this);
		TownyEconomyHandler.initialize(this);
		TownyFormatter.initialize();
		PlayerCacheUtil.initialize(this);
		TownyPerms.initialize(this);
		InviteHandler.initialize(this);
		SpawnUtil.initialize(this);

		try {
			// Load the foundation of Towny, containing config, locales, database.
			loadFoundation(false);

			// Make sure the timers are stopped for a reset, then started.
			cycleTimers();
			// Reset the player cache.
			resetCache();
			// Check for plugin updates if the Minecraft version is still supported.
			if (isMinecraftVersionStillSupported())
				TownyUpdateChecker.checkForUpdates(this);
		} catch (TownyInitException tie) {
			addError(tie.getError());
			getLogger().log(Level.SEVERE, tie.getMessage(), tie);
		}
		
		// NOTE: Runs regardless if Towny errors out!
		// Important for safe mode.

		// Check for plugins that we use or we develop, 
		// print helpful information to startup log.
		PluginIntegrations.getInstance().checkForPlugins(this);
		// Setup bukkit command interfaces
		registerSpecialCommands();
		registerCommands();
		// Add custom metrics charts.
		addMetricsCharts();

		// If we aren't going to enter safe mode, do the following:
		if (!isError()) {
			if (TownySettings.isTownyUpdating(getVersion())) {
				printChangelogToConsole();

				// Save database.
				townyUniverse.getDataSource().saveAll();
				// cleanup() updates SQL schema for any changes.
				townyUniverse.getDataSource().cleanup();
			}

			if (!TownySettings.getLastRunVersion().equals(getVersion()))
				TownySettings.setLastRunVersion(getVersion());
		}
		
		// It is probably a good idea to always handle permissions
		// However, this would spit out an ugly Exception if perms or the config are bugged.
		// Hence, these if checks.
		if (!isError(TownyInitException.TownyError.MAIN_CONFIG) && !isError(TownyInitException.TownyError.PERMISSIONS)) {
			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
		}

		registerEvents();

		Bukkit.getLogger().info("=============================================================");
		if (isError()) {
			plugin.getLogger().warning("[WARNING] - ***** SAFE MODE ***** " + version);
		} else {
			plugin.getLogger().info("Version: " + version + " - Plugin Enabled");
		}
		Bukkit.getLogger().info("=============================================================");

		if (!isError()) {
			TownyAsciiMap.initialize();

			// Re login anyone online. (In case of plugin reloading)
			for (Player player : BukkitTools.getOnlinePlayers())
				if (player != null) {

					// Test and kick any players with invalid names.
					if (player.getName().contains(" ")) {
						player.kick(Component.text("[Towny] Invalid name!"));
						return;
					}

					// Perform login code in it's own thread to update Towny data.
					scheduler.run(new OnPlayerLogin(this, player));
				}
		}
	}

	public void loadFoundation(boolean reload) {
		// Before anything can continue we must load the databaseconfig, config 
		// file, language and permissions, setting the foundation for Towny.
		
		// Handle any legacy config settings.
		handleLegacyConfigs();

		// Load the database config first, so any conversion happens before the config is loaded.
		loadDatabaseConfig(reload);
		// Then load the config.
		loadConfig(reload);
		// Then load the language files.
		loadLocalization(reload);
		// Then load permissions
		loadPermissions(reload);
		// Unregister PAPIExpansion.
		PluginIntegrations.getInstance().unloadPAPIExpansion(reload);

		// Initialize the type handler after the config is loaded and before the database is.
		TownBlockTypeHandler.initialize();

		// Initialize the special log4j hook logger.
		TownyLogger.initialize();

		// Initialize the ResidentModeHandler.
		ResidentModeHandler.initialize();

		// Clear all objects from the TownyUniverse class.
		townyUniverse.clearAllObjects();

		// Try to load and save the database.
		townyUniverse.loadAndSaveDatabase(TownySettings.getLoadDatabase(), TownySettings.getSaveDatabase());

		// Schedule metadata to be loaded
		MetadataLoader.getInstance().scheduleDeserialization();

		// Try migrating the config and world files if the version has changed.
		if (!TownySettings.getLastRunVersion().equals(getVersion())) {
			ConfigMigrator migrator = new ConfigMigrator(TownySettings.getConfig(), "config-migration.json", false);
			migrator.migrate();
		}

		// Loads Town and Nation Levels after migration has occured.
		loadTownAndNationLevels();

		// Re-register PAPIExpansion.
		PluginIntegrations.getInstance().loadPAPIExpansion(reload);

		// Run both the cleanup and backup async.
		townyUniverse.performCleanupAndBackup();
	}

	public void loadConfig(boolean reload) {
		TownySettings.loadConfig(getDataFolder().toPath().resolve("settings").resolve("config.yml"), getVersion());
		if (reload) {
			// If Towny is in Safe Mode (for the main config) turn off Safe Mode and setup economy if it isn't already.
			if (removeError(TownyInitException.TownyError.MAIN_CONFIG) && TownySettings.isUsingEconomy() && !TownyEconomyHandler.isActive()) {
				PluginIntegrations.getInstance().setupAndPrintEconomy(TownySettings.isUsingEconomy());
			}
			TownyMessaging.sendMsg(Translatable.of("msg_reloaded_config"));
		}
	}
	
	public void loadLocalization(boolean reload) {
		Translation.loadTranslationRegistry();
		if (reload) {
			// If Towny is in Safe Mode (because of localization) turn off Safe Mode.
			removeError(TownyInitException.TownyError.LOCALIZATION);
			TownyMessaging.sendMsg(Translatable.of("msg_reloaded_lang"));
		}
	}

	private void loadDatabaseConfig(boolean reload) {
		if (!checkForLegacyDatabaseConfig()) {
			throw new TownyInitException("Unable to migrate old database settings to Towny\\data\\settings\\database.yml", TownyInitException.TownyError.DATABASE_CONFIG);
		}
		DatabaseConfig.loadDatabaseConfig(getDataFolder().toPath().resolve("settings").resolve("database.yml"));
		if (reload) {
			// If Towny is in Safe Mode (because of localization) turn off Safe Mode.
			removeError(TownyInitException.TownyError.DATABASE_CONFIG);
		}
	}
	
	public void loadPermissions(boolean reload) {
		TownyPerms.loadPerms(getDataFolder().toPath().resolve("settings").resolve("townyperms.yml"));
		// This will only run if permissions is fine.
		if (reload) {
			// If Towny is in Safe Mode (for Permissions) turn off Safe Mode.
			removeError(TownyInitException.TownyError.PERMISSIONS);
			// Update everyone who is online with the changes made.
			TownyPerms.updateOnlinePerms();
		}
	}

	/**
	 * Loads the Town and Nation Levels from the config.yml
	 *
	 * @throws TownyInitException if a TownyException occurs while loading the levels
	 */
	private void loadTownAndNationLevels() throws TownyInitException {
		// Load Nation & Town level data into maps.
		try {
			TownySettings.loadTownLevelConfig();
		} catch (TownyException e) {
			throw new TownyInitException("Failed to load town level config", TownyInitException.TownyError.MAIN_CONFIG, e);
		}
		try {
			TownySettings.loadNationLevelConfig();
		} catch (TownyException e) {
			throw new TownyInitException("Failed to load nation level config", TownyInitException.TownyError.MAIN_CONFIG, e);
		}
	}

	/**
	 * Handle any legacy config settings before we load the config and database.
	 */
	private void handleLegacyConfigs() {
		Path configPath = Towny.getPlugin().getDataFolder().toPath().resolve("settings").resolve("config.yml");
		if (!Files.exists(configPath))
			return;

		CommentedConfiguration config = new CommentedConfiguration(configPath);
		if (!config.load() || config.getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), "0.0.0.0").equals(getVersion()))
			return;

		// Old configs stored various TownBlock settings throughout the config.
		// This will migrate the old settings into the TownBlockType config section.
		// Since 0.97.5.4.
		TownBlockTypeHandler.Migrator.checkForLegacyOptions(config);
		
		ConfigMigrator earlyMigrator = new ConfigMigrator(config, "config-migration.json", true);
		earlyMigrator.migrate();
	}
	
	/**
	 * Converts the older config.yml's database settings into the database.yml file.
	 * @return true if successful
	 * @since 0.97.0.24
	 */
	private boolean checkForLegacyDatabaseConfig() {
		Path configYMLPath = getDataFolder().toPath().resolve("settings").resolve("config.yml");
		// Bail if the config doesn't exist at all yet.
		if (!Files.exists(configYMLPath))
			return true;

		CommentedConfiguration config = new CommentedConfiguration(configYMLPath);
		// return false if the config cannot be loaded.
		if (!config.load())
			return false;
		if (config.contains("plugin.database.database_load")) {
			/*
			 * Get old settings from config.
			 */
			String dbload = config.getString("plugin.database.database_load");
			String dbsave = config.getString("plugin.database.database_save");
			String hostname = config.getString("plugin.database.sql.hostname");
			String port = config.getString("plugin.database.sql.port");
			String dbname = config.getString("plugin.database.sql.dbname");
			String tableprefix = config.getString("plugin.database.sql.table_prefix");
			String username = config.getString("plugin.database.sql.username");
			String password = config.getString("plugin.database.sql.password");
			String flags = config.getString("plugin.database.sql.flags");
			String max_pool = config.getString("plugin.database.sql.pooling.max_pool_size");
			String max_lifetime = config.getString("plugin.database.sql.pooling.max_lifetime");
			String connection_timeout = config.getString("plugin.database.sql.pooling.connection_timeout");

			/*
			 * Create database.yml if it doesn't exist yet, with new settings.
			 */
			Path databaseYMLPath = getDataFolder().toPath().resolve("settings").resolve("database.yml");
			if (FileMgmt.checkOrCreateFile(databaseYMLPath.toString())) {
				CommentedConfiguration databaseConfig = new CommentedConfiguration(databaseYMLPath);
				databaseConfig.set("database.database_load", dbload);
				databaseConfig.set("database.database_save", dbsave);
				databaseConfig.set("database.sql.hostname", hostname);
				databaseConfig.set("database.sql.port", port);
				databaseConfig.set("database.sql.dbname", dbname);
				databaseConfig.set("database.sql.table_prefix", tableprefix);
				databaseConfig.set("database.sql.username", username);
				databaseConfig.set("database.sql.password", password);
				databaseConfig.set("database.sql.flags", flags);
				databaseConfig.set("database.sql.pooling.max_pool_size", max_pool);
				databaseConfig.set("database.sql.pooling.max_lifetime", max_lifetime);
				databaseConfig.set("database.sql.pooling.connection_timeout", connection_timeout);
				databaseConfig.save();
				getLogger().info("Database settings migrated to towny\\data\\settings\\database.yml");
			} else {
				getLogger().severe("Unable to migrate old database settings to towny\\data\\settings\\database.yml");
				return false;
			}
		}
		return true;
	}

	@Override
	public void onDisable() {

		Bukkit.getLogger().info("==============================================================");

		// Turn off timers.		
		toggleTimersOff();

		TownyRegenAPI.cancelProtectionRegenTasks();
		ChunkNotificationUtil.cancelChunkNotificationTasks();

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getDataSource() != null && !isError(TownyInitException.TownyError.DATABASE)) {
			townyUniverse.getDataSource().saveQueues();
			townyUniverse.getDataSource().saveCooldowns();

			// Shut down our saving task.
			plugin.getLogger().info("Finishing File IO Tasks...");
			townyUniverse.getDataSource().finishTasks();
		}

		playerCache.clear();

		plugin.getLogger().info("Finishing Universe Tasks...");
		townyUniverse.finishTasks();

		PluginIntegrations.getInstance().disable3rdPartyPluginIntegrations();

		this.townyUniverse = null;

		// Used to be required, but in the latest versions the server will cancel these tasks for us as well.
		if (this.scheduler instanceof FoliaTaskScheduler foliaScheduler)
			foliaScheduler.cancelTasks();

		plugin.getLogger().info("Version: " + version + " - Plugin Disabled");
		Bukkit.getLogger().info("=============================================================");
	}

	private void cycleTimers() {

		toggleTimersOff();
		TownyTimerHandler.toggleTownyRepeatingTimer(true);
		TownyTimerHandler.toggleDailyTimer(true);
		TownyTimerHandler.toggleHourlyTimer(true);
		TownyTimerHandler.toggleShortTimer(true);
		TownyTimerHandler.toggleMobRemoval(true);
		TownyTimerHandler.toggleHealthRegen(TownySettings.hasHealthRegen());
		TownyTimerHandler.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0);
		TownyTimerHandler.toggleCooldownTimer(true);
		TownyTimerHandler.toggleDrawSmokeTask(true);
		TownyTimerHandler.toggleDrawSpointsTask(TownySettings.getVisualizedSpawnPointsEnabled());
	}
	
	private void toggleTimersOff() {

		TownyTimerHandler.toggleTownyRepeatingTimer(false);
		TownyTimerHandler.toggleDailyTimer(false);
		TownyTimerHandler.toggleHourlyTimer(false);
		TownyTimerHandler.toggleShortTimer(false);
		TownyTimerHandler.toggleMobRemoval(false);
		TownyTimerHandler.toggleHealthRegen(false);
		TownyTimerHandler.toggleTeleportWarmup(false);
		TownyTimerHandler.toggleCooldownTimer(false);
		TownyTimerHandler.toggleDrawSmokeTask(false);
		TownyTimerHandler.toggleDrawSpointsTask(false);
	}

	private void registerEvents() {

		final PluginManager pluginManager = getServer().getPluginManager();

		// Huds
		pluginManager.registerEvents(new HUDManager(this), this);

		// Manage player deaths and death payments
		pluginManager.registerEvents(new TownyEntityMonitorListener(this), this);
		pluginManager.registerEvents(new TownyVehicleListener(this), this);
		pluginManager.registerEvents(new TownyServerListener(this), this);
		pluginManager.registerEvents(new TownyCustomListener(this), this);
		pluginManager.registerEvents(new TownyWorldListener(this), this);
		pluginManager.registerEvents(new TownyLoginListener(), this);
		

		// Always register these events.
		pluginManager.registerEvents(new TownyPlayerListener(this), this);
		pluginManager.registerEvents(new TownyBlockListener(this), this);
		pluginManager.registerEvents(new TownyEntityListener(this), this);
		pluginManager.registerEvents(new TownyInventoryListener(this), this);

		new TownyPaperEvents(this).register();
	}

	private void printChangelogToConsole() {

		try (InputStream is = JavaUtil.readResource("/ChangeLog.txt")) {
			String lastVersion = Version.fromString(TownySettings.getLastRunVersion()).toString(); // Parse out any trailing text after the *.*.*.* version, ie "-for-1.12.2".
			ChangelogReader reader = ChangelogReader.reader(lastVersion, is, 100);
			ChangelogResult result = reader.read();
			
			if (!result.successful()) {
				plugin.getLogger().warning("Could not find starting index for the changelog.");
				return;
			}
			
			plugin.getLogger().info("------------------------------------");
			plugin.getLogger().info("ChangeLog since v" + lastVersion + ":");
			
			for (String line : result.lines()) {
				if (line.trim().replaceAll("\t", "").isEmpty())
					continue;
				
				// We sadly don't have a logger capable of logging components, so we have to resort to sending it to the console logger for it to be coloured.
				Bukkit.getConsoleSender().sendMessage(line.trim().startsWith("-") ? line : Colors.Yellow + line);
			}
			
			if (result.limitReached()) {
				plugin.getLogger().info("<snip>");
				plugin.getLogger().info("Changelog continues for another " + (result.totalSize() - (result.nextVersionIndex() + 99)) + " lines.");
				plugin.getLogger().info("To read the full changelog since " + lastVersion + ", go to https://github.com/TownyAdvanced/Towny/blob/master/Towny/src/main/resources/ChangeLog.txt#L" + (result.nextVersionIndex() + 1));
			}
			
			plugin.getLogger().info("------------------------------------");
		} catch (IOException e) {
			plugin.getLogger().log(Level.WARNING, "Could not read ChangeLog.txt", e);
		}
	}

	public String getVersion() {

		return version;
	}

	/**
	 * @return the error
	 */
	public boolean isError() {
		return !errors.isEmpty();
	}
	
	@ApiStatus.Internal
	public boolean isError(@NotNull TownyInitException.TownyError error) {
		return errors.contains(error);
	}
	
	@ApiStatus.Internal
	public void addError(@NotNull TownyInitException.TownyError error) {
		errors.add(error);
	}
	
	@ApiStatus.Internal
	public boolean removeError(@NotNull TownyInitException.TownyError error) {
		return errors.remove(error);
	}

	@ApiStatus.Internal
	@NotNull
	public Collection<TownyInitException.TownyError> getErrors() {
		return errors;
	}

	public boolean hasCache(Player player) {

		return playerCache.containsKey(player.getUniqueId());
	}

	private PlayerCache newCache(Player player) {

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());
		if (world == null) {
			TownyMessaging.sendErrorMsg(player, "Could not create permission cache for unregistered world (" + player.getWorld().getName() + ").");
			return null;
		}

        return new PlayerCache(player);
	}

	public void deleteCache(Resident resident) {
		final Player player = resident.getPlayer();
		if (player != null)
			deleteCache(player);
	}

	public void deleteCache(Player player) {

		deleteCache(player.getUniqueId());
	}

	public void deleteCache(UUID uuid) {

		playerCache.remove(uuid);
	}

	/**
	 * Fetch the current players cache
	 * Creates a new one, if one doesn't exist.
	 * 
	 * @param player - Player to get the current cache from.
	 * @return the current (or new) cache for this player.
	 */
	public PlayerCache getCache(Player player) {
		return playerCache.computeIfAbsent(player.getUniqueId(), k -> newCache(player));
	}

	/**
	 * @param uuid - UUID of the player to get the cache for.
	 * @return The cache, or <code>null</code> if it doesn't exist.
	 */
	public PlayerCache getCacheOrNull(@NotNull UUID uuid) {
		return playerCache.get(uuid);
	}

	/**
	 * Resets all Online player caches, retaining their location info.
	 */
	public void resetCache() {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				getCache(player).resetAndUpdate(WorldCoord.parseWorldCoord(player)); // Automatically resets permissions.
	}

	/**
	 * Resets all Online player caches if their location equals this one
	 * 
	 * @param worldCoord - the location to check for
	 */
	public void updateCache(WorldCoord worldCoord) {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (WorldCoord.parseWorldCoord(player).equals(worldCoord))
					getCache(player).resetAndUpdate(worldCoord); // Automatically resets permissions.
	}

	/**
	 * Resets all Online player caches if their location has changed
	 */
	public void updateCache() {

		WorldCoord worldCoord = null;

		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				worldCoord = WorldCoord.parseWorldCoord(player);
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

		WorldCoord worldCoord = WorldCoord.parseWorldCoord(player);
		PlayerCache cache = getCache(player);

		if (!cache.getLastTownBlock().equals(worldCoord))
			cache.resetAndUpdate(worldCoord);
	}

	/**
	 * Resets a specific players cache
	 * 
	 * @param player - Player, whose cache is to be reset.
	 */
	public void resetCache(Player player) {

		getCache(player).resetAndUpdate(WorldCoord.parseWorldCoord(player));
	}

	/**
	 * @deprecated since 0.100.4.6, use {@link ResidentModeHandler#toggleModes(Player, String[], boolean)} instead.
	 * @param player Player to act upon.
	 * @param modes String[] of mode names to toggle.
	 * @param notify whether to notify the player of their modes afterwards.
	 */
	@Deprecated
	public void setPlayerMode(Player player, String[] modes, boolean notify) {

		if (player == null)
			return;

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident == null)
			return;

		ResidentModeHandler.toggleModes(resident, modes, notify, false);
	}

	/**
	 * Remove ALL current modes (and set the defaults)
	 * 
	 * @deprecated since 0.100.4.6, use {@link ResidentModeHandler#clearModes(Player)} instead.
	 * @param player Player, whose modes are to be reset (all removed).
	 */
	@Deprecated
	public void removePlayerMode(Player player) {

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident != null)
			ResidentModeHandler.clearModes(resident, false);
	}

	/**
	 * Remove ALL current modes.
	 * 
	 * @deprecated since 0.100.4.6, use {@link ResidentModeHandler#clearModes(Player)} instead.
	 * @param player - player, whose modes are to be reset (all removed).
	 */
	@Deprecated
	public void removePlayerModes(Player player) {

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		ResidentModeHandler.clearModes(resident, false);
	}
	
	/**
	 * Fetch a list of all the players current modes.
	 * 
	 * @deprecated since 0.100.4.6, use {@link ResidentModeHandler#getModes(Player)} instead.
	 * @param player - player, whose modes are to be listed, taken.
	 * @return list of modes
	 */
	@Deprecated
	public List<String> getPlayerMode(Player player) {

		return getPlayerMode(player.getName());
	}

	public List<String> getPlayerMode(String name) {

		Resident resident = TownyUniverse.getInstance().getResident(name);
		return resident != null ? resident.getModes() : null;
	}

	/**
	 * Check if the player has a specific mode.
	 * 
	 * @param player - Player to be checked
	 * @param mode - Mode to be checked for within player.
	 * @return true if the mode is present.
	 */
	public boolean hasPlayerMode(Player player, String mode) {

		return hasPlayerMode(player.getUniqueId(), mode);
	}
	
	public boolean hasPlayerMode(UUID uuid, String mode) {
		Resident resident = TownyUniverse.getInstance().getResident(uuid);
		
		return resident != null && resident.hasMode(mode);
	}

	public boolean hasPlayerMode(String name, String mode) {
		
		Resident resident = TownyUniverse.getInstance().getResident(name);
		return resident != null && resident.hasMode(mode);
	}

	public String getConfigPath() {

		return getDataFolder().getPath() + File.separator + "settings" + File.separator + "config.yml";
	}

	public Object getSetting(String root) {

		return TownySettings.getProperty(root);
	}

	/**
	 * @return the Towny instance
	 */
	@NotNull
	public static Towny getPlugin() {
		if (plugin == null)
			throw new IllegalStateException("Attempted to use getPlugin() while the plugin is null, are you shading Towny? If you do not understand this message, join the Towny discord using https://discord.com/invite/gnpVs5m and ask for support.");

		return plugin;
	}

	private void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>(4);
		commands.add(new AcceptCommand(this, TownySettings.getAcceptCommand()));
		commands.add(new DenyCommand(this, TownySettings.getDenyCommand()));
		commands.add(new ConfirmCommand(this, TownySettings.getConfirmCommand()));
		commands.add(new CancelCommand(this, TownySettings.getCancelCommand()));

		getServer().getCommandMap().registerAll("towny", commands);
	}
	
	private void registerCommands() {
		getCommand("townyadmin").setExecutor(new TownyAdminCommand(this));
		getCommand("townyworld").setExecutor(new TownyWorldCommand(this));
		getCommand("resident").setExecutor(new ResidentCommand(this));
		getCommand("towny").setExecutor(new TownyCommand(this));

		CommandExecutor townCommandExecutor = new TownCommand(this);
		getCommand("town").setExecutor(townCommandExecutor);
		
		// This is needed because the vanilla "/t" tab completer needs to be overridden.
		getCommand("t").setTabCompleter((TabCompleter)townCommandExecutor);
		
		getCommand("nation").setExecutor(new NationCommand(this));
		getCommand("plot").setExecutor(new PlotCommand(this));
		getCommand("invite").setExecutor(new InviteCommand(this));
	}

	private void addMetricsCharts() {
		/*
		 * Register bStats Metrics
		 */
		Metrics metrics = new Metrics(this, 2244);
		
		metrics.addCustomChart(new SimplePie("language", () -> TownySettings.getString(ConfigNodes.LANGUAGE)));
		
		// Determine the server name/brand, i.e. "Paper"
		String serverBrand = getServer().getName();
		try {
			serverBrand = ServerBuildInfo.buildInfo().brandName();
		} catch (NoClassDefFoundError ignored) {}

		final String finalBrand = serverBrand;
		metrics.addCustomChart(new SimplePie("server_type", () -> finalBrand));

		metrics.addCustomChart(new SimplePie("nation_zones_enabled", () -> TownySettings.getNationZonesEnabled() ? "true" : "false"));
		
		metrics.addCustomChart(new SimplePie("database_type", () -> TownySettings.getSaveDatabase().toLowerCase(Locale.ROOT)));
		
		metrics.addCustomChart(new SimplePie("town_block_size", () -> String.valueOf(TownySettings.getTownBlockSize())));
		
		metrics.addCustomChart(new SimplePie("closed_economy_enabled", () -> String.valueOf(TownySettings.isEcoClosedEconomyEnabled())));
	}

	/**
	 * Check if the version of Towny is newer than or equal to the supplied version.
	 * Used by other plugins to determine if Towny is of the required version level.
	 *
	 * @param version String version ie: "0.99.1.0"
	 * @return true if Towny's version is sufficient.
	 * @since 0.99.0.10.
	 */
	public static boolean isTownyVersionSupported(String version) {
		return Version.fromString(Towny.getPlugin().getVersion()).isNewerThanOrEquals(Version.fromString(version)); 
	}

	public static boolean isMinecraftVersionStillSupported() {
		return MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.OLDEST_VERSION_SUPPORTED);
	}

	@ApiStatus.Internal
	public boolean isFolia() {
		return this.isFolia;
	}
	
	private boolean expandedSchedulingAvailable() {
		return JavaUtil.classExists("io.papermc.paper.threadedregions.scheduler.FoliaAsyncScheduler");
	}

	@NotNull
	public TaskScheduler getScheduler() {
		return this.scheduler;
	}

	/**
	 * @deprecated since 0.100.2.13, unused by Towny for many years. Use {@link BukkitTools#getWorld(String)} instead.
	 * @param name Name of the World.
	 * @return a World
	 * @throws NotRegisteredException if the world doesn't exist.
	 */
	@Deprecated
	public World getServerWorld(String name) throws NotRegisteredException {
		World world = BukkitTools.getWorld(name);
		
		if (world == null)
			throw new NotRegisteredException(String.format("A world called '$%s' has not been registered.", name));
		
		return world;
	}
}
