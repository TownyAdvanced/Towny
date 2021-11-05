package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.HelpMenu;
import com.palmergames.bukkit.towny.event.TranslationLoadEvent;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.FileMgmt;
import org.apache.commons.compress.utils.FileNameUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	private Translation() {}

	private static final Map<String, String> oldLangFileNames = createLegacyLangMap();
	private static final Set<String> langFiles = createValidLang();
	private static final Map<String, Map<String, String>> translations = new HashMap<>();
	private static Locale defaultLocale;
	private static final Path langFolder = Paths.get(TownyUniverse.getInstance().getRootFolder()).resolve("settings").resolve("lang");

	public static void loadTranslationRegistry() {
		translations.clear();
		updateLegacyLangFileName(TownySettings.getString(ConfigNodes.LANGUAGE));

		// Load global override file
		Map<String, Object> globalOverrides = new HashMap<>();
		Path globalYMLPath = langFolder.resolve("global.yml");
		
		if (!FileMgmt.checkOrCreateFile(globalYMLPath.toString())) {
			throw new TownyInitException("Failed to touch '" + globalYMLPath + "'.", TownyInitException.TownyError.LOCALIZATION);
		}
		try {
			InputStream resource = Towny.class.getResourceAsStream("/global.yml");
			if (resource == null) {
				throw new TownyInitException("Could not find global.yml in the JAR", TownyInitException.TownyError.LOCALIZATION);
			}
			Files.copy(resource, globalYMLPath);
		} catch (FileAlreadyExistsException ignored) {
			// Expected behaviour
		} catch (IOException e) {
			throw new TownyInitException("Failed to copy global.yml from the JAR to '" + globalYMLPath + "'", TownyInitException.TownyError.LOCALIZATION, e);
		}
		
		try (InputStream is = Files.newInputStream(globalYMLPath)) {
			globalOverrides = new Yaml(new SafeConstructor()).load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// There is no need to touch langPath, Files.copy takes care of that.
		try {
			Files.createDirectories(langFolder.resolve("reference"));
		} catch (IOException e) {
			throw new TownyInitException("Failed to create language reference folder.", TownyInitException.TownyError.LOCALIZATION, e);
		}
		// Load bundled language files
		for (String lang : langFiles) {
			try (InputStream is = Translation.class.getResourceAsStream("/lang/" + lang + ".yml")) {
				if (is == null) {
					throw new TownyInitException("Could not find " + "'/lang/" + lang + ".yml'" + " in the JAR", TownyInitException.TownyError.LOCALIZATION);
				}
				Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
				
				saveReferenceFile(values.get("version"), lang);
				
				lang = lang.replace("-", "_"); // Locale#toString uses underscores instead of dashes
				translations.put(lang, new HashMap<>());
				
				for (Map.Entry<String, Object> entry : values.entrySet())
					translations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
			} catch (Exception e) {
				// An IO exception occured, or the file had invalid yaml
				Towny.getPlugin().getLogger().log(Level.WARNING, "Failed to load/save '" + lang + ".yml'.", e);
			}
		}

		TranslationLoadEvent translationLoadEvent = new TranslationLoadEvent();
		Bukkit.getPluginManager().callEvent(translationLoadEvent);
		
		Map<String, Map<String, String>> addedTranslations = translationLoadEvent.getAddedTranslations();
		if (addedTranslations != null && !addedTranslations.isEmpty()) {
			for (String lang : addedTranslations.keySet())
				if (addedTranslations.get(lang) != null && !addedTranslations.get(lang).isEmpty())
					for (Map.Entry<String, String> entry : addedTranslations.get(lang).entrySet()) {
						translations.computeIfAbsent(lang, k -> new HashMap<>());
						translations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
					}
		}
		
		// Load optional override files.
		File[] overrideFiles = new File(langFolder + File.separator + "override").listFiles();
		if (overrideFiles != null) {
			for (File file : overrideFiles) {
				if (file.isFile() && FileNameUtils.getExtension(file.getName()).equalsIgnoreCase("yml") && !file.getName().equalsIgnoreCase("global.yml")) {
					try (FileInputStream is = new FileInputStream(file)) {
						Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
						String lang = FileNameUtils.getBaseName(file.getName());

						if (values != null) {
							translations.computeIfAbsent(lang, k -> new HashMap<>());
							for (Map.Entry<String, Object> entry : values.entrySet())
								translations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		//Can be null if no overrides have been added
		if (globalOverrides != null)
			for (Map.Entry<String, Object> entry : globalOverrides.entrySet())
				for (String lang : translations.keySet())
					translations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
		
		defaultLocale = loadDefaultLocale();		
		Towny.getPlugin().getLogger().info(String.format("Successfully loaded translations for %d languages.", translations.keySet().size()));
		HelpMenu.loadMenus();
	}
	
	private static void saveReferenceFile(@Nullable Object currentVersion, String lang) {
		if (currentVersion == null)
			return;

		// Resolves langfolder/reference/whatever_language.yml
		Path langPath = langFolder.resolve("reference").resolve(lang + ".yml");
		// Files.copy takes care of the creation of lang.yml AS LONG AS the parent directory exists
		// Which we take care of right before the languages are looped through.
		
		// Get the resource
		try (InputStream resource = Towny.class.getResourceAsStream("/lang/" + lang + ".yml")) {

			if (resource == null) {
				throw new TownyInitException("Could not find " + "'/lang/" + lang + ".yml'" + " in the JAR.", TownyInitException.TownyError.LOCALIZATION);
			}

			// Check the existing lang file for the version, and if necessary, replace it.
			try (InputStream is = Files.newInputStream(langPath)) {
				Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
				if (values == null || (double) currentVersion != (double) values.get("version")) {
					is.close();
					// Remove the old reference file.
					langPath.toFile().delete();
					// Copy resource to location.
					Files.copy(resource, langPath);
				}
			} catch (NoSuchFileException e) {
				// We haven't got this file in the reference folder yet.
				Files.copy(resource, langPath);
			}
			resource.close();
		} catch (IOException e) {
			throw new TownyInitException("Failed to copy " + "'/lang/" + lang + ".yml'" + " from the JAR to '" + langPath + " during a langauge file update.'", TownyInitException.TownyError.LOCALIZATION, e);
		}
	}
	
	public static Set<String> createValidLang() {
		final Set<String> lang = new HashSet<>();
		final URI uri;
		try {
			uri = Towny.class.getResource("").toURI();
			final FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
			Files.list(fs.getRootDirectories().iterator().next().resolve("/lang")).forEach(p -> lang.add(FileNameUtils.getBaseName(p.toString())));
			fs.close();
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		return lang;
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
			TownySettings.sendError(key.toLowerCase() + " from " + TownySettings.getString(ConfigNodes.LANGUAGE));
			return key;
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

	/**
	 * Attempt to rename old languages files (ie: english.yml to en-US.yml.)
	 * 
	 * @param lang String name of the language file in the config's language setting.
	 * @since 0.97.0.21
	 */
	private static void updateLegacyLangFileName(String lang) {
		if (!oldLangFileNames.containsKey(lang))
			return;
		String path = Towny.getPlugin().getDataFolder().getPath() + File.separator + "settings" + File.separator ;
		File oldFile = new File(path + lang);
		File newFile = new File(path + oldLangFileNames.get(lang));
		boolean rename = oldFile.renameTo(newFile);
		if (rename) {
			Towny.getPlugin().getLogger().info("Language file name updated.");
			TownySettings.setLanguage(oldLangFileNames.get(lang));
		} else 
			Towny.getPlugin().getLogger().warning("Language file was not updated.");
	}

	private static Map<String, String> createLegacyLangMap() {
		Map<String, String> oldLangFileNames = new HashMap<>();
		oldLangFileNames.put("danish.yml", "da-DK.yml");
		oldLangFileNames.put("german.yml", "de-DE.yml");
		oldLangFileNames.put("english.yml", "en-US.yml");
		oldLangFileNames.put("spanish.yml", "es-ES.yml");
		oldLangFileNames.put("french.yml", "fr-FR.yml");
		oldLangFileNames.put("italian.yml", "it-IT.yml");
		oldLangFileNames.put("korean.yml", "ko-KR.yml");
		oldLangFileNames.put("norwegian.yml", "no-NO.yml");
		oldLangFileNames.put("polish.yml", "pl-PL.yml");
		oldLangFileNames.put("pt-br.yml", "pt-BR.yml");
		oldLangFileNames.put("russian.yml", "ru-RU.yml");
		oldLangFileNames.put("sv-SE.yml", "sv-SE.yml");
		oldLangFileNames.put("chinese.yml", "zh-CN.yml");
		return oldLangFileNames;
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
	
	private static Locale loadDefaultLocale() {
		Locale locale = toLocale(TownySettings.getString(ConfigNodes.LANGUAGE), true);
		String stringLocale = locale.toString();
		
		if (!translations.containsKey(stringLocale)) {
			locale = new Locale("en", "US");
			Towny.getPlugin().getLogger().warning(String.format("The locale '%s' is currently not loaded, falling back to en_US. (Is it being loaded correctly?)", stringLocale));
		}
		
		return locale;
	}
	

	public static void addTranslations(Map<String, Map<String, String>> addedTranslations) {
		for (String language : addedTranslations.keySet())
			for (Map.Entry<String, String> entry : addedTranslations.get(language).entrySet()) {
				translations.computeIfAbsent(language, k -> new HashMap<>());
				translations.get(language).put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
			}
	}
}