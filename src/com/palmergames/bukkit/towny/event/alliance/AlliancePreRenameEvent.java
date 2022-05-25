package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AlliancePreRenameEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	
	private final Alliance alliance;
	private final String newName;
	private boolean isCancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public AlliancePreRenameEvent(Alliance alliance, String newName) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.alliance = alliance;
		this.newName = newName;
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

	public Alliance getAlliance() {
		return alliance;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public String getNationName() {
		return newName;
	}
}
