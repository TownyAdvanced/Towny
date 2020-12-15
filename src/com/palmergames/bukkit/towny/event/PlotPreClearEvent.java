package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotPreClearEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;
	private final String cancelMessage = "Sorry this event was cancelled";
	private final TownBlock townBlock;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public PlotPreClearEvent(TownBlock _townBlock) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.townBlock = _townBlock;
	}

	/**
	 * @return the new TownBlock.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}
}
