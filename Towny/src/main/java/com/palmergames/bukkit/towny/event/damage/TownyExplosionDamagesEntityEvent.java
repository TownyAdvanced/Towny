package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyExplosionDamagesEntityEvents are thrown when an explosion 
 * occurs in a Towny world, causing damage to an entity.
 * 
 * <br> - When an entity explosion, block explosion or lightning damages an entity.
 * <br> - When an explosion would damage a Hanging entity.
 * <br> - When a pig is zapped by lightning.
 * <br> - When an explosion would damage a Vehicle.
 * 
 * @author LlmDl
 */
public class TownyExplosionDamagesEntityEvent extends TownyDamageEvent {

	private static final HandlerList handlers = new HandlerList();
	
	/**
	 * Event thrown when an explosion damages an entity. 
	 * Use ignorecancelled = true in order to filter out explosions Towny will already have stopped.
	 * 
	 * @param location - Location of the entity being damaged.
	 * @param harmedEntity - Entity getting exploded.
	 * @param cause - DamageCause.
	 * @param townblock - TownBlock or null if in the wilderness.
	 * @param cancelled - Whether Towny will cancel this already.
	 */
	public TownyExplosionDamagesEntityEvent(Location location, Entity harmedEntity, DamageCause cause, TownBlock townblock, boolean cancelled) {
		super(location, harmedEntity, cause, townblock, cancelled);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}
}
