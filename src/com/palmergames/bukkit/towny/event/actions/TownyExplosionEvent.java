package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyExplosionEvents are thrown when:
 *  - A whither would be exploding blocks.
 *  
 * @author LlmDl
 */
public class TownyExplosionEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Location location;
	private boolean canExplode;

	public TownyExplosionEvent(Location location, boolean canExplode) {
		this.location = location;
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
}
