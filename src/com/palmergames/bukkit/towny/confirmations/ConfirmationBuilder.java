package com.palmergames.bukkit.towny.confirmations;

import org.bukkit.command.CommandSender;

public class ConfirmationBuilder {
	Runnable acceptHandler;
	Runnable cancelHandler;
	String title;
	int duration;
	boolean runAsync;
	
	public ConfirmationBuilder onCancel(Runnable cancelHandler) {
		this.cancelHandler = cancelHandler;
		return this;
	}
	
	public ConfirmationBuilder setTitle(String title) {
		this.title = title;
		return this;
	}
	
	public ConfirmationBuilder setDuration(int duration) {
		this.duration = duration;
		return this;
	}
	
	public ConfirmationBuilder setAsync(boolean runAsync) {
		this.runAsync = runAsync;
		return this;
	}

	public Runnable getAcceptHandler() {
		return acceptHandler;
	}
	
	public Runnable getCancelHandler() {
		return cancelHandler;
	}

	public String getTitle() {
		return title;
	}

	public int getDuration() {
		return duration;
	}
	
	public Confirmation build() {
		return new Confirmation(this);
	}
	
	public void sendTo(CommandSender sender) {
		Confirmation confirmation = build();
		ConfirmationHandler.sendConfirmation(sender, confirmation);
	}
}
