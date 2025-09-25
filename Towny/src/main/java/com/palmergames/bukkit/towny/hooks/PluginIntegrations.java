package com.palmergames.bukkit.towny.hooks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.permissions.LuckPermsSource;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.chat.TNCRegister;
import com.palmergames.bukkit.towny.permissions.BukkitPermSource;
import com.palmergames.bukkit.towny.permissions.GroupManagerSource;
import com.palmergames.bukkit.towny.permissions.VaultPermSource;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.StringMgmt;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class PluginIntegrations {
	private static PluginIntegrations instance; 
	private final String[] TOWNYADVANCED_PLUGINS = { "TownyChat", "TownyFlight", "TownyCultures", "TownyResources",
			"TownyCombat", "FlagWar", "SiegeWar", "MapTowny", "Dynmap-Towny", "ChestShop-Towny", "mcMMO-Towny",
			"Towny-TNE", "WorldGuard-Towny", "TownyProvinces" };
	private final String[] SPONSOR_PLUGINS = { "EventWar", "SiegeConquest", "TownyCamps", "TownyHistories", "TownyRTP",
			"TownyWayPointTravel", "TownOptionalLWC", "TowerOfBable", "TownyCaptureSites" };
	private final String NEWLINE_STRING = System.lineSeparator() + "                           ";
	private final Map<String, Level> warnings = new LinkedHashMap<>();

	private TownyPlaceholderExpansion papiExpansion = null;
	private LuckPermsContexts luckPermsContexts;
	private NamespacedKey eliteKey;
	private static final Version POWERRANKS_FIXED_VERSION = Version.fromString("1.10.8");

	public static PluginIntegrations getInstance() {
		if (instance == null)
			instance = new PluginIntegrations();
		return instance;
	}

	public List<String> getTownyPluginsForUniverseCommand() {
		List<String> out = new ArrayList<>();
		for (String pluginName : TOWNYADVANCED_PLUGINS)
			formatForUniverseCommand(out, pluginName);
		for (String pluginName : SPONSOR_PLUGINS)
			formatForUniverseCommand(out, pluginName);
		return out;
	}

	private void formatForUniverseCommand(List<String> out, String pluginName) {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
		if (plugin != null)
			out.add(Colors.Yellow + pluginName + " " + Colors.Green + plugin.getPluginMeta().getVersion());
	}

	/**
	 * Find permission, economy and addon plugins, plugins which require warnings,
	 * print out information to the log.
	 * 
	 * @param towny Towny plugin instance.
	 */
	public void checkForPlugins(Towny towny) {
		towny.getLogger().info("Searching for third-party plugins...");
		towny.getLogger().info("Plugins found: ");

	 	// Check for permission source.
		detectAndPrintPermissions(towny);

		// Check for an economy plugin if the main config loaded correctly.
		if (!towny.isError(TownyInitException.TownyError.MAIN_CONFIG))
			setupAndPrintEconomy(TownySettings.isUsingEconomy());

		// Find supporting plugins and set them up if needed.
		findSetupAndPrintAddons(towny);

		// Potentially warn about other plugins which are present.
		printPluginWarnings();

		// Run the leakchecker.
		LeakChecker.checkForLeaks();
	}

	private void detectAndPrintPermissions(Towny towny) {
		for (String permissions : registerPermissionsProviders(towny).split("\n"))
			towny.getLogger().info(permissions);
	}

	public void setupAndPrintEconomy(boolean configSetForEconomy) {
		String ecowarn = "No compatible Economy plugins found."
				+ " Install Vault.jar or Reserve.jar with any of the supported eco systems."
				+ " If you do not want an economy to be used, set using_economy: false in your Towny config.yml.";

		// Check if the economy is enabled in the config and attempt to set it up.
		if (configSetForEconomy && TownyEconomyHandler.setupEconomy()) {
			ecowarn = ""; // Used to be useful, hopefully we don't need to use it again.
		}

		if (TownyEconomyHandler.isActive())
			Towny.getPlugin().getLogger().info("  Economy: " + TownyEconomyHandler.getVersion());
		else if (configSetForEconomy && (isPluginPresent("Vault") || isPluginPresent("Reserve")))
			ecowarn = "No compatible Economy plugins found. If you do not want an economy to be used, set using_economy: false in your Towny config.yml.";

		if (!ecowarn.isEmpty() && configSetForEconomy)
			warnings.put(ecowarn, Level.INFO);
	}

	private void findSetupAndPrintAddons(Towny towny) {
		// Get TownyAdvancedOrg and Sponsor plugins.
		List<String> addons = new ArrayList<>();
		for (String townyAdvancedAndSponsorPlugins : getTownyPluginsForStartup())
			addons.add(townyAdvancedAndSponsorPlugins);

		// Check for 3rd party plugins we hook into.
		if (isPluginPresent("PlaceholderAPI")) {
			enablePAPI(towny);
			formatForStartup(addons, "PlaceholderAPI");
		}

		if (isPluginPresent("LuckPerms") && TownySettings.isContextsEnabled()) {
			enableLuckPermsContexts(towny);
			formatForStartup(addons, "LuckPerms");
		}

		// Add our chat handler to TheNewChat via the API.
		if(Bukkit.getPluginManager().isPluginEnabled("TheNewChat")) {
			TNCRegister.initialize();
		}

		// Test for EliteMobs.
		Plugin eliteMobs = Bukkit.getServer().getPluginManager().getPlugin("EliteMobs");
		if (eliteMobs != null)
			eliteKey = new NamespacedKey(eliteMobs, "EliteEntity");

		if (!addons.isEmpty())
			towny.getLogger().info("  Add-ons: " + StringMgmt.wrap(String.join(", ", addons), 52, NEWLINE_STRING));
	}

	private void printPluginWarnings() {
		//Legacy check to see if questioner.jar is still present.
		if (isPluginPresent("Questioner"))
			warnings.put("Questioner.jar present on server, Towny no longer requires Questioner for invites/confirmations."
					+ " You may safely remove Questioner.jar from your plugins folder.", Level.WARNING);

		//Add warning about outdated PowerRanks.
		if (isPluginPresent("PowerRanks")) {
			Version version = Version.fromString(Bukkit.getPluginManager().getPlugin("PowerRanks").getPluginMeta().getVersion());
			if (version.isOlderThan(POWERRANKS_FIXED_VERSION))
				warnings.put("Your outdated PowerRanks is incompatible with Towny. PowerRanks will override Towny's ability to give permissions via the townyperms.yml file."
					+ " Update your PowerRanks to version 1.10.8 or newer!", Level.WARNING);
		}

		//Add warning about MyCommand which will have a /t alias for /time in it by default.
		if (isPluginPresent("MyCommand"))
			warnings.put("By default MyCommand has an alias for /time that uses /t stored in the mycommand/commands/examples.yml file."
					+ " If you have not removed it you can expect the /t command to not work properly for Towny!", Level.WARNING);

		if (!warnings.isEmpty()) {
			for (Map.Entry<String, Level> warning : warnings.entrySet())
				Towny.getPlugin().getLogger().log(warning.getValue(), StringMgmt.wrap("  Warning: " + warning.getKey(), 55, NEWLINE_STRING));

			warnings.clear();
		}
	}

	private List<String> getTownyPluginsForStartup() {
		List<String> out = new ArrayList<>();
		for (String pluginName : TOWNYADVANCED_PLUGINS)
			formatForStartup(out, pluginName);
		for (String pluginName : SPONSOR_PLUGINS)
			formatForStartup(out, pluginName);
		return out;
	}

	private void formatForStartup(List<String> out, String pluginName) {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
		if (plugin != null)
			out.add(pluginName + " v" + plugin.getPluginMeta().getVersion());
	}

	public String registerPermissionsProviders(Towny towny) {
		// TownyPerms is always present.
		String output = "  Permissions: TownyPerms, ";

		Plugin test = Bukkit.getServer().getPluginManager().getPlugin("LuckPerms");
		if (test != null) {
			TownyUniverse.getInstance().setPermissionSource(new LuckPermsSource(towny));
			return output + String.format("%s v%s", "LuckPerms", test.getPluginMeta().getVersion());
		}

		// Test for GroupManager being present.
		test = Bukkit.getServer().getPluginManager().getPlugin("GroupManager");
		if (test != null && JavaUtil.classExists("org.anjocaido.groupmanager.GroupManager")) {
			TownyUniverse.getInstance().setPermissionSource(new GroupManagerSource(towny, test));
			return output += String.format("%s v%s", "GroupManager", test.getPluginMeta().getVersion());
		}

		// Else test for vault being present.
		test = Bukkit.getServer().getPluginManager().getPlugin("Vault");
		if (test != null) {
			RegisteredServiceProvider<Chat> chatProvider = findChatImplementation();
			RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

			if (chatProvider == null) {
				// No Chat implementation
				test = null;
				// Fall back to BukkitPermissions below
			} else {
				TownyUniverse.getInstance().setPermissionSource(new VaultPermSource(towny, chatProvider.getProvider()));
				
				if (permissionProvider != null) {
					output += permissionProvider.getPlugin().getName() + " v" + permissionProvider.getPlugin().getPluginMeta().getVersion() + " via Vault";
				} else {
					output += String.format("Vault v%s", test.getPluginMeta().getVersion());
				}
				
				output += String.format("\n  Chat: %s v%s via Vault", chatProvider.getPlugin().getName(), chatProvider.getPlugin().getPluginMeta().getVersion());
				return output;
			}

		}
		// No Vault found, fall back to Bukkit's native permission source.
		TownyUniverse.getInstance().setPermissionSource(new BukkitPermSource(towny));
		return output += "BukkitPermissions";
	}
	
	@Nullable
	private RegisteredServiceProvider<Chat> findChatImplementation() {
		Iterator<RegisteredServiceProvider<Chat>> iterator = Bukkit.getServicesManager().getRegistrations(Chat.class).iterator();
		
		while (iterator.hasNext()) {
			RegisteredServiceProvider<Chat> chatProvider = iterator.next();
			
			if (chatProvider == null)
				continue;
			
			try {
				// If the 'perms' field in the chat implementation is null, log some warning messages.
				// The perms field being null causes issues with plot claiming, and is caused by a faulty chat implementation.
				Field field = Chat.class.getDeclaredField("perms");
				field.setAccessible(true);
				
				if (field.get(chatProvider.getProvider()) == null) {
					Towny.getPlugin().getLogger().warning(String.format("WARNING: Plugin %s v%s has an improper Chat implementation, please inform the authors about the following:", chatProvider.getPlugin().getName(), chatProvider.getPlugin().getPluginMeta().getVersion()));
					Towny.getPlugin().getLogger().warning(String.format("Class '%s' has a null Permission field, which is not supported.", chatProvider.getProvider().getClass().getName()));
					
					if (!iterator.hasNext())
						return chatProvider;
					else 
						continue;
				}
			} catch (Exception ignored) {}
			
			return chatProvider;
		}
		
		return null;
	}

	private boolean isPluginPresent(String pluginName) {
		return Bukkit.getServer().getPluginManager().getPlugin(pluginName) != null;
	}
	
	public boolean isPluginEnabled(@NotNull String pluginName) {
		return Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName);
	}

	/*
	 * Disable 3rd Party integrations.
	 */

	public void disable3rdPartyPluginIntegrations() {
		unregisterLuckPermsContexts();
		unloadPAPIExpansion(true);
	}

	/*
	 * Placeholder API integration methods.
	 */

	private boolean isPAPI() {
		return papiExpansion != null;
	}

	private void enablePAPI(@NotNull Towny towny) {
		papiExpansion = new TownyPlaceholderExpansion(towny);
		papiExpansion.register();
	}

	public void loadPAPIExpansion(boolean reload) {
		if (reload && isPAPI())
			papiExpansion.register();
	}

	public void unloadPAPIExpansion(boolean reload) {
		if (reload && isPAPI())
			papiExpansion.unregister();
	}

	/*
	 * LuckPermsContexts integration methods.
	 */

	private void enableLuckPermsContexts(@NotNull Towny towny) {
		luckPermsContexts = new LuckPermsContexts(towny);
		luckPermsContexts.registerContexts();
	}

	private void unregisterLuckPermsContexts() {
		if (luckPermsContexts != null) {
			luckPermsContexts.unregisterContexts();
			luckPermsContexts = null;
		}
	}

	/*
	 * Citizens2 integration methods.
	 */

	/**
	 * Check if the entity is a Citizens NPC.
	 * 
	 * Catches the NoClassDefFoundError thrown when Citizens is present 
	 * but failed to start up correctly.
	 * 
	 * @param entity Entity to check.
	 * @return true if the entity is an NPC.
	 * @deprecated Deprecated as of 0.100.1.10, please use {@link #isNPC(Entity)} instead.
	 */
	@Deprecated
	public boolean checkCitizens(Entity entity) {
		return entity != null && isNPC(entity);
	}

	/**
	 * @param entity Entity to check.
	 * @return true if the entity is a plugin-created NPC.
	 */
	public boolean isNPC(final @NotNull Entity entity) {
		return entity.hasMetadata("NPC");
	}

	/*
	 * EliteMobs integration methods.
	 */

	public boolean checkHostileEliteMobs(Entity entity) {
		return isEliteMobsPresent() && entity != null && entity.getPersistentDataContainer().has(eliteKey, PersistentDataType.STRING);
	}

	private boolean isEliteMobsPresent() {
		return eliteKey != null;
	}
}

