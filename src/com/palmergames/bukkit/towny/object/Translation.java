package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.HelpMenu;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * A convenience object to facilitate translation. 
 */
public final class Translation {
	
	private static final HashMap<String, Locale> legacyKeys = new HashMap<>(12);
	
	static {
		legacyKeys.put("english.yml", Locale.getDefault());
		legacyKeys.put("chinese.yml", new Locale("zh", "CN"));
		legacyKeys.put("es-mx.yml", new Locale("es", "MX"));
		legacyKeys.put("french.yml", new Locale("fr", "FR"));
		legacyKeys.put("italian.yml", new Locale("it", "IT"));
		legacyKeys.put("norwegian.yml", new Locale("no", "NO"));
		legacyKeys.put("pt-br.yml", new Locale("pr","BR"));
		legacyKeys.put("korean.yml", new Locale("ko"));
		legacyKeys.put("german.yml", new Locale("de", "DE"));
		legacyKeys.put("russian.yml", new Locale("ru", "RU"));
		legacyKeys.put("spanish.yml", new Locale("es"));
		legacyKeys.put("polish.yml", new Locale("pl", "PL"));
		legacyKeys.put("english", Locale.getDefault());
	}
	
	public static ResourceBundle language;

	// This will read the language entry in the config.yml to attempt to load
	// custom languages
	// if the file is not found it will load the default from resource
	public static void loadLanguage() throws IOException {
		
		String val = TownySettings.getString(ConfigNodes.LANGUAGE);
		String[] configVal = val.split("_");
		
		String lang;
		String country = null;
		if (configVal.length == 2) {
			lang = configVal[0];
			country = configVal[1];
		} else {
			lang = configVal[0];
		}

		Locale locale;
		if (legacyKeys.containsKey(val)) {
			locale = legacyKeys.get(val);
		}else if (country == null) {
			locale =  new Locale(lang);
		} else {
			locale = new Locale(lang, country);
		}

		String fileName = "translation_" + locale.getLanguage() + "_"
			+ (locale.getCountry() == null ? "" : locale.getCountry()) + ".properties";
		InputStream inputStream = Towny.getPlugin().getResource(fileName);

		HelpMenu.loadMenus();
		
		if (inputStream == null) {
			loadCustomLang();
			return;
		}

		File dest = new File(Towny.getPlugin().getDataFolder() + File.separator
			+ "settings" + File.separator + fileName);

		FileMgmt.save(inputStream, dest);
		
		try {
			language = ResourceBundle.getBundle("translation", locale);
		} catch (MissingResourceException e) {
			e.printStackTrace();
		}
		
	}

	private static void loadCustomLang() throws IOException {

		String lang = TownySettings.getString(ConfigNodes.LANGUAGE);
		File langPath = new File(Towny.getPlugin().getDataFolder() + "/settings/" + lang + ".properties");

		InputStream inputStream = new FileInputStream(langPath);

		language = new PropertyResourceBundle(inputStream);
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