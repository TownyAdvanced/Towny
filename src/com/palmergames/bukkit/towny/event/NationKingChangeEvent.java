package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationKingChangeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Nation nation;
	private Resident king;
	private boolean isCancellable;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationKingChangeEvent(Nation nation, Resident newKing, boolean isCancellable) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.king = newKing;
		this.isCancellable = isCancellable;
	}


	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Nation getNation() {
		return nation;
	}

	public Resident getNewKing() {
		return king;
	}

	@Override
	public boolean isCancelled() {
		return isCancellable && isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		if (isCancellable)
			isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public boolean isCancellable() {
		return isCancellable;
	}
}
