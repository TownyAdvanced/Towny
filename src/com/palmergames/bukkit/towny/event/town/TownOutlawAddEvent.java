package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownOutlawAddEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final Resident outlawedResident;
	private final CommandSender sender;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	private boolean cancelled = false;
	
	public TownOutlawAddEvent(CommandSender sender, Resident outlawedResident, Town town) {
		super(!Bukkit.isPrimaryThread());
		this.town = town;
		this.outlawedResident = outlawedResident;
		this.sender = sender;
	}

	/**
	 * @return The town where the resident is being added as an outlaw.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return The resident that is being added as an outlaw.
	 */
	public Resident getOutlawedResident() {
		return outlawedResident;
	}

	@NotNull
	public CommandSender getSender() {
		return sender;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
