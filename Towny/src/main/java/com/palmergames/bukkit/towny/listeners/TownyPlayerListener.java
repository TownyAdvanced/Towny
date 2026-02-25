package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.BedExplodeEvent;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.TitleNotificationEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.event.player.PlayerDeniedBedUseEvent;
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoDistrictEvent;
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent;
import com.palmergames.bukkit.towny.event.player.PlayerExitsFromDistrictEvent;
import com.palmergames.bukkit.towny.event.player.PlayerExitsFromTownBorderEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKeepsExperienceEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKeepsInventoryEvent;
import com.palmergames.bukkit.towny.event.teleport.CancelledTownyTeleportEvent.CancelledTeleportReason;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.CommandList;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.notification.TitleNotification;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.ChunkNotificationUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;

/**
 * Handle events for all Player related events
 * Players deaths are handled both here and in the TownyEntityMonitorListener
 * 
 * @author Shade/ElgarL
 * 
 */
public class TownyPlayerListener implements Listener {

	private final Towny plugin;
	private CommandList blockedJailCommands;
	private CommandList blockedTownCommands;
	private CommandList blockedTouristCommands;
	private CommandList blockedOutlawCommands;
	private CommandList blockedWarCommands;
	private CommandList ownPlotLimitedCommands;
	
	private int teleportWarmupTime = TownySettings.getTeleportWarmupTime();
	private boolean isMovementCancellingWarmup = TownySettings.isMovementCancellingSpawnWarmup();
	private boolean isPreventingSaturationLoss = TownySettings.preventSaturationLoss();
	
	public TownyPlayerListener(Towny plugin) {
		this.plugin = plugin;
		loadBlockedCommandLists();
		TownySettings.addReloadListener(NamespacedKey.fromString("blocked-commands", plugin), config -> loadBlockedCommandLists());
		
		TownySettings.addReloadListener(NamespacedKey.fromString("teleport-warmups", plugin), () -> {
			this.teleportWarmupTime = TownySettings.getTeleportWarmupTime();
			this.isMovementCancellingWarmup = TownySettings.isMovementCancellingSpawnWarmup();
		});
		TownySettings.addReloadListener(NamespacedKey.fromString("saturation", plugin), () -> {
			this.isPreventingSaturationLoss = TownySettings.preventSaturationLoss();
		});
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();

		if (!player.isOnline()) {
			return;
		}

		// Test and kick any players with invalid names.
		if (player.getName().contains(" ")) {
			player.kickPlayer("Invalid name!");
			return;
		}

		// Safe Mode Join Messages
		if (plugin.isError()) {
			sendSafeModeMessage(player);
			return;
		}
		
		// Perform login code to update Towny data.
		plugin.getScheduler().run(new OnPlayerLogin(Towny.getPlugin(), player));
	}

	private void sendSafeModeMessage(Player player) {
		try {
			Translatable tipMsg = player.isOp() || player.hasPermission("towny.admin")
				? Translatable.of("msg_safe_mode_admin")
				: Translatable.of("msg_safe_mode_player");
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_safe_mode_base"), tipMsg);
		} catch (Exception e) {
			// Safemode is affecting Towny's ability to use Translatables.
			String msg = player.isOp() || player.hasPermission("towny.admin") 
				? "Check the server's console for more information."
				: "Tell an admin to check the server's console.";
			player.sendMessage(Component.text("[Towny] [Error] Towny is locked in Safe Mode due to an error! " + msg, NamedTextColor.RED));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.deleteCache(event.getPlayer());
		TownyPerms.removeAttachment(event.getPlayer().getName());

		if (plugin.isError())
			return;
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (resident != null) {
			// Don't set last online if the player was vanished.
			if (!BukkitTools.hasVanishedMeta(event.getPlayer()))
				resident.setLastOnline(System.currentTimeMillis());

			resident.setGUIPageNum(0);
			resident.setGUIPages(null);

			resident.clearModes(false);
			resident.save();

			if (TownyTimerHandler.isTeleportWarmupRunning()) {
				TownyAPI.getInstance().abortTeleportRequest(resident);
			}
			
			if (JailUtil.isQueuedToBeJailed(resident))
				event.getPlayer().setHealth(0);
		}

		ChunkNotificationUtil.cancelPlayerTasks(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (plugin.isError() || event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.END_PORTAL || !TownySettings.isTownRespawning()) {
			return;
		}

		// If respawn anchors have higher precedence than town spawns, use them instead.
		if (event.isAnchorSpawn() && TownySettings.isRespawnAnchorHigherPrecedence()) {
			return;
		}

		Player player = event.getPlayer();
		Location respawn = TownyAPI.getInstance().getTownSpawnLocation(player);
		Resident resident = TownyAPI.getInstance().getResident(player);

		// Towny or the Resident might be prioritizing bed spawns over town spawns.
		if (TownySettings.getBedUse() ||
			(resident != null && resident.hasMode("bedspawn"))) {
			Location bed = BukkitTools.getBedOrRespawnLocation(player);
			if (bed != null)
				respawn = bed;
		}

		// Town spawn could be null and no bed was available.
		if (respawn == null)
			return;
		
		// Check if only respawning in the same world as the town's spawn.
		if (TownySettings.isTownRespawningInOtherWorlds() && !player.getWorld().equals(respawn.getWorld()))
			return;
		
		event.setRespawnLocation(respawn);
		
		// Handle Spawn protection
		long protectionTime = TownySettings.getSpawnProtectionDuration();
		if (protectionTime > 0L && resident != null)
			resident.addRespawnProtection(protectionTime);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJailRespawn(PlayerRespawnEvent event) {

		if (plugin.isError()) {
			return;
		}
	
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());

		// If player is jailed send them to their jailspawn.
		if (resident != null && resident.isJailed() && resident.getJailSpawn().isWorldLoaded())
			event.setRespawnLocation(resident.getJailSpawn());

	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		// Test whether we can build in the place they are pouring their liquid.
		event.setCancelled(!TownyActionEventExecutor.canBuild(event.getPlayer(), event.getBlock().getLocation(), event.getBucket()));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		// Bail if we're milking a cow, goat, or if we're filling air.
		if (event.getItemStack().getType().equals(Material.MILK_BUCKET)
			|| event.getBlockClicked().getType().equals(Material.AIR))
			return;
		
		// Test whether we can fill the bucket by testing if they would be able to destroy the liquid it is picking up.
		event.setCancelled(!TownyActionEventExecutor.canDestroy(event.getPlayer(), event.getBlockClicked().getLocation(), event.getBlockClicked().getType()));

	}

	/*
	* Handles Blocks for Itemuse & Switch test.
	*/
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		Action action = event.getAction();
		if(actionIsNotRightClickOrPhysical(action) && actionIsNotLeftClickThatCountsAsSwitch(event, action)) {
			return;
		}
		
		Player player = event.getPlayer();
		Block clickedBlock = event.getClickedBlock();

		/*
		 * Item Use or and Item's that call destroy tests.
		 */
		if (event.hasItem()) {
			
			Material item =  event.getItem().getType();
			
			Location loc = null;
			if (clickedBlock != null)
				loc = clickedBlock.getLocation();
			else 
				loc = player.getLocation();
			/*
			 * Test item_use. 
			 */
			if (TownySettings.isItemUseMaterial(item, loc) && !TownyActionEventExecutor.canItemuse(player, loc, item)) {
				event.setCancelled(true);
				return;
			}

			/*
			 * Test other Items using non-ItemUse test.
			 * 
			 * This means less configuration for the end user,
			 * for what should be considered build or destroy 
			 * tests, based on their world-altering properties
			 * 
			 */
			if (clickedBlock != null) {
				Material clickedMat = clickedBlock.getType();

				/* Fixes a known duping exploit. */
				if (Tag.BEDS.isTagged(item) && Tag.CROPS.isTagged(clickedBlock.getType()) && clickedBlock.getLightLevel() == 0) {
					event.setCancelled(true);
					return;
				}

				/*
				 * Test stripping logs, scraping copper blocks, dye-able signs,
				 * flint&steel on TNT and shears on pumpkins,
				 * catches hoes taking dirt from Rooted Dirt blocks,
				 * prevents players from using brushes on brush-able blocks (suspicious sand, suspicious gravel)
				 * 
				 * Treat interaction as a Destroy test.
				 */
				if ((ItemLists.AXES.contains(item) && (ItemLists.UNSTRIPPED_WOOD.contains(clickedMat) || ItemLists.WAXED_BLOCKS.contains(clickedMat) || ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat))) ||
					(ItemLists.DYES.contains(item) && ItemLists.SIGNS.contains(clickedMat)) ||
					(item == Material.FLINT_AND_STEEL && clickedMat == Material.TNT) ||
					(item == Material.SHEARS && clickedMat == Material.PUMPKIN) ||
					clickedMat == Material.ROOTED_DIRT && ItemLists.HOES.contains(item) ||
					ItemLists.BRUSHABLE_BLOCKS.contains(clickedMat) && item == Material.BRUSH) { 

					if (!TownyActionEventExecutor.canDestroy(player, loc, clickedMat)) {
						event.setCancelled(true);
						return;
					}
				}

				/*
				 * Test putting candles on cakes.
				 * Test wax usage.
				 * Test putting plants in pots.
				 * Test if we're putting a book into a BookContainer.
				 * Test if something is being put onto a Campfire.
				 * Test bonemeal usage.
				 * 
				 * Treat interaction as a Build test.
				 */
				if ((ItemLists.CANDLES.contains(item) && clickedMat == Material.CAKE) ||  
					ItemLists.PLANTS.contains(item) && clickedMat == Material.FLOWER_POT ||
					item == Material.HONEYCOMB && ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat) ||
					ItemLists.PLACEABLE_BOOKS.contains(item) && ItemLists.BOOK_CONTAINERS.contains(clickedMat) ||
					ItemLists.CAMPFIRES.contains(clickedMat) && item != Material.FLINT_AND_STEEL ||
					item == Material.BONE_MEAL && !TownyActionEventExecutor.canBuild(player, loc, item)) {

					if (!TownyActionEventExecutor.canBuild(player, loc, item)) {
						event.setCancelled(true);
						return;
					}
				}

				/*
				 * Test if we're about to spawn either entity. Uses build test.
				 */
				if (item == Material.ARMOR_STAND || item == Material.END_CRYSTAL) {
					if (!TownyActionEventExecutor.canBuild(player, clickedBlock.getRelative(event.getBlockFace()).getLocation(), item)) {
						event.setCancelled(true);
						return;
					}
				}

				/*
				 * Prevents players using wax on signs, harvesting honey/honeycomb
				 */
				if (
					(item == Material.HONEYCOMB && ItemLists.SIGNS.contains(clickedMat) && !isSignWaxed(clickedBlock)) ||
					((item == Material.GLASS_BOTTLE || item == Material.SHEARS) && (clickedMat == Material.BEE_NEST || clickedMat == Material.BEEHIVE))
					) {
					if (!TownyActionEventExecutor.canItemuse(player, clickedBlock.getLocation(), clickedMat)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		
		/*
		 * No Item used.
		 */
		if (clickedBlock != null) {
			Material clickedMat = clickedBlock.getType(); 
			/*
			 * Test switch use.
			 */
			if (TownySettings.isSwitchMaterial(clickedMat, clickedBlock.getLocation()) && !TownyActionEventExecutor.canSwitch(player, clickedBlock.getLocation(), clickedMat)) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(true);
				return;
			}
			/*
			 * Test potted plants, redstone interactables, candles and other blocks which 
			 * cause an interaction that could be considered destructive, or 
			 * something which wouldn't be given out like a normal 
			 * door/inventory permission. 
			 * 
			 * Test interaction as a Destroy test. (These used to be switches pre-0.96.3.1)
			 */
			if (ItemLists.POTTED_PLANTS.contains(clickedMat) ||
				ItemLists.HARVESTABLE_BERRIES.contains(clickedMat) ||
				ItemLists.REDSTONE_INTERACTABLES.contains(clickedMat) ||
				ItemLists.CANDLES.contains(clickedMat) ||
				clickedMat == Material.TURTLE_EGG ||
				clickedMat.getKey().equals(NamespacedKey.minecraft("chiseled_bookshelf")) ||
				clickedMat == Material.BEACON || clickedMat == Material.DRAGON_EGG || 
				clickedMat == Material.COMMAND_BLOCK){
				
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				if (!TownyActionEventExecutor.canDestroy(player, clickedBlock.getLocation(), clickedMat)) {
					event.setCancelled(true);
					return;
				}
			}
			
			/*
			 * Prevents players from editing signs where they shouldn't.
			 * This check is only used when our listener for paper's sign open event is not in use, since that event fires when the sign is actually opened instead of interact.
			 */
			if (TownyPaperEvents.SIGN_OPEN_GET_CAUSE == null && ItemLists.SIGNS.contains(clickedMat) && !isSignWaxed(clickedBlock) &&
					!TownyActionEventExecutor.canDestroy(player, clickedBlock.getLocation(), clickedMat))
				event.setCancelled(true);
		}
	}

	/**
	 * Is the action one that involves left-clicking on a Switch block? This is
	 * useful for protecting (usually) modded blocks that can be used via left
	 * clicks.
	 * 
	 * @param event  PlayerInteractEvent causing a switch test.
	 * @param action Action that has to be LEFT_CLICK_BLOCK for this to count.
	 * @return true if the player is left clicking a block that is technically a
	 *         switch_id in Towny.
	 */
	private boolean actionIsNotLeftClickThatCountsAsSwitch(PlayerInteractEvent event, Action action) {
		return action != Action.LEFT_CLICK_BLOCK || !event.hasBlock() || !TownySettings.isSwitchMaterial(event.getClickedBlock().getType(), event.getClickedBlock().getLocation());
	}

	/**
	 * Is the action something we don't want to worry about when we're dealing with something like honey comb and a sign, or candles and cake when testing PlayerInteractEvents.
	 * @param action Action that player is making for this to matter.
	 * @return true if the action is a right click or physical Action.
	 */
	private boolean actionIsNotRightClickOrPhysical(Action action) {
		return action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR && action != Action.PHYSICAL;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDragonEggLeftClick(PlayerInteractEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		Player player = event.getPlayer();
		if (!TownyAPI.getInstance().isTownyWorld(player.getWorld()))
			return;
		
		if (!event.hasBlock() || event.getAction() != Action.LEFT_CLICK_BLOCK)
			return;
		
		Block block = event.getClickedBlock();
		if (block.getType() != Material.DRAGON_EGG)
			return;
		
		if (TownyActionEventExecutor.canDestroy(player, block))
			return;
		
		event.setCancelled(true);
		
	}

	/**
	 * Handles clicking on beds in the nether/respawn anchors in the overworld sending blocks to a map so we can track when explosions occur from beds.
	 * Spigot API's BlockExplodeEvent#getBlock() always returns AIR for beds/anchors exploding, which is why this is necessary.
	 * @param event PlayerInteractEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerBlowsUpBedOrRespawnAnchor(PlayerInteractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (event.hasBlock() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			Location blockLoc = block.getLocation();
			/*
			 * Catches respawn anchors blowing up and allows us to track their explosions.
			 */
			if (block.getType() == Material.RESPAWN_ANCHOR && !block.getWorld().isRespawnAnchorWorks()) {
				RespawnAnchor anchor = ((RespawnAnchor) block.getBlockData());
				if (anchor.getCharges() > 0)
					BukkitTools.fireEvent(new BedExplodeEvent(player, blockLoc, null, block.getType()));
				return;
			}
			
			/*
			 * Catches beds blowing up and allows us to track their explosions.
			 */
			if (Tag.BEDS.isTagged(block.getType()) && player.getWorld().getEnvironment().equals(Environment.NETHER)) {
				org.bukkit.block.data.type.Bed bed = ((org.bukkit.block.data.type.Bed) block.getBlockData());
				BukkitTools.fireEvent(new BedExplodeEvent(player, blockLoc, block.getRelative(bed.getFacing()).getLocation(), block.getType()));
				return;
			}
			
			/*
			 * Prevents setting the spawn point of the player using beds or respawn anchors, 
			 * except in allowed plots (personally-owned and Inns)
			 */
			if (TownySettings.getBedUse() 
				&& !TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_BYPASS_BED_RESTRICTION.getNode())
				&& (Tag.BEDS.isTagged(block.getType()) || disallowedAnchorClick(event, block))) {

				boolean isOwner = false;
				boolean isInnPlot = false;
				boolean isEnemy = false;
				Translatable denialMessage = Translatable.of("msg_err_cant_use_bed");

				if (!TownyAPI.getInstance().isWilderness(blockLoc)) {
					
					TownBlock townblock = TownyAPI.getInstance().getTownBlock(blockLoc);
					Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
					Town town = townblock.getTownOrNull();
					if (resident == null || town == null)
						return;
					
					isOwner = townblock.isOwner(resident);
					isInnPlot = townblock.getType() == TownBlockType.INN;
					
					//Prevent enemies and outlaws using the Inn plots.
					if (CombatUtil.isEnemyTownBlock(player, townblock.getWorldCoord()) || town.hasOutlaw(resident)) {
						isEnemy = true;
						denialMessage = Translatable.of("msg_err_no_sleep_in_enemy_inn");
					}
				}

				if (isEnemy || !isOwner && !isInnPlot) {
					// The player is not allowed to use the bed.
					
					// Fire a cancellable event prior to us denying bed use.
					PlayerDeniedBedUseEvent pdbue = new PlayerDeniedBedUseEvent(player, blockLoc, isEnemy, denialMessage);
					if (!BukkitTools.isEventCancelled(pdbue)) {
						event.setCancelled(true);
						TownyMessaging.sendErrorMsg(player, pdbue.getDenialMessage());
					}
				}
			}
		}
	}

	/*
	 * This method will stop a player Right Clicking on a respawn anchor if:
	 * - The world is an anchor-allowed world (the nether) and,
	 * - The Block is an anchor and,
	 * - The Anchor has charges and,
	 * - The Item in their hand is nothing or (not-glowstone or the charges are full.) 
	 */
	private boolean disallowedAnchorClick(PlayerInteractEvent event, Block block) {
		return block.getWorld().isRespawnAnchorWorks()
			&& block.getBlockData() instanceof RespawnAnchor anchor 
			&& anchor.getCharges() > 0 
			&& (event.getItem() == null || (event.getItem().getType() != Material.GLOWSTONE || anchor.getCharges() >= anchor.getMaximumCharges()));
	}

	/*
	 * Handles projectiles which are considered for Itemuse, in order to catch them
	 * when they are used on AIR which do not register PlayerInteractEvents.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerSpawnItemuseProjectile(ProjectileLaunchEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		Projectile projectile = event.getEntity();
		ProjectileSource source = projectile.getShooter();
		if (!(source instanceof Player player))
			return;

		Material item = EntityTypeUtil.parseEntityToMaterial(event.getEntityType());
		Location loc = player.getLocation();
		if (item == null || !TownySettings.isItemUseMaterial(item, loc))
			return;

		/*
		 * Test item_use. 
		 */
		if (!TownyActionEventExecutor.canItemuse(player, loc, item))
			event.setCancelled(true);
	}

	/*
	* Handles protection of Armor Stands.
	*/	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteractWithArmourStand(PlayerArmorStandManipulateEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
		event.setCancelled(!TownyActionEventExecutor.canDestroy(event.getPlayer(), event.getRightClicked().getLocation(), Material.ARMOR_STAND));
	}

	/*
	* Handles right clicking of entities: Item Frames, Paintings, Minecarts.
	* Entities right clicked with an item, tests the item for ItemUse.
	* Sheeps and wolves from being dyed.
	* 
	* Treats entities as their Materials in order to run permission tests.
	*/
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		if (event.getRightClicked() == null)
			return;

		Player player = event.getPlayer();
		Material mat = null;
		ActionType actionType = ActionType.DESTROY;
		EntityType entityType = event.getRightClicked().getType();
		
		plugin.getLogger().info("PlayerInteractEntityEvent - Entity Type: " + entityType.name());

		Material item = player.getInventory().getItemInMainHand().getType();

		/*
		 * The following will get us a Material substituted in for an Entity so that we can run permission tests.
		 */
		if (EntityLists.SWITCH_PROTECTED.contains(entityType)) {
			mat = EntityTypeUtil.parseEntityToMaterial(entityType);
			actionType = ActionType.SWITCH;
		} else if (EntityLists.DYEABLE.contains(entityType) && ItemLists.DYES.contains(item))
			mat = item;
		else if (item == Material.BUCKET && EntityLists.MILKABLE.contains(entityType)) {
			mat = EntityTypeUtil.parseEntityToMaterial(entityType);
			actionType = ActionType.ITEM_USE;
		} else if (item == Material.COOKIE && EntityType.PARROT.equals(entityType)) {
			mat = EntityTypeUtil.parseEntityToMaterial(entityType);
		} else if ((ItemLists.AXES.contains(item) || item == Material.HONEYCOMB) && entityType.getKey().getKey().equals("copper_golem")) {
			mat = EntityTypeUtil.parseEntityToMaterial(entityType);
			actionType = ActionType.ITEM_USE;
		} else if (EntityLists.RIGHT_CLICK_PROTECTED.contains(entityType)) {
			mat = EntityTypeUtil.parseEntityToMaterial(entityType);
		}

		/*
		 * A material has been substitued correctly in place of one of the above EntityTypes.
		 * 
		 * We will decide how to react based on either of the following tests.
		 */
		if (mat != null) {
			// Material has been supplied in place of an entity, run Destroy Tests.
			if (actionType == ActionType.DESTROY) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getRightClicked().getLocation(), mat));
				return;
			}
			// Material has been supplied in place of an entity, run Switch Tests.
			if (TownySettings.isSwitchMaterial(mat, event.getRightClicked().getLocation()) && actionType == ActionType.SWITCH) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canSwitch(player, event.getRightClicked().getLocation(), mat));
				return;
			} 
		}
		
		/*
		 * Handle things which need an item in hand.
		 */
		if (item == null)
			return;

		/*
		 * Sheep and mountable entities can be sheared, protect them if they aren't in the wilderness.
		 */
		if (item == Material.SHEARS && !TownyAPI.getInstance().isWilderness(event.getRightClicked().getLocation()) && (event.getRightClicked().getType().equals(EntityType.SHEEP) || EntityLists.MOUNTABLE.contains(entityType))) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getRightClicked().getLocation(), item));
			return;
		}

		/*
		 * Nametags can be used on things.
		 */
		if (item == Material.NAME_TAG) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getRightClicked().getLocation(), item));
			return;
		}

		/*
		 * Item_use protection.
		 */
		if (TownySettings.isItemUseMaterial(item, event.getRightClicked().getLocation())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			event.setCancelled(!TownyActionEventExecutor.canItemuse(player, event.getRightClicked().getLocation(), item));
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		// Let's ignore Citizens NPCs
		if (PluginIntegrations.getInstance().isNPC(event.getPlayer()))
			return;
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from = event.getFrom();

		/*
		 * Abort if we haven't really moved, or if the event.getTo() is null (which is allowed...)
		 */
		if (to == null
			|| from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY()) {
			return;
		}

		if (this.teleportWarmupTime > 0 && this.isMovementCancellingWarmup) {
			final Resident resident = TownyAPI.getInstance().getResident(player);
			
			if (resident != null && resident.hasRequestedTeleport() && !resident.isAdmin() && TeleportWarmupTimerTask.abortTeleportRequest(resident, CancelledTeleportReason.MOVEMENT))
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_teleport_cancelled"));
		}

		if (WorldCoord.cellChanged(from, to)) {

			TownyWorld fromWorld = TownyAPI.getInstance().getTownyWorld(from.getWorld());				
			TownyWorld toWorld = TownyAPI.getInstance().getTownyWorld(to.getWorld());
			if (fromWorld == null || toWorld == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("not_registered"));
				return;
			}
			WorldCoord fromCoord = WorldCoord.parseWorldCoord(from);
			WorldCoord toCoord = WorldCoord.parseWorldCoord(to);
			
			onPlayerMoveChunk(player, fromCoord, toCoord, event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		// Let's ignore Citizens NPCs. This must come before the safemode check, as Citizens stores their NPCs
		// at the world spawn until a player loads a chunk, to which the NPC is then teleported. Towny would
		// prevent them teleporting, leaving them at spawn even after Safe Mode is cleaned up.
		if (PluginIntegrations.getInstance().isNPC(event.getPlayer()))
			return;

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null)
			return;

		boolean isAdmin = !Towny.getPlugin().hasPlayerMode(player, "adminbypass") && (resident.isAdmin() || resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_OUTLAW_TELEPORT_BYPASS.getNode()));
		if (isAdmin) {
			// Admins don't get restricted further but they do need to fire the PlayerChangePlotEvent.
			onPlayerMove(event);
			return;
		}

		// Cancel teleport if Jailed by Towny.
		if (resident.isJailed()) {
			if (event.getCause() == TeleportCause.COMMAND) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
				return;
			}
			if (!TownySettings.JailAllowsTeleportItems() && (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT)) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
				return;
			}
		}

		// Cancel teleport if resident is outlawed in Town.
		if (!TownySettings.canOutlawsTeleportOutOfTowns()) {
			TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getFrom());
			if (tb != null && tb.hasTown()) {
				Town town = tb.getTownOrNull();
				if (town != null && town.hasOutlaw(resident)) {
					if (event.getCause() == TeleportCause.COMMAND) {
						TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_outlawed_players_no_teleport"));
						event.setCancelled(true);
						return;
					}
					if (!TownySettings.canOutlawsUseTeleportItems() && (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT)) {
						TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_outlawed_players_no_teleport"));
						event.setCancelled(true);
						return;
					}
				}
			}
		}

		// Test to see if CHORUS_FRUIT is in the item_use list.
		if (event.getCause() == TeleportCause.CHORUS_FRUIT && TownySettings.isItemUseMaterial(Material.CHORUS_FRUIT, event.getTo())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.CHORUS_FRUIT)) {
				event.setCancelled(true);
				return;
			}
		}

		// Test to see if Ender pearls is in the item_use list.
		if (event.getCause() == TeleportCause.ENDER_PEARL && TownySettings.isItemUseMaterial(Material.ENDER_PEARL, event.getTo())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.ENDER_PEARL)) {
				event.setCancelled(true);
				return;
			}
		}

		// Remove spawn protection if the player is teleporting since spawning.
		if (resident.hasRespawnProtection())
			resident.removeRespawnProtection();

		// Send the event to the onPlayerMove so Towny can fire the PlayerChangePlotEvent.
		onPlayerMove(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) { // has changed worlds
		if (event.getPlayer().isOnline())
			TownyPerms.assignPermissions(null, event.getPlayer());
	}

	/*
	 * PlayerFishEvent
	 * 
	 * Prevents players from fishing for entities in protected regions.
	 * - Armorstands, animals, players, any entity affected by rods.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled= true)
	public void onPlayerFishEvent(PlayerFishEvent event) {
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		if (event.getState().equals(PlayerFishEvent.State.CAUGHT_ENTITY)) {
			Player player = event.getPlayer();
			Entity caught = event.getCaught();
			boolean test = false;

			// Required because some times a plugin will throw the PlayerFishEvent with a null caught.
			if (caught == null)
				return;
			
			// Caught players are tested for pvp at the location of the catch.
			if (caught.getType().equals(EntityType.PLAYER)) {
				TownyWorld world = TownyAPI.getInstance().getTownyWorld(event.getCaught().getWorld());
				if (world == null)
					return;

				TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getCaught().getLocation());
				test = !CombatUtil.preventPvP(world, tb);
			// Non-player catches are tested for destroy permissions.
			} else {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				test = TownyActionEventExecutor.canDestroy(player, caught.getLocation(), Material.DIRT);
			}
			if (!test) {
				event.setCancelled(true);
				event.getHook().remove();
			}
		}	
	}
	
	@EventHandler
	public void onEntityExhaustion(EntityExhaustionEvent event) {
		// Stop player exhaustion if criteria is met to prevent saturation loss
		if (!this.isPreventingSaturationLoss)
			return;
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!TownyAPI.getInstance().isTownyWorld(player.getWorld()))
			return;
		TownBlock tbAtPlayer = TownyAPI.getInstance().getTownBlock(player);
		if (tbAtPlayer == null)
			return;
		Town townAtPlayer = tbAtPlayer.getTownOrNull();
		Town playersTown = TownyAPI.getInstance().getTown(player);
		if (playersTown == null)
			return;
		if (townAtPlayer != null && !townAtPlayer.hasActiveWar() && CombatUtil.isAlly(townAtPlayer, playersTown) && !tbAtPlayer.getType().equals(TownBlockType.ARENA))
			event.setCancelled(true);
	} 

	/*
	* PlayerMoveEvent that can fire the PlayerChangePlotEvent
	*/
	private void onPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to, PlayerMoveEvent moveEvent) {

		final PlayerCache cache = plugin.getCacheOrNull(player.getUniqueId());
		if (cache != null)
			cache.resetAndUpdate(to);

		PlayerChangePlotEvent event = new PlayerChangePlotEvent(player, from, to, moveEvent);
		BukkitTools.fireEvent(event);
	}
	
	/*
	* PlayerChangePlotEvent that can fire the PlayerLeaveTownEvent and PlayerEnterTownEvent
	*/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangePlotEvent(PlayerChangePlotEvent event) {
		if (!TownyUniverse.getInstance().hasResident(event.getPlayer().getUniqueId()))
			return;
		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();
		if (to.isWilderness() && from.isWilderness()) 
			// Both are wilderness, no event will fire.
			return;
		if (to.isWilderness()) {
			// Gone from a Town into the wilderness.
			BukkitTools.fireEvent(new PlayerExitsFromTownBorderEvent(event.getPlayer(), to, from, from.getTownOrNull(), event.getMoveEvent()));
		} else if (from.isWilderness()) {
			// Gone from wilderness into Town.
			BukkitTools.fireEvent(new PlayerEntersIntoTownBorderEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
		// Both to and from have towns.
		} else if (to.getTownOrNull().equals(from.getTownOrNull())) {
			// The towns are the same, no event will fire.
			return;
		} else {
			// Player has left one Town and immediately entered a different one.
			BukkitTools.fireEvent(new PlayerEntersIntoTownBorderEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
			BukkitTools.fireEvent(new PlayerExitsFromTownBorderEvent(event.getPlayer(), to, from, from.getTownOrNull(), event.getMoveEvent()));
		}
	}
	
	/*
	* PlayerChangePlotEvent that can fire the PlayerExitsFromDistrictEvent and PlayerEntersIntoDistrictEvent
	*/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangeDistricts(PlayerChangePlotEvent event) {
		if (!TownyUniverse.getInstance().hasResident(event.getPlayer().getUniqueId()))
			return;

		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();
		boolean fromHasDistrict = !from.isWilderness() && from.getTownBlockOrNull().hasDistrict();
		boolean toHasDistrict = !to.isWilderness() && to.getTownBlockOrNull().hasDistrict();
		if (to.isWilderness() && from.isWilderness() || (!fromHasDistrict && !toHasDistrict)) 
			// Both are wilderness, or neither plot involves a District. No event will fire.
			return;

		District fromDistrict = fromHasDistrict ? from.getTownBlockOrNull().getDistrict() : null;
		District toDistrict = toHasDistrict ? to.getTownBlockOrNull().getDistrict() : null;

		if (to.isWilderness() && fromHasDistrict) {
			// Gone from a Town into the wilderness.
			BukkitTools.fireEvent(new PlayerExitsFromDistrictEvent(event.getPlayer(), to, from, fromDistrict, event.getMoveEvent()));

		} else if (from.isWilderness() && toHasDistrict) {
			// Gone from wilderness into Town.
			BukkitTools.fireEvent(new PlayerEntersIntoDistrictEvent(event.getPlayer(), to, from, toDistrict, event.getMoveEvent()));

		} else if (!to.isWilderness() && !from.isWilderness() && to.getTownOrNull().equals(from.getTownOrNull())
			&& fromHasDistrict && toHasDistrict && !fromDistrict.equals(toDistrict)) {
				// Moving in same town, between two different Districts.
				BukkitTools.fireEvent(new PlayerExitsFromDistrictEvent(event.getPlayer(), to, from, fromDistrict, event.getMoveEvent()));
				BukkitTools.fireEvent(new PlayerEntersIntoDistrictEvent(event.getPlayer(), to, from, toDistrict, event.getMoveEvent()));

		} else {
			// Player has left one Town and immediately entered a different one, check if there were districts.
			if (fromHasDistrict)
				BukkitTools.fireEvent(new PlayerExitsFromDistrictEvent(event.getPlayer(), to, from, fromDistrict, event.getMoveEvent()));
			if (toHasDistrict)
				BukkitTools.fireEvent(new PlayerEntersIntoDistrictEvent(event.getPlayer(), to, from, toDistrict, event.getMoveEvent()));
		}
	}

	/*
	 * onOutlawEnterTown
	 * - Handles outlaws entering a town they are outlawed in.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onOutlawEnterTown(PlayerEntersIntoTownBorderEvent event) {

		Resident outlaw = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (outlaw == null || event.getPlayer().getGameMode().equals(GameMode.SPECTATOR))
			return;
		
		Town town = event.getEnteredTown();

		if (outlaw.isJailed() && outlaw.getJailTown().equals(town))
			return;
		
		if (town.hasOutlaw(outlaw))
			ResidentUtil.outlawEnteredTown(outlaw, town, event.getPlayer().getLocation());
	}

	/**
	 * - Handles the KeepInventory/KeepLevel aspects of Towny's feature-set.
	 * - Throws API events which can allow other plugins to cancel Towny saving
	 *   inventory and/or experience.
	 * 
	 * @param event - PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	// Why Highest??, so that we are the last ones to check for if it keeps their inventory, and then have no problems with it.
	public void onPlayerDeathHandleKeepLevelAndInventory(PlayerDeathEvent event) {
		Resident resident = TownyAPI.getInstance().getResident(event.getEntity());
		if (resident == null)
			return;

		TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getEntity().getLocation());

		/* Handle Inventory Keeping with our own PlayerKeepsInventoryEvent. */
		tryKeepInventory(event, resident, tb);

		/* Handle Experience Keeping with our own PlayerKeepsExperienceEvent. */
		tryKeepExperience(event, tb);
	}

	private boolean tryKeepInventory(PlayerDeathEvent event, Resident resident, TownBlock tb) {
		boolean keepInventory = getKeepInventoryValue(event.getKeepInventory(), resident, tb);
		PlayerKeepsInventoryEvent pkie = new PlayerKeepsInventoryEvent(event, keepInventory);
		if (!BukkitTools.isEventCancelled(pkie)) {
			event.setKeepInventory(true);
			event.getDrops().clear();
			return true;
		}
		return false;
	}

	private boolean getKeepInventoryValue(boolean keepInventory, Resident resident, TownBlock tb) {
		// Run it this way so that we will override a plugin that has kept the
		// inventory, but they're in the wilderness where we don't want to keep
		// inventories.
		// Sometimes we keep the inventory when they are in any town.
		keepInventory = TownySettings.getKeepInventoryInTowns() && tb != null;

		// All of the other tests require a town.
		if (tb == null)
			return keepInventory;

		if (resident.hasTown() && !keepInventory) {
			Town town = resident.getTownOrNull();
			Town tbTown = tb.getTownOrNull();
			// Sometimes we keep the inventory only when they are in their own town.
			if (TownySettings.getKeepInventoryInOwnTown() && tbTown.equals(town))
				keepInventory = true;
			// Sometimes we keep the inventory only when they are in a Town that considers them an ally.
			if (TownySettings.getKeepInventoryInAlliedTowns() && !keepInventory && tbTown.isAlliedWith(town))
				keepInventory = true;
		}

		// Sometimes we keep the inventory when they are in an Arena plot.
		if (TownySettings.getKeepInventoryInArenas() && !keepInventory && tb.getType() == TownBlockType.ARENA)
			keepInventory = true;

		return keepInventory;
	}

	@EventHandler(ignoreCancelled = true)
	public void onArmourDamageEvent(PlayerItemDamageEvent event) {
		if (!TownySettings.arenaPlotPreventArmourDegrade())
			return;

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getPlayer());
		if (tb == null || !tb.getType().equals(TownBlockType.ARENA))
			return;

		if (!ItemLists.ARMOURS.contains(event.getItem()) && !ItemLists.WEAPONS.contains(event.getItem()))
			return;

		event.setCancelled(true);
	}

	private boolean tryKeepExperience(PlayerDeathEvent event, TownBlock tb) {
		boolean keepExperience = getKeepExperienceValue(tb != null, tb != null ? tb.getType() : null); 
		PlayerKeepsExperienceEvent pkee = new PlayerKeepsExperienceEvent(event, keepExperience);
		if (!BukkitTools.isEventCancelled(pkee)) {
			event.setKeepLevel(true);
			event.setDroppedExp(0);
			return true;
		}
		return false;
	}

	private boolean getKeepExperienceValue(boolean inTown, TownBlockType type) {
		// We never keep experience in the wilderness.
		if (!inTown)
			return false;

		// We sometimes keep experience if its in town.
		if (TownySettings.getKeepExperienceInTowns())
			return true;

		// We sometimes keep experience in Arena Plots.
		return type != null && type == TownBlockType.ARENA && TownySettings.getKeepExperienceInArenas();
	}

	/**
	 * PlayerEnterTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering a town.
	 *   
	 * @param event PlayerEntersIntoTownBorderEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerEnterTown(PlayerEntersIntoTownBorderEvent event) {
		Resident resident = event.getResident();
		Town town = event.getEnteredTown();
		if (resident == null || town == null)
			return;

		if (TownySettings.isNotificationUsingTitles() && resident.isSeeingBorderTitles()) {
			TitleNotificationEvent tne = new TitleNotificationEvent(new TitleNotification(town, event.getTo()), event.getPlayer());
			BukkitTools.fireEvent(tne);
			String title = tne.getTitleNotification().getTitleNotification();
			String subtitle = tne.getTitleNotification().getSubtitleNotification();
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle, TownySettings.getNotificationTitlesDurationTicks());
		}
	}
	

	/**
	 * PlayerLeaveTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering the wilderness.
	 *   - unjailing residents
	 *   
	 * @param event PlayerExitsFromTownBorderEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLeaveTown(PlayerExitsFromTownBorderEvent event) {
		Resident resident = event.getResident();
		// Likely a Citizens NPC.
		if (resident == null || !event.getTo().isWilderness())
			return;

		if (TownySettings.isNotificationUsingTitles() && resident.isSeeingBorderTitles()) {
			TitleNotificationEvent tne = new TitleNotificationEvent(new TitleNotification(event.getLeftTown(), event.getTo()), event.getPlayer());
			BukkitTools.fireEvent(tne);
			String title = tne.getTitleNotification().getTitleNotification();
			String subtitle = tne.getTitleNotification().getSubtitleNotification();
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle, TownySettings.getNotificationTitlesDurationTicks());
		}

		if (resident.isJailed())
			JailUtil.unJailResident(resident, UnJailReason.ESCAPE);
	}
	
	/**
	 * Any player that can break the lectern will be able to get the book anyways.
	 * @param event - PlayerTakeLecternBookEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerTakeLecternBookEvent(PlayerTakeLecternBookEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getLectern().getWorld()))
			return;
		
		//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
		event.setCancelled(!TownyActionEventExecutor.canDestroy(event.getPlayer(), event.getLectern().getLocation(), Material.LECTERN));
	}

	/**
	 * Blocks jailed players using blacklisted commands.
	 * @param event - PlayerCommandPreprocessEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerUsesCommand(PlayerCommandPreprocessEvent event) {
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		checkForOpDeOpCommand(event);

		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());

		// More than likely another plugin using a fake player to run a command or,
		// the player is an admin/op.
		if (resident == null || resident.isAdmin())
			return;
		
		final String command = event.getMessage();
		
		if (blockJailedPlayerCommand(event.getPlayer(), resident, command)) {
			event.setCancelled(true);
			return;
		}
		
		// Potentially blocks players with a war from using the command.
		if (blockWarPlayerCommand(event.getPlayer(), resident, command)) {
			event.setCancelled(true);
			return;
		}
		
		// Location-dependent blocked commands
		final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(event.getPlayer());
		if (blockOutlawedPlayerCommand(event.getPlayer(), resident, townBlock, command) || blockCommandInsideTown(event.getPlayer(), resident, townBlock, command))
			event.setCancelled(true);
	}

	private void checkForOpDeOpCommand(PlayerCommandPreprocessEvent event) {
		String[] args = CommandList.normalizeCommand(event.getMessage()).split(" ");
		String command = args[0];
		// Fail early if we aren't looking at /op|deop [playername]
		if ((!command.equalsIgnoreCase("op") && !command.equalsIgnoreCase("deop")) || args.length != 2)
			return;

		// Get the target.
		Player target = Bukkit.getPlayer(args[1]);
		if (target == null || !target.isOnline())
			return;

		// Make sure they have the permission to run the command.
		if (!event.getPlayer().hasPermission("minecraft.command." + command))
			return;

		// Make sure they're not running the command which will have no effect.
		if (target.isOp() == "op".equalsIgnoreCase(command))
			return;

		// Delete the online player's cache because they have been op'd or deop'd.
		Towny plugin = Towny.getPlugin();
		plugin.getScheduler().runLater(target, () -> plugin.deleteCache(target), 1L);
	}

	public boolean blockWarPlayerCommand(Player player, Resident resident, String command) {
		if (resident.hasTown() && resident.getTownOrNull().hasActiveWar() && blockedWarCommands.containsCommand(command)) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_war_blocked"));
			return true;
		}
		return false;
	}
	
	/**
	 * Blocks outlawed players using blacklisted commands.
	 * @return Whether the command has been blocked.   
	 */
	public boolean blockOutlawedPlayerCommand(Player player, Resident resident, TownBlock townBlock, String command) {
		if (townBlock != null && townBlock.hasTown()) {
			Town town = townBlock.getTownOrNull();
			
			if (town != null && town.hasOutlaw(resident) && blockedOutlawCommands.containsCommand(command)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_you_cannot_use_command_while_in_outlaw_town"));
				return true;
			}
		}
		
		return false;
	}
	
	public boolean blockJailedPlayerCommand(Player player, Resident resident, String command) {
		if (!resident.isJailed())
			return false;
		
		if (blockedJailCommands.containsCommand(command)) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_you_cannot_use_that_command_while_jailed"));
			return true;
		}
		
		return false;
	}
	
	/** 
	 * Allows restricting commands while being in a town.
	 * Also allows limiting commands to personally-owned plots only.
	 * Works almost the same way as jail command blacklisting, except has more stuff
	 */
	public boolean blockCommandInsideTown(Player player, Resident resident, TownBlock townBlock, String command) {
		if (!TownySettings.allowTownCommandBlacklisting())
			return false;
		
		// Let admins run commands.
		if (resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_TOWN_COMMAND_BLACKLIST_BYPASS.getNode()))
			return false;
		
		final Town town = townBlock == null ? null : townBlock.getTownOrNull();

		if (town != null && town.hasActiveWar() && blockedWarCommands.containsCommand(command)) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_war_blocked"));
			return true;
		}
		/*
		 * Commands are sometimes blocked from being run by outsiders on an town. 
		 */
		if (blockedTownCommands.containsCommand(command) && blockedTouristCommands.containsCommand(command)) {
			// Allow these commands to be run in the wilderness.
			if (town == null)
				return false;

			// Allow own town & let globally welcomed players run commands, also potentially allow trusted and allied residents.
			if (town.hasResident(resident) 
				|| resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_TOURIST_COMMAND_LIMITATION_BYPASS.getNode())
				|| TownySettings.doTrustedResidentsBypassTownBlockedCommands() && town.hasTrustedResident(resident)
				|| (resident.hasTown() && TownySettings.doAlliesBypassTownBlockedCommands() && CombatUtil.isAlly(town, resident.getTownOrNull())))
				return false;

			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_outsider_blocked", town.getName()));
			return true;
		}

		/*
		 * Commands are sometimes blocked from being run inside any town.
		 */
		if (town != null && blockedTownCommands.containsCommand(command)) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_blocked_inside_towns"));
			return true;
		}

		/*
		 * Commands are sometimes limited to only plots that players personally own.
		 */
		if (ownPlotLimitedCommands.containsCommand(command)) {
			
			// Stop the command being run because this is in the wilderness.
			if (town == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_limited"));
				return true;
			}

			// If the player is in their own town and has towny.claimed.owntown.build.dirt 
			// (or more likely towny.claimed.owntown.*) then allow them to use the command.
			// It is likely a mayor/assistant.
			if (town.hasResident(player) && TownyUniverse.getInstance().getPermissionSource().hasOwnTownOverride(player, Material.DIRT, ActionType.BUILD))
				return false;
			
			Resident owner = townBlock.getResidentOrNull();
			
			if (owner != null) {
				// This is a personally-owned plot, let's make sure the player is the plot owner.
				
				// The owner and player are not the same, cancel the command.
				if (!owner.getName().equals(player.getName())) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_limited"));
					return true;
				}
				// Player owns this plot personally, we won't limit their command-usage.
			} else {
				// This is not a personally-owned plot, but let's only block this command if the player isn't special to the town.
				
				// If the player is in their own town and has towny.claimed.townowned.build.dirt 
				// (or more likely towny.claimed.townowned.*) then allow them to use the command.
				// It is likely a assistant or town-ranked player. 
				if (town.hasResident(player) && TownyUniverse.getInstance().getPermissionSource().hasTownOwnedOverride(player, Material.DIRT, ActionType.BUILD)) {
					return false;
					
				// Not a special person, and not in a personally-owned plot, cancel this command.
				} else { 
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_command_limited"));
					return true;
				}
			}
		}

		return false;
	}
	
	/*
	 *  Handles AdminTool use on Blocks
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onAdminToolUseOnBlocks(PlayerInteractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getHand() == EquipmentSlot.OFF_HAND || !TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		if (event.hasItem()
				&& event.getPlayer().getInventory().getItemInMainHand().getType().getKey().getKey().equalsIgnoreCase(TownySettings.getTool()) 
				&& plugin.hasPlayerMode(event.getPlayer(), "infotool")
				&& TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer())
				&& event.getClickedBlock() != null) {
					Player player = event.getPlayer();
					Block block = event.getClickedBlock();
					final BlockState state = block.getState(false);
					final BlockData data = state.getBlockData();
					
					if (ItemLists.SIGNS.contains(block.getType()) && data instanceof Rotatable rotatable) {
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Sign Info"),
								ChatTools.formatCommand("", "Sign Type", "", block.getType().name()),
								ChatTools.formatCommand("", "Facing", "", rotatable.getRotation().toString())
								));
					} else if (Tag.DOORS.isTagged(block.getType())) {
						org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) block.getBlockData();
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Door Info"),
								ChatTools.formatCommand("", "Door Type", "", block.getType().getKey().toString()),
								ChatTools.formatCommand("", "hinged on ", "", String.valueOf(door.getHinge())),
								ChatTools.formatCommand("", "isOpen", "", String.valueOf(door.isOpen())),
								ChatTools.formatCommand("", "getFacing", "", door.getFacing().name())
								));
					} else {
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Block Info"),
								ChatTools.formatCommand("", "Material", "", block.getType().getKey().toString()),								      
								ChatTools.formatCommand("", "MaterialData", "", block.getBlockData().getAsString())
								));
					}
					event.setUseInteractedBlock(Event.Result.DENY);
					event.setCancelled(true);
		}
	}

	/*
	 *  Handles AdminTool use on Entities
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onAdminToolUseOnEntities(PlayerInteractEntityEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (event.getHand() == EquipmentSlot.OFF_HAND || !TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		if (event.getRightClicked() != null
				&& event.getPlayer().getInventory().getItemInMainHand().getType().getKey().getKey().equalsIgnoreCase(TownySettings.getTool())
				&& plugin.hasPlayerMode(event.getPlayer(), "infotool")
				&& TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer())) {

				Entity entity = event.getRightClicked();

				TownyMessaging.sendMessage(event.getPlayer(), Arrays.asList(
						ChatTools.formatTitle("Entity Info"),
						ChatTools.formatCommand("", "Entity Class", "", entity.getType().getEntityClass().getSimpleName()),
						ChatTools.formatCommand("", "Entity Type", "", entity.getType().getKey().toString())
						));

				event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEggLand(PlayerEggThrowEvent event) {
		if (TownySettings.isItemUseMaterial(Material.EGG, event.getEgg().getLocation()) && !TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getEgg().getLocation(), Material.EGG))
			event.setHatching(false);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerPickupItem(EntityPickupItemEvent event) {
		if (TownySettings.getRespawnProtectionAllowPickup() || !(event.getEntity() instanceof Player player))
			return;
		
		Resident resident = TownyAPI.getInstance().getResident(player);
		if (resident == null)
			return;
		
		if (resident.hasRespawnProtection()) {
			event.setCancelled(true);

			if (!resident.isRespawnPickupWarningShown()) {
				resident.setRespawnPickupWarningShown(true);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_pickup_respawn_protection"));
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerChangeGameMode(PlayerGameModeChangeEvent event) {
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		Towny.getPlugin().deleteCache(event.getPlayer());
	}

	private void loadBlockedCommandLists() {
		this.blockedJailCommands = new CommandList(TownySettings.getJailBlacklistedCommands());
		this.blockedTouristCommands = new CommandList(TownySettings.getTouristBlockedCommands());
		this.blockedTownCommands = new CommandList(TownySettings.getTownBlacklistedCommands());
		this.blockedOutlawCommands = new CommandList(TownySettings.getOutlawBlacklistedCommands());
		this.blockedWarCommands = new CommandList(TownySettings.getWarBlacklistedCommands());
		this.ownPlotLimitedCommands = new CommandList(TownySettings.getPlayerOwnedPlotLimitedCommands());
	}
	
	private boolean isSignWaxed(Block block) {
		if (MinecraftVersion.CURRENT_VERSION.isOlderThan(MinecraftVersion.MINECRAFT_1_20) || !(block.getState(false) instanceof Sign sign))
			return false;
		
		try {
			return sign.isWaxed();
		} catch (NoSuchMethodError e) {
			// Method does not exist in this version
			return false;
		}
	}
}
