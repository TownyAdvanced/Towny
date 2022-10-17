package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class TownPreAddResidentEvent extends CancellableTownyEvent {

	private final String townName;
	private final Town town;
	private final Resident resident;
	
	public TownPreAddResidentEvent(Town town, Resident resident) {
		this.town = town;
		this.townName = town.getName();
		this.resident = resident;
	}

	public String getTownName() {
		return townName;
	}

	public Town getTown() { return town; }

	public Resident getResident() { return resident; }
}
