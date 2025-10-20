package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.mobs.MobSpawnRemovalEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;

import com.palmergames.util.JavaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.memory.MemoryKey;
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
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
			attacker.getType().getKey().equals(NamespacedKey.minecraft("axolotl")) &&
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

		if (!TownyAPI.getInstance().isTownyWorld(entity.getWorld()))
			return;
		
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(entity.getLocation());
		if (townBlock == null)
			return;
		
		if (entity instanceof Monster && townBlock.getType() == TownBlockType.ARENA)
			event.getDrops().clear();
	}
	
	/**
	 * Prevent entity and block explosions and lightning from hurting entities.
	 * 
	 * Doesn't stop damage to vehicles or hanging entities.
	 *  
	 * @param event - EntityDamageEvent
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityTakesExplosionDamage(EntityDamageEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		// Don't make Creeper damage tied to the explosion setting of a plot, otherwise
		// Creepers are completely useless.
		if (event instanceof EntityDamageByEntityEvent eevent && eevent.getDamager() instanceof Creeper)
			return;

		if (event.getCause() != null && causeIsExplosive(event.getCause()) && entityProtectedFromExplosiveDamageHere(event.getEntity(), event.getCause())) {
			event.setDamage(0);
			event.setCancelled(true);
		}
	}

	private boolean causeIsExplosive(DamageCause cause) {
		return switch(cause) {
		case ENTITY_EXPLOSION, BLOCK_EXPLOSION, LIGHTNING -> true;
		default -> false;
		};
	}
	
	/**
	 * Prevent lingering potion damage on players in non PVP areas
	 * 
	 *  @param event - LingeringPotionSplashEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onLingeringPotionApplyEvent(AreaEffectCloudApplyEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;
		
		final AreaEffectCloud effectCloud = event.getEntity();
		final List<PotionEffect> effects = new ArrayList<>();
		
		if (effectCloud.getBasePotionType() != null) {
			effects.addAll(effectCloud.getBasePotionType().getPotionEffects());
		}

		if (effectCloud.hasCustomEffects()) {
			effects.addAll(effectCloud.getCustomEffects());
		}

		if (!hasDetrimentalEffects(effects))
			return;

		// Prevent players from potentially damaging entities by logging out to null out the projectile source.
		if (effectCloud.getSource() == null && effectCloud.getOwnerUniqueId() != null && TownyUniverse.getInstance().hasResident(effectCloud.getOwnerUniqueId())) {
			event.setCancelled(true);
			return;
		}
		
		event.getAffectedEntities().removeIf(defender ->
			CombatUtil.preventDamageCall(effectCloud, defender, DamageCause.MAGIC));
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
		
		ThrownPotion potion = event.getPotion();
		
		if (!hasDetrimentalEffects(potion.getEffects()))
			return;
		
		for (final LivingEntity defender : event.getAffectedEntities()) {
			if (CombatUtil.preventDamageCall(potion, defender, DamageCause.MAGIC)) {
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

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld());

		// ignore non-Towny worlds.
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;

		// ignore Citizens NPCs and named-mobs (if configured.) 
		LivingEntity livingEntity = event.getEntity();
		if (entityIsExempt(livingEntity, event.getSpawnReason()))
			return;

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

	private boolean entityIsExempt(LivingEntity livingEntity, CreatureSpawnEvent.SpawnReason spawnReason) {
		return PluginIntegrations.getInstance().isNPC(livingEntity)
			|| entityIsExemptByName(livingEntity)
			|| MobRemovalTimerTask.isSpawnReasonIgnored(livingEntity, spawnReason);
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
	 * Handles:
	 *  - vehicles that would trigger a switch, allowed if a permitted player is riding.
	 *  - switch use and pressure plates triggered by Creatures.
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
		for (Entity passenger : passengers) {
			if (!(passenger instanceof Player player))
				continue;

			if (TownySettings.isSwitchMaterial(block.getType(), block.getLocation())) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canSwitch(player, block.getLocation(), block.getType()));
				return;
			}
		}

		// We only care about creatures at this point, players are handled in the TownyPlayerListener. 
		if (!(entity instanceof Creature))
			return;

		// Allow villagers to open doors
		if (entity instanceof Villager && ItemLists.WOOD_DOORS.contains(block.getType()))
			return;

		// Special case protecting stone pressure plates triggered by creatures.
		if (block.getType() == Material.STONE_PRESSURE_PLATE) {
			if(TownySettings.isCreatureTriggeringPressurePlateDisabled())
				event.setCancelled(true);
			return;
		}

		// Prevent protecting the wilderness from switch use.
		if (TownyAPI.getInstance().isWilderness(block))
			return;

		// Prevent creatures triggering switch items.
		if (TownySettings.isSwitchMaterial(block.getType(), block.getLocation())) {
			event.setCancelled(true);
			return;
		}

	}

	/**
	 * Handles: 
	 *  Enderman thieving protected blocks.
	 *  Ravagers breaking protected blocks.
	 *  Withers blowing up protected blocks.
	 *  Water being used to put out campfires.
	 *  Boats breaking lilypads.
	 *  Crop Trampling.
	 *  Silverfish infesting blocks inside towns.
	 * 
	 * Because we use ignoreCancelled = true we dont need to worry about setting the
	 * cancelled state to false overriding other plugins.
	 * 
	 * @param event onEntityChangeBlockEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		final Block block = event.getBlock();
		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(block.getWorld());
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;

		final Material blockMat = block.getType();
		final Entity entity = event.getEntity();
		final EntityType entityType = event.getEntityType();

		// Crop trampling protection done here.
		if (blockMat.equals(Material.FARMLAND)) {
			if (entity instanceof Player player) // Handle player trampling crops if disabled in the world.
				event.setCancelled(TownySettings.isPlayerCropTramplePrevented() || !TownyActionEventExecutor.canDestroy(player, block));
			else                                 // Handle creature trampling crops if disabled in the world.
				event.setCancelled(townyWorld.isDisableCreatureTrample());
			return;
		}

		// Prevent blocks from falling while their plot is being regenerated back to it's pre-claimed state.
		if (entity instanceof FallingBlock && TownyRegenAPI.hasActiveRegeneration(WorldCoord.parseWorldCoord(event.getBlock()))) {
			event.setCancelled(true);
			return;
		}

		// Test other instances of Entities altering blocks.
		if (entityType == EntityType.ENDERMAN) {
			event.setCancelled(townyWorld.isEndermanProtect());

		} else if (entityType == EntityType.RAVAGER) {
			event.setCancelled(townyWorld.isDisableCreatureTrample());

		} else if (entityType == EntityType.WITHER) {
			List<Block> allowed = TownyActionEventExecutor.filterExplodableBlocks(Collections.singletonList(block), blockMat, entity, event);
			event.setCancelled(allowed.isEmpty());

		} else if (EntityLists.BOATS.contains(entityType) && blockMat.equals(Material.LILY_PAD)) {
			// Protect lily pads.
			final List<Entity> passengers = entity.getPassengers();
			if (!passengers.isEmpty() && passengers.get(0) instanceof Player player)
				// Test if the player driving the boat can break here.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, block));
			else 
				// Protect townland from non-player-ridden boats. (Maybe someone is pushing a boat?)
				event.setCancelled(!TownyAPI.getInstance().isWilderness(block));

		} else if (entity instanceof ThrownPotion potion && potion.getEffects().isEmpty() && ItemLists.CAMPFIRES.contains(blockMat)) {
			// Protect campfires from being extinguished by plain Water Bottles (which are potions with no effects.)
			if (potion.getShooter() instanceof BlockProjectileSource bps)
				event.setCancelled(!BorderUtil.allowedMove(bps.getBlock(), block));
			else if (potion.getShooter() instanceof Player player)
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, block));
		} else if (entityType == EntityType.SILVERFISH && ItemLists.INFESTED_BLOCKS.contains(event.getTo()) && !TownyAPI.getInstance().isWilderness(event.getBlock().getLocation())) {
			event.setCancelled(true);
		}
	}

	/**
	 * Decides how explosions made by entities will be handled ie: TNT, Creepers, etc.
	 * <br>
	 * Handles wilderness entity explosion regeneration.
	 * <br>
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

		if (isWindCharge(event))
			return;

		List<Block> blocks = TownyActionEventExecutor.filterExplodableBlocks(event.blockList(), null, event.getEntity(), event);
		event.blockList().clear();
		event.blockList().addAll(blocks);

		if (event.blockList().isEmpty())
			return;
		
		Entity entity = event.getEntity();
		if (townyWorld.isUsingPlotManagementWildEntityRevert() && townyWorld.isProtectingExplosionEntity(entity)) {
			int count = 0;
			for (Block block : event.blockList()) {
				// Only regenerate in the wilderness.
				if (!TownyAPI.getInstance().isWilderness(block))
					continue;
				// Check the white/blacklist
				if (!townyWorld.isExplodedBlockAllowedToRevert(block.getType()))
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
	 * Protects against players using Wind Charges to open doors and use switches.
	 * 
	 * @param event - EntityExplodeEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onWindChargeExplode(EntityExplodeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getEntity().getWorld());
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;

		if (!isWindCharge(event))
			return;

		Player player = getWindChargePlayerOrNull(event);
		if (player == null)
			return;

		List<Block> deniedBlocks = new ArrayList<>();
		for (Block block : event.blockList())
			if (TownySettings.isSwitchMaterial(block.getType(), block.getLocation()) && !TownyActionEventExecutor.canSwitch(player, block.getLocation(), block.getType(), true))
				deniedBlocks.add(block);

		if (deniedBlocks.isEmpty())
			return;

		event.blockList().removeAll(deniedBlocks);
		TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_allowed_to_switch"));
	}
	
	private boolean isWindCharge(EntityExplodeEvent event) {
		return MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.MINECRAFT_1_21)
				&& event.getEntity() instanceof WindCharge charge;
	}

	@Nullable
	private Player getWindChargePlayerOrNull(EntityExplodeEvent event) {
		if (event.getEntity() instanceof WindCharge charge && charge.getShooter() instanceof Player player)
			return player;
		return null;
	}

	/**
	 * Prevent fire arrows and charges igniting players when PvP is disabled
	 * <br>
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
			
			final ProjectileSource source = projectile.getShooter();
			
			if (source instanceof BlockProjectileSource blockSource) {
				if (CombatUtil.preventDispenserDamage(blockSource.getBlock(), defender, DamageCause.PROJECTILE)) {
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
		} else if (combuster instanceof LightningStrike lightning) {
			// Protect entities from being lit on fire by player caused lightning
			if (lightning.getCausingEntity() instanceof Player player && CombatUtil.preventDamageCall(player, defender, DamageCause.LIGHTNING))
				event.setCancelled(true);
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
		return EntityLists.HANGING.contains(hanging) && 
			TownyRegenAPI.hasProtectionRegenTask(new BlockLocation(hanging.getLocation().add(hanging.getFacing().getOppositeFace().getDirection())));
	}

	private boolean itemFrameBrokenByBoatExploit(Entity hanging) {
		// This workaround prevent boats from destroying item_frames, detailed in https://hub.spigotmc.org/jira/browse/SPIGOT-3999.
		if (EntityLists.ITEM_FRAMES.contains(hanging)) {
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
			// Player doesn't have permission to break this hanging entity.
			return !allowedToBreak(player, hanging);
		} else if (remover instanceof Entity) {
			// An entity (probably a skeleton,) breaking a hanging entity in a town.
			return !TownyAPI.getInstance().isWilderness(hanging.getLocation());
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
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) || event.getHitBlock() == null || !(event.getEntity().getShooter() instanceof Player player))
			return;
		
		Block block = event.getHitBlock().getRelative(event.getHitBlockFace());
		Material material = block.getType();
		if (ItemLists.PROJECTILE_TRIGGERED_REDSTONE.contains(material) && TownySettings.isSwitchMaterial(material, block.getLocation())) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canSwitch(player, block.getLocation(), material)) {
				/*
				 * Since we are unable to cancel a ProjectileHitEvent on buttons & 
				 * pressure plates even using MC 1.17 we must set the block to air
				 * then set it back to its original form. 
				 */
				BlockData data = block.getBlockData();
				block.setType(Material.AIR);
				plugin.getScheduler().run(block.getLocation(), () -> block.setBlockData(data));
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
			|| hitBlock == null || projectileIsIrrelevantToBlock(hitBlock))
			return;

		// Prevent non-player actions outright if it is in a town.
		if (!(event.getEntity().getShooter() instanceof Player player)) {
			if (!TownyAPI.getInstance().isWilderness(hitBlock))
				event.setCancelled(true);
			return;
		}

		// Prevent players based on their PlayerCache/towny's cancellable event.
		if (disallowedTargetSwitch(hitBlock, player) ||
			disallowedProjectileBlockBreak(hitBlock, event.getEntity(), player) ||
			disallowedCampfireLighting(hitBlock, event.getEntity(), player)) {
			event.setCancelled(true);
		}
	}

	/**
	 * Weeds out scenarios where we don't care what an arrow would do when it hits a
	 * block.
	 * 
	 * @param hitBlock Block that is hit by an arrow.
	 * @return true if we don't care about this arrow hitting the block.
	 */
	private boolean projectileIsIrrelevantToBlock(Block hitBlock) {
		return hitBlock.getType() != Material.TARGET && !ItemLists.PROJECTILE_BREAKABLE_BLOCKS.contains(hitBlock.getType()) && !ItemLists.CAMPFIRES.contains(hitBlock.getType());
	}

	private boolean disallowedTargetSwitch(Block hitBlock, Player player) {
		return hitBlock.getType() == Material.TARGET && TownySettings.isSwitchMaterial(Material.TARGET, hitBlock.getLocation())
			&& !TownyActionEventExecutor.canSwitch(player, hitBlock.getLocation(), hitBlock.getType());
	}

	private boolean disallowedProjectileBlockBreak(Block hitBlock, Projectile projectile, Player player) {
		// Pointed dripstone can only be broken by tridents
		if (hitBlock.getType() == Material.POINTED_DRIPSTONE && !(projectile instanceof Trident))
			return false;

		// Decorated pots can't be broken by these 3 projectiles
		if (hitBlock.getType().getKey().equals(NamespacedKey.minecraft("decorated_pot")) && (projectile instanceof ShulkerBullet || projectile instanceof EnderPearl || projectile instanceof LlamaSpit))
			return false;

		return ItemLists.PROJECTILE_BREAKABLE_BLOCKS.contains(hitBlock.getType()) && !TownyActionEventExecutor.canDestroy(player, hitBlock.getLocation(), hitBlock.getType());
	}

	private boolean disallowedCampfireLighting(Block hitBlock, Projectile projectile, Player player) {
		return ItemLists.CAMPFIRES.contains(hitBlock.getType()) && isFireArrow(projectile) && !TownyActionEventExecutor.canDestroy(player, hitBlock);
	}

	private boolean isFireArrow(Projectile projectile) {
		return projectile instanceof Arrow arrow && arrow.getFireTicks() > 0;
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

	private static final Map<String, String> POTION_LEGACY_NAMES = JavaUtil.make(new HashMap<>(), map -> {
		map.put("slowness", "slow");
		map.put("haste", "fast_digging");
		map.put("mining_fatigue", "slow_digging");
		map.put("strength", "increase_damage");
		map.put("instant_health", "heal");
		map.put("instant_damage", "harm");
		map.put("jump_boost", "jump");
		map.put("nausea", "confusion");
		map.put("resistance", "damage_resistance");
	});

	private boolean hasDetrimentalEffects(Collection<PotionEffect> effects) {
		if (effects.isEmpty())
			return false;

		/*
		 * List of potion effects blocked from PvP.
		 */
		final List<String> detrimentalPotions = TownySettings.getPotionTypes().stream().map(type -> type.toLowerCase(Locale.ROOT)).toList();

		return effects.stream()
			.map(effect -> BukkitTools.potionEffectName(effect.getType()))
			.anyMatch(name -> {
				// Check to see if any of the potion effects are protected against.
				if (detrimentalPotions.contains(name))
					return true;

				// Account for PotionEffect#getType possibly returning the new name post enum removal.
				final String legacyName = POTION_LEGACY_NAMES.get(name);

                return legacyName != null && detrimentalPotions.contains(legacyName);
            });
	}
}