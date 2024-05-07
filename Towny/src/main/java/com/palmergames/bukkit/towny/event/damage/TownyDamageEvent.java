package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * 
 * @author LlmDl
 */
public abstract class TownyDamageEvent extends Event implements Cancellable {
	protected final Entity entity;
	protected final Location loc;
	protected final DamageCause cause;
	protected final TownBlock townblock;
	protected boolean cancelled;
	protected String message;

	public TownyDamageEvent(Location loc, Entity entity, DamageCause cause, TownBlock townblock, boolean cancelled) {
		this.entity = entity;
		this.loc = loc;
		this.cause = cause;
		this.townblock = townblock;
		setCancelled(cancelled);
	}

	/**
	 * Whether the event has been cancelled.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Set the event to cancelled. False meaning damage, True meaning damage will be prevented.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	/**
	 * The {@link com.palmergames.bukkit.towny.object.Town} where the damage has happened.
	 * 
	 * @return town or null;
	 */
	@Nullable
	public Town getTown() {
		return townblock == null ? null : townblock.getTownOrNull();
	}

	/**
	 * @return Location of the entity being damaged.
	 */
	public Location getLocation() {
		return loc;
	}

	/**
	 * @return Entity being damaged.
	 */
	public Entity getEntity() {
		return entity;
	}
	
	/**
	 * The {@link com.palmergames.bukkit.towny.object.TownBlock} this action occured in,
	 * or null if in the wilderness.
	 * @return TownBlock or null. 
	 */
	@Nullable
	public TownBlock getTownBlock() {
		return townblock;
	}

	/**
	 * Did this action occur in the wilderness?
	 * 
	 * @return return true if this was in the wilderness.
	 */
	public boolean isInWilderness() {
		return townblock == null;
	}
	
	/**
	 * Did this action occur inside of a town's townblock?
	 * 
	 * @return true if this has a townblock.
	 */
	public boolean hasTownBlock() {
		return townblock != null;
	}

	/**
	 * @return cancellation message shown when the damage is cancelled.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message Message shown when the damage is cancelled.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * DamageCause of damage.
	 * @return DamageCause.
	 */
	public DamageCause getCause() {
		return cause;
	}
	
}
