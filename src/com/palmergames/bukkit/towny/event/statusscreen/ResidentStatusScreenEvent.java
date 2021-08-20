package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Resident;

import java.util.List;

public class ResidentStatusScreenEvent extends StatusScreenEvent {

	private Resident resident;
	
	public ResidentStatusScreenEvent(Resident resident, List<String> originalLines) {
		super(originalLines);
		this.resident = resident;
	}
	
	public Resident getResident() {
		return resident;
	}
	
}
