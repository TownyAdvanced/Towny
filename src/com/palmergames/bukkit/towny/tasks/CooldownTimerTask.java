package com.palmergames.bukkit.towny.tasks;

import java.util.concurrent.ConcurrentHashMap;
import javafx.util.Pair;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

public class CooldownTimerTask extends TownyTimerTask {
	
	private static ConcurrentHashMap<Pair<Object, CooldownType>, Long> cooldowns;	


	public enum CooldownType{
		PVP(TownySettings.getPVPCoolDownTime()),
		TELEPORT(TownySettings.getSpawnCooldownTime());
		
		private int seconds;
		
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
			for (Pair<Object, CooldownType> map : cooldowns.keySet()) {
				long time = cooldowns.get(map);
				if (time < currentTime)					
					cooldowns.remove(map);
			}
			break;
		}
	}
	
	public static void addCooldownTimer(Object object, CooldownType type) {
		Pair<Object, CooldownType> map = new Pair<Object, CooldownType>(object, type);
		cooldowns.put(map, (System.currentTimeMillis() + (type.getSeconds() * 1000)));
	}
	
	public static boolean hasCooldown(Object object, CooldownType type) {
		Pair<Object, CooldownType> map = new Pair<Object, CooldownType>(object, type);
		if (cooldowns.containsKey(map))			
			return true;
		return false;
	}
	
	public static int getCooldownRemaining(Object object, CooldownType type) {
		Pair<Object, CooldownType> map = new Pair<Object, CooldownType>(object, type);
		if (cooldowns.containsKey(map))
			return (int) ((cooldowns.get(map) - System.currentTimeMillis())/1000);
		return 0;		
	}
}
