package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;

import io.papermc.lib.PaperLib;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayDeque;
import java.util.Arrays;
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
				resident.clearTeleportRequest();
				// Make sure the chunk we teleport to is loaded.
				Chunk chunk = resident.getTeleportDestination().getWorld().getChunkAt(resident.getTeleportDestination().getBlock());
				if (!chunk.isLoaded()) {
					chunk.load();
				}
				Player p = TownyAPI.getInstance().getPlayer(resident);
				if (p == null) {
					return;
				}
				PaperLib.teleportAsync(p, resident.getTeleportDestination(), TeleportCause.COMMAND);
				if (TownySettings.getSpawnCooldownTime() > 0)
					CooldownTimerTask.addCooldownTimer(resident.getName(), CooldownType.TELEPORT);
				teleportQueue.poll();
			} else {
				break;
			}
		}
	}

	public static void requestTeleport(Resident resident, Location spawnLoc) {

		resident.setTeleportRequestTime();
		resident.setTeleportDestination(spawnLoc);
		try {
			teleportQueue.add(resident);
		} catch (NullPointerException e) {
			System.out.println("[Towny] Error: Null returned from teleport queue.");
			System.out.println(Arrays.toString(e.getStackTrace()));
		}
	}

	public static void abortTeleportRequest(Resident resident) {

		if (resident != null && teleportQueue.contains(resident)) {
			resident.clearTeleportRequest();
			teleportQueue.remove(resident);
			if ((resident.getTeleportCost() != 0) && (TownySettings.isUsingEconomy())) {
				try {
					resident.getAccount().collect(resident.getTeleportCost(), TownySettings.getLangString("msg_cost_spawn_refund"));
					resident.setTeleportCost(0);
					TownyMessaging.sendResidentMessage(resident, TownySettings.getLangString("msg_cost_spawn_refund"));
				} catch (EconomyException e) {
					// Economy error trap
					e.printStackTrace();
				} catch (TownyException e) {
					// Resident not registered exception.
				}

			}
		}
	}
}
