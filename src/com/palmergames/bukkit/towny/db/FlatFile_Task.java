package com.palmergames.bukkit.towny.db;

import java.util.List;

public class FlatFile_Task {

	public List<String> list;
	public final String path;
	
	/**
	 * Constructor to save a list
	 * @param list
	 * @param path
	 */
	public FlatFile_Task(List<String> list, String path) {

		this.list = list;
		this.path = path;	
	}
}