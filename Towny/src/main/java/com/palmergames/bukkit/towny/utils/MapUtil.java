package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownySettings;
import java.util.ArrayList;
import java.util.List;

public class MapUtil {

	/**
	 * Choose a random colour for a nation from the allowed list
	 *
	 * @return color in hex code format
	 */
	public static String generateRandomNationColourAsHexCode() {
		List<String> allowedColourCodes = new ArrayList<>(TownySettings.getNationColorsMap().values());
		int randomIndex = (int)(Math.random() * allowedColourCodes.size());
		return allowedColourCodes.get(randomIndex);
	}
	
	/**
	 * Choose a random colour for a town from the allowed list
	 *
	 * @return color in hex code format
	 */
	public static String generateRandomTownColourAsHexCode() {
		List<String> allowedColourCodes = new ArrayList<>(TownySettings.getTownColorsMap().values());
		int randomIndex = (int)(Math.random() * allowedColourCodes.size());
		return allowedColourCodes.get(randomIndex);
	}
}
