package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;

public class ResidentToggleModeEvent extends CancellableTownyEvent {

	private final Resident resident;
	private final String mode;
	private final boolean toggleOn;
	
	public ResidentToggleModeEvent(Resident resident, String mode) {
		this.resident = resident;
		this.mode = mode;
		this.toggleOn = !resident.hasMode(mode);
	}

	public Resident getResident() {
		return resident;
	}

	public String getMode() {
		return mode;
	}

	public boolean isTogglingOn() {
		return toggleOn;
	}
}
