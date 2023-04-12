package com.palmergames.bukkit.towny.confirmations.event;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a {@link Confirmation} is about to be sent to a {@link CommandSender}
 */
public class ConfirmationSendEvent extends ConfirmationEvent implements Cancellable {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = "";
	private boolean sendMessage = true;
	
	public ConfirmationSendEvent(Confirmation confirmation, CommandSender sender) {
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

	/**
	 * @return Whether the confirmation will be sent in chat using {@link TownyMessaging#sendConfirmationMessage(CommandSender, Confirmation)}.
	 */
	public boolean isSendingMessage() {
		return this.sendMessage;
	}
	
	public void setSendMessage(boolean sendMessage) {
		this.sendMessage = sendMessage;
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
