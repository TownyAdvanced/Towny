package com.palmergames.bukkit.towny.scheduling;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

@DefaultQualifier(NotNull.class)
public interface TaskScheduler {
	boolean isGlobalThread();
	
	boolean isTickThread();

	boolean isEntityThread(Entity entity);

	boolean isRegionThread(Location location);

	ScheduledTask run(Runnable runnable);

	default ScheduledTask run(Entity entity, Runnable runnable) {
		return run(runnable);
	}

	default ScheduledTask run(Location location, Runnable runnable) {
		return run(runnable);
	}
	
	ScheduledTask runLater(Runnable runnable, long delay);
	
	default ScheduledTask runLater(Entity entity, Runnable runnable, long delay) {
		return runLater(runnable, delay);
	}
	
	default ScheduledTask runLater(Location location, Runnable runnable, long delay) {
		return runLater(runnable, delay);
	}
	
	ScheduledTask runRepeating(Runnable runnable, long delay, long period);
	
	default ScheduledTask runRepeating(Entity entity, Runnable runnable, long delay, long period) {
		return runRepeating(runnable, delay, period);
	}
	
	default ScheduledTask runRepeating(Location location, Runnable runnable, long delay, long period) {
		return runRepeating(runnable, delay, period);
	}
	
	ScheduledTask runAsync(Runnable runnable);
	
	ScheduledTask runAsyncLater(Runnable runnable, long delay);
	
	ScheduledTask runAsyncRepeating(Runnable runnable, long delay, long period);
}
