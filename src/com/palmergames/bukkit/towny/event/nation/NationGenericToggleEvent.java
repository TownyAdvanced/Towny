package com.palmergames.bukkit.towny.event.nation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;

public class NationGenericToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Nation nation;
	private String[] args;
	private boolean isCancelled = false;
	private String cancellationMsg = "You are unable to use this command.";
	
	/**
	 * A generic cancellable event thrown when a player uses the /nation toggle {args} command.
	 * 
	 * @param player Player who has run the command.
	 * @param nation Nation which will have something cancelled.
	 * @param args String[] of one or more words that could be /nation toggle subcommands.
	 */
	public NationGenericToggleEvent(Player player, Nation nation, String[] args) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.nation = nation;
		this.args = args;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public HandlerList getHandlerList() {
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

	public Nation getnation() {
		return nation;
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
