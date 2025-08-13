package com.palmergames.bukkit.towny.scheduling.impl;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class BukkitScheduledTask implements ScheduledTask {
	private BukkitTask task;
	private final boolean repeating;
	
	public BukkitScheduledTask(final BukkitTask task) {
		this.task = task;
		this.repeating = false;
	}
	
	public BukkitScheduledTask(final BukkitTask task, final boolean repeating) {
		this.task = task;
		this.repeating = repeating;
	}

	@ApiStatus.Internal
	public void setTask(final BukkitTask task) {
		Preconditions.checkState(this.task == null, "Task is already set");
		this.task = task;
	}

	@Override
	public void cancel() {
		this.task.cancel();
	}

	@Override
	public boolean isCancelled() {
		return !Bukkit.getScheduler().isQueued(this.task.getTaskId()) && !Bukkit.getScheduler().isCurrentlyRunning(this.task.getTaskId());
	}

	@Override
	public @NotNull Plugin getOwningPlugin() {
		return this.task.getOwner();
	}

	@Override
	public boolean isCurrentlyRunning() {
		return Bukkit.getServer().getScheduler().isCurrentlyRunning(this.task.getTaskId());
	}

	@Override
	public boolean isRepeatingTask() {
		return this.repeating;
	}
}
