package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nameable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A helper class to extract name data from classes and filter matching strings.
 * 
 * @author Suneet Tipirneni (Siris)
 * @author stzups
 */
public class NameUtil {
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

	/**
	 * Returns strings that start with a string
	 * 
	 * @param list strings to check
	 * @param startingWith string to check with list
	 * @return strings from list that start with startingWith
	 */
	public static List<String> filterByStart(List<String> list, String startingWith) {
		return list.stream().filter(name -> name.toLowerCase().startsWith(startingWith.toLowerCase())).collect(Collectors.toList());
	}

	/**
	 * Returns the names a player's town's residents that start with a string
	 *
	 * @param player the player to get the town's residents of
	 * @param str the string to check if the town's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getTownResidentNamesOfPlayerStartingWith(Player player, String str){
		try {
			return filterByStart(getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getResidents()), str);
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the names a player's nation's residents that start with a string
	 *
	 * @param player the player to get the nation's residents of
	 * @param str the string to check if the nation's residents start with
	 * @return the resident names that match str
	 */
	public static List<String> getNationResidentNamesOfPlayerStartingWith(Player player, String str) {
		try {
			return filterByStart(getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getNation().getResidents()), str);
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the names of towns of a player that start with a string
	 *
	 * @param player the player to get the towns of
	 * @param str the string to check if the town names start with
	 * @return the town names that match str
	 */
	public static List<String> getTownNamesOfPlayerNationStartingWith(Player player, String str) {
		try {
			return filterByStart(getNames(TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getNation().getTowns()), str);
		} catch (TownyException e) {
			return Collections.emptyList();
		}
	}
}
