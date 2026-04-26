package com.palmergames.bukkit.towny.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.LoaderOptions;
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
	private boolean updateReferenceFiles = true;
	private Map<String, Map<String, String>> newTranslations = new HashMap<>();
	
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
	 * An object which allows a plugin to load language files into Towny's
	 * translations hashmap. Enabling the given plugin to use Towny's built-in
	 * messaging and translating so that messages will display in the player's own
	 * Locale (determined by the client's locale setting.)<br>
	 * <br>
	 * 
	 * This constructor requires that your Language files be saved in your plugin's
	 * resources\lang\ folder, using valid Locale file names ie: en-US.yml,
	 * de-DE.yml. Locales which do not appear in Minecraft will not be used.<br>
	 * <br>
	 * 
	 * You may opt to provide a global.yml file in your plugin's resources folder,
	 * which will allow an admin to globally override language strings for all
	 * locales.<br>
	 * <br>
	 * 
	 * Example: <br>
	 * Plugin plugin = Towny.getPlugin(); <br>
	 * TranslationLoader loader = new TranslationLoader(plugin);<br>
	 * loader.load();<br>
	 * TownyAPI.addTranslations(plugin, loader.getTranslations());<br>
	 * 
	 * @param plugin Plugin, your plugin.
	 * @throws TownyInitException When files cannot be saved, loaded or something
	 *                            else goes wrong.
	 */
	public TranslationLoader(Plugin plugin) {
		this.langFolderPath = Paths.get(plugin.getDataFolder().getPath()).resolve("lang");
		this.plugin = plugin;
		this.clazz = plugin.getClass();
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
	 * @param translations Map&ltString, Map&lt;String, String&gt;&gt; Translations to set.
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
		
		// Collect contents of the default lang file, used for saving reference files
		String defaultLangContent = null;
		if (updateReferenceFiles) {
			try (InputStream is = clazz.getResourceAsStream("/lang/en-US.yml")) {
				if (is != null) {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
						defaultLangContent = reader.lines().collect(Collectors.joining("\n"));
					}
				}
			} catch (IOException ignored) {}
		}
		
		// Load bundled language files
		for (String lang : getLangFileNamesFromPlugin()) {
			try (InputStream is = clazz.getResourceAsStream("/lang/" + lang + ".yml")) {
				if (is == null)
					throw new TownyInitException("Could not find " + "'/lang/" + lang + ".yml'" + " in the JAR", TownyInitException.TownyError.LOCALIZATION);
				
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					String content = reader.lines().collect(Collectors.joining("\n"));
					Map<String, Object> values = new Yaml(new SafeConstructor(new LoaderOptions())).load(content);

					if (updateReferenceFiles)
						saveReferenceFile(lang, defaultLangContent, content, values);

					lang = lang.replace("-", "_"); // Locale#toString uses underscores instead of dashes

					Map<String, String> translations = newTranslations.computeIfAbsent(lang, k -> new HashMap<>());
					for (Map.Entry<String, Object> entry : values.entrySet())
						translations.put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
				}
			} catch (Exception e) {
				// An IO exception occured, or the file had invalid yaml
				plugin.getLogger().log(Level.WARNING, "Unabled to read yaml file: '" + lang + ".yml' from within the " + plugin.getName() + ".jar.", e);
			}
		}
	}
	
	/**
	 * @return lang Set&lt;String&gt; Set containing the names of the locale files. 
	 */
	private Set<String> getLangFileNamesFromPlugin() {
		final Set<String> lang = new HashSet<>();
		try {
			URL root = clazz.getResource("");
			if (root == null)
				return lang;
			
			try (final FileSystem fs = FileSystems.newFileSystem(root.toURI(), Collections.emptyMap()); Stream<Path> stream  = Files.list(fs.getRootDirectories().iterator().next().resolve("/lang"))) {
				stream.map(FileMgmt::getFileName)
					.filter(TownySettings::isLanguageEnabled)
					.forEach(lang::add);
			}
		} catch (URISyntaxException | IOException e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred while getting language file names from the plugin jar", e);
		}
		return lang;
	}
	
	/**
	 * Saves a copy of the language file for admin reference.
	 * 
	 * @param lang String locale and file name to be used for the reference file.
	 * @param defaultLangContent The contents of the default language file (the one that has ALL translations)   
	 * @param content The contents of the resource file
	 * @param translations The contents, parsed through yaml
	 */
	private void saveReferenceFile(String lang, String defaultLangContent, String content, Map<String, Object> translations) {
		// Resolves langfolder/reference/whatever_language.yml
		Path langPath = langFolderPath.resolve("reference").resolve(lang + ".yml");
		// Files.copy takes care of the creation of lang.yml AS LONG AS the parent directory exists
		// Which we take care of right before the languages are looped through.
		
		try {
			if (!Files.exists(langPath))
				Files.createFile(langPath);

			if (defaultLangContent == null || lang.equals("en-US")) {
				try (Stream<String> lines = Files.lines(langPath)) {
					if (!content.equals(lines.collect(Collectors.joining("\n"))))
						Files.writeString(langPath, content);
				}
				
				return;
			}
			
			List<String> list = Arrays.asList(defaultLangContent.split("\n"));
			ListIterator<String> iterator = list.listIterator();
			
			while (iterator.hasNext()) {
				String line = iterator.next();
				
				if (line.contains(":")) {
					String key = line.substring(0, line.indexOf(":"));
					
					Object translated = translations.get(key);
					if (translated != null) {
						String replace = String.valueOf(translated);
						if (replace.contains("'"))
							replace = '"' + replace + '"';
						else 
							replace = "'" + replace + "'";
						
						iterator.set(key + ": " + replace);
					}
				}
			}
			
			Files.writeString(langPath, String.join("\n", list));			
		} catch (IOException e) {
			plugin.getLogger().log(Level.WARNING, "Failed to copy " + "'/lang/" + lang + ".yml'" + " from the JAR to '" + langPath.toAbsolutePath() + "' during a reference language file update.", e);
		}
	}

	/**
	 * Create and load the override folder which is used by the server admin to
	 * replace strings in individual locale files.
	 */
	void loadOverrideFiles() {
		try {
			Path overrides = Files.createDirectories(langFolderPath.resolve("override"));
			
			try (Stream<Path> overrideStream = Files.list(overrides)) {
				for (final Path path : overrideStream.collect(Collectors.toList())) {
					if (!FileMgmt.getExtension(path).equalsIgnoreCase("yml"))
						return;
					
					final String fileName = FileMgmt.getFileName(path);
					if (fileName.equals("global") || !TownySettings.isLanguageEnabled(fileName))
						return;
					
					try (InputStream is = Files.newInputStream(path)) {
						Map<String, Object> values = new Yaml(new SafeConstructor(new LoaderOptions())).load(is);
						String lang = fileName.replaceAll("-", "_");

						Map<String, String> translations = newTranslations.computeIfAbsent(lang, k -> new HashMap<>());
						for (Map.Entry<String, Object> entry : values.entrySet())
							translations.put(entry.getKey().toLowerCase(Locale.ROOT), getTranslationValue(entry));
					}
				};
			}
		} catch (IOException e) {
			throw new TownyInitException("Failed to read language override folder.", TownyInitException.TownyError.LOCALIZATION, e);
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
		// Messages blocked from being overridden.
		if (entry.getKey().toLowerCase(Locale.ROOT).startsWith("msg_ptw_warning")) {
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

	void createReadmeFile() {
		try (InputStream resourceAsStream = clazz.getResourceAsStream("/README - Translations.txt")) {
			if (resourceAsStream == null)
				return;

			Path readmePath = langFolderPath.resolve("README - Translations.txt");
			if (Files.exists(readmePath))
				return;
			if (!FileMgmt.checkOrCreateFile(readmePath.toString()))
				throw new TownyInitException("Failed to touch '" + readmePath + "'.", TownyInitException.TownyError.LOCALIZATION);
			try {
				Files.copy(resourceAsStream, readmePath, StandardCopyOption.REPLACE_EXISTING);
			} catch (FileAlreadyExistsException ignored) {
				// Should not be possible.
			} catch (IOException e) {
				throw new TownyInitException("Failed to copy README - Translations.txt from the JAR to '" + readmePath + "'", TownyInitException.TownyError.LOCALIZATION, e);
			}
		} catch (IOException ignored) {}
	}
	
	/*
	 * Global.yml methods.
	 */
	
	/**
	 * Creates and loads a global.yml if one is present in the plugin's jar.
	 */
	void loadGlobalFile() {
		try {
			Path globalYMLPath = langFolderPath.resolve("override").resolve("global.yml");
			Path glitchedGlobalPath = langFolderPath.resolve("global.yml");

			// Move any old global.yml files that are in the wrong location.
			if (Files.exists(glitchedGlobalPath)) {
				Files.move(glitchedGlobalPath, globalYMLPath, StandardCopyOption.REPLACE_EXISTING);
			}

			// Create the global.yml file if it doesn't exist.
			if (!Files.exists(globalYMLPath)) {
				try (InputStream resourceAsStream = clazz.getResourceAsStream("/global.yml")) {
					if (resourceAsStream == null) {
						// No global.yml file exists 
						return;
					}

					createGlobalYML(globalYMLPath, resourceAsStream);
				}
			}

			// Load global override file into memory.
			Map<String, Object> globalOverrides = loadGlobalFile(globalYMLPath);
			// Can be null if no overrides have been added
			if (globalOverrides != null)
				overwriteKeysWithGlobalOverrides(globalOverrides);
		} catch (IOException ignored) {}
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
			globalOverrides = new Yaml(new SafeConstructor(new LoaderOptions())).load(is);
		} catch (Exception e) {
			plugin.getLogger().log(Level.WARNING, "An exception occurred while reading the global.yml file", e);
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
				newTranslations.get(lang).put(entry.getKey().toLowerCase(Locale.ROOT), getTranslationValue(entry));
			}
	}

	/**
	 * @param update Whether to create and update reference files.
	 */
	public void updateReferenceFiles(final boolean update) {
		this.updateReferenceFiles = update;
	}
}
