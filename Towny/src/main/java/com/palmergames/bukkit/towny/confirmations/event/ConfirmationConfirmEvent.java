package com.palmergames.bukkit.towny.confirmations.event;

import com.palmergames.bukkit.towny.confirmations.Confirmation;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a {@link Confirmation} is confirmed using /confirm.
 */
public class ConfirmationConfirmEvent extends ConfirmationEvent implements Cancellable {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = "";
	
	public ConfirmationConfirmEvent(Confirmation confirmation, CommandSender sender) {
		super(confirmation, sender);
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
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
