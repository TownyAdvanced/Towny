package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.TimeMgmt;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class NewDayScheduler extends TownyTimerTask {

	public NewDayScheduler(Towny plugin) {
		super(plugin);
	}
	
	private static long newDayInterval = -1;

	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:new-day-scheduler"), config -> {
			if (newDayInterval != TownySettings.getDayInterval() && isNewDaySchedulerRunning()) {
				cancelScheduledNewDay();
				new NewDayScheduler(Towny.getPlugin()).run();
			}
		});
	}

	private static Timer newDayTimer;
	private static int scheduleTask = -1;
	private static int newDayTask = -1;
	
	@Override
	public void run() {
		logTime();
		cancelScheduledNewDay();
		
		newDayInterval = TownySettings.getDayInterval();

		if (!TownySettings.doesNewDayUseTimer()) {
			long secondsUntilNextNewDay = TimeMgmt.townyTime();
			
			// If the next new day is less than 2 minutes away, schedule the new day.
			if (secondsUntilNextNewDay < TimeUnit.MINUTES.toSeconds(2)) {
				TownyMessaging.sendDebugMsg("New Day time finalized for: " + TimeMgmt.formatCountdownTime(secondsUntilNextNewDay) + " from now.");
				scheduleUpComingNewDay(secondsUntilNextNewDay);
				// Else the new day scheduler will run again at half the secondsUntilNextNewDay, to check again.
			} else {
				scheduleTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new NewDayScheduler(plugin), (secondsUntilNextNewDay / 2) * 20).getTaskId();
				TownyMessaging.sendDebugMsg("Re-evaluation of New Day time scheduled for: " + TimeMgmt.formatCountdownTime(secondsUntilNextNewDay / 2) + " from now.");
			}
		} else {
			TownyMessaging.sendDebugMsg("Starting new new day scheduler timer.");

			newDayTimer = new Timer("towny-new-day-scheduler", true);
			newDayTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					newDay();
				}
			}, TimeUnit.SECONDS.toMillis(TimeMgmt.townyTime()), TimeUnit.SECONDS.toMillis(TownySettings.getDayInterval()));
		}
	}

	/**
	 * Schedules the next occurrence of the NewDayScheduler.
	 * @param secondsUntilNextNewDay long seconds until the next task.
	 */
	private void scheduleUpComingNewDay(long secondsUntilNextNewDay) {
		if (TownySettings.isEconomyAsync())
			newDayTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new DailyTimerTask(plugin), secondsUntilNextNewDay * 20).getTaskId();
		else
			newDayTask = Bukkit.getScheduler().runTaskLater(plugin, new DailyTimerTask(plugin), secondsUntilNextNewDay * 20).getTaskId();
	
		if (newDayTask == -1)
			TownyMessaging.sendErrorMsg("Could not schedule DailyTimerTask.");
	}
	
	public static boolean isNewDaySchedulerRunning() {
		if (TownySettings.doesNewDayUseTimer())
			return newDayTimer != null;
		
		return Bukkit.getScheduler().isCurrentlyRunning(scheduleTask) || Bukkit.getScheduler().isQueued(scheduleTask);
	}
	
	public static boolean isNewDayScheduled() {
		return Bukkit.getScheduler().isCurrentlyRunning(newDayTask) || Bukkit.getScheduler().isQueued(newDayTask);
	}
	
	public static void cancelScheduledNewDay() {
		if (newDayTimer != null) {
			newDayTimer.cancel();
			newDayTimer = null;
		}
		
		if (scheduleTask != -1) {
			Bukkit.getScheduler().cancelTask(scheduleTask);
			scheduleTask = -1;
		}
		
		if (newDayTask != -1) {
			Bukkit.getScheduler().cancelTask(newDayTask);
			newDayTask = -1;
		}
	}

	/**
	 * Fires a newday to collect taxes and upkeep and other daily activities.
	 * Does not disturb any already scheduled new day timers.
	 */
	public static void newDay() {
		if (TownySettings.isEconomyAsync())
			newDayTask = Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), new DailyTimerTask(Towny.getPlugin())).getTaskId();
		else
			newDayTask = Bukkit.getScheduler().runTask(Towny.getPlugin(), new DailyTimerTask(Towny.getPlugin())).getTaskId();
		
		if (newDayTask == -1)
			TownyMessaging.sendErrorMsg("Could not run newDay.");
	}
	
	public static void logTime() {
		Towny.getPlugin().getLogger().info("Time until a New Day: " + TimeMgmt.formatCountdownTime(TimeMgmt.townyTime()));
	}
	
	/**
	 * Calculates the time in seconds until the next new day event.
	 * TimeZone specific, including daylight savings.
	 * 
	 * @deprecated Deprecated, use {@link TimeMgmt#townyTime()}
	 * 
	 * @return seconds until event
	 */
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public static Long townyTime() {
		return TimeMgmt.townyTime();
	}
}
