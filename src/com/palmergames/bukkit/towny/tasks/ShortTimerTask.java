package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.time.NewShortTimeEvent;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;

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
		
		// Check and see if we have any room in the PlotChunks regeneration, and more in the queue.
		if (TownyRegenAPI.getPlotChunks().size() < 20 && TownyRegenAPI.regenQueueHasAvailable()) {
			TownyRegenAPI.getWorldCoordFromQueueForRegeneration();
		}
		
		/*
		 * Fire an event other plugins can use.
		 */
		Bukkit.getPluginManager().callEvent(new NewShortTimeEvent(System.currentTimeMillis()));
	}
}