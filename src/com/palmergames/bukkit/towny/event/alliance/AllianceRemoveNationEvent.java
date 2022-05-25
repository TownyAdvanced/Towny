package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AllianceRemoveNationEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Alliance alliance;
	private final Nation removedNation;
	boolean cancelled;
	private String cancelMessage = "Sorry this event was cancelled";

	public AllianceRemoveNationEvent(Alliance alliance, Nation removedNation) {
		this.alliance = alliance;
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

	public Alliance getAlliance() {
		return alliance;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}


}
