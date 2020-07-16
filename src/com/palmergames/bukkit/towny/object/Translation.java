package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	/**
	 * Translates give key into its respective language. 
	 * 
	 * @param key The language key.
	 * @return The localized string.
	 */
	public static String of(String key) {
		return TownySettings.getLangString(key);
	}

	/**
	 * Translates give key into its respective language. 
	 *
	 * @param key The language key.
	 * @param args The arguments to format the localized string.   
	 * @return The localized string.
	 */
	public static String of(String key, String... args) {
		return String.format(of(key), args);
	}

	private Translation() {}
}