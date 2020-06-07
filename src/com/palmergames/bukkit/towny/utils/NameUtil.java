package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.Nameable;
import org.bukkit.ChatColor;

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
	 * @param <T> The collection type that implements {@link Nameable}   
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
		if (list == null || startingWith == null) {
			return Collections.emptyList();
		}
		return list.stream().filter(name -> name.toLowerCase().startsWith(startingWith.toLowerCase())).collect(Collectors.toList());
	}

	public static String translateColorCodes(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}
}
