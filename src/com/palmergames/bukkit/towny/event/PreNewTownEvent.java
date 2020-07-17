package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreNewTownEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player player;
	private final String townName;
	private boolean isCancelled = false;
	private String cancelMessage = "Sorry this event was cancelled";
	
	public PreNewTownEvent(Player player, String townName) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.townName = townName;
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

	public Player getPlayer() {
		return player;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public String getTownName() {
		return townName;
	}
}
