package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A class which provides the basis for spawn events.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public abstract class SpawnEvent extends Event implements Cancellable {
	private final Location from;
	private final Location to;
	private final Player player;
	private String cancelMessage = "Sorry, this event was cancelled.";
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;

	/**
	 * Creates a Spawn event.
	 * 
	 * @param player The player spawning.
	 * @param from The from location.
	 * @param to The to location.
	 */
	public SpawnEvent(Player player, Location from, Location to) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.to = to;
		this.from = from;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the location from which the player is teleporting from.
	 * 
	 * @return The location being teleported from.
	 */
	public Location getFrom() {
		return from;
	}

	/**
	 * Gets the location to which the player is teleporting to.
	 *
	 * @return The location being teleported to.
	 */
	public Location getTo() {
		return to;
	}

	/**
	 * Gets the player whom is teleporting.
	 * 
	 * @return The player teleporting.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * The message sent when the event is cancelled.
	 * 
	 * @return The cancel message.
	 */
	public String getCancelMessage() {
		return cancelMessage;
	}

	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	/**
	 * Sets the cancel message for the event.
	 * 
	 * @param cancelMessage The cancel message to use when cancelling.
	 */
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
