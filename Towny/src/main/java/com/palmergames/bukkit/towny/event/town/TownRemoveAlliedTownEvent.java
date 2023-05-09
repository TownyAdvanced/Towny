package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownRemoveAlliedTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Town town;
	private final Town removedAlly;
	
	public TownRemoveAlliedTownEvent(Town town, Town newAlly) {
		this.town = town;
		this.removedAlly = newAlly;
		setCancelMessage("The disolution of a town alliance was cancelled by another plugin.");
	}

	/**
	 * @return town Town which is removing an ally.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return removedAlly Town which is being removed as an ally.
	 */
	public Town getRemovedAlly() {
		return removedAlly;
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
