package com.palmergames.bukkit.towny.scheduling.impl;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@DefaultQualifier(NotNull.class)
public class FoliaTaskScheduler implements TaskScheduler {
	private static final long MS_PER_TICK = 50L; // in an ideal world
	private final RegionScheduler regionScheduler = Bukkit.getServer().getRegionScheduler();
	final GlobalRegionScheduler globalRegionScheduler = Bukkit.getServer().getGlobalRegionScheduler();
	private final AsyncScheduler asyncScheduler = Bukkit.getServer().getAsyncScheduler();
	final Plugin plugin;
	
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
	public boolean isEntityThread(final Entity entity) {
		return Bukkit.getServer().isOwnedByCurrentRegion(entity);
	}

	@Override
	public boolean isRegionThread(final Location location) {
		return Bukkit.getServer().isOwnedByCurrentRegion(location);
	}

	@Override
	public ScheduledTask run(final Consumer<ScheduledTask> task) {
		return runAsync(task);
	}

	@Override
	public ScheduledTask run(final Entity entity, final Consumer<ScheduledTask> task) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(entity.getScheduler().run(this.plugin, t -> task.accept(ret), null));
		
		return ret;
	}

	@Override
	public ScheduledTask run(final Location location, final Consumer<ScheduledTask> task) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(regionScheduler.run(this.plugin, location, t -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runLater(final Consumer<ScheduledTask> task, final long delay) {
		Preconditions.checkArgument(task != null, "task may not be null");

		if (delay == 0) {
			return run(task);
		}
		
		return runAsyncLater(task, delay * MS_PER_TICK, TimeUnit.MILLISECONDS);
	}

	@Override
	public ScheduledTask runLater(final Entity entity, final Consumer<ScheduledTask> task, final long delay) {
		Preconditions.checkArgument(task != null, "task may not be null");

		if (delay == 0) {
			return run(entity, task);
		}

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(entity.getScheduler().runDelayed(this.plugin, t -> task.accept(ret), null, delay));
		
		return ret;
	}

	@Override
	public ScheduledTask runLater(final Location location, final Consumer<ScheduledTask> task, final long delay) {
		Preconditions.checkArgument(task != null, "task may not be null");

		if (delay == 0) {
			return run(location, task);
		}
		
		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(regionScheduler.runDelayed(this.plugin, location, t -> task.accept(ret), delay));
		
		return ret;
	}

	@Override
	public ScheduledTask runRepeating(final Consumer<ScheduledTask> task, final long delay, final long period) {
		Preconditions.checkArgument(task != null, "task may not be null");

		return runAsyncRepeating(task, delay * MS_PER_TICK, period * MS_PER_TICK, TimeUnit.MILLISECONDS);
	}

	@Override
	public ScheduledTask runRepeating(final Entity entity, final Consumer<ScheduledTask> task, final long delay, final long period) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(entity.getScheduler().runAtFixedRate(this.plugin, t -> task.accept(ret), null, delay, period));
		
		return ret;
	}

	@Override
	public ScheduledTask runRepeating(final Location location, final Consumer<ScheduledTask> task, final long delay, final long period) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(regionScheduler.runAtFixedRate(this.plugin, location, t -> task.accept(ret), delay, period));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsync(final Consumer<ScheduledTask> task) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.asyncScheduler.runNow(this.plugin, t -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsyncLater(final Consumer<ScheduledTask> task, final long delay, TimeUnit timeUnit) {
		Preconditions.checkArgument(task != null, "task may not be null");

		if (delay == 0) {
			return runAsync(task);
		}

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.asyncScheduler.runDelayed(this.plugin, t -> task.accept(ret), delay, timeUnit));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsyncRepeating(final Consumer<ScheduledTask> task, final long delay, final long period, TimeUnit timeUnit) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.asyncScheduler.runAtFixedRate(this.plugin, t -> task.accept(ret), delay, period, timeUnit));
		
		return ret;
	}

	@Override
	public ScheduledTask runGlobal(final Consumer<ScheduledTask> task) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.globalRegionScheduler.run(this.plugin, t -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runGlobalLater(final Consumer<ScheduledTask> task, final long delay) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.globalRegionScheduler.runDelayed(this.plugin, t -> task.accept(ret), delay));
		
		return ret;
	}

	@Override
	public ScheduledTask runGlobalRepeating(final Consumer<ScheduledTask> task, final long delay, final long period) {
		Preconditions.checkArgument(task != null, "task may not be null");

		final FoliaScheduledTask ret = new FoliaScheduledTask(null);
		ret.setTask(this.globalRegionScheduler.runAtFixedRate(this.plugin, t -> task.accept(ret), delay, period));

		return ret;
	}

	/**
	 * Cancels all active tasks that have been scheduled by {@code this.plugin}
	 */
	public void cancelTasks() {
		this.asyncScheduler.cancelTasks(this.plugin);
		this.globalRegionScheduler.cancelTasks(this.plugin);
	}
}
