package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.ChunkNotification;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.ChunkNotificationEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.util.BukkitTools;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class ChunkNotificationUtil {

	private final static Map<Player, ScheduledTask> playerActionTasks = new HashMap<>();
	private final static Map<Player, BossBar> playerBossBarMap = new HashMap<>();

	public static void showChunkNotification(Player player, Resident resident, WorldCoord to, WorldCoord from) {
		String msg = null;
		try {
			ChunkNotification chunkNotifier = new ChunkNotification(from, to);
			msg = chunkNotifier.getNotificationString(resident);
		} catch (NullPointerException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "ChunkNotifier generated an NPE, this is harmless but if you'd like to report it the following information will be useful: " + System.lineSeparator() +
				"  Player: " + player.getName() + "  To: " + to.getWorldName() + "," + to.getX() + "," + to.getZ() + "  From: " + from.getWorldName() + "," + from.getX() + "," + from.getZ(), e);
		}
		if (msg == null)
			return;

		ChunkNotificationEvent cne = new ChunkNotificationEvent(player, msg, to, from);
		BukkitTools.fireEvent(cne);
		msg = cne.getMessage();
		if (cne.isCancelled() || msg == null || msg.isEmpty())
			return;

		sendChunkNoticiation(player, msg);
	}

	public static void cancelChunkNotificationTasks() {
		List<ScheduledTask> tasks = new ArrayList<>(playerActionTasks.values());
		tasks.forEach(task -> task.cancel());
	}

	private static void sendChunkNoticiation(Player player, String msg) {
		switch (TownySettings.getNotificationsAppearAs().toLowerCase(Locale.ROOT)) {
			case "bossbar" -> sendBossBarChunkNotification(player, TownyComponents.miniMessage(msg));
			case "chat" -> TownyMessaging.sendMessage(player, msg);
			case "none" -> {}
			default -> sendActionBarChunkNotification(player, TownyComponents.miniMessage(msg));
		}
	}
	
	private static void sendActionBarChunkNotification(Player player, Component msgComponent) {
		int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION);
		if (seconds > 3) {
			// Towny is showing the actionbar message longer than vanilla MC allows, using a scheduled task.
			// Cancel any older tasks running to prevent them from leaking over.
			if (playerActionTasks.get(player) != null)
				removePlayerActionTasks(player);
	
			AtomicInteger remainingSeconds = new AtomicInteger(seconds);
			final ScheduledTask task = Towny.getPlugin().getScheduler().runAsyncRepeating(() -> {
				TownyMessaging.sendActionBarMessageToPlayer(player, msgComponent);
				remainingSeconds.getAndDecrement();
				
				if (remainingSeconds.get() == 0 && playerActionTasks.containsKey(player)) 
					removePlayerActionTasks(player);
			}, 0, 20L);
			
			playerActionTasks.put(player, task);
		} else {
			// Vanilla action bar displays for 3 seconds, so we shouldn't bother with any scheduling.
			TownyMessaging.sendActionBarMessageToPlayer(player, msgComponent);
		}
	}

	private static void sendBossBarChunkNotification(Player player, Component message) {
		int ticks = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION) * 20;
		if (playerBossBarMap.containsKey(player)) {
			removePlayerActionTasks(player);
			removePlayerBossBar(player);
		}
		
		final BossBar.Color color = BossBar.Color.NAMES.valueOr(TownySettings.getBossBarNotificationColor().toLowerCase(Locale.ROOT), BossBar.Color.WHITE);
		final BossBar.Overlay overlay = BossBar.Overlay.NAMES.valueOr(TownySettings.getBossBarNotificationOverlay().toLowerCase(Locale.ROOT), BossBar.Overlay.PROGRESS);
		
		final BossBar bossBar = BossBar.bossBar(message, TownySettings.getBossBarNotificationProgress(), color, overlay);

		TownyMessaging.sendBossBarMessageToPlayer(player, bossBar);

		final ScheduledTask task = Towny.getPlugin().getScheduler().runAsyncLater(() -> {
			playerActionTasks.remove(player);
			removePlayerBossBar(player);
		}, ticks);

		playerBossBarMap.put(player, bossBar);
		playerActionTasks.put(player, task);
	}

	private static void removePlayerActionTasks(Player player) {
		final ScheduledTask task = playerActionTasks.remove(player);
		if (task != null)
			task.cancel();
	}

	private static void removePlayerBossBar(Player player) {
		final BossBar bar = playerBossBarMap.remove(player);
		if (bar != null)
			Towny.getAdventure().player(player).hideBossBar(bar);
	}
}
