package com.palmergames.bukkit.towny.scheduling.impl;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@DefaultQualifier(NotNull.class)
public class PaperTaskScheduler extends FoliaTaskScheduler {
	public PaperTaskScheduler(Plugin plugin) {
		super(plugin);
	}

	@Override
	public boolean isGlobalThread() {
		// isGlobalThread does not exist on paper, match the bukkit task scheduler's behaviour.
		return Bukkit.getServer().isPrimaryThread();
	}
	
	/*
	 * These methods run async on folia, but on paper we expect them to be sync
	 */
	
	@Override
	public ScheduledTask run(final Consumer<ScheduledTask> task) {
		final AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new FoliaScheduledTask(globalRegionScheduler.run(plugin, t -> task.accept(taskRef.get()))));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runLater(final Consumer<ScheduledTask> task, long delay) {
		final AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new FoliaScheduledTask(globalRegionScheduler.runDelayed(plugin, t -> task.accept(taskRef.get()), delay)));

		return taskRef.get();
	}

	@Override
	public ScheduledTask runRepeating(final Consumer<ScheduledTask> task, long delay, long period) {
		final AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new FoliaScheduledTask(globalRegionScheduler.runAtFixedRate(plugin, t -> task.accept(taskRef.get()), delay, period)));

		return taskRef.get();
	}
}
