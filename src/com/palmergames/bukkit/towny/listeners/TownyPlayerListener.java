package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.BedExplodeEvent;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.PlayerEnterTownEvent;
import com.palmergames.bukkit.towny.event.PlayerLeaveTownEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.event.player.PlayerDeniedBedUseEvent;
import com.palmergames.bukkit.towny.event.player.PlayerEntersIntoTownBorderEvent;
import com.palmergames.bukkit.towny.event.player.PlayerExitsFromTownBorderEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKeepsExperienceEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKeepsInventoryEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.CommandList;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.util.StringMgmt;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.MetadataValue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle events for all Player related events
 * Players deaths are handled both here and in the TownyEntityMonitorListener
 * 
 * @author Shade/ElgarL
 * 
 */
@SuppressWarnings("deprecation")
public class TownyPlayerListener implements Listener {

	private final Towny plugin;
	private CommandList blockedJailCommands;
	private CommandList blockedTownCommands;
	private CommandList blockedTouristCommands;
	private CommandList blockedOutlawCommands;
	private CommandList blockedWarCommands;
	private CommandList ownPlotLimitedCommands;

	public TownyPlayerListener(Towny plugin) {
		this.plugin = plugin;
		TownySettings.addReloadListener(NamespacedKey.fromString("blocked-commands", plugin), config -> loadBlockedCommandLists());
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
		
		// Perform login code in it's own thread to update Towny data.
		if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(Towny.getPlugin(), player), 0L) == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
		}
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
			player.sendMessage(ChatColor.RED + "[Towny] [Error] Towny is locked in Safe Mode due to an error! " + msg);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {

		if (plugin.isError()) {
			return;
		}
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (resident != null) {
			// Don't set last online if the player was vanished.
			if (!event.getPlayer().getMetadata("vanished").stream().anyMatch(MetadataValue::asBoolean))
				resident.setLastOnline(System.currentTimeMillis());
			resident.clearModes();
			resident.save();

			if (TownyTimerHandler.isTeleportWarmupRunning()) {
				TownyAPI.getInstance().abortTeleportRequest(resident);
			}
			
			if (JailUtil.isQueuedToBeJailed(resident))
				event.getPlayer().setHealth(0);
		}

		plugin.deleteCache(event.getPlayer());
		TownyPerms.removeAttachment(event.getPlayer().getName());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (plugin.isError() || isEndPortalRespawn(event)) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!TownySettings.isTownRespawning()) {
			return;
		}
		
		// If respawn anchors have higher precedence than town spawns, use them instead.
		if (event.isAnchorSpawn() && TownySettings.isRespawnAnchorHigherPrecedence()) {
			return;
		}

		Location respawn;
		respawn = TownyAPI.getInstance().getTownSpawnLocation(player);

		// Towny might be prioritizing bed spawns over town spawns.
		if (TownySettings.getBedUse()) { 
			Location bed = player.getBedSpawnLocation();
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
		if (protectionTime > 0L) {
			Resident res = TownyAPI.getInstance().getResident(player);
			if (res == null)
				return;
			
			res.addRespawnProtection(protectionTime);
		}
	}
	
	@SuppressWarnings({"unchecked", "JavaReflectionMemberAccess"})
	private boolean isEndPortalRespawn(PlayerRespawnEvent event) {
		try {
			// https://jd.papermc.io/paper/1.19/org/bukkit/event/player/PlayerRespawnEvent.html#getRespawnFlags()
			Collection<Object> respawnFlags = (Collection<Object>) PlayerRespawnEvent.class.getDeclaredMethod("getRespawnFlags").invoke(event);
			
			Method name = null;
			for (Object flag : respawnFlags) {
				if (name == null)
					name = flag.getClass().getMethod("name");
				
				if ("END_PORTAL".equals(name.invoke(flag)))
					return true;
			}
			
			return false;
		} catch (ReflectiveOperationException e) {
			// Spigot
			final Player player = event.getPlayer();
			
			if (player.getWorld().getEnvironment() != Environment.THE_END)
				return false;
			
			// Can cause a sync chunk load
			// Check if legs or head is inside an end portal block
			return player.getLocation().getBlock().getType() == Material.END_PORTAL || player.getEyeLocation().getBlock().getType() == Material.END_PORTAL;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJailRespawn(PlayerRespawnEvent event) {

		if (plugin.isError()) {
			return;
		}
	
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());

		// If player is jailed send them to their jailspawn.
		if (resident != null && resident.isJailed())
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
		
		// Bail if we're filling air, usually a milked cow.
		if (event.getBlockClicked().getType().equals(Material.AIR))
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
		if(action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR)
			return;
		
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
			if (TownySettings.isItemUseMaterial(item, loc))
				event.setCancelled(!TownyActionEventExecutor.canItemuse(player, loc, item));

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
				/*
				 * Test stripping logs, scraping copper blocks, dye-able signs,
				 * glass bottles, flint&steel on TNT and shears on beehomes
				 * 
				 * Treat interaction as a Destroy test.
				 */
				if ((ItemLists.AXES.contains(item) && (ItemLists.UNSTRIPPED_WOOD.contains(clickedMat) || ItemLists.WAXED_BLOCKS.contains(clickedMat) || ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat))) ||
					(ItemLists.DYES.contains(item) && Tag.SIGNS.isTagged(clickedMat)) ||
					(item == Material.FLINT_AND_STEEL && clickedMat == Material.TNT) ||
					((item == Material.GLASS_BOTTLE || item == Material.SHEARS) && (clickedMat == Material.BEE_NEST || clickedMat == Material.BEEHIVE || clickedMat == Material.PUMPKIN))) { 

					event.setCancelled(!TownyActionEventExecutor.canDestroy(player, loc, clickedMat));
				}

				/*
				 * Test bonemeal usage. Treat interaction as a Build test.
				 */
				if (item == Material.BONE_MEAL) 
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, loc, item));
				
				/*
				 * Test putting candles on cakes. Treat interaction as a Build test.
				 */
				if (ItemLists.CANDLES.contains(item) && clickedMat == Material.CAKE) 
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, loc, item));
				
				/*
				 * Test wax usage. Treat interaction as a Build test.
				 */
				if (item == Material.HONEYCOMB && ItemLists.WEATHERABLE_BLOCKS.contains(clickedMat))
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, loc, item));

				/*
				 * Test if we're about to spawn either entity. Uses build test.
				 */
				if (item == Material.ARMOR_STAND || item == Material.END_CRYSTAL) 
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, clickedBlock.getRelative(event.getBlockFace()).getLocation(), item));

				/*
				 * Test if we're putting a book into a BookContainer.
				 */
				if (ItemLists.PLACEABLE_BOOKS.contains(item) && ItemLists.BOOK_CONTAINERS.contains(clickedMat))
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, loc, item));

				/*
				 * Catches hoes taking dirt from Rooted Dirt blocks.
				 */
				if (clickedMat.name().equals("ROOTED_DIRT") && item.name().toLowerCase().contains("_hoe"))
					event.setCancelled(!TownyActionEventExecutor.canDestroy(player, clickedBlock));

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
			if (TownySettings.isSwitchMaterial(clickedMat, clickedBlock.getLocation())) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canSwitch(player, clickedBlock.getLocation(), clickedMat));
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
				clickedMat.name().equals("CHISELED_BOOKSHELF") ||
				clickedMat == Material.BEACON || clickedMat == Material.DRAGON_EGG || 
				clickedMat == Material.COMMAND_BLOCK){
				
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, clickedBlock.getLocation(), clickedMat));
				return;
			}
		}
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
			if (block.getType() == Material.RESPAWN_ANCHOR && !isRespawnAnchorWorking(block)) {
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

	private boolean isRespawnAnchorWorking(Block block) {
		boolean allowedInWorld = false;
		try {
			allowedInWorld = block.getWorld().isRespawnAnchorWorks();
		} catch (NoSuchMethodError ignored) {
			// Pre-1.18 versions of the Bukkit API do not have an isRespawnAnchorWorks() method.
			// TODO: Drop this when Towny no longer supports earlier than MC 1.18.
			allowedInWorld = block.getWorld().getEnvironment().equals(Environment.NETHER);
		}
		return allowedInWorld;
	}

	/*
	 * This method will stop a player Right Clicking on a respawn anchor if:
	 * - The world is an anchor-allowed world (the nether) and,
	 * - The Block is an anchor and,
	 * - The Anchor has charges and,
	 * - The Item in their hand is nothing or (not-glowstone or the charges are full.) 
	 */
	private boolean disallowedAnchorClick(PlayerInteractEvent event, Block block) {
		return isRespawnAnchorWorking(block)
			&& block.getBlockData() instanceof RespawnAnchor anchor 
			&& anchor.getCharges() > 0 
			&& (event.getItem() == null || (event.getItem().getType() != Material.GLOWSTONE || anchor.getCharges() >= anchor.getMaximumCharges()));
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
		
		if (event.getRightClicked() != null) {
			Player player = event.getPlayer();
			Material mat = null;
			ActionType actionType = ActionType.DESTROY;
			
			// PlayerInventory#getItem(EquipmentSlot) does not exist on <1.16, so this has to be used
			Material item = event.getHand().equals(EquipmentSlot.HAND) ? event.getPlayer().getInventory().getItemInMainHand().getType() : event.getPlayer().getInventory().getItemInOffHand().getType();

			/*
			 * The following will get us a Material substituted in for an Entity so that we can run permission tests.
			 * Anything not in the switch will leave the block null.
			 */
			switch (event.getRightClicked().getType()) {
				/*
				 * First are tested with a Destroy perm check.
				 */
				case PUFFERFISH:
				case TROPICAL_FISH:
				case SALMON:
				case COD:
				case ITEM_FRAME:
				case GLOW_ITEM_FRAME:
				case PAINTING:
				case LEASH_HITCH:
				case MINECART_COMMAND:
				case MINECART_TNT:
				case MINECART_MOB_SPAWNER:
				case TADPOLE:
				case AXOLOTL:
					mat = EntityTypeUtil.parseEntityToMaterial(event.getRightClicked().getType());
					break;
				/*
				 * These two block the dying of sheep and wolf's collars.
				 */
				case SHEEP:
				case WOLF:
					if (item != null) {
						if (ItemLists.DYES.contains(item)) {
							mat = item;
							break;
						}
					}	
				/*
				 * Afterwards they will remain as Switch perm checks.
				 */
				case MINECART_CHEST:
				case MINECART_FURNACE:
				case MINECART_HOPPER:
				case CHEST_BOAT:
					mat = EntityTypeUtil.parseEntityToMaterial(event.getRightClicked().getType());
					actionType = ActionType.SWITCH;
					break;
				/*
				 * Don't set {@code mat} for other entity types.
				 */
				default:
				    break;
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
			if (item != null) {

				/*
				 * Sheep can be sheared, protect them if they aren't in the wilderness.
				 */
				if (event.getRightClicked().getType().equals(EntityType.SHEEP) && item == Material.SHEARS && !TownyAPI.getInstance().isWilderness(event.getRightClicked().getLocation())) {
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
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		// Let's ignore Citizens NPCs
		if (PluginIntegrations.getInstance().checkCitizens(event.getPlayer()))
			return;
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		/*
		 * Abort if we havn't really moved
		 */
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
			return;
		}

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from;
		PlayerCache cache = plugin.getCache(player);
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident != null
				&& TownyTimerHandler.isTeleportWarmupRunning()	 
				&& TownySettings.getTeleportWarmupTime() > 0
				&& TownySettings.isMovementCancellingSpawnWarmup()
				&& resident.getTeleportRequestTime() > 0
				&& !townyUniverse.getPermissionSource().isTownyAdmin(player)) {
			TeleportWarmupTimerTask.abortTeleportRequest(resident);
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_teleport_cancelled"));
		}

		try {
			from = cache.getLastLocation();
		} catch (NullPointerException e) {
			from = event.getFrom();
		}
		
		if (WorldCoord.cellChanged(from, to)) {

			TownyWorld fromWorld = TownyAPI.getInstance().getTownyWorld(from.getWorld());				
			TownyWorld toWorld = TownyAPI.getInstance().getTownyWorld(to.getWorld());
			if (fromWorld == null || toWorld == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("not_registered"));
				cache.setLastLocation(to);
				return;
			}
			WorldCoord fromCoord = new WorldCoord(fromWorld.getName(), fromWorld.getUUID(), Coord.parseCoord(from));
			WorldCoord toCoord = new WorldCoord(toWorld.getName(), fromWorld.getUUID(), Coord.parseCoord(to));
			
			onPlayerMoveChunk(player, fromCoord, toCoord, from, to, event);
		}

		// Update the cached players current location
		cache.setLastLocation(to);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		// Let's ignore Citizens NPCs. This must come before the safemode check, as Citizens stores their NPCs
		// at the world spawn until a player loads a chunk, to which the NPC is then teleported. Towny would
		// prevent them teleporting, leaving them at spawn even after Safe Mode is cleaned up.
		if (PluginIntegrations.getInstance().checkCitizens(event.getPlayer()))
			return;
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		Player player = event.getPlayer();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		boolean isAdmin = resident != null && (resident.isAdmin() || resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_OUTLAW_TELEPORT_BYPASS.getNode()));
		// Cancel teleport if Jailed by Towny and not an admin.
		if (resident != null && resident.isJailed() && !isAdmin) {
			if ((event.getCause() == TeleportCause.COMMAND)) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
				return;
			}
			if (event.getCause() == TeleportCause.PLUGIN)
				return;
			if ((event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT) && !TownySettings.JailAllowsTeleportItems()) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
			}
		}
		
		// Cancel teleport if resident is outlawed in Town and not an admin.
		if (resident != null && !TownySettings.canOutlawsTeleportOutOfTowns() && !isAdmin) {
			TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getFrom());
			if (tb != null && tb.hasTown()) {
				Town town = tb.getTownOrNull();
				
				if (town != null && town.hasOutlaw(resident)) {
					if ((event.getCause() == TeleportCause.COMMAND)) {
						TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_outlawed_players_no_teleport"));
						event.setCancelled(true);
						return;
					}
					if (event.getCause() == TeleportCause.PLUGIN)
						return;
					if (!TownySettings.canOutlawsUseTeleportItems() && (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT)) {
						TownyMessaging.sendErrorMsg(event.getPlayer(), Translatable.of("msg_err_outlawed_players_no_teleport"));
						event.setCancelled(true);
					}
				}
			}
		}

		/*
		 * Test to see if CHORUS_FRUIT is in the item_use list.
		 */
		if (event.getCause() == TeleportCause.CHORUS_FRUIT && TownySettings.isItemUseMaterial(Material.CHORUS_FRUIT, event.getTo()) && !isAdmin) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.CHORUS_FRUIT)) {
				event.setCancelled(true);
				return;
			}
		}	
			
		/*
		 * Test to see if Ender pearls are disabled.
		 */		
		if (event.getCause() == TeleportCause.ENDER_PEARL && TownySettings.isItemUseMaterial(Material.ENDER_PEARL, event.getTo()) && !isAdmin) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.ENDER_PEARL)) {
				event.setCancelled(true);
				return;
			}
		}
		
		/*
		 * Remove spawn protection if the player is teleporting since spawning.
		 */
		if (resident != null && resident.hasRespawnProtection()) {
			resident.removeRespawnProtection();
		}
		
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
				test = TownyActionEventExecutor.canDestroy(player, caught.getLocation(), Material.GRASS);
			}
			if (!test) {
				event.setCancelled(true);
				event.getHook().remove();
			}
		}	
	}

	/*
	* PlayerMoveEvent that can fire the PlayerChangePlotEvent
	*/
	public void onPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to, Location fromLoc, Location toLoc, PlayerMoveEvent moveEvent) {

		plugin.getCache(player).setLastLocation(toLoc);
		plugin.getCache(player).updateCoord(to);

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
			// Old event which will be removed later on.
			BukkitTools.fireEvent(new PlayerLeaveTownEvent(event.getPlayer(), to, from, from.getTownOrNull(), event.getMoveEvent()));
		} else if (from.isWilderness()) {
			// Gone from wilderness into Town.
			BukkitTools.fireEvent(new PlayerEntersIntoTownBorderEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
			// Old event which will be removed later on.
			BukkitTools.fireEvent(new PlayerEnterTownEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
		// Both to and from have towns.
		} else if (to.getTownOrNull().equals(from.getTownOrNull())) {
			// The towns are the same, no event will fire.
			return;
		} else {
			// Player has left one Town and immediately entered a different one.
			BukkitTools.fireEvent(new PlayerEntersIntoTownBorderEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
			BukkitTools.fireEvent(new PlayerExitsFromTownBorderEvent(event.getPlayer(), to, from, from.getTownOrNull(), event.getMoveEvent()));
			// Old events which will be removed later on.
			BukkitTools.fireEvent(new PlayerEnterTownEvent(event.getPlayer(), to, from, to.getTownOrNull(), event.getMoveEvent()));
			BukkitTools.fireEvent(new PlayerLeaveTownEvent(event.getPlayer(), to, from, from.getTownOrNull(), event.getMoveEvent()));
		}
	}
	
	/*
	 * onOutlawEnterTown
	 * - Handles outlaws entering a town they are outlawed in.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onOutlawEnterTown(PlayerEntersIntoTownBorderEvent event) {

		Resident outlaw = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (outlaw == null)
			return;
		
		Town town = event.getEnteredTown();

		if (outlaw.isJailed() && outlaw.getJailTown().equals(town))
			return;
		
		if (town.hasOutlaw(outlaw))
			ResidentUtil.outlawEnteredTown(outlaw, town, event.getPlayer().getLocation());
	}

	/**
	 * onPlayerDieInTown
	 * - Handles death events and the KeepInventory/KeepLevel options are being used.
	 * - Throws API events which can allow other plugins to cancel Towny saving
	 *   inventory and/or experience.
	 * 
	 * @author - Articdive, LlmDl
	 * @param event - PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	// Why Highest??, so that we are the last ones to check for if it keeps their inventory, and then have no problems with it.
	public void onPlayerDieInTown(PlayerDeathEvent event) {
		Resident resident = TownyAPI.getInstance().getResident(event.getEntity());
		TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getEntity().getLocation());
		if (resident == null || tb == null)
			return;
		boolean keepInventory = event.getKeepInventory();
		boolean keepLevel = event.getKeepLevel();
		if (TownySettings.getKeepExperienceInTowns() && !keepLevel)
			keepLevel = tryKeepExperience(event);
		
		if (TownySettings.getKeepInventoryInTowns() && !keepInventory)
			keepInventory = tryKeepInventory(event);
		
		if (resident.hasTown() && !keepInventory) {
			Town town = resident.getTownOrNull();
			Town tbTown = tb.getTownOrNull();
			if (TownySettings.getKeepInventoryInOwnTown() && tbTown.equals(town))
				keepInventory = tryKeepInventory(event);
			if (TownySettings.getKeepInventoryInAlliedTowns() && !keepInventory && tbTown.isAlliedWith(town))
				keepInventory = tryKeepInventory(event);
		}
		
		if (TownySettings.getKeepInventoryInArenas() && !keepInventory && tb.getType() == TownBlockType.ARENA)
			tryKeepInventory(event);
		
		if (TownySettings.getKeepExperienceInArenas() && !keepLevel && tb.getType() == TownBlockType.ARENA)
			tryKeepExperience(event);
	}

	private boolean tryKeepExperience(PlayerDeathEvent event) {
		PlayerKeepsExperienceEvent pkee = new PlayerKeepsExperienceEvent(event);
		if (!BukkitTools.isEventCancelled(pkee)) {
			event.setKeepLevel(true);
			event.setDroppedExp(0);
			return true;
		}
		return false;
	}

	private boolean tryKeepInventory(PlayerDeathEvent event) {
		PlayerKeepsInventoryEvent pkie = new PlayerKeepsInventoryEvent(event);
		if (!BukkitTools.isEventCancelled(pkie)) {
			event.setKeepInventory(true);
			event.getDrops().clear();
			return true;
		}
		return false;
	}


	/**
	 * PlayerEnterTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering a town.
	 *   
	 * @param event - PlayerEnterTownEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerEnterTown(PlayerEntersIntoTownBorderEvent event) {
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		Town town = event.getEnteredTown();
		
		if (resident != null && resident.isSeeingBorderTitles() && town != null && TownySettings.isNotificationUsingTitles()) {
			String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownTitle());
			String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownSubtitle());
			
			HashMap<String, Object> placeholders = new HashMap<>();
			placeholders.put("{townname}", StringMgmt.remUnderscore(TownySettings.isNotificationsTownNamesVerbose() ? town.getFormattedName() : town.getName()));
			placeholders.put("{town_motd}", town.getBoard());
			placeholders.put("{town_residents}", town.getNumResidents());
			placeholders.put("{town_residents_online}", TownyAPI.getInstance().getOnlinePlayers(town).size());

			Nation nation = town.getNationOrNull();
			placeholders.put("{nationname}", nation == null ? "" : String.format(TownySettings.getNotificationTitlesNationNameFormat(), nation.getName()));
			placeholders.put("{nation_residents}", nation == null ? "" : nation.getNumResidents());
			placeholders.put("{nation_residents_online}", nation == null ? "" : TownyAPI.getInstance().getOnlinePlayers(nation).size());
			placeholders.put("{nation_motd}", nation == null ? "" : nation.getBoard());
			placeholders.put("{nationcapital}", !town.isCapital() ? "" : getCapitalSlug(town.getName(), nation.getName()));

			for(Map.Entry<String, Object> placeholder: placeholders.entrySet()) {
				title = title.replace(placeholder.getKey(), placeholder.getValue().toString());
				subtitle = subtitle.replace(placeholder.getKey(), placeholder.getValue().toString());
			}
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);
		}
	}
	
	private Object getCapitalSlug(String townName, String nationName) {
		String format = TownySettings.getNotificationTitlesNationCapitalFormat();
		if (format.contains("%t") || format.contains("%n"))
			return format.replace("%t", townName).replace("%n", nationName);
		else 
			return String.format(format, nationName, townName);
	}

	/**
	 * PlayerLeaveTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering the wilderness.
	 *   - unjailing residents
	 *   
	 * @param event - PlayerLeaveTownEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLeaveTown(PlayerExitsFromTownBorderEvent event) {
		Resident resident = TownyAPI.getInstance().getResident(event.getPlayer().getUniqueId());
		String worldName = TownyAPI.getInstance().getTownyWorld(event.getPlayer().getWorld()).getFormattedUnclaimedZoneName();

		// Likely a Citizens NPC.
		if (resident == null || worldName == null)
			return;
		
		if (TownySettings.isNotificationUsingTitles() && resident.isSeeingBorderTitles() && event.getTo().getTownBlockOrNull() == null) {
			String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildTitle());
			String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildSubtitle());
			if (title.contains("{wilderness}")) {
				title = title.replace("{wilderness}", worldName);
			}
			if (subtitle.contains("{wilderness}")) {
				subtitle = subtitle.replace("{wilderness}", worldName);
			}
			if (title.contains("{townname}")) {
				subtitle = subtitle.replace("{townname}", StringMgmt.remUnderscore(event.getFrom().getTownOrNull().getName()));
			}
			if (subtitle.contains("{townname}")) {
				subtitle = subtitle.replace("{townname}", StringMgmt.remUnderscore(event.getFrom().getTownOrNull().getName()));
			}
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);
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
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());

		// More than likely another plugin using a fake player to run a command or,
		// the player is an admin/op.
		if (resident == null || resident.isAdmin())
			return;
		
		final String command = event.getMessage().split(" ")[0];
		
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
		if (town != null && blockedTownCommands.containsCommand(command) && blockedTouristCommands.containsCommand(command)) {
			// Allow own town & let globally welcomed players run commands.
			if (town.hasResident(resident) || resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_TOURIST_COMMAND_LIMITATION_BYPASS.getNode()))
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
				&& event.getPlayer().getInventory().getItemInMainHand().getType().name().equalsIgnoreCase(TownySettings.getTool()) 
				&& plugin.hasPlayerMode(event.getPlayer(), "infotool")
				&& TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer())
				&& event.getClickedBlock() != null) {
					Player player = event.getPlayer();
					Block block = event.getClickedBlock();
					
					if (Tag.SIGNS.isTagged(block.getType())) {
						BlockFace facing = null;
						if (block.getBlockData() instanceof Sign) {
							org.bukkit.block.data.type.Sign sign = (org.bukkit.block.data.type.Sign) block.getBlockData();
							facing = sign.getRotation();
						}
						if (block.getBlockData() instanceof WallSign)  { 
							org.bukkit.block.data.type.WallSign sign = (org.bukkit.block.data.type.WallSign) block.getBlockData();
							facing = sign.getFacing();	
						}
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Sign Info"),
								ChatTools.formatCommand("", "Sign Type", "", block.getType().name()),
								ChatTools.formatCommand("", "Facing", "", facing.toString())
								));
					} else if (Tag.DOORS.isTagged(block.getType())) {
						org.bukkit.block.data.type.Door door = (org.bukkit.block.data.type.Door) block.getBlockData();
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Door Info"),
								ChatTools.formatCommand("", "Door Type", "", block.getType().name()),
								ChatTools.formatCommand("", "hinged on ", "", String.valueOf(door.getHinge())),
								ChatTools.formatCommand("", "isOpen", "", String.valueOf(door.isOpen())),
								ChatTools.formatCommand("", "getFacing", "", door.getFacing().name())
								));
					} else {
						TownyMessaging.sendMessage(player, Arrays.asList(
								ChatTools.formatTitle("Block Info"),
								ChatTools.formatCommand("", "Material", "", block.getType().name()),								      
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
				&& event.getPlayer().getInventory().getItemInMainHand().getType().name().equalsIgnoreCase(TownySettings.getTool())
				&& plugin.hasPlayerMode(event.getPlayer(), "infotool")
				&& TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer())) {

				Entity entity = event.getRightClicked();

				TownyMessaging.sendMessage(event.getPlayer(), Arrays.asList(
						ChatTools.formatTitle("Entity Info"),
						ChatTools.formatCommand("", "Entity Class", "", entity.getType().getEntityClass().getSimpleName()),
						ChatTools.formatCommand("", "Entity Type", "", entity.getType().name() + " (" + entity.getType().getKey() + ")")
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
	
	private void loadBlockedCommandLists() {
		this.blockedJailCommands = new CommandList(TownySettings.getJailBlacklistedCommands());
		this.blockedTouristCommands = new CommandList(TownySettings.getTouristBlockedCommands());
		this.blockedTownCommands = new CommandList(TownySettings.getTownBlacklistedCommands());
		this.blockedOutlawCommands = new CommandList(TownySettings.getOutlawBlacklistedCommands());
		this.blockedWarCommands = new CommandList(TownySettings.getWarBlacklistedCommands());
		this.ownPlotLimitedCommands = new CommandList(TownySettings.getPlayerOwnedPlotLimitedCommands());
	}
}