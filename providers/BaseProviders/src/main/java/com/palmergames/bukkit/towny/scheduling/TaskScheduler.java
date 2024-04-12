package com.palmergames.bukkit.towny.scheduling;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@DefaultQualifier(NotNull.class)
public interface TaskScheduler {
	boolean isGlobalThread();
	
	boolean isTickThread();

	boolean isEntityThread(Entity entity);

	boolean isRegionThread(Location location);

	ScheduledTask run(Consumer<ScheduledTask> task);

	default ScheduledTask run(Entity entity, Consumer<ScheduledTask> task) {
		return run(task);
	}

	default ScheduledTask run(Location location, Consumer<ScheduledTask> task) {
		return run(task);
	}
	
	ScheduledTask runLater(Consumer<ScheduledTask> task, long delay);
	
	default ScheduledTask runLater(Entity entity, Consumer<ScheduledTask> task, long delay) {
		return runLater(task, delay);
	}
	
	default ScheduledTask runLater(Location location, Consumer<ScheduledTask> task, long delay) {
		return runLater(task, delay);
	}
	
	ScheduledTask runRepeating(Consumer<ScheduledTask> task, long delay, long period);
	
	default ScheduledTask runRepeating(Entity entity, Consumer<ScheduledTask> task, long delay, long period) {
		return runRepeating(task, delay, period);
	}

	default ScheduledTask runRepeating(Location location, Consumer<ScheduledTask> task, long delay, long period) {
		return runRepeating(task, delay, period);
	}
	
	ScheduledTask runAsync(Consumer<ScheduledTask> task);
	
	ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit timeUnit);
	
	ScheduledTask runAsyncRepeating(Consumer<ScheduledTask> task, long delay, long period, TimeUnit timeUnit);
	
	@ApiStatus.Experimental
	default ScheduledTask runGlobal(final Consumer<ScheduledTask> task) {
		return run(task);
	}
	
	@ApiStatus.Experimental
	default ScheduledTask runGlobalLater(final Consumer<ScheduledTask> task, final long delay) {
		return runLater(task, delay);
	}
	
	@ApiStatus.Experimental
	default ScheduledTask runGlobalRepeating(final Consumer<ScheduledTask> task, final long delay, final long period) {
		return runRepeating(task, delay, period);
	}

	/*
	 * Runnable methods
	 */
	
	default ScheduledTask run(Runnable runnable) {
		return run(task -> runnable.run());
	}

	default ScheduledTask run(Entity entity, Runnable runnable) {
		return run(runnable);
	}

	default ScheduledTask run(Location location, Runnable runnable) {
		return run(runnable);
	}
	
	default ScheduledTask runLater(Runnable runnable, long delay) {
		return runLater(task -> runnable.run(), delay);
	}
	
	default ScheduledTask runLater(Entity entity, Runnable runnable, long delay) {
		return runLater(runnable, delay);
	}
	
	default ScheduledTask runLater(Location location, Runnable runnable, long delay) {
		return runLater(runnable, delay);
	}
	
	default ScheduledTask runRepeating(Runnable runnable, long delay, long period) {
		return runRepeating(task -> runnable.run(), delay, period);
	}
	
	default ScheduledTask runRepeating(Entity entity, Runnable runnable, long delay, long period) {
		return runRepeating(runnable, delay, period);
	}
	
	default ScheduledTask runRepeating(Location location, Runnable runnable, long delay, long period) {
		return runRepeating(runnable, delay, period);
	}
	
	default ScheduledTask runAsync(Runnable runnable) {
		return runAsync(task -> runnable.run());
	}
	
	default ScheduledTask runAsyncLater(Runnable runnable, long delay) {
		return runAsyncLater(task -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS);
	}
	
	default ScheduledTask runAsyncRepeating(Runnable runnable, long delay, long period) {
		return runAsyncRepeating(task -> runnable.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS);
	}
}
