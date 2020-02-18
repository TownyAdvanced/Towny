package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nameable;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
		
}
