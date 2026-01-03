package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.BedExplodeEvent;
import com.palmergames.bukkit.towny.event.ChunkNotificationEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NationAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownBlockPermissionChangeEvent;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.nation.NationLevelDecreaseEvent;
import com.palmergames.bukkit.towny.event.nation.NationLevelIncreaseEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.teleport.SuccessfulTownyTeleportEvent;
import com.palmergames.bukkit.towny.event.town.TownLevelDecreaseEvent;
import com.palmergames.bukkit.towny.event.town.TownLevelIncreaseEvent;
import com.palmergames.bukkit.towny.event.town.TownOutlawAddEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.CellSurface;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.ChunkNotificationUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;
import com.palmergames.util.TimeMgmt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
		
		if (!TownyAPI.getInstance().isTownyWorld(to.getBukkitWorld()))
			return;
		
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null)
			return;

		// Run the following with a one tick delay, so that everything has a chance to take in the player's position.
		plugin.getScheduler().runLater(player, () -> {
			try {
				if (resident.hasMode("townclaim"))
					TownCommand.parseTownClaimCommand(player, new String[] {});
				if (resident.hasMode("townunclaim"))
					TownCommand.parseTownUnclaimCommand(player, new String[] {});
				if (resident.hasMode("plotgroup") && resident.hasPlotGroupName()) 
					Towny.getPlugin().getScheduler().runLater(player, () -> Bukkit.dispatchCommand(player, "plot group add " + resident.getPlotGroupName()), 1L);
				if (resident.hasMode("district") && resident.hasDistrictName())
					Towny.getPlugin().getScheduler().runLater(player, () -> Bukkit.dispatchCommand(player, "plot district add " + resident.getDistrictName()), 1L);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
			if (resident.hasMode("map"))
				TownyCommand.showMap(player);

			if (resident.hasMode("plotborder") || resident.hasMode("constantplotborder"))
				BorderUtil.getPlotBorder(to).runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.showToPlayer(player, to));

		}, 1L);

		// Run the following with a two tick delay, so that newly claimed land will appear correctly.
		// Check if player has entered a new town/wilderness
		if (event.isShowingPlotNotifications())
			plugin.getScheduler().runLater(() -> ChunkNotificationUtil.showChunkNotification(player, resident, to, from), 2L);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCreateTown(NewTownEvent event) {
		final Town town = event.getTown();
		Resident mayor = town.getMayor();
		if (mayor.isOnline() && town.hasHomeBlock() && TownySettings.isShowingClaimParticleEffect())
			plugin.getScheduler().runAsync(() ->
				CellSurface.getCellSurface(town.getHomeBlockOrNull().getWorldCoord()).runClaimingParticleOverSurfaceAtPlayer(mayor.getPlayer()));

		// Run the bank warning with a 10 second delay.
		plugin.getScheduler().runLater(() -> {
			double upkeep = TownySettings.getTownUpkeepCost(town);
			if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily() && upkeep > 0) {
				String cost = TownyEconomyHandler.getFormattedBalance(upkeep);
				String time = TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true));
				TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_new_town_advice", cost, time));
			}
		}, 200L);

		// Award new town with any potential bonus blocks.
		int bonus = TownySettings.getNewTownBonusBlocks();
		if (bonus > 0)
			town.setBonusBlocks(town.getBonusBlocks() + bonus);

		//TODO: at some point it might be nice to have a written_book given to mayors 
		// which could contain the above advice about depositing money, or containing
		// links to the commands page on the wiki.
	}
	
	/**
	 * Handles recently-created towns getting a refund when they are deleted.
	 * 
	 * @param event DeleteTownEvent to listen to.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownDeleted(DeleteTownEvent event) {
		if (!TownySettings.refundDeletedNewTowns() || event.getMayor() == null || !event.getCause().equals(DeleteTownEvent.Cause.COMMAND))
			return;

		int maxTownblocks = TownySettings.refundDeletedNewTownsMaxTownBlocks();
		int maxHours = TownySettings.refundDeletedNewTownsMaxHours();
		double newTownPrice = TownySettings.getNewTownPrice();

		if (event.getNumTownBlocks() > maxTownblocks || newTownPrice <= 0
			|| System.currentTimeMillis() - event.getTownCreated() > TimeMgmt.ONE_HOUR_IN_MILLIS * maxHours)
			return;

		Resident mayor = event.getMayor();
		mayor.getAccount().deposit(newTownPrice, "Town deletion refund.");
		TownyMessaging.sendMsg(mayor, Translatable.of("msg_you_have_been_refunded_your_town_cost", TownyEconomyHandler.getFormattedBalance(newTownPrice), maxHours, maxTownblocks));
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
		
		plugin.getScheduler().runLater(event.getLocation(), () -> {
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
	 * Used to display a message to the mayor and assistants of a town, alerting
	 * them to someone spawning at their town when money is earned by the town.
	 * 
	 * @param event SuccessfulTownyTeleportEvent thrown when someone spawns to town.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerSpawnToTown(SuccessfulTownyTeleportEvent event) {
		if (!TownySettings.isTownSpawnPaidToTown() || event.getTeleportCost() <= 0)
			return;

		Player player = event.getResident().getPlayer();
		if (player == null)
			return;
		Town toTown = TownyAPI.getInstance().getTown(event.getTeleportLocation());
		if (toTown == null)
			return;
		toTown.getResidents().stream()
			.filter(r -> r.isOnline() && (r.isMayor() || TownyPerms.hasAssistantTownRank(r)))
			.map(Resident::getPlayer)
			.forEach(p -> TownyMessaging.sendMsg(p,
				Translatable.of("msg_a_player_spawned_to_your_town_earning_you_x", player.getName(), TownyEconomyHandler.getFormattedBalance(event.getTeleportCost()))));
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
	 * Used to prevent unclaiming when a District would be cut in two parts.
	 * 
	 * @param event {@link TownPreUnclaimEvent} thrown when someone runs /t unclaim.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onTownUnclaimDistrict(TownPreUnclaimEvent event) {
		TownBlock townBlock = event.getTownBlock();
		if (!townBlock.hasDistrict())
			return;

		try {
			ProximityUtil.testAdjacentRemoveDistrictRulesOrThrow(townBlock.getWorldCoord(), event.getTown(), townBlock.getDistrict(), 1);
		} catch (TownyException e) {
			event.setCancelled(true);
			event.setCancelMessage(e.getMessage());
		}
	}

	/**
	 * Used to warn towns when they're approaching their claim limit, when the
	 * takeoverclaim feature is enabled, as well as claiming particles.
	 * 
	 * @param event TownClaimEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownClaim(TownClaimEvent event) {
		if (TownySettings.isShowingClaimParticleEffect() && event.getTownBlock().getWorldCoord().isFullyLoaded())
			Towny.getPlugin().getScheduler().runAsync(() ->
				CellSurface.getCellSurface(event.getTownBlock().getWorldCoord()).runClaimingParticleOverSurfaceAtPlayer(event.getResident().getPlayer()));

		if (!TownySettings.isOverClaimingAllowingStolenLand())
			return;
		if (event.getTown().availableTownBlocks() <= TownySettings.getTownBlockRatio())
			TownyMessaging.sendMsg(event.getResident(), Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_you_are_almost_out_of_townblocks")));
	}
	
	/**
	 * Used to warn towns when they've lost a resident, so they know they're at risk
	 * of having claims stolen in the takeoverclaim feature.
	 * 
	 * Used for town_level and nation_level decrease events.
	 * 
	 * @param event TownRemoveResidentEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownLosesResident(TownRemoveResidentEvent event) {
		Town town = event.getTown();
		if (town.getLevelNumber() < TownySettings.getTownLevelFromGivenInt(town.getNumResidents() + 1, town)) {
			BukkitTools.fireEvent(new TownLevelDecreaseEvent(town));
		}
		if (town.hasNation()) {
			Nation nation = town.getNationOrNull();
			if (nation.getLevelNumber() < TownySettings.getNationLevelFromGivenInt(nation.getNumResidents() + 1)) {
				BukkitTools.fireEvent(new NationLevelDecreaseEvent(nation));
			}	
		}
		
		if (!TownySettings.isOverClaimingAllowingStolenLand())
			return;
		if (town.isOverClaimed())
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.literal(Colors.Red).append(Translatable.of("msg_warning_your_town_is_overclaimed")));
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

		Town town = TownyAPI.getInstance().getTown(event.getPlayer());
		if (town == null)
			return;

		if (town.availableTownBlocks() < 1 || !town.equals(event.getFromCoord().getTownOrNull()))
			return;

		if (!event.getToCoord().canBeStolen())
			return;

		String message = event.getMessage() + Translatable.of("chunk_notification_takeover_available").forLocale(event.getPlayer());
		event.setMessage(message);

	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) 
	public void onResidentPreJoinTown(TownPreAddResidentEvent event) {
		Resident resident = event.getResident();

		long minTime = TownySettings.getResidentMinTimeToJoinTown();
		if (minTime <= 0L)
			return;

		long timePlayed = System.currentTimeMillis() - resident.getRegistered();
		if (timePlayed >= minTime)
			return;

		String timeRemaining = TimeMgmt.getFormattedTimeValue(minTime - timePlayed);
		event.setCancelled(true);
		event.setCancelMessage(Translatable.of("msg_err_you_cannot_join_town_you_have_not_played_long_enough", timeRemaining).forLocale(resident));
	}

	@EventHandler(ignoreCancelled = true)
	public void onResidentJoinTown(TownAddResidentEvent event) {
		Town town = event.getTown();

		if (town.getLevelNumber() > TownySettings.getTownLevelFromGivenInt(town.getNumResidents() - 1, town)) {
			BukkitTools.fireEvent(new TownLevelIncreaseEvent(town));
		}
		if (town.hasNation()) {
			Nation nation = town.getNationOrNull();
			if (nation.getLevelNumber() > TownySettings.getNationLevelFromGivenInt(nation.getNumResidents() - 1)) {
				BukkitTools.fireEvent(new NationLevelIncreaseEvent(nation));
			}	
		}

		if (!TownySettings.isPromptingNewResidentsToTownSpawn() || !TownySettings.isConfigAllowingTownSpawn())
			return;

		Player player = event.getResident().getPlayer();
		Town playerLocationTown = Optional.ofNullable(player).map(p -> TownyAPI.getInstance().getTown(p.getLocation())).orElse(null);

		if (player == null || (playerLocationTown != null && playerLocationTown.equals(town)))
			return;
		
		String notAffordMsg = Translatable.of("msg_err_cant_afford_tp").forLocale(player);

		double cost = town.getSpawnCost();
		if (cost > 0) {
			// The costed spawn will have its own Confirmation.
			try {
				SpawnUtil.sendToTownySpawn(player, new String[0], town, notAffordMsg, false, false, SpawnType.TOWN);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
		} else {
			// No cost, so lets offer the new resident a choice.
			Confirmation.runOnAccept(() -> {
				try {
					SpawnUtil.sendToTownySpawn(player, new String[0], town, notAffordMsg, false, false, SpawnType.TOWN);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				}
			})
			.setTitle(Translatable.of("msg_new_resident_spawn_to_town_prompt"))
			.sendTo(player);
			
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTownBlockPermissionChange(TownBlockPermissionChangeEvent event) {
		WorldCoord wc = event.getTownBlock().getWorldCoord();
		for (Player player : Bukkit.getOnlinePlayers())
			Towny.getPlugin().getScheduler().runAsync(() -> attemptPlayerCacheReset(player, wc));
	}

	private void attemptPlayerCacheReset(Player player, WorldCoord worldCoord) {
		if (!worldCoord.getWorldName().equalsIgnoreCase(player.getWorld().getName()))
			return;
		PlayerCache cache = Towny.getPlugin().getCache(player);
		if (cache == null || !cache.getLastTownBlock().equals(worldCoord) || PlayerCacheUtil.isOwnerCache(cache))
			return;
		Towny.getPlugin().resetCache(player);
	}

	/*
	 * Watch for town and nation level increasing/decreasing and reassign permissions in case the players have level-requirement permissions. 
	 */

	@EventHandler
	public void onTownLevelIncrease(TownLevelIncreaseEvent event) {
		if (!TownyPerms.ranksWithTownLevelRequirementPresent())
			return;
		event.getTown().getResidents()
		.stream()
		.filter(Resident::isOnline)
		.forEach(r -> TownyPerms.assignPermissions(r, r.getPlayer()));
	}
	
	@EventHandler
	public void onTownLevelDecrease(TownLevelDecreaseEvent event) {
		if (!TownyPerms.ranksWithTownLevelRequirementPresent())
			return;
		event.getTown().getResidents()
		.stream()
		.filter(Resident::isOnline)
		.forEach(r -> TownyPerms.assignPermissions(r, r.getPlayer()));
	}
	
	@EventHandler
	public void onNationLevelIncrease(NationLevelIncreaseEvent event) {
		if (!TownyPerms.ranksWithNationLevelRequirementPresent())
			return;
		event.getNation().getResidents()
		.stream()
		.filter(Resident::isOnline)
		.forEach(r -> TownyPerms.assignPermissions(r, r.getPlayer()));
	}
	
	@EventHandler
	public void onNationLevelDecrease(NationLevelDecreaseEvent event) {
		if (!TownyPerms.ranksWithNationLevelRequirementPresent())
			return;
		event.getNation().getResidents()
		.stream()
		.filter(Resident::isOnline)
		.forEach(r -> TownyPerms.assignPermissions(r, r.getPlayer()));
	}

	@EventHandler
	public void onNationAddEnemy(NationAddEnemyEvent event) {
		Nation targetNation = event.getEnemy();
		Nation targettingNation = event.getNation();
		for (Town nationTown : targettingNation.getTowns()) {
			boolean save = false;
			for (Town town : new ArrayList<>(nationTown.getTrustedTowns())) {
				save = false;
				if (town.hasNation() && town.getNationOrNull().equals(targetNation)) {
					nationTown.removeTrustedTown(town);
					save = true;
				}
				if (save)
					town.save();
			}
			for (Resident resident : new ArrayList<>(nationTown.getTrustedResidents())) {
				save = false;
				if (resident.hasNation() && resident.getNationOrNull().equals(targetNation)) {
					nationTown.removeTrustedResident(resident);
					save = true;
				}
				if (save)
					resident.save();
			}
			for (TownBlock tb : nationTown.getTownBlocks()) {
				if (!tb.hasTrustedResidents())
					continue;
				save = false;
				for (Resident resident : new ArrayList<>(tb.getTrustedResidents())) {
					if (resident.hasNation() && resident.getNationOrNull().equals(targetNation)) {
						tb.removeTrustedResident(resident);
						save = true;
					}
				}
				if (save)
					tb.save();
			}
		}
	}

	@EventHandler
	public void onTownAddOutlaw(TownOutlawAddEvent event) {
		Resident outlaw = event.getOutlawedResident();
		Town town = event.getTown();
		for (Town trustedTown : new ArrayList<>(town.getTrustedTowns())) {
			if (trustedTown.hasResident(outlaw)) {
				town.removeTrustedTown(trustedTown);
				town.save();
			}
		}

		if (town.hasTrustedResident(outlaw)) {
			town.removeTrustedResident(outlaw);
			town.save();
		}

		for (TownBlock tb : town.getTownBlocks()) {
			if (tb.hasTrustedResident(outlaw)) {
				tb.removeTrustedResident(outlaw);
				tb.save();
			}
		}
	}
}
