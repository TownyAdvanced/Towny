package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.TownySettings;

import javax.naming.InvalidNameException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author ElgarL
 * 
 */
public class NameValidation {

	private static Pattern namePattern = null;
	private static Pattern stringPattern = null;

	/**
	 * Check and perform getNameCheckRegex on any town/nation names
	 * 
	 * @param name
	 * @return result of getNameCheckRegex
	 * @throws InvalidNameException
	 */
	public static String checkAndFilterName(String name) throws InvalidNameException {

		String out = filterName(name);

		if (isBlacklistName(out))
			throw new InvalidNameException(out + " is an invalid name.");

		return out;
	}

	/**
	 * Check and perform regex on any Player names
	 * 
	 * @param name
	 * @return String of the valid name result.
	 * @throws InvalidNameException
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
	 * @param arr
	 * @return string array of the filtered names.
	 */
	public static String[] checkAndFilterArray(String[] arr) {

		String[] out = arr;
		int count = 0;

		for (String word : arr) {
			out[count] = filterName(word);
			count++;
		}

		return out;
	}

	/**
	 * Is this name in our blacklist?
	 * If not a blacklist, call isValidName and
	 * return true if it is an invalid name.
	 * 
	 * @param name - Name to be checked for invalidility.
	 * @return true if this name is blacklist/invalid
	 */
	public static boolean isBlacklistName(String name) {

		// Max name length
		if (name.length() > TownySettings.getMaxNameLength())
			return true;
		/*
		  A list of all banned names (notably all sub commands like 'spawn'
		  used in '/town spawn')
		 */
		ArrayList<String> bannedNames = new ArrayList<String>();
		bannedNames.addAll(Arrays.asList("list", "new", "here", "help", "?", "leave", "withdraw", "deposit", "set", "toggle", "mayor", "assistant", "kick", "add", "claim", "unclaim", "title", "outpost", "ranklist", "invite", "invites", "buy", "create"));
		// Banned names
		if (bannedNames.contains(name.toLowerCase()))
			return true;

		return !isValidName(name);
	}

	public static boolean isValidName(String name) {
	
		try {
			if (namePattern == null)
				namePattern = Pattern.compile(TownySettings.getNameCheckRegex());
			return namePattern.matcher(name).find();
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			return false;
		}
		
	}
	/**
	 * Is this a valid name via getNameCheckRegex
	 * 
	 * @param name
	 * @return true if this name is valid.
	 */
	public static boolean isValidString(String name) {

		try {
			if (stringPattern == null)
				stringPattern = Pattern.compile(TownySettings.getStringCheckRegex());
			return stringPattern.matcher(name).find();
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String filterName(String input) {

		return input.replaceAll(TownySettings.getNameFilterRegex(), "_").replaceAll(TownySettings.getNameRemoveRegex(), "");
	}
}
