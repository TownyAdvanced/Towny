package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NationSpawnEvent extends Event implements Cancellable {
	
	private Nation toNation;
	private Nation fromNation;
	private Location from;
	private Location to;
	private Player player;
	private String cancelMessage = "Sorry, this event was canceled.";
	private static final HandlerList handlers = new HandlerList();
	boolean isCancelled;
	
	public NationSpawnEvent(Player player, Location from, Location to) {
		this.player = player;
		this.to = to;
		this.from = from;

		try {
			fromNation = WorldCoord.parseWorldCoord(from).getTownBlock().getTown().getNation();
		} catch (NotRegisteredException ignored) {}

		try {
			toNation = WorldCoord.parseWorldCoord(to).getTownBlock().getTown().getNation();
		} catch (NotRegisteredException ignored) {}
	}

	public Nation getToNation() {
		return toNation;
	}

	public Nation getFromNation() {
		return fromNation;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Location getFrom() {
		return from;
	}

	public Location getTo() {
		return to;
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		isCancelled = cancel;
	}
}
