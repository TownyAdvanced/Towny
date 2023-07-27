package com.palmergames.bukkit.towny.scheduling.impl;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class FoliaTaskScheduler implements TaskScheduler {
	private final RegionScheduler regionScheduler = Bukkit.getServer().getRegionScheduler();
	private final GlobalRegionScheduler globalRegionScheduler = Bukkit.getServer().getGlobalRegionScheduler();
	private final AsyncScheduler asyncScheduler = Bukkit.getServer().getAsyncScheduler();
	private final Plugin plugin;
	
	public FoliaTaskScheduler(final Plugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean isGlobalThread() {
		return Bukkit.getServer().isGlobalTickThread();
	}

	@Override
	public boolean isTickThread() {
		return Bukkit.getServer().isPrimaryThread(); // The Paper implementation checks whether this is a tick thread, this method exists to avoid confusion.
	}

	@Override
	public boolean isEntityThread(@NotNull Entity entity) {
		return Bukkit.getServer().isOwnedByCurrentRegion(entity);
	}

	@Override
	public boolean isRegionThread(@NotNull Location location) {
		return Bukkit.getServer().isOwnedByCurrentRegion(location);
	}

	@Override
	public @NotNull ScheduledTask run(@NotNull Runnable runnable) {
		return new FoliaScheduledTask(globalRegionScheduler.run(this.plugin, task -> runnable.run()));
	}

	@Override
	public @NotNull ScheduledTask run(@NotNull Entity entity, @NotNull Runnable runnable) {
		return new FoliaScheduledTask(entity.getScheduler().run(this.plugin, task -> runnable.run(), null));
	}

	@Override
	public @NotNull ScheduledTask run(@NotNull Location location, @NotNull Runnable runnable) {
		return new FoliaScheduledTask(regionScheduler.run(this.plugin, location, task -> runnable.run()));
	}

	@Override
	public @NotNull ScheduledTask runLater(@NotNull Runnable runnable, long delay) {
		if (delay == 0)
			return run(runnable);
		
		return new FoliaScheduledTask(globalRegionScheduler.runDelayed(this.plugin, task -> runnable.run(), delay));
	}

	@Override
	public @NotNull ScheduledTask runLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
		if (delay == 0)
			return run(entity, runnable);
		
		return new FoliaScheduledTask(entity.getScheduler().runDelayed(this.plugin, task -> runnable.run(), null, delay));
	}

	@Override
	public @NotNull ScheduledTask runLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
		if (delay == 0)
			return run(location, runnable);
		
		return new FoliaScheduledTask(regionScheduler.runDelayed(this.plugin, location, task -> runnable.run(), delay));
	}

	@Override
	public @NotNull ScheduledTask runRepeating(@NotNull Runnable runnable, long delay, long period) {
		return new FoliaScheduledTask(globalRegionScheduler.runAtFixedRate(this.plugin, task -> runnable.run(), delay, period));
	}

	@Override
	public @NotNull ScheduledTask runRepeating(@NotNull Entity entity, @NotNull Runnable runnable, long delay, long period) {
		return new FoliaScheduledTask(entity.getScheduler().runAtFixedRate(this.plugin, task -> runnable.run(), null, delay, period));
	}

	@Override
	public @NotNull ScheduledTask runRepeating(@NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
		return new FoliaScheduledTask(regionScheduler.runAtFixedRate(this.plugin, location, task -> runnable.run(), delay, period));
	}

	@Override
	public @NotNull ScheduledTask runAsync(@NotNull Runnable runnable) {
		return new FoliaScheduledTask(this.asyncScheduler.runNow(this.plugin, task -> runnable.run()));
	}

	@Override
	public @NotNull ScheduledTask runAsyncLater(@NotNull Runnable runnable, long delay) {
		if (delay == 0)
			return runAsync(runnable);
		
		return new FoliaScheduledTask(this.asyncScheduler.runDelayed(this.plugin, task -> runnable.run(), delay * 50L, TimeUnit.MILLISECONDS));
	}

	@Override
	public @NotNull ScheduledTask runAsyncRepeating(@NotNull Runnable runnable, long delay, long period) {
		return new FoliaScheduledTask(this.asyncScheduler.runAtFixedRate(this.plugin, task -> runnable.run(), delay * 50L, period * 50L, TimeUnit.MILLISECONDS));
	}

	/**
	 * Cancels all active tasks that have been scheduled by {@code this.plugin}
	 */
	public void cancelTasks() {
		this.asyncScheduler.cancelTasks(this.plugin);
		this.globalRegionScheduler.cancelTasks(this.plugin);
	}
}
