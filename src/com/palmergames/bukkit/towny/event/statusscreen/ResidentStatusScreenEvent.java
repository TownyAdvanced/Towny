package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Resident;

public class ResidentStatusScreenEvent extends StatusScreenEvent {

	private Resident resident;
	
	public ResidentStatusScreenEvent(Resident resident) {
		this.resident = resident;
	}
	
	public Resident getResident() {
		return resident;
	}
	
}
