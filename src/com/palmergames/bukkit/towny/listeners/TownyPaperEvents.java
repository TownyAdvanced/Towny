package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.projectiles.BlockProjectileSource;

import java.util.function.Consumer;

public class TownyPaperEvents implements Listener {
	private final Towny plugin;
	
	public TownyPaperEvents(Towny plugin) {
		this.plugin = plugin;
	}
	
	public void register() {
		registerEvent("com.destroystokyo.paper.event.block.TNTPrimeEvent", TNTPrimeEvent, EventPriority.NORMAL, true);
	}
	
	private void registerEvent(String className, Consumer<Event> executor, EventPriority eventPriority, boolean ignoreCancelled) {
		try {
			Class<? extends Event> eventClass = Class.forName(className).asSubclass(Event.class);
			Bukkit.getPluginManager().registerEvent(eventClass, this, eventPriority, (listener, event) -> executor.accept(event), plugin, ignoreCancelled);
		} catch (Exception ignored) {}
	}
	
	// https://papermc.io/javadocs/paper/1.18/com/destroystokyo/paper/event/block/TNTPrimeEvent.html
	private final Consumer<Event> TNTPrimeEvent = (event) -> {
		Entity primerEntity = null;
		try {
			primerEntity = (Entity) event.getClass().getDeclaredMethod("getPrimerEntity").invoke(event);
		} catch (NoSuchMethodException e) {
			// Should not happen, unless the getPrimerEntity method is renamed.
			e.printStackTrace();
			return;
		} catch (Exception ignored) {}

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
}
