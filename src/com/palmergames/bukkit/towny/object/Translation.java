package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.HelpMenu;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.FileMgmt;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.configuration.InvalidConfigurationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	public static CommentedConfiguration language;
	private static final Map<String, String> oldLangFileNames = createLegacyLangMap();
	private static final Set<String> langFiles = new HashSet<>(Arrays.asList("da-DK", "de-DE", "en-US", "es-419", "es-ES", "fr-FR", "id-ID", "it-IT", "ko-KR", "nl-NL", "no-NO", "pl-PL", "pt-BR", "ru-RU", "sv-SE", "tr-TR", "zh-CN", "zh-TW"));
	private static TranslationRegistry registry;
	
	public static void loadTranslationRegistry() {
		if (registry != null)
			GlobalTranslator.get().removeSource(registry);
		
		registry = TranslationRegistry.create(Key.key("towny", "main"));		
		registry.defaultLocale(toLocale(TownySettings.getString(ConfigNodes.LANGUAGE)));
		
		for (String lang : langFiles) {
			try (InputStream is = Translation.class.getResourceAsStream("/lang/" + lang + ".yml")) {
				Map<String, Object> values = new Yaml(new SafeConstructor()).load(is);
				Locale locale = toLocale(lang);
				
				for (Map.Entry<String, Object> entry : values.entrySet()) {
					if (!(entry.getValue() instanceof String))
						continue;
					
					try {
						registry.register(entry.getKey(), locale, new MessageFormat((String) entry.getValue()));
					} catch (IllegalArgumentException e) {
						Towny.getPlugin().getLogger().warning("The string '" + entry.getValue() + "' is not a valid MessageFormat. Key: " + entry.getKey() + " | Locale: " + locale);
					}
				}
			} catch (Exception e) {
				// An IO exception occured, or the file had invalid yaml
				e.printStackTrace();
			}
		}
		
		//TODO: allow languages to be added/overridden
		
		GlobalTranslator.get().addSource(registry);
	}

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage(String filepath, String defaultRes) throws IOException {

		updateLegacyLangFileName(TownySettings.getString(ConfigNodes.LANGUAGE));
		
		String res = TownySettings.getString(ConfigNodes.LANGUAGE.getRoot(), defaultRes);
		String fullPath = filepath + File.separator + res;
		File file = FileMgmt.unpackResourceFile(fullPath, "lang/" + res, defaultRes);

		// read the (language).yml into memory
		language = new CommentedConfiguration(file);
		language.load();
		HelpMenu.loadMenus();
		CommentedConfiguration newLanguage = new CommentedConfiguration(file);
		
		try {
			newLanguage.loadFromString(FileMgmt.convertStreamToString("/lang/" + res));
		} catch (IOException e) {
			Towny.getPlugin().getLogger().info("Lang: Custom language file detected, not updating.");
			Towny.getPlugin().getLogger().info("Lang: " + res + " v" + Translation.of("version") + " loaded.");
			return;
		} catch (InvalidConfigurationException e) {
			TownyMessaging.sendMsg("Invalid Configuration in language file detected.");
		}
		
		String resVersion = newLanguage.getString("version");
		String langVersion = Translation.of("version");

		if (!langVersion.equalsIgnoreCase(resVersion)) {
			language = newLanguage;
			Towny.getPlugin().getLogger().info("Lang: Language file replaced with updated version.");
			FileMgmt.stringToFile(FileMgmt.convertStreamToString("/lang/" + res), file);
		}
		Towny.getPlugin().getLogger().info("Lang: " + res + " v" + Translation.of("version") + " loaded.");
	}
	
	/**
	 * Translates given key into its respective language. 
	 * 
	 * @param key The language key.
	 * @return The localized string.
	 */
	public static String of(String key) {
		String data = language.getString(key.toLowerCase());

		if (data == null) {
			TownySettings.sendError(key.toLowerCase() + " from " + TownySettings.getString(ConfigNodes.LANGUAGE));
			return "";
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

	private Translation() {}

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
	
	private static Locale toLocale(String fileName) {
		int lastIndex = fileName.lastIndexOf(".") == -1 ? fileName.length() : fileName.lastIndexOf(".");
		try {
			String[] locale = fileName.substring(0, lastIndex).split("-");
			return new Locale(locale[0], locale[1]);
		} catch (Exception e) {
			return Locale.ENGLISH;
		}
	}
}