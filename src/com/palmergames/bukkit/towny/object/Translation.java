package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.util.StringMgmt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	private static class LanguageContext {
		final String language;
		final String country;
		
		LanguageContext(String language, String country) {
			this.language = language;
			this.country = country;
		}
	}
	
	public static ResourceBundle language;
	private static final Map<String, LanguageContext> langKeys = new HashMap<>();
	
	static {
		langKeys.put("english", new LanguageContext("en", "US"));
	}

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage() throws IOException {
		
		String configVal = TownySettings.getString(ConfigNodes.LANGUAGE).replace(".yml", "");
		TownyMessaging.sendErrorMsg("configVal = " + configVal);
		LanguageContext context = langKeys.get(configVal);

		Locale locale = new Locale(context.language, context.country);
		language = ResourceBundle.getBundle("translation", locale);

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
		
		// Covert to UTF-8 format
		String retVal = new String(data.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
		
		return StringMgmt.translateHexColors(parseSingleLineString(retVal));
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