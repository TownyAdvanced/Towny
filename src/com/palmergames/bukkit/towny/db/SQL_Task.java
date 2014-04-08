package com.palmergames.bukkit.towny.db;

import java.util.HashMap;
import java.util.List;


public class SQL_Task {

	// Update flags this for an insert/update or delete.
	public final boolean update;
	
	public final String tb_name;
	public final HashMap<String, Object> args;
	public final List<String> keys;

	public SQL_Task(String tb_name, HashMap<String, Object> args) {

		this(false, tb_name, args, null);

	}

	public SQL_Task(String tb_name, HashMap<String, Object> args,
			List<String> keys) {

		this(true, tb_name, args, keys);

	}

	private SQL_Task(boolean update, String tb_name,
			HashMap<String, Object> args, List<String> keys) {

		this.update = update;
		this.tb_name = tb_name;
		this.args = args;
		this.keys = keys;

	}

}