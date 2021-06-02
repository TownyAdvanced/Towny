package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentPreJailEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	private final Jail jail;
	private final int cell;
	private final int hours;
	private final JailReason reason;
	private boolean isCancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public ResidentPreJailEvent(Resident resident, Jail jail, int cell, int hours, JailReason reason) {
		this.resident = resident;
		this.jail = jail;
		this.cell = cell;
		this.hours = hours;
		this.reason = reason;
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
	
	public Jail getJail() {
		return jail;
	}

	public Town getJailTown() {
		return jail.getTown();
	}
	
	public TownBlock getJailTownBlock() {
		return jail.getTownBlock();
	}
	
	public int getCell() {
		return cell;
	}

	public int getHours() {
		return hours;
	}

	public JailReason getReason() {
		return reason;
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
