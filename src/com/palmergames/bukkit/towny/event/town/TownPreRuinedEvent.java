package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event which is fired before Towny puts a Town into a ruined status. If
 * this event is cancelled, Towny will move on to deleting the Town.
 * 
 * @author LlmDl
 * @since 0.98.2.6
 */
public class TownPreRuinedEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	boolean cancelled;

	public TownPreRuinedEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	public Town getTown() {
		return town;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
