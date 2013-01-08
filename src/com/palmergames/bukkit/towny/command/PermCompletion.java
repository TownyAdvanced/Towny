package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

public class PermCompletion {
	private static List<String> onOffList = new ArrayList<String>(2);
	
	static {
		onOffList.add("on");
		onOffList.add("off");
	}
	
	public static List<String> onOffCompletion(String partial) {
		List<String> matches = new ArrayList<String>(2);
		for (String o: onOffList) {
			if (o.startsWith(partial.toLowerCase())) {
				matches.add(o);
			}
		}
		return matches;
	}
	public static List<String> handleSetPerm(String[] args, String resFriend) {	
		if (args.length == 3) {
			List<String> matches = permLevelCompletion(args[2], resFriend);
			matches.addAll(permTypeCompletion(args[2]));
			matches.addAll(onOffCompletion(args[2]));
			return matches;
		} else if (args.length == 4) {
			switch(args[2].toLowerCase()) {
			case "resident":
			case "friend":
			case "ally":
			case "outsider":
				List<String> matches = permTypeCompletion(args[3]);
				matches.addAll(onOffCompletion(args[3]));
				return matches;
			case "build":
			case "destroy":
			case "switch":
			case "itemuse":
				return onOffCompletion(args[3]);
			default:
				return null;
			}
		} else if (args.length == 5) {
			return onOffCompletion(args[3]);
		}
		return null;
	}
	
	public static List<String> permLevelCompletion(String partial, String resFriend) {
		List<String> matches = new ArrayList<String>();
		String p = partial.toLowerCase();
		if (resFriend.equals("resident")) {
			if (("resident").startsWith(p)) {
				matches.add("resident");
			}
		} else if (resFriend.equals("friend")) {
			if (("friend").startsWith(p)) {
				matches.add("friend");
			}
		}
		if (("ally").startsWith(p)) {
			matches.add("ally");
		}
		if (("outsider").startsWith(p)) {
			matches.add("outsider");
		}
		return matches;
	}
	
	public static List<String> permTypeCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		String p = partial.toLowerCase();
		if (("build").startsWith(p)) {
			matches.add("build");
		}
		if (("destroy").startsWith(p)) {
			matches.add("destroy");
		}
		if (("switch").startsWith(p)) {
			matches.add("switch");
		}
		if (("itemuse").startsWith(p)) {
			matches.add("itemuse");
		}
		return matches;
	}
	
	
	
}