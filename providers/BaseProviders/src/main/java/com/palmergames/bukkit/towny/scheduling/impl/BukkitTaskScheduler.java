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
import java.util.concurrent.atomic.AtomicReference;
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
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTask(this.plugin, () -> task.accept(taskRef.get()))));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runLater(Consumer<ScheduledTask> task, long delay) {
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTaskLater(this.plugin, () -> task.accept(taskRef.get()), delay)));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runRepeating(Consumer<ScheduledTask> task, long delay, long period) {
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTaskTimer(this.plugin, () -> task.accept(taskRef.get()), delay, period), true));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runAsync(Consumer<ScheduledTask> task) {
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTaskAsynchronously(this.plugin, () -> task.accept(taskRef.get()))));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit timeUnit) {
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTaskLaterAsynchronously(this.plugin, () -> task.accept(taskRef.get()), timeUnit.toMillis(delay) / 50)));
		
		return taskRef.get();
	}

	@Override
	public ScheduledTask runAsyncRepeating(Consumer<ScheduledTask> task, long delay, long period, TimeUnit timeUnit) {
		AtomicReference<ScheduledTask> taskRef = new AtomicReference<>();
		taskRef.set(new BukkitScheduledTask(this.scheduler.runTaskTimerAsynchronously(this.plugin, () -> task.accept(taskRef.get()), timeUnit.toMillis(delay) / 50, timeUnit.toMillis(period) / 50), true));
		
		return taskRef.get();
	}
}
