package com.palmergames.bukkit.towny.tasks;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

public class CooldownTimerTask extends TownyTimerTask {
	
	private static ConcurrentHashMap<AbstractMap.SimpleEntry<String, CooldownType>, Long> cooldowns;	


	public enum CooldownType{
		PVP(TownySettings.getPVPCoolDownTime()),
		TELEPORT(TownySettings.getSpawnCooldownTime());
		
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
		AbstractMap.SimpleEntry<String, CooldownType> map = new AbstractMap.SimpleEntry<String, CooldownTimerTask.CooldownType>(object, type);		
		cooldowns.put(map, (System.currentTimeMillis() + (type.getSeconds() * 1000)));
	}
	
	public static boolean hasCooldown(String object, CooldownType type) {
		AbstractMap.SimpleEntry<String, CooldownType> map = new AbstractMap.SimpleEntry<String, CooldownTimerTask.CooldownType>(object, type);
		if (cooldowns.containsKey(map))			
			return true;
		return false;
	}
	
	public static int getCooldownRemaining(String object, CooldownType type) {
		AbstractMap.SimpleEntry<String, CooldownType> map = new AbstractMap.SimpleEntry<String, CooldownTimerTask.CooldownType>(object, type);
		if (cooldowns.containsKey(map))
			return (int) ((cooldowns.get(map) - System.currentTimeMillis())/1000);
		return 0;		
	}
}
