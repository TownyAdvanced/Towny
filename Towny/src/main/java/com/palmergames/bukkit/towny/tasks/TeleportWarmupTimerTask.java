package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import io.papermc.lib.PaperLib;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author dumptruckman
 */
public class TeleportWarmupTimerTask extends TownyTimerTask {

	private static Queue<Resident> teleportQueue;

	public TeleportWarmupTimerTask(Towny plugin) {

		super(plugin);
		teleportQueue = new ArrayDeque<>();
	}

	@Override
	public void run() {

		long currentTime = System.currentTimeMillis();

		while (true) {
			Resident resident = teleportQueue.peek();
			if (resident == null)
				break;
			if (currentTime > resident.getTeleportRequestTime() + (TownySettings.getTeleportWarmupTime() * 1000)) {
				int cooldown = resident.getTeleportCooldown();
				resident.clearTeleportRequest();
				
				Player p = TownyAPI.getInstance().getPlayer(resident);
				// Only teleport & add cooldown if player is valid
				if (p != null) {
					PaperLib.teleportAsync(p, resident.getTeleportDestination(), TeleportCause.COMMAND);
					if (cooldown > 0)
						CooldownTimerTask.addCooldownTimer(resident.getName(), "teleport", cooldown);
				}
				
				teleportQueue.poll();
			} else {
				break;
			}
		}
	}

	public static void requestTeleport(Resident resident, Location spawnLoc) {
		requestTeleport(resident, spawnLoc, 0);
	}

	public static void requestTeleport(Resident resident, Location spawnLoc, int cooldown) {
		resident.setTeleportRequestTime();
		resident.setTeleportDestination(spawnLoc);
		resident.setTeleportCooldown(cooldown);
		try {
			if (teleportQueue.contains(resident))
				teleportQueue.remove(resident);
			teleportQueue.add(resident);
		} catch (NullPointerException e) {
			Towny.getPlugin().getLogger().severe("Error: Null returned from teleport queue.");
			e.printStackTrace();
		}
	}

	public static void abortTeleportRequest(Resident resident) {

		if (resident != null && teleportQueue.contains(resident)) {
			if (resident.getTeleportCost() != 0 && TownyEconomyHandler.isActive() && resident.getTeleportAccount() != null) {
				resident.getTeleportAccount().payTo(resident.getTeleportCost(), resident.getAccount(), Translation.of("msg_cost_spawn_refund"));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_cost_spawn_refund"));
			}
			resident.clearTeleportRequest();
			teleportQueue.remove(resident);
		}
	}
}
