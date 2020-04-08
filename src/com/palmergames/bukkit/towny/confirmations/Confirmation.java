package com.palmergames.bukkit.towny.confirmations;

import org.bukkit.command.CommandSender;

/**
 * An object which stores information about confirmations.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public class Confirmation {
	
	private CommandSender sender;
	private Runnable handler;
	private String title;

	/**
	 * Creates a new confirmation object.
	 * 
	 * @param sender The sender the confirmation belongs to.
	 */
	public Confirmation(CommandSender sender) {
		this.sender = sender;
	}

	/**
	 * Creates a new confirmation object.
	 * 
	 * @param sender The sender the confirmation belongs to.
	 * @param handler The handler to run after accepting the command.
	 */
	public Confirmation(CommandSender sender, Runnable handler) {
		this(sender);
		this.setHandler(handler);
	}

	/**
	 * Gets the {@link CommandSender} whom the confirmation is for.
	 * 
	 * @return The confirmation's respective sender.
	 */
	public CommandSender getSender() {
		return sender;
	}

	/**
	 * Gets the handler that contains the code to run on
	 * completion.
	 * 
	 * @return The handler
	 */
	public Runnable getHandler() {
		return handler;
	}

	/**
	 * Sets the handler for when the confirmation is accepted.
	 * 
	 * @param handler The handler to run when the command is accepted.
	 */
	public void setHandler(Runnable handler) {
		this.handler = handler;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
