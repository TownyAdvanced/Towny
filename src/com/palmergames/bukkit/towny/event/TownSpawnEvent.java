package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TownSpawnEvent extends Event implements Cancellable {
	
	Town fromTown;
	private Town toTown;
	private Location from;
	private Location to;
	private String cancelMessage = "Sorry, this event was canceled.";
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private boolean isCancelled;
	
	public TownSpawnEvent(Player player, Location from, Location to) {
		this.from = from;
		this.to = to;
		this.player = player;
		
		try {
			fromTown = WorldCoord.parseWorldCoord(from).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
		try {
			toTown = WorldCoord.parseWorldCoord(to).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
	}

	public Town getToTown() {
		return toTown;
	}

	public Town getFromTown() {
		return fromTown;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public Location getFrom() {
		return from;
	}

	public Location getTo() {
		return to;
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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}
}
