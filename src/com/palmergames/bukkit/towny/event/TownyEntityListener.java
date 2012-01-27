package com.palmergames.bukkit.towny.event;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EndermanPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.painting.PaintingBreakEvent;
import org.bukkit.event.painting.PaintingPlaceEvent;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.PlayerCache;
import com.palmergames.bukkit.towny.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.BlockLocation;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.townywar.TownyWarConfig;
import com.palmergames.bukkit.util.ArraySort;

public class TownyEntityListener implements Listener {

	private final Towny plugin;

	public TownyEntityListener(Towny instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageEvent event) {

		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		long start = System.currentTimeMillis();

		Entity attacker = null;
		Entity defender = null;

		if (event instanceof EntityDamageByEntityEvent) {
			//plugin.sendMsg("EntityDamageByEntityEvent");
			EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
			if (entityEvent.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) entityEvent.getDamager();
				attacker = projectile.getShooter();
				defender = entityEvent.getEntity();
			} else {
				attacker = entityEvent.getDamager();
				defender = entityEvent.getEntity();
			}
		}

		if (attacker != null) {
			//plugin.sendMsg("Attacker not null");

			TownyUniverse universe = plugin.getTownyUniverse();
			try {
				TownyWorld world = TownyUniverse.getDataSource().getWorld(defender.getWorld().getName());

				// Wartime
				if (universe.isWarTime()) {
					event.setCancelled(false);
					throw new Exception();
				}

				Player a = null;
				Player b = null;

				if (attacker instanceof Player)
					a = (Player) attacker;
				if (defender instanceof Player)
					b = (Player) defender;

				if (preventDamageCall(world, attacker, defender, a, b))
					event.setCancelled(true);

			} catch (Exception e) {
			}

			TownyMessaging.sendDebugMsg("onEntityDamagedByEntity took " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
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

				//remove drops from monster deaths if in an arena plot           
				if (townyWorld.isUsingTowny()) {
					if (townyWorld.getTownBlock(Coord.parseCoord(loc)).getType() == TownBlockType.ARENA)
						event.getDrops().clear();
				}

			} catch (NotRegisteredException e) {
				// Unknown world or not in a town
			}
		}
	}
	
	/**
	 * Prevent potion damage on players in non PVP areas
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		
		List<LivingEntity> affectedEntities = (List<LivingEntity>) event.getAffectedEntities();
		ThrownPotion potion = event.getPotion();
		
		Entity attacker = potion.getShooter();
		
		Player a = null;
		if (attacker instanceof Player)
			a = (Player) attacker;
		
		TownyUniverse universe = plugin.getTownyUniverse();
		
		try {
			TownyWorld world = TownyUniverse.getDataSource().getWorld(potion.getWorld().getName());
			
			for (LivingEntity defender : affectedEntities) {
				
				try {
					// Wartime
					if (universe.isWarTime()) {
						event.setCancelled(false);
						throw new Exception();
					}
					
					Player b = null;
					
					if (defender instanceof Player)
						b = (Player) defender;

					if (preventDamageCall(world, attacker, defender, a, b))
						event.setIntensity(defender, -1.0);

				} catch (Exception e) {
					//do nothing as this is war.
				}

			}
		} catch (NotRegisteredException e1) {
			// Not a registered world
		}
		
		
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		
		if (event.isCancelled() || plugin.isError()) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//remove from world if set to remove mobs globally
			if (townyWorld.isUsingTowny())
				if (!townyWorld.hasWorldMobs() && MobRemovalTimerTask.isRemovingWorldEntity(livingEntity)) {
					//TownyMessaging.sendDebugMsg("onCreatureSpawn world: Canceled " + event.getCreatureType() + " from spawning within "+coord.toString()+".");
					event.setCancelled(true);
				}

			//remove from towns if in the list and set to remove            
			try {
				TownBlock townBlock = townyWorld.getTownBlock(coord);
				if (townyWorld.isUsingTowny() && !townyWorld.isForceTownMobs()) {
					if (!townBlock.getTown().hasMobs() && !townBlock.getPermissions().mobs) {
						if (MobRemovalTimerTask.isRemovingTownEntity(livingEntity)) {
							//TownyMessaging.sendDebugMsg("onCreatureSpawn town: Canceled " + event.getCreatureType() + " from spawning within "+coord.toString()+".");
							event.setCancelled(true);
						}
					}
				}
			} catch (TownyException x) {
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event) {

		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();
		Entity entity = event.getEntity();

		try {
			TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(block.getLocation().getWorld().getName());

			// Prevent creatures trampling crops
			if ((townyWorld.isUsingTowny()) && (townyWorld.isDisableCreatureTrample())) {
				if ((block.getType() == Material.SOIL) || (block.getType() == Material.CROPS)) {
					if (entity instanceof Creature)
						event.setCancelled(true);
					return;
				}
			}

		} catch (NotRegisteredException e) {
			// Failed to fetch world
			e.printStackTrace();
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEndermanPickup(EndermanPickupEvent event) {
		
		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();

		TownyWorld townyWorld = null;
		TownBlock townBlock;

		try {
			townyWorld = TownyUniverse.getDataSource().getWorld(block.getLocation().getWorld().getName());
			
			if (!townyWorld.isUsingTowny())
				return;
			
			if (townyWorld.isEndermanProtect())
				try {
					townBlock = townyWorld.getTownBlock(new Coord(Coord.parseCoord(block)));
					if (!townyWorld.isForceTownMobs() && !townBlock.getPermissions().mobs && !townBlock.getTown().hasMobs())
						event.setCancelled(true);
				} catch (NotRegisteredException e) {
					// not in a townblock
					event.setCancelled(true);
				}
		} catch (NotRegisteredException e) {
			// Failed to fetch world
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEndermanPlace(EndermanPlaceEvent event) {
		
		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		TownyWorld townyWorld = null;
		TownBlock townBlock;

		try {
			townyWorld = TownyUniverse.getDataSource().getWorld(event.getLocation().getWorld().getName());
			
			if (!townyWorld.isUsingTowny())
				return;
			
			if (townyWorld.isEndermanProtect())
				try {
					townBlock = townyWorld.getTownBlock(new Coord(Coord.parseCoord(event.getLocation())));
					if (!townyWorld.isForceTownMobs() && !townBlock.getPermissions().mobs && !townBlock.getTown().hasMobs())
						event.setCancelled(true);
				} catch (NotRegisteredException e) {
					// not in a townblock
					event.setCancelled(true);
				}
		} catch (NotRegisteredException e) {
			// Failed to fetch world
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		
		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Location loc;
		Coord coord;
		List<Block> blocks = event.blockList();
		Entity entity = event.getEntity();
		int count = 0;

		Collections.sort(blocks, ArraySort.getInstance());

		for (Block block : blocks) {
			loc = block.getLocation();
			coord = Coord.parseCoord(loc);
			count++;
			TownyWorld townyWorld;

			try {
				townyWorld = TownyUniverse.getDataSource().getWorld(loc.getWorld().getName());
				
				if (!townyWorld.isUsingTowny())
					return;
				
			} catch (NotRegisteredException e) {
				// failed to get world so abort
				return;
			}

			// Warzones
			if (townyWorld.isWarZone(coord)) {
				if (!TownyWarConfig.isAllowingExplosionsInWarZone()) {
					if (event.getEntity() != null)
						TownyMessaging.sendDebugMsg("onEntityExplode: Canceled " + event.getEntity().getEntityId() + " from exploding within " + coord.toString() + ".");
					event.setCancelled(true);
					break;
				} else {
					if (TownyWarConfig.explosionsBreakBlocksInWarZone()) {
						if (TownyWarConfig.regenBlocksAfterExplosionInWarZone()) {
							// ***********************************
							// TODO

							// On completion, remove TODO from config.yml comments.

							/*
							if (!plugin.getTownyUniverse().hasProtectionRegenTask(new BlockLocation(block.getLocation()))) {
								ProtectionRegenTask task = new ProtectionRegenTask(plugin.getTownyUniverse(), block, false);
								task.setTaskId(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ((TownySettings.getPlotManagementWildRegenDelay() + count)*20)));
								plugin.getTownyUniverse().addProtectionRegenTask(task);
							}
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

			//TODO: expand to protect neutrals during a war
			try {
				TownBlock townBlock = townyWorld.getTownBlock(coord);

				// If explosions are off, or it's wartime and explosions are off and the towns has no nation
				if (townyWorld.isUsingTowny() && !townyWorld.isForceExpl()) {
					if ((!townBlock.getPermissions().explosion) || (plugin.getTownyUniverse().isWarTime() && TownySettings.isAllowWarBlockGriefing() && !townBlock.getTown().hasNation() && !townBlock.getTown().isBANG())) {
						if (event.getEntity() != null)
							TownyMessaging.sendDebugMsg("onEntityExplode: Canceled " + event.getEntity().getEntityId() + " from exploding within " + coord.toString() + ".");
						event.setCancelled(true);
					}
				}
			} catch (TownyException x) {
				// Wilderness explosion regeneration
				if (townyWorld.isUsingTowny())
					if (townyWorld.isExpl()) {
						if (townyWorld.isUsingPlotManagementWildRevert()) {
							if (entity instanceof Creature) {
								if (!plugin.getTownyUniverse().hasProtectionRegenTask(new BlockLocation(block.getLocation()))) {
									ProtectionRegenTask task = new ProtectionRegenTask(plugin.getTownyUniverse(), block, false);
									task.setTaskId(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, ((TownySettings.getPlotManagementWildRegenDelay() + count) * 20)));
									plugin.getTownyUniverse().addProtectionRegenTask(task);
									event.setYield((float) 0.0);
								}
							}
						}
					} else
						event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPaintingBreak(PaintingBreakEvent event) {
		
		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		long start = System.currentTimeMillis();

		if (event instanceof PaintingBreakByEntityEvent) {
			PaintingBreakByEntityEvent evt = (PaintingBreakByEntityEvent) event;
			Painting painting = evt.getPainting();
			Object remover = evt.getRemover();
			WorldCoord worldCoord;
			
			try {
				TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(painting.getWorld().getName());
					
				if (!townyWorld.isUsingTowny())
					return;
				
				worldCoord = new WorldCoord(townyWorld, Coord.parseCoord(painting.getLocation()));
			} catch (NotRegisteredException e1) {
				//TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
				event.setCancelled(true);
				return;
			}
			
			if (remover instanceof Player) {
				Player player = (Player) evt.getRemover();

				//Get destroy permissions (updates if none exist)
				boolean bDestroy = TownyUniverse.getCachePermissions().getCachePermission(player, painting.getLocation(), TownyPermission.ActionType.DESTROY);

				PlayerCache cache = plugin.getCache(player);
				cache.updateCoord(worldCoord);
				TownBlockStatus status = cache.getStatus();
				if (status == TownBlockStatus.UNCLAIMED_ZONE && TownyUniverse.getPermissionSource().hasWildOverride(worldCoord.getWorld(), player, painting.getEntityId(), TownyPermission.ActionType.DESTROY))
					return;
				if (!bDestroy)
					event.setCancelled(true);
				if (cache.hasBlockErrMsg())
					TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());
				
			} else if ((remover instanceof Fireball) || (remover instanceof LightningStrike)) {
				
				try {
					TownBlock townBlock = worldCoord.getTownBlock();
					
					// Explosions are blocked in this plot
					if ((!townBlock.getPermissions().explosion) && (!townBlock.getWorld().isForceExpl()))
						event.setCancelled(true);					
					
				} catch (NotRegisteredException e) {
					// Not in a town
					if ((!worldCoord.getWorld().isExpl()) && (!worldCoord.getWorld().isForceExpl()))
						event.setCancelled(true);
				}

			}

		}

		TownyMessaging.sendDebugMsg("onPaintingBreak took " + (System.currentTimeMillis() - start) + "ms (" + event.getCause().name() + ", " + event.isCancelled() + ")");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPaintingPlace(PaintingPlaceEvent event) {

		if (event.isCancelled() || plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		long start = System.currentTimeMillis();

		Player player = event.getPlayer();
		Painting painting = event.getPainting();

		WorldCoord worldCoord;
		try {
			TownyWorld townyWorld = TownyUniverse.getDataSource().getWorld(painting.getWorld().getName());
			
			if (!townyWorld.isUsingTowny())
				return;
			
			worldCoord = new WorldCoord(townyWorld, Coord.parseCoord(painting.getLocation()));
		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		//Get build permissions (updates if none exist)
		boolean bBuild = TownyUniverse.getCachePermissions().getCachePermission(player, painting.getLocation(), TownyPermission.ActionType.BUILD);

		PlayerCache cache = plugin.getCache(player);
		TownBlockStatus status = cache.getStatus();

		if (status == TownBlockStatus.UNCLAIMED_ZONE && TownyUniverse.getPermissionSource().hasWildOverride(worldCoord.getWorld(), player, painting.getEntityId(), TownyPermission.ActionType.BUILD))
			return;

		if (!bBuild)
			event.setCancelled(true);

		if (cache.hasBlockErrMsg())
			TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

		TownyMessaging.sendDebugMsg("onPaintingBreak took " + (System.currentTimeMillis() - start) + "ms (" + event.getEventName() + ", " + event.isCancelled() + ")");
	}

	public boolean preventDamageCall(TownyWorld world, Entity a, Entity b, Player ap, Player bp) {
		// World using Towny
		if (!world.isUsingTowny())
			return false;

		Coord coord = Coord.parseCoord(b);

		if (ap != null && bp != null) {
			if (world.isWarZone(coord))
				return false;

			if (preventFriendlyFire(ap, bp)) // (preventDamagePvP(world, ap, bp) || 
				return true;
		}

		try {
			// Check Town PvP status
			TownBlock townblock = world.getTownBlock(coord);
			if (!townblock.getTown().isPVP() && !world.isForcePVP() && !townblock.getPermissions().pvp) {
				if (bp != null && (ap != null || a instanceof Arrow || a instanceof ThrownPotion))
					return true;

				if (b instanceof Wolf) {
					Wolf wolf = (Wolf) b;
					if (wolf.isTamed() && !wolf.getOwner().equals((AnimalTamer) a)) {
						return true;
					}
				}

				if (b instanceof Animals) {
					Resident resident = TownyUniverse.getDataSource().getResident(ap.getName());
					if ((!resident.hasTown()) || (resident.hasTown() && (resident.getTown() != townblock.getTown())))
						return true;
				}
			}
		} catch (NotRegisteredException e) {
			// Not in a town
			if ((a instanceof Player) && (b instanceof Player) && (!world.isPVP()))
				return true;
		}

		//if (plugin.getTownyUniverse().canAttackEnemy(ap.getName(), bp.getName()))
		//	return false;

		return false;
	}

	public boolean preventDamagePvP(TownyWorld world, Player a, Player b) {
		// Universe is only PvP
		if (world.isForcePVP() || world.isPVP())
			return false;
		//plugin.sendDebugMsg("is not forcing pvp");
		// World PvP
		if (!world.isPVP())
			return true;
		//plugin.sendDebugMsg("world is pvp");
		return false;
	}

	public boolean preventFriendlyFire(Player a, Player b) {
		TownyUniverse universe = plugin.getTownyUniverse();
		if (!TownySettings.getFriendlyFire() && universe.isAlly(a.getName(), b.getName())) {
			try {
				TownyWorld world = TownyUniverse.getDataSource().getWorld(b.getWorld().getName());
				TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(b)).getTownBlock();
				if (!townBlock.getType().equals(TownBlockType.ARENA))
					return true;
			} catch (TownyException x) {
				//world or townblock failure
				// But we want to prevent friendly fire in the wilderness too.
				return true;
			}
		}

		return false;
	}
}
