package com.palmergames.bukkit.towny.listeners;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
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
import com.palmergames.bukkit.towny.utils.MinecraftVersion;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.TimeTools;

import io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

@ApiStatus.Internal
public class TownyPaperEvents implements Listener {
	private final Towny plugin;
	
	private static final String SIGN_OPEN_EVENT = "io.papermc.paper.event.player.PlayerOpenSignEvent";
	private static final String SPIGOT_SIGN_OPEN_EVENT = "org.bukkit.event.player.PlayerSignOpenEvent";
	private static final String USED_SIGN_OPEN_EVENT = JavaUtil.classExists(SIGN_OPEN_EVENT) ? SIGN_OPEN_EVENT : SPIGOT_SIGN_OPEN_EVENT;

	private static final String PLAYER_ELYTRA_BOOST_EVENT = "com.destroystokyo.paper.event.player.PlayerElytraBoostEvent";

	private static final String DRAGON_FIREBALL_HIT_EVENT = "com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent";

	private static final String BEACON_EFFECT_EVENT = "com.destroystokyo.paper.event.block.BeaconEffectEvent";

	public static final MethodHandle SIGN_OPEN_GET_CAUSE = JavaUtil.getMethodHandle(USED_SIGN_OPEN_EVENT, "getCause");
	private static final MethodHandle SIGN_OPEN_GET_SIGN = JavaUtil.getMethodHandle(USED_SIGN_OPEN_EVENT, "getSign");

	public static final String ADD_TO_WORLD_EVENT = "com.destroystokyo.paper.event.entity.EntityAddToWorldEvent";

	public static final String COPPER_GOLEM_MOVES_ITEM_EVENT = "io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent";
	
	public TownyPaperEvents(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void register() {
		registerEvent(TNTPrimeEvent.class, tntPrimeEvent(), EventPriority.LOW, true);
		registerEvent(EntityChangeBlockEvent.class, fallingBlockListener(), EventPriority.LOW, true);
		
		if (SIGN_OPEN_GET_CAUSE != null) {
			registerEvent(JavaUtil.classExists(SIGN_OPEN_EVENT) ? SIGN_OPEN_EVENT : SPIGOT_SIGN_OPEN_EVENT, this::openSignListener, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("PlayerOpenSignEvent#getCause found, using PlayerOpenSignEvent listener.");
		}
		
		if (JavaUtil.classExists(BEACON_EFFECT_EVENT)) {
			registerEvent(BEACON_EFFECT_EVENT, this::beaconEffectEventListener, EventPriority.LOW, true);
			TownyMessaging.sendDebugMsg("Using " + BEACON_EFFECT_EVENT + " listener.");
		}
		
		registerEvent(PLAYER_ELYTRA_BOOST_EVENT, this::playerElytraBoostListener, EventPriority.LOW, true);
		
		if (this.plugin.isFolia()) {
			registerEvent(ADD_TO_WORLD_EVENT, this::entityAddToWorldListener, EventPriority.MONITOR, false /* n/a */);
		}
		
		if (MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(Version.fromString("1.21.10")))
			registerEvent(COPPER_GOLEM_MOVES_ITEM_EVENT, this::onGolemMoveItem, EventPriority.NORMAL, true);
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
	
	private Consumer<TNTPrimeEvent> tntPrimeEvent() {
		return event -> {
			Entity primerEntity = event.getPrimingEntity();

			if (primerEntity instanceof Projectile projectile) {
				Block block = event.getBlock();

				if (projectile.getShooter() instanceof Player player) {
					// A player shot a flaming arrow at the block, use a regular destroy test.
					event.setCancelled(!TownyActionEventExecutor.canDestroy(player, block));
				} else if (projectile.getShooter() instanceof BlockProjectileSource bps) {
					// A block (likely a dispenser) shot a flaming arrow, cancel it if plot owners aren't the same.
					if (!BorderUtil.allowedMove(bps.getBlock(), block)) {
						event.setCancelled(true);
					}
				}
			}
		};
	};
	
	private Consumer<EntityChangeBlockEvent> fallingBlockListener() {
		return event -> {
			if (event.getEntityType() != EntityType.FALLING_BLOCK || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
				return;
			
			Location origin = event.getEntity().getOrigin();
			
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

	private Consumer<BeaconEffectEvent> beaconEffectEventListener() {
		return event -> {
			if (!TownySettings.beaconsForTownMembersOnly())
				return;

			final Player player = event.getPlayer();
			final Block block = event.getBlock();

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

	private static final @NotNull NamespacedKey GOLEM_CHEST_X = Objects.requireNonNull(NamespacedKey.fromString("towny:golemchestx"));
	private static final @NotNull NamespacedKey GOLEM_CHEST_Z = Objects.requireNonNull(NamespacedKey.fromString("towny:golemchestz"));
	public Consumer<ItemTransportingEntityValidateTargetEvent> onGolemMoveItem() {
		return event -> {
			if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()) || !(event.getEntity() instanceof CopperGolem golem))
				return;

			// The golem is attempting to remove an item from a copper chest.
			if (ItemLists.COPPER_CHEST.contains(event.getBlock().getType())) {
				setGolemLastChestLocation(golem, event.getBlock().getX(), event.getBlock().getZ());
				return;
			}

			Location golemChestLoc = getLastUsedChestLocation(golem);
			if (golemChestLoc == null || BorderUtil.allowedCopperGolemMove(golemChestLoc, event.getBlock().getLocation()))
				return;

			event.setAllowed(false);
		};
	}

	private void setGolemLastChestLocation(CopperGolem golem, int x, int z) {
		PersistentDataContainer pdc = golem.getPersistentDataContainer();
		pdc.set(GOLEM_CHEST_X, PersistentDataType.INTEGER, x);
		pdc.set(GOLEM_CHEST_Z, PersistentDataType.INTEGER, z);
	}

	private Location getLastUsedChestLocation(CopperGolem golem) {
		PersistentDataContainer pdc = golem.getPersistentDataContainer();
		if (!pdc.has(GOLEM_CHEST_X) || !pdc.has(GOLEM_CHEST_Z))
			return null;

		return new Location(golem.getWorld(), Double.valueOf(pdc.get(GOLEM_CHEST_X, PersistentDataType.INTEGER)), 0.0, Double.valueOf(pdc.get(GOLEM_CHEST_Z, PersistentDataType.INTEGER)));
	}
}
