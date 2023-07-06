package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreDeleteTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final String townName;
	private final Town town;

	public PreDeleteTownEvent(Town town) {
		this.townName = town.getName();
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
