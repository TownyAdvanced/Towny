package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.ChunkNotification;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.event.BedExplodeEvent;
import com.palmergames.bukkit.towny.event.ChunkNotificationEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import com.palmergames.util.TimeMgmt;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

		try {
			if (resident.hasMode("townclaim"))
				TownCommand.parseTownClaimCommand(player, new String[] {});
			if (resident.hasMode("townunclaim"))
				TownCommand.parseTownUnclaimCommand(player, new String[] {});
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
		if (resident.hasMode("map"))
			TownyCommand.showMap(player);
		if (resident.hasMode("plotborder") || resident.hasMode("constantplotborder"))
			BorderUtil.getPlotBorder(to).runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, to));

		// Check if player has entered a new town/wilderness
		if (event.isShowingPlotNotifications()) {
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

			ChunkNotificationEvent cne = new ChunkNotificationEvent(player, msg, to, from);
			BukkitTools.fireEvent(cne);
			msg = cne.getMessage();
			if (cne.isCancelled() || msg == null || msg.isEmpty())
				return;

			sendChunkNoticiation(player, msg);
		}
	}
	
	private void sendChunkNoticiation(Player player, String msg) {
		switch (TownySettings.getNotificationsAppearAs().toLowerCase(Locale.ROOT)) {
			case "bossbar" -> sendBossBarChunkNotification(player, TownyComponents.miniMessage(msg));
			case "chat" -> TownyMessaging.sendMessage(player, msg);
			case "none" -> {}
			default -> sendActionBarChunkNotification(player, TownyComponents.miniMessage(msg));
		}
	}
	
	private void sendActionBarChunkNotification(Player player, Component msgComponent) {
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

	private void sendBossBarChunkNotification(Player player, Component message) {
		int ticks = TownySettings.getInt(ConfigNodes.NOTIFICATION_DURATION) * 20;
		if (playerBossBarMap.containsKey(player)) {
			removePlayerActionTasks(player);
			removePlayerBossBar(player);
		}
		
		final BossBar.Color color = BossBar.Color.NAMES.valueOr(TownySettings.getBossBarNotificationColor().toLowerCase(Locale.ROOT), BossBar.Color.WHITE);
		final BossBar.Overlay overlay = BossBar.Overlay.NAMES.valueOr(TownySettings.getBossBarNotificationOverlay().toLowerCase(Locale.ROOT), BossBar.Overlay.PROGRESS);
		
		final BossBar bossBar = BossBar.bossBar(message, TownySettings.getBossBarNotificationProgress(), color, overlay);

		TownyMessaging.sendBossBarMessageToPlayer(player, bossBar);

		int taskID = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			playerActionTasks.remove(player);
			removePlayerBossBar(player);
		}, ticks).getTaskId();

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
			String time = TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true));
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
		final TownyWorld world = Optional.ofNullable(event.getLocation().getWorld()).map(w -> TownyAPI.getInstance().getTownyWorld(w)).orElse(null);
		if (world == null)
			return;
		
		world.addBedExplosionAtBlock(event.getLocation(), event.getMaterial());
		if (event.getLocation2() != null)
			world.addBedExplosionAtBlock(event.getLocation2(), event.getMaterial());
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
			world.removeBedExplosionAtBlock(event.getLocation());
			world.removeBedExplosionAtBlock(event.getLocation2());
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
		if (victim != null && victim.hasRespawnProtection()) {
			event.setCancelled(true);
			event.setMessage(Translatable.of("msg_err_player_cannot_be_harmed", victim.getName()).forLocale(attacker));
		}
		if (attacker != null && attacker.hasRespawnProtection()) {
			event.setCancelled(true);
			attacker.removeRespawnProtection();
		}
	}

	/**
	 * Used to deny outlawed players spawning into Towns they are enemied in.
	 * @param event SpawnEvent which ResidentSpawnEvent, TownSpawnEvent, NationSpawnEvent extend.
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerSpawnsWithTown(SpawnEvent event) {

		if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer()))
			return;

		Town town = TownyAPI.getInstance().getTown(event.getTo());
		if (town == null || !town.hasOutlaw(event.getPlayer().getName()))
			return;
		event.setCancelled(true);
		event.setCancelMessage(Translatable.of("msg_error_cannot_town_spawn_youre_an_outlaw_in_town", town.getName()).forLocale(event.getPlayer()));
	}

	/**
	 * Used to prevent unclaiming when there is an outsider in the TownBlock,
	 * and the config does not allow for this.
	 * 
	 * @param event {@link TownPreUnclaimCmdEvent} thrown when someone runs /t unclaim.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onTownUnclaim(TownPreUnclaimCmdEvent event) {
		Player player = event.getResident().getPlayer();
		if (!TownySettings.getOutsidersUnclaimingTownBlocks() || player == null)
			return;

		List<WorldCoord> unclaimSelection = event.getUnclaimSelection();

		Town town = event.getTown();
		for (Player target : Bukkit.getOnlinePlayers()) {
			if (!town.hasResident(target) &&
				!TownyAPI.getInstance().isWilderness(target.getLocation()) &&
				unclaimSelection.contains(WorldCoord.parseWorldCoord(target))) {
				event.setCancelled(true);
				event.setCancelMessage(Translatable.of("msg_cant_unclaim_outsider_in_town").forLocale(event.getResident()));
				break;
			}
		}
	}

	/**
	 * Used to warn towns when they're approaching their claim limit, when the
	 * takeoverclaim feature is enabled
	 * 
	 * @param event TownClaimEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownClaim(TownClaimEvent event) {
		if (!TownySettings.isOverClaimingAllowingStolenLand())
			return;
		if (event.getTown().availableTownBlocks() <= TownySettings.getTownBlockRatio())
			TownyMessaging.sendMsg(event.getResident(), Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_you_are_almost_out_of_townblocks")));
	}

	/**
	 * Used to warn towns when they've lost a resident, so they know they're at risk
	 * of having claims stolen in the takeoverclaim feature.
	 * 
	 * @param event TownRemoveResidentEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownLosesResident(TownRemoveResidentEvent event) {
		if (!TownySettings.isOverClaimingAllowingStolenLand())
			return;
		if (event.getTown().getTownBlocks().size() > event.getTown().getMaxTownBlocks())
			TownyMessaging.sendPrefixedTownMessage(event.getTown(), Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_your_town_is_overclaimed")));
	}

	/**
	 * Used to inform players they can /t takeoverclaim a plot when they enter towns that are overclaimed.
	 * 
	 * @param event ChunkNotificationEvent thrown by Towny to construct the chunk notifications.
	 */
	@EventHandler
	public void onChunkNotification(ChunkNotificationEvent event) {
		if (!TownySettings.isOverClaimingAllowingStolenLand() || event.getToCoord().isWilderness() || event.getFromCoord().isWilderness())
			return;

		Resident resident = TownyAPI.getInstance().getResident(event.getPlayer());
		if (resident == null || !resident.hasTown())
			return;

		Town town = resident.getTownOrNull();
		if  (town.availableTownBlocks() < 1 || !event.getFromCoord().getTownOrNull().equals(town))
			return;

		if (!event.getToCoord().canBeStolen())
			return;

		String message = event.getMessage() + Translatable.of("chunk_notification_takeover_available").forLocale(event.getPlayer());
		event.setMessage(message);

	}

	@EventHandler (ignoreCancelled = true)
	public void onResidentJoinTown(TownAddResidentEvent event) {
		if (!TownySettings.isPromptingNewResidentsToTownSpawn() || !TownySettings.getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN))
			return;

		Town town = event.getTown();
		Player player = event.getResident().getPlayer();

		if (player == null || TownyAPI.getInstance().getTown(player).equals(town))
			return;
		String notAffordMsg = Translatable.of("msg_err_cant_afford_tp").forLocale(player);

		try {
			SpawnUtil.sendToTownySpawn(player, new String[0], town, notAffordMsg, false, false, SpawnType.TOWN);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}
}
