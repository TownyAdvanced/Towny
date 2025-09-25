package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.event.teleport.CancelledTownyTeleportEvent;
import com.palmergames.bukkit.towny.event.teleport.CancelledTownyTeleportEvent.CancelledTeleportReason;
import com.palmergames.bukkit.towny.event.teleport.SuccessfulTownyTeleportEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TeleportWarmupParticle;
import com.palmergames.bukkit.towny.object.TeleportRequest;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.Contract;
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
		int teleportWarmupTime = TownySettings.getTeleportWarmupTime();
		Iterator<Map.Entry<Resident, TeleportRequest>> iterator = TELEPORT_QUEUE.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<Resident, TeleportRequest> next = iterator.next();
			final Resident resident = next.getKey();
			final TeleportRequest request = next.getValue();
			long teleportTime = request.requestTime() + (teleportWarmupTime * 1000L);

			if (currentTime > teleportTime) {
				iterator.remove();
				
				Player player = resident.getPlayer();
				// Only teleport & add cooldown if player is valid
				if (player == null)
					continue;

				// Teleporting a player can cause the chunk to unload too fast, abandoning pets.
				SpawnUtil.addAndRemoveChunkTicket(WorldCoord.parseWorldCoord(player.getLocation()));

				player.teleportAsync(request.destinationLocation(), TeleportCause.COMMAND).thenAccept(successfulTeleport -> {
					if (successfulTeleport)
						BukkitTools.fireEvent(new SuccessfulTownyTeleportEvent(resident, request.destinationLocation(), request.teleportCost()));
				});

				if (request.cooldown() > 0)
					CooldownTimerTask.addCooldownTimer(resident.getName(), "teleport", request.cooldown());
				continue;
			}

			long millis = teleportTime - currentTime;
			int seconds = (int) Math.max(1, millis/1000);
			// Send a title message.
			if (TownySettings.isTeleportWarmupUsingTitleMessage() && millis >= 1000) {
				String title = TownySettings.isMovementCancellingSpawnWarmup() ? Translatable.of("teleport_warmup_title_dont_move").forLocale(resident) : "";
				String subtitle = Translatable.of("teleport_warmup_subtitle_seconds_remaining", seconds).forLocale(resident);
				resident.getPlayer().showTitle(Title.title(Component.text(title), Component.text(subtitle), 0, 25, (seconds == 1 ? 15 : 0)));
			}
			// Send a particle that drops from above the player to their feet over the course of the warmup.
			if (TownySettings.isTeleportWarmupShowingParticleEffect()) {
				double progress = (double) (teleportWarmupTime - seconds) / teleportWarmupTime;
				double yOffset = 2.0 + (progress * -2.0);
				TeleportWarmupParticle.drawParticles(resident.getPlayer(), yOffset);
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
	@Contract("null -> false")
	public static boolean abortTeleportRequest(@Nullable Resident resident) {
		return abortTeleportRequest(resident, CancelledTeleportReason.UNKNOWN);
	}

	/**
	 * Aborts the current active teleport request for the given resident.
	 * @param resident The resident to abort the request for.
	 * @param reason   The CancelledSpawnReason this player has had their teleport request cancel.
	 * @return Whether the resident had an active teleport request.
	 */
	@Contract("null, _ -> false")
	public static boolean abortTeleportRequest(@Nullable Resident resident, CancelledTeleportReason reason) {
		if (resident == null)
			return false;

		TeleportRequest request = TELEPORT_QUEUE.remove(resident);
		if (request == null)
			return false;

		if (request.teleportCost() != 0 && TownyEconomyHandler.isActive() && request.teleportAccount() != null) {
			TownyEconomyHandler.economyExecutor().execute(() -> request.teleportAccount().payTo(request.teleportCost(), resident, Translation.of("msg_cost_spawn_refund")));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_cost_spawn_refund"));
		}

		BukkitTools.fireEvent(new CancelledTownyTeleportEvent(resident, request.destinationLocation(), request.teleportCost(), reason));

		return true;
	}
	
	public static boolean hasTeleportRequest(@NotNull Resident resident) {
		return TELEPORT_QUEUE.containsKey(resident);
	}
	
	public static TeleportRequest getTeleportRequest(@NotNull Resident resident) {
		return TELEPORT_QUEUE.get(resident);
	}
}
