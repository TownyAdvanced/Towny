package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentJailEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final String jailTown;
	
	public ResidentJailEvent(Resident resident, String jailTown){

		this.resident = resident;
		this.jailTown = jailTown;
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

	public String getJailTown() {
		return jailTown;
	}
}
