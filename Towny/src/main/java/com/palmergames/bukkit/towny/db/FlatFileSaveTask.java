package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.util.FileMgmt;

import java.util.List;

public class FlatFileSaveTask implements Runnable {

	private final List<String> list;
	private final String path;
	
	/**
	 * Constructor to save a list
	 * @param list - list to save.
	 * @param path - path on filesystem.
	 */
	public FlatFileSaveTask(List<String> list, String path) {
		this.list = list;
		this.path = path;	
	}

	@Override
	public void run() {
		try {
			FileMgmt.listToFile(list, path);
		} catch (NullPointerException ex) {
			TownyMessaging.sendErrorMsg("Null Error saving to file - " + path);
		}
	}
}