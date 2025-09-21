package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

@ApiStatus.Internal
public class TownyPaperEvents implements Listener {
	private final Towny plugin;
	
	private static final String SPIGOT_PRIME_EVENT = "org.bukkit.event.block.TNTPrimeEvent"; // Added somewhere during 1.19.4
	private static final String PAPER_PRIME_EVENT = "com.destroystokyo.paper.event.block.TNTPrimeEvent";
	
	private static final String SIGN_OPEN_EVENT = "io.papermc.paper.event.player.PlayerOpenSignEvent";
	private static final String SPIGOT_SIGN_OPEN_EVENT = "org.bukkit.event.player.PlayerSignOpenEvent";
	private static final String USED_SIGN_OPEN_EVENT = JavaUtil.classExists(SIGN_OPEN_EVENT) ? SIGN_OPEN_EVENT : SPIGOT_SIGN_OPEN_EVENT;

	private static final String PLAYER_ELYTRA_BOOST_EVENT = "com.destroystokyo.paper.event.player.PlayerElytraBoostEvent";

	private static final String DRAGON_FIREBALL_HIT_EVENT = "com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent";

	private static final String BEACON_EFFECT_EVENT = "com.destroystokyo.paper.event.block.BeaconEffectEvent";

	public static final MethodHandle SIGN_OPEN_GET_CAUSE = JavaUtil.getMethodHandle(USED_SIGN_OPEN_EVENT, "getCause");
	private static final MethodHandle SIGN_OPEN_GET_SIGN = JavaUtil.getMethodHandle(USED_SIGN_OPEN_EVENT, "getSign");

	private static final MethodHandle GET_ORIGIN = JavaUtil.getMethodHandle(Entity.class, "getOrigin");
	private static final MethodHandle GET_PRIMER_ENTITY = JavaUtil.getMethodHandle(PAPER_PRIME_EVENT, "getPrimerEntity");

	public static final MethodHandle DRAGON_FIREBALL_GET_EFFECT_CLOUD = JavaUtil.getMethodHandle(DRAGON_FIREBALL_HIT_EVENT, "getAreaEffectCloud");

	public static final MethodHandle BEACON_EFFECT_GET_PLAYER = JavaUtil.getMethodHandle(BEACON_EFFECT_EVENT, "getPlayer");
	
	public static final String ADD_TO_WORLD_EVENT = "com.destroystokyo.paper.event.entity.EntityAddToWorldEvent";
	
	public TownyPaperEvents(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void register() {
		if (JavaUtil.classExists(SPIGOT_PRIME_EVENT))
			registerEvent(SPIGOT_PRIME_EVENT, this::tntPrimeEvent, EventPriority.LOW, true);
		else if (GET_PRIMER_ENTITY != null) {
			registerEvent(PAPER_PRIME_EVENT, this::tntPrimeEvent, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("TNTPRimeEvent#getPrimerEntity method found, using TNTPrimeEvent listener.");
		}
		
		if (GET_ORIGIN != null) {
			registerEvent(EntityChangeBlockEvent.class, fallingBlockListener(), EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("Entity#getOrigin found, using falling block listener.");
		}
		
		if (SIGN_OPEN_GET_CAUSE != null) {
			registerEvent(JavaUtil.classExists(SIGN_OPEN_EVENT) ? SIGN_OPEN_EVENT : SPIGOT_SIGN_OPEN_EVENT, this::openSignListener, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("PlayerOpenSignEvent#getCause found, using PlayerOpenSignEvent listener.");
		}
		
		if (DRAGON_FIREBALL_GET_EFFECT_CLOUD != null) {
			registerEvent(DRAGON_FIREBALL_HIT_EVENT, this::dragonFireballHitEventListener, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("Using " + DRAGON_FIREBALL_GET_EFFECT_CLOUD + " listener.");
		}

		if (BEACON_EFFECT_GET_PLAYER != null) {
			registerEvent(BEACON_EFFECT_EVENT, this::beaconEffectEventListener, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("Using " + BEACON_EFFECT_GET_PLAYER + " listener.");
		}
		
		registerEvent(PLAYER_ELYTRA_BOOST_EVENT, this::playerElytraBoostListener, EventPriority.LOW, true);
		
		if (this.plugin.isFolia()) {
			registerEvent(ADD_TO_WORLD_EVENT, this::entityAddToWorldListener, EventPriority.MONITOR, false /* n/a */);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Event> void registerEvent(String className, Supplier<Consumer<T>> executor, EventPriority eventPriority, boolean ignoreCancelled) {
		try {
			Class<T> eventClass = (Class<T>) Class.forName(className).asSubclass(Event.class);
			registerEvent(eventClass, executor.get(), eventPriority, ignoreCancelled);
		} catch (ClassNotFoundException ignored) {}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> consumer, EventPriority eventPriority, boolean ignoreCancelled) {
		Bukkit.getPluginManager().registerEvent(eventClass, this, eventPriority, (listener, event) -> consumer.accept((T) event), plugin, ignoreCancelled);
	}
	
	// https://jd.papermc.io/paper/1.15/index.html?com/destroystokyo/paper/event/player/PlayerElytraBoostEvent.html
	private Consumer<PlayerEvent> playerElytraBoostListener() {
		return event -> {
			Player player = event.getPlayer();
			if (!TownySettings.isItemUseMaterial(Material.FIREWORK_ROCKET, player.getLocation()))
				return;

			((Cancellable) event).setCancelled(!TownyActionEventExecutor.canItemuse(player, player.getLocation(), Material.FIREWORK_ROCKET));
		};
	}
	
	// https://papermc.io/javadocs/paper/1.19/com/destroystokyo/paper/event/block/TNTPrimeEvent.html
	private Consumer<Event> tntPrimeEvent() {
		return event -> {
			Entity primerEntity = null;
			
			if (event.getClass().getName().equals(SPIGOT_PRIME_EVENT)) {
				primerEntity = ((TNTPrimeEvent) event).getPrimingEntity();
			} else if (GET_PRIMER_ENTITY != null) {
				try {
					primerEntity = (Entity) GET_PRIMER_ENTITY.invoke(event);
				} catch (final Throwable e) {
					return;
				}
			}

			if (primerEntity instanceof Projectile projectile) {
				Cancellable cancellable = (Cancellable) event;
				Block block = ((BlockEvent) event).getBlock();
				if (projectile.getShooter() instanceof Player player) {
					// A player shot a flaming arrow at the block, use a regular destroy test.
					cancellable.setCancelled(!TownyActionEventExecutor.canDestroy(player, block));
				} else if (projectile.getShooter() instanceof BlockProjectileSource bps) {
					// A block (likely a dispenser) shot a flaming arrow, cancel it if plot owners aren't the same.
					if (!BorderUtil.allowedMove(bps.getBlock(), block))
						cancellable.setCancelled(true);
				}
			}
		};
	};
	
	private Consumer<EntityChangeBlockEvent> fallingBlockListener() {
		return event -> {
			if (GET_ORIGIN == null || event.getEntityType() != EntityType.FALLING_BLOCK || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
				return;
			
			Location origin;
			try {
				origin = (Location) GET_ORIGIN.invokeExact(event.getEntity());
			} catch (final Throwable e) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred while invoking Entity#getOrigin reflectively", e);
				return;
			}
			
			if (origin == null)
				return;
			
			// If the z and x are the same then don't process allowedMove logic, since it couldn't have crossed a town boundary.
			if (origin.getBlockZ() == event.getBlock().getZ() && origin.getBlockX() == event.getBlock().getX())
				return;
			
			if (!BorderUtil.allowedMove(origin.getBlock(), event.getBlock()))
				event.setCancelled(true);
		};
	}
	
	private Consumer<Event> openSignListener() {
		return event -> {
			if (SIGN_OPEN_GET_CAUSE == null || SIGN_OPEN_GET_SIGN == null)
				return;
			
			final Enum<?> cause;
			final Sign sign;
			try {
				cause = (Enum<?>) SIGN_OPEN_GET_CAUSE.invoke(event);
				sign = (Sign) SIGN_OPEN_GET_SIGN.invoke(event);
			} catch (final Throwable e) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred while invoking " + USED_SIGN_OPEN_EVENT + "#getCause/#getSign reflectively", e);
				return;
			}
			
			if (!cause.name().equals("INTERACT") || !sign.isPlaced())
				return;
			
			if (!TownyActionEventExecutor.canDestroy(((PlayerEvent) event).getPlayer(), sign.getBlock()))
				((Cancellable) event).setCancelled(true);
		};
	}

	private Consumer<Event> dragonFireballHitEventListener() {
		return event -> {
			if (DRAGON_FIREBALL_GET_EFFECT_CLOUD == null)
				return;

			final AreaEffectCloud effectCloud;

			try {
				effectCloud = (AreaEffectCloud) DRAGON_FIREBALL_GET_EFFECT_CLOUD.invoke(event);
			} catch (final Throwable thr) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred when invoking " + DRAGON_FIREBALL_HIT_EVENT + "#getAreaEffectCloud reflectively.", thr);
				return;
			}

			if (TownyEntityListener.discardAreaEffectCloud(effectCloud))
				((Cancellable) event).setCancelled(true);
		};
	}
	
	private Consumer<Event> beaconEffectEventListener() {
		return event -> {
			if (BEACON_EFFECT_GET_PLAYER == null || !TownySettings.beaconsForTownMembersOnly())
				return;

			final Player player;
			final Block block;

			try {
				player = (Player) BEACON_EFFECT_GET_PLAYER.invoke(event);
				block = ((BlockEvent) event).getBlock();
			} catch (final Throwable thr) {
				plugin.getLogger().log(Level.WARNING, "An exception occurred when invoking " + BEACON_EFFECT_EVENT + "#getBlock or #getPlayer reflectively.", thr);
				return;
			}

			Town blockTown = TownyAPI.getInstance().getTown(block.getLocation());
			Town playerTown = TownyAPI.getInstance().getTown(player);
			
			// Beacon is in the wild.
			if (blockTown == null)
				return;
			
			if (playerTown != null && CombatUtil.isAlly(playerTown, blockTown)) {
				if (!(playerTown.isConquered() && blockTown.hasNation() && blockTown.getNationOrNull().hasTown(playerTown) && TownySettings.beaconsExcludeConqueredTowns()))
					return;
			}

			((Cancellable) event).setCancelled(true);
		};
	}

	private Consumer<EntityEvent> entityAddToWorldListener() {
		return event -> {
			if (!(event.getEntity() instanceof LivingEntity entity))
				return;
			
			if (entity instanceof Player || PluginIntegrations.getInstance().isNPC(entity))
				return;

			final TownyWorld world = TownyAPI.getInstance().getTownyWorld(entity.getWorld());

			plugin.getScheduler().runRepeating(entity, task -> {
				if (!entity.isValid()) {
					task.cancel();
					return;
				}

				if (MobRemovalTimerTask.isRemovingEntities(world)) {
					MobRemovalTimerTask.checkEntity(plugin, world, entity);
				}
			}, 1L, TimeTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
		};
	}
}
