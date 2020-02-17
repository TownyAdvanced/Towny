package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nameable;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

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

	/**
	 * A helper function that extracts names from objects.
	 * 
	 * @param objs The Nameable objects to get the names from.
	 * @return A list of the names of the objects.
	 */
	public static List<String> getNames(Collection<Town> objs) {

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

		return getTownNames().stream().filter(name -> name.startsWith(str)).collect(Collectors.toList());
	}
	
	public static List<String> filterByStart(List<String> list, String startingWith) {
		return list.stream().filter(name -> name.startsWith(startingWith)).collect(Collectors.toList());
	}
		
}
