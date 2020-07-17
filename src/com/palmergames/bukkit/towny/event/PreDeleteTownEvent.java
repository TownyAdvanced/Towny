package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/*
 * @author LlmDl
 *
 */

public class PreDeleteTownEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final String townName;
	private final Town town;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public PreDeleteTownEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.townName = town.getName();
		this.town = town;
	}

	/**
	 * @return the deleted town name.
	 */
	public String getTownName() {
		return townName;
	}

	/**
	 * @return the deleted town object.
	 */
	public Town getTown() {
		return town;
	}

	@Override
	public boolean isCancelled() { return isCancelled; }

	@Override
	public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
	
	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
