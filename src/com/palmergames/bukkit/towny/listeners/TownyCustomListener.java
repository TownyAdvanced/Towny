package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.ChunkNotification;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 4/15/12
 */
public class TownyCustomListener implements Listener {
	private final Towny plugin;
	private Map<Player, Integer> playerActionTasks = new HashMap<>();

	public TownyCustomListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangePlotEvent(PlayerChangePlotEvent event) {

		Player player = event.getPlayer();
		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();

		// TODO: Player mode
		if (plugin.hasPlayerMode(player, "townclaim"))
			TownCommand.parseTownClaimCommand(player, new String[] {});
		if (plugin.hasPlayerMode(player, "townunclaim"))
			TownCommand.parseTownUnclaimCommand(player, new String[] {});
		if (plugin.hasPlayerMode(player, "map"))
			TownyCommand.showMap(player);

		// claim: attempt to claim area
		// claim remove: remove area from town

		// Check if player has entered a new town/wilderness
		try {
			if (to.getTownyWorld().isUsingTowny() && TownySettings.getShowTownNotifications()) {
				Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
				ChunkNotification chunkNotifier = new ChunkNotification(from, to);
				String msg = chunkNotifier.getNotificationString(resident);
				
				int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_ACTIONBAR_DURATION);
				if (seconds > 3) { // Vanilla action bar displays for 3 seconds, so we shouldn't bother with any scheduling.
					// Cancel any older tasks running to prevent them from leaking over.
					if (playerActionTasks.get(player) != null) {
						Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
						playerActionTasks.remove(player);
					}
					
					if (msg != null)
						if (Towny.isSpigot && TownySettings.isNotificationsAppearingInActionBar()) {

							AtomicInteger remainingSeconds = new AtomicInteger(seconds);
							int taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
								player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
								remainingSeconds.getAndDecrement();
								
								if (remainingSeconds.get() == 0) {
									Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
									playerActionTasks.remove(player);
								}
							}, 0, 20L).getTaskId();
							
							playerActionTasks.put(player, taskID);
						} else {						
							player.sendMessage(msg);
						}
				} else 
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
			}
		} catch (NotRegisteredException e) {
			// likely Citizens' NPC
		}

		if (plugin.hasPlayerMode(player, "plotborder")) {
			CellBorder cellBorder = BorderUtil.getPlotBorder(to);
			cellBorder.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.sendToPlayer(player));
		}
	}
}
