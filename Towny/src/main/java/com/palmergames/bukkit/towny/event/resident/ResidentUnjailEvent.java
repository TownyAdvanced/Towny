package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentUnjailEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final UnJailReason reason;

	public ResidentUnjailEvent(Resident resident, UnJailReason reason){

		this.resident = resident;
		this.reason = reason;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Resident getResident() {
		return resident;
	}

	public UnJailReason getReason() {
		return reason;
	}
}
