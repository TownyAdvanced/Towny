package com.palmergames.bukkit.towny.scheduling.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PaperTaskScheduler extends FoliaTaskScheduler {
	public PaperTaskScheduler(Plugin plugin) {
		super(plugin);
	}

	@Override
	public boolean isGlobalThread() {
		// isGlobalThread does not exist on paper, match the bukkit task scheduler's behaviour.
		return Bukkit.getServer().isPrimaryThread();
	}
}
