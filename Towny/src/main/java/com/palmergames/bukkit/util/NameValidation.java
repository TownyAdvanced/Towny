package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.util.StringMgmt;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.VisibleForTesting;

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
					"plotgrouplist","trust","purge","leader","baltop","all","help", "spawn", "takeoverclaim", "ban", "unjail",
					"trusttown","forsale","fs","notforsale","nfs","buytown","sanctiontown","create"));
	}

	/**
	 * Check and perform regex on player names
	 * 
	 * @param name of a player in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the player name is invalid.
	 */
	public static String checkAndFilterPlayerName(String name) throws InvalidNameException {

		String out = filterName(name);

		testForBadSymbolsAndThrow(name);

		if (!isNameAllowedViaRegex(out))
			throw new InvalidNameException(out + " contains characters which aren't allowed in the Towny regex settings.");

		return out;
	}

	/**
	 * Check and perform regex on town names
	 * 
	 * @param name of a Town object in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the Town name is invalid.
	 */
	public static String checkAndFilterTownNameOrThrow(String name) throws InvalidNameException {
		String out = filterName(name);

		testNameLengthAndThrow(out);

		testForNumbersAndThrow(out);

		testForImproperNameAndThrow(out);

		testForSubcommandAndThrow(out);

		if (out.startsWith(TownySettings.getTownAccountPrefix()))
			throw new InvalidNameException(out + " begins with letters used in the economy features, you cannot use this name.");

		if (TownyUniverse.getInstance().hasTown(out))
			throw new InvalidNameException(out + " is a name that is already in use.");

		return out;
	}

	/**
	 * Check and perform regex on nation names
	 * 
	 * @param name of a Nation object in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the Nation name is invalid.
	 */
	public static String checkAndFilterNationNameOrThrow(String name) throws InvalidNameException {
		String out = filterName(name);

		testNameLengthAndThrow(out);

		testForNumbersAndThrow(out);

		testForImproperNameAndThrow(out);

		testForSubcommandAndThrow(out);

		if (out.startsWith(TownySettings.getNationAccountPrefix()))
			throw new InvalidNameException(out + " begins with letters used in the economy features, you cannot use this name.");

		if (TownyUniverse.getInstance().hasNation(out))
			throw new InvalidNameException(out + " is a name that is already in use.");

		return out;
	}

	/**
	 * Check and perform regex on any town and nations names
	 * 
	 * @param name of a Government object in {@link String} format.
	 * @param gov the Government to validate.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the Government name is invalid.
	 */
	public static String checkAndFilterGovernmentNameOrThrow(String name, Government gov) throws InvalidNameException {
		if (gov instanceof Town)
			return checkAndFilterTownNameOrThrow(name);

		if (gov instanceof Nation)
			return checkAndFilterNationNameOrThrow(name);

		return name;
	}

	/**
	 * Check and perform regex on plot names
	 * 
	 * @param name of a TownBlock object in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the TownBlock name is invalid.
	 */
	public static String checkAndFilterPlotNameOrThrow(String name) throws InvalidNameException {
		name = filterName(name);

		testNameLengthAndThrow(name);

		testForImproperNameAndThrow(name);

		return name;
	}

	/**
	 * Check and perform regex on plotgroup names
	 * 
	 * @param name of a PlotGroup object in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the PlotGroup name is invalid.
	 */
	public static String checkAndFilterPlotGroupNameOrThrow(String name) throws InvalidNameException {
		return checkAndFilterPlotNameOrThrow(filterCommas(name));
	}

	/**
	 * Check and perform regex on Titles and Surnames given to residents.
	 * 
	 * @param words an Array of strings that make up the title or surname.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the title or surname is invalid.
	 */	
	public static String checkAndFilterTitlesSurnameOrThrow(String[] words) throws InvalidNameException {
		for (String word : words)
			testForConfigBlacklistedNameAndThrow(word);

		String title = StringMgmt.join(NameValidation.filterNameArray(words));

		if (title.length() > TownySettings.getMaxTitleLength())
			throw new InvalidNameException(title + " is too long to use as a title or surname.");

		testForEmptyAndThrow(title);

		return title;
	}

	/**
	 * Check and perform regex on Tags given to towns and nations.
	 * 
	 * @param tag the Tag which was submitted by the user.
	 * @return String of the valid tag result.
	 * @throws TownyException if the title or surname is invalid.
	 */
	public static String checkAndFilterTagOrThrow(String tag) throws TownyException {
		tag = filterName(tag);

		if (tag.length() > TownySettings.getMaxTagLength())
			throw new TownyException(Translatable.of("msg_err_tag_too_long"));

		testForEmptyAndThrow(tag);

		testAllUnderscoresAndThrow(tag);

		testForImproperNameAndThrow(tag);

		return tag;
	}

	/**
	 * Used in validating the strings saved for town and nation boards and resident about sections.
	 * 
	 * @param message String needing validation.
	 * @return true if the message is allowed.
	 */
	public static boolean isValidBoardString(String message) {
		try {
			testForBadSymbolsAndThrow(message);

			String[] words = message.split(" ");
			for (String word : words)
				testForConfigBlacklistedNameAndThrow(word);
		} catch (InvalidNameException e1) {
			return false;
		}

		try {
			if (stringPattern == null)
				stringPattern = Pattern.compile(TownySettings.getStringCheckRegex(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
			return stringPattern.matcher(message).find();
		} catch (PatternSyntaxException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "Failed to compile the string check regex pattern because it contains errors (" + TownySettings.getStringCheckRegex() + ")", e);
			return false;
		}
	}

	/**
	 * Stops Names which are:
	 * empty, in the config blacklist, all underscores, containing
	 * bad symbols, using characters not in the name regex.
	 * 
	 * @param name Name to validate.
	 * @throws InvalidNameException when the name is now allowed.
	 */
	private static void testForImproperNameAndThrow(String name) throws InvalidNameException {

		testForEmptyAndThrow(name);

		testForConfigBlacklistedNameAndThrow(name);

		testAllUnderscoresAndThrow(name);

		testForBadSymbolsAndThrow(name);

		if (!isNameAllowedViaRegex(name))
			throw new InvalidNameException(name + " contains characters which aren't allowed in the Towny regex settings.");
	}

	/**
	 * Stops any empty strings passing through.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when name is an empty String.
	 */
	private static void testForEmptyAndThrow(String name) throws InvalidNameException {
		if (name.isEmpty())
			throw new InvalidNameException(name + " is empty.");
	}

	/**
	 * Does this name not pass the config blacklist at plugin.name_blacklist
	 * 
	 * @param name String name to check.
	 * @throws InvalidNameException if the string is blacklisted in the config.
	 */
	private static void testForConfigBlacklistedNameAndThrow(String name) throws InvalidNameException {
		if(!name.isEmpty() && TownySettings.getBlacklistedNames().stream().anyMatch(name::equalsIgnoreCase))
			throw new InvalidNameException(name + " is not permitted.");
	}

	/**
	 * Stop objects being named with underscores, which Towny will filter into
	 * spaces in some occaissions.
	 * 
	 * @param name String submitted for testing.
	 * @throws InvalidNameException when the name is entirely underscores.
	 */
	private static void testAllUnderscoresAndThrow(String name) throws InvalidNameException {
		for (char letter : name.toCharArray())
			if (letter != '_')
				return;
		throw new InvalidNameException(name + " is entirely underscores.");
	}

	/**
	 * Stops escape characters being used, something that could harm mysql if things weren't sanitized.
	 * 
	 * @param message String to validate.
	 * @throws InvalidNameException when escape characters are present. 
	 */
	private static void testForBadSymbolsAndThrow(String message) throws InvalidNameException {
		if (message.contains("'") || message.contains("`"))
			throw new InvalidNameException(message + " contains symbols that could be harmful.");
	}

	/**
	 * Stops town and nation subcommands being used as town and nation names.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when a name is used as a subcommand.
	 */
	private static void testForSubcommandAndThrow(String name) throws InvalidNameException {
		if (isBannedName(name))
			throw new InvalidNameException(name + " is used in the command structure, you cannot use this name.");
	}

	/**
	 * Is this name too long for the config, set at
	 * filters_colour_chat.modify_chat.max_name_length
	 * 
	 * @param name String to check
	 * @throws InvalidNameException if the name is too long.
	 */
	private static void testNameLengthAndThrow(String name) throws InvalidNameException {
		if (name.length() > TownySettings.getMaxNameLength())
			throw new InvalidNameException(name + " is too long to use as a name.");
	}

	/**
	 * Stops numbers in town and nation names, when these are disallowed.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when numbers aren't allowed and they are present.
	 */
	private static void testForNumbersAndThrow(String name) throws InvalidNameException {
		if (TownySettings.areNumbersAllowedInNationNames() && numberPattern.matcher(name).find())
			throw new InvalidNameException(name + " contains numbers which aren't allowed in names.");
	}

	/**
	 * Is this a valid name via getNameCheckRegex
	 *
	 * @param name - {@link String} containing a name from getNameCheckRegex
	 * @return true if this name is valid.
	 */
	private static boolean isNameAllowedViaRegex(String name) {
		try {
			if (namePattern == null)
				namePattern = Pattern.compile(TownySettings.getNameCheckRegex(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
			return namePattern.matcher(name).find();
		} catch (PatternSyntaxException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "Failed to compile the name check regex pattern because it contains errors (" + TownySettings.getNameCheckRegex() + ")", e);
			return false;
		}
	}

	/**
	 * Filters out characters that match the NameFilterRegex, NameRemoveRegex and
	 * the &k symbol.
	 * 
	 * @param input String to filter
	 * @return filtered String.
	 */
	private static String filterName(String input) {
		return input.replaceAll(TownySettings.getNameFilterRegex(), "_").replaceAll(TownySettings.getNameRemoveRegex(), "").replace("&k", "");
	}

	/**
	 * Perform regex on all names passed and return the results.
	 * 
	 * @param arr - Array of names
	 * @return string array of the filtered names.
	 */
	private static String[] filterNameArray(String[] arr) {
		
		int count = 0;

		for (String word : arr) {
			arr[count] = filterName(word);
			count++;
		}

		return arr;
	}

	/**
	 * Used to sanitize commas from plot group names.
	 * 
	 * @param input String to filter.
	 * @return filtered String with commas made into _'s
	 */
	private static String filterCommas(String input) {
		return input.replace(",", "_");
	}

	/**
	 * Does this name match one of the bannedNames which are usually town and nation
	 * subcommands.
	 * 
	 * @param name String to check.
	 * @return true if this is a banned name.
	 */
	@VisibleForTesting
	public static boolean isBannedName(String name) {
		return bannedNames.contains(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Check and perform getNameCheckRegex on any town/nation names
	 * 
	 * @param name - Town/Nation name {@link String}
	 * @return result of getNameCheckRegex
	 * @throws InvalidNameException if the name parsed is blacklisted
	 * @deprecated 0.100.1.5 use any of the other checkAndFilter methods found in this class.
	 */
	@Deprecated
	public static String checkAndFilterName(String name) throws InvalidNameException {

		String out = filterName(name);
		testForEmptyAndThrow(out);
		testAllUnderscoresAndThrow(out);
		testForImproperNameAndThrow(out);

		return out;
	}
}
