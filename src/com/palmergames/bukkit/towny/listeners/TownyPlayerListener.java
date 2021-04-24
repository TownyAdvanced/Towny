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
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.util.StringMgmt;

import com.palmergames.util.TimeMgmt;

import io.papermc.lib.PaperLib;
import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Handle events for all Player related events
 * Players deaths are handled both here and in the TownyEntityMonitorListener
 * 
 * @author Shade/ElgarL
 * 
 */
public class TownyPlayerListener implements Listener {

	private final Towny plugin;

	public TownyPlayerListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event) {

		Player player = event.getPlayer();

		if (plugin.isError()) {
			TownyMessaging.sendMessage(player, Colors.Rose + "[Towny Error] Locked in Safe mode!");
			return;
		}

		if (!player.isOnline()) {
			return;
		}

		// Test and kick any players with invalid names.
		if (player.getName().contains(" ")) {
			player.kickPlayer("Invalid name!");
			return;
		}

		// Perform login code in it's own thread to update Towny data.
		if (BukkitTools.scheduleSyncDelayedTask(new OnPlayerLogin(Towny.getPlugin(), player), 0L) == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule OnLogin.");
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {

		if (plugin.isError()) {
			return;
		}
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (resident != null) {
			resident.setLastOnline(System.currentTimeMillis());
			resident.clearModes();
			resident.save();

			if (TownyTimerHandler.isTeleportWarmupRunning()) {
				TownyAPI.getInstance().abortTeleportRequest(resident);
			}
		}

		plugin.deleteCache(event.getPlayer());
		TownyPerms.removeAttachment(event.getPlayer().getName());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (plugin.isError()) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!TownySettings.isTownRespawning()) {
			return;
		}
		
		// If respawn anchors have higher precedence than town spawns, use them instead.
		if (Towny.is116Plus() && event.isAnchorSpawn() && TownySettings.isRespawnAnchorHigherPrecedence()) {
			return;
		}
		
		Location respawn;
		respawn = TownyAPI.getInstance().getTownSpawnLocation(player);
		if (respawn == null) {
			// Town has not set respawn location. Using default.
			return;
		}
		// Check if only respawning in the same world as the town's spawn.
		if (TownySettings.isTownRespawningInOtherWorlds() && !player.getWorld().equals(respawn.getWorld()))
			return;
		
		// Bed spawn or town.
		if (TownySettings.getBedUse() && (player.getBedSpawnLocation() != null)) {
			event.setRespawnLocation(player.getBedSpawnLocation());
		} else {
			event.setRespawnLocation(respawn);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJailRespawn(PlayerRespawnEvent event) {

		if (plugin.isError() || !TownySettings.isTownRespawning()) {
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
		event.setCancelled(!TownyActionEventExecutor.canBuild(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getBucket()));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
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
			if (TownySettings.isItemUseMaterial(item.name()))
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
				 * Test stripping logs, dye-able signs, glass bottles,
				 * flint&steel on TNT and shears on beehomes
				 * 
				 * Treat interaction as a Destroy test.
				 */
				if ((ItemLists.AXES.contains(item.name()) && Tag.LOGS.isTagged(clickedMat)) || // This will also catched already stripped logs but it is cleaner than anything else.
					(ItemLists.DYES.contains(item.name()) && Tag.SIGNS.isTagged(clickedMat)) ||
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
				 * Test if we're about to spawn either entity. Uses build test.
				 */
				if (item == Material.ARMOR_STAND || item == Material.END_CRYSTAL) 
					event.setCancelled(!TownyActionEventExecutor.canBuild(player, clickedBlock.getRelative(event.getBlockFace()).getLocation(), item));

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
			if (TownySettings.isSwitchMaterial(clickedMat.name()) || event.getAction() == Action.PHYSICAL) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canSwitch(player, clickedBlock.getLocation(), clickedMat));
				return;
			}
			/*
			 * Test potted plants, redstone interactables, other blocks which 
			 * cause an interaction that could be considered destructive, or 
			 * something which wouldn't be given out like a normal 
			 * door/inventory permission. 
			 * 
			 * Test interaction as a Destroy test. (These used to be switches pre-0.96.3.1)
			 */
			if (ItemLists.POTTED_PLANTS.contains(clickedMat.name()) ||                          
				ItemLists.REDSTONE_INTERACTABLES.contains(clickedMat.name()) ||
				clickedMat == Material.BEACON || clickedMat == Material.DRAGON_EGG || 
				clickedMat == Material.COMMAND_BLOCK || clickedMat == Material.SWEET_BERRY_BUSH){
				
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
		if (event.hasBlock()) {
			/*
			 * Catches respawn anchors blowing up and allows us to track their explosions.
			 */
			if (block.getType().name().equals("RESPAWN_ANCHOR")) {
				org.bukkit.block.data.type.RespawnAnchor anchor = ((org.bukkit.block.data.type.RespawnAnchor) block.getBlockData());
				if (anchor.getCharges() == 4)
					BukkitTools.getPluginManager().callEvent(new BedExplodeEvent(event.getPlayer(), block.getLocation(), null, block.getType()));
				return;
			}
			
			/*
			 * Catches beds blowing up and allows us to track their explosions.
			 */
			if (Tag.BEDS.isTagged(block.getType()) && event.getPlayer().getWorld().getEnvironment().equals(Environment.NETHER)) {
				org.bukkit.block.data.type.Bed bed = ((org.bukkit.block.data.type.Bed) block.getBlockData());
				BukkitTools.getPluginManager().callEvent(new BedExplodeEvent(event.getPlayer(), block.getLocation(), block.getRelative(bed.getFacing()).getLocation(), block.getType()));
				return;
			}
			
			/*
			 * Prevents setting the spawn point of the player using beds, 
			 * except in allowed plots (personally-owned and Inns)
			 */
			if (Tag.BEDS.isTagged(block.getType()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (!TownySettings.getBedUse())
					return;

				boolean isOwner = false;
				boolean isInnPlot = false;

				if (!TownyAPI.getInstance().isWilderness(block.getLocation())) {
					
					TownBlock townblock = TownyAPI.getInstance().getTownBlock(block.getLocation());
					Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
					Town town = townblock.getTownOrNull();
					if (resident == null || town == null)
						return;
					
					isOwner = townblock.isOwner(resident);
					isInnPlot = townblock.getType() == TownBlockType.INN;
					
					//Prevent enemies and outlaws using the Inn plots.
					if (CombatUtil.isEnemyTownBlock(event.getPlayer(), townblock.getWorldCoord()) || town.hasOutlaw(resident)) {
						event.setCancelled(true);
						TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_no_sleep_in_enemy_inn"));
						return;
					}
				}
				if (!isOwner && !isInnPlot) {

					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_cant_use_bed"));

				}
			}
		}
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
			
			/*
			 * The following will get us a Material substituted in for an Entity so that we can run permission tests.
			 * Anything not in the switch will leave the block null.
			 */
			switch (event.getRightClicked().getType()) {
				/*
				 * First are tested with a Destroy perm check.
				 */
				case ITEM_FRAME:
				case PAINTING:
				case LEASH_HITCH:
				case MINECART_COMMAND:
				case MINECART_TNT:
				case MINECART_MOB_SPAWNER:
					mat = EntityTypeUtil.parseEntityToMaterial(event.getRightClicked().getType());
					break;
				/*
				 * These two block the dying of sheep and wolf's collars.
				 */
				case SHEEP:
				case WOLF:
					if (event.getPlayer().getInventory().getItem(event.getHand()) != null) {
						Material dye = event.getPlayer().getInventory().getItem(event.getHand()).getType();
						if (ItemLists.DYES.contains(dye.name())) {
							mat = dye;
							break;
						}
					}	
				/*
				 * Afterwards they will remain as Switch perm checks.
				 */
				case MINECART_CHEST:
				case MINECART_FURNACE:				
				case MINECART_HOPPER:
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
				if (TownySettings.isSwitchMaterial(mat.name()) && actionType == ActionType.SWITCH) {
					//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
					event.setCancelled(!TownyActionEventExecutor.canSwitch(player, event.getRightClicked().getLocation(), mat));
					return;
				} 
			}
			
			/*
			 * Handle things which need an item in hand.
			 */
			if (event.getPlayer().getInventory().getItem(event.getHand()) != null) {
				Material item = event.getPlayer().getInventory().getItem(event.getHand()).getType();

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
				if (TownySettings.isItemUseMaterial(item.name())) {
					//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
					event.setCancelled(!TownyActionEventExecutor.canItemuse(player, event.getRightClicked().getLocation(), item));
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		/*
		 * Abort if we havn't really moved
		 */
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
			return;
		}

		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from;
		PlayerCache cache = plugin.getCache(player);
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident != null
				&& TownyTimerHandler.isTeleportWarmupRunning()				 
				&& TownySettings.getTeleportWarmupTime() > 0 
				&& TownySettings.isMovementCancellingSpawnWarmup() 
				&& !townyUniverse.getPermissionSource().isTownyAdmin(player) 
				&& resident.getTeleportRequestTime() > 0) {
			TeleportWarmupTimerTask.abortTeleportRequest(resident);
			TownyMessaging.sendMsg(resident, ChatColor.RED + Translation.of("msg_err_teleport_cancelled"));
		}

		try {
			from = cache.getLastLocation();
		} catch (NullPointerException e) {
			from = event.getFrom();
		}
		
		if (WorldCoord.cellChanged(from, to)) {
			try {
				TownyWorld fromWorld = townyUniverse.getDataSource().getWorld(from.getWorld().getName());
				WorldCoord fromCoord = new WorldCoord(fromWorld.getName(), Coord.parseCoord(from));
				TownyWorld toWorld = townyUniverse.getDataSource().getWorld(to.getWorld().getName());
				WorldCoord toCoord = new WorldCoord(toWorld.getName(), Coord.parseCoord(to));
				
				onPlayerMoveChunk(player, fromCoord, toCoord, from, to, event);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
		}

		// Update the cached players current location
		cache.setLastLocation(to);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {

		if (plugin.isError()) {
			// Citizens stores their NPCs at the world spawn and when players load chunks the NPC is teleported there. 
			// Towny was preventing them being teleported and causing NPCs to be at a world spawn, even after the Safe Mode was cleaned up. 
			if (plugin.isCitizens2() && CitizensAPI.getNPCRegistry().isNPC(event.getPlayer()))
				return;
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		// Cancel teleport if Jailed by Towny.
		if (resident != null && resident.isJailed()) {
			if ((event.getCause() == TeleportCause.COMMAND)) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
				return;
			}
			if (event.getCause() == TeleportCause.PLUGIN)
				return;
			if ((event.getCause() != TeleportCause.ENDER_PEARL) || (!TownySettings.JailAllowsEnderPearls())) {
				TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_err_jailed_players_no_teleport"));
				event.setCancelled(true);
			}
		}
		

		/*
		 * Test to see if CHORUS_FRUIT is in the item_use list.
		 */
		if (event.getCause() == TeleportCause.CHORUS_FRUIT && TownySettings.isItemUseMaterial(Material.CHORUS_FRUIT.name())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.CHORUS_FRUIT)) {
				event.setCancelled(true);
				return;
			}
		}	
			
		/*
		 * Test to see if Ender pearls are disabled.
		 */		
		if (event.getCause() == TeleportCause.ENDER_PEARL && TownySettings.isItemUseMaterial(Material.ENDER_PEARL.name())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canItemuse(event.getPlayer(), event.getTo(), Material.ENDER_PEARL)) {
				event.setCancelled(true);
				return;
			}
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
				TownyWorld world = null;
				try {
					world = TownyUniverse.getInstance().getDataSource().getWorld(event.getCaught().getWorld().getName());
				} catch (NotRegisteredException ignored) {}
				assert world != null;
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
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/*
	* PlayerChangePlotEvent that can fire the PlayerLeaveTownEvent and PlayerEnterTownEvent
	*/
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChangePlotEvent(PlayerChangePlotEvent event) {

		PlayerMoveEvent pme = event.getMoveEvent();
		Player player = event.getPlayer();		
		WorldCoord from = event.getFrom();
		WorldCoord to = event.getTo();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		if (resident == null)
			return;
		
		try {
			try {
				to.getTownBlock();
				if (to.getTownBlock().hasTown()) { 
					try {
						Town fromTown = from.getTownBlock().getTown();
						if (!to.getTownBlock().getTown().equals(fromTown)){
							Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterTownEvent(player,to,from,to.getTownBlock().getTown(), pme)); // From Town into different Town.
							Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from,from.getTownBlock().getTown(), pme));//
						}
						// Both are the same town, do nothing, no Event should fire here.
					} catch (NotRegisteredException e) { // From Wilderness into Town.
						Bukkit.getServer().getPluginManager().callEvent(new PlayerEnterTownEvent(player,to, from, to.getTownBlock().getTown(), pme));
					}
				} else {
					if (from.getTownBlock().hasTown() && !(to.getTownBlock().hasTown())){ // From has a town, to doesn't so: From Town into Wilderness
						Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from, from.getTownBlock().getTown(), pme));
					}
				}
			} catch (NotRegisteredException e) {
				Bukkit.getServer().getPluginManager().callEvent(new PlayerLeaveTownEvent(player,to,from, from.getTownBlock().getTown(), pme));
			}

		} catch (NotRegisteredException e) {
			// If not registered, both to and from locations are wilderness.	
		}		
	}
	
	/*
	 * onOutlawEnterTown
	 * - Handles outlaws entering a town they are outlawed in.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onOutlawEnterTown(PlayerEnterTownEvent event) {

		Resident outlaw = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		
		if (outlaw == null)
			return;

		boolean hasBypassNode = TownyUniverse.getInstance().getPermissionSource().testPermission(outlaw.getPlayer(), PermissionNodes.TOWNY_ADMIN_OUTLAW_TELEPORT_BYPASS.getNode());
		
		Town town = event.getEnteredtown();
		
		if (town.hasOutlaw(outlaw)) {
			// Admins are omitted so towns won't be informed an admin might be spying on them.
			if (TownySettings.doTownsGetWarnedOnOutlaw() && !hasBypassNode) {
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_outlaw_town_notify", outlaw.getFormattedName()));
			}
			// If outlaws can enter towns OR the outlaw has towny.admin.outlaw.teleport_bypass perm, player is warned but not teleported.
			if (TownySettings.canOutlawsEnterTowns() || hasBypassNode) {
				TownyMessaging.sendMsg(outlaw, Translation.of("msg_you_are_an_outlaw_in_this_town", town));
			} else {
				if (TownySettings.getOutlawTeleportWarmup() > 0) {
					TownyMessaging.sendMsg(outlaw, Translation.of("msg_outlaw_kick_cooldown", town, TimeMgmt.formatCountdownTime(TownySettings.getOutlawTeleportWarmup())));
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						if (TownyAPI.getInstance().getTown(outlaw.getPlayer().getLocation()) != null && TownyAPI.getInstance().getTown(outlaw.getPlayer().getLocation()) == town && town.hasOutlaw(outlaw.getPlayer().getName())) {
							Location spawnLocation = town.getWorld().getSpawnLocation();
							Player outlawedPlayer = outlaw.getPlayer();
							if (!TownySettings.getOutlawTeleportWorld().equals("")) {
								spawnLocation = Objects.requireNonNull(Bukkit.getWorld(TownySettings.getOutlawTeleportWorld())).getSpawnLocation();
							}
							// sets tp location to their bedspawn only if it isn't in the town they're being teleported from.
							if ((outlawedPlayer.getBedSpawnLocation() != null) && (TownyAPI.getInstance().getTown(outlawedPlayer.getBedSpawnLocation()) != town))
								spawnLocation = outlawedPlayer.getBedSpawnLocation();
							if (outlaw.hasTown() && TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer) != null)
								spawnLocation = TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer);
							TownyMessaging.sendMsg(outlaw, Translation.of("msg_outlaw_kicked", town));
							PaperLib.teleportAsync(outlaw.getPlayer(), spawnLocation, TeleportCause.PLUGIN);
						}
					}
				}.runTaskLater(plugin, TownySettings.getOutlawTeleportWarmup() * 20);
			}
		}
	}


	/**
	 * onPlayerDieInTown
	 * - Handles death events and the KeepInventory/KeepLevel options are being used.
	 * 
	 * @author - Articdive
	 * @param event - PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	// Why Highest??, so that we are the last ones to check for if it keeps their inventory, and then have no problems with it.
	public void onPlayerDieInTown(PlayerDeathEvent event) {
		boolean keepInventory = event.getKeepInventory();
		boolean keepLevel = event.getKeepLevel();
		Player player = event.getEntity();
		Location deathloc = player.getLocation();
		TownBlock tb = TownyAPI.getInstance().getTownBlock(deathloc);
		if (tb != null && tb.hasTown()) {
			if (TownySettings.getKeepExperienceInTowns() && !keepLevel) {
				event.setKeepLevel(true);
				event.setDroppedExp(0);
			}

			if (TownySettings.getKeepInventoryInTowns() && !keepInventory) {
				event.setKeepInventory(true);
				event.getDrops().clear();
				keepInventory = true;
			}

			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null && resident.hasTown() && !keepInventory) {
				Town town = resident.getTownOrNull();
				Town tbTown = tb.getTownOrNull();
				if (TownySettings.getKeepInventoryInOwnTown() && tbTown.equals(town)) {
					event.setKeepInventory(true);
					event.getDrops().clear();
					keepInventory = true;
				}
				if (TownySettings.getKeepInventoryInAlliedTowns() && !keepInventory && tbTown.isAlliedWith(town)) {
					event.setKeepInventory(true);
					event.getDrops().clear();
					keepInventory = true;
				}
			}

			if (TownySettings.getKeepInventoryInArenas() && !keepInventory && tb.getType() == TownBlockType.ARENA) {
				event.setKeepInventory(true);
				event.getDrops().clear();
			}
		}
	}


	/**
	 * PlayerEnterTownEvent
	 * Currently used for:
	 *   - showing NotificationsUsingTitles upon entering a town.
	 *   
	 * @param event - PlayerEnterTownEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerEnterTown(PlayerEnterTownEvent event) {
		
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());
		Town town = event.getEnteredtown();
		
		if (resident != null && town != null && TownySettings.isNotificationUsingTitles()) {
			String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownTitle());
			String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesTownSubtitle());
			
			HashMap<String, Object> placeholders = new HashMap<>();
			placeholders.put("{townname}", StringMgmt.remUnderscore(town.getName()));
			placeholders.put("{town_motd}", town.getBoard());
			placeholders.put("{town_residents}", town.getNumResidents());
			placeholders.put("{town_residents_online}", TownyAPI.getInstance().getOnlinePlayers(town).size());

			for(Map.Entry<String, Object> placeholder: placeholders.entrySet()) {
				title = title.replace(placeholder.getKey(), placeholder.getValue().toString());
				subtitle = subtitle.replace(placeholder.getKey(), placeholder.getValue().toString());
			}
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);
		}
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
	public void onPlayerLeaveTown(PlayerLeaveTownEvent event) {
		Resident resident = TownyAPI.getInstance().getResident(event.getPlayer().getUniqueId());
		String worldName = TownyAPI.getInstance().getTownyWorld(event.getPlayer().getWorld().getName()).getUnclaimedZoneName();

		// Likely a Citizens NPC.
		if (resident == null || worldName == null)
			return;
		
		if (TownySettings.isNotificationUsingTitles() && event.getTo().getTownBlockOrNull() == null) {
			String title = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildTitle());
			String subtitle = ChatColor.translateAlternateColorCodes('&', TownySettings.getNotificationTitlesWildSubtitle());
			if (title.contains("{wilderness}")) {
				title = title.replace("{wilderness}", StringMgmt.remUnderscore(worldName));
			}
			if (subtitle.contains("{wilderness}")) {
				subtitle = subtitle.replace("{wilderness}", StringMgmt.remUnderscore(worldName));
			}
			TownyMessaging.sendTitleMessageToResident(resident, title, subtitle);		
		}

		if (resident.isJailed())
			JailUtil.unJailResident(resident, UnJailReason.LEFT_TOWN);
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
	public void onJailedPlayerUsesCommand(PlayerCommandPreprocessEvent event) {
		if (plugin.isError()) {
			return;
		}
		Resident resident = TownyUniverse.getInstance().getResident(event.getPlayer().getUniqueId());

		// More than likely another plugin using a fake player to run a command. 
		if (resident == null || !resident.isJailed())
			return;
				
		String[] split = event.getMessage().substring(1).split(" ");
		if (TownySettings.getJailBlacklistedCommands().contains(split[0])) {
			TownyMessaging.sendErrorMsg(event.getPlayer(), Translation.of("msg_you_cannot_use_that_command_while_jailed"));
			event.setCancelled(true);
		}
	}
	
	/** 
	 * Allows restricting commands while being in a town.
	 * Also allows limiting commands to personally-owned plots only.
	 * Works almost the same way as jail command blacklisting, except has more stuff
	 * @param event PlayerCommandPreprocessEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerUsesCommandInsideTown(PlayerCommandPreprocessEvent event) {
		if (plugin.isError() 
			|| !TownySettings.allowTownCommandBlacklisting() 
			|| !TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		Player player = event.getPlayer();
		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (res == null)
			return;
		
		String command = event.getMessage().substring(1).split(" ")[0];

		/*
		 * Commands are sometimes blocked from being run by outsiders on an town.
		 */
		if (TownySettings.getTownBlacklistedCommands().contains(command) && TownySettings.getTouristBlockedCommands().contains(command)) {
			// Let admins and globally welcomed players run commands.
			if (TownyUniverse.getInstance().getPermissionSource().has(player, PermissionNodes.TOWNY_ADMIN_TOWN_COMMAND_BLACKLIST_BYPASS.getNode())
					|| TownyUniverse.getInstance().getPermissionSource().has(player, PermissionNodes.TOWNY_ADMIN_TOURIST_COMMAND_LIMITATION_BYPASS.getNode()))
							return;
			
			Town town = TownyAPI.getInstance().getTown(player.getLocation());
			if (town == null)
				return;
			
			// Allow for wilderness
			if (TownyAPI.getInstance().isWilderness(player.getLocation()))
				return;
			
			// Allow own town
			if (town.hasResident(res)) {
				return;
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_command_outsider_blocked", town.getName()));
			    event.setCancelled(true);
			    return;
			}
		}
		
		/*
		 * Commands are sometimes blocked from being run inside any town.
		 */
		if (TownySettings.getTownBlacklistedCommands().contains(command) && !TownyAPI.getInstance().isWilderness(player.getLocation())) {
			// Let admins run commands.
			if (TownyUniverse.getInstance().getPermissionSource().has(player, PermissionNodes.TOWNY_ADMIN_TOWN_COMMAND_BLACKLIST_BYPASS.getNode()))
				return;
			
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_command_blocked_inside_towns"));
			event.setCancelled(true);
			return;
		}

		/*
		 * Commands are sometimes limited to only plots that players personally own.
		 */
		if (TownySettings.getPlayerOwnedPlotLimitedCommands().contains(command)) {
			// Let admins run commands.
			if (TownyUniverse.getInstance().getPermissionSource().has(player, PermissionNodes.TOWNY_ADMIN_TOWN_COMMAND_BLACKLIST_BYPASS.getNode()))
				return;
			
			// Stop the command being run because this is in the wilderness.
			if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_command_limited"));
				event.setCancelled(true);
				return;
			}
			
			TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
			Town town = TownyAPI.getInstance().getTown(player.getLocation());
			if (tb == null || town == null)
				return;

			// If the player is in their own town and has towny.claimed.owntown.build.dirt 
			// (or more likely towny.claimed.owntown.*) then allow them to use the command.
			// It is likely a mayor/assistant.
			if (town.hasResident(res) && TownyUniverse.getInstance().getPermissionSource().hasOwnTownOverride(player, Material.DIRT, ActionType.BUILD))
				return;
			
			if (tb.hasResident()) {
				// This is a personally-owned plot, let's make sure the player is the plot owner.
				
				Resident owner = tb.getResidentOrNull();
			
				// The owner and player are not the same, cancel the command.
				if (!owner.getName().equals(player.getName())) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_command_limited"));
					event.setCancelled(true);
				}
				// Player owns this plot personally, we won't limit their command-usage.
			} else {
				// This is not a personally-owned plot, but let's only block this command if the player isn't special to the town.
				
				// If the player is in their own town and has towny.claimed.townowned.build.dirt 
				// (or more likely towny.claimed.townowned.*) then allow them to use the command.
				// It is likely a assistant or town-ranked player. 
				if (town.hasResident(res) && TownyUniverse.getInstance().getPermissionSource().hasTownOwnedOverride(player, Material.DIRT, ActionType.BUILD)) {
					return;
					
				// Not a special person, and not in a personally-owned plot, cancel this command.
				} else { 
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_command_limited"));
					event.setCancelled(true);
				}
			}
		}

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

		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;
		
		if (event.hasItem()
				&& event.getPlayer().getInventory().getItemInMainHand().getType() == Material.getMaterial(TownySettings.getTool()) 
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
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getPlayer().getWorld()))
			return;

		if (event.getRightClicked() != null
				&& event.getPlayer().getInventory().getItemInMainHand() != null
				&& event.getPlayer().getInventory().getItemInMainHand().getType() == Material.getMaterial(TownySettings.getTool())
				&& TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(event.getPlayer())) {
				if (event.getHand().equals(EquipmentSlot.OFF_HAND))
					return;

				Entity entity = event.getRightClicked();

				TownyMessaging.sendMessage(event.getPlayer(), Arrays.asList(
						ChatTools.formatTitle("Entity Info"),
						ChatTools.formatCommand("", "Entity Class", "", entity.getType().getEntityClass().getSimpleName())
						));

				event.setCancelled(true);
		}
	}
	
	
}
