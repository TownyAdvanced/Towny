package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;

public class DebugRepeatingTimerTask extends TownyTimerTask {

	public DebugRepeatingTimerTask(Towny plugin) {

		super(plugin);
	}

	private Long incrementer = 0L;

	@Override
	public void run() {

		incrementer++;
		plugin.getLogger().info("Bread Log: DebugRepeatingTimer has run " + String.valueOf(incrementer) + " times.");
	}
}
