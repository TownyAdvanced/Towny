package com.palmergames.bukkit.towny.scheduling.impl;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@DefaultQualifier(NotNull.class)
public class PaperTaskScheduler extends FoliaTaskScheduler {
	public PaperTaskScheduler(Plugin plugin) {
		super(plugin);
	}

	@Override
	public boolean isGlobalThread() {
		// isGlobalThread exists on paper since 1.21.3, but is implemented the exact same way as this method.
		return Bukkit.getServer().isPrimaryThread();
	}
	
	/*
	 * These methods run async on folia, but on paper we expect them to be sync
	 */
	
	@Override
	public ScheduledTask run(final Consumer<ScheduledTask> task) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(globalRegionScheduler.run(plugin, t -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runLater(final Consumer<ScheduledTask> task, long delay) {
		Preconditions.checkArgument(task != null, "task may not be null");

		if (delay == 0) {
			return run(task);
		}
		
		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(globalRegionScheduler.runDelayed(plugin, t -> task.accept(ret), delay));

		return ret;
	}

	@Override
	public ScheduledTask runRepeating(final Consumer<ScheduledTask> task, long delay, long period) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(globalRegionScheduler.runAtFixedRate(plugin, t -> task.accept(ret), delay, period));

		return ret;
	}
}
