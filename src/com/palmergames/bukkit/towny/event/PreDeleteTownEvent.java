package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/*
 * @author LlmDl
 *
 */

public class PreDeleteTownEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private String townName;
	private Town town;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public PreDeleteTownEvent(String town) {
		this.townName = town;
	}

	public PreDeleteTownEvent(Town town) {
		this.town = town;
	}

	/**
	 * @return the deleted town name.
	 */
	public String getTownName() {
		return townName;
	}

	/**
	 * @return the deleted town object.
	 */
	public Town getTown() {
		return town;
	}

}
