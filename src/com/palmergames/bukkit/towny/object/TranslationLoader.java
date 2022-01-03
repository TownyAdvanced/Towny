package com.palmergames.bukkit.towny.object;

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
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.compress.utils.FileNameUtils;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.util.FileMgmt;

public class TranslationLoader {
	private final Path langFolderPath;
	private final Plugin plugin;
	private final Class<?> clazz;
	private static Map<String, Map<String, String>> newTranslations = new HashMap<>();
	private static final Map<String, String> oldLangFileNames = createLegacyLangMap();
	
	/**
	 * An object which allows a plugin to load language files into Towny's
	 * translations hashmap. Enabling the given plugin to use Towny's built-in
	 * messaging and translating so that messages will display in the player's own
	 * Locale (determined by the client's locale setting.)<br><br>
	 * 
	 * Language files should be saved in your plugin's resources\lang\ folder, using
	 * valid Locale file names ie: en-US.yml, de-DE.yml. Locales which do not appear
	 * in Minecraft will not be used.<br><br>
	 * 
	 * You may opt to provide a global.yml file in your plugin's resources folder,
	 * which will allow an admin to globally override language strings for all
	 * locales.<br><br>
	 * 
	 * Example: <br>
	 *     Plugin plugin = Towny.getPlugin(); <br> 
	 *     Path langFolderPath = Paths.get(plugin.getDataFolder().getPath()).resolve("lang"); <br>
	 *     TranslationLoader loader = new TranslationLoader(langFolderPath, plugin, Towny.class);<br>
	 *     loader.load();<br> 
	 *     TownyAPI.addTranslations(plugin, loader.getTranslations());<br>
	 * 
	 * @param langFolderPath Path to where the plugin stores their language files on
	 *                       the server.
	 * @param plugin         Plugin, your plugin.
	 * @param clazz          Class file of your plugin, ie: Towny.class.
	 * @throws TownyInitException When files cannot be saved, loaded or something
	 *                            else goes wrong.
	 */
	public TranslationLoader(Path langFolderPath, Plugin plugin, Class<?> clazz) {
		this.langFolderPath = langFolderPath;
		this.plugin = plugin;
		this.clazz = clazz;
	}

	/**
	 * Load the translations from inside the plugin into memory, create reference
	 * folder, create and load override folder, create and load global.yml file.
	 */
	public void load() {
		newTranslations = new HashMap<>();

		// Load built-in translations into memory.
		// Dumps built-in language files into reference folder. These are for reading
		// only, no changes to them will have an effect.
		loadTranslationsIntoMemory();

		// Load optional override files.
		loadOverrideFiles();

		// Load optional global file.
		loadGlobalFile();

		plugin.getLogger().info(String.format("Successfully loaded translations for %d languages.", newTranslations.keySet().size()));
	}
	
	/**
	 * @return translations Map&lt;String, Map&lt;String, String&gt;&gt; A hashmap
	 *         keyed by the locale name, with a value of secondary hashmap of the
	 *         locale's language string keys and their corresponding values.
	 */
	public Map<String, Map<String, String>> getTranslations() {
		return newTranslations;
	}
	
	/**
	 * Used internally by Towny after the TranslationLoadEvent.
	 * @param _translations Map&ltString, Map&lt;String, String&gt;&gt; Translations to set.
	 */
	void setTranslations(Map<String, Map<String, String>> translations) {
		newTranslations = translations;
	}

	/**
	 * Loads the language files from the plugin's jar into memory in the
	 * translations map, saves the locale files into the reference folder.
	 */
	void loadTranslationsIntoMemory() {
		// There is no need to touch langPath, Files.copy takes care of that.
		try {
			Files.createDirectories(langFolderPath.resolve("reference"));
		} catch (IOException e) {
			throw new TownyInitException("Failed to create language reference folder.", TownyInitException.TownyError.LOCALIZATION, e);
		}
		// Load bundled language files
		for (String lang : getLangFileNamesFromPlugin()) {
			try (InputStream is = clazz.getResourceAsStream("/lang/" + lang + ".yml")) {
				if (is == null) {
					throw new TownyInitException("Could not find " + "'/lang/" + lang + ".yml'" + " in the JAR", TownyInitException.TownyError.LOCALIZATION);
				}
				Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
				
				saveReferenceFile(values.get("version"), lang);
				
				lang = lang.replace("-", "_"); // Locale#toString uses underscores instead of dashes
				if (!newTranslations.containsKey(lang))
					newTranslations.put(lang, new HashMap<>());
				
				for (Map.Entry<String, Object> entry : values.entrySet())
					newTranslations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
			} catch (Exception e) {
				// An IO exception occured, or the file had invalid yaml
				plugin.getLogger().log(Level.WARNING, "Failed to load/save '" + lang + ".yml'.", e);
			}
		}
	}
	
	/**
	 * @return lang Set&lt;String&gt; Set containing the names of the locale files. 
	 */
	private Set<String> getLangFileNamesFromPlugin() {
		final Set<String> lang = new HashSet<>();
		final URI uri;
		try {
			uri = clazz.getResource("").toURI();
			final FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
			Files.list(fs.getRootDirectories().iterator().next().resolve("/lang")).forEach(p -> lang.add(FileNameUtils.getBaseName(p.toString())));
			fs.close();
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		return lang;
	}
	
	/**
	 * Saves a copy of the language file for admin reference.
	 * 
	 * @param currentVersion Object what will be the version number of the lang file. 
	 * @param lang String locale and file name to be used for the reference file.
	 */
	private void saveReferenceFile(@Nullable Object currentVersion, String lang) {
		if (currentVersion == null)
			return;

		// Resolves langfolder/reference/whatever_language.yml
		Path langPath = langFolderPath.resolve("reference").resolve(lang + ".yml");
		// Files.copy takes care of the creation of lang.yml AS LONG AS the parent directory exists
		// Which we take care of right before the languages are looped through.
		
		// Get the resource
		try (InputStream resource = clazz.getResourceAsStream("/lang/" + lang + ".yml")) {

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

	/**
	 * Create and load the override folder which is used by the server admin to
	 * replace strings in individual locale files.
	 */
	void loadOverrideFiles() {
		try {
			Files.createDirectories(langFolderPath.resolve("override"));
		} catch (IOException e) {
			throw new TownyInitException("Failed to create language override folder.", TownyInitException.TownyError.LOCALIZATION, e);
		}
		
		File[] overrideFiles = new File(langFolderPath + File.separator + "override").listFiles();
		if (overrideFiles != null) {
			for (File file : overrideFiles) {
				if (file.isFile() && FileNameUtils.getExtension(file.getName()).equalsIgnoreCase("yml") && !file.getName().equalsIgnoreCase("global.yml")) {
					try (FileInputStream is = new FileInputStream(file)) {
						Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
						String lang = FileNameUtils.getBaseName(file.getName()).replaceAll("-", "_");

						if (values != null) {
							newTranslations.computeIfAbsent(lang, k -> new HashMap<>());
							for (Map.Entry<String, Object> entry : values.entrySet())
								newTranslations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), getTranslationValue(entry));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Get the language string associated with the given key for a locale.
	 * 
	 * @param entry Map.Entry&lt;String, Object&gt; which holds the keys and values
	 *              of a locale.
	 * @return string language value of a locale's language key.
	 */
	private static String getTranslationValue(Map.Entry<String, Object> entry) {
		// Messages blocked from being overriden.
		if (entry.getKey().toLowerCase().startsWith("msg_ptw_warning")) {
			// Get the defaultLocale's translation of the PTW warnings.
			String msg = String.valueOf(entry.getValue());
			Towny.getPlugin().getLogger().warning("Attempted to override an protected string. Skipped " + entry.getKey());
			// It's extremely possible the jar was edited and the string is missing/was modified.
			if (!msg.contains("Towny"))
				// Return a hard-coded message, the translation in the jar was likely tampered with.
				return switch (entry.getKey()) {
					case "msg_ptw_warning_1" -> "If you have paid any real-life money for these townblocks please understand: the server you play on is in violation of the Minecraft EULA and the Towny license.";
					case "msg_ptw_warning_2" -> "The Towny team never intended for townblocks to be purchaseable with real money.";
					case "msg_ptw_warning_3" -> "If you did pay real money you should consider playing on a Towny server that respects the wishes of the Towny Team.";
					default -> throw new IllegalArgumentException("Unexpected value: " + entry.getKey());
				};
			// Return the defaultLocale's message, it appears to have been left alone.
			return msg;
		}
		// Return the normal translation of the entry.
		return String.valueOf(entry.getValue());
	}
	
	/*
	 * Global.yml methods.
	 */
	
	/**
	 * Creates and loads a global.yml if one is present in the plugin's jar.
	 */
	void loadGlobalFile() {
		InputStream resourceAsStream = clazz.getResourceAsStream("/global.yml");
		// A plugin might be using this without also making use of the global.yml.
		if (resourceAsStream == null)
			return;

		// Create the global.yml file if it doesn't exist.
		Path globalYMLPath = langFolderPath.resolve("global.yml");
		if (!globalYMLPath.toFile().exists())
			createGlobalYML(globalYMLPath, resourceAsStream);

		// Load global override file into memory.
		Map<String, Object> globalOverrides = loadGlobalFile(globalYMLPath);
		// Can be null if no overrides have been added
		if (globalOverrides != null)
			overwriteKeysWithGlobalOverrides(globalOverrides);
		
	}
	
	/**
	 * Creates the global.yml file.
	 * 
	 * @param globalYMLPath Path where the global.yml will be saved.
	 * @param resource InputStream holding the global.yml contents.
	 */
	private void createGlobalYML(Path globalYMLPath, InputStream resource) {
		if (!FileMgmt.checkOrCreateFile(globalYMLPath.toString())) {
			throw new TownyInitException("Failed to touch '" + globalYMLPath + "'.", TownyInitException.TownyError.LOCALIZATION);
		}
		try {
			if (resource == null) {
				throw new TownyInitException("Could not find global.yml in the JAR", TownyInitException.TownyError.LOCALIZATION);
			}
			Files.copy(resource, globalYMLPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (FileAlreadyExistsException ignored) {
			// Should not be possible.
		} catch (IOException e) {
			throw new TownyInitException("Failed to copy global.yml from the JAR to '" + globalYMLPath + "'", TownyInitException.TownyError.LOCALIZATION, e);
		}
	}
	
	/**
	 * Loads the global override language keys and values.
	 * 
	 * @param globalYMLPath The path where the global.yml exists.
	 * @return globalOverrides Map&lt;String, Object&gt; of keys and their values
	 *         which will override all other locale's language keys and values;
	 */
	private Map<String, Object> loadGlobalFile(Path globalYMLPath) {
		Map<String, Object> globalOverrides = new HashMap<>();
		try (InputStream is = Files.newInputStream(globalYMLPath)) {
			globalOverrides = new Yaml(new SafeConstructor()).load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return globalOverrides;
	}
	
	/**
	 * Overwrite each locale with the globally overridden keys and their values.
	 * 
	 * @param globalOverrides Map&lt;String, Object&gt; of keys and their values
	 *                        which are overriding all other locale's language keys
	 *                        and values;
	 */
	private void overwriteKeysWithGlobalOverrides(Map<String, Object> globalOverrides) {
		for (Map.Entry<String, Object> entry : globalOverrides.entrySet())
			for (String lang : newTranslations.keySet()) {
				System.out.println("global override lang " + lang + " size " + newTranslations.get(lang).keySet().size());
				newTranslations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), getTranslationValue(entry));
			}
	}
	
	/*
	 * Legacy file name methods.
	 */
	
	/**
	 * Attempt to rename old languages files (ie: english.yml to en-US.yml.)
	 * 
	 * @param lang String name of the language file in the config's language setting.
	 * @since 0.97.0.21
	 */
	void updateLegacyLangFileName(String lang) {
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

}
