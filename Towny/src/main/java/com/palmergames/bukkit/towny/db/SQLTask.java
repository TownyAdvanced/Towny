package com.palmergames.bukkit.towny.db;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public class SQLTask implements Runnable {
	private final TownySQLSource source;

	// Update flags this for an insert/update or delete.
	public final boolean update;

	public final String tb_name;
	public final Map<String, ?> args;
	public final List<String> keys;

	/**
	 * Constructor for a Delete task
	 * 
	 * @param tb_name - Table name.
	 * @param args - Arguments.
	 */
	public SQLTask(TownySQLSource source, String tb_name, Map<String, ?> args) {

		this(source, false, tb_name, args, null);

	}

	/**
	 * Constructor for an INSERT/UPDATE task.
	 * 
	 * @param tb_name - Table Name.
	 * @param args - Arguments.
	 * @param keys - Keys to add to table.
	 */
	public SQLTask(TownySQLSource source, String tb_name, Map<String, ?> args, List<String> keys) {

		this(source, true, tb_name, args, keys);

	}

	private SQLTask(TownySQLSource source, boolean update, String tb_name, Map<String, ?> args, List<String> keys) {
		this.source = source;
		this.update = update;
		this.tb_name = tb_name;
		this.args = args;
		this.keys = keys;

	}

	@Override
	public void run() {
		if (this.update) {
			source.queueUpdateDB(this.tb_name, this.args, this.keys);
		} else {
			source.queueDeleteDB(this.tb_name, this.args);
		}
	}
}
