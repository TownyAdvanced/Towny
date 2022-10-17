package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Translatable;

/**
 * An object which stores information about confirmations. While this 
 * object itself is immutable and threadsafe, async operations within
 * its handlers may not be thus, use async judiciously.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class Confirmation {
	
	private final Runnable acceptHandler;
	private final Runnable cancelHandler;
	private final Translatable title;
	private final int duration;
	private final ConfirmationTransaction transaction;
	private final String confirmCommand;
	private final String cancelCommand;
	private final boolean isAsync;
	private String pluginPrefix;
	private final CancellableTownyEvent event;

	/**
	 * Creates a new {@link ConfirmationBuilder} with the supplied accept handler.
	 * 
	 * @param acceptHandler The runnable to run on accepting the confirmation.
	 * @return A new confirmation builder with the given accept handler.
	 */
	public static ConfirmationBuilder runOnAccept(Runnable acceptHandler) {
		ConfirmationBuilder builder = new ConfirmationBuilder();
		builder.acceptHandler = acceptHandler;
		return builder;
	}

	/**
	 * Creates a new {@link ConfirmationBuilder} with the given
	 * accept handler.
	 * 
	 * @param acceptHandler The runnable to run when the confirmation is accepted.
	 * @return A new builder with the given accept handler.
	 */
	public static ConfirmationBuilder runOnAcceptAsync(Runnable acceptHandler) {
		ConfirmationBuilder builder = new ConfirmationBuilder();
		builder.acceptHandler = acceptHandler;
		builder.runAsync = true;
		return builder;
	}

	/**
	 * Internal use only.
	 * 
	 * @param builder The builder to construct from.
	 */
	protected Confirmation(ConfirmationBuilder builder) {
		this.acceptHandler = builder.acceptHandler;
		this.cancelHandler = builder.cancelHandler;
		this.title = builder.title;
		this.duration = builder.duration;
		this.transaction = builder.transaction;
		this.isAsync = builder.runAsync;
		this.confirmCommand = builder.confirmCommand;
		this.cancelCommand = builder.cancelCommand;
		this.pluginPrefix = builder.pluginPrefix;
		this.event = builder.event;
	}
	
	/**
	 * Gets the handler that contains the code to run on
	 * completion.
	 * 
	 * @return The handler
	 */
	public Runnable getAcceptHandler() {
		return acceptHandler;
	}

	/**
	 * Gets the handler that contains the code to run
	 * on cancellation.
	 * 
	 * @return The handler.
	 */
	public Runnable getCancelHandler() {
		return cancelHandler;
	}

	/**
	 * Gets the title of the confirmation message.
	 * 
	 * @return The title of the confirmation message.
	 */
	public Translatable getTitle() {
		return title;
	}
	
	/**
	 * Gets the duration (in seconds) of this confirmation.
	 * 
	 * @return The duration in seconds.
	 */
	public int getDuration() {
		return duration;
	}
	
	/**
	 * @return True when there is a ConfirmationTransaction.
	 */
	public boolean hasCost() {
		return transaction != null;
	}

	/**
	 * Gets the ConfirmationTransaction.
	 * @return the transaction.
	 */
	public ConfirmationTransaction getTransaction() {
		return transaction;
	}

	public String getConfirmCommand() {
		return confirmCommand;
	}
	
	public String getCancelCommand() {
		return cancelCommand;
	}
	
	public String getPluginPrefix() {
		return pluginPrefix;
	}
	
	public CancellableTownyEvent getEvent() {
		return event;
	}

	/**
	 * Whether the handers of this confirmation will run async or not.
	 * 
	 * @return true if async, false otherwise.
	 */
	public boolean isAsync() {
		return isAsync;
	}
}
