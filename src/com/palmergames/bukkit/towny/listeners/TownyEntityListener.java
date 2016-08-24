package com.palmergames.bukkit.towny.listeners;

import java.util.Collections;
import java.util.List;

import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.potion.PotionEffect;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import com.palmergames.bukkit.util.ArraySort;

/**
 * 
 * @author ElgarL,Shade
 * 
 */
public class TownyEntityListener implements Listener {

	private final Towny plugin;

	public TownyEntityListener(Towny instance) {

		plugin = instance;
	}

	/**
	 * Prevent PvP and PvM damage dependent upon PvP settings and location.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Entity attacker = event.getDamager();

		// Not wartime
		if (!TownyUniverse.isWarTime()) {

			if (CombatUtil.preventDamageCall(plugin, attacker, event.getEntity())) {
				// Remove the projectile here so no
				// other events can fire to cause damage
				if (attacker instanceof Projectile)
					attacker.remove();

				event.setCancelled(true);
			}
		}

	}

	/**
	 * Prevent monsters from dropping blocks if within an arena plot.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) {

		if (plugin.isError()) {
			return;
		}

		Entity entity = event.getEntity();

		if (entity instanceof Monster) {

			Location loc = entity.getLocation();
			TownyWorld townyWorld = null;

			try {
				townyWorld = TownyUniverse.getDataSource().getWorld(loc.getWorld().getName());

				// remove drops from monster deaths if in an arena plot
				if (townyWorld.isUsingTowny()) {
					if (townyWorld.getTownBlock(Coord.parseCoord(loc)).getType() == TownBlockType.ARENA)
						event.getDrops().clear();
				}

			} catch (NotRegisteredException e) {
				// Unknown world or not in a town
			}
		}
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {

		if (plugin.isError()) {
				return;
		}

		TownyWorld townyWorld = null;
		
		Entity entity = event.getEntity();		
		
		if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Animals) {
			String damager = event.getDamager().getType().name();

			if (damager == "PRIMED_TNT" || damager == "WITHER_SKULL" || damager == "FIREBALL" || damager == "SMALL_FIREBALL" || damager == "LARGE_FIREBALL" || damager == "WITHER" || damager == "CREEPER") {
											
				try {
					townyWorld = TownyUniverse.getDataSource().getWorld(entity.getWorld().getName());
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (!locationCanExplode(townyWorld, entity.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}
			if (event.getDamager() instanceof Projectile) {
				
				try {
					townyWorld = TownyUniverse.getDataSource().getWorld(entity.getWorld().getName());
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			Object remover = event.getDamager();
			remover = ((Projectile) remover).getShooter();
				if (remover instanceof Monster) {
					event.setCancelled(true);	
				} else if (remover instanceof Player) {
					Player player = (Player) remover;
			
					// Get destroy permissions (updates if none exist)
					boolean bDestroy = PlayerCacheUtil.getCachePermission(player, entity.getLocation(), 416, (byte) 0, TownyPermission.ActionType.DESTROY);

					// Allow the removal if we are permitted
					if (bDestroy)
						return;

					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * Prevent potion damage on players in non PVP areas
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPotionSplashEvent(PotionSplashEvent event) {

		List<LivingEntity> affectedEntities = (List<LivingEntity>) event.getAffectedEntities();
		ThrownPotion potion = event.getPotion();
		Entity attacker;

		List<PotionEffect> effects = (List<PotionEffect>) potion.getEffects();
		boolean detrimental = false;

		/*
		 * List of potion effects blocked from PvP.
		 */
		List<String> prots = TownySettings.getPotionTypes();
		
		
		for (PotionEffect effect : effects) {

			/*
			 * Check to see if any of the potion effects are protected.
			 */
			
			if (prots.contains(effect.getType().getName())) {
				detrimental = true;
			}

		}

		Object source = potion.getShooter();

		if (!(source instanceof Entity)) {

			return;	// TODO: prevent damage from dispensers

		} else {

			attacker = (Entity) source;

		}

		// Not Wartime
		if (!TownyUniverse.isWarTime())
			for (LivingEntity defender : affectedEntities) {
				/*
				 * Don't block potion use on ourselves
				 * yet allow the use of beneficial potions on all.
				 */
				if (attacker != defender)
					if (CombatUtil.preventDamageCall(plugin, attacker, defender) && detrimental) {

						event.setIntensity(defender, -1.0);
					}
			}

	}

	/**
	 * 
	 * @param event
	 * @throws NotRegisteredException
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) throws NotRegisteredException {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (event.getEntity() instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity) event.getEntity();
			Location loc = event.getLocation();
			Coord coord = Coord.parseCoord(loc);
			TownyWorld townyWorld = null;

			try {
				townyWorld = TownyUniverse.getDataSource().getWorld(loc.getWorld().getName());
			} catch (NotRegisteredException e) {
				// Failed to fetch a world
				return;
			}

			// remove from world if set to remove mobs globally
			if (townyWorld.isUsingTowny())
				if (!townyWorld.hasWorldMobs() && ((MobRemovalTimerTask.isRemovingWorldEntity(livingEntity) || ((livingEntity instanceof Villager) && !((Villager) livingEntity).isAdult() && (TownySettings.isRemovingVillagerBabiesWorld()))))) {
					if (plugin.isCitizens2()) {
						if (!CitizensAPI.getNPCRegistry().isNPC(livingEntity)) {
							// TownyMessaging.sendDebugMsg("onCreatureSpawn world: Canceled "
							// + event.getEntityType().name() +
							// " from spawning within "+coord.toString()+".");
							event.setCancelled(true);
						}
					} else
						event.setCancelled(true);
				}

			if (!townyWorld.hasTownBlock(coord))
				return;
			
			TownBlock townBlock = townyWorld.getTownBlock(coord);
			try {
				
				if (townyWorld.isUsingTowny() && !townyWorld.isForceTownMobs()) {
					if (!townBlock.getTown().hasMobs() && !townBlock.getPermissions().mobs) {
						if ((MobRemovalTimerTask.isRemovingTownEntity(livingEntity) || ((livingEntity instanceof Villager) && !((Villager) livingEntity).isAdult() && (TownySettings.isRemovingVillagerBabiesTown())))) {
							if (plugin.isCitizens2()) {
								if (!CitizensAPI.getNPCRegistry().isNPC(livingEntity)) {
									// TownyMessaging.sendDebugMsg("onCreatureSpawn town: Canceled "
									// + event.getEntityType().name() +
									// " from spawning within "+coord.toString()+".");
									event.setCancelled(true);
								}
							} else
								event.setCancelled(true);
						}
					}
				}
			} catch (TownyException x) {
				
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityInteract(EntityInteractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();
		Entity entity = event.getEntity();
		Entity passenger = entity.getPassenger();

		TownyWorld World = null;

		try {
			World = TownyUniverse.getDataSource().getWorld(block.getLocation().getWorld().getName());
			if (!World.isUsingTowny())
				return;

		} catch (NotRegisteredException e) {
			// World not registered with Towny.
			e.printStackTrace();
			return;
		}

		try {
			TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(block.getLocation().getWorld().getName());

			if (townyWorld.isUsingTowny()) {

				// Prevent creatures trampling crops
				if (townyWorld.isDisableCreatureTrample()) {
					if ((block.getType() == Material.SOIL) || (block.getType() == Material.CROPS)) {
						if (entity instanceof Creature) {
							event.setCancelled(true);
							return;
						}
					}
				}

				/*
				 * Allow players in vehicles to activate pressure plates if they
				 * are permitted.
				 */
				if (passenger != null && passenger instanceof Player) {

					// PlayerInteractEvent newEvent = new
					// PlayerInteractEvent((Player)passenger, Action.PHYSICAL,
					// null, block, BlockFace.SELF);
					// Bukkit.getServer().getPluginManager().callEvent(newEvent);

					if (TownySettings.isSwitchMaterial(block.getType().name())) {
						if (!plugin.getPlayerListener().onPlayerSwitchEvent((Player) passenger, block, null, World))
							return;
					}

				}

				// System.out.println("EntityInteractEvent triggered for " +
				// entity.toString());

				// Prevent creatures triggering stone pressure plates
				if (TownySettings.isCreatureTriggeringPressurePlateDisabled()) {
					if (block.getType() == Material.STONE_PLATE) {
						if (entity instanceof Creature) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}

		} catch (NotRegisteredException e) {
			// Failed to fetch world
			e.printStackTrace();
		}

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		switch (event.getEntity().getType()) {

		case WITHER:

			try {
				TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(event.getBlock().getWorld().getName());

				if (!townyWorld.isUsingTowny())
					return;

				if (!locationCanExplode(townyWorld, event.getBlock().getLocation())) {
					event.setCancelled(true);
					return;
				}

			} catch (NotRegisteredException e) {
				// Failed to fetch world
			}
			break;

		case ENDERMAN:

			try {
				TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(event.getBlock().getWorld().getName());

				if (!townyWorld.isUsingTowny())
					return;

				if (townyWorld.isEndermanProtect())
					event.setCancelled(true);

			} catch (NotRegisteredException e) {
				// Failed to fetch world
			}
			break;

		default:

		}

	}

	/**
	 * Test if this location has explosions enabled.
	 * 
	 * @param world
	 * @param target
	 * @return true if allowed.
	 */
	public boolean locationCanExplode(TownyWorld world, Location target) {

		Coord coord = Coord.parseCoord(target);

		if (world.isWarZone(coord) && !TownyWarConfig.isAllowingExplosionsInWarZone()) {
			return false;
		}

		try {
			TownBlock townBlock = world.getTownBlock(coord);
			if (world.isUsingTowny() && !world.isForceExpl()) {
				if ((!townBlock.getPermissions().explosion) || (TownyUniverse.isWarTime() && TownySettings.isAllowWarBlockGriefing() && !townBlock.getTown().hasNation() && !townBlock.getTown().isBANG())) {
					return false;
				}
			}
		} catch (NotRegisteredException e) {
			return world.isExpl();
		}
		return true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		TownyWorld townyWorld;

		/**
		 * Perform this test outside the block loop so we only get the world
		 * once per explosion.
		 */
		try {
			townyWorld = TownyUniverse.getDataSource().getWorld(event.getLocation().getWorld().getName());

			if (!townyWorld.isUsingTowny())
				return;

		} catch (NotRegisteredException e) {
			// failed to get world so abort
			return;
		}

		Coord coord;
		List<Block> blocks = event.blockList();
		Entity entity = event.getEntity();
		int count = 0;

		// Sort blocks by height (lowest to highest).
		Collections.sort(blocks, ArraySort.getInstance());

		for (Block block : blocks) {
			coord = Coord.parseCoord(block.getLocation());
			count++;

			// Warzones
			if (townyWorld.isWarZone(coord)) {
				if (!TownyWarConfig.isAllowingExplosionsInWarZone()) {
					if (event.getEntity() != null)
						TownyMessaging.sendDebugMsg("onEntityExplode: Canceled " + event.getEntity().getEntityId() + " from exploding within " + coord.toString() + ".");
					event.setCancelled(true);
					return;
				} else {
					if (TownyWarConfig.explosionsBreakBlocksInWarZone()) {
						if (TownyWarConfig.regenBlocksAfterExplosionInWarZone()) {
							// ***********************************
							// TODO

							// On completion, remove TODO from config.yml
							// comments.

							/*
							 * if
							 * (!plugin.getTownyUniverse().hasProtectionRegenTask
							 * (new BlockLocation(block.getLocation()))) {
							 * ProtectionRegenTask task = new
							 * ProtectionRegenTask(plugin.getTownyUniverse(),
							 * block, false);
							 * task.setTaskId(plugin.getServer().getScheduler().
							 * scheduleSyncDelayedTask(plugin, task,
							 * ((TownySettings.getPlotManagementWildRegenDelay()
							 * + count)*20)));
							 * plugin.getTownyUniverse().addProtectionRegenTask
							 * (task ); }
							 */

							// TODO
							// ***********************************
						}

						// Break the block
					} else {
						event.blockList().remove(block);
					}
				}
				return;
			}

			try {
				TownBlock townBlock = townyWorld.getTownBlock(coord);

				// If explosions are off, or it's wartime and explosions are off
				// and the towns has no nation
				if (townyWorld.isUsingTowny() && !townyWorld.isForceExpl()) {
					if ((!townBlock.getPermissions().explosion) || (TownyUniverse.isWarTime() && TownySettings.isAllowWarBlockGriefing() && !townBlock.getTown().hasNation() && !townBlock.getTown().isBANG())) {
						if (event.getEntity() != null)
							TownyMessaging.sendDebugMsg("onEntityExplode: Canceled " + event.getEntity().getEntityId() + " from exploding within " + coord.toString() + ".");
						event.setCancelled(true);
						return;
					}
				}
			} catch (TownyException x) {
				// Wilderness explosion regeneration
				if (townyWorld.isUsingTowny())
					if (townyWorld.isExpl()) {
						if (townyWorld.isUsingPlotManagementWildRevert() && (entity != null)) {
							
							TownyMessaging.sendDebugMsg("onEntityExplode: Testing entity: " + entity.getType().getEntityClass().getSimpleName().toLowerCase() + " @ " + coord.toString() + ".");
							
							if (townyWorld.isProtectingExplosionEntity(entity)) {
								if ((!TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(block.getLocation()))) && (block.getType() != Material.TNT)) {
									ProtectionRegenTask task = new ProtectionRegenTask(plugin, block, false);
									task.setTaskId(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ((TownySettings.getPlotManagementWildRegenDelay() + count) * 20)));
									TownyRegenAPI.addProtectionRegenTask(task);
									event.setYield((float) 0.0);
									block.getDrops().clear();
								}
							}
						}
					} else {
						event.setCancelled(true);
						return;
					}
			}
		}
	}

	/**
	 * Prevent fire arrows and charges igniting players when PvP is disabled
	 * 
	 * Can also prevent tnt from destroying armorstands
	 * 
	 * @param event
	 * @throws NotRegisteredException 
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityCombustByEntityEvent(EntityCombustByEntityEvent event) throws NotRegisteredException {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Entity combuster = event.getCombuster();
		Entity defender = event.getEntity();
		LivingEntity attacker;
		if (combuster instanceof Projectile) {
			
			Object source = ((Projectile) combuster).getShooter();
			
			if (!(source instanceof LivingEntity)) {
				return; // TODO: prevent damage from dispensers
			} else {
				attacker = (LivingEntity) source;
			}

			// There is an attacker and Not war time.
			if ((attacker != null) && (!TownyUniverse.isWarTime())) {

				if (CombatUtil.preventDamageCall(plugin, attacker, defender)) {
					// Remove the projectile here so no
					// other events can fire to cause damage
					combuster.remove();
					event.setCancelled(true);
				}
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		TownyWorld townyWorld = null;
		String worldName = null;
		Entity hanging = event.getEntity();

		try {
			worldName = hanging.getWorld().getName();
			townyWorld = TownyUniverse.getDataSource().getWorld(worldName);

			if (!townyWorld.isUsingTowny())
				return;

		} catch (NotRegisteredException e1) {
			// Not a known Towny world.
			// event.setCancelled(true);
			return;
		}

		if (event instanceof HangingBreakByEntityEvent) {
			HangingBreakByEntityEvent evt = (HangingBreakByEntityEvent) event;

			Object remover = evt.getRemover();

			/*
			 * Check if this has a shooter.
			 */
			if (remover instanceof Projectile) {
				remover = ((Projectile) remover).getShooter();
			}

			if (remover instanceof Player) {
				Player player = (Player) remover;

				// Get destroy permissions (updates if none exist)
				boolean bDestroy = PlayerCacheUtil.getCachePermission(player, hanging.getLocation(), 321, (byte) 0, TownyPermission.ActionType.DESTROY);

				// Allow the removal if we are permitted
				if (bDestroy)
					return;

				/*
				 * Fetch the players cache
				 */
				PlayerCache cache = plugin.getCache(player);

				event.setCancelled(true);

				if (cache.hasBlockErrMsg())
					TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

			} else {

				// Explosions are blocked in this plot
				if (!locationCanExplode(townyWorld, hanging.getLocation()))
					event.setCancelled(true);
			}

		} else {

			switch (event.getCause()) {

			case EXPLOSION:

				if (!locationCanExplode(townyWorld, event.getEntity().getLocation()))
					event.setCancelled(true);
				break;

			default:

			}

		}

	}

	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		long start = System.currentTimeMillis();

		Player player = event.getPlayer();
		Entity hanging = event.getEntity();

		try {
			TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(hanging.getWorld().getName());

			if (!townyWorld.isUsingTowny())
				return;

			// Get build permissions (updates if none exist)
			boolean bBuild = PlayerCacheUtil.getCachePermission(player, hanging.getLocation(), 321, (byte) 0, TownyPermission.ActionType.BUILD);

			// Allow placing if we are permitted
			if (bBuild)
				return;

			/*
			 * Fetch the players cache
			 */
			PlayerCache cache = plugin.getCache(player);

			event.setCancelled(true);

			if (cache.hasBlockErrMsg())
				TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		TownyMessaging.sendDebugMsg("onHangingBreak took " + (System.currentTimeMillis() - start) + "ms (" + event.getEventName() + ", " + event.isCancelled() + ")");
	}

//	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//	public void onEntityDamageByBlock(EntityDamageByBlockEvent event) {
//		DamageCause type = event.getCause();
//		Location loc = event.getDamager().getLocation();
//		TownyWorld townyWorld = null;

//		if (type == DamageCause.BLOCK_EXPLOSION) {
//			try {
//				townyWorld = TownyUniverse.getDataSource().getWorld(loc.getWorld().getName());
//				if (!locationCanExplode(townyWorld, loc)) {
//					event.setCancelled(true);
//					return;
//				}
//			} catch (NotRegisteredException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
}