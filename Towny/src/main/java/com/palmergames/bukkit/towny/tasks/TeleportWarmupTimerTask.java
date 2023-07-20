package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TeleportRequest;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.economy.Account;
import io.papermc.lib.PaperLib;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dumptruckman
 */
public class TeleportWarmupTimerTask extends TownyTimerTask {

	private static final Map<Resident, TeleportRequest> TELEPORT_QUEUE = new ConcurrentHashMap<>();
	
	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:warmup-task"), () -> TownyTimerHandler.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0));
	}

	public TeleportWarmupTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {

		long currentTime = System.currentTimeMillis();
		Iterator<Map.Entry<Resident, TeleportRequest>> iterator = TELEPORT_QUEUE.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<Resident, TeleportRequest> next = iterator.next();
			final Resident resident = next.getKey();
			final TeleportRequest request = next.getValue();

			if (currentTime > request.requestTime() + (TownySettings.getTeleportWarmupTime() * 1000L)) {
				iterator.remove();
				
				Player player = resident.getPlayer();
				// Only teleport & add cooldown if player is valid
				if (player == null)
					continue;
				
				PaperLib.teleportAsync(player, request.destinationLocation(), TeleportCause.COMMAND);
				
				if (request.cooldown() > 0)
					CooldownTimerTask.addCooldownTimer(resident.getName(), "teleport", request.cooldown());
			}
		}
	}

	public static void requestTeleport(Resident resident, Location spawnLoc) {
		requestTeleport(resident, spawnLoc, 0);
	}

	public static void requestTeleport(Resident resident, Location spawnLoc, int cooldown) {
		requestTeleport(resident, spawnLoc, cooldown, null, 0);
	}

	/**
	 * This does not refund any other teleport requests that might be active for the resident, use
	 * {@link #abortTeleportRequest(Resident)} first if you want them to be refunded.
	 */
	public static void requestTeleport(@NotNull Resident resident, @NotNull Location destination, int cooldown, @Nullable Account teleportAccount, double teleportCost) {
		TeleportRequest request = TeleportRequest.teleportRequest(System.currentTimeMillis(), destination, cooldown, teleportCost, teleportAccount);
		TELEPORT_QUEUE.put(resident, request);
	}
	
	/**
	 * Aborts the current active teleport request for the given resident.
	 * @param resident The resident to abort the request for.
	 * @return Whether the resident had an active teleport request.
	 */
	public static boolean abortTeleportRequest(Resident resident) {
		if (resident == null)
			return false;

		TeleportRequest request = TELEPORT_QUEUE.remove(resident);
		if (request == null)
			return false;

		if (request.teleportCost() != 0 && TownyEconomyHandler.isActive() && request.teleportAccount() != null) {
			TownyEconomyHandler.economyExecutor().execute(() -> request.teleportAccount().payTo(request.teleportCost(), resident.getAccount(), Translation.of("msg_cost_spawn_refund")));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_cost_spawn_refund"));
		}

		return true;
	}
	
	public static boolean hasTeleportRequest(@NotNull Resident resident) {
		return TELEPORT_QUEUE.containsKey(resident);
	}
	
	public static TeleportRequest getTeleportRequest(@NotNull Resident resident) {
		return TELEPORT_QUEUE.get(resident);
	}
}
