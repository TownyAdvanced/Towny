package com.palmergames.bukkit.towny.scheduling.impl;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class BukkitTaskScheduler implements TaskScheduler {
	private final Plugin plugin;
	private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
	
	public BukkitTaskScheduler(final Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean isGlobalThread() {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public boolean isTickThread() {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public boolean isEntityThread(@NotNull Entity entity) {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public boolean isRegionThread(@NotNull Location location) {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public @NotNull ScheduledTask run(@NotNull Runnable runnable) {
		return new BukkitScheduledTask(this.scheduler.runTask(this.plugin, runnable));
	}

	@Override
	public @NotNull ScheduledTask runLater(@NotNull Runnable runnable, long delay) {
		return new BukkitScheduledTask(this.scheduler.runTaskLater(this.plugin, runnable, delay));
	}

	@Override
	public @NotNull ScheduledTask runRepeating(@NotNull Runnable runnable, long delay, long period) {
		return new BukkitScheduledTask(this.scheduler.runTaskTimer(this.plugin, runnable, delay, period), true);
	}

	@Override
	public @NotNull ScheduledTask runAsync(@NotNull Runnable runnable) {
		return new BukkitScheduledTask(this.scheduler.runTaskAsynchronously(this.plugin, runnable));
	}

	@Override
	public @NotNull ScheduledTask runAsyncLater(@NotNull Runnable runnable, long delay) {
		return new BukkitScheduledTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, runnable, delay));
	}

	@Override
	public @NotNull ScheduledTask runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
		return new BukkitScheduledTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, runnable, delay, period), true);
	}
}
