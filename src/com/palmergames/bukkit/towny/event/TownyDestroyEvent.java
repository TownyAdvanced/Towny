package com.palmergames.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * Destroy events are thrown when a player attempts to destroy blocks in the
 * world, or would otherwise be using an item or block in a way that could be
 * considered a destroy event.
 * 
 * @author LlmDl
 */
public class TownyDestroyEvent extends TownyActionEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * Destroy event thrown when a player attempts to destroy blocks in the world.
	 * 
	 * This will be thrown even if Towny has already decided to cancel the event,
	 * giving other plugins (and Towny's internal war system) the chance to modify
	 * the outcome.
	 * 
	 * If you do not intend to un-cancel something already cancelled by Towny, use
	 * ignorecancelled=true in order to get only events which Towny will otherwise
	 * allow.
	 * 
	 * @param player    involved in the destroy event.
	 * @param loc       location of the block being destroyed.
	 * @param mat       material of the block being destroyed.
	 * @param cancelled true if Towny has already determined this will be cancelled.
	 */
	public TownyDestroyEvent(Player player, Location loc, Material mat, boolean cancelled) {
		super(player, loc, mat, cancelled);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
