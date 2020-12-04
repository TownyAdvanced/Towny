package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationRankAddEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private final Nation nation;
	private final Resident res;
	private final String rank;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled.";

	public NationRankAddEvent(Nation nation, String rank, Resident res) {
		this.nation = nation;
		this.rank = rank;
		this.res = res;
	}

	public Nation getNation() {
		return nation;
	}

	public Resident getResident() {
		return res;
	}

	public String getRank() {
		return rank;
	}

	public void setCancelMessage(String s) {
		cancelMessage = s;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.isCancelled = b;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
