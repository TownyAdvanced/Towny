package com.palmergames.bukkit.towny.confirmations;

/**
 * An object which stores information about confirmations. While this 
 * object itself is immutable and threadsafe, async operations within
 * its handlers may not be. Use async judiciously.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class Confirmation {
	
	private final Runnable acceptHandler;
	private final Runnable cancelHandler;
	private final String title;
	private final int duration;
	private final boolean isAsync;
	
	public static ConfirmationBuilder runOnAccept(Runnable acceptHandler) {
		ConfirmationBuilder builder = new ConfirmationBuilder();
		builder.acceptHandler = acceptHandler;
		return builder;
	}
	
	public static ConfirmationBuilder runOnAcceptAsync(Runnable acceptHandler) {
		ConfirmationBuilder builder = new ConfirmationBuilder();
		builder.acceptHandler = acceptHandler;
		builder.runAsync = true;
		return builder;
	}
	
	protected Confirmation(ConfirmationBuilder builder) {
		this.acceptHandler = builder.getAcceptHandler();
		this.cancelHandler = builder.getCancelHandler();
		this.title = builder.getTitle();
		this.duration = builder.getDuration();
		this.isAsync = builder.runAsync;
	}
	
	/**
	 * Creates a new confirmation object.
	 *
	 * @param acceptHandler The handler to run after accepting the command.
	 */
	public Confirmation(Runnable acceptHandler) {
		this.acceptHandler = acceptHandler;
		this.cancelHandler = null;
		this.duration = 20;
		this.title = null;
		this.isAsync = false;
	}

	/**
	 * Creates a new confirmation object.
	 *
	 * @param acceptHandler The handler to run after accepting the command.
	 * @param title The title of the confirmation message.
	 * @param duration The amount of time to allow for the confirmation to be processed.   
	 *          
	 */
	public Confirmation(Runnable acceptHandler, String title, int duration) {
		this.acceptHandler = acceptHandler;
		this.cancelHandler = null;
		this.title = title;
		this.duration = duration;
		this.isAsync = false;
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
