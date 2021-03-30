package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownPreMergeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
	private final Town remainingTown;
	private final Town succumbingTown;
	boolean cancelled;
	private String cancelMessage = Translation.of("msg_town_merge_cancelled");

	public TownPreMergeEvent(Town remainingTown, Town succumbingTown) {
		this.remainingTown = remainingTown;
		this.succumbingTown = succumbingTown;
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
	
	public String getCancelMessage() {
		return cancelMessage;
	}

	public Town getRemainingTown() {
		return remainingTown;
	}

	public Town getSuccumbingTown() {
		return succumbingTown;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
