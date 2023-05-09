package com.palmergames.bukkit.towny.confirmations.event;

import com.palmergames.bukkit.towny.confirmations.Confirmation;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;

public abstract class ConfirmationEvent extends Event {
	private final Confirmation confirmation;
	private final CommandSender sender;
	
	public ConfirmationEvent(Confirmation confirmation, CommandSender sender) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.confirmation = confirmation;
		this.sender = sender;
	}

	/**
	 * @return The {@link Confirmation} associated with this event.
	 */
	public Confirmation getConfirmation() {
		return confirmation;
	}

	/**
	 * @return The {@link CommandSender} associated with this event.
	 */
	public CommandSender getSender() {
		return sender;
	}
}
