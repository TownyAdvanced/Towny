package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationPreMergeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Nation nation;
	private final Nation remainingNation;
	boolean cancelled;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationPreMergeEvent(Nation nation, Nation remainingNation) {
		this.nation = nation;
		this.remainingNation = remainingNation;
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

	public Nation getRemainingNation() {
		return remainingNation;
	}

	public Nation getNation() {
		return nation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}


}
