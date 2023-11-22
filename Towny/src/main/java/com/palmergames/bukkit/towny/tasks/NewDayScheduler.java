package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import com.palmergames.util.TimeMgmt;
import org.bukkit.NamespacedKey;
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
	private static ScheduledTask scheduleTask = null;
	private static ScheduledTask newDayTask = null;
	
	@Override
	public void run() {
		logTime();
		cancelScheduledNewDay();
		
		newDayInterval = TownySettings.getDayInterval();

		if (!TownySettings.doesNewDayUseTimer()) {
			long secondsUntilNextNewDay = TimeMgmt.townyTime();
			
			// If the next new day is less than 2 minutes away, schedule the new day.
			if (plugin.getScheduler() instanceof FoliaTaskScheduler || secondsUntilNextNewDay < TimeUnit.MINUTES.toSeconds(2)) {
				TownyMessaging.sendDebugMsg("New Day time finalized for: " + TimeMgmt.formatCountdownTime(secondsUntilNextNewDay) + " from now.");
				scheduleUpComingNewDay(secondsUntilNextNewDay);
				// Else the new day scheduler will run again at half the secondsUntilNextNewDay, to check again.
			} else {
				scheduleTask = plugin.getScheduler().runLater(new NewDayScheduler(plugin), (secondsUntilNextNewDay / 2) * 20);
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
		plugin.getScheduler().runAsyncLater(() -> TownyEconomyHandler.economyExecutor().execute(new DailyTimerTask(plugin)), secondsUntilNextNewDay * 20);
	}
	
	public static boolean isNewDaySchedulerRunning() {
		if (TownySettings.doesNewDayUseTimer())
			return newDayTimer != null;
		
		return scheduleTask != null && !scheduleTask.isCancelled();
	}
	
	public static boolean isNewDayScheduled() {
		return newDayTask != null && !newDayTask.isCancelled();
	}
	
	public static void cancelScheduledNewDay() {
		if (newDayTimer != null) {
			newDayTimer.cancel();
			newDayTimer = null;
		}
		
		if (scheduleTask != null) {
			scheduleTask.cancel();
			scheduleTask = null;
		}
		
		if (newDayTask != null) {
			newDayTask.cancel();
			newDayTask = null;
		}
	}

	/**
	 * Fires a newday to collect taxes and upkeep and other daily activities.
	 * Does not disturb any already scheduled new day timers.
	 */
	public static void newDay() {
		TownyEconomyHandler.economyExecutor().execute(new DailyTimerTask(Towny.getPlugin()));
	}
	
	public static void logTime() {
		Towny.getPlugin().getLogger().info("Time until a New Day: " + TimeMgmt.formatCountdownTime(TimeMgmt.townyTime()));
	}
}
