package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResidentPreJailEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final Town jailTown;
	private boolean isCancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public ResidentPreJailEvent(Resident resident, Town jailTown){

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
	
	@Nullable
	public Town getJailTown() {
		return jailTown;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
