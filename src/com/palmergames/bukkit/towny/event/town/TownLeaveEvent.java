package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public class TownLeaveEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	private final Town town;
	private final Resident resident;
	
	public TownLeaveEvent(Resident resident, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.resident = resident;
		this.town = town;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * @return Town which is about to lose the resident.
	 */
	public Town getTown() {
		return town;
	}
	
	/**
	 * @return Resident which is about to leave their town.
	 */
	public Resident getResident() {
		return resident;
	}

	/**
	 * @return String which is the error message shown to the player when this event is cancelled.
	 */
	public String getCancelMessage() {
		return cancelMessage;
	}

	/**
	 * Set a custom error message show to the player when the event is cancelled.
	 * 
	 * @param cancelMessage String which will be the error message.
	 */
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
