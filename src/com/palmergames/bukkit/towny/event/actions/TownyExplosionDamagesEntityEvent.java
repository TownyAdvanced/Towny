package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyExplosionDamagesEntityEvents are thrown when an explosion 
 * occurs in a Towny world, causing damage to an entity.
 * 
 *  - When an entity explosion, block explosion or lightning damages an entity.
 *  - When an explosion would damage a Hanging entity.
 *  - When a pig is zapped by lightning.
 *  - When an explosion would damage a vehicle.
 * 
 * @author LlmDl
 * 
 * @param location - Location of the entity being damaged.
 * @param entity - Entity getting exploded.
 * @param cause - DamageCause.
 * @param canExplode - Whether Towny will cancel this already.
 */
public class TownyExplosionDamagesEntityEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Location location;
	private final Entity entity;
	private final DamageCause cause;
	private boolean canExplode;

	/**
	 * Event thrown when an explosion damages an entity.
	 * 
	 * @param location - Location of the entity being damaged.
	 * @param entity - Entity getting exploded.
	 * @param cause - DamageCause.
	 * @param canExplode - Whether Towny will cancel this already.
	 */
	public TownyExplosionDamagesEntityEvent(Location location, Entity entity, DamageCause cause, boolean canExplode) {
		this.location = location;
		this.entity = entity;
		this.cause = cause;
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
	
	public DamageCause getCause() {
		return cause;
	}
}
