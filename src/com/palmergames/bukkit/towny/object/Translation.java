package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.HelpMenu;
import com.palmergames.bukkit.towny.event.TranslationLoadEvent;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	private Translation() {}
	
	private static Map<String, Map<String, String>> translations = new HashMap<>();
	private static Locale defaultLocale;
	
	public static void loadTranslationRegistry() {
		translations.clear();
		Path langFolder = Paths.get(TownyUniverse.getInstance().getRootFolder()).resolve("settings").resolve("lang");
		TranslationLoader loader = new TranslationLoader(langFolder, Towny.getPlugin(), Towny.class);
		loader.updateLegacyLangFileName(TownySettings.getString(ConfigNodes.LANGUAGE));

		// Load built-in translations into memory.
		// Dumps built-in language files into reference folder. These are for reading
		// only, no changes to them will have an effect.
		loader.loadTranslationsIntoMemory();

		// Get the translations ahead of the TranslationLoadEvent.
		translations = loader.getTranslations();
		
		// Fire the TranslationLoadEvent, allowing other plugins to add Translations.
		TranslationLoadEvent translationLoadEvent = new TranslationLoadEvent();
		Bukkit.getPluginManager().callEvent(translationLoadEvent);
		// If another plugin added translations, add them to the transations hashmap.
		if (!translationLoadEvent.getAddedTranslations().isEmpty()) {
			addTranslations(translationLoadEvent.getAddedTranslations());
			// Set the translations back into the loader.
			loader.setTranslations(translations);
		}
		
		// Load optional override files.
		loader.loadOverrideFiles();

		// Load optional global file.
		loader.loadGlobalFile();
		
		// Get the finalized translation back from the loader.
		translations = loader.getTranslations();
		// Set the defaultLocale.
		setDefaultLocale();
		
		// Remove any disabled languages from the translations map.
		translations.keySet().removeIf(lang -> !TownySettings.isLanguageEnabled(lang) && !lang.equalsIgnoreCase(defaultLocale.toString()));

		Towny.getPlugin().getLogger().info(String.format("Successfully loaded translations for %d languages.", translations.keySet().size()));

		// Load HelpMenus only after translations have been set.
		HelpMenu.loadMenus();
	}
	
	/**
	 * Translates given key into the default Locale.
	 * 
	 * @param key The language key.
	 * @return The localized string.
	 */
	public static String of(String key) {
		if (defaultLocale == null) {
			Towny.getPlugin().getLogger().warning("Error: Tried to translate before a locale could be loaded!");
			return key;
		}
		
		String data = translations.get(defaultLocale.toString()).get(key.toLowerCase(Locale.ROOT));

		if (data == null) {
			TownySettings.sendError(key.toLowerCase() + " from " + TownySettings.getString(ConfigNodes.LANGUAGE));
			return key;
		}
		return Colors.translateColorCodes(data);
	}

	/**
	 * Translates given key into the default Locale. 
	 *
	 * @param key The language key.
	 * @param args The arguments to format the localized string.
	 * @return The localized string.
	 */
	public static String of(String key, Object... args) {
		return String.format(of(key), args);
	}

	/**
	 * Translates given key into the given locale. 
	 * 
	 * @param key The language key.
	 * @param locale Locale to translate to.
	 * @return The localized string.
	 */
	public static String of(String key, Locale locale) {
		String data = translations.get(validateLocale(locale.toString())).get(key.toLowerCase(Locale.ROOT));

		if (data == null) {
			// The locale is missing the language string or the locale is invalid, try to use the default locale.
			return of(key);
		}

		return Colors.translateColorCodes(data);
	}
	
	/**
	 * Translates given key into the given locale. 
	 * 
	 * @param key The language key.
	 * @param locale Locale to translate to.
	 * @param args The arguments to format the localized string.
	 * @return The localized string.
	 */
	public static String of(String key, Locale locale, Object... args) {
		return String.format(of(key, locale), args);
	}
	
	public static String of(String key, CommandSender sender) {
		return of(key, getLocale(sender));
	}
	
	public static String of(String key, CommandSender sender, Object... args) {
		return String.format(of(key, getLocale(sender)), args);
	}
	
	public static String of(String key, Resident resident) {
		return of(key, getLocale(resident));
	}
	
	public static String of(String key, Resident resident, Object... args) {
		return String.format(of(key, getLocale(resident)), args);
	}

	public static Locale toLocale(String fileName, boolean shouldWarn) {
		int lastIndex = fileName.lastIndexOf(".") == -1 ? fileName.length() : fileName.lastIndexOf(".");
		try {
			String[] locale = fileName.substring(0, lastIndex).split("[-_]");
			return new Locale(locale[0], locale[1]);
		} catch (Exception e) {
			if (shouldWarn)
				Towny.getPlugin().getLogger().warning(String.format("Could not convert '%s' into a locale, falling back to en_US.", fileName));
			
			return new Locale("en", "US");
		}
	}
	
	protected static void setDefaultLocale() {
		defaultLocale = loadDefaultLocale();
	}
	
	private static Locale loadDefaultLocale() {
		Locale locale = toLocale(TownySettings.getString(ConfigNodes.LANGUAGE), true);
		String stringLocale = locale.toString();
		
		if (!translations.containsKey(stringLocale)) {
			locale = new Locale("en", "US");
			Towny.getPlugin().getLogger().warning(String.format("The locale '%s' is currently not loaded, falling back to en_US. (Is it being loaded correctly?)", stringLocale));
		}
		
		return locale;
	}
	
	public static Locale getDefaultLocale() {
		return defaultLocale;
	}
	
	private static String validateLocale(String locale) {
		return translations.containsKey(locale) ? locale : defaultLocale.toString();
	}
	
	public static String translateTranslatables(CommandSender sender, Translatable... translatables) {
		return translateTranslatables(sender, " ", translatables);
	}
	
	public static String translateTranslatables(CommandSender sender, String delimiter, Translatable... translatables) {
		Locale locale = getLocale(sender);
		return Arrays.stream(translatables).map(translatable -> translatable.translate(locale)).collect(Collectors.joining(delimiter));
	}
	
	public static Locale getLocale(CommandSender sender) {
		return sender instanceof Player ? Translation.toLocale(((Player) sender).getLocale(), false) : defaultLocale;
	}
	
	public static Locale getLocale(Resident resident) {
		return BukkitTools.isOnline(resident.getName()) ? getLocale(resident.getPlayer()) : defaultLocale;
	}
	
	public static void addTranslations(Map<String, Map<String, String>> addedTranslations) {
		if (addedTranslations != null && !addedTranslations.isEmpty()) {
			for (String language : addedTranslations.keySet()) {
				if (!TownySettings.isLanguageEnabled(language))
					continue;

				if (addedTranslations.get(language) != null && !addedTranslations.get(language).isEmpty()) {
					language = language.replaceAll("-", "_");
					Map<String, String> newTranslations = addedTranslations.get(language);
					translations.computeIfAbsent(language, k -> new HashMap<>());
					for (Map.Entry<String, String> entry : newTranslations.entrySet())
						translations.get(language).put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
				}
			}
		}
	}
}