package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DrawSmokeTask;
import com.palmergames.bukkit.towny.tasks.DrawSpawnPointsTask;
import com.palmergames.bukkit.towny.tasks.HealthRegenTimerTask;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.RepeatingTimerTask;
import com.palmergames.bukkit.towny.tasks.NewDayScheduler;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.tasks.HourlyTimerTask;
import com.palmergames.bukkit.towny.tasks.ShortTimerTask;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;

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
	
	private static ScheduledTask townyRepeatingTask = null;
	private static ScheduledTask hourlyTask = null;
	private static ScheduledTask shortTask = null;
	private static ScheduledTask mobRemoveTask = null;
	private static ScheduledTask healthRegenTask = null;
	private static ScheduledTask teleportWarmupTask = null;
	private static ScheduledTask cooldownTimerTask = null;
	private static ScheduledTask drawSmokeTask = null;
	private static ScheduledTask drawSpawnPointsTask = null;

	public static void newHour() {
		if (!isHourlyTimerRunning())
			toggleHourlyTimer(true);

		plugin.getScheduler().run(new HourlyTimerTask(plugin));
	}

	public static void toggleTownyRepeatingTimer(boolean on) {

		if (on && !isTownyRepeatingTaskRunning()) {
			townyRepeatingTask = plugin.getScheduler().runRepeating(new RepeatingTimerTask(plugin), 1, TimeTools.convertToTicks(1L));
		} else if (!on && isTownyRepeatingTaskRunning()) {
			townyRepeatingTask.cancel();
			townyRepeatingTask = null;
		}
	}

	public static void toggleMobRemoval(boolean on) {

		if (on && !isMobRemovalRunning()) {
			mobRemoveTask = plugin.getScheduler().runRepeating(new MobRemovalTimerTask(plugin), 1, TimeTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
		} else if (!on && isMobRemovalRunning()) {
			mobRemoveTask.cancel();
			mobRemoveTask = null;
		}
	}

	public static void toggleDailyTimer(boolean on) {

		if (on && !NewDayScheduler.isNewDaySchedulerRunning())
			plugin.getScheduler().runAsync(new NewDayScheduler(plugin));
		else if (!on && NewDayScheduler.isNewDaySchedulerRunning())
			NewDayScheduler.cancelScheduledNewDay();
	}

	public static void toggleHourlyTimer(boolean on) {
		if (on && !isHourlyTimerRunning()) {
			hourlyTask = plugin.getScheduler().runAsyncRepeating(new HourlyTimerTask(plugin), TimeTools.convertToTicks(getTimeUntilNextHourInSeconds()), TimeTools.convertToTicks(TownySettings.getHourInterval()));
		} else if (!on && isHourlyTimerRunning()) {
			hourlyTask.cancel();
			hourlyTask = null;
		}
	}

	public static void toggleShortTimer(boolean on) {
		if (on && !isShortTimerRunning()) {
			//This small delay is a safeguard against race conditions
			long delayTicks = TimeTools.convertToTicks(60);
			shortTask = plugin.getScheduler().runAsyncRepeating(new ShortTimerTask(plugin), delayTicks, TimeTools.convertToTicks(TownySettings.getShortInterval()));
		} else if (!on && isShortTimerRunning()) {
			shortTask.cancel();
			shortTask = null;
		}
	}

	public static void toggleHealthRegen(boolean on) {

		if (on && !isHealthRegenRunning()) {
			healthRegenTask = plugin.getScheduler().runAsyncRepeating(new HealthRegenTimerTask(plugin, BukkitTools.getServer()), 1, TimeTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
		} else if (!on && isHealthRegenRunning()) {
			healthRegenTask.cancel();
			healthRegenTask = null;
		}
	}

	public static void toggleTeleportWarmup(boolean on) {

		if (on && !isTeleportWarmupRunning()) {
			teleportWarmupTask = plugin.getScheduler().runRepeating(new TeleportWarmupTimerTask(plugin), 1, 20);
		} else if (!on && isTeleportWarmupRunning()) {
			teleportWarmupTask.cancel();
			teleportWarmupTask = null;
		}
	}
	
	public static void toggleCooldownTimer(boolean on) {
		
		if (on && !isCooldownTimerRunning()) {
			cooldownTimerTask = plugin.getScheduler().runAsyncRepeating(new CooldownTimerTask(plugin), 1, 20);
		} else if (!on && isCooldownTimerRunning()) {
			cooldownTimerTask.cancel();
			cooldownTimerTask = null;
		}
	}
	
	public static void toggleDrawSmokeTask(boolean on) {
		if (on && !isDrawSmokeTaskRunning()) {
			drawSmokeTask = plugin.getScheduler().runAsyncRepeating(new DrawSmokeTask(plugin), 1, 40);
		} else if (!on && isDrawSmokeTaskRunning()) {
			drawSmokeTask.cancel();
			drawSmokeTask = null;
		}
	}
	
	public static void toggleDrawSpointsTask(boolean on) {
		if (on && !isDrawSpawnPointsTaskRunning()) {
			// This is given a delay because it was causing ConcurrentModificationExceptions on startup on one server.
			drawSpawnPointsTask = plugin.getScheduler().runAsyncRepeating(new DrawSpawnPointsTask(plugin), 40, 52);
		} else if (!on && isDrawSpawnPointsTaskRunning()) {
			drawSpawnPointsTask.cancel();
			drawSpawnPointsTask = null;
		}
	}

	public static boolean isTownyRepeatingTaskRunning() {

		return townyRepeatingTask != null && !townyRepeatingTask.isCancelled();

	}

	public static boolean isMobRemovalRunning() {

		return mobRemoveTask != null && !mobRemoveTask.isCancelled();
	}

	public static boolean isHourlyTimerRunning() {

		return hourlyTask != null && !hourlyTask.isCancelled();
	}

	public static boolean isShortTimerRunning() {

		return shortTask != null && !shortTask.isCancelled();
	}

	public static boolean isHealthRegenRunning() {

		return healthRegenTask != null && !healthRegenTask.isCancelled();
	}

	public static boolean isTeleportWarmupRunning() {

		return teleportWarmupTask != null && !teleportWarmupTask.isCancelled();
	}
	
	public static boolean isCooldownTimerRunning() {

		return cooldownTimerTask != null && !cooldownTimerTask.isCancelled();
	}
	
	public static boolean isDrawSmokeTaskRunning() {
		
		return drawSmokeTask != null && !drawSmokeTask.isCancelled();
	}

	public static boolean isDrawSpawnPointsTaskRunning() {
		
		return drawSpawnPointsTask != null && !drawSpawnPointsTask.isCancelled();
	}

	public static Long getTimeUntilNextHourInSeconds() {
		long timeSinceLastHourMillis = System.currentTimeMillis() % (1000 * 60 * 60);
		long timeSinceLastHourSeconds = timeSinceLastHourMillis / 1000;
		return (60 * 60) - timeSinceLastHourSeconds;
	}
}
