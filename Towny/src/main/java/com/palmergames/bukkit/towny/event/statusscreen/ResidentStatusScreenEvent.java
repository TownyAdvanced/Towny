package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

public class ResidentStatusScreenEvent extends StatusScreenEvent {

	private Resident resident;
	
	public ResidentStatusScreenEvent(StatusScreen screen, Resident resident) {
		super(screen);
		this.resident = resident;
	}
	
	public Resident getResident() {
		return resident;
	}
	
}
