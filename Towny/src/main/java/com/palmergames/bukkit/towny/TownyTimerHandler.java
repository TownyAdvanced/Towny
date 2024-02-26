package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.DebugRepeatingTimerTask;
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
	// Repeating Tasks
	private static ScheduledTask townyDebugRepeatingTask = null;
	private static ScheduledTask townyRepeatingTask = null;
	private static ScheduledTask mobRemoveTask = null;
	private static ScheduledTask healthRegenTask = null;
	private static ScheduledTask teleportWarmupTask = null;
	
	// Async Repeating Tasks
	private static ScheduledTask hourlyTask = null;
	private static ScheduledTask shortTask = null;
	private static ScheduledTask cooldownTimerTask = null;
	private static ScheduledTask drawSmokeTask = null;
	private static ScheduledTask drawSpawnPointsTask = null;

	private static DrawSmokeTask drawSmokeRunnable;
	private static DrawSpawnPointsTask drawSpawnPointRunnable;
	private static CooldownTimerTask coolDownTimerRunnable;
	private static HourlyTimerTask hourlyTimerRunnable;
	private static ShortTimerTask shortTimerRunnable;
	

	
	public static void newHour() {
		if (!isHourlyTimerRunning())
			toggleHourlyTimer(true);

		plugin.getScheduler().run(new HourlyTimerTask(plugin));
	}

	public static void toggleDebugRepeatingTimer(boolean on) {
		Towny.getPlugin().getLogger().info("Bread Log: toggling debugRepeatingTimerTask: " + (on ? "on" : "off"));
		if (on && !isTownyDebugRepeatingTaskRunning()) {
			townyDebugRepeatingTask = plugin.getScheduler().runRepeating(new DebugRepeatingTimerTask(plugin), 1, TimeTools.convertToTicks(60L));
		} else if (!on && isTownyDebugRepeatingTaskRunning()) {
			townyDebugRepeatingTask.cancel();
			townyDebugRepeatingTask = null;
		}
		Towny.getPlugin().getLogger().info("Bread Log: debugRepeatingTimerTask status: " + String.valueOf(isTownyDebugRepeatingTaskRunning()));
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
			if (hourlyTimerRunnable == null)
				hourlyTimerRunnable = new HourlyTimerTask(plugin);
			hourlyTask = plugin.getScheduler().runAsyncRepeating(() -> hourlyTimerRunnable.run(), TimeTools.convertToTicks(getTimeUntilNextHourInSeconds()), TimeTools.convertToTicks(TownySettings.getHourInterval()));
		} else if (!on && isHourlyTimerRunning()) {
			hourlyTask.cancel();
			hourlyTask = null;
			hourlyTimerRunnable = null; 
		}
	}

	public static void toggleShortTimer(boolean on) {
		if (on && !isShortTimerRunning()) {
			if (shortTimerRunnable == null)
				shortTimerRunnable = new ShortTimerTask(plugin);
			//This small delay is a safeguard against race conditions
			long delayTicks = TimeTools.convertToTicks(60);
			shortTask = plugin.getScheduler().runAsyncRepeating(() -> shortTimerRunnable.run(), delayTicks, TimeTools.convertToTicks(TownySettings.getShortInterval()));
		} else if (!on && isShortTimerRunning()) {
			shortTask.cancel();
			shortTask = null;
			shortTimerRunnable = null;
		}
	}

	public static void toggleHealthRegen(boolean on) {

		if (on && !isHealthRegenRunning()) {
			healthRegenTask = plugin.getScheduler().runRepeating(new HealthRegenTimerTask(plugin, BukkitTools.getServer()), 1, TimeTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
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
			if (coolDownTimerRunnable == null)
				coolDownTimerRunnable = new CooldownTimerTask(plugin);
			cooldownTimerTask = plugin.getScheduler().runAsyncRepeating(() -> coolDownTimerRunnable.run(), 1, 20);
		} else if (!on && isCooldownTimerRunning()) {
			cooldownTimerTask.cancel();
			cooldownTimerTask = null;
			coolDownTimerRunnable = null;
		}
	}
	
	public static void toggleDrawSmokeTask(boolean on) {
		if (on && !isDrawSmokeTaskRunning()) {
			if (drawSmokeRunnable == null)
				drawSmokeRunnable = new DrawSmokeTask(plugin);
			drawSmokeTask = plugin.getScheduler().runAsyncRepeating(() -> drawSmokeRunnable.run(), 1, 40);
		} else if (!on && isDrawSmokeTaskRunning()) {
			drawSmokeTask.cancel();
			drawSmokeTask = null;
			drawSmokeRunnable = null;
		}
	}
	
	public static void toggleDrawSpointsTask(boolean on) {
		if (on && !isDrawSpawnPointsTaskRunning()) {
			if (drawSpawnPointRunnable == null)
				drawSpawnPointRunnable = new DrawSpawnPointsTask(plugin);
			// This is given a delay because it was causing ConcurrentModificationExceptions on startup on one server.
			drawSpawnPointsTask = plugin.getScheduler().runAsyncRepeating(() -> drawSpawnPointRunnable.run(), 40, 52);
		} else if (!on && isDrawSpawnPointsTaskRunning()) {
			drawSpawnPointsTask.cancel();
			drawSpawnPointsTask = null;
			drawSpawnPointRunnable = null;
		}
	}

	public static boolean isTownyDebugRepeatingTaskRunning() {

		Towny.getPlugin().getLogger().info("Bread Log:     debug task is null: " + String.valueOf(townyDebugRepeatingTask == null));
		if (townyDebugRepeatingTask != null)
			Towny.getPlugin().getLogger().info("Bread Log:     debug task is cancelled: " + String.valueOf(townyDebugRepeatingTask.isCancelled()));
		Towny.getPlugin().getLogger().info("Bread Log:     isTownyDebugRepeatingTaskRunning: " + String.valueOf(townyDebugRepeatingTask != null && !townyDebugRepeatingTask.isCancelled()));
		return townyDebugRepeatingTask != null && !townyDebugRepeatingTask.isCancelled();

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
