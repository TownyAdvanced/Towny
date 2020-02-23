package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.utils.NameUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class BaseCommand implements TabCompleter{
	
	
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
}
