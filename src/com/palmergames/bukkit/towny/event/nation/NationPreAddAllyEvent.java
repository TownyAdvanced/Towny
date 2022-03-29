package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationPreAddAllyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;
	private final String allyName;
	private final Nation ally;
	private final String nationName;
	private final Nation nation;
	private String cancelMessage = "Sorry, this event was cancelled.";

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public NationPreAddAllyEvent(Nation nation, Nation ally) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.allyName = ally.getName();
		this.ally = ally;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getAllyName() {
		return allyName;
	}

	public String getNationName() {
		return nationName;
	}

	public Nation getAlly() {
		return ally;
	}

	public Nation getNation() {
		return nation;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
