package com.palmergames.bukkit.towny.event.town;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownRemoveEnemiedTownEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	boolean cancelled;
	private final Town town;
	private final Town removedEnemy;
	private String cancelMessage = "A town removing an another town as an enemy was cancelled by another plugin.";
	
	public TownRemoveEnemiedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.removedEnemy = newAlly;
	}

	/**
	 * @return town Town which is removing an enemy.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return removedEnemy Town which is being removed as an enemy.
	 */
	public Town getRemovedEnemy() {
		return removedEnemy;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}
	
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
	
	public String getCancelMessage() { return this.cancelMessage; }

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
