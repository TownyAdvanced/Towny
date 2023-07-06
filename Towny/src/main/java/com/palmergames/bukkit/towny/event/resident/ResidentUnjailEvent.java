package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentUnjailEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;

	public ResidentUnjailEvent(Resident resident){

		this.resident = resident;
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
}
