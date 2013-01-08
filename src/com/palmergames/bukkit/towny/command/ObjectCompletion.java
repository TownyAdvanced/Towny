package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class ObjectCompletion {
	
	public static List<String> townCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (Town t: TownyUniverse.getDataSource().getTowns()) {
			if (t.getName().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(t.getName());
			}
		}
		return matches;
	}

	public static List<String> nationTownCompletion(Nation n, String partial) {
		List<String> matches = new ArrayList<String>();
		for (Town t: n.getTowns()) {
			if (t.getName().toLowerCase().startsWith(partial.toLowerCase())) {
					matches.add(t.getName());
			}
		}
		return matches;
	}

	public static List<String> nationCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (Nation n: TownyUniverse.getDataSource().getNations()) {
			if (n.getName().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(n.getName());
			} else if (n.getTag().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(n.getName());
			}
		}
		return matches;
	}
	
	public static List<String> townResidentCompletion(Town town, String partial) {
		List<String> matches = new ArrayList<String>();
		for (Resident r: town.getResidents()) {
			if (r.getName().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(r.getName());
			}
		}
		return matches;
	}
	
	public static List<String> nationResidentCompletion(Nation nation, String partial) {
		List<String> matches = new ArrayList<String>();
		for (Resident r: nation.getResidents()) {
			if (r.getName().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(r.getName());
			}
		}
		return matches;
	}
	
	public static List<String> playerCompletion(String partial, boolean onlineOnly) {
		List<String> matches = new ArrayList<String>();
		if (onlineOnly) {
			for (Player p: Bukkit.getServer().getOnlinePlayers()) {
				if (p.getName().toLowerCase().startsWith(partial.toLowerCase())) {
					matches.add(p.getName());
				}
			}
		} else {
			for (OfflinePlayer p: Bukkit.getServer().getOfflinePlayers()) {
				if (p.getName().toLowerCase().startsWith(partial.toLowerCase())) {
					matches.add(p.getName());
				}
			}
		}
		return matches;
	}
	
	public static List<String> friendCompletion(Player p, String partial) {
		List<String> matches = new ArrayList<String>();
		Resident r;
		try {
			r = TownyUniverse.getDataSource().getResident(p.getName());
		} catch (NotRegisteredException e) {
			return null;
		}
		for (Resident f: r.getFriends()) {
			if (f.getName().toLowerCase().startsWith(partial.toLowerCase())) {
				matches.add(f.getName());
			}
		}
		return matches;
	}
}