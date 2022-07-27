package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author ElgarL
 * 
 */
public class NameValidation {

	private static Pattern namePattern = null;
	private static Pattern stringPattern = null;
	private static final Collection<String> bannedNames;
	private static final Pattern numberPattern = Pattern.compile("\\d");
	
	static {
		bannedNames = new HashSet<>(
			Arrays.asList("here","leave","list","online","new","plots","add","kick","claim","unclaim","withdraw","delete",
					"outlawlist","deposit","outlaw","outpost","ranklist","rank","reclaim","reslist","say","set","toggle","join",
					"invite","buy","mayor","bankhistory","enemy","ally","townlist","allylist","enemylist","king","merge","jail",
					"plotgrouplist","trust","purge","leader", "baltop"));
	}

	/**
	 * Check and perform getNameCheckRegex on any town/nation names
	 * 
	 * @param name - Town/Nation name {@link String}
	 * @return result of getNameCheckRegex
	 * @throws InvalidNameException if the name parsed is blacklisted
	 */
	public static String checkAndFilterName(String name) throws InvalidNameException {

		String out = filterName(name);
		if (out.isEmpty())
			throw new InvalidNameException(name + " is an invalid name.");

		if (isBlacklistName(out))
			throw new InvalidNameException(out + " is an invalid name.");

		return out;
	}

	/**
	 * Check and perform regex on any player names
	 * 
	 * @param name of a player in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the player name is invalid.
	 */
	public static String checkAndFilterPlayerName(String name) throws InvalidNameException {

		String out = filterName(name);

		if (!isValidName(out))
			throw new InvalidNameException(out + " is an invalid name.");

		return out;
	}

	/**
	 * Perform regex on all names passed and return the results.
	 * 
	 * @param arr - Array of names
	 * @return string array of the filtered names.
	 */
	public static String[] checkAndFilterArray(String[] arr) {
		
		int count = 0;

		for (String word : arr) {
			arr[count] = filterName(word);
			count++;
		}

		return arr;
	}
	/**
	 * Is this name in our blacklist?
	 * If not a blacklist, call isValidName and
	 * return true if it is an invalid name.
	 * 
	 * @param name - Name to be checked for invalidity.
	 * @return true if this name is blacklist/invalid
	 */
	public static boolean isBlacklistName(String name) {

		// Max name length
		if (name.length() > TownySettings.getMaxNameLength())
			return true;
		
		// Economy prefixes 
		if (name.equalsIgnoreCase(TownySettings.getNationAccountPrefix()) || name.equalsIgnoreCase(TownySettings.getTownAccountPrefix()))
			return true;
		
		// A list of all banned names (notably all sub commands like 'spawn' used in '/town spawn')
		if (bannedNames.contains(name.toLowerCase()))
			return true;

		// Config's name blacklist.
		if (isConfigBlacklistedName(name))
			return true;

		// Finally, send it over to pass the regex test.
		return !isValidName(name);
	}
	
	/**
	 * Does this name not pass the config blacklist at plugin.name_blacklist
	 * @param name String name to check.
	 * @return true if this is something that isn't allowed in the config's name blacklist.
	 */
	public static boolean isConfigBlacklistedName(String name) {
		if (name.isEmpty())
			return false;
		
		return TownySettings.getBlacklistedNames().stream().anyMatch(name::equalsIgnoreCase);
	}
	
	/**
	 * Is this a valid name via getNameCheckRegex
	 *
	 * @param name - {@link String} containing a name from getNameCheckRegex
	 * @return true if this name is valid.
	 */
	public static boolean isValidName(String name) {

		// Characters that mysql might not like.
		if (name.contains("'") || name.contains("`"))
			return false;
		
		try {
			if (namePattern == null)
				namePattern = Pattern.compile(TownySettings.getNameCheckRegex(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
			return namePattern.matcher(name).find();
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	
	/**
	 * Used in validating the strings saved for town and nation boards.
	 * 
	 * @param message - String needing validation.
	 * @return approved message.
	 */
	public static boolean isValidString(String message) {
		
		if (message.contains("'") || message.contains("`")) {
			return false;
		}

		try {
			if (stringPattern == null)
				stringPattern = Pattern.compile(TownySettings.getStringCheckRegex(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
			return stringPattern.matcher(message).find();
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String filterName(String input) {

		return input.replaceAll(TownySettings.getNameFilterRegex(), "_").replaceAll(TownySettings.getNameRemoveRegex(), "").replace("&k", "");
	}
	
	public static String filterCommas(String input) {
		return input.replace(",", "_");
	}
	
	public static boolean containsNumbers(String input) {
		return numberPattern.matcher(input).find();
	}
}
