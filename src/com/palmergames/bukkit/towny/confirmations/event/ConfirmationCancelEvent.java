package com.palmergames.bukkit.towny.confirmations.event;

import com.palmergames.bukkit.towny.confirmations.Confirmation;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a {@link Confirmation} is successfully cancelled, either due to timing out or due to the sender cancelling it manually.
 */
public class ConfirmationCancelEvent extends ConfirmationEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final boolean timeout;
	
	public ConfirmationCancelEvent(Confirmation confirmation, CommandSender sender, boolean timeout) {
		super(confirmation, sender);
		this.timeout = timeout;
	}

	/**
	 * @return Whether this confirmation was cancelled due to timing out.
	 */
	public boolean isTimeout() {
		return timeout;
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
