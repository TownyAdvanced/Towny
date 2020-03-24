package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationCapitalChangeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Nation nation;
	private Town capital;
	private boolean isCancellable;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	public NationCapitalChangeEvent(Nation nation, Town capital, boolean isCancellable) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.capital = capital;
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
		return capital.getMayor();
	}

	public Town getNewCapital() {
		return capital;
	}

	@Override
	public boolean isCancelled() {
		return isCancellable ? isCancelled : isCancellable;
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
