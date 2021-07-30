package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.ChunkNotification;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.event.BedExplodeEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import com.palmergames.util.TimeMgmt;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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
	private final Map<Player, Integer> playerActionTasks = new HashMap<>();
	private final Map<Player, BossBar> playerBossBarMap = new HashMap<>();

	public TownyCustomListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangePlotEvent(PlayerChangePlotEvent event) {

		Player player = event.getPlayer();
		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();

		if (plugin.hasPlayerMode(player, "townclaim"))
			TownCommand.parseTownClaimCommand(player, new String[] {});
		if (plugin.hasPlayerMode(player, "townunclaim"))
			TownCommand.parseTownUnclaimCommand(player, new String[] {});
		if (plugin.hasPlayerMode(player, "map"))
			TownyCommand.showMap(player);

		// Check if player has entered a new town/wilderness
		try {
			if (to.getTownyWorld().isUsingTowny() && TownySettings.getShowTownNotifications()) {
				Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
				String msg = null;
				try {
					if (resident != null) {
						ChunkNotification chunkNotifier = new ChunkNotification(from, to);
						msg = chunkNotifier.getNotificationString(resident);
					}
				} catch (NullPointerException e) {
					plugin.getLogger().info("ChunkNotifier generated an NPE, this is harmless but if you'd like to report it the following information will be useful:");
					plugin.getLogger().info("  Player: " + player.getName() + "  To: " + to.getWorldName() + "," + to.getX() + "," + to.getZ() + "  From: " + from.getWorldName() + "," + from.getX() + "," + from.getZ());
					e.printStackTrace();
				}
				if (msg != null) {
					TextComponent msgComponent = LegacyComponentSerializer.builder().build().deserialize(msg);
					
					Audience playerAudience = Towny.getAdventure().player(player);
					if (TownySettings.isNotificationsAppearingInActionBar() && !TownySettings.isNotificationsAppearingOnBossbar()) {
						int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION);
						if (seconds > 3) {
							// Vanilla action bar displays for 3 seconds, so we shouldn't bother with any scheduling.
							// Cancel any older tasks running to prevent them from leaking over.
							if (playerActionTasks.get(player) != null) {
								Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
								playerActionTasks.remove(player);
							}
					
							final TextComponent messageComponent = msgComponent;
							AtomicInteger remainingSeconds = new AtomicInteger(seconds);
							int taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
								playerAudience.sendActionBar(messageComponent);
								remainingSeconds.getAndDecrement();
								
								if (remainingSeconds.get() == 0 && playerActionTasks.containsKey(player)) {
									Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
									playerActionTasks.remove(player);
								}
							}, 0, 20L).getTaskId();
							
							playerActionTasks.put(player, taskID);
						} else {
							playerAudience.sendActionBar(msgComponent);
						}
					} else if (TownySettings.isNotificationsAppearingOnBossbar()) {
						int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION);
						BossBar bossBar = BossBar.bossBar(msgComponent, 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

						if (playerBossBarMap.containsKey(player)) {
							Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
							playerAudience.hideBossBar(playerBossBarMap.get(player));
							playerActionTasks.remove(player);
							playerBossBarMap.remove(player);
						}

						playerAudience.showBossBar(bossBar);
						playerBossBarMap.put(player, bossBar);

						int taskID = Bukkit.getScheduler().runTaskLater(plugin, () -> {
							playerAudience.hideBossBar(bossBar);
							playerActionTasks.remove(player);
							playerBossBarMap.remove(player);
						}, seconds*20).getTaskId();

						playerActionTasks.put(player, taskID);						
					} else
						TownyMessaging.sendMessage(player, msg);
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
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCreateTown(NewTownEvent event) {
		Town town = event.getTown();
		double upkeep = TownySettings.getTownUpkeepCost(town);
		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily() && upkeep > 0) {
			String cost = TownyEconomyHandler.getFormattedBalance(upkeep);
			String time = TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime());
			TownyMessaging.sendTownMessagePrefixed(town, Translation.of("msg_new_town_advice", cost, time));
		}
		//TODO: at some point it might be nice to have a written_book given to mayors 
		// which could contain the above advice about depositing money, or containing
		// links to the commands page on the wiki.
		
	}
	
	/**
	 * Runs when a bed or respawn anchor explodes that we can track them in the BlockExplodeEvent,
	 * which always returns AIR for that event's getBlock().
	 * @param event {@link BedExplodeEvent}
	 */
	@EventHandler(priority = EventPriority.NORMAL) 
	public void onBedExplodeEvent(BedExplodeEvent event) {
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(event.getLocation().getWorld().getName());
		world.addBedExplosionAtBlock(event.getLocation(), event.getMaterial());
		if (event.getLocation2() != null);
			world.addBedExplosionAtBlock(event.getLocation2(), event.getMaterial());
		final TownyWorld finalWorld = world;
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                finalWorld.removeBedExplosionAtBlock(event.getLocation());
                finalWorld.removeBedExplosionAtBlock(event.getLocation2());
            }
        }, 20L);
	}
	
	@EventHandler(priority = EventPriority.LOWEST) 
	public void onTownLeaveNation(NationPreTownLeaveEvent event ) {
		if (event.getTown().isConquered()) {
			event.setCancelMessage(Translation.of("msg_err_your_conquered_town_cannot_leave_the_nation_yet"));
			event.setCancelled(true);
		}
	}
}
