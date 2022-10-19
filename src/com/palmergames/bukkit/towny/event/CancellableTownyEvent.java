package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.confirmations.ConfirmationBuilder;
import com.palmergames.bukkit.towny.exceptions.CancelledEventException;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * A class extended by Towny Events which are Cancellable and which contain a
 * cancelMessage.
 * 
 * Having a getCancelMessage() available to us allows us to throw a
 * {@link CancelledEventException} which can use the cancelMessage in feedback
 * messaging or logging.
 * 
 * Used primarily in the
 * {@link ConfirmationBuilder#setCancellableEvent(CancellableTownyEvent)} and
 * {@link BukkitTools#ifCancelledThenThrow(CancellableTownyEvent)} code.
 * 
 * @author LlmDL
 * @since 0.98.4.0.
 */
public abstract class CancellableTownyEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";

	public CancellableTownyEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * @return true if this event is cancelled.
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Sets the event to be cancelled or not.
	 * 
	 * @param cancelled true will cancel the event.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	/**
	 * @return cancelMessage a String which will be displayed to the player/sender
	 *         informing them that the action they commited is being denied, if
	 *         cancelMessage is blank then no message will be displayed.
	 */
	public String getCancelMessage() {
		return cancelMessage;
	}

	/**
	 * Sets the cancellation message which will display to the player. Set to "" to
	 * display nothing.
	 * 
	 * @param msg cancelMessage to display as feedback.
	 */
	public void setCancelMessage(String msg) {
		this.cancelMessage = msg;
	}

}