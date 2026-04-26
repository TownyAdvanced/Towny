package com.palmergames.bukkit.towny.scheduling;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.framework.qual.DefaultQualifier;
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
	
	default ScheduledTask runGlobal(final Consumer<ScheduledTask> task) {
		return run(task);
	}
	
	default ScheduledTask runGlobalLater(final Consumer<ScheduledTask> task, final long delay) {
		return runLater(task, delay);
	}
	
	default ScheduledTask runGlobalRepeating(final Consumer<ScheduledTask> task, final long delay, final long period) {
		return runRepeating(task, delay, period);
	}

	/*
	 * Runnable methods
	 */
	
	default ScheduledTask run(Runnable runnable) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return run(task -> runnable.run());
	}

	default ScheduledTask run(Entity entity, Runnable runnable) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return run(entity, task -> runnable.run());
	}

	default ScheduledTask run(Location location, Runnable runnable) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return run(location, task -> runnable.run());
	}
	
	default ScheduledTask runLater(Runnable runnable, long delay) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runLater(task -> runnable.run(), delay);
	}
	
	default ScheduledTask runLater(Entity entity, Runnable runnable, long delay) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runLater(entity, task -> runnable.run(), delay);
	}
	
	default ScheduledTask runLater(Location location, Runnable runnable, long delay) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runLater(location, task -> runnable.run(), delay);
	}
	
	default ScheduledTask runRepeating(Runnable runnable, long delay, long period) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runRepeating(task -> runnable.run(), delay, period);
	}
	
	default ScheduledTask runRepeating(Entity entity, Runnable runnable, long delay, long period) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runRepeating(entity, task -> runnable.run(), delay, period);
	}
	
	default ScheduledTask runRepeating(Location location, Runnable runnable, long delay, long period) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runRepeating(location, task -> runnable.run(), delay, period);
	}
	
	default ScheduledTask runAsync(Runnable runnable) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runAsync(task -> runnable.run());
	}
	
	default ScheduledTask runAsyncLater(Runnable runnable, long delay) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runAsyncLater(task -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS);
	}
	
	default ScheduledTask runAsyncRepeating(Runnable runnable, long delay, long period) {
		Preconditions.checkArgument(runnable != null, "runnable may not be null");
		return runAsyncRepeating(task -> runnable.run(), delay * 50, period * 50, TimeUnit.MILLISECONDS);
	}
}
