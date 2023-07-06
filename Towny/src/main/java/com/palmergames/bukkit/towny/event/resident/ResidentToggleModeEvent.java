package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentToggleModeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
