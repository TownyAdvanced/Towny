package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.utils.NameUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BaseCommand implements TabCompleter{
	
	private static final List<String> setPermTabCompletes = new ArrayList<>(Arrays.asList(
		"on",
		"off",
		"ally",
		"outsider",
		"build",
		"destroy",
		"switch",
		"itemuse",
		"reset"
	));

	private static final List<String> setLevelCompletes = new ArrayList<>(Arrays.asList(
		"resident",
		"ally",
		"outsider",
		"nation",
		"friend"
	));

	private static final List<String> setTypeCompletes = new ArrayList<>(Arrays.asList(
		"build",
		"destroy",
		"switch",
		"itemuse"
	));

	private static final List<String> setOnOffCompletes = new ArrayList<>(Arrays.asList(
		"on",
		"off"
	));

	private static final List<String> toggleTypeOnOffCompletes = new ArrayList<>(Arrays.asList(
		"build",
		"destroy",
		"switch",
		"itemuse",
		"on",
		"off"
	));

	private static final List<String> toggleTabCompletes = new ArrayList<>(Arrays.asList(
		"fire",
		"pvp",
		"explosion",
		"mob"
	));
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return getTownyStartingWith(args[args.length - 1], "rtn");
	}
	
	/**
	 * Returns a List<String> containing strings of resident, town, and/or nation names that match with arg.
	 * Can check for multiple types, for example "rt" would check for residents and towns but not nations or worlds.
	 *
	 * @param arg the string to match with the chosen type
	 * @param type the type of Towny object to check for, can be r(esident), t(own), n(ation), w(orld), or any combination of those to check
	 * @return Matches for the arg with the chosen type
	 */
	static List<String> getTownyStartingWith(String arg, String type) {
		
		List<String> matches = new ArrayList<>();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		if (type.contains("r")) {
			matches.addAll(townyUniverse.getResidentsTrie().getStringsFromKey(arg));
		}

		if (type.contains("t")) {
			matches.addAll(townyUniverse.getTownsTrie().getStringsFromKey(arg));
		}

		if (type.contains("n")) {
			matches.addAll(townyUniverse.getNationsTrie().getStringsFromKey(arg));
		}
		
		if (type.contains("w")) { // There aren't many worlds so check even if arg is empty
			matches.addAll(NameUtil.filterByStart(NameUtil.getNames(townyUniverse.getWorldMap().values()), arg));
		}
		
		return matches;
	}

	/**
	 * Checks if arg starts with filters, if not returns matches from {@link #getTownyStartingWith(String, String)}. 
	 * Add a "+" to the type to return both cases
	 *
	 * @param filters the strings to filter arg with
	 * @param arg the string to check with filters and possibly match with Towny objects if no filters are found
	 * @param type the type of check to use, see {@link #getTownyStartingWith(String, String)} for possible types. Add "+" to check for both filters and {@link #getTownyStartingWith(String, String)}
	 * @return Matches for the arg filtered by filters or checked with type
	 */
	static List<String> filterByStartOrGetTownyStartingWith(List<String> filters, String arg, String type) {
		List<String> filtered = NameUtil.filterByStart(filters, arg);
		if (type.contains("+")) {
			filtered.addAll(getTownyStartingWith(arg, type));
			return filtered;
		} else {
			if (filtered.size() > 0) {
				return filtered;
			} else {
				return getTownyStartingWith(arg, type);
			}
		}
	}

	/**
	 * Used for set tab completes which are common across several commands
	 * 
	 * @param args args, make sure to remove the first few irrelevant args
	 * @return tab completes matching the proper arg
	 */
	static List<String> permTabComplete(String[] args) {
		switch (args.length) {
			case 1:
				return NameUtil.filterByStart(setPermTabCompletes, args[0]);
			case 2:
				if (setTypeCompletes.contains(args[0].toLowerCase()))
					return NameUtil.filterByStart(setOnOffCompletes, args[1]);
				if (setLevelCompletes.contains(args[0].toLowerCase()))
					return NameUtil.filterByStart(toggleTypeOnOffCompletes, args[1]);
				break;
			case 3:
				return NameUtil.filterByStart(setOnOffCompletes, args[2]);
		}
		
		return Collections.emptyList();
	}

	/**
	 * Used for toggle tab completes which are common across several commands
	 * 
	 * @param args args, make sure to remove the first few irrelevant args
	 * @return tab completes matching the proper arg
	 */
	static List<String> toggleTabCompletes(String[] args) {
		if (args.length == 1) {
			return NameUtil.filterByStart(toggleTabCompletes, args[0]);
		}
		
		return Collections.emptyList();
	}
}
