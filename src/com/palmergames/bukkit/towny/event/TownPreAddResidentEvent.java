package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownPreAddResidentEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private String townName;
	private Town town;
	private Resident resident;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";
	
	public TownPreAddResidentEvent(Town town, Resident resident) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.townName = town.getName();
		this.resident = resident;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return null;
	}

	public String getTownName() {
		return townName;
	}

	public Town getTown() { return town; }

	public boolean isCancelled() {
		return isCancelled;
	}

	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public Resident getResident() { return resident; }
}
