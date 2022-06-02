 package com.palmergames.bukkit.towny;

import com.earth2me.essentials.Essentials;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.config.migration.ConfigMigrator;
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
import com.palmergames.bukkit.towny.db.DatabaseConfig;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.hooks.LuckPermsContexts;
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
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.bukkit.towny.permissions.BukkitPermSource;
import com.palmergames.bukkit.towny.permissions.GroupManagerSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.permissions.VaultPermSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.permission.Permission;

import org.apache.commons.lang.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Towny Plugin for Bukkit
 * 
 * Website &amp; Source: https://github.com/TownyAdvanced/Towny
 * 
 * @author Shade, ElgarL, LlmDl
 */
public class Towny extends JavaPlugin {
	private static final Version OLDEST_MC_VER_SUPPORTED = Version.fromString("1.16");
	private static final Version CUR_BUKKIT_VER = Version.fromString(Bukkit.getBukkitVersion());
	private final String version = this.getDescription().getVersion();

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyVehicleListener vehicleListener = new TownyVehicleListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyCustomListener customListener = new TownyCustomListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private final TownyServerListener serverListener = new TownyServerListener(this);
	private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
	private final TownyWorldListener worldListener = new TownyWorldListener(this);
	private final TownyInventoryListener inventoryListener = new TownyInventoryListener();
	private final TownyLoginListener loginListener = new TownyLoginListener();
	private final HUDManager HUDManager = new HUDManager(this);
	private final TownyPaperEvents paperEvents = new TownyPaperEvents(this);
	private LuckPermsContexts luckPermsContexts;

	private TownyUniverse townyUniverse;

	private final Map<UUID, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<>());

	private Essentials essentials = null;
	private boolean citizens2 = false;
	
	private final List<TownyInitException.TownyError> errors = new ArrayList<>();
	
	private static Towny plugin;

	private static BukkitAudiences adventure;
	
	public Towny() {
		
		plugin = this;
	}

	@Override
	public void onEnable() {

		Bukkit.getLogger().info("====================      Towny      ========================");

		townyUniverse = TownyUniverse.getInstance();
		
		// Setup static classes
		BukkitTools.initialize(this);
		TownyTimerHandler.initialize(this);
		TownyEconomyHandler.initialize(this);
		TownyFormatter.initialize();
		PlayerCacheUtil.initialize(this);
		TownyPerms.initialize(this);
		InviteHandler.initialize(this);

		try {
			// Load the foundation of Towny, containing config, locales, database.
			loadFoundation(false);

			// Check for plugins that we use or we develop.
			// N.B. Includes the hook for TownyChat
			checkPlugins();
			// Make sure the timers are stopped for a reset, then started.
			cycleTimers();
			// Reset the player cache.
			resetCache();
			// Check for plugin updates if the Minecraft version is still supported.
			if (isMinecraftVersionStillSupported())
				TownyUpdateChecker.checkForUpdates(this);
			// Initialize SpawnUtil only after the Translation class has figured out a language.
			// N.B. Important that localization loaded correctly for this step.
			SpawnUtil.initialize(this);
			// Setup bukkit command interfaces
			registerSpecialCommands();
			registerCommands();
			// Add custom metrics charts.
			addMetricsCharts();
		} catch (TownyInitException tie) {
			addError(tie.getError());
			getLogger().log(Level.SEVERE, tie.getMessage(), tie);
		}
		
		// NOTE: Runs regardless if Towny errors out!
		// Important for safe mode.

		adventure = BukkitAudiences.create(this);

		// If we aren't going to enter safe mode, do the following:
		if (!isError() &&TownySettings.isTownyUpdating(getVersion())) {

			printChangelogToConsole();
			// Update config with new version.
			TownySettings.setLastRunVersion(getVersion());
			// Save database.
			townyUniverse.getDataSource().saveAll();
			// cleanup() updates SQL schema for any changes.
			townyUniverse.getDataSource().cleanup();
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

		// Initialize the type handler after the config is loaded and before the database is.
		TownBlockTypeHandler.initialize();

		// Initialize the special log4j hook logger.
		TownyLogger.getInstance();

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

		// Run both the cleanup and backup async.
		townyUniverse.performCleanupAndBackup();
	}

	private void loadConfig(boolean reload) {
		TownySettings.loadConfig(getDataFolder().toPath().resolve("settings").resolve("config.yml"), getVersion());
		if (reload) {
			// If Towny is in Safe Mode (for the main config) turn off Safe Mode.
			if (isError(TownyInitException.TownyError.MAIN_CONFIG)) {
				removeError(TownyInitException.TownyError.MAIN_CONFIG);
			}
			TownyMessaging.sendMsg(Translatable.of("msg_reloaded_config"));
		}
	}
	
	private void loadLocalization(boolean reload) {
		Translation.loadTranslationRegistry();
		if (reload) {
			// If Towny is in Safe Mode (because of localization) turn off Safe Mode.
			if (isError(TownyInitException.TownyError.LOCALIZATION)) {
				removeError(TownyInitException.TownyError.LOCALIZATION);
			}
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
			if (isError(TownyInitException.TownyError.DATABASE_CONFIG)) {
				removeError(TownyInitException.TownyError.DATABASE_CONFIG);
			}
		}
	}
	
	public void loadPermissions(boolean reload) {
		TownyPerms.loadPerms(getDataFolder().toPath().resolve("settings").resolve("townyperms.yml"));
		// This will only run if permissions is fine.
		if (reload) {
			// If Towny is in Safe Mode (for Permissions) turn off Safe Mode.
			if (isError(TownyInitException.TownyError.PERMISSIONS)) {
				removeError(TownyInitException.TownyError.PERMISSIONS);
			}
			// Update everyone who is online with the changes made.
			TownyPerms.updateOnlinePerms();
		}
	}

	/**
	 * Loads the Town and Nation Levels from the config.yml
	 *
	 * @return true if they have the required elements.
	 */
	private void loadTownAndNationLevels() {
		// Load Nation & Town level data into maps.
		try {
			TownySettings.loadTownLevelConfig();
		} catch (IOException e) {
			throw new TownyInitException("Failed to load town level config", TownyInitException.TownyError.MAIN_CONFIG);
		}
		try {
			TownySettings.loadNationLevelConfig();
		} catch (IOException e) {
			throw new TownyInitException("Failed to load nation level config", TownyInitException.TownyError.MAIN_CONFIG);
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
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (townyUniverse.getDataSource() != null && !isError()) {
			townyUniverse.getDataSource().saveQueues();
		}

		// Turn off timers.		
		toggleTimersOff();

		TownyRegenAPI.cancelProtectionRegenTasks();

		playerCache.clear();
		
		try {
			// Shut down our saving task.
			plugin.getLogger().info("Finishing File IO Tasks...");
			townyUniverse.getDataSource().finishTasks();
			townyUniverse.finishTasks();
		} catch (NullPointerException ignored) {
			// The saving task will not have started if this disable was fired by onEnable failing.			
		}

		if (adventure != null) {
			adventure.close();
			adventure = null;
		}
		
		if (luckPermsContexts != null) {
			luckPermsContexts.unregisterContexts();
			luckPermsContexts = null;
		}

		this.townyUniverse = null;

		plugin.getLogger().info("Version: " + version + " - Plugin Disabled");
		Bukkit.getLogger().info("=============================================================");
	}
	
	private void checkPlugins() {
		
		plugin.getLogger().info("Searching for third-party plugins...");
		String ecowarn = "";
		List<String> addons = new ArrayList<>();
		Plugin test;

	 	//Check for permission source.
		String permissions = returnPermissionsProviders();
		String economy = "";
		/*
		 * Check for economy source.
		 */
		if (TownySettings.isUsingEconomy()) {			
			if (TownyEconomyHandler.setupEconomy()) {
				economy = "  Economy: " + TownyEconomyHandler.getVersion();				
				if (TownyEconomyHandler.isEssentials())
					ecowarn = "Warning: EssentialsX Economy has been known to reset town and nation bank accounts on rare occasions.";
				
		        File f = new File(TownyUniverse.getInstance().getRootFolder(), "debtAccountsConverted.txt");                   // For a short time Towny stored debt accounts in the server's
		        if (!f.exists())                                                                                               // economy plugin. This practice had to end, being replaced 
		        	Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> MoneyUtil.convertLegacyDebtAccounts(), 600l); // with the debtBalance which is stored in the Town object.
					
			} else {
				ecowarn = "Warning: No compatible Economy plugins found. Install Vault.jar or Reserve.jar with any of the supported eco systems. If you do not want an economy to be used, set using_economy: false in your Towny config.yml.";
			}
		}
		
		/*
		 * Check add-ons and third-party plugins we use.
		 */
		// LlmDl Sponsor exclusive
		test = getServer().getPluginManager().getPlugin("TownyCamps");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyCamps", test.getDescription().getVersion()));
		}
		
		test = getServer().getPluginManager().getPlugin("TownyChat");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyChat", test.getDescription().getVersion()));			
		}
		
		test = getServer().getPluginManager().getPlugin("TownyCultures");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyCultures", test.getDescription().getVersion()));
		}

		test = getServer().getPluginManager().getPlugin("TownyFlight");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyFlight", test.getDescription().getVersion()));			
		}

		// LlmDl Sponsor exclusive
		test = getServer().getPluginManager().getPlugin("TownyHistories");
		if (test != null) {
			addons.add(String.format("%s v%s", "TownyHistories", test.getDescription().getVersion()));
		}
		
		test = getServer().getPluginManager().getPlugin("SiegeWar");
		if (test != null) {
			addons.add(String.format("%s v%s", "SiegeWar", test.getDescription().getVersion()));
		}
		
		test = getServer().getPluginManager().getPlugin("FlagWar");
		if (test != null) {
			addons.add(String.format("%s v%s", "FlagWar", test.getDescription().getVersion()));
		}
		
		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null) {
			TownySettings.setUsingEssentials(false);
		} else if (TownySettings.isUsingEssentials()) {
			this.essentials = (Essentials) test;
			addons.add(String.format("%s v%s", "Essentials", test.getDescription().getVersion()));
		}

		test = getServer().getPluginManager().getPlugin("PlaceholderAPI");
		if (test != null) {
            new TownyPlaceholderExpansion(this).register();
            addons.add(String.format("%s v%s", "PlaceholderAPI", test.getDescription().getVersion()));
		}
		
		test = getServer().getPluginManager().getPlugin("LuckPerms");
		if (test != null && TownySettings.isContextsEnabled()) {
			this.luckPermsContexts = new LuckPermsContexts(this);
			luckPermsContexts.registerContexts();
			addons.add(String.format("%s v%s", "LuckPerms", test.getDescription().getVersion()));
		}

		//Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}
		
		/*
		 * Test for Citizens2 so we can avoid removing their NPC's
		 */
		setCitizens2(getServer().getPluginManager().isPluginEnabled("Citizens"));

		/*
		 * Output discovered plugins and warnings.
		 */
		plugin.getLogger().info("Plugins found: ");
		plugin.getLogger().info(permissions);
		if (!economy.isEmpty())
			plugin.getLogger().info(economy);
		if (!addons.isEmpty())
			plugin.getLogger().info("  Add-ons: " + WordUtils.wrap(StringMgmt.join(addons, ", "), 52, System.lineSeparator() + "                           ", true));
		if (!ecowarn.isEmpty())
			plugin.getLogger().info(WordUtils.wrap(ecowarn, 55, System.lineSeparator() + "                           ", true));

		//Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}

		//Legacy check to see if questioner.jar is still present.
		test = getServer().getPluginManager().getPlugin("Questioner");
		if (test != null) {
			String questioner = "Warning: Questioner.jar present on server, Towny no longer requires Questioner for invites/confirmations. You may safely remove Questioner.jar from your plugins folder.";
			plugin.getLogger().info(WordUtils.wrap(questioner, 55, System.lineSeparator() + "                           ", true));
		}
	}
	
	private String returnPermissionsProviders() {
		// TownyPerms is always present.
		String output = "  Permissions: TownyPerms, ";
		
		// Test for GroupManager being present.
		Plugin test = getServer().getPluginManager().getPlugin("GroupManager");
		if (test != null) {
			TownyUniverse.getInstance().setPermissionSource(new GroupManagerSource(this, test));
			output += String.format("%s v%s", "GroupManager", test.getDescription().getVersion());
		// Else test for vault being present.
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
					TownyUniverse.getInstance().setPermissionSource(new VaultPermSource(this, chat));
					RegisteredServiceProvider<Permission> vaultPermProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
					if (vaultPermProvider != null) {
						output += vaultPermProvider.getPlugin().getName() + " " + vaultPermProvider.getPlugin().getDescription().getVersion() + " via Vault";
					} else {
						output += String.format("%s v%s", "Vault", test.getDescription().getVersion());
					}
				}
			}

			if (test == null) {
				TownyUniverse.getInstance().setPermissionSource(new BukkitPermSource(this));
				output += "BukkitPermissions";
			}
		}
		return output;		
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

		if (!isError()) {			
			// Huds
			pluginManager.registerEvents(HUDManager, this);

			// Manage player deaths and death payments
			pluginManager.registerEvents(entityMonitorListener, this);
			pluginManager.registerEvents(vehicleListener, this);
			pluginManager.registerEvents(serverListener, this);
			pluginManager.registerEvents(customListener, this);
			pluginManager.registerEvents(worldListener, this);
			pluginManager.registerEvents(loginListener, this);
		}

		// Always register these events.
		pluginManager.registerEvents(playerListener, this);
		pluginManager.registerEvents(blockListener, this);
		pluginManager.registerEvents(entityListener, this);
		pluginManager.registerEvents(inventoryListener, this);

		paperEvents.register();
	}

	private void printChangelogToConsole() {

		try {
			List<String> changeLog = JavaUtil.readTextFromJar("/ChangeLog.txt");
			int startingIndex = 0;
			int linesDisplayed = 0;
			String lastVersion = Version.fromString(TownySettings.getLastRunVersion()).toString(); // Parse out any trailing text after the *.*.*.* version, ie "-for-1.12.2".
			plugin.getLogger().info("------------------------------------");
			plugin.getLogger().info("ChangeLog since v" + lastVersion + ":");
			
			// Go backwards through the changelog to get to the last run version.
			for (int i = changeLog.size() - 1; i >= 0; i--) {
				if (changeLog.get(i).startsWith(lastVersion)) {
					// Go forwards through the changelog to find the next version after the last run version.
					for (int j = i + 1; j < changeLog.size(); j++) {
						if (!changeLog.get(j).trim().startsWith("-")) {
							startingIndex = j;
							break;
						}
					}
					break;
				}
			}
			
			if (startingIndex != 0) {
				for (int i = startingIndex; i < changeLog.size(); i++) {
					if (linesDisplayed > 100) {
						plugin.getLogger().warning("<snip>");
						plugin.getLogger().warning("Changelog continues for another " + (changeLog.size() - (startingIndex + 99)) + " lines.");
						plugin.getLogger().warning("To read the full changelog since " + lastVersion + ", go to https://github.com/TownyAdvanced/Towny/blob/master/resources/ChangeLog.txt#L" + ++startingIndex);
						break;
					} 
					String line = changeLog.get(i);
					if (line.replaceAll(" ", "").replaceAll("\t", "").length() > 0) {
						Bukkit.getLogger().info(line.trim().startsWith("-") ? line : "\u001B[33m" + line);
						++linesDisplayed;
					}
				}
			} else {
				plugin.getLogger().warning("Could not find starting index for the changelog.");	
			}
			plugin.getLogger().info("------------------------------------");
		} catch (IOException e) {
			plugin.getLogger().warning("Could not read ChangeLog.txt");
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
	
	private boolean isError(@NotNull TownyInitException.TownyError error) {
		return errors.contains(error);
	}
	
	public void addError(@NotNull TownyInitException.TownyError error) {
		errors.add(error);
	}
	
	private void removeError(@NotNull TownyInitException.TownyError error) {
		errors.remove(error);
	}

	@NotNull
	public List<TownyInitException.TownyError> getErrors() {
		return errors;
	}

	// is Essentials active
	public boolean isEssentials() {

		return (TownySettings.isUsingEssentials() && (this.essentials != null));
	}

	// is Citizens2 active
	public boolean isCitizens2() {

		return citizens2;
	}

	public void setCitizens2(boolean b) {

		citizens2 = b;
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
		World world = BukkitTools.getWorld(name);
		
		if (world == null)
			throw new NotRegisteredException(String.format("A world called '$%s' has not been registered.", name));
		
		return world;
	}

	public boolean hasCache(Player player) {

		return playerCache.containsKey(player.getUniqueId());
	}

	public PlayerCache newCache(Player player) {

		TownyWorld world = TownyUniverse.getInstance().getWorld(player.getWorld().getName());
		if (world == null) {
			TownyMessaging.sendErrorMsg(player, "Could not create permission cache for this world (" + player.getWorld().getName() + ".");
			return null;	
		}
		PlayerCache cache = new PlayerCache(world, player);
		playerCache.put(player.getUniqueId(), cache);
		return cache;

	}

	public void deleteCache(Resident resident) {
		if (!resident.isOnline())
			return;
		deleteCache(resident.getPlayer());
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

		PlayerCache cache = playerCache.get(player.getUniqueId());
		
		if (cache == null) {
			cache = newCache(player);
			
			if (cache != null)
				cache.setLastTownBlock(WorldCoord.parseWorldCoord(player));
		}

		return cache;
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

	public void setPlayerMode(Player player, String[] modes, boolean notify) {

		if (player == null)
			return;

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident != null)
			resident.setModes(modes, notify);
	}

	/**
	 * Remove ALL current modes (and set the defaults)
	 * 
	 * @param player - player, whose modes are to be reset (all removed).
	 */
	public void removePlayerMode(Player player) {

		Resident resident = TownyUniverse.getInstance().getResident(player.getName());
		if (resident != null)
			resident.clearModes();
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
	 * @return the HUDManager
	 */
	public HUDManager getHUDManager() {
		
		return HUDManager;
	}

	public static BukkitAudiences getAdventure() {
		return adventure;
	}

	// https://www.spigotmc.org/threads/small-easy-register-command-without-plugin-yml.38036/
	private void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>(4);
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
			throw new TownyInitException("An issue has occured while registering custom commands.", TownyInitException.TownyError.OTHER, e);
		}
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
		
		metrics.addCustomChart(new SimplePie("server_type", () -> {
			if (Bukkit.getServer().getName().equalsIgnoreCase("paper"))
				return "Paper";
			else if (Bukkit.getServer().getName().equalsIgnoreCase("craftbukkit")) {
				if (isSpigotOrDerivative())
					return "Spigot";
				else 
					return "CraftBukkit";
			}
			return "Unknown";
		}));

		metrics.addCustomChart(new SimplePie("nation_zones_enabled", () -> TownySettings.getNationZonesEnabled() ? "true" : "false"));
		
		metrics.addCustomChart(new SimplePie("database_type", () -> TownySettings.getSaveDatabase().toLowerCase()));
		
		metrics.addCustomChart(new SimplePie("town_block_size", () -> String.valueOf(TownySettings.getTownBlockSize())));
		
		metrics.addCustomChart(new SimplePie("closed_economy_enabled", () -> String.valueOf(TownySettings.isEcoClosedEconomyEnabled())));
		
		metrics.addCustomChart(new SimplePie("resident_uuids_stored", TownySettings::getUUIDPercent));
	}
	
	public static boolean isMinecraftVersionStillSupported() {
		return CUR_BUKKIT_VER.compareTo(OLDEST_MC_VER_SUPPORTED) >= 0;
	}
	
	/**
	 * @deprecated since 0.98.1.1. Towny will only support 1.16 and newer going forward.
	 */
	@Deprecated
	public static boolean is116Plus() {
		return true;
	}

	/**
	 * @return whether server is running spigot (and not CraftBukkit.)
	 */
	private static boolean isSpigotOrDerivative() {
		try {
			Class.forName("org.bukkit.entity.Player$Spigot");
			return true;
		} catch (ClassNotFoundException tr) {
			return false;
		}

	}

}
