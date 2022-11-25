package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ApiStatus.Internal
public class TownyPaperEvents implements Listener {
	private final Towny plugin;
	private MethodHandle getOrigin = null;
	private MethodHandle getPrimerEntity = null;
	
	public TownyPaperEvents(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void register() {
		initializeReflections();
		
		if (this.getPrimerEntity != null)
			registerEvent("com.destroystokyo.paper.event.block.TNTPrimeEvent", this::tntPrimeEvent, EventPriority.LOW, true);
		
		if (this.getOrigin != null)
			registerEvent(EntityChangeBlockEvent.class, fallingBlockListener(), EventPriority.LOW, true);
	}
	
	@SuppressWarnings("JavaReflectionMemberAccess")
	private void initializeReflections() {
		try {
			//https://jd.papermc.io/paper/1.19/org/bukkit/entity/Entity.html#getOrigin()
			this.getOrigin = MethodHandles.publicLookup().unreflect(Entity.class.getMethod("getOrigin"));
			TownyMessaging.sendDebugMsg("Entity#getOrigin found, using falling block listener.");
		} catch (ReflectiveOperationException ignored) {}
		
		try {
			// https://jd.papermc.io/paper/1.19/com/destroystokyo/paper/event/block/TNTPrimeEvent.html#getPrimerEntity()
			this.getPrimerEntity = MethodHandles.publicLookup().unreflect(Class.forName("com.destroystokyo.paper.event.block.TNTPrimeEvent").getMethod("getPrimerEntity"));
			TownyMessaging.sendDebugMsg("TNTPRimeEvent#getPrimerEntity method found, using TNTPrimeEvent listener.");
		} catch (ReflectiveOperationException ignored) {}
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
	
	// https://papermc.io/javadocs/paper/1.19/com/destroystokyo/paper/event/block/TNTPrimeEvent.html
	private Consumer<Event> tntPrimeEvent() {
		return event -> {
			Entity primerEntity;
			try {
				primerEntity = (Entity) getPrimerEntity.invoke(event);
			} catch (final Throwable e) {
				// Should not happen, unless the getPrimerEntity method is renamed.
				e.printStackTrace();
				return;
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
			if (event.getEntityType() != EntityType.FALLING_BLOCK || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
				return;
			
			Location origin;
			try {
				origin = (Location) getOrigin.invoke(event.getEntity());
			} catch (final Throwable e) {
				e.printStackTrace();
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
}
