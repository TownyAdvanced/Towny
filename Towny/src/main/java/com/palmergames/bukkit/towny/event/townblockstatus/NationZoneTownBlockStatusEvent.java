package com.palmergames.bukkit.towny.event.townblockstatus;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

public class NationZoneTownBlockStatusEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private Town town;

	/**
	 * Event thrown when Towny would return a TownBlockStatus of NationZone,
	 * used by the Towny PlayerCache, which would provide a protective bubble
	 * surrounding towns which belong to nations. Cancelling this event will
	 * result in the TownBlockStatus returning as UnclaimedZone (normal wild-
	 * erness) instead.
	 * 
	 * @param town Town which is surrounded by a valid NationZone.
	 */
	public NationZoneTownBlockStatusEvent(Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
	}
	
	public Town getTown() {
		return town;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
