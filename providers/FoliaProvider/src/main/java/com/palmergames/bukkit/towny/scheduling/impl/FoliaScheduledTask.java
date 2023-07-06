package com.palmergames.bukkit.towny.scheduling.impl;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class FoliaScheduledTask implements com.palmergames.bukkit.towny.scheduling.ScheduledTask {
	private final ScheduledTask task;
	
	public FoliaScheduledTask(final ScheduledTask task) {
		this.task = task;
	}

	@Override
	public void cancel() {
		this.task.cancel();
	}

	@Override
	public boolean isCancelled() {
		return this.task.isCancelled();
	}
	
	@Override
	public @NotNull Plugin getOwningPlugin() {
		return this.task.getOwningPlugin();
	}
	
	@Override
	public boolean isCurrentlyRunning() {
		final ScheduledTask.ExecutionState state = this.task.getExecutionState();
		return state == ScheduledTask.ExecutionState.RUNNING || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
	}

	@Override
	public boolean isRepeatingTask() {
		return this.task.isRepeatingTask();
	}
}
