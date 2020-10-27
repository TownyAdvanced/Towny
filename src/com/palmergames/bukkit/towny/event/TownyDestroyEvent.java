package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyDestroyEvent extends Event implements Cancellable {

	private Player player;
	private Location loc;
	private Material mat;
	private boolean cancelled;
	private String message;
	private static final HandlerList handlers = new HandlerList();

	/**
	 * Destroy event thrown when a player attempts to destroy blocks in the world.
	 * 
	 * This will be thrown even if Towny has already decided to cancel
	 * the event, giving other plugins (and Towny's internal war system)
	 * the chance to modify the outcome.
	 * 
	 * If you do not intend to un-cancel something already cancelled by Towny,
	 * use ignorecancelled=true in order to get only events which Towny
	 * will otherwise allow.
	 * 
	 * @param player involved in the destroy event.
	 * @param loc location of the block being destroyed.
	 * @param mat material of the block being destroyed.
	 * @param cancelled true if Towny has already determined this will be cancelled.
	 */
	public TownyDestroyEvent(Player player, Location loc, Material mat, boolean cancelled) {
		super(!Bukkit.getServer().isPrimaryThread());
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

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * 
	 * @return Material of the block being destroyed.
	 */
	public Material getMaterial() {
		return mat;
	}

	/**
	 * 
	 * @return Location of the block being destroyed.
	 */
	public Location getLocation() {
		return loc;
	}

	/**
	 * 
	 * @return player involved in the destroy event.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * 
	 * @return cancellation message shown to players when their destroy attempt is cancelled or null.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 
	 * @param message Message shown to players when their destroy attempts is cancelled.
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
