package com.palmergames.bukkit.towny.listeners;

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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 4/15/12
 */
public class TownyCustomListener implements Listener {
	private final Towny plugin;

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
				if (msg != null)
					if (Towny.isSpigot && TownySettings.isNotificationsAppearingInActionBar())
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
					else {						
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
