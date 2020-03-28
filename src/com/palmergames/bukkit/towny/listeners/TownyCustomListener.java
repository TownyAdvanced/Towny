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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 4/15/12
 */
public class TownyCustomListener implements Listener {
	private final Towny plugin;
	private ConcurrentHashMap<Player, Integer> playerActionTasks = new ConcurrentHashMap<>();

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
				
				// Cancel any older tasks running to prevent them from leaking over.
				if (playerActionTasks.get(player) != null) {
					Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
				}
				
				int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_ACTIONBAR_DURATION);;
				if (msg != null)
					if (Towny.isSpigot && TownySettings.isNotificationsAppearingInActionBar()) {
						int taskID = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg)), 0, 20L).getTaskId();
						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							
							// Cancel task.
							Bukkit.getScheduler().cancelTask(taskID);
							
							// Remove cached task.
							playerActionTasks.remove(player);
						}, 20L * seconds);
						
						// Cache ID
						playerActionTasks.put(player, taskID);
					} else {						
						player.sendMessage(msg);
					}
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
