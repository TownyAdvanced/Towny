package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Allows testing support for known unsupported classes
 * @author GNosii
 */
@ApiStatus.Internal
public class PluginSupportUtil {

	private static final Map<String, Support> TESTS = getSupportData();

	/**
	 * Run tests defined in {@link #getSupportData()}
	 * @return map with classes detected and their support data
	 */
	public static Map<String, Support> test() {
		final HashMap<String, Support> map = new HashMap<>();
		
		TESTS.forEach((test, support) -> {
			if (test.startsWith("platform:")) {
				if (Bukkit.getName().contains(cleanTest(test))) {
					map.put(cleanTest(test), support);
				}
			} else if (test.startsWith("plugin:")) {
				final Plugin plugin = Bukkit.getPluginManager().getPlugin(cleanTest(test));
				if (plugin != null) {
					map.put(cleanTest(test) + " " + plugin.getDescription().getVersion(), support);
				}
			} else if (test.startsWith("economy:")){
				if (TownyEconomyHandler.getVersion().contains(cleanTest(test))) {
					map.put(cleanTest(test), support);
				}
			} else {
				// Always try to avoid these tests!
				try {
					Class.forName(test);
					map.put(test, support);
				} catch (final ClassNotFoundException ignored) {}
			}
		});
		
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Defines which classes should be tested for, and describe their support level and description.
	 * @return hard-coded map with the qualified name for the class and the support data.
	 */
	private static Map<String, Support> getSupportData() {
		final Map<String, Support> map = new WeakHashMap<>();
		
		/* 
		 * Note: There's no current way of checking specific plugin versions.
		 * To add an class to this list, copy and paste its qualified name:
		 * map.put("com.palmergames.bukkit.towny.Towny", new Support(SupportType.EXTENSION, "Optional description."));
		 * 
		 * If the plugin loads before Towny does, and it's available on Bukkit's PluginManager you can use this syntax:
		 * map.put("plugin:Towny", new Support(SupportType.EXTENSION)
		 */
		
		map.put("plugin:MDCQuestioner", new Support(SupportType.UNNECESSARY, " Questioner present on server, Towny no longer requires Questioner for invites/confirmations. You may safely remove Questioner.jar from your plugins folder."));
		map.put("plugin:TownyNameUpdater", new Support(SupportType.UNNECESSARY, "Towny no longer depends on TownyNameUpdater for username changes and you can safely remove this plugin."));
		 
		map.put("plugin:floodgate", new Support(SupportType.UNSUPPORTED_PLUGIN, "Floodgate is known to cause issues regarding their username format."));
		
		map.put("economy:Essentials Economy", new Support(SupportType.UNSUPPORTED_ECONOMY, "Essentials Economy is known to reset town/nation balances on rare occasions. Be careful if you're using Essentials Economy."));
		map.put("economy:EssentialsX Economy", new Support(SupportType.UNSUPPORTED_ECONOMY, "EssentialsX Economy is known to reset town/nation balances on rare occasions. Be careful if you're using EssentialsX Economy."));
		
		// Unsupported economies, most of them don't implement Vault correctly.
		map.put("plugin:EconomyX", new Support(SupportType.UNSUPPORTED_ECONOMY));
		map.put("plugin:DKCoins", new Support(SupportType.UNSUPPORTED_ECONOMY));
		map.put("plugin:Economy", new Support(SupportType.UNSUPPORTED_ECONOMY));
		map.put("plugin:LiteEco", new Support(SupportType.UNSUPPORTED_ECONOMY));
		
		// TownyAdvanced plugins
		map.put("plugin:TownyCamps", new Support(SupportType.EXTENSION));
		map.put("plugin:TownyChat", new Support(SupportType.EXTENSION));
		map.put("plugin:TownyCultures", new Support(SupportType.EXTENSION));
		map.put("plugin:TownyFlight", new Support(SupportType.EXTENSION));
		map.put("plugin:TownyHistories", new Support(SupportType.EXTENSION));
		
		// Official war plugins
		map.put("plugin:SiegeWar", new Support(SupportType.EXTENSION));
		map.put("plugin:FlagWar", new Support(SupportType.EXTENSION));
		map.put("plugin:EventWar", new Support(SupportType.EXTENSION));

		// Plugins we hook with
		map.put("plugin:PlaceholderAPI", new Support(SupportType.SUPPORTED));
		map.put("plugin:TheNewChat", new Support(SupportType.SUPPORTED));
		map.put("plugin:LuckPerms", new Support(SupportType.SUPPORTED));
		map.put("plugin:Citizens", new Support(SupportType.SUPPORTED));
		map.put("plugin:GroupManager", new Support(SupportType.SUPPORTED));
		map.put("plugin:Vault", new Support(SupportType.SUPPORTED));
		map.put("plugin:Essentials", new Support(SupportType.SUPPORTED));
		
		// Commit #87421e0
		map.put("plugin:PowerRanks", new Support(SupportType.UNSUPPORTED_PLUGIN, "PowerRanks is incompatible with Towny. PowerRanks will override Towny's ability to give permissions via the townyperms.yml file. You can expect issues with Towny permissions (and other permission providers) while PowerRanks is installed."));
		
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Removes the prefix for the test. Namely, "plugin", "platform" and "economy"
	 * @return clean test string
	 */
	public static String cleanTest(String test) {
		return test.replaceFirst("(plugin|platform|economy|)\\:", "");
	}
	
	/**
	 * Defines the level of support for an certain class, including the support level, and an
	 * optional explanation of why it's tested for
	 * @author GNosii
	 */
	public static class Support {
		@NotNull
		public SupportType type;
		
		@NotNull
		public String description;

		/**
		 * Support information with an description
		 * @param type Level of support
		 * @param description Explanation as to why this is tested for
		 */
		public Support(@NotNull SupportType type, @NotNull String description) {
			this.type = type;
			this.description = description;
		}

		/**
		 * Support information without an description, defaults to {@link SupportType#defaultDescription}
		 * @param type Level of support
		 */
		public Support(@NotNull SupportType type) {
			this.type = type;
			this.description = type.defaultDescription;
		}
	}

	/**
	 * Defines the level of support Towny provides for an specific class, this being
	 * libraries, plugins or server implementations.
	 * @author GNosii
	 */
	public enum SupportType {

		/**
		 * Plugin built as an extension to Towny
		 * Example: TownyChat, SiegeWar
		 */
		EXTENSION(false),

		/**
		 * No issues have been reported for this plugin and we can help troubleshoot
		 * these issues
		 * Example: PlaceholderAPI
		 */
		SUPPORTED(false),

		/**
		 * Plugin was required by Towny, no longer needed
		 * Example: TownyNameUpdater and Questioner
		 */
		UNNECESSARY(true, "This plugin is no longer needed.", "Unnecessary"),
		
		/**
		 * Economy plugin does not implement Vault/Reserve methods correctly 
		 * and won't work with Towny
		 */
		UNSUPPORTED_ECONOMY(true, "This economy provider is not supported.", "UnsupportedEconomy"),

		/**
		 * Plugin can cause issues with Towny
		 * Example: Floodgate (issues related to user data)
		 */
		UNSUPPORTED_PLUGIN(true, "This plugin is not supported.", "UnsupportedPlugin"),

		/**
		 * Fork/platform can cause issues with Towny
		 * As of now, no platform is known to conflict with Towny
		 */
		UNSUPPORTED_PLATFORM(true, "This platform is not supported.", "UnsupportedPlatform");
		
		public final boolean warn;
		
		private final String defaultDescription;
		
		private final String name;
		
		SupportType(boolean warn) {
			this.warn = warn;
			this.defaultDescription = "";
			this.name = "";
		}
		
		SupportType(boolean warn, String desc, String name) {
			this.warn = warn;
			this.defaultDescription = desc;
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
