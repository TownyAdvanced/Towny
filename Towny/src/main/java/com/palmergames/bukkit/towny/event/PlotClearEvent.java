package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class PlotClearEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final TownBlock townBlock;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public PlotClearEvent(TownBlock _townBlock) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.townBlock = _townBlock;
	}

	/**
	 * @return the new TownBlock.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}

}