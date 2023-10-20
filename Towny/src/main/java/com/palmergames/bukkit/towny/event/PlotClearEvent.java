package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is no longer called.
 * @deprecated since 0.99.6.4 use {@link com.palmergames.bukkit.towny.event.plot.PlotClearEvent} instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.plot package.")
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