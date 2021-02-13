package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownPreMergeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final Town remainingTown;
	boolean cancelled;
	private String cancelMessage = Translation.of("msg_town_merge_cancelled");

	public TownPreMergeEvent(Town town, Town remainingTown) {
		this.town = town;
		this.remainingTown = remainingTown;
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

	public Town getRemainingTown() {
		return remainingTown;
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
