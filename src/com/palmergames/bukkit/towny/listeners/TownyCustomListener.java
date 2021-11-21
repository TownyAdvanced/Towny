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
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import com.palmergames.util.TimeMgmt;

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
		
		if (!TownyAPI.getInstance().isTownyWorld(to.getBukkitWorld()))
			return;
		
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null)
			return;

		if (resident.hasMode("townclaim"))
			TownCommand.parseTownClaimCommand(player, new String[] {});
		if (resident.hasMode("townunclaim"))
			TownCommand.parseTownUnclaimCommand(player, new String[] {});
		if (resident.hasMode("map"))
			TownyCommand.showMap(player);
		if (resident.hasMode("plotborder"))
			BorderUtil.getPlotBorder(to).runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.sendToPlayer(player));

		// Check if player has entered a new town/wilderness
		if (TownySettings.getShowTownNotifications()) {
			String msg = null;
			try {
				ChunkNotification chunkNotifier = new ChunkNotification(from, to);
				msg = chunkNotifier.getNotificationString(resident);
			} catch (NullPointerException e) {
				plugin.getLogger().info("ChunkNotifier generated an NPE, this is harmless but if you'd like to report it the following information will be useful:");
				plugin.getLogger().info("  Player: " + player.getName() + "  To: " + to.getWorldName() + "," + to.getX() + "," + to.getZ() + "  From: " + from.getWorldName() + "," + from.getX() + "," + from.getZ());
				e.printStackTrace();
			}
			if (msg == null)
				return;

			sendChunkNoticiation(player, msg);
		}
	}
	
	private void sendChunkNoticiation(Player player, String msg) {
		if (TownySettings.isNotificationsAppearingInActionBar() && !TownySettings.isNotificationsAppearingOnBossbar())
			sendActionBarChunkNotification(player, LegacyComponentSerializer.builder().build().deserialize(msg));
		else if (TownySettings.isNotificationsAppearingOnBossbar())
			sendBossBarChunkNotification(player, BossBar.bossBar(LegacyComponentSerializer.builder().build().deserialize(msg), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS));
		else
			TownyMessaging.sendMessage(player, msg);
	}
	
	private void sendActionBarChunkNotification(Player player, TextComponent msgComponent) {
		int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION);
		if (seconds > 3) {
			// Towny is showing the actionbar message longer than vanilla MC allows, using a scheduled task.
			// Cancel any older tasks running to prevent them from leaking over.
			if (playerActionTasks.get(player) != null)
				removePlayerActionTasks(player);
	
			AtomicInteger remainingSeconds = new AtomicInteger(seconds);
			int taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
				TownyMessaging.sendActionBarMessageToPlayer(player, msgComponent);
				remainingSeconds.getAndDecrement();
				
				if (remainingSeconds.get() == 0 && playerActionTasks.containsKey(player)) 
					removePlayerActionTasks(player);
			}, 0, 20L).getTaskId();
			
			playerActionTasks.put(player, taskID);
		} else {
			// Vanilla action bar displays for 3 seconds, so we shouldn't bother with any scheduling.
			TownyMessaging.sendActionBarMessageToPlayer(player, msgComponent);
		}
	}

	private void sendBossBarChunkNotification(Player player, BossBar bossBar) {
		int seconds = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION) * 20;
		if (playerBossBarMap.containsKey(player)) {
			removePlayerActionTasks(player);
			removePlayerBossBar(player);
		}

		TownyMessaging.sendBossBarMessageToPlayer(player, bossBar);

		int taskID = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			playerActionTasks.remove(player);
			removePlayerBossBar(player);
		}, seconds).getTaskId();

		playerBossBarMap.put(player, bossBar);
		playerActionTasks.put(player, taskID);
	}
	
	private void removePlayerActionTasks(Player player) {
		Bukkit.getScheduler().cancelTask(playerActionTasks.get(player));
		playerActionTasks.remove(player);
	}
	
	private void removePlayerBossBar(Player player) {
		Towny.getAdventure().player(player).hideBossBar(playerBossBarMap.get(player));
		playerBossBarMap.remove(player);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCreateTown(NewTownEvent event) {
		Town town = event.getTown();
		double upkeep = TownySettings.getTownUpkeepCost(town);
		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily() && upkeep > 0) {
			String cost = TownyEconomyHandler.getFormattedBalance(upkeep);
			String time = TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime());
			TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_new_town_advice", cost, time));
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
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true) 
	public void onPlayerDamagePlayerEvent(TownyPlayerDamagePlayerEvent event) {
		Resident victim = event.getVictimResident();
		Resident attacker = event.getAttackingResident();
		if (victim.getSpawnProtectionTaskID() != 0) {			
			event.setCancelled(true);
			event.setMessage(Translatable.of("msg_err_player_cannot_be_harmed", victim.getName()).forLocale(attacker));
		}
		if (attacker.getSpawnProtectionTaskID() != 0) {
			event.setCancelled(true);
			attacker.removeSpawnProtection();
		}
	}
}
