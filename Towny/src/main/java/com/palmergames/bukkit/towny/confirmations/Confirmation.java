package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BooleanSupplier;

/**
 * An object which stores information about confirmations. While this 
 * object itself is immutable and threadsafe, async operations within
 * its handlers may not be thus, use async judiciously.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class Confirmation {
	
	private final BooleanSupplier acceptHandler;
	private final Runnable cancelHandler;
	private final Translatable title;
	private final int duration;
	private final ConfirmationTransaction transaction;
	private final String confirmCommand;
	private final String cancelCommand;
	private final boolean isAsync;
	private String pluginPrefix;
	private final CancellableTownyEvent event;
	private final boolean serious;

	/**
	 * Creates a new {@link ConfirmationBuilder} with the supplied accept handler.
	 * 
	 * @param acceptHandler The runnable to run on accepting the confirmation.
	 * @return A new confirmation builder with the given accept handler.
	 */
	public static ConfirmationBuilder runOnAccept(Runnable acceptHandler) {
		return runOnAccept(() -> {
			acceptHandler.run();
			return true;
		});
	}

	/**
	 * Creates a new {@link ConfirmationBuilder} with the given
	 * accept handler.
	 * 
	 * @param acceptHandler The runnable to run when the confirmation is accepted.
	 * @return A new builder with the given accept handler.
	 */
	public static ConfirmationBuilder runOnAcceptAsync(Runnable acceptHandler) {
		return runOnAccept(acceptHandler).setAsync(true);
	}

	/**
	 * Creates a new {@link ConfirmationBuilder} with the supplied accept handler.
	 *	 
	 * @param acceptHandler The boolean supplier to call upon the confirmation being accepted.
	 *   The value returned by the supplier is whether the action was successfully performed, and is used for refunding what was paid if not.
	 * @return A new confirmation builder with the given accept handler.
	 * @see ConfirmationBuilder#setCost(ConfirmationTransaction) 
	 */
	public static ConfirmationBuilder runOnAccept(BooleanSupplier acceptHandler) {
		final ConfirmationBuilder builder = new ConfirmationBuilder();
		builder.acceptHandler = acceptHandler;
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
		this.serious = builder.serious;
	}
	
	/**
	 * Gets the handler that contains the code to run on
	 * completion.
	 * 
	 * @return The handler
	 */
	public Runnable getAcceptHandler() {
		return acceptHandler::getAsBoolean;
	}

	@ApiStatus.Internal
	public BooleanSupplier acceptHandler() {
		return this.acceptHandler;
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

	public boolean isSerious() {
		return serious;
	}
}
