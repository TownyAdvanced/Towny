package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentToggleModeEvent extends Event implements Cancellable {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = "";
	private final Resident resident;
	private final String mode;
	private final boolean toggleOn;
	
	public ResidentToggleModeEvent(Resident resident, String mode, boolean toggleOn) {
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

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public void setCancelMessage(@NotNull String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	@NotNull
	public String getCancelMessage() {
		return cancelMessage;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
