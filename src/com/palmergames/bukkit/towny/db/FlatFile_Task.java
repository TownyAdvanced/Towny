package com.palmergames.bukkit.towny.db;

import java.util.List;

public class FlatFile_Task {

	public final List<String> list;
	public final String path;
	
	/**
	 * Constructor to getString a list
	 * @param list - list to getString.
	 * @param path - path on filesystem.
	 */
	public FlatFile_Task(List<String> list, String path) {

		this.list = list;
		this.path = path;	
	}
}