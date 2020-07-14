package com.palmergames.bukkit.towny.confirmations;

import org.bukkit.command.CommandSender;

/**
 * A class responsible for assembling confirmations.
 */
public class ConfirmationBuilder {
	Runnable acceptHandler;
	Runnable cancelHandler;
	String title;
	int duration = 20;
	boolean runAsync;

	/**
	 * The code to run on cancellation.
	 * 
	 * @param cancelHandler The runnable to run on cancellation of the confirmation.
	 * @return A builder reference of this object.
	 */
	public ConfirmationBuilder runOnCancel(Runnable cancelHandler) {
		this.cancelHandler = cancelHandler;
		return this;
	}

	/**
	 * Sets the title of the confirmation to be sent.
	 * 
	 * @param title The title of the confirmation.
	 * @return A builder reference of this object.
	 */
	public ConfirmationBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Sets the duration the confirmation will run for. 
	 * 
	 * @param duration The duration in second.
	 * @return A builder reference of this object.
	 */
	public ConfirmationBuilder setDuration(int duration) {
		this.duration = duration;
		return this;
	}

	/**
	 * Sets whether the confirmation will run it's accept handler
	 * async or not.
	 * 
	 * @param runAsync Whether to run async or not.
	 * @return A builder reference of this object.
	 */
	public ConfirmationBuilder setAsync(boolean runAsync) {
		this.runAsync = runAsync;
		return this;
	}
	
	/**
	 * Builds a new instance of {@link Confirmation} from 
	 * this object's state.
	 * 
	 * @return A new Confirmation object.
	 */
	public Confirmation build() {
		return new Confirmation(this);
	}

	/**
	 * Builds and sends this confirmation to the given CommandSender.
	 * 
	 * @param sender The sender to send the confirmation to.
	 */
	public void sendTo(CommandSender sender) {
		Confirmation confirmation = build();
		ConfirmationHandler.sendConfirmation(sender, confirmation);
	}
}
