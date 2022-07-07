package com.palmergames.bukkit.towny.tasks;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

public class CooldownTimerTask extends TownyTimerTask {
	
	private static ConcurrentHashMap<AbstractMap.SimpleEntry<String, CooldownType>, Long> cooldowns;


	public enum CooldownType{
		PVP(TownySettings.getPVPCoolDownTime()),
		NEUTRALITY(TownySettings.getPeacefulCoolDownTime()),
		TELEPORT(TownySettings.getSpawnCooldownTime()),
		TOWN_RENAME(60),
		OUTLAW_WARNING(TownySettings.getOutlawWarningMessageCooldown());
		
		private final int seconds;
		
		private int getSeconds() {
			return seconds;
		}

		CooldownType(int seconds) {
			this.seconds = seconds;
		}
		
	}

	public CooldownTimerTask(Towny plugin) {

		super(plugin);
		cooldowns = new ConcurrentHashMap<>();
	}

	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();

		while (!cooldowns.isEmpty()) {
			for (AbstractMap.SimpleEntry<String, CooldownType> map : cooldowns.keySet()) {
				long time = cooldowns.get(map);
				if (time < currentTime)
					cooldowns.remove(map);
			}
			break;
		}
	}
	
	public static void addCooldownTimer(String object, CooldownType type) {
		cooldowns.put(mapOf(object, type), getCooldownEndTime(type));
	}

	public static boolean hasCooldown(String object, CooldownType type) {
		return cooldowns.containsKey(mapOf(object, type));
	}
	
	public static int getCooldownRemaining(String object, CooldownType type) {
		AbstractMap.SimpleEntry<String, CooldownType> map = mapOf(object, type);
		if (cooldowns.containsKey(map))
			return getSecondsRemaining(map);
		return 0;
	}

	private static AbstractMap.SimpleEntry<String, CooldownType> mapOf(String object, CooldownType type) {
		return new AbstractMap.SimpleEntry<String, CooldownTimerTask.CooldownType>(object, type);
	}

	private static long getCooldownEndTime(CooldownType type) {
		return System.currentTimeMillis() + (type.getSeconds() * 1000);
	}

	private static int getSecondsRemaining(AbstractMap.SimpleEntry<String, CooldownType> map) {
		return (int) ((cooldowns.get(map) - System.currentTimeMillis()) / 1000);
	}

}
