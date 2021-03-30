package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreDeleteNationEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	
	private final String nationName;
	private final Nation nation;
	private boolean isCancelled = false;
	
	public PreDeleteNationEvent(Nation nation) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.nationName = nation.getName();
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	/**
	 *
	 * @return the deleted nation name.
	 */
	public String getNationName() {
		return nationName;
	}

	/**
	 * @return the deleted nation object.
	 */
	public Nation getNation() {
		return nation;
	}
}
