package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreNewNationEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	
	private final Town town;
	private final String nationName;
	private boolean isCancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public PreNewNationEvent(Town town, String nationName) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.nationName = nationName;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public String getNationName() {
		return nationName;
	}
}
