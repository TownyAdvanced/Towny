package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.HelpMenu;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.FileMgmt;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	public static CommentedConfiguration language;
	private static Map<String, String> oldLangFileNames = createLegacyLangMap();

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
		Map<String, String> oldLangFileNames = new HashMap<String, String>();
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
		oldLangFileNames.put("chinese.yml", "zh-CN.yml");
		return oldLangFileNames;
	}
}