package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationRemoveAllyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Nation nation;
	private final Nation removedNation;
	boolean cancelled;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationRemoveAllyEvent(Nation nation, Nation removedNation) {
		this.nation = nation;
		this.removedNation = removedNation;

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

	public Nation getRemovedNation() {
		return removedNation;
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
