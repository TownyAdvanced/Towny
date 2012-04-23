/**
 * 
 */
package com.palmergames.bukkit.towny;

import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_DAY;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_REPEATING_TIMER;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_DAILY_TIMER;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_HEALTH_REGEN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_MOB_REMOVAL;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_TELEPORT_WARMUP;

import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.DailyTimerTask;
import com.palmergames.bukkit.towny.tasks.HealthRegenTimerTask;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.RepeatingTimerTask;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;


/**
 * Handler for all running timers
 * 
 * @author ElgarL
 *
 */
public class TownyTimerHandler{
	
	public TownyTimerHandler (Towny plugin) {
		
		TownyTimerHandler.plugin = plugin;
		universe = plugin.getTownyUniverse();
	}
	
	private static Towny plugin;
	private TownyUniverse universe;
	
	private int townyRepeatingTask = -1;
	private int dailyTask = -1;
	private int mobRemoveTask = -1;
	private int healthRegenTask = -1;
	private int teleportWarmupTask = -1;

	public void newDay() {

		if (!isDailyTimerRunning())
			toggleDailyTimer(true);
		//dailyTimer.schedule(new DailyTimerTask(this), 0);
		if (plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new DailyTimerTask(universe)) == -1)
			TownyMessaging.sendErrorMsg("Could not schedule newDay.");
		universe.setChangedNotify(NEW_DAY);
	}

	public void toggleTownyRepeatingTimer(boolean on) {

		if (on && !isTownyRepeatingTaskRunning()) {
			townyRepeatingTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new RepeatingTimerTask(universe), 0, TimeTools.convertToTicks(1L));
			if (townyRepeatingTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule Towny Timer Task.");
		} else if (!on && isTownyRepeatingTaskRunning()) {
			plugin.getServer().getScheduler().cancelTask(townyRepeatingTask);
			townyRepeatingTask = -1;
		}
		universe.setChangedNotify(TOGGLE_REPEATING_TIMER);
	}

	public void toggleMobRemoval(boolean on) {

		if (on && !isMobRemovalRunning()) {
			mobRemoveTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new MobRemovalTimerTask(universe, plugin.getServer()), 0, TimeTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
			if (mobRemoveTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule mob removal loop.");
		} else if (!on && isMobRemovalRunning()) {
			plugin.getServer().getScheduler().cancelTask(mobRemoveTask);
			mobRemoveTask = -1;
		}
		plugin.getTownyUniverse().setChangedNotify(TOGGLE_MOB_REMOVAL);
	}

	public void toggleDailyTimer(boolean on) {

		if (on && !isDailyTimerRunning()) {
			long timeTillNextDay = townyTime();
			TownyMessaging.sendMsg("Time until a New Day: " + TimeMgmt.formatCountdownTime(timeTillNextDay));
			dailyTask = plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new DailyTimerTask(universe), TimeTools.convertToTicks(timeTillNextDay), TimeTools.convertToTicks(TownySettings.getDayInterval()));
			if (dailyTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule new day loop.");
		} else if (!on && isDailyTimerRunning()) {
			plugin.getServer().getScheduler().cancelTask(dailyTask);
			dailyTask = -1;
		}
		universe.setChangedNotify(TOGGLE_DAILY_TIMER);
	}

	public void toggleHealthRegen(boolean on) {

		if (on && !isHealthRegenRunning()) {
			healthRegenTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new HealthRegenTimerTask(universe, plugin.getServer()), 0, TimeTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
			if (healthRegenTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule health regen loop.");
		} else if (!on && isHealthRegenRunning()) {
			plugin.getServer().getScheduler().cancelTask(healthRegenTask);
			healthRegenTask = -1;
		}
		universe.setChangedNotify(TOGGLE_HEALTH_REGEN);
	}

	public void toggleTeleportWarmup(boolean on) {

		if (on && !isTeleportWarmupRunning()) {
			teleportWarmupTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new TeleportWarmupTimerTask(universe), 0, 20);
			if (teleportWarmupTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule teleport warmup loop.");
		} else if (!on && isTeleportWarmupRunning()) {
			plugin.getServer().getScheduler().cancelTask(teleportWarmupTask);
			teleportWarmupTask = -1;
		}
		universe.setChangedNotify(TOGGLE_TELEPORT_WARMUP);
	}

	public boolean isTownyRepeatingTaskRunning() {

		return townyRepeatingTask != -1;

	}

	public boolean isMobRemovalRunning() {

		return mobRemoveTask != -1;
	}

	public boolean isDailyTimerRunning() {

		return dailyTask != -1;
	}

	public boolean isHealthRegenRunning() {

		return healthRegenTask != -1;
	}

	public boolean isTeleportWarmupRunning() {

		return teleportWarmupTask != -1;
	}
	
	public static Long townyTime() {

		Long oneDay = TownySettings.getDayInterval() * 1000;
		Long time = ((TownySettings.getNewDayTime() * 1000) - (System.currentTimeMillis() % oneDay)) / 1000;

		time = time - 3600;

		if (time < 0)
			time = (oneDay / 1000) - Math.abs(time);

		return time % oneDay;
	}

}
