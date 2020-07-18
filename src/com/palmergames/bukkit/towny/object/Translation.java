package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	public static ResourceBundle language;

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage(String filepath, String defaultRes) throws IOException {

		Locale locale_it_IT = new Locale("zh", "CN");
		ResourceBundle resourceBundle = ResourceBundle.getBundle("translation", locale_it_IT);
		String val = resourceBundle.getString("msg_player_put_group_up_for_sale");
		String thin = new String(val.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

//		String res = TownySettings.getString(ConfigNodes.LANGUAGE.getRoot(), defaultRes);
//		String fullPath = filepath + File.separator + res;
//		File file = FileMgmt.unpackResourceFile(fullPath, res, defaultRes);
//
//		// read the (language).yml into memory
//		language = new CommentedConfiguration(file);
//		language.load();
//		CommentedConfiguration newLanguage = new CommentedConfiguration(file);
//		
//		try {
//			newLanguage.loadFromString(FileMgmt.convertStreamToString("/" + res));
//		} catch (IOException e) {
//			TownyMessaging.sendMsg("Custom language file detected, not updating.");
//			return;
//		} catch (InvalidConfigurationException e) {
//			TownyMessaging.sendMsg("Invalid Configuration in language file detected.");
//		}
//		
//		String resVersion = newLanguage.getString("version");
//		String langVersion = Translation.of("version");
//
//		if (!langVersion.equalsIgnoreCase(resVersion)) {
//			language = newLanguage;
//			TownyMessaging.sendMsg("Newer language file available, language file updated.");
//			FileMgmt.stringToFile(FileMgmt.convertStreamToString("/" + res), file);
//		}
	}

	private static String parseSingleLineString(String str) {
		return NameUtil.translateColorCodes(str);
	}
	
	/**
	 * Translates give key into its respective language. 
	 * 
	 * @param key The language key.
	 * @return The localized string.
	 */
	public static String of(String key) {
		String data;
		try {
			data = language.getString(key.toLowerCase());
		} catch (MissingResourceException e) {
			TownySettings.sendError(key.toLowerCase() + " from " + TownySettings.getString(ConfigNodes.LANGUAGE));
			return "";
		}
		
		return StringMgmt.translateHexColors(parseSingleLineString(data));
	}

	/**
	 * Translates give key into its respective language. 
	 *
	 * @param key The language key.
	 * @param args The arguments to format the localized string.   
	 * @return The localized string.
	 */
	public static String of(String key, Object... args) {
		return String.format(of(key), args);
	}

	private Translation() {}
}