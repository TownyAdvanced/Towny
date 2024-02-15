package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyTimerHandler;

public class DebugRepeatingTimerTask extends TownyTimerTask {

	public DebugRepeatingTimerTask(Towny plugin) {

		super(plugin);
	}

	long counter = 0L;
	long townyRepeatingTask = 0L;
	long townMobRemovalTask = 0L;
	long dailyTimerTask = 0L;
	long hourlyTimerTask = 0L;
	long shortTimerTask = 0L;
	long healthTimerTask = 0L;
	long teleportWarmupTask = 0L;
	long teleportCooldownTask = 0L;
	long drawSmokeTask = 0L;
	long drawSpawnPointTask = 0L;
	@Override
	public void run() {
		counter++;
		plugin.getLogger().info("Bread Log: TownyTimerHandler status report #" + counter + ".");

		townyRepeatingTask();
		townMobRemovalTask();
		dailyTimerTask();
		hourlyTimerTask();
		shortTimerTask();
		healthTimerTask();
		teleportWarmupTask();
		teleportCooldownTask();
		drawSmokeTask();
		drawSpawnPointTask();
		plugin.getLogger().info("Bread Log: TownyTimerHandler status report completed.");
	}
	private String status(boolean on) {
		return on ? "active" : "inactive";
	}
	private void townyRepeatingTask() {
		boolean active = TownyTimerHandler.isTownyRepeatingTaskRunning();
		plugin.getLogger().info("Bread Log:   townyRepeatingTask status: " + status(active));
		if (active) {
			townyRepeatingTask++;
			plugin.getLogger().info("Bread Log:    townyRepeatingTask has been confirmed active " + townyRepeatingTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart townyRepeatingTask!");
			TownyTimerHandler.toggleTownyRepeatingTimer(true);
			townyRepeatingTask = 0L;
		}
	}

	private void townMobRemovalTask() {
		boolean active = TownyTimerHandler.isMobRemovalRunning();
		plugin.getLogger().info("Bread Log:   townMobRemovalTask status: " + status(active));
		if (active) {
			townMobRemovalTask++;
			plugin.getLogger().info("Bread Log:    townMobRemovalTask has been confirmed active " + townMobRemovalTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart townMobRemovalTask!");
			TownyTimerHandler.toggleMobRemoval(true);
			townMobRemovalTask = 0L;
		}
	}

	private void dailyTimerTask() {
		boolean active = NewDayScheduler.isNewDaySchedulerRunning();
		plugin.getLogger().info("Bread Log:   dailyTimerTask status: " + status(active));
		if (active) {
			dailyTimerTask++;
			plugin.getLogger().info("Bread Log:    dailyTimerTask has been confirmed active " + dailyTimerTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart dailyTimerTask!");
			TownyTimerHandler.toggleDailyTimer(true);
			dailyTimerTask = 0L;
		}
	}

	private void hourlyTimerTask() {
		boolean active = TownyTimerHandler.isHourlyTimerRunning();
		plugin.getLogger().info("Bread Log:   hourlyTimerTask status: " + status(active));
		if (active) {
			hourlyTimerTask++;
			plugin.getLogger().info("Bread Log:    hourlyTimerTask has been confirmed active " + hourlyTimerTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart hourlyTimerTask!");
			TownyTimerHandler.toggleHourlyTimer(true);
			hourlyTimerTask = 0L;
		}
	}

	private void shortTimerTask() {
		boolean active = TownyTimerHandler.isShortTimerRunning();
		plugin.getLogger().info("Bread Log:   shortTimerTask status: " + status(active));
		if (active) {
			shortTimerTask++;
			plugin.getLogger().info("Bread Log:    shortTimerTask has been confirmed active " + shortTimerTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart shortTimerTask!");
			TownyTimerHandler.toggleShortTimer(true);
			shortTimerTask = 0L;
		}
	}

	private void healthTimerTask() {
		boolean active = TownyTimerHandler.isHealthRegenRunning();
		plugin.getLogger().info("Bread Log:   healthTimerTask status: " + status(active));
		if (active) {
			healthTimerTask++;
			plugin.getLogger().info("Bread Log:    healthTimerTask has been confirmed active " + healthTimerTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart healthTimerTask!");
			TownyTimerHandler.toggleHealthRegen(true);
			healthTimerTask = 0L;
		}
	}

	private void teleportWarmupTask() {
		boolean active = TownyTimerHandler.isTeleportWarmupRunning();
		plugin.getLogger().info("Bread Log:   teleportWarmupTask status: " + status(active));
		if (active) {
			teleportWarmupTask++;
			plugin.getLogger().info("Bread Log:    teleportWarmupTask has been confirmed active " + teleportWarmupTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart teleportWarmupTask!");
			TownyTimerHandler.toggleTeleportWarmup(true);
			teleportWarmupTask = 0L;
		}
	}

	private void teleportCooldownTask() {
		boolean active = TownyTimerHandler.isCooldownTimerRunning();
		plugin.getLogger().info("Bread Log:   teleportCooldownTask status: " + status(active));
		if (active) {
			teleportCooldownTask++;
			plugin.getLogger().info("Bread Log:    teleportCooldownTask has been confirmed active " + teleportCooldownTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart teleportCooldownTask!");
			TownyTimerHandler.toggleCooldownTimer(true);
			teleportCooldownTask = 0L;
		}
	}

	private void drawSmokeTask() {
		boolean active = TownyTimerHandler.isDrawSmokeTaskRunning();
		plugin.getLogger().info("Bread Log:   drawSmokeTask status: " + status(active));
		if (active) {
			drawSmokeTask++;
			plugin.getLogger().info("Bread Log:    drawSmokeTask has been confirmed active " + drawSmokeTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart drawSmokeTask!");
			TownyTimerHandler.toggleDrawSmokeTask(true);
			drawSmokeTask = 0L;
		}
	}

	private void drawSpawnPointTask() {
		boolean active = TownyTimerHandler.isDrawSpawnPointsTaskRunning();
		plugin.getLogger().info("Bread Log:   drawSpawnPointTask status: " + status(active));
		if (active) {
			drawSpawnPointTask++;
			plugin.getLogger().info("Bread Log:    drawSpawnPointTask has been confirmed active " + drawSpawnPointTask + " times in a row.");
		} else {
			plugin.getLogger().info("Bread Log:    Attempting to restart drawSpawnPointTask!");
			TownyTimerHandler.toggleDrawSpointsTask(true);
			drawSpawnPointTask = 0L;
		}
	}
}
