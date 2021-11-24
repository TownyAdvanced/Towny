package com.palmergames.bukkit.towny.tasks;

import java.util.Calendar;
import java.util.TimeZone;

import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;

public class NewDayScheduler extends TownyTimerTask {

	public NewDayScheduler(Towny plugin) {
		super(plugin);
	}

	private static int scheduleTask = -1;
	
	@Override
	public void run() {
		long timeTillNextDay = townyTime();
		plugin.getLogger().info("Time until a New Day: " + TimeMgmt.formatCountdownTime(timeTillNextDay));

		// If the next new day is less than 5 minutes away, schedule the new day.
		if (timeTillNextDay < TimeTools.secondsFromDhms("5m")) {
			scheduleUpComingNewDay(timeTillNextDay);
		// Else the new day scheduler will run again at half the timeTillNextDay, to check again.
		} else {
			Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new NewDayScheduler(plugin), timeTillNextDay / 2);
			plugin.getLogger().info("New Day scheduled for: " + TimeMgmt.formatCountdownTime(timeTillNextDay/2) + " from now.");
		}
	}

	/**
	 * Schedules the next occurence of the NewDayScheduler.
	 * @param timeTillNextDay long seconds until the next task.
	 */
	private void scheduleUpComingNewDay(long timeTillNextDay) {
		if (TownySettings.isEconomyAsync())
			scheduleTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new DailyTimerTask(plugin), timeTillNextDay).getTaskId();
		else
			scheduleTask = Bukkit.getScheduler().runTaskLater(plugin, new DailyTimerTask(plugin), timeTillNextDay).getTaskId();
	
		if (scheduleTask == -1)
			TownyMessaging.sendErrorMsg("Could not schedule newdayscheduler task.");
	}
	
	public static boolean isNewDaySchedulerRunning() {
		return scheduleTask != -1;
	}
	
	public static void cancelScheduledNewDay() {
		if (scheduleTask != -1)
			Bukkit.getScheduler().cancelTask(scheduleTask);
	}

	/**
	 * Fires a newday to collect taxes and upkeep and other daily activities.
	 * Does not disturb any already scheduled new day timers.
	 */
	public static void newDay() {
		if (TownySettings.isEconomyAsync()) {
			if (BukkitTools.scheduleAsyncDelayedTask(new DailyTimerTask(Towny.getPlugin()),0L) == -1)
				TownyMessaging.sendErrorMsg("Could not run newDay.");
		} else {
			if (BukkitTools.scheduleSyncDelayedTask(new DailyTimerTask(Towny.getPlugin()),0L) == -1)
				TownyMessaging.sendErrorMsg("Could not run newDay.");
		}
	}
	
	/**
	 * Calculates the time in seconds until the next new day event.
	 * TimeZone specific, including daylight savings.
	 * 
	 * @return seconds until event
	 */
	public static Long townyTime() {

		long secondsInDay = TownySettings.getDayInterval();

		// Get Calendar instance
		Calendar now = Calendar.getInstance();

		// Get current TimeZone
		TimeZone timeZone = now.getTimeZone();
		
		// Get current system time in milliseconds
		long timeMilli = System.currentTimeMillis();
		
		// Calculate the TimeZone specific offset (including DST)
		int timeOffset = timeZone.getOffset(timeMilli)/1000;

		return (secondsInDay + (TownySettings.getNewDayTime() - ((timeMilli/1000) % secondsInDay) - timeOffset)) % secondsInDay;
	}

}
