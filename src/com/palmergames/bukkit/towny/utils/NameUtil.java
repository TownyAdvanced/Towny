package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nameable;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.util.Trie;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A helper class to extract name data from classes.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class NameUtil {
	
	private static final int MAX_RETURNS = 50;

	/**
	 * A helper function that extracts names from objects.
	 * 
	 * @param objs The Nameable objects to get the names from.
	 * @return A list of the names of the objects.
	 */
	public static <T extends Nameable> List<String> getNames(Collection<T> objs) {

		ArrayList<String> names = new ArrayList<>();
		
		for (Nameable obj : objs) {
			if (obj.getName() == null) {
				continue;
			}
			names.add(obj.getName());
		}
		
		return names;
	}
	
	public static List<String> getTownResidentNamesOfPlayer(Player player) {
		try {
			return getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getResidents());
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}
	
	public static List<String> getTownResidentNamesOfPlayerStartingWith(Player player, String str){
		return filterByStart(getTownResidentNamesOfPlayer(player), str);
	}
	
	public static List<String> getNationResidentNamesOfPlayer(Player player) {
		try {
			return getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getNation().getResidents());
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}
	
	public static List<String> getNationResidentNamesOfPlayerStartingWith(Player player, String str) {
		return filterByStart(getNationResidentNamesOfPlayer(player), str);
	}
	
	public static List<String> getTownNamesOfPlayerNation(Player player) {
		try {
			return getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getNation().getTowns());
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}
	
	public static List<String> getTownNamesOfPlayerNationStartingWith(Player player, String str) {
		return filterByStart(getTownNamesOfPlayerNation(player), str);
	}
	
	public static List<String> getTownNames() {
		Collection<Town> towns = TownyUniverse.getInstance().getTownsMap().values();
		return getNames(towns);
	}
	
	public static List<String> getTownNamesStartingWith(String str) {
		return filterByStart(getTownNames(), str);
	}
	
	public static List<String> filterByStart(List<String> list, String startingWith) {
		return list.stream().filter(name -> name.toLowerCase().startsWith(startingWith.toLowerCase())).limit(MAX_RETURNS).collect(Collectors.toList());
	}
	
	public static List<String> getNationNames() {
		Collection<Nation> nations = TownyUniverse.getInstance().getNationsMap().values();
		return getNames(nations);
	}
	
	public static List<String> getNationNamesStartingWith(String str) {
		return filterByStart(getNationNames(), str);
	}
	
	public static List<String> getWorldNames() {
		Collection<TownyWorld> worlds = TownyUniverse.getInstance().getWorldMap().values();
		return getNames(worlds);
	}
	
	public static List<String> getWorldNamesStartingWith(String str) {
		return filterByStart(getWorldNames(), str);
	}

	/**
	 * Returns a List<String> containing strings of resident, town, and/or nation names that match with arg.
	 * Can check for multiple types, for example "rt" would check for residents and towns but not nations or worlds.
	 * 
	 * @param arg the string to match with the chosen type
	 * @param type the type of Towny object to check for, can be r(esident), t(own), n(ation), w(orld), or any combination of those to check
	 * @return Matches for the arg with the chosen type
	 */
	public static List<String> getTownyStartingWith(String arg, String type) {
		long start = System.nanoTime();
		List<String> matches = new ArrayList<>();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		if (type.contains("r")) {
			for (String string:townyUniverse.getResidentsTrie().getStringsFromKey(arg)) {
				matches.add(string);
			}
		}
		
		if (type.contains("t")) {
			for (String string:townyUniverse.getTownsTrie().getStringsFromKey(arg)) {
				matches.add(string);
			}
		}
		
		if (type.contains("n")) {
			for (String string:townyUniverse.getNationsTrie().getStringsFromKey(arg)) {
				matches.add(string);
			}
		}
		
		if (type.contains("w")) {
			matches.addAll(filterByStart(NameUtil.getNames(townyUniverse.getWorldMap().values()), arg));
		}
		
		System.out.println("Found "+matches.size()+" for "+type+" in "+(System.nanoTime()-start)/1000000+"ms");
		return matches;
	}

	/**
	 * Checks if arg starts with filters, if not return matches from {@link #getTownyStartingWith(String, String)}. 
	 * Add a "+" to the type to return both cases
	 * 
	 * @param filters the strings to filter arg with
	 * @param arg the string to check with filters and possibly match with Towny objects if no filters are found
	 * @param type the type of check to use, see {@link #getTownyStartingWith(String, String)} for possible types. Add "+" to check for both filters and {@link #getTownyStartingWith(String, String)}
	 * @return Matches for the arg filtered by filters or checked with type
	 */
	public static List<String> filterByStartOrGetTownyStartingWith(List<String> filters, String arg, String type) {
		List<String> filtered = filterByStart(filters, arg);
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
