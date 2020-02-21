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
	 * Returns a List<String> containing strings of resident, town, and/or nation names that match with str
	 * @param str String to match, usually a command argument
	 * @param type  Can be r, t, n, or any combination of those to check. For example, passing "rt" to type would check residents and towns but not nations
	 * @return The matches for the str with the chosen type
	 */
	public static List<String> getTownyStartingWith(String str, String type) {
		long start = System.nanoTime();
		List<String> matches = new ArrayList<>();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		if (type.contains("r")) {
			Map<Trie.TrieNode, String> residentsTrieNode = townyUniverse.getResidentsTrieNode();
			for (Trie.TrieNode trieNode:townyUniverse.getResidentsTrie().getTrieNodeLeavesFromKey(str)) {
				matches.add(residentsTrieNode.get(trieNode));
			}
		}
		
		if (type.contains("t")) {
			Map<Trie.TrieNode, String> townsTrieNode = townyUniverse.getTownsTrieNode();
			for (Trie.TrieNode trieNode:townyUniverse.getTownsTrie().getTrieNodeLeavesFromKey(str)) {
				matches.add(townsTrieNode.get(trieNode));
			}
		}
		
		if (type.contains("n")) {
			Map<Trie.TrieNode, String> nationsTrieNode = townyUniverse.getNationsTrieNode();
			for (Trie.TrieNode trieNode:townyUniverse.getNationsTrie().getTrieNodeLeavesFromKey(str)) {
				matches.add(nationsTrieNode.get(trieNode));
			}
		}
		System.out.println("Found "+matches.size()+" for "+type+" in "+(System.nanoTime()-start)/1000000+"ms");
		return matches;
	}
}
