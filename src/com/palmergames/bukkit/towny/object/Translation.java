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
	private static final Path langFolder = Paths.get(TownyUniverse.getInstance().getRootFolder()).resolve("settings").resolve("lang");
		
	public static void loadTranslationRegistry() {
		translations.clear();
		TranslationLoader loader = new TranslationLoader(langFolder, Towny.getPlugin(), Towny.class);
		loader.updateLegacyLangFileName(TownySettings.getString(ConfigNodes.LANGUAGE));

		// Create the global.yml file if it doesn't exist.
		Path globalYMLPath = langFolder.resolve("global.yml");
		if (!globalYMLPath.toFile().exists())
			loader.createGlobalYML(globalYMLPath, Towny.class.getResourceAsStream("/global.yml"));

		// Load global override file into memory.
		Map<String, Object> globalOverrides = loader.loadGlobalFile(globalYMLPath);
		
		// Dump built-in language files into reference folder.
		// These are for reading only, no changes to them will have an effect.
		// Loads translations into memory.
		loader.loadTranslationsIntoMemory(Towny.getPlugin(), Towny.class, langFolder);

		translations = loader.getTranslations();
		
		// Fire the TranslationLoadEvent, allowing other plugins to add Translations.
		TranslationLoadEvent translationLoadEvent = new TranslationLoadEvent();
		Bukkit.getPluginManager().callEvent(translationLoadEvent);
		addTranslations(translationLoadEvent.getAddedTranslations());

		loader.setTranslations(translations);
		
		// Load optional override files.
		loader.loadOverrideFiles();

		// Can be null if no overrides have been added
		if (globalOverrides != null)
			loader.overwriteKeysWithGlobalOverrides(globalOverrides);

		translations = loader.getTranslations();
		// Set the defaultLocale.
		setDefaultLocale();

		Towny.getPlugin().getLogger().info(String.format("Successfully loaded translations for %d languages.", translations.keySet().size()));

		// Load HelpMenus only after translations have been set.
		HelpMenu.loadMenus();
	}
	
	/**
	 * Translates given key into its respective language. 
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
	 * Translates given key into its respective language. 
	 *
	 * @param key The language key.
	 * @param args The arguments to format the localized string.   
	 * @return The localized string.
	 */
	public static String of(String key, Object... args) {
		return String.format(of(key), args);
	}

	public static String of(String key, Locale locale) {
		String data = translations.get(validateLocale(locale.toString())).get(key.toLowerCase(Locale.ROOT));

		if (data == null) {
			// The locale is missing the language string or the locale is invalid, try to use the default locale.
			return of(key);
		}

		return Colors.translateColorCodes(data);
	}
	
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
	
	static void addLocale(String lang) {
		translations.put(lang, new HashMap<>());
	}
	
	public static void addTranslations(String lang, Map<String, Object> values) {
		translations.computeIfAbsent(lang, k -> new HashMap<>());
		for (Map.Entry<String, Object> entry : values.entrySet())
			translations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), TranslationLoader.getTranslationValue(entry));
	}
	
	public static void addTranslations(Map<String, Map<String, String>> addedTranslations) {
		if (addedTranslations != null && !addedTranslations.isEmpty()) {
			for (String language : addedTranslations.keySet())
				if (addedTranslations.get(language) != null && !addedTranslations.get(language).isEmpty()) {
					Map<String, String> newTranslations = addedTranslations.get(language);
					language = language.replaceAll("-", "_");

					for (Map.Entry<String, String> entry : newTranslations.entrySet()) {
						translations.computeIfAbsent(language, k -> new HashMap<>());
						translations.get(language).put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
					}
				}
		}
	}
}