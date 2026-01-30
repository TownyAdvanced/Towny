package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.util.StringMgmt;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.NamespacedKey;

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
					"trusttown","forsale","fs","notforsale","nfs","buytown","sanctiontown","create","cede","nearby"));

		TownySettings.addReloadListener(NamespacedKey.fromString("towny:regex-patterns"), () -> {
			namePattern = null;
			stringPattern = null;
		});
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

		testForBadSymbols(name);

		if (!isNameAllowedViaRegex(out))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_invalid_characters", out));

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

		testNameLength(out);

		testForNumbersInTownName(out);

		testForImproperNameAndThrow(out);

		testCapitalization(out);

		testForSubcommand(out);

		if (out.startsWith(TownySettings.getTownAccountPrefix()))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_begins_with_eco_prefix", out));

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

		testNameLength(out);

		testForNumbersInNationName(out);

		testForImproperNameAndThrow(out);

		testCapitalization(out);

		testForSubcommand(out);

		if (out.startsWith(TownySettings.getNationAccountPrefix()))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_begins_with_eco_prefix", out));

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

		testNameLength(name);

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
	 * Check and perform regex on District names
	 * 
	 * @param name of a District object in {@link String} format.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the District name is invalid.
	 */
	public static String checkAndFilterDistrictNameOrThrow(String name) throws InvalidNameException {
		return checkAndFilterPlotNameOrThrow(filterCommas(name));
	}

	/**
	 * Check and perform regex on Titles and Surnames given to residents. Ignores minimessage tags when validating string length.
	 *
	 * @param words an Array of strings that make up the title or surname.
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the title or surname is invalid.
	 */
	public static String checkAndFilterTitlesSurnameOrThrow(String[] words) throws InvalidNameException {
		return checkAndFilterTitlesSurnameOrThrow(words, false);
	}
	
	/**
	 * Check and perform regex on Titles and Surnames given to residents.
	 * 
	 * @param words an Array of strings that make up the title or surname.
	 * @param countColors whether to count minimessage tags in the string length
	 * @return String of the valid name result.
	 * @throws InvalidNameException if the title or surname is invalid.
	 */	
	public static String checkAndFilterTitlesSurnameOrThrow(String[] words, boolean countColors) throws InvalidNameException {
		String title = StringMgmt.join(NameValidation.filterNameArray(words));

		testForConfigBlacklistedName(title);
		
		int textLength = countColors ? title.length() : TownyComponents.plain(TownyComponents.miniMessage(title)).length();

		if (textLength > TownySettings.getMaxTitleLength())
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_title_too_long", title));

		return TownyComponents.stripClickTags(title);
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

		testForEmptyName(tag);

		testAllUnderscores(tag);

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
			testForBadSymbols(message);

			testForConfigBlacklistedName(message);
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
	 * @throws InvalidNameException when the name is not allowed.
	 */
	public static void testForImproperNameAndThrow(String name) throws InvalidNameException {

		testForEmptyName(name);

		testForConfigBlacklistedName(name);

		testAllUnderscores(name);

		testForBadSymbols(name);

		if (!isNameAllowedViaRegex(name))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_invalid_characters", name));
	}

	/**
	 * Stops any empty strings passing through.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when name is an empty String.
	 */
	private static void testForEmptyName(String name) throws InvalidNameException {
		if (name.isEmpty())
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_is_empty"));
	}

	/**
	 * Does this name not pass the config blacklist at plugin.name_blacklist
	 * 
	 * @param line String to check.
	 * @throws InvalidNameException if the string is blacklisted in the config.
	 */
	private static void testForConfigBlacklistedName(String line) throws InvalidNameException {
		String[] words = line.split(" ");
		for (String word : words)
			if(!word.isEmpty() && TownySettings.getBlacklistedNames().stream().anyMatch(word::equalsIgnoreCase))
				throw new InvalidNameException(Translatable.of("msg_err_name_validation_is_not_permitted", word));
	}

	/**
	 * Stop objects being named with underscores, which Towny will filter into
	 * spaces in some occaissions.
	 * 
	 * @param name String submitted for testing.
	 * @throws InvalidNameException when the name is entirely underscores.
	 */
	private static void testAllUnderscores(String name) throws InvalidNameException {
		for (char letter : name.toCharArray())
			if (letter != '_')
				return;
		throw new InvalidNameException(Translatable.of("msg_err_name_validation_is_all_underscores", name));
	}

	/**
	 * Stops objects being named with too many capital letters.
	 * 
	 * @param name String submitted for testing.
	 * @throws InvalidNameException when the name uses too many capital letters.
	 */
	private static void testCapitalization(String name) throws InvalidNameException {
		int maxCapitals = TownySettings.getMaxNameCapitalLetters();
		if (maxCapitals == -1)
			return;

		int capitals = 0;
		boolean skip = true;
		for (char letter : name.toCharArray()) {
			if (skip) { // First character of the name or character after a _.
				skip = false;
				continue;
			}

			if (letter == '_') { // Next character will be allowed to be capitalized.
				skip = true;
				continue;
			}

			if (Character.isLowerCase(letter)) // Not a capital.
				continue;

			capitals++;
		}

		if (capitals > maxCapitals)
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_too_many_capitals", name, capitals, maxCapitals));
	}

	/**
	 * Stops escape characters being used, something that could harm mysql if things weren't sanitized.
	 * 
	 * @param message String to validate.
	 * @throws InvalidNameException when escape characters are present. 
	 */
	private static void testForBadSymbols(String message) throws InvalidNameException {
		if (message.contains("'") || message.contains("`"))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_contains_harmful_characters", message));
	}

	/**
	 * Stops town and nation subcommands being used as town and nation names.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when a name is used as a subcommand.
	 */
	private static void testForSubcommand(String name) throws InvalidNameException {
		if (isBannedName(name))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_used_in_command_structure", name));
	}

	/**
	 * Is this name too long for the config, set at
	 * filters_colour_chat.modify_chat.max_name_length
	 * 
	 * @param name String to check
	 * @throws InvalidNameException if the name is too long.
	 */
	private static void testNameLength(String name) throws InvalidNameException {
		if (name.length() > TownySettings.getMaxNameLength())
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_name_too_long", name));
	}

	/**
	 * Stops numbers in nation names, when these are disallowed.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when numbers aren't allowed and they are present.
	 */
	private static void testForNumbersInNationName(String name) throws InvalidNameException {
		if (!TownySettings.areNumbersAllowedInNationNames() && nameContainsNumbers(name))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_contains_numbers", name));
	}
	
	/**
	 * Stops numbers in town names, when these are disallowed.
	 * 
	 * @param name String to validate.
	 * @throws InvalidNameException thrown when numbers aren't allowed and they are present.
	 */
	private static void testForNumbersInTownName(String name) throws InvalidNameException {
		if (!TownySettings.areNumbersAllowedInTownNames() && nameContainsNumbers(name))
			throw new InvalidNameException(Translatable.of("msg_err_name_validation_contains_numbers", name));
	}

	private static boolean nameContainsNumbers(String name) {
		return numberPattern.matcher(name).find();
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
	static boolean isBannedName(String name) {
		return bannedNames.contains(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Check and perform getNameCheckRegex on any town/nation names
	 * 
	 * @param name - Town/Nation name {@link String}
	 * @return result of getNameCheckRegex
	 * @throws InvalidNameException if the name parsed is blacklisted
	 * @deprecated 0.100.1.10 use any of the other checkAndFilter methods found in this class.
	 */
	@Deprecated
	public static String checkAndFilterName(String name) throws InvalidNameException {

		String out = filterName(name);
		testForEmptyName(out);
		testAllUnderscores(out);
		testForImproperNameAndThrow(out);

		return out;
	}
}
