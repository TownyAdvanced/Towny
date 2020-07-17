package com.palmergames.bukkit.towny.confirmations;

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
	private final String title;
	private final int duration;
	private final boolean isAsync;

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
		this.isAsync = builder.runAsync;
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
	public String getTitle() {
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
	 * Whether the handers of this confirmation will run async or not.
	 * 
	 * @return true if async, false otherwise.
	 */
	public boolean isAsync() {
		return isAsync;
	}
}
