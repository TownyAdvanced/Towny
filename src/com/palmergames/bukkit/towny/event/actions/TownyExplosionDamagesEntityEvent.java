package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * Explosion events are thrown when an explosion occurs in a Towny world.
 * 
 * @author LlmDl
 */
public class TownyExplosionDamagesEntityEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Location location;
	private final Entity entity;
	private boolean canExplode;

	public TownyExplosionDamagesEntityEvent(Location location, Entity entity, boolean canExplode) {
		this.location = location;
		this.entity = entity;
		this.canExplode = canExplode;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return canExplode;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.canExplode = cancel;
	}

	public Location getLocation() {
		return location;
	}
	
	public Entity getEntity() {
		return entity;
	}
}
