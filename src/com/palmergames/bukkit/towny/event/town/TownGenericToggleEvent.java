package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class TownGenericToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Town town;
	private final String[] args;
	private boolean isCancelled = false;
	private String cancellationMsg = "You are unable to use this command.";
	
	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param player Player who has run the command.
	 * @param town Town which will have something cancelled.
	 * @param args String[] of one or more words that could be /town toggle subcommands.
	 */
	public TownGenericToggleEvent(Player player, Town town, String[] args) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.town = town;
		this.args = args;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	public Player getPlayer() {
		return player;
	}

	public Town getTown() {
		return town;
	}

	public String[] getArgs() {
		return args;
	}

	public String getCancellationMsg() {
		return cancellationMsg;
	}

	public void setCancellationMsg(String cancellationMsg) {
		this.cancellationMsg = cancellationMsg;
	}
}
