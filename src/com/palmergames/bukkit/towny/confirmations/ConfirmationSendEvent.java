package com.palmergames.bukkit.towny.confirmations;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a {@link Confirmation} is about to be sent to a {@link CommandSender}
 */
public class ConfirmationSendEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Confirmation confirmation;
	private final CommandSender sender;
	
	public ConfirmationSendEvent(Confirmation confirmation, CommandSender sender) {
		this.confirmation = confirmation;
		this.sender = sender;
	}

	/**
	 * @return The {@link Confirmation} that is about to be sent.
	 */
	public Confirmation getConfirmation() {
		return confirmation;
	}

	/**
	 * @return The {@link CommandSender} that will receive the confirmation.
	 */
	public CommandSender getSender() {
		return sender;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
