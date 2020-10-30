package com.palmergames.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 
 * @author LlmDl
 */
public class TownySwitchEvent extends Event implements Cancellable, TownyActionEvent {

	private Player player;
	private Location loc;
	private Material mat;
	private boolean cancelled;
	private String message;
	private static final HandlerList handlers = new HandlerList();

	/**
	 * Switch event thrown when a player attempts to use a block that
	 * is in the Towny config's switch_use_ids list. These are typically
	 * inventory blocks, or toggle-able blocks like levers, doors, buttons.
	 * 
	 * This will be thrown even if Towny has already decided to cancel
	 * the event, giving other plugins (and Towny's internal war system)
	 * the chance to modify the outcome.
	 * 
	 * If you do not intend to un-cancel something already cancelled by Towny,
	 * use ignorecancelled=true in order to get only events which Towny
	 * will otherwise allow.
	 * 
	 * @param player involved in the itemuse event.
	 * @param loc location of the block which has an item being used on it.
	 * @param mat material of the item being used.
	 * @param cancelled true if Towny has already determined this will be cancelled.
	 */
	public TownySwitchEvent(Player player, Location loc, Material mat, boolean cancelled) {
		this.player = player;
		this.loc = loc;
		this.mat = mat;
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
	 * Set the event to cancelled.
	 */
	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	/**
	 * @return Material of the block being switched/toggled.
	 */
	public Material getMaterial() {
		return mat;
	}

	/**
	 * @return Location of the block being switched/toggled.
	 */
	public Location getLocation() {
		return loc;
	}

	/**
	 * @return player involved in the  switch event.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return cancellation message shown to players when their switch attempt is cancelled or null.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message Message shown to players when their switch attempts is cancelled.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
