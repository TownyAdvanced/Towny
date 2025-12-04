package com.palmergames.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A class which provides the basis for spawn events.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public abstract class SpawnEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Location from;
	private final Location to;
	private final Player player;
	private final double cost;

	/**
	 * Creates a Spawn event.
	 * 
	 * @param player The player spawning.
	 * @param from The from location.
	 * @param to The to location.
	 * @param cost The amount paid to teleport.
	 */
	public SpawnEvent(Player player, Location from, Location to, double cost) {
		this.player = player;
		this.to = to;
		this.from = from;
		this.cost = cost;
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
	 * Gets the cost that the player has paid to teleport.
	 * 
	 * @return the cost as a double.
	 */
	public double getCost() {
		return cost;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
