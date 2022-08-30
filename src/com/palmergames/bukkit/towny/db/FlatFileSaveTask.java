package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.util.FileMgmt;

import java.util.HashMap;

public class FlatFileSaveTask implements Runnable {

	private final HashMap<String, Object> map;
	private final String path;
	
	/**
	 * Constructor to save a HashMap to a file.
	 * @param map HashMap to save.
	 * @param path String path on filesystem.
	 */
	public FlatFileSaveTask(HashMap<String, Object> map, String path) {
		this.map = map;
		this.path = path;	
	}

	@Override
	public void run() {
		try {
			FileMgmt.mapToFile(map, path);
		} catch (NullPointerException ex) {
			TownyMessaging.sendErrorMsg("Null Error saving to file - " + path);
		}
	}
}