package com.palmergames.bukkit.towny.event.town;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownAddEnemiedTownEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	boolean cancelled;
	private final Town town;
	private final Town newEnemy;
	private String cancelMessage = "A town enemying another town was cancelled by another plugin.";
	
	public TownAddEnemiedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.newEnemy = newAlly;
	}

	/**
	 * @return town Town which is receiving a new enemy.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return newEnemy Town which is being added as a new enemy.
	 */
	public Town getNewEnemy() {
		return newEnemy;
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
