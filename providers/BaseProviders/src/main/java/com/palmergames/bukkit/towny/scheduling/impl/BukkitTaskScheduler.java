package com.palmergames.bukkit.towny.scheduling.impl;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@DefaultQualifier(NotNull.class)
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
	public boolean isEntityThread(Entity entity) {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public boolean isRegionThread(Location location) {
		return Bukkit.getServer().isPrimaryThread();
	}

	@Override
	public ScheduledTask run(Consumer<ScheduledTask> task) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null);
		ret.setTask(this.scheduler.runTask(this.plugin, () -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runLater(Consumer<ScheduledTask> task, long delay) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null);
		ret.setTask(this.scheduler.runTaskLater(this.plugin, () -> task.accept(ret), delay));
		
		return ret;
	}

	@Override
	public ScheduledTask runRepeating(Consumer<ScheduledTask> task, long delay, long period) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null, true);
		ret.setTask(this.scheduler.runTaskTimer(this.plugin, () -> task.accept(ret), delay, period));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsync(Consumer<ScheduledTask> task) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null);
		ret.setTask(this.scheduler.runTaskAsynchronously(this.plugin, () -> task.accept(ret)));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit timeUnit) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null);
		ret.setTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, () -> task.accept(ret), timeUnit.toMillis(delay) / 50));
		
		return ret;
	}

	@Override
	public ScheduledTask runAsyncRepeating(Consumer<ScheduledTask> task, long delay, long period, TimeUnit timeUnit) {
		final BukkitScheduledTask ret = new BukkitScheduledTask(null, true);
		ret.setTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, () -> task.accept(ret), timeUnit.toMillis(delay) / 50, timeUnit.toMillis(period) / 50));
		
		return ret;
	}
}
