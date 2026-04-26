package com.palmergames.bukkit.towny.hooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.StringMgmt;

public class LeakChecker {

	final static List<Version> EWVersions = Arrays.asList(Version.fromString("0.0.104"),
			Version.fromString("0.0.105"),
			Version.fromString("0.0.121"));
	final static List<Version> TCVersions = Arrays.asList(Version.fromString("0.0.46"));
	final static List<Version> TRTPVersions = Arrays.asList(Version.fromString("0.0.9"));
	final static List<Version> ToBVersions = Arrays.asList(Version.fromString("0.2.0"));
	final static List<Version> THVersions = Arrays.asList(Version.fromString("0.0.30"));

	public static void checkForLeaks() {

		List<String> leaks = new ArrayList<>();
		checkPlugin(leaks, EWVersions, "EventWar");
		checkPlugin(leaks, TCVersions, "TownyCamps");
		checkPlugin(leaks, TRTPVersions, "TownyRTP");
		checkPlugin(leaks, ToBVersions, "TowerOfBabel");
		checkPlugin(leaks, THVersions, "TownyHistories");
		if (leaks.isEmpty())
			return;

		Towny.getPlugin().getLogger().warning("Detected memory leaks coming from " + StringMgmt.join(leaks, ",") + "!");
	}

	private static void checkPlugin(List<String> leaks, List<Version> versionList, String name) {
		PluginManager pm = Bukkit.getPluginManager();
		Plugin plugin = pm.getPlugin(name);
		if (plugin == null || !versionList.contains(Version.fromPlugin(plugin)))
			return;
		Towny.getPlugin().getScheduler().runLater(() -> pm.disablePlugin(plugin), 10L);
		leaks.add(name);
	}
}
