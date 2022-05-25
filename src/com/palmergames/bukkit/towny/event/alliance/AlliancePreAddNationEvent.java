package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AlliancePreAddNationEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled = false;
	private final String nationName;
	private final Nation nation;
	private final String allianceName;
	private final Alliance alliance;
	private String cancelMessage = "Sorry, this event was cancelled.";

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public AlliancePreAddNationEvent(Alliance alliance, Nation ally) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nationName = ally.getName();
		this.nation = ally;
		this.alliance = alliance;
		this.allianceName = alliance.getName();
	}

	public String getAllyName() {
		return nationName;
	}

	public String getNationName() {
		return allianceName;
	}

	public Nation getAlly() {
		return nation;
	}

	public Alliance getAlliance() {
		return alliance;
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
