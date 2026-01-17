package com.palmergames.bukkit.towny.tasks;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

public class CooldownTimerTask extends TownyTimerTask {
	
	private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();

	public enum CooldownType{
		PVP(TownySettings.getPVPCoolDownTime()),
		NEUTRALITY(TownySettings.getPeacefulCoolDownTime()),
		TELEPORT(TownySettings.getSpawnCooldownTime()),
		TOWN_RENAME(60),
		TOWN_DELETE(TownySettings.getTownDeleteCoolDownTime()),
		TOWNBLOCK_UNCLAIM(TownySettings.getTownUnclaimCoolDownTime()),
		OUTLAW_WARNING(TownySettings.getOutlawWarningMessageCooldown()),
		RESIDENT_OUTLAWED(TownySettings.getResidentOutlawWarningMessageCooldown()),
		RESIDENT_UNOUTLAWED(TownySettings.getResidentOutlawWarningMessageCooldown());
		
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
	}

	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();

		COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
	}
	
	public static void addCooldownTimer(String object, CooldownType type) {
		addCooldownTimer(object, type.name(), type.getSeconds());
	}

	public static void addCooldownTimer(String object, String cooldownTypeName, int coolDownSeconds) {
		COOLDOWNS.put(key(object, cooldownTypeName), getCooldownEndTime(coolDownSeconds));
	}

	public static boolean hasCooldown(String object, CooldownType type) {
		return hasCooldown(object, type.name());
	}

	public static boolean hasCooldown(String object, String cooldownTypeName) {
		final Long endTime = COOLDOWNS.get(key(object, cooldownTypeName));
		if (endTime == null)
			return false;
		
		return endTime > System.currentTimeMillis();
	}

	public static int getCooldownRemaining(String object, CooldownType type) {
		return getCooldownRemaining(object, type.name());
	}

	public static int getCooldownRemaining(String object, String cooldownTypeName) {
		return getSecondsRemaining(key(object, cooldownTypeName));
	}

	private static String key(String object, String cooldownTypeName) {
		final String key = object + ":" + cooldownTypeName.toLowerCase(Locale.ROOT);
		
		if (key.length() > 200)
			throw new IllegalArgumentException("Cooldown key length cannot exceed 200 characters, got '" + key + "'.");
		
		return key;
	}

	private static long getCooldownEndTime(int coolDownSeconds) {
		return System.currentTimeMillis() + (coolDownSeconds * 1000L);
	}

	private static int getSecondsRemaining(String key) {
		final Long endTime = COOLDOWNS.get(key);
		if (endTime == null)
			return 0;
		
		return (int) TimeUnit.MILLISECONDS.toSeconds(endTime - System.currentTimeMillis());
	}

	public static Map<String, Long> getCooldowns() {
		return COOLDOWNS;
	}
}
