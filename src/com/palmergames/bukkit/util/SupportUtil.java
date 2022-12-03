package com.palmergames.bukkit.util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * Allows testing support for known unsupported classes
 * @author GNosii
 */
public class SupportUtil {

	private static final HashMap<String, Support> TESTS = getSupportData();

	/**
	 * Run tests defined in {@link #getSupportData()}
	 * @return map with classes detected and their support data
	 */
	public static HashMap<String, Support> test() {
		final HashMap<String, Support> map = new HashMap<>();
		
		TESTS.forEach((clazz, support) -> {
			try {
				/*
				 * I'd love to not need to do Class#forName
				 * Sadly, if the plugin is not loaded (most of the time)
				 * the warning will not be shown (as the plugin is not yet enabled
				 */
				Class.forName(clazz);
				if (support.type.shouldWarn) {
					map.put(clazz, support);
				}
			} catch (final ClassNotFoundException ignored) {}
		});
		
		return map;
	}

	/**
	 * Defines which classes should be tested for, and describe their support level and description.
	 * @return hard-coded map with the qualified name for the class and the support data.
	 */
	private static HashMap<String, Support> getSupportData() {
		final HashMap<String, Support> map = new HashMap<>();
		
		/* 
		 * To add an plugin to this list: Paste the classpath as it appears on plugin.yml's main field
		 * For example, if you were to add Towny (why would you?):
		 * map.put("com.palmergames.bukkit.towny.Towny", new Support(SupportType.EXTENSION, "Optional description."));
		 */
		map.put("com.mdc.mib.questioner.Question", new Support(SupportType.UNNECESSARY, "Towny no longer requires Questioner for questions and you can safely remove this plugin."));
		map.put("com.palmergames.townynameupdater.TownyNameUpdater", new Support(SupportType.UNNECESSARY, "Towny no longer depends on TownyNameUpdater for username changes and you can safely remove this plugin."));
		
		map.put("org.geysermc.floodgate.SpigotPlugin", new Support(SupportType.UNSUPPORTED_PLUGIN, "Floodgate is known to cause issues regarding their username format."));
		map.put("com.earth2me.essentials.economy.vault.VaultEconomyProvider", new Support(SupportType.UNSUPPORTED_ECONOMY, "Essentials Economy is known to reset town/nation balances on rare occasions. Be careful if you're using EssentialsEco."));
		
		return map;
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
		 * Support information without an description, defaults to {@link SupportType#toString()}
		 * @param type Level of support
		 */
		public Support(@NotNull SupportType type) {
			this.type = type;
			this.description = type.toString();
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
		 * Example: PlaceholderAPI, HolographicDisplays
		 */
		SUPPORTED(false),

		/**
		 * Plugin was required by Towny, no longer needed
		 * Example: TownyNameUpdater and Questioner
		 */
		UNNECESSARY(true),
		
		/**
		 * Economy plugin does not implement Vault/Reserve methods correctly 
		 * and won't work with Towny
		 */
		UNSUPPORTED_ECONOMY(true),

		/**
		 * Plugin can cause issues with Towny
		 * Example: Floodgate (issues related to user data)
		 */
		UNSUPPORTED_PLUGIN(true),

		/**
		 * Fork/platform can cause issues with Towny
		 * As of now, no platform is known to conflict with Towny
		 */
		UNSUPPORTED_PLATFORM(true);
		
		final boolean shouldWarn;

		/**
		 * Constructor for the {@link SupportType} enum
		 * @param warn Defines if this support level should emit warnings to console
		 */
		SupportType(Boolean warn) {
			this.shouldWarn = warn;
		}
	}
}
