package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DailyTimerTask;
import com.palmergames.bukkit.towny.tasks.DrawSmokeTask;
import com.palmergames.bukkit.towny.tasks.DrawSpawnPointsTask;
import com.palmergames.bukkit.towny.tasks.GatherResidentUUIDTask;
import com.palmergames.bukkit.towny.tasks.HealthRegenTimerTask;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.RepeatingTimerTask;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.tasks.HourlyTimerTask;
import com.palmergames.bukkit.towny.tasks.ShortTimerTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;

import java.util.Calendar;
import java.util.TimeZone;


/**
 * Handler for all running timers
 * 
 * @author ElgarL
 *
 */
public class TownyTimerHandler{
	
	private static Towny plugin;
	
	public static void initialize (Towny plugin) {
		
		TownyTimerHandler.plugin = plugin;
	}
	
	private static int townyRepeatingTask = -1;
	private static int dailyTask = -1;
	private static int hourlyTask = -1;
	private static int shortTask = -1;
	private static int mobRemoveTask = -1;
	private static int healthRegenTask = -1;
	private static int teleportWarmupTask = -1;
	private static int cooldownTimerTask = -1;
	private static int drawSmokeTask = -1;
	private static int gatherResidentUUIDTask = -1;
	private static int drawSpawnPointsTask = -1;

	public static void newDay() {

		if (!isDailyTimerRunning())
			toggleDailyTimer(true);
		//dailyTimer.schedule(new DailyTimerTask(this), 0);
		if (TownySettings.isEconomyAsync()) {
			if (BukkitTools.scheduleAsyncDelayedTask(new DailyTimerTask(plugin),0L) == -1)
				TownyMessaging.sendErrorMsg("Could not schedule newDay.");
		} else {
			if (BukkitTools.scheduleSyncDelayedTask(new DailyTimerTask(plugin),0L) == -1)
				TownyMessaging.sendErrorMsg("Could not schedule newDay.");
		}
	}

	public static void newHour() {
		if (!isHourlyTimerRunning())
			toggleHourlyTimer(true);

		if (BukkitTools.scheduleAsyncDelayedTask(new HourlyTimerTask(plugin),0L) == -1)
			TownyMessaging.sendErrorMsg("Could not schedule new hour.");
	}

	public static void toggleTownyRepeatingTimer(boolean on) {

		if (on && !isTownyRepeatingTaskRunning()) {
			townyRepeatingTask = BukkitTools.scheduleSyncRepeatingTask(new RepeatingTimerTask(plugin), 0, TimeTools.convertToTicks(1L));
			if (townyRepeatingTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule Towny Timer Task.");
		} else if (!on && isTownyRepeatingTaskRunning()) {
			BukkitTools.getScheduler().cancelTask(townyRepeatingTask);
			townyRepeatingTask = -1;
		}
	}

	public static void toggleMobRemoval(boolean on) {

		if (on && !isMobRemovalRunning()) {
			mobRemoveTask = BukkitTools.scheduleSyncRepeatingTask(new MobRemovalTimerTask(plugin, BukkitTools.getServer()), 0, TimeTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
			if (mobRemoveTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule mob removal loop.");
		} else if (!on && isMobRemovalRunning()) {
			BukkitTools.getScheduler().cancelTask(mobRemoveTask);
			mobRemoveTask = -1;
		}
	}

	public static void toggleDailyTimer(boolean on) {

		if (on && !isDailyTimerRunning()) {
			long timeTillNextDay = townyTime();
			plugin.getLogger().info("Time until a New Day: " + TimeMgmt.formatCountdownTime(timeTillNextDay));
			
			if (TownySettings.isEconomyAsync())
				dailyTask = BukkitTools.scheduleAsyncRepeatingTask(new DailyTimerTask(plugin), TimeTools.convertToTicks(timeTillNextDay), TimeTools.convertToTicks(TownySettings.getDayInterval()));
			else
				dailyTask = BukkitTools.scheduleSyncRepeatingTask(new DailyTimerTask(plugin), TimeTools.convertToTicks(timeTillNextDay), TimeTools.convertToTicks(TownySettings.getDayInterval()));
			
			if (dailyTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule new day loop.");
		} else if (!on && isDailyTimerRunning()) {
			BukkitTools.getScheduler().cancelTask(dailyTask);
			dailyTask = -1;
		}
	}

	public static void toggleHourlyTimer(boolean on) {
		if (on && !isHourlyTimerRunning()) {
			long timeUntilNextHourInSections = getTimeUntilNextHourInSeconds();
			hourlyTask = BukkitTools.scheduleAsyncRepeatingTask(new HourlyTimerTask(plugin), timeUntilNextHourInSections, TimeTools.convertToTicks(TownySettings.getHourInterval()));

			if (hourlyTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule hourly timer.");

		} else if (!on && isHourlyTimerRunning()) {
			BukkitTools.getScheduler().cancelTask(hourlyTask);
			hourlyTask = -1;
		}
	}

	public static void toggleShortTimer(boolean on) {
		if (on && !isShortTimerRunning()) {
			//This small delay is a safeguard against race conditions
			long delayTicks = TimeTools.convertToTicks(60);
			shortTask = BukkitTools.scheduleAsyncRepeatingTask(new ShortTimerTask(plugin), delayTicks, TimeTools.convertToTicks(TownySettings.getShortInterval()));

			if (shortTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule short timer.");

		} else if (!on && isDailyTimerRunning()) {
			BukkitTools.getScheduler().cancelTask(shortTask);
			shortTask = -1;
		}
	}

	public static void toggleHealthRegen(boolean on) {

		if (on && !isHealthRegenRunning()) {
			healthRegenTask = BukkitTools.scheduleSyncRepeatingTask(new HealthRegenTimerTask(plugin, BukkitTools.getServer()), 0, TimeTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
			if (healthRegenTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule health regen loop.");
		} else if (!on && isHealthRegenRunning()) {
			BukkitTools.getScheduler().cancelTask(healthRegenTask);
			healthRegenTask = -1;
		}
	}

	public static void toggleTeleportWarmup(boolean on) {

		if (on && !isTeleportWarmupRunning()) {
			teleportWarmupTask = BukkitTools.scheduleSyncRepeatingTask(new TeleportWarmupTimerTask(plugin), 0, 20);
			if (teleportWarmupTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule teleport warmup loop.");
		} else if (!on && isTeleportWarmupRunning()) {
			BukkitTools.getScheduler().cancelTask(teleportWarmupTask);
			teleportWarmupTask = -1;
		}
	}
	
	public static void toggleCooldownTimer(boolean on) {
		
		if (on && !isCooldownTimerRunning()) {
			cooldownTimerTask = BukkitTools.scheduleAsyncRepeatingTask(new CooldownTimerTask(plugin), 0, 20);
			if (cooldownTimerTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule cooldown timer loop.");			
		} else if (!on && isCooldownTimerRunning()) {
			BukkitTools.getScheduler().cancelTask(cooldownTimerTask);
			cooldownTimerTask = -1;
		}
	}
	
	public static void toggleDrawSmokeTask(boolean on) {
		if (on && !isDrawSmokeTaskRunning()) {
			drawSmokeTask = BukkitTools.scheduleAsyncRepeatingTask(new DrawSmokeTask(plugin), 0, 100);
			if (drawSmokeTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule draw smoke loop");			
		} else if (!on && isDrawSmokeTaskRunning()) {
			BukkitTools.getScheduler().cancelTask(drawSmokeTask);
			drawSmokeTask = -1;
		}
	}
	
	public static void toggleGatherResidentUUIDTask(boolean on) {
		if (on && !isGatherResidentUUIDTaskRunning()) {
			gatherResidentUUIDTask = BukkitTools.scheduleAsyncRepeatingTask(new GatherResidentUUIDTask(plugin) , 1200, 40);
			TownyMessaging.sendDebugMsg("Starting GatherResidentUUIDTask in 60 seconds.");
			if (gatherResidentUUIDTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule gather resident UUID loop");
		} else if (!on && isGatherResidentUUIDTaskRunning()) {
			BukkitTools.getScheduler().cancelTask(gatherResidentUUIDTask);
			gatherResidentUUIDTask = -1;
			TownyMessaging.sendDebugMsg("Shutting down GatherResidentUUIDTask.");
		}
	}
	
	public static void toggleDrawSpointsTask(boolean on) {
		if (on && !isDrawSpawnPointsTaskRunning()) {
			drawSpawnPointsTask = BukkitTools.scheduleAsyncRepeatingTask(new DrawSpawnPointsTask(plugin), 0, 52);
			if (drawSpawnPointsTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule draw spawn points loop");			
		} else if (!on && isDrawSpawnPointsTaskRunning()) {
			BukkitTools.getScheduler().cancelTask(drawSpawnPointsTask);
			drawSpawnPointsTask = -1;
		}
	}

	public static boolean isTownyRepeatingTaskRunning() {

		return townyRepeatingTask != -1;

	}

	public static boolean isMobRemovalRunning() {

		return mobRemoveTask != -1;
	}

	public static boolean isDailyTimerRunning() {

		return dailyTask != -1;
	}

	public static boolean isHourlyTimerRunning() {

		return hourlyTask != -1;
	}

	public static boolean isShortTimerRunning() {

		return shortTask != -1;
	}

	public static boolean isHealthRegenRunning() {

		return healthRegenTask != -1;
	}

	public static boolean isTeleportWarmupRunning() {

		return teleportWarmupTask != -1;
	}
	
	public static boolean isCooldownTimerRunning() {

		return cooldownTimerTask != -1;
	}
	
	public static boolean isDrawSmokeTaskRunning() {
		
		return drawSmokeTask != -1;
	}

	public static boolean isGatherResidentUUIDTaskRunning() {
		
		return gatherResidentUUIDTask != -1;
	}
	
	public static boolean isDrawSpawnPointsTaskRunning() {
		
		return drawSpawnPointsTask != -1;
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

	public static Long getTimeUntilNextHourInSeconds() {
		long timeSinceLastHourMillis = System.currentTimeMillis() % (1000 * 60 * 60);
		long timeSinceLastHourSeconds = timeSinceLastHourMillis / 1000;
		long timeUntilNextHourSeconds = (60 * 60) - timeSinceLastHourSeconds;
		return timeUntilNextHourSeconds;
	}
}
