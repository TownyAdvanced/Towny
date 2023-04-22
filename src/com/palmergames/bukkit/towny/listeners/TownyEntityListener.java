package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.mobs.MobSpawnRemovalEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.utils.BorderUtil;
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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
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
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
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
				&& TownySettings.areProtectedEntitiesProtectedAgainstMobs()
				&& entityProtectedFromExplosiveDamageHere(defender, event.getCause()))
				cancelExplosiveDamage = true;
			
			/*
			 * Second we protect players from PVP-based explosions which 
			 * aren't projectiles based on whether the location has PVP enabled.
			 */
			if (defender instanceof Player && EntityTypeUtil.isPVPExplosive(attacker.getType()))
				cancelExplosiveDamage = CombatUtil.preventPvP(TownyAPI.getInstance().getTownyWorld(defender.getWorld()), TownyAPI.getInstance().getTownBlock(defender.getLocation()));
			
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
	 * Prevent axolotl from targeting protected mobs.
	 *
	 * @param event - EntityTargetLivingEntityEvent
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onAxolotlTarget(EntityTargetLivingEntityEvent event) {
		if (event.getEntity() instanceof Mob attacker &&
			attacker.getType().name().equals("AXOLOTL") &&
			event.getTarget() instanceof Mob defender &&
			CombatUtil.preventDamageCall(attacker, defender, DamageCause.ENTITY_ATTACK)) {
			attacker.setMemory(MemoryKey.HAS_HUNTING_COOLDOWN, true);
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

		if ((event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.LIGHTNING) && entityProtectedFromExplosiveDamageHere(event.getEntity(), event.getCause())) {
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

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld());
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
		boolean detrimental = false;

		/*
		 * List of potion effects blocked from PvP.
		 */
		List<String> detrimentalPotions = TownySettings.getPotionTypes();

		for (PotionEffect effect : potion.getEffects()) {

			/*
			 * Check to see if any of the potion effects are protected.
			 */
			if (detrimentalPotions.contains(effect.getType().getName())) {
				detrimental = true;
				break;
			}
		}

		if (!detrimental)
			return;
		
		Location loc = potion.getLocation();		
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(loc.getWorld());
		float radius = event.getAreaEffectCloud().getRadius();
		List<Block> blocks = new ArrayList<>();
		
		for(double x = loc.getX() - radius; x < loc.getX() + radius; x++ ) {
			for(double z = loc.getZ() - radius; z < loc.getZ() + radius; z++ ) {
				Location loc2 = new Location(potion.getWorld(), x, loc.getY(), z);
			    Block b = loc2.getBlock();
			    if (b.getType().equals(Material.AIR)) blocks.add(b);
			}		   
		}

		for (Block block : blocks) {
						
			if (!TownyAPI.getInstance().isWilderness(block.getLocation()) 
					&& CombatUtil.preventPvP(townyWorld, TownyAPI.getInstance().getTownBlock(block.getLocation()))) {
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
		
		boolean detrimental = false;

		/*
		 * List of potion effects blocked from PvP.
		 */
		List<String> detrimentalPotions = TownySettings.getPotionTypes();
		
		for (PotionEffect effect : event.getPotion().getEffects()) {

			/*
			 * Check to see if any of the potion effects are protected.
			 */
			
			if (detrimentalPotions.contains(effect.getType().getName())) {
				detrimental = true;
				break;
			}
		}
		
		if (!detrimental)
			return;
		
		for (LivingEntity defender : event.getAffectedEntities()) {
			if (CombatUtil.preventDamageCall(event.getPotion(), defender, DamageCause.MAGIC)) {
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

		// ignore Citizens NPCs and named-mobs (if configured.) 
		LivingEntity livingEntity = event.getEntity();
		if (entityIsExempt(livingEntity))
			return;

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld());
		if (disallowedWorldMob(townyWorld.hasWorldMobs(), livingEntity)) {
			// Handle mob removal world-wide. 
			if (weAreAllowedToRemoveThis(livingEntity))
				event.setCancelled(true);
		} else if (disallowedWildernessMob(townyWorld.hasWildernessMobs(), livingEntity)) {
			// Handle mob removal specific to the wilderness.
			if (weAreAllowedToRemoveThis(livingEntity))
				event.setCancelled(true);
		} else if (disallowedTownMob(livingEntity)) {
			// Handle mob removal specific to towns.
			if (weAreAllowedToRemoveThis(livingEntity))
				event.setCancelled(true);
		}
	}

	private boolean entityIsExempt(LivingEntity livingEntity) {
		return PluginIntegrations.getInstance().checkCitizens(livingEntity)
			|| entityIsExemptByName(livingEntity);
	}

	private boolean entityIsExemptByName(LivingEntity livingEntity) {
		return TownySettings.isSkippingRemovalOfNamedMobs() && livingEntity.getCustomName() != null 
				&& !PluginIntegrations.getInstance().checkHostileEliteMobs(livingEntity);
	}

	private boolean disallowedWorldMob(boolean worldAllowsMobs, LivingEntity livingEntity) {
		return (!worldAllowsMobs && MobRemovalTimerTask.isRemovingWorldEntity(livingEntity))
				|| disallowedWorldVillagerBaby(livingEntity);
	}

	private boolean disallowedWorldVillagerBaby(LivingEntity livingEntity) {
		return TownySettings.isRemovingVillagerBabiesWorld() && livingEntity instanceof Villager villager && !villager.isAdult();
	}

	private boolean disallowedWildernessMob(boolean wildernessAllowsMobs, LivingEntity livingEntity) {
		return TownyAPI.getInstance().isWilderness(livingEntity.getLocation()) && !wildernessAllowsMobs && MobRemovalTimerTask.isRemovingWildernessEntity(livingEntity);
	}

	private boolean disallowedTownMob(LivingEntity livingEntity) {
		return !TownyAPI.getInstance().isWilderness(livingEntity.getLocation()) && (disallowedByTown(livingEntity) || disallowedTownVillagerBaby(livingEntity));
	}

	private boolean disallowedByTown(LivingEntity livingEntity) {
		return !TownyAPI.getInstance().areMobsEnabled(livingEntity.getLocation()) && MobRemovalTimerTask.isRemovingTownEntity(livingEntity);
	}

	private boolean disallowedTownVillagerBaby(LivingEntity livingEntity) {
		return TownySettings.isRemovingVillagerBabiesTown() && livingEntity instanceof Villager villager && !villager.isAdult();
	}

	/**
	 * Towny would normally remove this entity, but we use a
	 * {@link MobSpawnRemovalEvent} to allow other plugins to override us. The event
	 * starts out in a cancelled state.
	 * 
	 * @param livingEntity LivingEntity which spawned.
	 * @return true if Towny is allowed to remove this entity.
	 */
	private boolean weAreAllowedToRemoveThis(LivingEntity livingEntity) {
		return !BukkitTools.isEventCancelled(new MobSpawnRemovalEvent(livingEntity));
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

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getBlock().getWorld());

		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;

		
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
			case ENDERMAN -> {
				if (townyWorld.isEndermanProtect())
					event.setCancelled(true);
			}

			/* Protect lily pads. */
			case BOAT, CHEST_BOAT -> {
				if (!event.getBlock().getType().equals(Material.LILY_PAD))
					return;
				
				final List<Entity> passengers = event.getEntity().getPassengers();
				
				if (!passengers.isEmpty() && passengers.get(0) instanceof Player player)
					// Test if the player can break here.
					event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getBlock()));
				else if (!TownyAPI.getInstance().isWilderness(event.getBlock()))
					// Protect townland from non-player-ridden boats. (Maybe someone is pushing a boat?)
					event.setCancelled(true);
			}

			case RAVAGER -> {
				if (townyWorld.isDisableCreatureTrample())
					event.setCancelled(true);
			}

			case WITHER -> {
				List<Block> allowed = TownyActionEventExecutor.filterExplodableBlocks(Collections.singletonList(event.getBlock()), event.getBlock().getType(), event.getEntity(), event);
				event.setCancelled(allowed.isEmpty());
			}

			/* Protect campfires from SplashWaterBottles. Uses a destroy test. */
			case SPLASH_POTION -> {
				final ThrownPotion potion = (ThrownPotion) event.getEntity();
				// Check that the affected block is a campfire and that a water bottle (no effects) was used.
				if (event.getBlock().getType() != Material.CAMPFIRE || !potion.getEffects().isEmpty())
					return;
				
				if (potion.getShooter() instanceof BlockProjectileSource bps)
					event.setCancelled(!BorderUtil.allowedMove(bps.getBlock(), event.getBlock()));
				else if (potion.getShooter() instanceof Player player)
					event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getBlock().getLocation(), Material.CAMPFIRE));
			}
			
			default -> {}
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

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld());
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;
		
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
		RemoveCause removeCause = event.getCause();

		// Protect hanging entities from Physics-caused removals, including keeping them
		// in place while we regenerate their exploded-attached-block.
		if (removeCause.equals(RemoveCause.PHYSICS)) {
			if (attachedToRegeneratingBlock(hanging) || itemFrameBrokenByBoatExploit(hanging))
				event.setCancelled(true);
			return;
		}

		// It's an Entity that has broken this Hanging.
		if (event instanceof HangingBreakByEntityEvent evt) {
			// Test entity-caused Explosions, Players and Mobs.
			if (preventHangingBrokenByEntity(hanging, removeCause, evt.getRemover()))
				event.setCancelled(true);
			return;
		}

		// Probably a case of a block explosion/created explosion with no Entity.
		if (removeCause.equals(RemoveCause.EXPLOSION)) {
			if (entityProtectedFromExplosiveDamageHere(hanging, DamageCause.BLOCK_EXPLOSION) || weAreRevertingBlockExplosionsInWild(hanging.getLocation()))
				event.setCancelled(true);
			return;
		}
	}

	private boolean attachedToRegeneratingBlock(Entity hanging) {
		// Prevent an item_frame or painting from breaking if it is attached to something which will be regenerated.
		return ItemLists.HANGING_ENTITIES.contains(hanging.getType().name()) && 
			TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(hanging.getLocation().add(hanging.getFacing().getOppositeFace().getDirection())));
	}

	private boolean itemFrameBrokenByBoatExploit(Entity hanging) {
		// This workaround prevent boats from destroying item_frames, detailed in https://hub.spigotmc.org/jira/browse/SPIGOT-3999.
		if (ItemLists.ITEM_FRAMES.contains(hanging.getType().name())) {
			Block block = hanging.getLocation().add(hanging.getFacing().getOppositeFace().getDirection()).getBlock();
			if (block.isLiquid() || block.isEmpty())
				return false;
			
			for (Entity entity : hanging.getNearbyEntities(0.5, 0.5, 0.5)) {
				if (entity instanceof Vehicle) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean preventHangingBrokenByEntity(Entity hanging, RemoveCause removeCause, Object remover) {
		// Handle instances of Explosions caused by entities.
		if (removeCause.equals(RemoveCause.EXPLOSION)
				&& (entityProtectedFromExplosiveDamageHere(hanging, DamageCause.ENTITY_EXPLOSION) || weAreRevertingThisRemoversExplosionsInWild(hanging.getLocation(), remover))) {
			return true;
		}

		// Parse a potential projectile from its shooter entity.
		if (remover instanceof Projectile projectile)
			remover = projectile.getShooter();

		if (remover instanceof Player player) {
			if (!allowedToBreak(player, hanging))
				// Player doesn't have permission to break this hanging entity.
				return true;
		} else if (remover instanceof Entity) {
			if (!TownyAPI.getInstance().isWilderness(hanging.getLocation()))
				// An entity (probably a skeleton,) breaking a hanging entity in a town.
				return true;
		}
		return false;
	}

	private boolean allowedToBreak(Player player, Entity hanging) {
		// Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
		return TownyActionEventExecutor.canDestroy(player, hanging.getLocation(), EntityTypeUtil.parseEntityToMaterial(hanging.getType(), Material.GRASS_BLOCK));
	}

	private boolean weAreRevertingThisRemoversExplosionsInWild(Location loc, Object remover) {
		// Already tested if Explosions are enabled here, must check if in the
		// wilderness and if we have explrevert in that world and we are reverting this
		// type of entity's explosions.
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(loc.getWorld());
		return remover != null && TownyAPI.getInstance().isWilderness(loc) 
				&& townyWorld.isUsingPlotManagementWildEntityRevert() && townyWorld.isProtectingExplosionEntity((Entity)remover);
	}

	private boolean weAreRevertingBlockExplosionsInWild(Location loc) {
		// Already tested if Explosions are enabled here, must check if in the
		// wilderness and if we have blockexplrevert enabled in the world.
		// Because of the nature of block explosions in Bukkit's API it is impossible or
		// not worth finding the explosion's source-block, we assume it is being reverted.
		return TownyAPI.getInstance().isWilderness(loc) && TownyAPI.getInstance().getTownyWorld(loc.getWorld()).isUsingPlotManagementWildBlockRevert();
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
		
		if (entityProtectedFromExplosiveDamageHere(event.getEntity(), DamageCause.LIGHTNING))
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
		if (ItemLists.PROJECTILE_TRIGGERED_REDSTONE.contains(material) && TownySettings.isSwitchMaterial(material, block.getLocation())) {
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
	 * Allows us to treat the hitting of Target and ChorusFlower blocks by arrows as cancellable events.
	 * 
	 * @param event ProjectileHitEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHitBlockEvent(ProjectileHitEvent event) {
		/*
		 * Bypass any occasion where there is no block being hit or this is not a chorus flower or target block being hit.
		 */
		Block hitBlock = event.getHitBlock();
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) 
			|| hitBlock == null || (hitBlock.getType() != Material.TARGET && hitBlock.getType() != Material.CHORUS_FLOWER))
			return;

		// Prevent non-player actions outright if it is in a town.
		if (!(event.getEntity().getShooter() instanceof Player player)) {
			if (!TownyAPI.getInstance().isWilderness(hitBlock))
				cancelProjectileHitEvent(event, hitBlock);
			return;
		}

		// Prevent players based on their PlayerCache/towny's cancellable event.
		if (disallowedTargetSwitch(hitBlock, player) || disallowedChorusFlowerBreak(hitBlock, player)) {
			cancelProjectileHitEvent(event, hitBlock);
		}
	}

	private boolean disallowedTargetSwitch(Block hitBlock, Player player) {
		return hitBlock.getType() == Material.TARGET && TownySettings.isSwitchMaterial(Material.TARGET, hitBlock.getLocation())
			&& !TownyActionEventExecutor.canSwitch(player, hitBlock.getLocation(), hitBlock.getType());
	}

	private boolean disallowedChorusFlowerBreak(Block hitBlock, Player player) {
		return hitBlock.getType() == Material.CHORUS_FLOWER && !TownyActionEventExecutor.canDestroy(player, hitBlock.getLocation(), hitBlock.getType());
	}

	private void cancelProjectileHitEvent(ProjectileHitEvent event, Block block) {
		if (event instanceof Cancellable) {
			event.setCancelled(true);
			return;
		}
		/*
		 * Since we are unable to cancel a ProjectileHitEvent before MC 1.17 we must
		 * set the block to air then set it back to its original form. 
		 * TODO: When support is dropped for pre-1.17 MC versions this method can be removed.
		 */
		BlockData data = block.getBlockData();
		block.setType(Material.AIR);
		BukkitTools.getScheduler().runTask(plugin, () -> block.setBlockData(data));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDoorBreak(EntityBreakDoorEvent event) {
		if (TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()) && !TownyAPI.getInstance().isWilderness(event.getBlock().getLocation()))
			event.setCancelled(true);
	}
	
	/**
	 * Are entities protected from Explosions here.
	 * 
	 * @param entity Entity which is being damaged.
	 * @param cause  DamageCause that is hurting said entity.
	 * @return True if Towny would protect the entity from explosions, using the
	 *         plot permissions and a cancellable event.
	 */
	private boolean entityProtectedFromExplosiveDamageHere(Entity entity, DamageCause cause) {
		return !TownyActionEventExecutor.canExplosionDamageEntities(entity.getLocation(), entity, cause);
	}

}