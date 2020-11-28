package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;

/**
 * This class represents the short timer task
 *
 * It is generally set to run about once per 20 seconds
 * This rate can be configured.
 *
 * @author Goosius
 */
public class ShortTimerTask extends TownyTimerTask {

	public ShortTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
	}
}