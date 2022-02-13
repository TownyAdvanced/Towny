package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.mobs.MobSpawnRemovalEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ItemLists;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creature;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	 * Also handles EntityExplosions that damage entities.
	 * 
	 * @param event - EntityDamageByEntityEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		Entity attacker = event.getDamager();
		Entity defender = event.getEntity();

		/* 
		 * This test will check all Entity_Explosion-caused damaged, as long as it is
		 * not from a projectile (FireWorks and Fireballs will be handled later using
		 * the CombatUtil#preventDamageCall.) 
		 * 
		 * The reason for this is while we want to protect some mobs from explosions,
		 * players should be hurt by monster-related explosions or they will exploit their
		 * explosion-immunity while farming creepers/withers. PVP-related explosions are
		 * like-wise tested vs the area's PVP status.
		 */
		if (event.getCause() == DamageCause.ENTITY_EXPLOSION && !(attacker instanceof Projectile)) {
			boolean cancelExplosiveDamage = false;
			/*
			 * First we protect all protectedMobs as long as the location cannot explode.
			 */
			if (EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defender)
				&& !TownyActionEventExecutor.canExplosionDamageEntities(event.getEntity().getLocation(), event.getEntity(), event.getCause()))
				cancelExplosiveDamage = true;
			
			/*
			 * Second we protect players from PVP-based explosions which 
			 * aren't projectiles based on whether the location has PVP enabled.
			 */
			if (defender instanceof Player && EntityTypeUtil.isPVPExplosive(attacker.getType()))
				cancelExplosiveDamage = CombatUtil.preventPvP(TownyAPI.getInstance().getTownyWorld(defender.getWorld().getName()), TownyAPI.getInstance().getTownBlock(defender.getLocation()));
			
			/*
			 * Cancel explosion damage accordingly.
			 */
			if (cancelExplosiveDamage) {
				event.setDamage(0);
				event.setCancelled(true);
				return;
			}
		}

		/*
		 * This handles the remaining non-explosion damages. 
		 */
		if (CombatUtil.preventDamageCall(attacker, defender, event.getCause())) {
			// Remove the projectile here so no
			// other events can fire to cause damage
			if (attacker instanceof Projectile && !attacker.getType().equals(EntityType.TRIDENT))
				attacker.remove();

			event.setCancelled(true);
		}
	}

	/**
	 * Prevent monsters from dropping blocks if within an arena plot.
	 * 
	 * @param event - EntityDeathEvent
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) {
		if (plugin.isError()) {
			return;
		}

		Entity entity = event.getEntity();

		if (!TownyAPI.getInstance().isTownyWorld(entity.getWorld()) || TownyAPI.getInstance().isWilderness(entity.getLocation()))
			return;
		
		if (entity instanceof Monster)
			if (TownyAPI.getInstance().getTownBlock(entity.getLocation()).getType() == TownBlockType.ARENA)
				event.getDrops().clear();
	}
	
	/**
	 * Prevent block explosions and lightning from hurting entities.
	 * 
	 * Doesn't stop damage to vehicles or hanging entities.
	 *  
	 * @param event - EntityDamageEvent
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityTakesBlockExplosionDamage(EntityDamageEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		if ((event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.LIGHTNING) && !TownyActionEventExecutor.canExplosionDamageEntities(event.getEntity().getLocation(), event.getEntity(), event.getCause())) {
			event.setDamage(0);
			event.setCancelled(true);
		}
	}

	/**
	 * Removes dragon fireball AreaEffectClouds when they would spawn somewhere with PVP disabled.
	 * 
	 * @param event AreaEffectCloudApplyEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDragonFireBallCloudDamage(AreaEffectCloudApplyEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		if (!event.getEntity().getCustomEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.HARM)))
			return;

		if (!(event.getEntity().getSource() instanceof Player) || !(event.getEntity().getSource() instanceof DragonFireball))
			return;

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld().getName());
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(event.getEntity().getLocation());
		if (CombatUtil.preventPvP(townyWorld, townBlock)) {
			event.setCancelled(true);
			event.getEntity().remove();
		}

	}
	
	/**
	 * Prevent lingering potion damage on players in non PVP areas
	 * 
	 *  @param event - LingeringPotionSplashEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onLingeringPotionSplashEvent(LingeringPotionSplashEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		ThrownPotion potion = event.getEntity();
		Location loc = potion.getLocation();		
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(loc.getWorld().getName());
		float radius = event.getAreaEffectCloud().getRadius();
		List<Block> blocks = new ArrayList<>();
		
		for(double x = loc.getX() - radius; x < loc.getX() + radius; x++ ) {
			for(double z = loc.getZ() - radius; z < loc.getZ() + radius; z++ ) {
				Location loc2 = new Location(potion.getWorld(), x, loc.getY(), z);
			    Block b = loc2.getBlock();
			    if (b.getType().equals(Material.AIR)) blocks.add(b);
			}		   
		}
		
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

		for (Block block : blocks) {
						
			if (!TownyAPI.getInstance().isWilderness(block.getLocation()) 
					&& CombatUtil.preventPvP(townyWorld, TownyAPI.getInstance().getTownBlock(block.getLocation())) 
					&& detrimental) {
				event.setCancelled(true);
				break;
			}			
		}	
	}	
	
	/**
	 * Prevent splash potion damage on players in non PVP areas
	 * 
	 * @param event - PotionSplashEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		List<LivingEntity> affectedEntities = (List<LivingEntity>) event.getAffectedEntities();
		ThrownPotion potion = event.getPotion();
		Entity attacker = null;

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
		Block dispenser = null;

		if (source instanceof BlockProjectileSource blockProjectileSource) {
			dispenser = blockProjectileSource.getBlock();
		} else {
			attacker = (Entity) source;
		}
		
		for (LivingEntity defender : affectedEntities) {
			if (dispenser != null) {
				if (CombatUtil.preventDispenserDamage(dispenser, defender, DamageCause.MAGIC) && detrimental)
					event.setIntensity(defender, -1.0);
			} else 
			/*
			 * Don't block potion use on ourselves
			 * yet allow the use of beneficial potions on all.
			 */
			if (attacker != defender)
				if (CombatUtil.preventDamageCall(attacker, defender, DamageCause.MAGIC) && detrimental) {
					event.setIntensity(defender, -1.0);
				}
		}
	}

	/**
	 * Handles removal of newly spawned animals/monsters for use in the 
	 * world-removal and town-removal lists.
	 * 
	 * @param event - CreatureSpawnEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		// ignore non-Towny worlds.
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		if (event.getEntity() != null) {
			
			LivingEntity livingEntity = event.getEntity();

			// ignore Citizens NPCs
			if (BukkitTools.checkCitizens(livingEntity))
				return;
			
			if (TownySettings.isSkippingRemovalOfNamedMobs() && livingEntity.getCustomName() != null)
				return;

			MobSpawnRemovalEvent mobSpawnRemovalEvent;
			mobSpawnRemovalEvent = new MobSpawnRemovalEvent(event.getEntity());
			plugin.getServer().getPluginManager().callEvent(mobSpawnRemovalEvent);
			if(mobSpawnRemovalEvent.isCancelled()) return;

			Location loc = event.getLocation();
			TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(loc.getWorld().getName());
			
			// remove from world if set to remove mobs globally
			if (!townyWorld.hasWorldMobs() && MobRemovalTimerTask.isRemovingWorldEntity(livingEntity)) {
				event.setCancelled(true);
			}
			// handle villager baby removal in wilderness
			if (livingEntity instanceof Villager villager && !villager.isAdult() && (TownySettings.isRemovingVillagerBabiesWorld())) {
				event.setCancelled(true);
			}
			// Handle mob removal in wilderness
			if (TownyAPI.getInstance().isWilderness(loc)) {
				// Check if entity should be removed.
				if (!townyWorld.hasWildernessMobs() && MobRemovalTimerTask.isRemovingWildernessEntity(livingEntity)) {
					event.setCancelled(true);
				}
				return;
			}
			
			// handle mob removal in towns
			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(loc);
			if (!townyWorld.isForceTownMobs() && !townBlock.getPermissions().mobs && MobRemovalTimerTask.isRemovingTownEntity(livingEntity)) {
				event.setCancelled(true);
			}
			
			// handle villager baby removal in towns
			if (livingEntity instanceof Villager villager && !villager.isAdult() && TownySettings.isRemovingVillagerBabiesTown()) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Handles pressure plates (switch use) not triggered by players.
	 * example: animals or a boat with a player in it.
	 * 
	 * @param event - EntityInteractEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityInteract(EntityInteractEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;
		
		Block block = event.getBlock();
		Entity entity = event.getEntity();
		List<Entity> passengers = entity.getPassengers();

		/*
		 * Allow players in vehicles to activate pressure plates if they
		 * are permitted.
		 */
		if (passengers != null) {

			for (Entity passenger : passengers) {
				if (!(passenger instanceof Player player))
					return;

				if (TownySettings.isSwitchMaterial(block.getType(), block.getLocation())) {
					//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
					event.setCancelled(!TownyActionEventExecutor.canSwitch(player, block.getLocation(), block.getType()));
					return;
				}
			}
		}

		// Prevent creatures triggering stone pressure plates
		if (TownySettings.isCreatureTriggeringPressurePlateDisabled()) {
			if (block.getType() == Material.STONE_PRESSURE_PLATE) {
				if (entity instanceof Creature) {
					event.setCancelled(true);
					return;
				}
			}
		}

	}

	/**
	 * Handles:
	 *  Enderman thieving protected blocks.
	 *  Ravagers breaking protected blocks.
	 *  Withers blowing up protected blocks.
	 *  Water being used to put out campfires.
	 *  Crop Trampling.
	 * 
	 * @param event - onEntityChangeBlockEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getBlock().getWorld().getName());
		
		// Crop trampling protection done here.
		if (event.getBlock().getType().equals(Material.FARMLAND)) {
			// Handle creature trampling crops if disabled in the world.
			if (!event.getEntityType().equals(EntityType.PLAYER) && townyWorld.isDisableCreatureTrample()) {
				event.setCancelled(true);
				return;
			}
			// Handle player trampling crops if disabled in the world.
			if (event.getEntity() instanceof Player player) {
				event.setCancelled(TownySettings.isPlayerCropTramplePrevented() || !TownyActionEventExecutor.canDestroy(player, event.getBlock().getLocation(), Material.FARMLAND));
				return;
			}
		}

		switch (event.getEntity().getType()) {
	
			case ENDERMAN:
	
				if (townyWorld.isEndermanProtect())
					event.setCancelled(true);
				break;
				
			case RAVAGER:
				
				if (townyWorld.isDisableCreatureTrample())
					event.setCancelled(true);
				break;
		
			case WITHER:
				List<Block> allowed = TownyActionEventExecutor.filterExplodableBlocks(Collections.singletonList(event.getBlock()), event.getBlock().getType(), event.getEntity(), event);
				event.setCancelled(allowed.isEmpty());
				break;
			/*
			 * Protect campfires from SplashWaterBottles. Uses a destroy test.
			 */
			case SPLASH_POTION:			
				if (event.getBlock().getType() != Material.CAMPFIRE && ((ThrownPotion) event.getEntity()).getShooter() instanceof Player)
					return;
				event.setCancelled(!TownyActionEventExecutor.canDestroy((Player) ((ThrownPotion) event.getEntity()).getShooter(), event.getBlock().getLocation(), Material.CAMPFIRE));
				break;
			default:
		}
	}

	/**
	 * Decides how explosions made by entities will be handled ie: TNT, Creepers, etc.
	 * 
	 * Handles wilderness entity explosion regeneration.
	 * 
	 * Explosion blockList is filtered via the TownyActionEventExecutor,
	 * allowing Towny's war and other plugins to modify which blocks will
	 * be exploding.  
	 * 
	 * @param event - EntityExplodeEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld().getName());
		List<Block> blocks = TownyActionEventExecutor.filterExplodableBlocks(event.blockList(), null, event.getEntity(), event);
		event.blockList().clear();
		event.blockList().addAll(blocks);

		if (event.blockList().isEmpty())
			return;
		
		Entity entity = event.getEntity();
		if (townyWorld.isUsingPlotManagementWildEntityRevert() && entity != null && townyWorld.isProtectingExplosionEntity(entity)) {
			int count = 0;
			for (Block block : event.blockList()) {
				// Only regenerate in the wilderness.
				if (!TownyAPI.getInstance().isWilderness(block))
					continue;
				// Check the white/blacklist
				if (!townyWorld.isBlockAllowedToRevert(block.getType()))
					continue;
				// Don't start a revert on a block that is going to be reverted.
				if (TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(block.getLocation())))
					continue;
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, townyWorld, event);
			}
		}
	}

	/**
	 * Prevent fire arrows and charges igniting players when PvP is disabled
	 * 
	 * Can also prevent tnt from destroying armorstands
	 * 
	 * @param event - EntityCombustByEntityEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityCombustByEntityEvent(EntityCombustByEntityEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		Entity combuster = event.getCombuster();
		Entity defender = event.getEntity();
		LivingEntity attacker = null;
		if (combuster instanceof Projectile projectile) {
			
			Object source = projectile.getShooter();
			
			if (source instanceof BlockProjectileSource) {
				if (CombatUtil.preventDispenserDamage(((BlockProjectileSource) source).getBlock(), defender, DamageCause.PROJECTILE)) {
					combuster.remove();
					event.setCancelled(true);
					return;
				}
			} else {
				attacker = (LivingEntity) source;
			}

			// There is an attacker.
			if (attacker != null) {

				if (CombatUtil.preventDamageCall(attacker, defender, DamageCause.PROJECTILE)) {
					// Remove the projectile here so no
					// other events can fire to cause damage
					combuster.remove();
					event.setCancelled(true);
				}
			}
		}

	}

	/**
	 * Handles protection of item frames and other Hanging types.
	 * 
	 * @param event - HangingBreakEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		Entity hanging = event.getEntity();		
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(hanging.getWorld().getName());

		// Prevent an item_frame or painting from breaking if it is attached to something which will be regenerated.
		if (event.getCause().equals(RemoveCause.PHYSICS) && ItemLists.HANGING_ENTITIES.contains(hanging.getType().name())) {
			Location loc = hanging.getLocation().add(hanging.getFacing().getOppositeFace().getDirection());
			if (TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(loc))) {
				event.setCancelled(true);
				return;
			}
		}
		
		// TODO: Keep an eye on https://hub.spigotmc.org/jira/browse/SPIGOT-3999 to be completed.
		// This workaround prevent boats from destroying item_frames.
		if (event.getCause().equals(RemoveCause.PHYSICS) && ItemLists.ITEM_FRAMES.contains(hanging.getType().name())) {
			Block block = hanging.getLocation().add(hanging.getFacing().getOppositeFace().getDirection()).getBlock();
			if (block.isLiquid() || block.isEmpty())
				return;
			
			for (Entity entity : hanging.getNearbyEntities(0.5, 0.5, 0.5)) {
				if (entity instanceof Vehicle) {
					event.setCancelled(true);
					return;
				}
			}
		}

		/*
		 * It's a player or an entity (probably an explosion)
		 */
		if (event instanceof HangingBreakByEntityEvent evt) {
			Object remover = evt.getRemover();
			
			/*
			 * Check if this has a shooter.
			 */
			if (remover instanceof Projectile projectile)
				remover = projectile.getShooter();

			if (remover instanceof Player player) {
				Material mat = EntityTypeUtil.parseEntityToMaterial(event.getEntity().getType(), Material.GRASS_BLOCK);

				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, hanging.getLocation(), mat));
			} else if (remover instanceof Entity) {
				/*
				 * Probably a skeleton, cancel the break if it is in a town.
				 */
				if (!TownyAPI.getInstance().isWilderness(hanging.getLocation()))
					event.setCancelled(true);
			}
		
			if (event.getCause() == RemoveCause.EXPLOSION) {
				// Explosions are blocked in this plot
				if (!TownyActionEventExecutor.canExplosionDamageEntities(hanging.getLocation(), event.getEntity(), DamageCause.ENTITY_EXPLOSION)) {
					event.setCancelled(true);
				// Explosions are enabled, must check if in the wilderness and if we have explrevert in that world
				} else {
					TownBlock tb = TownyAPI.getInstance().getTownBlock(hanging.getLocation());
					// We're in the wilderness because the townblock is null and we have a remover.
					if (tb == null && remover != null)
					    if (townyWorld.isExpl() && townyWorld.isUsingPlotManagementWildEntityRevert() && townyWorld.isProtectingExplosionEntity((Entity)remover))
							event.setCancelled(true);
				}
			}

		/*
		 * Probably a case of a block explosion/created explosion with no Entity.
		 */
		} else {

			if (event.getCause() == RemoveCause.EXPLOSION) {
				if (!TownyActionEventExecutor.canExplosionDamageEntities(event.getEntity().getLocation(), event.getEntity(), DamageCause.BLOCK_EXPLOSION))
					event.setCancelled(true);
			}
		}

	}

	/**
	 * Placing of hanging objects like Item Frames.
	 * 
	 * @param event - HangingPlaceEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		Material mat = EntityTypeUtil.parseEntityToMaterial(event.getEntity().getType(), Material.GRASS_BLOCK);

		//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
		event.setCancelled(!TownyActionEventExecutor.canBuild(event.getPlayer(), event.getEntity().getLocation(), mat));
	}

	/**
	 * When a Pig is zapped by lightning
	 * 
	 * @param event - PigZapEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPigHitByLightning(PigZapEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		if (!TownyActionEventExecutor.canExplosionDamageEntities(event.getEntity().getLocation(), event.getEntity(), DamageCause.LIGHTNING))
			event.setCancelled(true);
	}
	
	/**
	 * Allows us to treat the hitting of wooden plates and buttons by arrows as cancellable events.
	 * 
	 * @param event ProjectileHitEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHitEventButtonOrPlate(ProjectileHitEvent event) {
		/*
		 * Bypass any occasion where there is no block being hit and the shooter isn't a player.
		 */
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) || event.getHitBlock() == null || !(event.getEntity().getShooter() instanceof Player))
			return;
		
		Block block = event.getHitBlock().getRelative(event.getHitBlockFace());
		Material material = block.getType();
		if (ItemLists.PROJECTILE_TRIGGERED_REDSTONE.contains(material.name()) && TownySettings.isSwitchMaterial(material, block.getLocation())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canSwitch((Player) event.getEntity().getShooter(), block.getLocation(), material)) {
				/*
				 * Since we are unable to cancel a ProjectileHitEvent on buttons & 
				 * pressure plates even using MC 1.17 we must set the block to air
				 * then set it back to its original form. 
				 */
				BlockData data = block.getBlockData();
				block.setType(Material.AIR);
				BukkitTools.getScheduler().runTask(plugin, () -> block.setBlockData(data));
			}
		}
	}
	
	/**
	 * Allows us to treat the hitting of Target blocks by arrows as cancellable events.
	 * 
	 * @param event ProjectileHitEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHitEventTarget(ProjectileHitEvent event) {
		/*
		 * Bypass any occasion where there is no block being hit and the shooter isn't a player.
		 */
		if (plugin.isError() || !Towny.is116Plus() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) || event.getHitBlock() == null || !(event.getEntity().getShooter() instanceof Player))
			return;

		if (event.getHitBlock().getType() == Material.TARGET && TownySettings.isSwitchMaterial(Material.TARGET, event.getHitBlock().getLocation())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canSwitch((Player) event.getEntity().getShooter(), event.getHitBlock().getLocation(), Material.TARGET)) {
				
				if (event instanceof Cancellable) { //TODO: When support is dropped for pre-1.17 MC versions the else can be removed.
					event.setCancelled(true);
				} else {
					/*
					 * Since we are unable to cancel a ProjectileHitEvent before MC 1.17 we must
					 * set the block to air then set it back to its original form. 
					 */
					BlockData data = event.getHitBlock().getBlockData();
					event.getHitBlock().setType(Material.AIR);
					BukkitTools.getScheduler().runTask(plugin, () -> event.getHitBlock().setBlockData(data));
				}
			}
		}
	}
	
	/**
	 * Allows us to protect chorus flowers being broken by bows.
	 * 
	 * @param event ProjectileHitEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHitEventChorusFlower(ProjectileHitEvent event) {
		/*
		 * Bypass any occasion where there is no block being hit and the shooter isn't a player.
		 */
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) || event.getHitBlock() == null || !(event.getEntity().getShooter() instanceof Player))
			return;

		if (event.getHitBlock().getType() == Material.CHORUS_FLOWER) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canDestroy((Player) event.getEntity().getShooter(), event.getHitBlock().getLocation(), Material.CHORUS_FLOWER)) {
				
				if (event instanceof Cancellable) { //TODO: When support is dropped for pre-1.17 MC versions the else can be removed.
					event.setCancelled(true);
				} else {
					/*
					 * Since we are unable to cancel a ProjectileHitEvent before MC 1.17 we must
					 * set the block to air then set it back to its original form. 
					 */
					BlockData data = event.getHitBlock().getBlockData();
					event.getHitBlock().setType(Material.AIR);
					BukkitTools.getScheduler().runTask(plugin, () -> event.getHitBlock().setBlockData(data));
				}
			}
		}
	}
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDoorBreak(EntityBreakDoorEvent event) {
		if (TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()) && !TownyAPI.getInstance().isWilderness(event.getBlock().getLocation()))
			event.setCancelled(true);
	}
}