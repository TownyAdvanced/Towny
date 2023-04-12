package com.palmergames.bukkit.towny.tasks;

import java.util.AbstractMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

public class CooldownTimerTask extends TownyTimerTask {
	
	private static ConcurrentHashMap<AbstractMap.SimpleEntry<String, String>, Long> cooldowns;


	public enum CooldownType{
		PVP(TownySettings.getPVPCoolDownTime()),
		NEUTRALITY(TownySettings.getPeacefulCoolDownTime()),
		TELEPORT(TownySettings.getSpawnCooldownTime()),
		TOWN_RENAME(60),
		TOWN_DELETE(TownySettings.getTownDeleteCoolDownTime()),
		TOWNBLOCK_UNCLAIM(TownySettings.getTownUnclaimCoolDownTime()),
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
			for (AbstractMap.SimpleEntry<String, String> map : cooldowns.keySet()) {
				long time = cooldowns.get(map);
				if (time < currentTime)
					cooldowns.remove(map);
			}
			break;
		}
	}
	
	public static void addCooldownTimer(String object, CooldownType type) {
		addCooldownTimer(object, type.name(), type.getSeconds());
	}

	public static void addCooldownTimer(String object, String cooldownTypeName, int coolDownSeconds) {
		cooldowns.put(mapOf(object, cooldownTypeName.toLowerCase(Locale.ROOT)), getCooldownEndTime(coolDownSeconds));
	}

	public static boolean hasCooldown(String object, CooldownType type) {
		return hasCooldown(object, type.name());
	}

	public static boolean hasCooldown(String object, String cooldownTypeName) {
		return cooldowns.containsKey(mapOf(object, cooldownTypeName.toLowerCase(Locale.ROOT)));
	}

	public static int getCooldownRemaining(String object, CooldownType type) {
		return getCooldownRemaining(object, type.name());
	}

	public static int getCooldownRemaining(String object, String cooldownTypeName) {
		AbstractMap.SimpleEntry<String, String> map = mapOf(object, cooldownTypeName.toLowerCase(Locale.ROOT));
		if (cooldowns.containsKey(map))
			return getSecondsRemaining(map);
		return 0;
	}

	private static AbstractMap.SimpleEntry<String, String> mapOf(String object, String cooldownTypeName) {
		return new AbstractMap.SimpleEntry<String, String>(object, cooldownTypeName);
	}

	private static Long getCooldownEndTime(int coolDownSeconds) {
		return System.currentTimeMillis() + (coolDownSeconds * 1000);
	}

	private static int getSecondsRemaining(AbstractMap.SimpleEntry<String, String> map) {
		return (int) ((cooldowns.get(map) - System.currentTimeMillis()) / 1000);
	}

}
